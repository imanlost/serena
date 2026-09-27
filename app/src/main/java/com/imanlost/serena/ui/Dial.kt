package com.imanlost.serena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imanlost.serena.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Selector de minutos: anillo indicador + contador digital.
 *
 * POR QUE NO SE ARRASTRA (fallo real, corregido): antes el anillo se giraba con el dedo.
 * Con los 60 minutos en una sola vuelta, cada minuto son 6 grados, y el giro se medía por
 * ANGULO: arrastrando cerca del centro (que es donde cae el dedo por comodidad) un
 * movimiento minimo barria muchos grados. En el tramo de 1 a 5 minutos, que ocupa menos
 * arco que el grosor de un dedo, el control era inmanejable. Lo dijo el usuario: "la
 * sensibilidad del dial es muy mala, sobre todo en menos de 5 minutos".
 *
 * Ahora el anillo solo MIRA: indica donde estas dentro de los 60 minutos (marcas cada cinco
 * minutos, las de cuarto de hora mas largas, y el arco del valor elegido). El ajuste se hace
 * con el contador de abajo, un toque un minuto, que no falla nunca, mas los atajos de los
 * tiempos de siempre.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialMinutos(
    minutos: Int,
    onMinutos: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minimo: Int = 1,
    maximo: Int = 60,
) {
    val acento = MaterialTheme.colorScheme.primary
    val pista = MaterialTheme.colorScheme.outline
    val colorMarcas = MaterialTheme.colorScheme.onSurfaceVariant
    val colorTexto = MaterialTheme.colorScheme.onBackground
    val colorSuave = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(248.dp)) {
                val grosor = 18.dp.toPx()
                val lado = min(size.width, size.height) - grosor
                val esquina = Offset((size.width - lado) / 2f, (size.height - lado) / 2f)
                val tam = Size(lado, lado)
                val centro = Offset(size.width / 2f, size.height / 2f)
                val radio = lado / 2f

                // Pista completa: el recorrido posible (los 60 minutos).
                drawArc(
                    color = pista,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = esquina,
                    size = tam,
                    style = Stroke(width = grosor, cap = StrokeCap.Round),
                )

                // Marcas cada cinco minutos, con las de cuarto de hora mas largas:
                // dan la lectura de esfera de reloj.
                val radioExterior = radio - grosor / 2f - 3.dp.toPx()
                for (marca in 0 until 12) {
                    val angulo = Math.toRadians((marca * 30 - 90).toDouble())
                    val largo = if (marca % 3 == 0) 12.dp.toPx() else 7.dp.toPx()
                    val ca = cos(angulo).toFloat()
                    val sa = sin(angulo).toFloat()
                    drawLine(
                        color = colorMarcas,
                        start = Offset(
                            centro.x + (radioExterior - largo) * ca,
                            centro.y + (radioExterior - largo) * sa,
                        ),
                        end = Offset(centro.x + radioExterior * ca, centro.y + radioExterior * sa),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                // Arco de los minutos elegidos.
                drawArc(
                    color = acento,
                    startAngle = -90f,
                    sweepAngle = 360f * minutos.toFloat() / 60f,
                    useCenter = false,
                    topLeft = esquina,
                    size = tam,
                    style = Stroke(width = grosor, cap = StrokeCap.Round),
                )

                // Asa: marca donde esta el minuto elegido. A los 60 el circulo ya esta
                // cerrado y el asa sobraria encima de las 12.
                if (minutos < 60) {
                    val anguloAsa = Math.toRadians((minutos.toFloat() / 60f * 360f - 90f).toDouble())
                    drawCircle(
                        color = acento,
                        radius = grosor * 0.62f,
                        center = Offset(
                            centro.x + radio * cos(anguloAsa).toFloat(),
                            centro.y + radio * sin(anguloAsa).toFloat(),
                        ),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = minutos.toString(),
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Light,
                    color = colorTexto,
                )
                Text(
                    text = stringResource(R.string.minutos),
                    fontSize = 17.sp,
                    color = colorSuave,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Contador digital: un toque, un minuto exacto. Es donde el arrastre fallaba.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            BotonPaso(
                simbolo = "−",
                descripcion = stringResource(R.string.un_minuto_menos),
                habilitado = minutos > minimo,
                onClick = { onMinutos((minutos - 1).coerceAtLeast(minimo)) },
            )
            BotonPaso(
                simbolo = "+",
                descripcion = stringResource(R.string.un_minuto_mas),
                habilitado = minutos < maximo,
                onClick = { onMinutos((minutos + 1).coerceAtMost(maximo)) },
            )
        }

        Spacer(Modifier.height(18.dp))

        // Atajos: los tiempos de siempre, de un solo toque, sin encadenar pulsaciones.
        val atajos = listOf(5, 10, 15, 20, 30, 45, 60)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            // Cuatro por fila: con siete atajos, el ultimo quedaba solo en una segunda fila
            // y se veia desequilibrado. Asi quedan 4 + 3.
            maxItemsInEachRow = 4,
        ) {
            atajos.forEach { valor ->
                FilterChip(
                    selected = minutos == valor,
                    onClick = { onMinutos(valor) },
                    label = { Text(valor.toString(), fontSize = 14.sp) },
                )
            }
        }
    }
}

/** Boton redondo de un minuto, con el simbolo grande para dar con el pulgar. */
@Composable
private fun BotonPaso(
    simbolo: String,
    descripcion: String,
    habilitado: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(66.dp)
            .semantics { contentDescription = descripcion },
    ) {
        Text(
            text = simbolo,
            fontSize = 30.sp,
            fontWeight = FontWeight.Light,
            color = if (habilitado) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        )
    }
}
