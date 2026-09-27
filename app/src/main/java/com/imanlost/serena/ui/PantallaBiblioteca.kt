package com.imanlost.serena.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imanlost.serena.R
import com.imanlost.serena.audio.Biblioteca
import com.imanlost.serena.audio.ControlSesion
import com.imanlost.serena.audio.EstadoBiblioteca
import com.imanlost.serena.audio.Pista
import com.imanlost.serena.audio.formatearDuracion
import com.imanlost.serena.datos.RepositorioSesiones

/**
 * Biblioteca local: lista las pistas de la carpeta que el usuario haya elegido.
 *
 * No se pide ningun permiso del sistema: solo la carpeta (SAF). Si aun no hay carpeta
 * elegida, o no tiene audios, se explica y se da un boton grande para elegirla.
 *
 * Al tocar una meditacion guiada se pregunta con que fondo acompanarla; la eleccion se
 * recuerda entre sesiones en `ControlSesion`. Las pistas de fondo y el caso sin fondos
 * disponibles arrancan directamente, sin dialogo.
 */
@Composable
fun PantallaBiblioteca(
    onPista: (Pista, Pista?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val pistas by EstadoBiblioteca.pistas.collectAsState()
    val cargando by EstadoBiblioteca.cargando.collectAsState()
    // Pistas que ya tienen una sesion con duracion real. Es un Flow de Room: al terminar
    // una sesion nueva la lista se reemite sola y la marca aparece sin forzar recargas.
    val escuchadas by remember(contexto) { RepositorioSesiones.pistasEscuchadas(contexto) }
        .collectAsState(initial = emptyList())
    // Conjunto ya preparado: preguntar por cada fila del catalogo queda en O(1).
    val yaEscuchadas = remember(escuchadas) { escuchadas.toSet() }
    var carpeta by remember { mutableStateOf(ControlSesion.carpetaAudio(contexto)) }
    // Pista guiada pendiente de elegir fondo. Se guarda el id (y no la Pista) para que el
    // dialogo sobreviva a un giro de pantalla con rememberSaveable.
    var idDialogo by rememberSaveable { mutableStateOf<String?>(null) }

    // Se relee al abrir la pantalla y cada vez que se elige otra carpeta.
    LaunchedEffect(carpeta) {
        EstadoBiblioteca.escanear(contexto)
    }

    val selector = rememberSelectorCarpeta { carpeta = ControlSesion.carpetaAudio(contexto) }

    // Los fondos, por nombre visible y en orden alfabetico: es el orden que pide el dialogo.
    val fondos = remember(pistas) {
        pistas.filter { it.esFondo }
            .sortedBy { it.titulo.lowercase() }
    }
    val pistaDialogo = pistas.firstOrNull { it.id == idDialogo }

    Column(modifier = modifier.fillMaxSize()) {
        when {
            carpeta == null -> MensajeCarpeta(
                texto = stringResource(R.string.biblioteca_sin_carpeta),
                onElegir = { selector.launch(null) },
            )
            cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            pistas.isEmpty() -> MensajeCarpeta(
                texto = stringResource(R.string.biblioteca_vacia),
                onElegir = { selector.launch(null) },
            )
            else -> ListaPistas(
                pistas = pistas,
                escuchadas = yaEscuchadas,
                hayFondos = fondos.isNotEmpty(),
                onAbrir = { pista ->
                    when {
                        // Un fondo suena solo: no se pregunta nada, como hasta ahora.
                        pista.esFondo -> onPista(pista, null)
                        // Sin fondos no hay nada que elegir: arranque directo y sin dialogo.
                        fondos.isEmpty() -> onPista(pista, null)
                        else -> idDialogo = pista.id
                    }
                },
                onDirecto = { pista ->
                    // Atajo: arranca ya con el fondo preseleccionado, sin abrir el dialogo.
                    onPista(pista, fondoPreseleccionado(contexto, fondos))
                },
            )
        }
    }

    // `let` deja `pendiente` como Pista no nula, lista para el lambda de confirmacion.
    pistaDialogo?.let { pendiente ->
        DialogoElegirFondo(
            fondos = fondos,
            seleccionInicial = fondoPreseleccionado(contexto, fondos),
            onCancelar = { idDialogo = null },
            onConfirmar = { fondo ->
                ControlSesion.recordarFondoElegido(contexto, fondo?.id)
                idDialogo = null
                onPista(pendiente, fondo)
            },
        )
    }
}

/**
 * Fondo que debe aparecer preseleccionado en el dialogo.
 *
 * Prioridad: el ultimo elegido (si el fondo sigue existiendo), y si no hay eleccion
 * guardada, el fondo por defecto de Ajustes. Si la eleccion guardada apunta a un fondo
 * que ya no esta en la biblioteca, tambien se cae al de Ajustes; y si no hay ninguno,
 * «Sin fondo».
 */
private fun fondoPreseleccionado(contexto: Context, fondos: List<Pista>): Pista? {
    val elegido = ControlSesion.fondoElegido(contexto)
    // Si el id guardado sigue en la lista, esa es la eleccion (incluye el caso de fondo).
    fondos.firstOrNull { it.id == elegido }?.let { return it }
    // "" es la marca de «Sin fondo»: el usuario lo eligio a proposito, no se toca.
    if (elegido != null && elegido.isEmpty()) return null
    // Sin eleccion guardada, o apuntando a un fondo que ya no existe: el de Ajustes.
    val porDefecto = ControlSesion.fondoPorDefecto(contexto)
    return fondos.firstOrNull { it.id == porDefecto }
}

@Composable
private fun ListaPistas(
    pistas: List<Pista>,
    escuchadas: Set<String>,
    hayFondos: Boolean,
    onAbrir: (Pista) -> Unit,
    onDirecto: (Pista) -> Unit,
) {
    val ramas = Biblioteca.jerarquia(pistas)
    // Carpetas desplegadas. La biblioteca del usuario tiene 210 pistas en 16 carpetas, y van
    // ordenadas por bloques: primero `Guiadas`, `Fondos` y `Curso`, y dentro de cada uno lo que
    // tenga (`2 - SOS`, `Basics 2`...). `rememberSaveable` para que un giro no las cierre.
    var desplegadas by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        ramas.forEach { rama ->
            ramasEnLista(
                rama = rama,
                nivel = 0,
                desplegadas = desplegadas,
                escuchadas = escuchadas,
                hayFondos = hayFondos,
                onAbrir = onAbrir,
                onDirecto = onDirecto,
                onDesplegar = { desplegadas = it },
            )
        }
    }
}

/**
 * Pinta una carpeta y, si esta desplegada, sus pistas y sus subcarpetas (que a su vez pueden estar
 * desplegadas). No se limita a dos niveles: la sangria crece con `nivel`.
 */
private fun LazyListScope.ramasEnLista(
    rama: Biblioteca.Rama,
    nivel: Int,
    desplegadas: List<String>,
    escuchadas: Set<String>,
    hayFondos: Boolean,
    onAbrir: (Pista) -> Unit,
    onDirecto: (Pista) -> Unit,
    onDesplegar: (ArrayList<String>) -> Unit,
) {
    val abierta = desplegadas.contains(rama.clave)
    item(key = "grupo-${rama.clave}") {
        CabeceraGrupo(
            etiqueta = etiquetaGrupo(rama.clave, rama.nombre),
            cuantas = rama.total,
            abierta = abierta,
            nivel = nivel,
            onAlternar = {
                onDesplegar(
                    ArrayList(desplegadas).apply {
                        if (abierta) remove(rama.clave) else add(rama.clave)
                    },
                )
            },
        )
    }
    if (!abierta) return
    items(rama.pistas, key = { it.id }) { pista ->
        // El atajo solo tiene sentido en las guiadas (en los fondos no se pregunta nada) y cuando
        // hay algun fondo entre el que elegir.
        val atajo = !pista.esFondo && hayFondos
        FilaPista(
            pista = pista,
            nivel = nivel,
            escuchada = pista.titulo in escuchadas,
            onAbrir = { onAbrir(pista) },
            onDirecto = if (atajo) { { onDirecto(pista) } } else null,
        )
    }
    rama.hijas.forEach { hija ->
        ramasEnLista(
            rama = hija,
            nivel = nivel + 1,
            desplegadas = desplegadas,
            escuchadas = escuchadas,
            hayFondos = hayFondos,
            onAbrir = onAbrir,
            onDirecto = onDirecto,
            onDesplegar = onDesplegar,
        )
    }
}

/** Sangria de cada nivel del arbol: las subcarpetas entran hacia dentro. */
private fun sangria(nivel: Int) = (24 + 16 * nivel).dp

/**
 * Cabecera de una carpeta: el bloque de primer nivel va en el color de acento y en negrita, para no
 * confundirlo con las pistas; los paquetes de dentro van en el color del texto y mas adentro. Dice
 * cuantas pistas cuelgan de la carpeta y la despliega o la pliega.
 */
@Composable
private fun CabeceraGrupo(
    etiqueta: String,
    cuantas: Int,
    abierta: Boolean,
    nivel: Int,
    onAlternar: () -> Unit,
) {
    val color = if (nivel == 0) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAlternar)
            .padding(start = sangria(nivel), end = 24.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (nivel == 0) FontWeight.Bold else FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.grupo_pistas, cuantas, cuantas),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
        Text(
            text = if (abierta) "▾" else "▸",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun FilaPista(
    pista: Pista,
    nivel: Int,
    escuchada: Boolean,
    onAbrir: () -> Unit,
    onDirecto: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAbrir)
            .padding(start = sangria(nivel), end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Marca de «ya escuchada»: un visto pequeno en color secundario. El ancho se reserva
        // siempre (con o sin visto) para que los titulos queden alineados en toda la lista y
        // la altura de la fila no cambie.
        Text(
            text = if (escuchada) "✓" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(18.dp),
        )
        Text(
            text = pista.titulo,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            // Dos lineas como tope: hay titulos de 90 caracteres y sin tope descuadran la lista.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp),
        )
        Text(
            text = formatearDuracion(pista.duracionMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Atajo visible solo en las guiadas con fondos: un toque y arranca con lo
        // preseleccionado, sin pasar por el dialogo.
        if (onDirecto != null) {
            TextButton(onClick = onDirecto) {
                Text(stringResource(R.string.empezar_ya))
            }
        }
    }
}

/** Mensaje de "sin carpeta" o "carpeta sin audios", con el boton grande de elegir. */
@Composable
private fun MensajeCarpeta(texto: String, onElegir: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onElegir,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(stringResource(R.string.elegir_carpeta), fontSize = 17.sp)
        }
    }
}

@Composable
private fun etiquetaGrupo(clave: String, nombre: String): String = when (clave) {
    "guiadas" -> stringResource(R.string.grupo_guiadas)
    Biblioteca.GRUPO_FONDO -> stringResource(R.string.grupo_fondos)
    "podcasts" -> stringResource(R.string.grupo_podcasts)
    // La raiz de la carpeta elegida: las pistas sueltas.
    "" -> stringResource(R.string.grupo_sueltas)
    // Los demas bloques y las subcarpetas se llaman como en el almacenamiento (`Curso`, `Nivel 1`).
    else -> nombre
}
