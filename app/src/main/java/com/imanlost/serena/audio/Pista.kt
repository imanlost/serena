package com.imanlost.serena.audio

/**
 * Una pista de audio de la biblioteca local.
 *
 * `id` es la URI `content://` del fichero (unica y estable entre reinicios); `ruta`
 * guarda esa misma URI, que ExoPlayer reproduce directamente.
 *
 * `carpeta` es la RUTA RELATIVA de la carpeta que contiene el fichero dentro de la carpeta
 * elegida, con `/` como separador y vacia cuando el fichero esta suelto en la raiz. La
 * biblioteca se recorre a cualquier profundidad, asi que hay grupos como `guiadas`, `fondo`
 * o `curso/nivel 2`. Se guarda la ruta ENTERA y no solo el nombre de la carpeta para
 * que dos carpetas que se llamen igual en ramas distintas no acaben mezcladas en una lista.
 */
data class Pista(
    val id: String,
    val titulo: String,
    val ruta: String,
    val duracionMs: Long,
    val carpeta: String,
) {
    /**
     * Una pista de una carpeta `fondo` suena sola: no se le pregunta que fondo la acompane
     * y manda el dial, no la duracion del fichero. Se mira el ULTIMO tramo de la ruta para
     * que siga valiendo si algun dia los ruidos se guardan mas adentro.
     */
    val esFondo: Boolean
        get() = ultimoTramo(carpeta) == Biblioteca.GRUPO_FONDO
}

/** Ultimo tramo de una ruta relativa (`a/b/c` -> `c`); vacio si la pista esta en la raiz. */
private fun ultimoTramo(ruta: String): String = ruta.substringAfterLast('/')

/**
 * Duracion de la pista redondeada a minutos para precargar el dial.
 *
 * Si la duracion no se ha podido leer, se usa 5 minutos: un valor prudente. Antes se
 * usaban 10 y el usuario se encontraba un dial de 10 minutos al elegir una pista de 5
 * (paso cuando la duracion llego vacia desde el indice del sistema).
 *
 * Se redondea HACIA ARRIBA: una pista de 5:29 con el dial en 5 cortaria la voz a los
 * cinco minutos. Con el redondeo hacia arriba sobran unos segundos de fondo y la voz
 * nunca se corta.
 * El dial solo llega a 60 minutos: una pista mas larga se recorta a ese tope porque
 * el temporizador de la sesion se fija con las mismas unidades que el dial.
 */
fun Pista.minutosSesion(): Int {
    val minutos = if (duracionMs > 0L) Math.ceil(duracionMs / 60_000.0).toInt() else 5
    return minutos.coerceIn(1, 60)
}

/** Duracion en formato mm:ss para la lista de la biblioteca. */
fun formatearDuracion(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val segundos = ms / 1000L
    return "%02d:%02d".format(segundos / 60L, segundos % 60L)
}

/** Tiempo restante en formato mm:ss para la pantalla de sesion. */
fun formatearTiempo(ms: Long): String {
    val segundos = (ms / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(segundos / 60L, segundos % 60L)
}
