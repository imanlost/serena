package com.imanlost.serena.datos

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una sesion de practica registrada.
 *
 * Una sola tabla evita desincronizaciones: los agregados (racha, minutos, series) se
 * calculan con consultas sobre estas filas.
 *
 * `duracionMs` es el tiempo REAL escuchado, no el reloj de pared: lo calcula el
 * servicio a partir del temporizador, que es quien sabe cuanto ha sonado de verdad
 * (total menos lo que quedaba). `idSalud` queda reservado para el identificador que
 * devuelva Health Connect en el encargo siguiente; hoy se guarda siempre vacio.
 */
@Entity(tableName = "sesiones")
data class Sesion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inicioMs: Long,
    val finMs: Long,
    val duracionMs: Long,
    val pista: String,
    val grupo: String,
    val completada: Boolean,
    val nota: String? = null,
    val idSalud: String? = null,
)
