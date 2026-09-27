package com.imanlost.serena.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Fila agregada del DAO: minutos practicados en un dia natural.
 *
 * `dia` es la fecha local en formato ISO (`yyyy-MM-dd`), tal como la devuelve SQLite.
 */
data class MinutosPorDia(val dia: String, val minutos: Long)

/**
 * Acceso a la tabla `sesiones`.
 *
 * Todas las consultas devuelven `Flow`: Room las re-ejecuta cuando la tabla cambia,
 * asi la interfaz se actualiza sola despues de registrar una sesion. Las sumas y los
 * agrupamientos se resuelven en SQL, nunca en el hilo principal.
 */
@Dao
interface SesionDao {

    @Insert
    suspend fun insertar(sesion: Sesion): Long

    /**
     * Todas las sesiones, la mas reciente primero: es la fuente del diario.
     *
     * Sin paginacion a proposito: una fila por sesion es una tabla pequena y el diario
     * necesita verlas todas para agruparlas por dia.
     */
    @Query("SELECT * FROM sesiones ORDER BY inicioMs DESC")
    fun observarSesiones(): Flow<List<Sesion>>

    /** Fija la nota de una sesion; `null` la deja sin nota. */
    @Query("UPDATE sesiones SET nota = :nota WHERE id = :id")
    suspend fun fijarNota(id: Long, nota: String?)

    /** Corrige la duracion real de una sesion ya registrada. */
    @Query("UPDATE sesiones SET duracionMs = :duracionMs WHERE id = :id")
    suspend fun cambiarDuracion(id: Long, duracionMs: Long)

    /** Borra una sesion. El resumen y la racha se recalculan solos: son Flows de Room. */
    @Query("DELETE FROM sesiones WHERE id = :id")
    suspend fun borrar(id: Long)

    /** Total practicado, en milisegundos (0 si todavia no hay sesiones). */
    @Query("SELECT COALESCE(SUM(duracionMs), 0) FROM sesiones")
    fun observarMinutosTotales(): Flow<Long>

    /**
     * Dias naturales (locales) con al menos una sesion que cuenta para la racha.
     *
     * Se usa `'localtime'` a proposito: la racha es por dia natural del dispositivo,
     * no por dia UTC; si no, una sesion de las 23:00 en verano podria contar como el
     * dia siguiente. El umbral lo pone `Calculos`/repositorio, no la consulta.
     */
    @Query(
        "SELECT DISTINCT date(inicioMs / 1000, 'unixepoch', 'localtime') FROM sesiones " +
            "WHERE duracionMs >= :minimoMs ORDER BY 1 DESC",
    )
    fun observarDiasConPractica(minimoMs: Long): Flow<List<String>>

    /**
     * Minutos practicados por dia natural, para el calendario de constancia y las
     * barras por semana. Se descartan las sesiones de duracion cero (un toque
     * accidental) para que no creen un dia fantasma.
     */
    @Query(
        "SELECT date(inicioMs / 1000, 'unixepoch', 'localtime') AS dia, " +
            "SUM(duracionMs) AS minutos FROM sesiones " +
            "WHERE duracionMs > 0 GROUP BY dia ORDER BY dia",
    )
    fun observarMinutosPorDia(): Flow<List<MinutosPorDia>>

    /**
     * Nombres de pista con al menos una sesion de duracion real.
     *
     * La biblioteca compara por nombre (es lo unico que guarda la tabla) para marcar
     * las pistas ya escuchadas. `DISTINCT` deja el resultado pequeno y sin duplicados,
     * que es lo que espera el `Set` que consume la lista.
     */
    @Query("SELECT DISTINCT pista FROM sesiones WHERE duracionMs > 0")
    fun observarPistasEscuchadas(): Flow<List<String>>
}
