package com.imanlost.serena.datos

import android.content.Context
import com.imanlost.serena.estadisticas.ResumenPractica
import com.imanlost.serena.estadisticas.mejorRacha
import com.imanlost.serena.estadisticas.minutosPorSemana
import com.imanlost.serena.estadisticas.rachaActual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Punto unico de acceso al registro de sesiones.
 *
 * El servicio escribe; la interfaz observa. Al ser consultas `Flow` de Room, cualquier
 * escritura reemite el resumen y las pantallas se actualizan solas.
 */
object RepositorioSesiones {

    /** Una sesion cuenta para la racha a partir de un minuto real (regla acordada). */
    const val UMBRAL_RACHA_MS = 60_000L

    /** Registra una sesion ya terminada. El servicio es el unico que la llama. */
    suspend fun registrar(contexto: Context, sesion: Sesion) {
        SerenaDb.obtener(contexto).sesiones().insertar(sesion)
    }

    /** Todas las sesiones, la mas reciente primero: alimenta el diario de Seguimiento. */
    fun sesiones(contexto: Context): Flow<List<Sesion>> =
        SerenaDb.obtener(contexto).sesiones().observarSesiones()

    /**
     * Foto actual del historial, sin quedarse observandolo.
     *
     * La exportacion es una accion puntual (escribir el CSV ahora), no una pantalla
     * reactiva: reutiliza `sesiones()` y se queda con el primer valor.
     */
    suspend fun sesionesAhora(contexto: Context): List<Sesion> = sesiones(contexto).first()

    /**
     * Nombres de las pistas que ya tienen una sesion con duracion real.
     *
     * La biblioteca lo convierte en `Set` para preguntar por cada fila en O(1).
     */
    fun pistasEscuchadas(contexto: Context): Flow<List<String>> =
        SerenaDb.obtener(contexto).sesiones().observarPistasEscuchadas()

    /**
     * Fija la nota de una sesion. La cadena vacia (o solo espacios) se guarda como `null`
     * para que "sin nota" sea un unico estado y no dos.
     */
    suspend fun anotar(contexto: Context, id: Long, nota: String?) {
        val limpia: String? = nota?.trim()?.takeIf { it.isNotEmpty() }
        SerenaDb.obtener(contexto).sesiones().fijarNota(id, limpia)
    }

    /** Corrige la duracion de una sesion ya registrada (minutos que el usuario ajusta). */
    suspend fun cambiarDuracion(contexto: Context, id: Long, duracionMs: Long) {
        SerenaDb.obtener(contexto).sesiones().cambiarDuracion(id, duracionMs)
    }

    /** Borra una sesion del diario. */
    suspend fun borrar(contexto: Context, id: Long) {
        SerenaDb.obtener(contexto).sesiones().borrar(id)
    }

    /**
     * Resumen agregado de la practica.
     *
     * Se combinan las tres consultas del DAO y el calculo se hace fuera del hilo
     * principal (`flowOn`), porque la racha y las series son trabajo en Kotlin, no SQL.
     */
    fun resumen(contexto: Context): Flow<ResumenPractica> {
        val dao = SerenaDb.obtener(contexto).sesiones()
        return combine(
            dao.observarMinutosTotales(),
            dao.observarDiasConPractica(UMBRAL_RACHA_MS),
            dao.observarMinutosPorDia(),
        ) { totalMs, diasValidos, porDia ->
            val hoy = LocalDate.now()
            val minutosPorDia = porDia.associate { fila ->
                LocalDate.parse(fila.dia) to minutos(fila.minutos)
            }
            val dias = diasValidos.map { LocalDate.parse(it) }
            ResumenPractica(
                rachaActual = rachaActual(dias, hoy),
                mejorRacha = mejorRacha(dias),
                minutosTotales = minutos(totalMs),
                diasTotales = minutosPorDia.size,
                minutosPorDia = minutosPorDia,
                semanas = minutosPorSemana(minutosPorDia, hoy),
            )
        }.flowOn(Dispatchers.Default)
    }

    /** Milisegundos a minutos redondeados, que es la unidad que muestra la interfaz. */
    private fun minutos(ms: Long): Int = (ms / 60_000.0).roundToInt()
}
