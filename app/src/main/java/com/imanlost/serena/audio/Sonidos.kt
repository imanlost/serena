package com.imanlost.serena.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sintesis de los avisos sonoros de la sesion.
 *
 * Se genera todo en memoria en lugar de empaquetar ficheros: el APK no lleva contenido
 * de audio y los avisos no dependen de que exista ninguna pista en la biblioteca.
 * `campana()` es el tono agudo de siempre; `gong()` es el aviso grave del ritual de
 * apertura y cierre.
 */
object Sonidos {

    /** Cuanto dura realmente la campana, para esperarla antes de apagar el servicio. */
    const val DURACION_CAMPANA_MS = 2400L

    /**
     * Cola real del gong. El servicio la suma a los segundos de preparacion (y espera a
     * que termine antes de dar la sesion por cerrada).
     */
    const val DURACION_GONG_MS = 6000L

    private const val FRECUENCIA_HZ = 44100

    // Campana: tono agudo con dos armonicos y decaimiento exponencial, sin ataque duro.
    private const val CAMPANA_FUNDAMENTAL_HZ = 660.0
    private const val CAMPANA_DECAIMIENTO = 3.2

    // Gong: fundamental grave de 110 Hz. Los parciales NO son multiplos exactos de la
    // fundamental; esa inarmonicidad es la que da el timbre metalico (un multiplo exacto
    // suena a nota afinada, no a gong).
    private const val GONG_FUNDAMENTAL_HZ = 110.0
    private const val GONG_ATAQUE_MS = 2.5
    private const val GONG_CIERRE_MS = 700L
    // Margen amplio: el gong es un aviso, no un sobresalto, y la voz entra despues.
    private const val GANANCIA_GONG = 0.45

    /**
     * Parcial del gong: factor sobre la fundamental, amplitud y velocidad de caida.
     * Los agudos caen antes (decaimento mayor); el fundamental aguanta la cola entera.
     */
    private data class ParcialGong(
        val factor: Double,
        val amplitud: Double,
        val decaimento: Double,
    )

    private val PARCIALES_GONG = listOf(
        ParcialGong(1.0, 0.50, 0.55),
        ParcialGong(2.37, 0.24, 0.75),
        ParcialGong(3.61, 0.13, 0.95),
        ParcialGong(5.12, 0.07, 1.25),
        ParcialGong(6.94, 0.04, 1.60),
        ParcialGong(8.6, 0.02, 2.00),
    )

    // Hilo propio y desechable: sintetizar 6 s de gong no debe tocar el hilo principal ni
    // el del servicio. Es el mismo patron que usaba la campana.
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // La sintesis se cachea porque el gong puede sonar dos veces por sesion (apertura y
    // cierre) y recalcularlo cada vez es trabajo tirado.
    private val muestrasCampana: ShortArray by lazy { sintetizarCampana() }
    private val muestrasGong: ShortArray by lazy { sintetizarGong() }

    /** Campana aguda de siempre, para avisos cortos. */
    fun campana() {
        alcance.launch {
            runCatching { reproducir(muestrasCampana, DURACION_CAMPANA_MS) }
        }
    }

    /** Gong grave del ritual: marca la apertura y el cierre de la practica. */
    fun gong() {
        alcance.launch {
            runCatching { reproducir(muestrasGong, DURACION_GONG_MS) }
        }
    }

    private suspend fun reproducir(pcm: ShortArray, duracionMs: Long) {
        val bytes = aBytes(pcm)

        val atributos = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val formato = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(FRECUENCIA_HZ)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val pista = AudioTrack.Builder()
            .setAudioAttributes(atributos)
            .setAudioFormat(formato)
            .setBufferSizeInBytes(bytes.size)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            pista.play()
            pista.write(bytes, 0, bytes.size)
            // write() vuelve al encolar los datos: hay que esperar a que suene el final.
            delay(duracionMs + 200L)
        } finally {
            runCatching { pista.stop() }
            runCatching { pista.release() }
        }
    }

    private fun sintetizarCampana(): ShortArray {
        val muestras = (FRECUENCIA_HZ * DURACION_CAMPANA_MS / 1000L).toInt()
        val pcm = ShortArray(muestras)
        for (i in 0 until muestras) {
            val t = i.toDouble() / FRECUENCIA_HZ
            val decaimiento = exp(-CAMPANA_DECAIMIENTO * t)
            val onda = sin(2.0 * PI * CAMPANA_FUNDAMENTAL_HZ * t) * 0.72 +
                sin(2.0 * PI * CAMPANA_FUNDAMENTAL_HZ * 2.01 * t) * 0.20 +
                sin(2.0 * PI * CAMPANA_FUNDAMENTAL_HZ * 3.0 * t) * 0.08
            pcm[i] = aMuestra(onda * decaimiento * 0.55)
        }
        return pcm
    }

    private fun sintetizarGong(): ShortArray {
        val muestras = (FRECUENCIA_HZ * DURACION_GONG_MS / 1000L).toInt()
        val pcm = ShortArray(muestras)
        val ataqueSeg = GONG_ATAQUE_MS / 1000.0
        val cierreSeg = GONG_CIERRE_MS / 1000.0
        val duracionSeg = DURACION_GONG_MS / 1000.0
        for (i in 0 until muestras) {
            val t = i.toDouble() / FRECUENCIA_HZ
            var onda = 0.0
            for (parcial in PARCIALES_GONG) {
                onda += sin(2.0 * PI * GONG_FUNDAMENTAL_HZ * parcial.factor * t) *
                    parcial.amplitud * exp(-parcial.decaimento * t)
            }
            // Ataque de 2-3 ms: sin el, el arranque desde cero chasquea.
            val ataque = (t / ataqueSeg).coerceAtMost(1.0)
            // Remate final suave: la cola ya es debil, pero se lleva a cero sin escalon.
            val cierre = if (t > duracionSeg - cierreSeg) {
                ((duracionSeg - t) / cierreSeg).coerceIn(0.0, 1.0)
            } else {
                1.0
            }
            pcm[i] = aMuestra(onda * ataque * cierre * GANANCIA_GONG)
        }
        return pcm
    }

    /** Escala una onda en [-1, 1] a PCM de 16 bits sin salirse de rango. */
    private fun aMuestra(valor: Double): Short =
        (valor * Short.MAX_VALUE).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()

    private fun aBytes(pcm: ShortArray): ByteArray {
        val bytes = ByteArray(pcm.size * 2)
        for (i in pcm.indices) {
            val valor = pcm[i].toInt()
            bytes[i * 2] = (valor and 0xFF).toByte()
            bytes[i * 2 + 1] = ((valor shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
