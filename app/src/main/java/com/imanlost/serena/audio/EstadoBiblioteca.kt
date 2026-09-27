package com.imanlost.serena.audio

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Estado de la biblioteca compartido entre la pantalla de audios y Ajustes.
 *
 * Asi "Volver a escanear" en Ajustes se refleja tambien en la lista, y el numero de
 * pistas que muestra Ajustes es el mismo que ve el usuario en la biblioteca.
 */
object EstadoBiblioteca {

    private val _pistas = MutableStateFlow<List<Pista>>(emptyList())
    val pistas: StateFlow<List<Pista>> = _pistas.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    /**
     * Relee la carpeta elegida. Sin carpeta guardada deja la lista vacia; si el permiso
     * persistente se ha perdido, el escaneo falla y tambien se queda vacia (sin romper).
     */
    suspend fun escanear(contexto: Context) {
        _cargando.value = true
        try {
            val guardada = ControlSesion.carpetaAudio(contexto)
            _pistas.value = if (guardada == null) {
                emptyList()
            } else {
                // El escaneo toca disco: fuera del hilo principal para no congelar la lista.
                withContext(Dispatchers.IO) {
                    runCatching { Biblioteca.escanear(contexto, Uri.parse(guardada)) }
                        .getOrDefault(emptyList())
                }
            }
        } finally {
            _cargando.value = false
        }
    }
}
