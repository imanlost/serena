package com.imanlost.serena.estadisticas

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Logica de racha y series temporales de la practica.
 *
 * Vive en Kotlin y no en SQL a proposito: la regla "hoy todavia no cuenta, pero la
 * racha sigue viva desde ayer; se rompe cuando pasa el dia entero sin practicar" es
 * mucho mas clara (y testeable) aqui que en una consulta. Los agregados por dia y los
 * totales si se piden a la base de datos.
 */

/** Resumen de una semana para el grafico de barras. */
data class SemanaMinutos(val etiqueta: String, val minutos: Int, val dias: Int)

/**
 * Todo lo que la interfaz necesita para pintar Progreso y el resumen de Hoy.
 * `hayDatos` distingue "todavia no hay practica" de "graficos a cero".
 */
data class ResumenPractica(
    val rachaActual: Int = 0,
    val mejorRacha: Int = 0,
    val minutosTotales: Int = 0,
    val diasTotales: Int = 0,
    val minutosPorDia: Map<LocalDate, Int> = emptyMap(),
    val semanas: List<SemanaMinutos> = emptyList(),
) {
    val hayDatos: Boolean get() = minutosPorDia.isNotEmpty()
}

/**
 * Racha actual: dias naturales consecutivos con practica que cuenta.
 *
 * Si hoy aun no hay sesion, la racha no se rompe: se cuenta desde ayer. Solo se
 * pierde cuando pasa un dia entero sin practicar.
 */
fun rachaActual(dias: Collection<LocalDate>, hoy: LocalDate): Int {
    if (dias.isEmpty()) return 0
    val conPractica = dias.toHashSet()
    var cursor = when {
        hoy in conPractica -> hoy
        hoy.minusDays(1) in conPractica -> hoy.minusDays(1)
        else -> return 0
    }
    var racha = 0
    while (cursor in conPractica) {
        racha++
        cursor = cursor.minusDays(1)
    }
    return racha
}

/** Mejor racha historica: la secuencia de dias consecutivos mas larga. */
fun mejorRacha(dias: Collection<LocalDate>): Int {
    if (dias.isEmpty()) return 0
    var mejor = 0
    var actual = 0
    var anterior: LocalDate? = null
    for (dia in dias.toSortedSet()) {
        actual = if (anterior != null && dia == anterior.plusDays(1)) actual + 1 else 1
        if (actual > mejor) mejor = actual
        anterior = dia
    }
    return mejor
}

/**
 * Minutos de cada una de las ultimas [semanas] semanas naturales (lunes a domingo),
 * de la mas antigua a la mas reciente, incluida la semana en curso.
 *
 * Las semanas sin practica salen a cero para que el grafico muestre el hueco: que una
 * barra desaparezca se lee peor que una barra vacia.
 */
fun minutosPorSemana(
    minutosPorDia: Map<LocalDate, Int>,
    hoy: LocalDate,
    semanas: Int = 8,
): List<SemanaMinutos> {
    if (semanas <= 0) return emptyList()
    val lunesDeEstaSemana = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (semanas - 1 downTo 0).map { semanasAtras ->
        val lunes = lunesDeEstaSemana.minusWeeks(semanasAtras.toLong())
        var minutos = 0
        var dias = 0
        for (desplazamiento in 0L..6L) {
            val delDia = minutosPorDia[lunes.plusDays(desplazamiento)] ?: 0
            if (delDia > 0) {
                minutos += delDia
                dias++
            }
        }
        // Etiqueta corta dia/mes: las ocho barras tienen que caber sin cortarse.
        SemanaMinutos(
            etiqueta = "${lunes.dayOfMonth}/${lunes.monthValue}",
            minutos = minutos,
            dias = dias,
        )
    }
}
