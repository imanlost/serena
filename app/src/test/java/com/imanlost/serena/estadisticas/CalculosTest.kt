package com.imanlost.serena.estadisticas

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Casos de la racha acordados en el plan: sin sesiones, un dia, cinco seguidos,
 * hueco de un dia, dos sesiones el mismo dia y racha que sigue viva si hoy aun no
 * ha habido practica. Todos los datos son dias naturales, no instantes.
 */
class CalculosTest {

    private val hoy: LocalDate = LocalDate.of(2026, 1, 15) // jueves

    @Test
    fun sinSesionesNoHayRacha() {
        assertEquals(0, rachaActual(emptyList(), hoy))
        assertEquals(0, mejorRacha(emptyList()))
    }

    @Test
    fun unDiaCuentaComoUno() {
        assertEquals(1, rachaActual(listOf(hoy), hoy))
        assertEquals(1, mejorRacha(listOf(hoy)))
    }

    @Test
    fun cincoDiasSeguidos() {
        val dias = (0L..4L).map { hoy.minusDays(it) }
        assertEquals(5, rachaActual(dias, hoy))
        assertEquals(5, mejorRacha(dias))
    }

    @Test
    fun huecoDeUnDiaRompeLaRacha() {
        // Hoy y anteayer, pero no ayer: la racha actual es 1 y la mejor tambien.
        val dias = listOf(hoy, hoy.minusDays(2))
        assertEquals(1, rachaActual(dias, hoy))
        assertEquals(1, mejorRacha(dias))
    }

    @Test
    fun laRachaSigueVivaSiHoyAunNoHaySesion() {
        val dias = listOf(hoy.minusDays(1), hoy.minusDays(2), hoy.minusDays(3))
        assertEquals(3, rachaActual(dias, hoy))
    }

    @Test
    fun dosSesionesElMismoDiaCuentanUnaVez() {
        val dias = listOf(hoy, hoy)
        assertEquals(1, rachaActual(dias, hoy))
        assertEquals(1, mejorRacha(dias))
    }

    @Test
    fun mejorRachaEsLaSecuenciaMasLarga() {
        val dias = listOf(
            hoy.minusDays(10), hoy.minusDays(9), hoy.minusDays(8), hoy.minusDays(7),
            hoy.minusDays(1), hoy,
        )
        assertEquals(2, rachaActual(dias, hoy))
        assertEquals(4, mejorRacha(dias))
    }

    @Test
    fun lasSemanasVanDeLunesADomingoEIncluyenLaActual() {
        // El 15 de enero de 2026 es jueves: su lunes es el 12.
        val semanas = minutosPorSemana(mapOf(hoy to 25), hoy, semanas = 2)
        assertEquals(2, semanas.size)
        assertEquals(0, semanas[0].minutos)
        assertEquals(25, semanas[1].minutos)
        assertEquals(1, semanas[1].dias)
        assertEquals("12/1", semanas[1].etiqueta)
    }
}
