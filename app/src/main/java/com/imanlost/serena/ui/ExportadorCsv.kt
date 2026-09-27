package com.imanlost.serena.ui

import android.content.Context
import android.net.Uri
import com.imanlost.serena.datos.Sesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Exportacion del historial de sesiones a CSV.
 *
 * Todo local: se escribe en la URI que devuelve el selector de documentos del sistema, sin
 * permisos de almacenamiento y sin que nada salga del telefono.
 */

/** Cabecera exacta del fichero. */
private const val CABECERA = "fecha,hora,pista,grupo,minutos,completada,nota"

/** Zona del dispositivo: la fecha y la hora del CSV son las locales, como en la app. */
private val ZONA: ZoneId = ZoneId.systemDefault()

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Nombre sugerido para el selector: con la fecha del dia no se pisa la exportacion anterior. */
fun nombreFicheroCsv(hoy: LocalDate = LocalDate.now(ZONA)): String = "serena-sesiones-$hoy.csv"

/**
 * Genera el CSV completo, la sesion mas reciente primero.
 *
 * Se separa de la escritura para poder verificarlo sin tocar el disco.
 */
internal fun generarCsv(sesiones: List<Sesion>): String {
    val filas = sesiones
        .sortedByDescending { it.inicioMs }
        .map { sesion ->
            val inicio = Instant.ofEpochMilli(sesion.inicioMs).atZone(ZONA)
            listOf(
                FORMATO_FECHA.format(inicio.toLocalDate()),
                FORMATO_HORA.format(inicio.toLocalTime()),
                campoCsv(sesion.pista),
                campoCsv(sesion.grupo),
                // Los mismos minutos que ensena la app (mismo redondeo que el diario).
                minutosDeSesion(sesion).toString(),
                if (sesion.completada) "si" else "no",
                campoCsv(sesion.nota.orEmpty()),
            ).joinToString(",")
        }
    // Salto \n explicito: en CSV se abre igual en cualquier hoja de calculo.
    return (listOf(CABECERA) + filas).joinToString("\n") + "\n"
}

/**
 * Escapa un campo de texto: entre comillas dobles, duplicando las internas y convirtiendo
 * los saltos de linea en espacio. Se aplica a pista, grupo y nota (pueden llevar comas).
 */
private fun campoCsv(valor: String): String {
    val plano = valor.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ')
    return "\"" + plano.replace("\"", "\"\"") + "\""
}

/**
 * Escribe el CSV en la URI elegida por el usuario.
 *
 * Devuelve `true` solo si se ha escrito entero; cualquier fallo del proveedor queda en
 * `false` para poder avisar en pantalla sin que la app reviente.
 */
suspend fun exportarCsv(
    contexto: Context,
    uri: Uri,
    sesiones: List<Sesion>,
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        contexto.contentResolver.openOutputStream(uri)?.use { salida ->
            salida.bufferedWriter(Charsets.UTF_8).use { escritor ->
                escritor.write(generarCsv(sesiones))
            }
        } ?: error("El proveedor devolvio un flujo nulo")
    }.isSuccess
}
