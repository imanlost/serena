package com.imanlost.serena.audio

import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.imanlost.serena.MainActivity
import com.imanlost.serena.datos.RepositorioSesiones
import com.imanlost.serena.datos.Sesion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Servicio en primer plano que mantiene el audio con la pantalla apagada.
 *
 * Se apoya en `MediaSessionService` de media3: el servicio pone la notificacion de
 * reproduccion y expone la sesion de medios. La logica del temporizador y de los
 * desvanecidos vive aqui, no en la Activity, porque la Activity puede morir y la
 * practica debe poder continuar.
 */
class ServicioSesion : MediaSessionService() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var reproductor: Reproductor? = null
    private var sesion: MediaSession? = null
    private var trabajoCuentaAtras: Job? = null
    private var trabajoPreparacion: Job? = null
    private var estado = EstadoSesion()
    private var finalizando = false

    // Datos del registro que solo conoce el servicio: cuando empezo la sesion (reloj de
    // pared) y el grupo de la pista, que la interfaz manda con la orden de arranque.
    private var inicioMs = 0L
    private var grupo = ""

    // El usuario tambien puede pausar desde la notificacion o los auriculares: hay que
    // reflejarlo en el temporizador para que no siga corriendo por detras.
    private val escuchaVoz = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // Durante la preparacion la voz ni suena ni se pausa: no hay nada que reflejar.
            if (finalizando || !estado.activa || estado.preparando) return
            if (!playWhenReady && !estado.pausada) {
                trabajoCuentaAtras?.cancel()
                reproductor?.pausar()
                publicar(estado.copy(pausada = true))
            } else if (playWhenReady && estado.pausada) {
                reproductor?.reanudar()
                val restante = estado.restanteMs
                publicar(estado.copy(pausada = false))
                lanzarCuentaAtras(restante)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val reproductor = Reproductor(this, alcance)
        this.reproductor = reproductor
        reproductor.playerVoz.addListener(escuchaVoz)

        // Tocar la notificacion abre la app, como en cualquier reproductor.
        val abrirApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        sesion = MediaSession.Builder(this, reproductor.playerVoz)
            .setSessionActivity(abrirApp)
            .build()
        // SIN ESTO NO HAY NOTIFICACION. La sesion solo se registra en el servicio cuando
        // un controlador se conecta (onGetSession), y esta app manda las ordenes por
        // Intent, asi que nunca se conectaba nadie: media3 no publicaba la notificacion
        // ni ponia el servicio en primer plano (startForegroundCount: 0 en el movil).
        sesion?.let { addSession(it) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = sesion

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // @CallSuper: MediaSessionService tambien procesa aqui los eventos de boton de medios.
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACCION_INICIAR -> iniciar(intent)
            ACCION_PAUSAR -> pausar()
            ACCION_REANUDAR -> reanudar()
            ACCION_TERMINAR -> finalizar()
            ACCION_VOLUMEN -> reproductor?.fijarVolumenObjetivo(
                intent.getFloatExtra(EXTRA_VOLUMEN, ControlSesion.VOLUMEN_POR_DEFECTO),
            )
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // Si el sistema se lleva por delante el servicio a mitad de sesion (por ejemplo
        // al descartar la notificacion), la interfaz no puede quedarse en "Terminando…".
        if (estado.activa) publicar(EstadoSesion())
        trabajoCuentaAtras?.cancel()
        trabajoPreparacion?.cancel()
        alcance.cancel()
        reproductor?.playerVoz?.removeListener(escuchaVoz)
        sesion?.release()
        sesion = null
        reproductor?.liberar()
        reproductor = null
        super.onDestroy()
    }

    private fun iniciar(intent: Intent) {
        val rutaVoz = intent.getStringExtra(EXTRA_VOZ) ?: return
        val titulo = intent.getStringExtra(EXTRA_TITULO).orEmpty()
        val rutaFondo = intent.getStringExtra(EXTRA_FONDO)
        // Debe leerse con el MISMO tipo con el que se envia (Int en ControlSesion.iniciar).
        // Con un desajuste de tipo, Android no falla: devuelve este valor por defecto.
        val minutos = intent.getIntExtra(EXTRA_MINUTOS, 5).coerceIn(1, 60)
        val volumen = intent.getFloatExtra(EXTRA_VOLUMEN, ControlSesion.VOLUMEN_POR_DEFECTO)
        val totalMs = minutos * 60_000L

        trabajoCuentaAtras?.cancel()
        trabajoPreparacion?.cancel()
        finalizando = false
        // El reloj del registro se fija al acabar la preparacion: lo que se espera antes
        // de empezar no es practica y no debe contar.
        inicioMs = 0L
        grupo = intent.getStringExtra(EXTRA_GRUPO).orEmpty()

        val gongActivado = ControlSesion.gongActivado(this)
        val preparacionMs = ControlSesion.preparacionTotalMs(this)
        publicar(
            EstadoSesion(
                activa = true,
                totalMs = totalMs,
                restanteMs = totalMs,
                titulo = titulo,
                preparando = preparacionMs > 0L,
                preparacionRestanteMs = preparacionMs,
                preparacionTotalMs = preparacionMs,
            ),
        )

        // Se dejan los dos reproductores cargados para que no haya espera al terminar la
        // preparacion, pero no suenan hasta que esta acabe.
        reproductor?.cargarVoz(rutaVoz, titulo)
        reproductor?.cargarFondo(rutaFondo)
        reproductor?.fijarVolumenObjetivo(volumen)

        if (gongActivado) Sonidos.gong()
        if (preparacionMs > 0L) {
            lanzarPreparacion(preparacionMs, totalMs)
        } else {
            empezarPractica(totalMs)
        }
    }

    /**
     * Cuenta atras de preparacion. Incluye la cola del gong (ControlSesion ya la suma),
     * para que la voz no entre encima del aviso.
     */
    private fun lanzarPreparacion(preparacionMs: Long, totalMs: Long) {
        val fin = SystemClock.elapsedRealtime() + preparacionMs
        trabajoPreparacion = alcance.launch {
            while (isActive) {
                val quedan = (fin - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                publicar(estado.copy(preparacionRestanteMs = quedan))
                if (quedan <= 0L) {
                    empezarPractica(totalMs)
                    break
                }
                delay(200L)
            }
        }
    }

    /** Arranca la practica de verdad: voz, fondo y temporizador, a la vez. */
    private fun empezarPractica(totalMs: Long) {
        inicioMs = System.currentTimeMillis()
        reproductor?.empezar()
        publicar(
            estado.copy(
                preparando = false,
                preparacionRestanteMs = 0L,
                restanteMs = totalMs,
                pausada = false,
            ),
        )
        lanzarCuentaAtras(totalMs)
    }

    private fun lanzarCuentaAtras(restanteMs: Long) {
        trabajoCuentaAtras?.cancel()
        val fin = SystemClock.elapsedRealtime() + restanteMs
        trabajoCuentaAtras = alcance.launch {
            while (isActive) {
                val quedan = (fin - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                publicar(estado.copy(restanteMs = quedan, pausada = false))
                if (quedan <= 0L) {
                    finalizar()
                    break
                }
                delay(500L)
            }
        }
    }

    private fun pausar() {
        if (!estado.activa || estado.pausada || estado.preparando || finalizando) return
        trabajoCuentaAtras?.cancel()
        reproductor?.pausar()
        publicar(estado.copy(pausada = true))
    }

    private fun reanudar() {
        if (!estado.activa || !estado.pausada || estado.preparando || finalizando) return
        reproductor?.reanudar()
        val restante = estado.restanteMs
        publicar(estado.copy(pausada = false))
        lanzarCuentaAtras(restante)
    }

    private fun finalizar() {
        if (!estado.activa || finalizando) return
        finalizando = true

        // Cortar durante la preparacion es cortar antes de practicar: no hay nada que
        // registrar y un gong de cierre no tendria sentido (no hubo sesion).
        if (estado.preparando) {
            trabajoPreparacion?.cancel()
            reproductor?.parar()
            publicar(EstadoSesion())
            finalizando = false
            stopSelf()
            return
        }

        trabajoCuentaAtras?.cancel()

        // El registro se construye AQUI, antes de tocar el estado y de que empiece el
        // desvanecido: `duracionMs` es el tiempo realmente escuchado (total menos lo que
        // quedaba). Si el usuario corta antes, se guarda igual con `completada = false`.
        val sesion = Sesion(
            inicioMs = inicioMs,
            finMs = System.currentTimeMillis(),
            duracionMs = (estado.totalMs - estado.restanteMs).coerceIn(0L, estado.totalMs),
            pista = estado.titulo,
            grupo = grupo,
            completada = estado.restanteMs <= 0L,
        )

        publicar(estado.copy(pausada = true, finalizando = true))
        alcance.launch {
            // La base de datos nunca se escribe en el hilo principal. Si fallara, la
            // sesion se pierde pero el audio y el cierre no se rompen.
            withContext(Dispatchers.IO) {
                runCatching { RepositorioSesiones.registrar(applicationContext, sesion) }
            }

            val gongActivado = ControlSesion.gongActivado(applicationContext)
            val cierreMs = ControlSesion.segundosVuelta(applicationContext) * 1000L

            // El gong marca el final de la practica; el fondo se apaga por debajo a lo
            // largo de la pausa de vuelta. La voz se calla ya: volver a la realidad es
            // quedarse solo con el fondo.
            if (gongActivado) Sonidos.gong()
            val finGong = SystemClock.elapsedRealtime() +
                if (gongActivado) Sonidos.DURACION_GONG_MS else 0L
            reproductor?.playerVoz?.pause()
            if (cierreMs > 0L) {
                reproductor?.desvanecerFondo(cierreMs)
            } else {
                // Pausa de cero: se para en el acto, sin un desvanecido de medio segundo.
                reproductor?.parar()
            }

            // El servicio no se apaga hasta que callen el gong y el fondo.
            val espera = finGong - SystemClock.elapsedRealtime()
            if (espera > 0L) delay(espera)

            reproductor?.parar()
            publicar(EstadoSesion())
            finalizando = false
            stopSelf()
        }
    }

    private fun publicar(nuevo: EstadoSesion) {
        estado = nuevo
        ControlSesion.publicar(nuevo)
    }

    companion object {
        const val ACCION_INICIAR = "com.imanlost.serena.accion.INICIAR"
        const val ACCION_PAUSAR = "com.imanlost.serena.accion.PAUSAR"
        const val ACCION_REANUDAR = "com.imanlost.serena.accion.REANUDAR"
        const val ACCION_TERMINAR = "com.imanlost.serena.accion.TERMINAR"
        const val ACCION_VOLUMEN = "com.imanlost.serena.accion.VOLUMEN"

        const val EXTRA_VOZ = "voz"
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_FONDO = "fondo"
        const val EXTRA_MINUTOS = "minutos"
        const val EXTRA_VOLUMEN = "volumen"
        const val EXTRA_GRUPO = "grupo"
    }
}
