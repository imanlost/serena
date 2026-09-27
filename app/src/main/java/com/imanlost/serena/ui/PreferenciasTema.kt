package com.imanlost.serena.ui

import android.content.Context

/**
 * Preferencias de interfaz de Serena: tema y paleta.
 *
 * Vive en su propio fichero para no mezclar preferencias de la interfaz con las de audio
 * de `ControlSesion`. La lectura es tolerante a valores raros guardados en disco: si algo
 * no encaja con el enum se cae al valor por defecto en lugar de romper el arranque.
 */
object PreferenciasTema {

    private const val FICHERO = "serena_ui"
    private const val CLAVE_MODO = "modo_tema"
    private const val CLAVE_PALETA = "paleta"

    fun modo(contexto: Context): ModoTema {
        val guardado = contexto.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .getString(CLAVE_MODO, null)
        return ModoTema.entries.firstOrNull { it.name == guardado } ?: ModoTema.SISTEMA
    }

    fun fijarModo(contexto: Context, modo: ModoTema) {
        contexto.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit()
            .putString(CLAVE_MODO, modo.name)
            .apply()
    }

    fun paleta(contexto: Context): Paleta {
        val guardado = contexto.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .getString(CLAVE_PALETA, null)
        return Paleta.entries.firstOrNull { it.name == guardado } ?: Paleta.porDefecto
    }

    fun fijarPaleta(contexto: Context, paleta: Paleta) {
        contexto.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
            .edit()
            .putString(CLAVE_PALETA, paleta.name)
            .apply()
    }
}
