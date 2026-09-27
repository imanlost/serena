package com.imanlost.serena.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imanlost.serena.R
import com.imanlost.serena.audio.ControlSesion
import com.imanlost.serena.audio.EstadoBiblioteca
import com.imanlost.serena.audio.Pista
import com.imanlost.serena.datos.RepositorioSesiones
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Ajustes de Serena: tema, biblioteca y sonido.
 *
 * La carpeta de audios se elige aqui y en la pantalla de la biblioteca con el mismo
 * selector SAF; el estado de escaneo es compartido para que ambas vean lo mismo.
 */
@Composable
fun PantallaAjustes(
    modo: ModoTema,
    onModo: (ModoTema) -> Unit,
    paleta: Paleta,
    onPaleta: (Paleta) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val pistas by EstadoBiblioteca.pistas.collectAsState()
    val cargando by EstadoBiblioteca.cargando.collectAsState()
    // Modo efectivo con el que se pintan las muestras de paleta: el selector debe
    // ensenar el color real que tendra la app.
    val oscuro = when (modo) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }
    var carpeta by remember { mutableStateOf(ControlSesion.carpetaAudio(contexto)) }
    var nombreCarpeta by remember { mutableStateOf(ControlSesion.nombreCarpetaAudio(contexto)) }
    var volumen by remember { mutableFloatStateOf(ControlSesion.volumenFondo(contexto)) }
    var fondoPorDefecto by remember { mutableStateOf(ControlSesion.fondoPorDefecto(contexto)) }
    var segundosPreparacion by remember {
        mutableIntStateOf(ControlSesion.segundosPreparacion(contexto))
    }
    var segundosVuelta by remember { mutableIntStateOf(ControlSesion.segundosVuelta(contexto)) }
    var gongActivado by remember { mutableStateOf(ControlSesion.gongActivado(contexto)) }
    val alcance = rememberCoroutineScope()
    // Resultado de la ultima exportacion. Los textos se resuelven aqui porque el callback del
    // selector no es un contexto de Compose y no puede llamar a stringResource.
    var mensajeExportar by remember { mutableStateOf<String?>(null) }
    val textoExportado = stringResource(R.string.exportar_ok)
    val textoError = stringResource(R.string.exportar_error)
    val textoCancelado = stringResource(R.string.exportar_cancelado)
    val exportar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) {
            // El usuario ha cerrado el selector sin elegir destino: no se ha guardado nada.
            mensajeExportar = textoCancelado
        } else {
            alcance.launch {
                val sesiones = RepositorioSesiones.sesionesAhora(contexto)
                mensajeExportar = if (exportarCsv(contexto, uri, sesiones)) {
                    textoExportado
                } else {
                    textoError
                }
            }
        }
    }

    // Fondos disponibles para el ajuste de fondo por defecto, por nombre visible.
    val fondos = remember(pistas) {
        pistas.filter { it.esFondo }
            .sortedBy { it.titulo.lowercase() }
    }

    // Con carpeta guardada, se escanea al abrir para poder mostrar el numero de pistas.
    LaunchedEffect(carpeta) {
        if (carpeta != null) EstadoBiblioteca.escanear(contexto)
    }

    val selector = rememberSelectorCarpeta {
        carpeta = ControlSesion.carpetaAudio(contexto)
        nombreCarpeta = ControlSesion.nombreCarpetaAudio(contexto)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        SeccionTema(
            modo = modo,
            onModo = onModo,
            paleta = paleta,
            onPaleta = onPaleta,
            oscuro = oscuro,
        )
        SeccionBiblioteca(
            nombreCarpeta = nombreCarpeta,
            pistas = pistas.size,
            escaneando = cargando,
            onElegir = { selector.launch(null) },
            onEscanear = { alcance.launch { EstadoBiblioteca.escanear(contexto) } },
        )
        SeccionExportar(
            onExportar = {
                // Se limpia el aviso anterior: si el selector se cancela, no se hereda.
                mensajeExportar = null
                exportar.launch(nombreFicheroCsv())
            },
            mensaje = mensajeExportar,
        )
        SeccionSonido(
            volumen = volumen,
            onVolumen = { nuevo ->
                volumen = nuevo
                ControlSesion.fijarVolumen(contexto, nuevo)
            },
            segundosPreparacion = segundosPreparacion,
            onSegundosPreparacion = { segundos ->
                segundosPreparacion = segundos
                ControlSesion.fijarSegundosPreparacion(contexto, segundos)
            },
            segundosVuelta = segundosVuelta,
            onSegundosVuelta = { segundos ->
                segundosVuelta = segundos
                ControlSesion.fijarSegundosVuelta(contexto, segundos)
            },
            gongActivado = gongActivado,
            onGong = { activado ->
                gongActivado = activado
                ControlSesion.fijarGong(contexto, activado)
            },
            fondos = fondos,
            fondoPorDefecto = fondoPorDefecto,
            onFondoPorDefecto = { id ->
                fondoPorDefecto = id
                ControlSesion.fijarFondoPorDefecto(contexto, id)
            },
        )
    }
}

@Composable
private fun SeccionTema(
    modo: ModoTema,
    onModo: (ModoTema) -> Unit,
    paleta: Paleta,
    onPaleta: (Paleta) -> Unit,
    oscuro: Boolean,
) {
    Column {
        Text(
            text = stringResource(R.string.tema),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val opciones = listOf(
                ModoTema.SISTEMA to stringResource(R.string.tema_sistema),
                ModoTema.CLARO to stringResource(R.string.tema_claro),
                ModoTema.OSCURO to stringResource(R.string.tema_oscuro),
            )
            opciones.forEachIndexed { indice, (valor, etiqueta) ->
                SegmentedButton(
                    selected = modo == valor,
                    onClick = { onModo(valor) },
                    shape = SegmentedButtonDefaults.itemShape(index = indice, count = opciones.size),
                ) {
                    Text(etiqueta, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SelectorPaleta(paleta = paleta, onPaleta = onPaleta, oscuro = oscuro)
    }
}

/**
 * Selector de paleta: una fila desplazable con las cuatro familias. Cada opcion lleva el
 * nombre y un circulo con el `primary` literal de esa paleta (no el del tema activo, que
 * seria el mismo para todas) y la elegida queda marcada por el propio chip.
 */
@Composable
private fun SelectorPaleta(
    paleta: Paleta,
    onPaleta: (Paleta) -> Unit,
    oscuro: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.paleta),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Paleta.entries.forEach { opcion ->
                FilterChip(
                    selected = opcion == paleta,
                    onClick = { onPaleta(opcion) },
                    label = {
                        Text(
                            text = when (opcion) {
                                Paleta.SALVIA -> stringResource(R.string.paleta_salvia)
                                Paleta.LAVANDA -> stringResource(R.string.paleta_lavanda)
                                Paleta.AMBAR -> stringResource(R.string.paleta_ambar)
                                Paleta.OCEANO -> stringResource(R.string.paleta_oceano)
                            },
                            fontSize = 13.sp,
                        )
                    },
                    leadingIcon = {
                        // Circulo opaco con borde del outline del tema: se distingue la
                        // muestra tanto en fondos claros como oscuros.
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(opcion.primario(oscuro), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SeccionBiblioteca(
    nombreCarpeta: String?,
    pistas: Int,
    escaneando: Boolean,
    onElegir: () -> Unit,
    onEscanear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.biblioteca),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.carpeta_elegida),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = nombreCarpeta ?: stringResource(R.string.carpeta_ninguna),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Button(
            onClick = onElegir,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.elegir_carpeta))
        }
        Button(
            onClick = onEscanear,
            enabled = nombreCarpeta != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.volver_escanear))
        }
        Text(
            text = if (escaneando) {
                stringResource(R.string.escaneando)
            } else {
                stringResource(R.string.pistas_encontradas, pistas)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SeccionExportar(
    onExportar: () -> Unit,
    mensaje: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.historial),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.exportar_descripcion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onExportar,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.exportar_historial))
        }
        // Solo se ensena algo despues de pulsar: exito, fallo o nada guardado.
        if (mensaje != null) {
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SeccionSonido(
    volumen: Float,
    onVolumen: (Float) -> Unit,
    segundosPreparacion: Int,
    onSegundosPreparacion: (Int) -> Unit,
    segundosVuelta: Int,
    onSegundosVuelta: (Int) -> Unit,
    gongActivado: Boolean,
    onGong: (Boolean) -> Unit,
    fondos: List<Pista>,
    fondoPorDefecto: String?,
    onFondoPorDefecto: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.ajustes_sonido),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.volumen_fondo_ajuste),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${(volumen * 100f).roundToInt()} %",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = volumen,
            onValueChange = onVolumen,
            valueRange = ControlSesion.VOLUMEN_MIN..ControlSesion.VOLUMEN_MAX,
        )
        SelectorFondo(
            fondos = fondos,
            seleccion = fondoPorDefecto,
            onSeleccion = onFondoPorDefecto,
        )
        SelectorSegundos(
            etiqueta = stringResource(R.string.segundos_preparacion),
            opciones = ControlSesion.SEGUNDOS_PREPARACION,
            seleccion = segundosPreparacion,
            onSeleccion = onSegundosPreparacion,
        )
        SelectorSegundos(
            etiqueta = stringResource(R.string.segundos_vuelta),
            opciones = ControlSesion.SEGUNDOS_VUELTA,
            seleccion = segundosVuelta,
            onSeleccion = onSegundosVuelta,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.gong),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = gongActivado, onCheckedChange = onGong)
        }
    }
}

/**
 * Fila de valores cerrados. Se usan chips en lugar de un desplegable porque caben todos
 * los valores de un vistazo y se eligen de un toque, sin abrir nada.
 */
@Composable
private fun SelectorSegundos(
    etiqueta: String,
    opciones: List<Int>,
    seleccion: Int,
    onSeleccion: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            opciones.forEach { segundos ->
                FilterChip(
                    selected = segundos == seleccion,
                    onClick = { onSeleccion(segundos) },
                    label = { Text(stringResource(R.string.segundos, segundos)) },
                )
            }
        }
    }
}

/**
 * Fondo que se propone la primera vez, mientras el usuario no haya elegido ninguno en una
 * meditacion. Chips en fila desplazable: se ve la eleccion de un vistazo y no hay que
 * abrir nada.
 */
@Composable
private fun SelectorFondo(
    fondos: List<Pista>,
    seleccion: String?,
    onSeleccion: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.fondo_por_defecto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.fondo_por_defecto_descripcion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = seleccion == null,
                onClick = { onSeleccion(null) },
                label = { Text(stringResource(R.string.fondo_sin_fondo)) },
            )
            fondos.forEach { fondo ->
                FilterChip(
                    selected = seleccion == fondo.id,
                    onClick = { onSeleccion(fondo.id) },
                    label = { Text(fondo.titulo) },
                )
            }
        }
    }
}
