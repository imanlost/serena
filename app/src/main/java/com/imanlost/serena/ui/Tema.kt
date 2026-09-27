package com.imanlost.serena.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paletas de Serena. Son cuatro familias con el mismo esqueleto: fondos calidos y un
// unico acento (Salvia, Lavanda, Ambar y Oceano), sin transparencias.
//
// En cada familia el modo oscuro NO es una inversion del claro: se ha disenado aparte
// (el papel y la tinta se invierten, pero el acento se aclara y los contenedores se
// oscurecen para mantener contraste suficiente). Por eso hay ocho esquemas literales.

private val EsquemaSalviaClaro = lightColorScheme(
    primary = Color(0xFF2F6B57),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E9E0),
    onPrimaryContainer = Color(0xFF0D2A20),
    secondary = Color(0xFF5B6E7F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE4EB),
    onSecondaryContainer = Color(0xFF15222B),
    background = Color(0xFFFBFAF7),
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFEFEDE6),
    onSurfaceVariant = Color(0xFF4A4841),
    outline = Color(0xFFBFBCB2),
    error = Color(0xFF9B2C2C),
    onError = Color(0xFFFFFFFF),
)

private val EsquemaSalviaOscuro = darkColorScheme(
    primary = Color(0xFF8FD3B6),
    onPrimary = Color(0xFF04372A),
    primaryContainer = Color(0xFF1F4A3D),
    onPrimaryContainer = Color(0xFFBFE8D8),
    secondary = Color(0xFF9FB2C4),
    onSecondary = Color(0xFF1B2A35),
    secondaryContainer = Color(0xFF2A3A45),
    onSecondaryContainer = Color(0xFFD5E1EA),
    background = Color(0xFF101312),
    onBackground = Color(0xFFE7E5DF),
    surface = Color(0xFF191C1B),
    onSurface = Color(0xFFE7E5DF),
    surfaceVariant = Color(0xFF232725),
    onSurfaceVariant = Color(0xFFC3C1B9),
    outline = Color(0xFF4E5250),
    error = Color(0xFFE8A0A0),
    onError = Color(0xFF3A0A0A),
)

private val EsquemaLavandaClaro = lightColorScheme(
    primary = Color(0xFF6A5B9E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7E1F6),
    onPrimaryContainer = Color(0xFF231A45),
    secondary = Color(0xFF756B8A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEAE4F0),
    onSecondaryContainer = Color(0xFF272132),
    background = Color(0xFFFAF8FD),
    onBackground = Color(0xFF1C1A21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1A21),
    surfaceVariant = Color(0xFFEFEBF5),
    onSurfaceVariant = Color(0xFF494455),
    outline = Color(0xFFBCB6C7),
    error = Color(0xFF9B2C2C),
    onError = Color(0xFFFFFFFF),
)

private val EsquemaLavandaOscuro = darkColorScheme(
    primary = Color(0xFFC5B6F0),
    onPrimary = Color(0xFF2B2050),
    primaryContainer = Color(0xFF3B2F63),
    onPrimaryContainer = Color(0xFFE3DBF9),
    secondary = Color(0xFFB4A9C4),
    onSecondary = Color(0xFF292333),
    secondaryContainer = Color(0xFF3A3347),
    onSecondaryContainer = Color(0xFFDAD2E5),
    background = Color(0xFF121016),
    onBackground = Color(0xFFE7E4ED),
    surface = Color(0xFF1A181F),
    onSurface = Color(0xFFE7E4ED),
    surfaceVariant = Color(0xFF242128),
    onSurfaceVariant = Color(0xFFC5C1CD),
    outline = Color(0xFF514D59),
    error = Color(0xFFE8A0A0),
    onError = Color(0xFF3A0A0A),
)

private val EsquemaAmbarClaro = lightColorScheme(
    primary = Color(0xFF9C5A1E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF8E2CA),
    onPrimaryContainer = Color(0xFF321A05),
    secondary = Color(0xFF7C6753),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE3D6),
    onSecondaryContainer = Color(0xFF281D13),
    background = Color(0xFFFDFAF6),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF1EAE1),
    onSurfaceVariant = Color(0xFF4D453D),
    outline = Color(0xFFC2B8AB),
    error = Color(0xFF9B2C2C),
    onError = Color(0xFFFFFFFF),
)

private val EsquemaAmbarOscuro = darkColorScheme(
    primary = Color(0xFFF0B77A),
    onPrimary = Color(0xFF3F2509),
    primaryContainer = Color(0xFF5A3A16),
    onPrimaryContainer = Color(0xFFFADFC4),
    secondary = Color(0xFFC9B7A3),
    onSecondary = Color(0xFF2F2518),
    secondaryContainer = Color(0xFF43382A),
    onSecondaryContainer = Color(0xFFE7DBCB),
    background = Color(0xFF14120F),
    onBackground = Color(0xFFEAE5DD),
    surface = Color(0xFF1C1916),
    onSurface = Color(0xFFEAE5DD),
    surfaceVariant = Color(0xFF262220),
    onSurfaceVariant = Color(0xFFC7C1B8),
    outline = Color(0xFF544C46),
    error = Color(0xFFE8A0A0),
    onError = Color(0xFF3A0A0A),
)

private val EsquemaOceanoClaro = lightColorScheme(
    primary = Color(0xFF1F5B7A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE6F3),
    onPrimaryContainer = Color(0xFF05212F),
    secondary = Color(0xFF546E7F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E5ED),
    onSecondaryContainer = Color(0xFF11222B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF181C1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181C1F),
    surfaceVariant = Color(0xFFE8EEF2),
    onSurfaceVariant = Color(0xFF434B51),
    outline = Color(0xFFB6C0C6),
    error = Color(0xFF9B2C2C),
    onError = Color(0xFFFFFFFF),
)

private val EsquemaOceanoOscuro = darkColorScheme(
    primary = Color(0xFF8ACCE8),
    onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF1B4A61),
    onPrimaryContainer = Color(0xFFC8E8F7),
    secondary = Color(0xFFA6C0D0),
    onSecondary = Color(0xFF102B39),
    secondaryContainer = Color(0xFF2A3D49),
    onSecondaryContainer = Color(0xFFD7E5EF),
    background = Color(0xFF0E1214),
    onBackground = Color(0xFFE3E7E9),
    surface = Color(0xFF161B1E),
    onSurface = Color(0xFFE3E7E9),
    surfaceVariant = Color(0xFF202629),
    onSurfaceVariant = Color(0xFFC0C7CB),
    outline = Color(0xFF4A5256),
    error = Color(0xFFE8A0A0),
    onError = Color(0xFF3A0A0A),
)

/** Modo de tema elegido por el usuario en Ajustes. */
enum class ModoTema { SISTEMA, CLARO, OSCURO }

/** Paleta elegida por el usuario en Ajustes. Los nombres visibles estan en strings.xml. */
enum class Paleta {
    SALVIA,
    LAVANDA,
    AMBAR,
    OCEANO;

    companion object {
        /** Paleta que se usa mientras el usuario no ha elegido otra. */
        val porDefecto = Paleta.SALVIA
    }
}

/** Devuelve el esquema Material 3 que corresponde a la paleta y al modo efectivo. */
fun esquema(paleta: Paleta, oscuro: Boolean): ColorScheme = when (paleta) {
    Paleta.SALVIA -> if (oscuro) EsquemaSalviaOscuro else EsquemaSalviaClaro
    Paleta.LAVANDA -> if (oscuro) EsquemaLavandaOscuro else EsquemaLavandaClaro
    Paleta.AMBAR -> if (oscuro) EsquemaAmbarOscuro else EsquemaAmbarClaro
    Paleta.OCEANO -> if (oscuro) EsquemaOceanoOscuro else EsquemaOceanoClaro
}

/**
 * Color `primary` literal de la paleta en ese modo, sin pasar por el tema activo.
 * Lo usa el selector de Ajustes para que cada muestra se pinte de SU color y no todas
 * del acento vigente.
 */
fun Paleta.primario(oscuro: Boolean): Color = when (this) {
    Paleta.SALVIA -> if (oscuro) Color(0xFF8FD3B6) else Color(0xFF2F6B57)
    Paleta.LAVANDA -> if (oscuro) Color(0xFFC5B6F0) else Color(0xFF6A5B9E)
    Paleta.AMBAR -> if (oscuro) Color(0xFFF0B77A) else Color(0xFF9C5A1E)
    Paleta.OCEANO -> if (oscuro) Color(0xFF8ACCE8) else Color(0xFF1F5B7A)
}

@Composable
fun SerenaTheme(
    modo: ModoTema = ModoTema.SISTEMA,
    paleta: Paleta = Paleta.porDefecto,
    content: @Composable () -> Unit,
) {
    val oscuro = when (modo) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }
    MaterialTheme(
        colorScheme = esquema(paleta, oscuro),
        content = content,
    )
}
