package com.imanlost.serena.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Los dos reproductores de una sesion: la voz y el fondo.
 *
 * Son dos ExoPlayer independientes porque tienen que sonar a la vez y con volumenes
 * distintos. El fondo va en bucle infinito (`REPEAT_MODE_ALL`) y su volumen entra y
 * sale con un desvanecido; la voz suena siempre al 100 %.
 *
 * El foco de audio lo pide SOLO la voz: si lo pidieran los dos, el segundo en pedirlo
 * dejaria al primero en pausa. El fondo acompaña lo que haga la voz.
 */
class Reproductor(contexto: Context, private val alcance: CoroutineScope) {

    val playerVoz: ExoPlayer = ExoPlayer.Builder(contexto.applicationContext).build().apply {
        volume = 1f
        setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
        setHandleAudioBecomingNoisy(true)
    }

    private val playerFondo: ExoPlayer = ExoPlayer.Builder(contexto.applicationContext).build().apply {
        volume = 0f
        repeatMode = Player.REPEAT_MODE_ALL
        setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ false)
    }

    private var volumenObjetivo = ControlSesion.VOLUMEN_POR_DEFECTO
    private var trabajoDesvanecido: Job? = null

    fun cargarVoz(ruta: String, titulo: String) {
        playerVoz.setMediaItem(mediaItem(ruta, titulo))
        playerVoz.prepare()
    }

    fun cargarFondo(ruta: String?) {
        if (ruta == null) {
            playerFondo.clearMediaItems()
            return
        }
        playerFondo.setMediaItem(mediaItem(ruta, null))
        playerFondo.prepare()
    }

    fun fijarVolumenObjetivo(volumen: Float) {
        volumenObjetivo = volumen.coerceIn(ControlSesion.VOLUMEN_MIN, ControlSesion.VOLUMEN_MAX)
        // Si ya esta sonando, se aplica con una transicion corta para no dar un salto.
        if (playerFondo.isPlaying) {
            trabajoDesvanecido?.cancel()
            trabajoDesvanecido = alcance.launch {
                animarVolumen(playerFondo.volume, volumenObjetivo, TRANSICION_CORTA_MS)
            }
        }
    }

    fun empezar() {
        playerVoz.play()
        if (playerFondo.mediaItemCount > 0) {
            playerFondo.volume = 0f
            playerFondo.play()
            trabajoDesvanecido?.cancel()
            trabajoDesvanecido = alcance.launch {
                animarVolumen(0f, volumenObjetivo, FADE_ENTRADA_MS)
            }
        }
    }

    fun pausar() {
        trabajoDesvanecido?.cancel()
        playerVoz.pause()
        playerFondo.pause()
    }

    fun reanudar() {
        playerVoz.play()
        if (playerFondo.mediaItemCount > 0) {
            playerFondo.play()
            // Si la pausa pillo el fondo a medio desvanecido, se recupera suavemente.
            if (playerFondo.volume < volumenObjetivo) {
                trabajoDesvanecido?.cancel()
                trabajoDesvanecido = alcance.launch {
                    animarVolumen(playerFondo.volume, volumenObjetivo, TRANSICION_CORTA_MS)
                }
            }
        }
    }

    /** Desvanecido de salida del fondo. Nunca se corta en seco. */
    suspend fun desvanecerFondo(duracionMs: Long) {
        trabajoDesvanecido?.cancel()
        animarVolumen(playerFondo.volume, 0f, duracionMs)
    }

    fun parar() {
        trabajoDesvanecido?.cancel()
        playerVoz.stop()
        playerFondo.stop()
        playerVoz.clearMediaItems()
        playerFondo.clearMediaItems()
    }

    fun liberar() {
        trabajoDesvanecido?.cancel()
        playerVoz.release()
        playerFondo.release()
    }

    private suspend fun animarVolumen(desde: Float, hasta: Float, duracionMs: Long) {
        val pasos = 25
        val espera = (duracionMs / pasos).coerceAtLeast(20L)
        for (paso in 0..pasos) {
            val fraccion = paso.toFloat() / pasos
            playerFondo.volume = (desde + (hasta - desde) * fraccion).coerceIn(0f, 1f)
            if (paso < pasos) delay(espera)
        }
        playerFondo.volume = hasta.coerceIn(0f, 1f)
    }

    private fun mediaItem(ruta: String, titulo: String?): MediaItem {
        val constructor = MediaItem.Builder().setUri(uriDePista(ruta))
        if (titulo != null) {
            constructor.setMediaMetadata(MediaMetadata.Builder().setTitle(titulo).build())
        }
        return constructor.build()
    }

    private companion object {
        const val FADE_ENTRADA_MS = 3000L
        const val TRANSICION_CORTA_MS = 400L
    }
}
