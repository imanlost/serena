package com.imanlost.serena.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local de Serena: una sola tabla, `sesiones`.
 *
 * `exportSchema = false` porque la v1 no tiene versiones anteriores que migrar. Cuando
 * exista la version 2 habra que activar la exportacion de esquemas y escribir la
 * migracion explicita, en lugar de dejar que Room borre los datos.
 */
@Database(entities = [Sesion::class], version = 1, exportSchema = false)
abstract class SerenaDb : RoomDatabase() {

    abstract fun sesiones(): SesionDao

    companion object {
        private const val NOMBRE = "serena.db"

        @Volatile
        private var instancia: SerenaDb? = null

        /**
         * Instancia unica: abrir la base de datos en cada consulta seria un desproposito.
         * Se usa `applicationContext` para que la Activity y el servicio compartan la
         * misma instancia sin fugas de contexto.
         */
        fun obtener(contexto: Context): SerenaDb =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    SerenaDb::class.java,
                    NOMBRE,
                ).build().also { instancia = it }
            }
    }
}
