package com.imanlost.serena.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Escaneo de la carpeta de audios elegida por el usuario.
 *
 * Se recorre con SAF (`DocumentFile` sobre el arbol persistente), no con MediaStore:
 * asi funciona tambien en la tarjeta SD y sin ningun permiso de almacenamiento. La
 * duracion se LEE del fichero con `MediaMetadataRetriever`, nunca se estima.
 *
 * El recorrido es RECURSIVO: la biblioteca del usuario esta ordenada por carpetas y
 * subcarpetas (`fondo`, `guiadas`, `curso/nivel 2`...), asi que quedarse en el primer
 * nivel dejaba fuera todo lo que estuviera a dos niveles o mas.
 */
object Biblioteca {

    /** Grupo para los ficheros que estan directamente en la carpeta elegida. */
    const val GRUPO_SUELTAS = "sueltas"

    /** Grupo de las pistas pensadas para sonar de fondo. */
    const val GRUPO_FONDO = "fondo"

    private const val TAG = "SerenaBiblioteca"

    /** Separador de la ruta relativa de las subcarpetas. */
    private const val SEPARADOR = "/"

    /** Orden de los grupos conocidos; el resto van despues, por orden alfabetico. */
    private val ORDEN_GRUPOS = listOf("guiadas", "fondo", "podcasts")

    private val EXTENSIONES = setOf("mp3", "m4a", "m4b", "aac", "ogg", "oga", "opus", "wav", "flac")

    /**
     * Tope de niveles que se recorren. Una biblioteca ordenada no pasa de aqui y evita que
     * un enlace raro del proveedor (o una carpeta que se apunte a si misma) cuelgue el escaneo.
     */
    private const val PROFUNDIDAD_MAX = 8

    /**
     * Duraciones que se leen a la vez. Abrir cada fichero con `MediaMetadataRetriever` es la
     * parte lenta del escaneo (y en una tarjeta SD el acceso es secuencial): con 210 audios en
     * serie el usuario se comia varios segundos de rueda de carga. Mas de seis lecturas a la
     * vez solo hacen cola en la tarjeta.
     */
    private const val PARALELISMO = 6

    /**
     * Duraciones ya leidas, por URI.
     *
     * Leer la duracion con `MediaMetadataRetriever` es lo caro del escaneo, y la pantalla de
     * Audios vuelve a escanear cada vez que se entra en ella: con la biblioteca del usuario
     * (mas de 200 audios) sin esta cache cada visita costaba segundos. Vive en memoria: si el
     * proceso muere se vuelve a leer y ya esta (la biblioteca no cambia sola).
     */
    private val duraciones = ConcurrentHashMap<String, Long>()

    /**
     * Una carpeta de la biblioteca con lo que cuelga de ella.
     *
     * `clave` es la ruta relativa, que es lo que identifica la carpeta en toda la app; `nombre` es
     * como se llama en la lista (el ultimo tramo: en `curso/nivel 2` el nombre es `nivel 2`), y
     * `total` cuenta tambien lo que hay en sus subcarpetas, para poder decir cuantas pistas cuelgan
     * de un bloque sin desplegarlo.
     */
    data class Rama(
        val clave: String,
        val nombre: String,
        val pistas: List<Pista>,
        val hijas: List<Rama>,
    ) {
        val total: Int get() = pistas.size + hijas.sumOf { it.total }
    }

    /**
     * Organiza las pistas en arbol.
     *
     * `Curso` es un bloque y cada nivel (`Nivel 1`, `Nivel 2`...) va DENTRO, como esta en el
     * almacenamiento: la pantalla queda en tres lineas (Guiadas, Fondos, Curso) y el detalle se
     * abre al tocar. La profundidad no esta limitada: `a/b/c` cuelga de `a/b`, que cuelga de `a`.
     */
    fun jerarquia(pistas: List<Pista>): List<Rama> {
        val raiz = Nodo("")
        val nodos = mutableMapOf("" to raiz)
        pistas.groupBy { it.carpeta }.forEach { (carpeta, lista) ->
            var padre = raiz
            if (carpeta.isNotEmpty()) {
                var ruta = ""
                carpeta.split(SEPARADOR).forEach { tramo ->
                    ruta = juntaRuta(ruta, tramo)
                    // Los nodos intermedios (`Curso`) se crean aunque no tengan ficheros.
                    val nodo = nodos.getOrPut(ruta) { Nodo(ruta).also { padre.hijas += it } }
                    padre = nodo
                }
            }
            padre.pistas += lista
        }
        // Las pistas sueltas de la raiz son una rama mas, con la etiqueta de siempre.
        val sueltas = if (raiz.pistas.isEmpty()) {
            emptyList()
        } else {
            listOf(Rama("", GRUPO_SUELTAS, raiz.pistas.sortedWith(porTitulo), emptyList()))
        }
        return (raiz.hijas.map { it.aRama() } + sueltas).sortedWith(porRama)
    }

    /** Carpeta mientras se arma el arbol; se convierte en `Rama` al terminar. */
    private class Nodo(val clave: String) {
        val pistas = mutableListOf<Pista>()
        val hijas = mutableListOf<Nodo>()

        fun aRama(): Rama = Rama(
            clave = clave,
            nombre = clave.substringAfterLast(SEPARADOR),
            pistas = pistas.sortedWith(porTitulo),
            hijas = hijas.map { it.aRama() }.sortedWith(porRama),
        )
    }

    // --- Recorrido del arbol ------------------------------------------------------------

    /**
     * Devuelve las pistas de la carpeta elegida, a cualquier profundidad.
     *
     * El grupo de cada pista es su carpeta contenedora (ruta relativa); la raiz es `sueltas`.
     */
    suspend fun escanear(contexto: Context, uriCarpeta: Uri): List<Pista> {
        val raiz = DocumentFile.fromTreeUri(contexto, uriCarpeta) ?: return emptyList()
        if (!raiz.isDirectory) return emptyList()

        val inicio = System.currentTimeMillis()
        val encontrados = mutableListOf<FicheroEncontrado>()
        recorrer(raiz, "", encontrados, 0)

        val pistas = conDuraciones(contexto, encontrados)
        val milisegundos = System.currentTimeMillis() - inicio
        Log.i(
            TAG,
            "Escaneadas ${pistas.size} pistas de ${contarCarpetas(encontrados)} carpetas en $milisegundos ms",
        )
        return pistas.sortedWith(porGrupo.thenComparing(porTitulo))
    }

    /** Fichero localizado en el arbol, antes de leerle la duracion. */
    private data class FicheroEncontrado(
        val uri: Uri,
        val titulo: String,
        val carpeta: String,
    )

    private fun recorrer(
        carpeta: DocumentFile,
        ruta: String,
        destino: MutableList<FicheroEncontrado>,
        profundidad: Int,
    ) {
        if (profundidad > PROFUNDIDAD_MAX) return
        carpeta.listFiles().forEach { hijo ->
            val nombre = hijo.name?.trim().orEmpty()
            // Carpetas y ficheros ocultos (`.thumbnails`, `.nomedia`) no son biblioteca.
            if (nombre.isEmpty() || nombre.startsWith(".")) return@forEach
            when {
                hijo.isDirectory ->
                    recorrer(hijo, juntaRuta(ruta, nombre), destino, profundidad + 1)
                hijo.isFile && extensionValida(nombre) ->
                    destino += FicheroEncontrado(hijo.uri, tituloDesdeNombre(nombre, ruta), ruta)
            }
        }
    }

    private fun juntaRuta(padre: String, nombre: String): String =
        if (padre.isEmpty()) nombre else padre + SEPARADOR + nombre

    private fun contarCarpetas(encontrados: List<FicheroEncontrado>): Int =
        encontrados.map { it.carpeta }.distinct().size

    /**
     * Lee la duracion de cada fichero con varios hilos. El reparto se hace con una cola
     * compartida: cada hilo coge el siguiente fichero cuando acaba el suyo, asi que un fichero
     * grande no deja a los demas esperando.
     */
    private suspend fun conDuraciones(
        contexto: Context,
        encontrados: List<FicheroEncontrado>,
    ): List<Pista> {
        if (encontrados.isEmpty()) return emptyList()
        val cola = ConcurrentLinkedQueue(encontrados)
        return coroutineScope {
            (1..PARALELISMO).map {
                async(Dispatchers.IO) {
                    val salida = mutableListOf<Pista>()
                    while (true) {
                        val fichero = cola.poll() ?: break
                        val uri = fichero.uri.toString()
                        salida += Pista(
                            // La propia URI de SAF sirve de id: es unica y estable entre reinicios.
                            id = uri,
                            titulo = fichero.titulo,
                            ruta = uri,
                            duracionMs = duracionDesdeUri(contexto, fichero.uri),
                            carpeta = fichero.carpeta,
                        )
                    }
                    salida
                }
            }.awaitAll().flatten()
        }
    }

    private fun extensionValida(nombre: String): Boolean {
        val extension = nombre.substringAfterLast('.', "").lowercase()
        return extension.isNotEmpty() && extension in EXTENSIONES
    }

    private fun duracionDesdeUri(contexto: Context, uri: Uri): Long {
        val clave = uri.toString()
        duraciones[clave]?.let { return it }
        val leida = leerDuracion(contexto, uri)
        // No se cachea un 0: si la lectura ha fallado, la proxima vez se vuelve a intentar.
        if (leida > 0L) duraciones[clave] = leida
        return leida
    }

    private fun leerDuracion(contexto: Context, uri: Uri): Long {
        val lector = MediaMetadataRetriever()
        return try {
            lector.setDataSource(contexto, uri)
            lector.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            runCatching { lector.release() }
        }
    }

    /** Titulo visible de una pista: nombre del fichero despejado y sin la carpeta repetida. */
    internal fun tituloDesdeNombre(nombreArchivo: String, carpeta: String): String = quitarCarpeta(
        nombre = base(nombreArchivo),
        carpeta = carpeta,
    ).ifBlank { base(nombreArchivo) }

    /**
     * Despeja el nombre del fichero: fuera la extension, el prefijo de orden y los separadores.
     *
     * Los guiones bajos y los guiones pasan a espacios porque en pantalla los nombres de descarga
     * (`MEDITACION_GUIADA_TU_GLANDULA_PINEAL_-_Cuencos...`) se leen fatal y ocupan de mas.
     */
    private fun base(nombreArchivo: String): String = nombreArchivo
        .substringBeforeLast('.', nombreArchivo)
        .replace(Regex("^\\d+[\\s_.-]+"), "")
        .replace(Regex("[_]+"), " ")
        .replace(Regex("\\s*[-–—]\\s*"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    /**
     * Quita del principio del titulo el nombre de la carpeta que lo contiene, si se repite.
     *
     * En `guiadas` el nombre del fichero ya dice todo, pero en `Curso/Nivel 1` los ficheros se
     * llaman `Curso - Nivel 1 - Pista 7` y dentro del grupo basta con `Pista 7`. Se prueba de dentro
     * hacia fuera (`Nivel 1` y luego `Curso`) y en bucle, porque el nombre puede venir desordenado.
     */
    private fun quitarCarpeta(nombre: String, carpeta: String): String {
        val tramos = carpeta.split(SEPARADOR).filter { it.isNotBlank() }.reversed()
        var resto = nombre
        var repetir = true
        while (repetir) {
            repetir = false
            tramos.forEach { tramo ->
                val patron = Regex("^" + Regex.escape(tramo) + "(\\s+|$)", RegexOption.IGNORE_CASE)
                val recortado = resto.replaceFirst(patron, "")
                if (recortado != resto && recortado.isNotBlank()) {
                    resto = recortado
                    repetir = true
                }
            }
        }
        return resto.trim()
    }

    // --- Orden ---------------------------------------------------------------------------

    /** Los grupos conocidos primero, en su orden; luego las pistas sueltas y por ultimo el resto. */
    private fun prioridadCarpeta(carpeta: String): Int {
        val indice = ORDEN_GRUPOS.indexOf(carpeta)
        return when {
            indice >= 0 -> indice
            // La raiz (pistas sueltas) va detras de los grupos conocidos y delante de los bloques.
            carpeta.isEmpty() -> ORDEN_GRUPOS.size
            else -> ORDEN_GRUPOS.size + 1
        }
    }

    /**
     * Comparador que ordena los tramos numericos como numeros.
     *
     * Alfabeticamente, `Pista 10` va antes que `Pista 2` y `10 - Noche` antes que `2 - Repaso`: en
     * una biblioteca por carpetas y pistas numeradas eso se ve enseguida y molesta.
     */
    internal val NATURAL: Comparator<String> = Comparator { a, b -> compararNatural(a, b) }

    internal fun compararNatural(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            if (a[i].isDigit() && b[j].isDigit()) {
                var finA = i
                while (finA < a.length && a[finA].isDigit()) finA++
                var finB = j
                while (finB < b.length && b[finB].isDigit()) finB++
                // Sin ceros a la izquierda y comparando por longitud: `9` < `10`.
                val numA = a.substring(i, finA).trimStart('0').ifEmpty { "0" }
                val numB = b.substring(j, finB).trimStart('0').ifEmpty { "0" }
                val comparacion =
                    if (numA.length != numB.length) numA.length - numB.length else numA.compareTo(numB)
                if (comparacion != 0) return comparacion
                i = finA
                j = finB
            } else {
                val caracterA = a[i].lowercaseChar()
                val caracterB = b[j].lowercaseChar()
                if (caracterA != caracterB) return caracterA.compareTo(caracterB)
                i++
                j++
            }
        }
        return (a.length - i) - (b.length - j)
    }

    // OJO con el orden: estos dos van DESPUES de NATURAL. Kotlin inicializa las propiedades de
    // un object de arriba abajo, y usarla antes daria "must be initialized".
    private val porGrupo = compareBy<Pista> { prioridadCarpeta(it.carpeta) }
        .thenComparing(compareBy(NATURAL) { it.carpeta })

    private val porTitulo = compareBy(NATURAL) { pista: Pista -> pista.titulo }

    /**
     * Orden de las carpetas en la lista: primero los bloques conocidos (Guiadas, Fondos...), luego las
     * pistas sueltas y despues el resto, y a igualdad de prioridad por el nombre visible con el
     * criterio natural (`2 - Repaso` antes que `10 - Noche`; `Nivel` antes que `Nivel 2`).
     */
    private val porRama = Comparator<Rama> { a, b ->
        val porPrioridad = prioridadCarpeta(a.clave) - prioridadCarpeta(b.clave)
        if (porPrioridad != 0) porPrioridad else NATURAL.compare(a.nombre, b.nombre)
    }
}

/** Convierte la ruta guardada en la pista a un Uri que ExoPlayer entiende. */
fun uriDePista(ruta: String): Uri =
    if (ruta.contains("://")) Uri.parse(ruta) else Uri.fromFile(File(ruta))
