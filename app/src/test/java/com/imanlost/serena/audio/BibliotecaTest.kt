package com.imanlost.serena.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Casos del arbol de la biblioteca, con la carpeta del usuario como referencia: en la raiz elegida
 * hay `guiadas` y `fondo`, y dentro de `Curso` un nivel por subcarpeta. Antes solo se leia el
 * primer nivel y los niveles no aparecian.
 */
class BibliotecaTest {

    private fun pista(titulo: String, carpeta: String) = Pista(
        id = "$carpeta/$titulo",
        titulo = titulo,
        ruta = "content://serena/$carpeta/$titulo",
        duracionMs = 60_000L,
        carpeta = carpeta,
    )

    @Test
    fun lasSubcarpetasDeCualquierNivelCuelganDeSuCarpeta() {
        val ramas = Biblioteca.jerarquia(
            listOf(
                pista("Pista 1", "Curso/Nivel 1"),
                pista("Olas del mar", "fondo"),
                pista("Guiada 1", "guiadas"),
            ),
        )
        assertEquals(listOf("guiadas", "fondo", "Curso"), ramas.map { it.nombre })
        val curso = ramas.single { it.nombre == "Curso" }
        assertEquals(1, curso.total)
        assertEquals(listOf("Nivel 1"), curso.hijas.map { it.nombre })
        assertEquals("Pista 1", curso.hijas.single().pistas.single().titulo)
    }

    @Test
    fun elBloqueCuentaTambienLoQueCuelgaDeSusSubcarpetas() {
        val ramas = Biblioteca.jerarquia(
            listOf(
                pista("A", "Curso/Nivel 1"),
                pista("B", "Curso/Nivel 2"),
                pista("C", "Curso"),
            ),
        )
        val curso = ramas.single()
        assertEquals(3, curso.total)
        assertEquals(1, curso.pistas.size)
        assertEquals(listOf("Nivel 1", "Nivel 2"), curso.hijas.map { it.nombre })
    }

    @Test
    fun laRaizEsElBloqueDePistasSueltasYVaDespuesDeLosConocidos() {
        val ramas = Biblioteca.jerarquia(
            listOf(
                pista("Suelta", ""),
                pista("Guiada 1", "guiadas"),
                pista("Pista 1", "Curso/Nivel 1"),
            ),
        )
        assertEquals(listOf("guiadas", "sueltas", "Curso"), ramas.map { it.nombre })
        assertEquals(1, ramas.single { it.nombre == "sueltas" }.total)
    }

    @Test
    fun dosCarpetasConElMismoNombreEnRamasDistintasNoSeMezclan() {
        val ramas = Biblioteca.jerarquia(
            listOf(
                pista("A", "guiadas"),
                pista("B", "copia/guiadas"),
            ),
        )
        assertEquals(listOf("guiadas", "copia"), ramas.map { it.nombre })
        assertEquals(1, ramas.first().total)
        val copia = ramas.single { it.nombre == "copia" }
        assertEquals(listOf("guiadas"), copia.hijas.map { it.nombre })
        assertEquals(1, copia.hijas.single().pistas.size)
    }

    @Test
    fun losNumerosSeOrdenanComoNumeros() {
        // Alfabeticamente saldria Pista 1, Pista 10, Pista 2.
        val ramas = Biblioteca.jerarquia(
            listOf(
                pista("Curso - Nivel 1 - Pista 10", "Curso/Nivel 1"),
                pista("Curso - Nivel 1 - Pista 2", "Curso/Nivel 1"),
                pista("Curso - Nivel 1 - Pista 1", "Curso/Nivel 1"),
            ),
        )
        assertEquals(
            listOf(
                "Curso - Nivel 1 - Pista 1",
                "Curso - Nivel 1 - Pista 2",
                "Curso - Nivel 1 - Pista 10",
            ),
            ramas.single().hijas.single().pistas.map { it.titulo },
        )
    }

    @Test
    fun losNivelesSeOrdenanPorSuNombre() {
        val ramas = Biblioteca.jerarquia(
            listOf(
                pista("A", "Curso/10 - Noche"),
                pista("B", "Curso/2 - Repaso"),
                pista("C", "Curso/Nivel 2"),
                pista("D", "Curso/Nivel 1"),
            ),
        )
        assertEquals(
            listOf("2 - Repaso", "10 - Noche", "Nivel 1", "Nivel 2"),
            ramas.single().hijas.map { it.nombre },
        )
    }

    @Test
    fun unaCarpetaFondoEnCualquierNivelSuenaSola() {
        assertTrue(pista("Lluvia", "fondo").esFondo)
        assertTrue(pista("Lluvia", "Curso/fondo").esFondo)
        assertFalse(pista("Guiada 1", "guiadas").esFondo)
        assertFalse(pista("Suelta", "").esFondo)
    }

    @Test
    fun elTituloPierdeLosGuionesBajosYElPrefijoDeOrden() {
        assertEquals("Ejemplo Corto", Biblioteca.tituloDesdeNombre("01_Ejemplo_Corto.mp3", "guiadas"))
        assertEquals("guiada 1", Biblioteca.tituloDesdeNombre("1_guiada_1.mp3", "guiadas"))
        assertEquals(
            "Ejemplo de Nombre Largo Con Guiones",
            Biblioteca.tituloDesdeNombre(
                "05_Ejemplo-de-Nombre-Largo_Con-Guiones.mp3",
                "fondo",
            ),
        )
    }

    @Test
    fun elTituloNoRepiteElNombreDeLaCarpetaQueLoContiene() {
        // Como estan en el disco: el nivel repite su nombre en cada fichero.
        assertEquals(
            "Pista 7",
            Biblioteca.tituloDesdeNombre("Curso - Nivel 1 - Pista 7.mp3", "Curso/Nivel 1"),
        )
        assertEquals(
            "Intro",
            Biblioteca.tituloDesdeNombre("Curso Nivel 1 Intro.mp3", "Curso/Nivel 1"),
        )
        // Un nivel de la raiz de Curso: se quita el tramo que se repite y se deja el resto.
        assertEquals(
            "Bloque Pista 30",
            Biblioteca.tituloDesdeNombre("Curso - Bloque - Pista 30.mp3", "Curso"),
        )
    }
}
