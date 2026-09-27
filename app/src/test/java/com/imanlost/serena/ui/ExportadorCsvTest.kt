package com.imanlost.serena.ui

import com.imanlost.serena.datos.Sesion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Casos del CSV del historial: la cabecera, el orden, los minutos redondeados y el escapado
 * de los campos de texto. Una nota escrita a mano puede llevar comas, comillas o saltos de
 * linea, y nada de eso puede romper el fichero ni desplazar las columnas.
 */
class ExportadorCsvTest {

    private val zona: ZoneId = ZoneId.systemDefault()

    private fun sesion(
        dia: String,
        hora: String,
        pista: String,
        grupo: String = "guiadas",
        duracionMs: Long = 600_000L,
        completada: Boolean = true,
        nota: String? = null,
    ): Sesion {
        val inicio = LocalDateTime.parse("${dia}T$hora").atZone(zona).toInstant().toEpochMilli()
        return Sesion(
            inicioMs = inicio,
            finMs = inicio + duracionMs,
            duracionMs = duracionMs,
            pista = pista,
            grupo = grupo,
            completada = completada,
            nota = nota,
        )
    }

    @Test
    fun cabeceraYLuegoLaMasRecientePrimero() {
        val csv = generarCsv(
            listOf(
                sesion("2026-09-26", "21:30", "Escaneo corporal"),
                sesion("2026-09-27", "08:05", "Respirar y soltar"),
            ),
        )
        val lineas = csv.trim().split("\n")
        assertEquals("fecha,hora,pista,grupo,minutos,completada,nota", lineas[0])
        assertEquals("2026-09-27,08:05,\"Respirar y soltar\",\"guiadas\",10,si,\"\"", lineas[1])
        assertEquals("2026-09-26,21:30,\"Escaneo corporal\",\"guiadas\",10,si,\"\"", lineas[2])
    }

    @Test
    fun laNotaSeEscapaConComillasComasYSaltosDeLinea() {
        val csv = generarCsv(
            listOf(sesion("2026-09-27", "08:05", "Respirar", nota = "Dormido, \"a ratos\"\ny luego mejor")),
        )
        // Comillas dobladas y salto de linea convertido en espacio: la fila no se parte.
        assertTrue(csv.contains("\"Dormido, \"\"a ratos\"\" y luego mejor\""))
        assertEquals(2, csv.trim().split("\n").size)
    }

    @Test
    fun laPistaConComaVaEntrecomillada() {
        val csv = generarCsv(listOf(sesion("2026-09-27", "08:05", "Calma, respira")))
        assertTrue(csv.contains("\"Calma, respira\""))
    }

    @Test
    fun losMinutosVanRedondeadosComoEnLaApp() {
        // 95 s = 1,58 min: la app muestra 2, y el CSV no puede decir otra cosa.
        val csv = generarCsv(listOf(sesion("2026-09-27", "08:05", "Corta", duracionMs = 95_000L)))
        assertTrue(csv.contains(",\"guiadas\",2,si,"))
    }

    @Test
    fun unaSesionSinCompletarSeEscribeNo() {
        val csv = generarCsv(listOf(sesion("2026-09-27", "08:05", "A medias", completada = false)))
        assertTrue(csv.contains(",no,\""))
    }

    @Test
    fun sinSesionesSoloHayCabecera() {
        assertEquals("fecha,hora,pista,grupo,minutos,completada,nota\n", generarCsv(emptyList()))
    }

    @Test
    fun elNombreDelFicheroLlevaLaFecha() {
        assertEquals("serena-sesiones-2026-09-27.csv", nombreFicheroCsv(java.time.LocalDate.of(2026, 9, 27)))
    }
}
