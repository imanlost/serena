package com.imanlost.serena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imanlost.serena.R
import com.imanlost.serena.estadisticas.SemanaMinutos
import java.time.LocalDate
import java.time.YearMonth

/**
 * Graficos de constancia. Todos reciben los datos ya calculados: aqui no se lee
 * la base de datos ni se decide la logica de racha, solo se dibuja.
 *
 * El color de intensidad es el mismo acento en cuatro pasos (opacos, sin
 * transparencias): asi el grafico informa sin introducir una segunda paleta.
 */

/** Cuatro niveles de intensidad del acento, de menos a mas. */
@Composable
private fun escaleraDeIntensidad(): List<Color> {
    val acento = MaterialTheme.colorScheme.primary
    val fondo = MaterialTheme.colorScheme.background
    return listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        lerp(fondo, acento, 0.28f),
        lerp(fondo, acento, 0.58f),
        acento,
    )
}

/** Un dia de practica, tal como lo consume el calendario. */
data class DiaPractica(val fecha: LocalDate, val minutos: Int)

/**
 * Calendario mensual de constancia: cada dia es una celda coloreada segun los
 * minutos practicados. Es la vista que responde a "¿estoy siendo constante?".
 */
@Composable
fun CalendarioConstancia(
    mes: YearMonth,
    dias: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
) {
    val intensidades = escaleraDeIntensidad()
    val colorBorde = MaterialTheme.colorScheme.outline
    val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val textoApagado = MaterialTheme.colorScheme.onSurface
    val letras = listOf("L", "M", "X", "J", "V", "S", "D")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = mesDeTexto(mes),
            style = MaterialTheme.typography.titleMedium,
            color = textoApagado,
        )
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            letras.forEach { letra ->
                Text(
                    text = letra,
                    fontSize = 12.sp,
                    color = colorTexto,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        // La primera semana se alinea al lunes: offset = dias desde el lunes.
        val primerDia = mes.atDay(1)
        val huecos = primerDia.dayOfWeek.value - 1
        val totalCeldas = huecos + mes.lengthOfMonth()
        val semanas = (totalCeldas + 6) / 7

        for (semana in 0 until semanas) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (diaSemana in 0 until 7) {
                    val numeroDia = semana * 7 + diaSemana - huecos + 1
                    if (numeroDia in 1..mes.lengthOfMonth()) {
                        val fecha = mes.atDay(numeroDia)
                        val minutos = dias[fecha] ?: 0
                        val nivel = when {
                            minutos <= 0 -> 0
                            minutos < 10 -> 1
                            minutos < 20 -> 2
                            else -> 3
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(intensidades[nivel]),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = numeroDia.toString(),
                                fontSize = 11.sp,
                                color = if (nivel >= 2) textoApagado else colorTexto,
                            )
                        }
                    } else {
                        Spacer(Modifier.size(28.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.menos),
                fontSize = 11.sp,
                color = colorTexto,
            )
            Spacer(Modifier.size(6.dp))
            intensidades.forEach { color ->
                Box(
                    modifier = Modifier
                        .padding(end = 3.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color),
                )
            }
            Spacer(Modifier.size(6.dp))
            Text(
                text = stringResource(R.string.mas),
                fontSize = 11.sp,
                color = colorTexto,
            )
        }
    }
}

/**
 * Barras de minutos por semana (las ultimas ocho). Responde a "¿cuanto practico?"
 * alli donde el calendario responde a "¿con que regularidad?".
 */
@Composable
fun BarrasSemanales(
    semanas: List<SemanaMinutos>,
    modifier: Modifier = Modifier,
) {
    if (semanas.isEmpty()) return
    val acento = MaterialTheme.colorScheme.primary
    val apagado = MaterialTheme.colorScheme.surfaceVariant
    val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val altoMaximo = 96.dp
    val maximo = semanas.maxOf { it.minutos }.coerceAtLeast(1)

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.minutos_por_semana),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(altoMaximo + 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            semanas.forEach { semana ->
                val proporcion = semana.minutos.toFloat() / maximo.toFloat()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Column(
                        modifier = Modifier
                            .size(width = 22.dp, height = altoMaximo)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((altoMaximo * proporcion).coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (semana.minutos > 0) acento else apagado),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = semana.etiqueta,
                        fontSize = 10.sp,
                        color = colorTexto,
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de racha: el numero grande y, debajo, el contexto que evita el
 * "ya la he roto, da igual": total acumulado y mejor racha siempre a la vista.
 */
@Composable
fun TarjetaRacha(
    rachaActual: Int,
    mejorRacha: Int,
    diasTotales: Int,
    modifier: Modifier = Modifier,
) {
    val acento = MaterialTheme.colorScheme.primary
    val colorTexto = MaterialTheme.colorScheme.onSurface
    val colorSuave = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = rachaActual.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = acento,
            )
            Text(
                text = " " + stringResource(R.string.dias),
                fontSize = 16.sp,
                color = colorSuave,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Text(
            text = stringResource(R.string.racha_actual),
            fontSize = 13.sp,
            color = colorSuave,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.contexto_racha, diasTotales, mejorRacha),
            fontSize = 13.sp,
            color = colorSuave,
        )
    }
}

/** "septiembre de 2026" sin depender del idioma del sistema. */
@Composable
private fun mesDeTexto(mes: YearMonth): String {
    val nombres = listOf(
        stringResource(R.string.enero), stringResource(R.string.febrero),
        stringResource(R.string.marzo), stringResource(R.string.abril),
        stringResource(R.string.mayo), stringResource(R.string.junio),
        stringResource(R.string.julio), stringResource(R.string.agosto),
        stringResource(R.string.septiembre), stringResource(R.string.octubre),
        stringResource(R.string.noviembre), stringResource(R.string.diciembre),
    )
    return nombres[mes.monthValue - 1] + " de " + mes.year
}
