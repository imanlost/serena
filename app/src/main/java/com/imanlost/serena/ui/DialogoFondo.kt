package com.imanlost.serena.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.imanlost.serena.R
import com.imanlost.serena.audio.Pista

/**
 * Pregunta con que fondo acompanar una meditacion guiada antes de arrancarla.
 *
 * Se usa `AlertDialog` de Material 3 para heredar del tema claro/oscuro de la app. La
 * lista se desplaza con un tope de altura: si el usuario anade muchos fondos, el dialogo
 * sigue cabiendo en pantalla en vez de tapar la biblioteca entera.
 */
@Composable
fun DialogoElegirFondo(
    fondos: List<Pista>,
    seleccionInicial: Pista?,
    onCancelar: () -> Unit,
    onConfirmar: (Pista?) -> Unit,
) {
    // null representa «Sin fondo». Se guarda el id (String) porque rememberSaveable no
    // sabe guardar una Pista y asi la eleccion sobrevive a un giro de pantalla.
    var seleccion by rememberSaveable(seleccionInicial?.id) {
        mutableStateOf(seleccionInicial?.id)
    }

    AlertDialog(
        onDismissRequest = onCancelar,
        // Se fija el color del tema propio: el gris por defecto de Material desentona
        // con la paleta calida de Serena.
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.elegir_fondo_titulo)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                // «Sin fondo» va siempre la primera: es la opcion mas rapida y la mas
                // probable para quien no quiere ruido bajo la voz.
                item(key = "sin-fondo") {
                    OpcionFondo(
                        texto = stringResource(R.string.fondo_sin_fondo),
                        seleccionada = seleccion == null,
                        onClick = { seleccion = null },
                    )
                }
                items(fondos, key = { it.id }) { fondo ->
                    OpcionFondo(
                        texto = fondo.titulo,
                        seleccionada = seleccion == fondo.id,
                        onClick = { seleccion = fondo.id },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                // La preseleccion ya viene marcada: confirmar es un solo toque.
                onClick = { onConfirmar(fondos.firstOrNull { it.id == seleccion }) },
            ) {
                Text(stringResource(R.string.empezar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.cancelar))
            }
        },
    )
}

/** Fila seleccionable de una opcion de fondo (radio, texto y toda la fila clicable). */
@Composable
private fun OpcionFondo(texto: String, seleccionada: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionada, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = seleccionada, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
