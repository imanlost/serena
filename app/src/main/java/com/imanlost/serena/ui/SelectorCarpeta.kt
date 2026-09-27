package com.imanlost.serena.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.imanlost.serena.audio.ControlSesion

/**
 * Selector SAF de la carpeta de audios, compartido por la biblioteca y Ajustes.
 *
 * Al recibir la URI se toma el permiso persistente (`takePersistableUriPermission`):
 * sin el, la eleccion se perderia al reiniciar. Despues se guarda la URI y su nombre
 * legible, y se avisa con `onElegida` para que la pantalla relea la carpeta.
 */
@Composable
fun rememberSelectorCarpeta(onElegida: () -> Unit): ActivityResultLauncher<Uri?> {
    val contexto = LocalContext.current
    return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            // Si el proveedor no concede el permiso persistente, la app sigue
            // funcionando en esta sesion; solo no sobrevive al reinicio.
            runCatching {
                contexto.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            ControlSesion.guardarCarpetaAudio(contexto, uri, nombreDeCarpeta(contexto, uri))
            onElegida()
        }
    }
}

/** Nombre legible de la carpeta: el que da SAF o, si falta, el ultimo tramo de la URI. */
private fun nombreDeCarpeta(contexto: Context, uri: Uri): String? =
    DocumentFile.fromTreeUri(contexto, uri)?.name
        ?: uri.lastPathSegment?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
