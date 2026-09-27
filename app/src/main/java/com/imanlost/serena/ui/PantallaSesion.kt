package com.imanlost.serena.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imanlost.serena.R
import com.imanlost.serena.audio.ControlSesion
import com.imanlost.serena.audio.EstadoSesion
import com.imanlost.serena.audio.formatearTiempo
import kotlin.math.min

/**
 * Pantalla de sesion: deliberadamente desnuda.
 *
 * Solo el tiempo restante, un anillo de progreso y pausa/terminar. Nada mas: se puede
 * usar sin mirar y el audio sigue aunque se apague la pantalla.
 */
@Composable
fun PantallaSesion(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val estado by ControlSesion.estado.collectAsState()
    val preparando = estado.preparando

    // El servicio marca el fin (natural o al terminar): se sale de la sesion.
    LaunchedEffect(estado.activa) {
        if (!estado.activa) onSalir()
    }
    // El boton atras tambien cierra con desvanecido, nunca de golpe. Durante la
    // preparacion cierra sin registrar nada, pero el gesto es el mismo.
    BackHandler(enabled = estado.activa) { ControlSesion.terminar(contexto) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnilloProgreso(
                progreso = progresoDe(estado),
                modifier = Modifier.size(260.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    // En preparacion el anillo y el numero cuentan los segundos de espera;
                    // al empezar, el mismo sitio pasa a ser el tiempo de practica.
                    text = if (preparando) {
                        formatearTiempo(estado.preparacionRestanteMs)
                    } else {
                        formatearTiempo(estado.restanteMs)
                    },
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                // Unico texto extra: la fase en la que estamos. Nada de titulos.
                if (preparando || estado.finalizando) {
                    Text(
                        text = if (preparando) {
                            stringResource(R.string.sesion_preparate)
                        } else {
                            stringResource(R.string.sesion_terminando)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = {
                    if (estado.pausada) {
                        ControlSesion.reanudar(contexto)
                    } else {
                        ControlSesion.pausar(contexto)
                    }
                },
                // En preparacion no hay nada que pausar: solo suena el gong.
                enabled = !estado.finalizando && !preparando,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = if (estado.pausada) {
                        stringResource(R.string.reanudar)
                    } else {
                        stringResource(R.string.pausar)
                    },
                    fontSize = 16.sp,
                )
            }
            OutlinedButton(
                onClick = { ControlSesion.terminar(contexto) },
                enabled = !estado.finalizando,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.terminar), fontSize = 16.sp)
            }
        }
    }
}

private fun progresoDe(estado: EstadoSesion): Float {
    // En preparacion el anillo marca la espera; despues, el avance de la practica.
    if (estado.preparando) {
        return if (estado.preparacionTotalMs > 0L) {
            (1f - estado.preparacionRestanteMs.toFloat() / estado.preparacionTotalMs.toFloat())
                .coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    return if (estado.totalMs > 0L) {
        (1f - estado.restanteMs.toFloat() / estado.totalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
}

@Composable
private fun AnilloProgreso(progreso: Float, modifier: Modifier = Modifier) {
    val acento = MaterialTheme.colorScheme.primary
    val pista = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier) {
        val grosor = 16.dp.toPx()
        val lado = min(size.width, size.height) - grosor
        val esquina = Offset((size.width - lado) / 2f, (size.height - lado) / 2f)
        val tam = Size(lado, lado)

        drawArc(
            color = pista,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = esquina,
            size = tam,
            style = Stroke(width = grosor, cap = StrokeCap.Round),
        )
        if (progreso > 0f) {
            drawArc(
                color = acento,
                startAngle = -90f,
                sweepAngle = 360f * progreso,
                useCenter = false,
                topLeft = esquina,
                size = tam,
                style = Stroke(width = grosor, cap = StrokeCap.Round),
            )
        }
    }
}
