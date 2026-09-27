package com.imanlost.serena

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imanlost.serena.audio.ControlSesion
import com.imanlost.serena.audio.minutosSesion
import com.imanlost.serena.datos.RepositorioSesiones
import com.imanlost.serena.estadisticas.ResumenPractica
import com.imanlost.serena.ui.BarrasSemanales
import com.imanlost.serena.ui.CalendarioConstancia
import com.imanlost.serena.ui.DialMinutos
import com.imanlost.serena.ui.DialogoAltaSesion
import com.imanlost.serena.ui.DialogoDetalleSesion
import com.imanlost.serena.ui.ModoTema
import com.imanlost.serena.ui.Paleta
import com.imanlost.serena.ui.PantallaAjustes
import com.imanlost.serena.ui.PantallaBiblioteca
import com.imanlost.serena.ui.PantallaSesion
import com.imanlost.serena.ui.PreferenciasTema
import com.imanlost.serena.ui.SerenaTheme
import com.imanlost.serena.ui.TarjetaRacha
import com.imanlost.serena.ui.diarioSesiones
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // La preferencia es la fuente de verdad: sobrevive al cierre de la app, asi que el
        // modo y la paleta se leen antes de montar la interfaz. El estado de Compose solo
        // refleja lo elegido mientras la actividad esta viva.
        val contexto = this
        val modoInicial = PreferenciasTema.modo(contexto)
        val paletaInicial = PreferenciasTema.paleta(contexto)
        setContent {
            var modo by remember { mutableStateOf(modoInicial) }
            var paleta by remember { mutableStateOf(paletaInicial) }
            SerenaTheme(modo = modo, paleta = paleta) {
                AppSerena(
                    modo = modo,
                    onModo = {
                        modo = it
                        PreferenciasTema.fijarModo(contexto, it)
                    },
                    paleta = paleta,
                    onPaleta = {
                        paleta = it
                        PreferenciasTema.fijarPaleta(contexto, it)
                    },
                )
            }
        }
    }
}

@Composable
private fun AppSerena(
    modo: ModoTema,
    onModo: (ModoTema) -> Unit,
    paleta: Paleta,
    onPaleta: (Paleta) -> Unit,
) {
    val contexto = LocalContext.current
    var destino by rememberSaveable { mutableIntStateOf(0) }
    // Mientras hay sesion, ocupa toda la pantalla: fuera pestanas.
    var enSesion by rememberSaveable { mutableStateOf(false) }

    // En Android 13+ la notificacion de reproduccion necesita POST_NOTIFICATIONS: en el
    // movil del usuario no aparecia el reproductor al bajar la barra. En versiones
    // anteriores el sistema responde sin mostrar dialogo, asi que pedirlo aqui es inocuo.
    val pedirNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(enSesion) {
        if (enSesion) pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    if (enSesion) {
        PantallaSesion(onSalir = { enSesion = false })
        return
    }

    val titulos = listOf(
        stringResource(R.string.hoy),
        // Etiqueta corta a proposito: "Biblioteca" no cabe en un cuarto de pantalla y se
        // partia en dos lineas en la barra de pestanas.
        stringResource(R.string.audios),
        stringResource(R.string.progreso),
        stringResource(R.string.ajustes),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
        ) {
            TabRow(
                selectedTabIndex = destino,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                titulos.forEachIndexed { indice, titulo ->
                    val activa = destino == indice
                    Tab(
                        selected = activa,
                        onClick = { destino = indice },
                        text = {
                            // La pestana activa lleva el acento; las demas, gris: sin esa
                            // diferencia no se sabe donde estas.
                            Text(
                                text = titulo,
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (activa) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                    )
                }
            }

            when (destino) {
                0 -> PantallaHoy(onElegirPista = { destino = 1 })
                1 -> PantallaBiblioteca(onPista = { voz, fondo ->
                    // Regla acordada: en un fondo sin voz manda el dial; en una meditacion
                    // guiada manda la meditacion y el dial es el tiempo minimo. Asi nunca
                    // se corta una voz a medias y, si pediste mas tiempo, el fondo sigue
                    // sonando hasta cumplirlo.
                    val esFondo = voz.esFondo
                    val duracion = if (esFondo) {
                        ControlSesion.minutosElegidos
                    } else {
                        maxOf(ControlSesion.minutosElegidos, voz.minutosSesion())
                    }
                    // En un fondo no hay acompanamiento: suena solo, a volumen pleno.
                    ControlSesion.iniciar(contexto, voz, if (esFondo) null else fondo, duracion)
                    enSesion = true
                })
                2 -> PantallaProgreso()
                else -> PantallaAjustes(
                    modo = modo,
                    onModo = onModo,
                    paleta = paleta,
                    onPaleta = onPaleta,
                )
            }
        }
    }
}

@Composable
private fun PantallaHoy(onElegirPista: () -> Unit) {
    val contexto = LocalContext.current
    // La duracion elegida se recuerda entre sesiones: abrir y empezar son dos toques.
    var minutos by rememberSaveable { mutableIntStateOf(10) }
    // Consulta reactiva: al terminar una sesion, este resumen se recalcula solo.
    val resumen by remember(contexto) { RepositorioSesiones.resumen(contexto) }
        .collectAsState(initial = ResumenPractica())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DialMinutos(
            minutos = minutos,
            onMinutos = {
                minutos = it
                // Se recuerda para la biblioteca, que es quien decide la duracion de la
                // sesion: en los fondos manda este tiempo, en las guiadas es el minimo.
                ControlSesion.minutosElegidos = it
            },
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onElegirPista,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(R.string.empezar), fontSize = 17.sp)
        }
        Spacer(Modifier.height(18.dp))
        // El progreso, visible donde se toma la decision de practicar, ya con los datos
        // reales de la base de datos.
        Text(
            // Dos plurales en vez de una sola cadena: con una cadena salia "1 dias seguidos",
            // sin concordar. El texto de union sigue en strings.xml.
            text = stringResource(
                R.string.resumen_hoy,
                androidx.compose.ui.res.pluralStringResource(
                    R.plurals.resumen_hoy_dias,
                    resumen.rachaActual,
                    resumen.rachaActual,
                ),
                androidx.compose.ui.res.pluralStringResource(
                    R.plurals.resumen_hoy_minutos,
                    resumen.minutosTotales,
                    resumen.minutosTotales,
                ),
            ),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PantallaProgreso() {
    val contexto = LocalContext.current
    val resumen by remember(contexto) { RepositorioSesiones.resumen(contexto) }
        .collectAsState(initial = ResumenPractica())
    val sesiones by remember(contexto) { RepositorioSesiones.sesiones(contexto) }
        .collectAsState(initial = emptyList())
    // El detalle se guarda por id (no por objeto) para que sobreviva a un giro de pantalla
    // y siga apuntando a la misma sesion. El id 0 no existe: Room empieza en 1.
    var idDetalle by rememberSaveable { mutableStateOf(0L) }
    var altaAbierta by rememberSaveable { mutableStateOf(false) }

    // Lista perezosa: el diario crece sin limite y dentro de un Column con scroll daria
    // altura infinita. Los graficos siguen siendo un item cada uno, en el mismo orden.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!resumen.hayDatos) {
            // Sin practica registrada no se dibujan graficos vacios: solo el aviso.
            item(key = "aviso") {
                Text(
                    text = stringResource(R.string.sin_datos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            item(key = "racha") {
                TarjetaRacha(
                    rachaActual = resumen.rachaActual,
                    mejorRacha = resumen.mejorRacha,
                    diasTotales = resumen.diasTotales,
                )
            }
            item(key = "minutos") {
                DatoProgreso(
                    etiqueta = stringResource(R.string.minutos_totales),
                    valor = resumen.minutosTotales.toString(),
                    unidad = stringResource(R.string.minutos),
                )
            }
            item(key = "calendario") {
                CalendarioConstancia(
                    mes = YearMonth.now(),
                    dias = resumen.minutosPorDia,
                )
            }
            item(key = "semanas") { BarrasSemanales(semanas = resumen.semanas) }
        }
        diarioSesiones(
            sesiones = sesiones,
            onAbrir = { idDetalle = it.id },
            onAnadir = { altaAbierta = true },
        )
    }

    val sesionAbierta = sesiones.firstOrNull { it.id == idDetalle }
    if (sesionAbierta != null) {
        DialogoDetalleSesion(sesion = sesionAbierta, onCerrar = { idDetalle = 0L })
    }
    if (altaAbierta) {
        DialogoAltaSesion(onCerrar = { altaAbierta = false })
    }
}

@Composable
private fun DatoProgreso(etiqueta: String, valor: String, unidad: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = valor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = " " + unidad,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

