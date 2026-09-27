package com.imanlost.serena.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado que la pantalla de sesion dibuja. Lo escribe siempre el servicio. */
data class EstadoSesion(
    val activa: Boolean = false,
    val pausada: Boolean = false,
    val finalizando: Boolean = false,
    val restanteMs: Long = 0L,
    val totalMs: Long = 0L,
    val titulo: String = "",
    // Fase de preparacion: la sesion ya esta activa, pero ni suena la voz ni corre el
    // reloj. `restanteMs` sigue siendo el tiempo completo de practica.
    val preparando: Boolean = false,
    val preparacionRestanteMs: Long = 0L,
    val preparacionTotalMs: Long = 0L,
)

/**
 * Punto de entrada de la interfaz al audio de la sesion.
 *
 * La pantalla no habla con ExoPlayer: manda ordenes al servicio en primer plano y
 * observa el estado. Asi el audio no depende de que la Activity siga viva.
 */
object ControlSesion {

    const val VOLUMEN_MIN = 0.15f
    const val VOLUMEN_MAX = 0.35f
    const val VOLUMEN_POR_DEFECTO = 0.25f

    // Valores que ofrece el dial de ajustes. Son listas cerradas para que el servicio no
    // tenga que validar numeros sueltos.
    val SEGUNDOS_PREPARACION = listOf(0, 5, 10, 15, 20, 30)
    val SEGUNDOS_VUELTA = listOf(0, 10, 20, 30, 45, 60)
    // 5 y no 10: la espera real es esta cifra MAS la cola del gong (6 s), asi que con 10 se
    // iban a 16 segundos hasta que entra la voz. El usuario pidio 5-10 s de preparacion.
    const val PREPARACION_POR_DEFECTO = 5
    const val VUELTA_POR_DEFECTO = 30

    private const val PREFS = "serena_audio"
    private const val CLAVE_VOLUMEN = "volumen_fondo"
    private const val CLAVE_CARPETA = "carpeta_audio"
    private const val CLAVE_CARPETA_NOMBRE = "carpeta_audio_nombre"
    private const val CLAVE_PREPARACION = "segundos_preparacion"
    private const val CLAVE_VUELTA = "segundos_vuelta"
    private const val CLAVE_GONG = "gong_activado"
    private const val CLAVE_FONDO_ELEGIDO = "fondo_elegido"
    private const val CLAVE_FONDO_POR_DEFECTO = "fondo_por_defecto"

    /**
     * Minutos elegidos en el dial de la pantalla Hoy.
     *
     * Vive aqui para que la biblioteca los use al arrancar la sesion, porque hay dos casos:
     * en un fondo sin voz manda este tiempo; en una meditacion guiada es el tiempo MINIMO
     * (si la meditacion dura mas, manda la meditacion; si dura menos, el fondo sigue
     * sonando hasta cumplirlo). Asi nunca se corta una voz a medias.
     */
    var minutosElegidos: Int = 10

    private val _estado = MutableStateFlow(EstadoSesion())
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    /** Carpeta de audios elegida con SAF (en texto), o null si aun no hay ninguna. */
    fun carpetaAudio(contexto: Context): String? =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CLAVE_CARPETA, null)

    /** Nombre legible de la carpeta elegida, para mostrarlo en Ajustes. */
    fun nombreCarpetaAudio(contexto: Context): String? =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CLAVE_CARPETA_NOMBRE, null)

    /**
     * Guarda la carpeta elegida. El permiso persistente lo toma quien recibe la URI
     * (el selector SAF); aqui solo se recuerda la eleccion entre reinicios.
     */
    fun guardarCarpetaAudio(contexto: Context, uri: Uri, nombre: String?) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CLAVE_CARPETA, uri.toString())
            .putString(CLAVE_CARPETA_NOMBRE, nombre)
            .apply()
    }

    /** Volumen relativo del fondo recordado entre sesiones. */
    fun volumenFondo(contexto: Context): Float =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(CLAVE_VOLUMEN, VOLUMEN_POR_DEFECTO)
            .coerceIn(VOLUMEN_MIN, VOLUMEN_MAX)

    fun fijarVolumen(contexto: Context, fraccion: Float) {
        val volumen = fraccion.coerceIn(VOLUMEN_MIN, VOLUMEN_MAX)
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(CLAVE_VOLUMEN, volumen)
            .apply()
        // Si hay sesion en marcha, el cambio se oye al momento.
        if (_estado.value.activa) {
            contexto.startService(
                Intent(contexto, ServicioSesion::class.java).apply {
                    action = ServicioSesion.ACCION_VOLUMEN
                    putExtra(ServicioSesion.EXTRA_VOLUMEN, volumen)
                },
            )
        }
    }

    /** Segundos de silencio antes de que arranquen la voz y el fondo. */
    fun segundosPreparacion(contexto: Context): Int {
        val guardado = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(CLAVE_PREPARACION, PREPARACION_POR_DEFECTO)
        return if (guardado in SEGUNDOS_PREPARACION) guardado else PREPARACION_POR_DEFECTO
    }

    fun fijarSegundosPreparacion(contexto: Context, segundos: Int) {
        if (segundos !in SEGUNDOS_PREPARACION) return
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(CLAVE_PREPARACION, segundos)
            .apply()
    }

    /** Segundos que el fondo tarda en apagarse al terminar (pausa de vuelta). */
    fun segundosVuelta(contexto: Context): Int {
        val guardado = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(CLAVE_VUELTA, VUELTA_POR_DEFECTO)
        return if (guardado in SEGUNDOS_VUELTA) guardado else VUELTA_POR_DEFECTO
    }

    fun fijarSegundosVuelta(contexto: Context, segundos: Int) {
        if (segundos !in SEGUNDOS_VUELTA) return
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(CLAVE_VUELTA, segundos)
            .apply()
    }

    /** Gong de apertura y cierre. Apagado, el ritual se mantiene pero en silencio. */
    fun gongActivado(contexto: Context): Boolean =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(CLAVE_GONG, true)

    fun fijarGong(contexto: Context, activado: Boolean) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CLAVE_GONG, activado)
            .apply()
    }

    /**
     * Ultimo fondo elegido en el dialogo de la biblioteca.
     *
     * Devuelve null si el usuario aun no ha elegido nunca (para poder caer al fondo por
     * defecto de Ajustes) y la cadena vacia si eligio «Sin fondo»: son dos casos distintos.
     */
    fun fondoElegido(contexto: Context): String? =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CLAVE_FONDO_ELEGIDO, null)

    /** Recuerda el fondo elegido; null representa «Sin fondo». */
    fun recordarFondoElegido(contexto: Context, idFondo: String?) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CLAVE_FONDO_ELEGIDO, idFondo ?: "")
            .apply()
    }

    /**
     * Fondo por defecto de Ajustes → Sonido, usado cuando aun no hay eleccion recordada.
     * Sin valor guardado (o «Sin fondo») devuelve null.
     */
    fun fondoPorDefecto(contexto: Context): String? =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CLAVE_FONDO_POR_DEFECTO, null)
            ?.takeIf { it.isNotBlank() }

    fun fijarFondoPorDefecto(contexto: Context, idFondo: String?) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CLAVE_FONDO_POR_DEFECTO, idFondo ?: "")
            .apply()
    }

    /**
     * Duracion total de la preparacion: los segundos elegidos mas la cola real del gong.
     * El servicio no debe arrancar la meditacion con el gong aun sonando.
     */
    fun preparacionTotalMs(contexto: Context): Long {
        val esperaGong = if (gongActivado(contexto)) Sonidos.DURACION_GONG_MS else 0L
        return esperaGong + segundosPreparacion(contexto) * 1000L
    }

    fun iniciar(contexto: Context, voz: Pista, fondo: Pista?, minutos: Int) {
        val totalMs = minutos.coerceIn(1, 60) * 60_000L
        // Se publica ya el estado para que la pantalla no parpadee antes de que arranque
        // el servicio. Durante la preparacion el reloj esta a cero: se muestra la cuenta
        // atras de preparacion, no el tiempo de practica.
        val preparacionMs = preparacionTotalMs(contexto)
        _estado.value = EstadoSesion(
            activa = true,
            totalMs = totalMs,
            restanteMs = totalMs,
            titulo = voz.titulo,
            preparando = preparacionMs > 0L,
            preparacionRestanteMs = preparacionMs,
            preparacionTotalMs = preparacionMs,
        )
        contexto.startService(
            Intent(contexto, ServicioSesion::class.java).apply {
                action = ServicioSesion.ACCION_INICIAR
                putExtra(ServicioSesion.EXTRA_VOZ, voz.ruta)
                putExtra(ServicioSesion.EXTRA_TITULO, voz.titulo)
                putExtra(ServicioSesion.EXTRA_FONDO, fondo?.ruta)
                // La carpeta relativa viaja con la orden: el servicio la necesita para el registro,
                // porque al terminar ya no tiene la Pista delante. Se guarda la ruta completa
                // (`curso/nivel 2`) para que el historial diga de donde salia cada practica.
                putExtra(ServicioSesion.EXTRA_GRUPO, voz.carpeta.ifEmpty { Biblioteca.GRUPO_SUELTAS })
                // OJO: tiene que ser un Int. Estuvo enviandose como Long y el servicio, que
                // lo lee con getIntExtra, recibia el desajuste en silencio y usaba su valor
                // por defecto: el temporizador marcaba siempre 10 minutos.
                putExtra(ServicioSesion.EXTRA_MINUTOS, minutos)
                putExtra(ServicioSesion.EXTRA_VOLUMEN, volumenFondo(contexto))
            },
        )
    }

    fun pausar(contexto: Context) = enviar(contexto, ServicioSesion.ACCION_PAUSAR)

    fun reanudar(contexto: Context) = enviar(contexto, ServicioSesion.ACCION_REANUDAR)

    fun terminar(contexto: Context) = enviar(contexto, ServicioSesion.ACCION_TERMINAR)

    internal fun publicar(nuevo: EstadoSesion) {
        _estado.value = nuevo
    }

    private fun enviar(contexto: Context, accion: String) {
        contexto.startService(Intent(contexto, ServicioSesion::class.java).apply { action = accion })
    }
}
