@file:OptIn(ExperimentalMaterial3Api::class)

package com.imanlost.serena.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imanlost.serena.R
import com.imanlost.serena.audio.Biblioteca
import com.imanlost.serena.audio.EstadoBiblioteca
import com.imanlost.serena.datos.RepositorioSesiones
import com.imanlost.serena.datos.Sesion
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Diario de sesiones de la pestana Seguimiento: lista agrupada por dia, detalle con
 * nota y minutos editables, y alta manual de una practica que la app no registro.
 *
 * La lista se pinta como items de la `LazyColumn` que la contiene (no dentro de una
 * tarjeta con un `Column` interno): el diario crece sin limite y asi no se mide una
 * altura infinita en tiempo de ejecucion.
 */

/** Zona del dispositivo: dia natural y hora locales, que es como se lee el diario. */
private val ZONA: ZoneId = ZoneId.systemDefault()

/** Cabecera de grupo del dia, sin anio: "lunes, 22 de septiembre". */
private val FORMATO_DIA: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))

/** Fecha completa con anio, para el detalle y el alta manual. */
private val FORMATO_FECHA_LARGA: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))

/** Fecha y hora completas del detalle de una sesion. */
private val FORMATO_FECHA_HORA: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy · HH:mm", Locale("es", "ES"))

/** Hora de cada fila, en formato de 24 horas. */
private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Un dia natural del diario con sus sesiones, ya ordenadas de mas reciente a mas antigua. */
internal data class GrupoDia(val dia: LocalDate, val sesiones: List<Sesion>)

/** Minutos que se muestran de una sesion; redondeados, igual que en el resumen. */
internal fun minutosDeSesion(sesion: Sesion): Int =
    (sesion.duracionMs / 60_000.0).roundToInt()

/**
 * Agrupa por dia natural local conservando el orden de entrada (que llega de la base de
 * datos de mas reciente a mas antigua), y ordena los grupos por fecha descendente.
 */
internal fun agruparPorDia(
    sesiones: List<Sesion>,
    zona: ZoneId = ZONA,
): List<GrupoDia> = sesiones
    .groupBy { Instant.ofEpochMilli(it.inicioMs).atZone(zona).toLocalDate() }
    .entries
    .sortedByDescending { it.key }
    .map { GrupoDia(it.key, it.value) }

/** Seccion del diario: cabecera, grupos por dia y filas. Va dentro de la `LazyColumn`. */
fun LazyListScope.diarioSesiones(
    sesiones: List<Sesion>,
    onAbrir: (Sesion) -> Unit,
    onAnadir: () -> Unit,
) {
    item(key = "diario-cabecera") { CabeceraDiario(onAnadir = onAnadir) }
    if (sesiones.isEmpty()) {
        item(key = "diario-vacio") {
            Text(
                text = stringResource(R.string.diario_vacio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        return
    }
    agruparPorDia(sesiones).forEach { grupo ->
        item(key = "diario-dia-${grupo.dia}") { CabeceraDia(dia = grupo.dia) }
        items(items = grupo.sesiones, key = { "diario-sesion-${it.id}" }) { sesion ->
            FilaSesion(sesion = sesion, onAbrir = { onAbrir(sesion) })
        }
    }
}

@Composable
private fun CabeceraDiario(onAnadir: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.diario),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        // Boton discreto: el alta manual es la excepcion, no el flujo normal.
        TextButton(onClick = onAnadir) {
            Text(stringResource(R.string.anadir_sesion))
        }
    }
}

@Composable
private fun CabeceraDia(dia: LocalDate) {
    val hoy = LocalDate.now(ZONA)
    val etiqueta = when (dia) {
        hoy -> stringResource(R.string.hoy)
        hoy.minusDays(1) -> stringResource(R.string.ayer)
        else -> FORMATO_DIA.format(dia)
    }
    Text(
        text = etiqueta,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun FilaSesion(sesion: Sesion, onAbrir: () -> Unit) {
    val hora = Instant.ofEpochMilli(sesion.inicioMs).atZone(ZONA).toLocalTime()
    // Dos marcas discretas, en su propia linea: que la sesion no se completo y que lleva nota.
    val incompleta = if (!sesion.completada) stringResource(R.string.incompleta) else null
    val conNota = if (sesion.nota != null) stringResource(R.string.con_nota) else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAbrir)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = FORMATO_HORA.format(hora),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = sesion.pista,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (incompleta != null || conNota != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    incompleta?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    conNota?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.minutos_corto, minutosDeSesion(sesion)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Detalle de una sesion: minutos y nota editables, y borrado con confirmacion. */
@Composable
fun DialogoDetalleSesion(sesion: Sesion, onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    // El estado se reinicia al abrir otra sesion (la clave es su id).
    var minutosTexto by rememberSaveable(sesion.id) {
        mutableStateOf(minutosDeSesion(sesion).coerceAtLeast(1).toString())
    }
    var notaTexto by rememberSaveable(sesion.id) { mutableStateOf(sesion.nota.orEmpty()) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    val minutos = minutosTexto.toIntOrNull()
    val minutosOk = minutos != null && minutos >= 1

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(sesion.pista) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = FORMATO_FECHA_HORA.format(Instant.ofEpochMilli(sesion.inicioMs).atZone(ZONA)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = minutosTexto,
                    onValueChange = { nuevo -> minutosTexto = nuevo.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.minutos)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = minutosTexto.isNotEmpty() && !minutosOk,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notaTexto,
                    onValueChange = { notaTexto = it },
                    label = { Text(stringResource(R.string.nota)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { confirmarBorrado = true }) {
                    Text(
                        text = stringResource(R.string.borrar),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = minutosOk,
                onClick = {
                    val valor = minutos
                    if (valor != null && valor >= 1) {
                        alcance.launch {
                            RepositorioSesiones.anotar(contexto, sesion.id, notaTexto)
                            RepositorioSesiones.cambiarDuracion(contexto, sesion.id, valor * 60_000L)
                            onCerrar()
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(R.string.cancelar))
            }
        },
    )

    // Borrar nunca es un solo toque: se confirma en un dialogo aparte.
    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            text = { Text(stringResource(R.string.borrar_sesion_pregunta)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        alcance.launch {
                            RepositorioSesiones.borrar(contexto, sesion.id)
                            onCerrar()
                        }
                    },
                ) {
                    Text(stringResource(R.string.borrar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        )
    }
}

/**
 * Alta manual de una sesion: fecha, hora, meditacion y minutos.
 *
 * No pasa por `ControlSesion` a proposito: aqui no hay que reproducir nada, solo escribir
 * la fila con `RepositorioSesiones.registrar`.
 */
@Composable
fun DialogoAltaSesion(onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val pistas by EstadoBiblioteca.pistas.collectAsState()
    // El catalogo puede no haberse escaneado nunca: se pide al abrir para poder ofrecerlo.
    LaunchedEffect(Unit) { EstadoBiblioteca.escanear(contexto) }

    // Por defecto, hoy y ahora. Se guardan los campos sueltos para que sobrevivan a un giro.
    val ahora = remember { LocalDateTime.now() }
    var diaEpoch by rememberSaveable { mutableStateOf(ahora.toLocalDate().toEpochDay()) }
    var hora by rememberSaveable { mutableStateOf(ahora.hour) }
    var minuto by rememberSaveable { mutableStateOf(ahora.minute) }
    var titulo by rememberSaveable { mutableStateOf("") }
    var minutosTexto by rememberSaveable { mutableStateOf("10") }
    var menuAbierto by remember { mutableStateOf(false) }
    var mostrarFecha by remember { mutableStateOf(false) }
    var mostrarHora by remember { mutableStateOf(false) }

    val minutos = minutosTexto.toIntOrNull()
    val minutosOk = minutos != null && minutos >= 1
    val tituloLimpio = titulo.trim()
    val puedeGuardar = minutosOk && tituloLimpio.isNotEmpty()
    val dia = LocalDate.ofEpochDay(diaEpoch)
    val pistasOrdenadas = remember(pistas) { pistas.sortedWith(compareBy(Biblioteca.NATURAL) { it.titulo }) }
    val sugerencias = remember(pistasOrdenadas, titulo) {
        if (titulo.isBlank()) {
            pistasOrdenadas
        } else {
            pistasOrdenadas.filter { it.titulo.contains(titulo, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.anadir_sesion)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { mostrarFecha = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.fecha_etiqueta, FORMATO_FECHA_LARGA.format(dia)),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = { mostrarHora = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.hora_etiqueta,
                                FORMATO_HORA.format(LocalTime.of(hora, minuto)),
                            ),
                        )
                    }
                }
                // Campo escribible con sugerencias del catalogo: vale una pista de la
                // biblioteca o un nombre libre (una meditacion hecha sin el movil).
                ExposedDropdownMenuBox(
                    expanded = menuAbierto,
                    onExpandedChange = { menuAbierto = it },
                ) {
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = {
                            titulo = it
                            menuAbierto = true
                        },
                        label = { Text(stringResource(R.string.meditacion)) },
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAbierto)
                        },
                        modifier = Modifier
                            // Campo editable: el ancla adecuada es la de texto libre.
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = menuAbierto,
                        onDismissRequest = { menuAbierto = false },
                    ) {
                        sugerencias.forEach { pista ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = pista.titulo,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                onClick = {
                                    titulo = pista.titulo
                                    menuAbierto = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = minutosTexto,
                    onValueChange = { nuevo -> minutosTexto = nuevo.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.minutos)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = minutosTexto.isNotEmpty() && !minutosOk,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = puedeGuardar,
                onClick = {
                    val valor = minutos
                    if (valor != null && valor >= 1) {
                        val duracionMs = valor * 60_000L
                        val inicioMs = dia.atTime(hora, minuto)
                            .atZone(ZONA)
                            .toInstant()
                            .toEpochMilli()
                        // Si el nombre coincide con el catalogo se guarda tambien su grupo.
                        val elegida = pistasOrdenadas.firstOrNull { it.titulo == tituloLimpio }
                        alcance.launch {
                            RepositorioSesiones.registrar(
                                contexto,
                                Sesion(
                                    inicioMs = inicioMs,
                                    finMs = inicioMs + duracionMs,
                                    duracionMs = duracionMs,
                                    pista = tituloLimpio,
                                    grupo = elegida?.carpeta.orEmpty(),
                                    completada = true,
                                    nota = null,
                                    idSalud = null,
                                ),
                            )
                            onCerrar()
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(R.string.cancelar))
            }
        },
    )

    if (mostrarFecha) {
        // `selectedDateMillis` viene en UTC: se lee como fecha UTC y se combina con la
        // hora local al guardar; asi el dia elegido no se desplaza por la zona horaria.
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = dia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarFecha = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estadoFecha.selectedDateMillis?.let { ms ->
                            diaEpoch = Instant.ofEpochMilli(ms)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toEpochDay()
                        }
                        mostrarFecha = false
                    },
                ) {
                    Text(stringResource(R.string.aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarFecha = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        ) {
            DatePicker(state = estadoFecha)
        }
    }

    if (mostrarHora) {
        // En 1.3.1 no hay `TimePickerDialog`: se envuelve el `TimePicker` en el AlertDialog.
        val estadoHora = rememberTimePickerState(
            initialHour = hora,
            initialMinute = minuto,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { mostrarHora = false },
            text = { TimePicker(state = estadoHora) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hora = estadoHora.hour
                        minuto = estadoHora.minute
                        mostrarHora = false
                    },
                ) {
                    Text(stringResource(R.string.aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarHora = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        )
    }
}
