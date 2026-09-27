# Encargo 07 — Cuatro paletas de color y persistencia del tema

## Objetivo

Serena tiene hoy UNA sola paleta (verde salvia) en `ui/Tema.kt`, con dos esquemas escritos a
mano (claro y oscuro). El encargo es convertirla en una **familia de cuatro paletas** que el
usuario pueda elegir en Ajustes → Tema, aplicadas a toda la aplicación, y **que la elección se
recuerde** entre arranques.

Hoy hay además un fallo que este encargo corrige: el modo de tema elegido
(`ModoTema`, Sistema/Claro/Oscuro) vive en `rememberSaveable` dentro de `MainActivity`, así que
**se pierde al cerrar la app**. Paleta y modo deben persistir los dos.

## Criterio de "hecho"

1. En Ajustes → Tema hay un selector de paleta con cuatro opciones visibles por su nombre y su
   color: **Salvia, Lavanda, Ámbar, Océano**.
2. Al tocar una, **toda la aplicación** cambia de paleta al instante (pestañas, botones, tarjetas,
   fondo), en claro y en oscuro.
3. Al cerrar y volver a abrir la app, se mantienen la paleta y el modo elegidos.
4. Compila sin errores y sin avisos nuevos de Kotlin (`./gradlew assembleDebug` lo ejecuta Hermes,
   no tú: deja el código listo y di si dudas de algo).

## Las cuatro paletas (valores EXACTOS, no los cambies)

Cada paleta necesita sus dos esquemas Material 3, con **los mismos 17 roles** que usa hoy
`EsquemaClaro`/`EsquemaOscuro` (primary, onPrimary, primaryContainer, onPrimaryContainer,
secondary, onSecondary, secondaryContainer, onSecondaryContainer, background, onBackground,
surface, onSurface, surfaceVariant, onSurfaceVariant, outline, error, onError).

El modo oscuro **no es una inversión** del claro: cada variante va diseñada aparte (regla del
proyecto). Los valores de abajo están ya verificados uno a uno con el cálculo de contraste WCAG
(texto ≥ 4,5:1 en todos los pares on\*/fondo): **cópialos tal cual**.

### 1. Salvia (la actual, por defecto)

Claro:

```
primary 0xFF2F6B57   onPrimary 0xFFFFFFFF   primaryContainer 0xFFD7E9E0   onPrimaryContainer 0xFF0D2A20
secondary 0xFF5B6E7F onSecondary 0xFFFFFFFF secondaryContainer 0xFFDCE4EB onSecondaryContainer 0xFF15222B
background 0xFFFBFAF7 onBackground 0xFF1C1B18 surface 0xFFFFFFFF onSurface 0xFF1C1B18
surfaceVariant 0xFFEFEDE6 onSurfaceVariant 0xFF4A4841 outline 0xFFBFBCB2
error 0xFF9B2C2C onError 0xFFFFFFFF
```

Oscuro:

```
primary 0xFF8FD3B6   onPrimary 0xFF04372A   primaryContainer 0xFF1F4A3D   onPrimaryContainer 0xFFBFE8D8
secondary 0xFF9FB2C4 onSecondary 0xFF1B2A35 secondaryContainer 0xFF2A3A45 onSecondaryContainer 0xFFD5E1EA
background 0xFF101312 onBackground 0xFFE7E5DF surface 0xFF191C1B onSurface 0xFFE7E5DF
surfaceVariant 0xFF232725 onSurfaceVariant 0xFFC3C1B9 outline 0xFF4E5250
error 0xFFE8A0A0 onError 0xFF3A0A0A
```

### 2. Lavanda

Claro:

```
primary 0xFF6A5B9E   onPrimary 0xFFFFFFFF   primaryContainer 0xFFE7E1F6   onPrimaryContainer 0xFF231A45
secondary 0xFF756B8A onSecondary 0xFFFFFFFF secondaryContainer 0xFFEAE4F0 onSecondaryContainer 0xFF272132
background 0xFFFAF8FD onBackground 0xFF1C1A21 surface 0xFFFFFFFF onSurface 0xFF1C1A21
surfaceVariant 0xFFEFEBF5 onSurfaceVariant 0xFF494455 outline 0xFFBCB6C7
error 0xFF9B2C2C onError 0xFFFFFFFF
```

Oscuro:

```
primary 0xFFC5B6F0   onPrimary 0xFF2B2050   primaryContainer 0xFF3B2F63   onPrimaryContainer 0xFFE3DBF9
secondary 0xFFB4A9C4 onSecondary 0xFF292333 secondaryContainer 0xFF3A3347 onSecondaryContainer 0xFFDAD2E5
background 0xFF121016 onBackground 0xFFE7E4ED surface 0xFF1A181F onSurface 0xFFE7E4ED
surfaceVariant 0xFF242128 onSurfaceVariant 0xFFC5C1CD outline 0xFF514D59
error 0xFFE8A0A0 onError 0xFF3A0A0A
```

### 3. Ámbar

Claro:

```
primary 0xFF9C5A1E   onPrimary 0xFFFFFFFF   primaryContainer 0xFFF8E2CA   onPrimaryContainer 0xFF321A05
secondary 0xFF7C6753 onSecondary 0xFFFFFFFF secondaryContainer 0xFFEFE3D6 onSecondaryContainer 0xFF281D13
background 0xFFFDFAF6 onBackground 0xFF1F1B16 surface 0xFFFFFFFF onSurface 0xFF1F1B16
surfaceVariant 0xFFF1EAE1 onSurfaceVariant 0xFF4D453D outline 0xFFC2B8AB
error 0xFF9B2C2C onError 0xFFFFFFFF
```

Oscuro:

```
primary 0xFFF0B77A   onPrimary 0xFF3F2509   primaryContainer 0xFF5A3A16   onPrimaryContainer 0xFFFADFC4
secondary 0xFFC9B7A3 onSecondary 0xFF2F2518 secondaryContainer 0xFF43382A onSecondaryContainer 0xFFE7DBCB
background 0xFF14120F onBackground 0xFFEAE5DD surface 0xFF1C1916 onSurface 0xFFEAE5DD
surfaceVariant 0xFF262220 onSurfaceVariant 0xFFC7C1B8 outline 0xFF544C46
error 0xFFE8A0A0 onError 0xFF3A0A0A
```

### 4. Océano

Claro:

```
primary 0xFF1F5B7A   onPrimary 0xFFFFFFFF   primaryContainer 0xFFCFE6F3   onPrimaryContainer 0xFF05212F
secondary 0xFF546E7F onSecondary 0xFFFFFFFF secondaryContainer 0xFFD8E5ED onSecondaryContainer 0xFF11222B
background 0xFFF8FAFC onBackground 0xFF181C1F surface 0xFFFFFFFF onSurface 0xFF181C1F
surfaceVariant 0xFFE8EEF2 onSurfaceVariant 0xFF434B51 outline 0xFFB6C0C6
error 0xFF9B2C2C onError 0xFFFFFFFF
```

Oscuro:

```
primary 0xFF8ACCE8   onPrimary 0xFF00344A   primaryContainer 0xFF1B4A61   onPrimaryContainer 0xFFC8E8F7
secondary 0xFFA6C0D0 onSecondary 0xFF102B39 secondaryContainer 0xFF2A3D49 onSecondaryContainer 0xFFD7E5EF
background 0xFF0E1214 onBackground 0xFFE3E7E9 surface 0xFF161B1E onSurface 0xFFE3E7E9
surfaceVariant 0xFF202629 onSurfaceVariant 0xFFC0C7CB outline 0xFF4A5256
error 0xFFE8A0A0 onError 0xFF3A0A0A
```

## Cambios pedidos, fichero a fichero

### `app/src/main/java/com/imanlost/serena/ui/Tema.kt` (reescribir)

- `enum class Paleta { SALVIA, LAVANDA, AMBAR, OCEANO }` con `val porDefecto = Paleta.SALVIA` (o
  `Paleta.SALVIA` como valor inicial donde haga falta).
- Una función pública `fun esquema(paleta: Paleta, oscuro: Boolean): ColorScheme` que devuelva el
  `lightColorScheme`/`darkColorScheme` correspondiente (un `when` de 8 ramas). Los ocho esquemas
  van como `private val` en este fichero.
- Un método público para pintar la muestra de color del selector, sin depender del tema activo:
  `fun Paleta.primario(oscuro: Boolean): Color` (devuelve el `primary` literal de esa paleta en ese
  modo). Sin esto, todas las muestras saldrían del mismo color.
- `ModoTema` se queda como está.
- `SerenaTheme(modo: ModoTema, paleta: Paleta, content: ...)`: resuelve `oscuro` como hoy
  (SISTEMA → `isSystemInDarkTheme()`) y aplica `esquema(paleta, oscuro)`.
- Mantén el comentario de cabecera del fichero explicando qué es cada paleta y por qué el oscuro no
  es una inversión.

### `app/src/main/java/com/imanlost/serena/ui/PreferenciasTema.kt` (nuevo)

Fichero nuevo, pequeño, para no mezclar preferencias de interfaz con `ControlSesion` (que es de
audio). Mismo patrón que las preferencias que ya existen:

- `SharedPreferences` propio llamado `"serena_ui"` (`Context.MODE_PRIVATE`).
- Claves `modo_tema` y `paleta`.
- `fun modo(contexto: Context): ModoTema` y `fun fijarModo(contexto: Context, modo: ModoTema)`.
- `fun paleta(contexto: Context): Paleta` y `fun fijarPaleta(contexto: Context, paleta: Paleta)`.
- **Robustez obligatoria**: leer con
  `ModoTema.entries.firstOrNull { it.name == guardado } ?: ModoTema.SISTEMA` (y lo mismo con
  `Paleta`), de modo que un valor raro guardado en disco nunca rompa el arranque.

### `app/src/main/java/com/imanlost/serena/MainActivity.kt`

- `onCreate`: leer `PreferenciasTema.modo(this)` y `PreferenciasTema.paleta(this)` **antes** de
  `setContent`, y usarlos como estado inicial (el estado de tema y paleta deja de ser
  `rememberSaveable`: la fuente de verdad es la preferencia).
- Al cambiar de modo o de paleta: actualizar el estado **y** escribir la preferencia.
- Pasa paleta y `onPaleta` a `PantallaAjustes` junto a `modo`/`onModo`.
- No toques nada más de la actividad (pestañas, sesión, permiso de notificaciones).

### `app/src/main/java/com/imanlost/serena/ui/PantallaAjustes.kt`

- `SeccionTema` pasa a recibir `modo`, `onModo`, `paleta`, `onPaleta` y un booleano
  `oscuro: Boolean` (el modo efectivo, para pintar bien las muestras).
- Debajo del selector de modo (que se queda como está), añade el **selector de paleta**: una fila
  desplazable horizontalmente con las cuatro paletas. Cada opción muestra el **nombre** y una
  **muestra de color** —un círculo del `primary` literal de esa paleta en el modo efectivo, con
  borde del `outline` del tema activo— y la elegida queda marcada (borde/fondo del `primary`
  activo o `selected`). Sirve un `FilterChip` con `leadingIcon` o un `Box` con
  `Modifier.clickable`; elige lo que dé un toque más grande y claro.
- Etiqueta de la sección: el título «Tema» ya existe y no cambia.
- No transpongas colores (`Color.copy(alpha=...)`): el proyecto no usa transparencias.

### `app/src/main/res/values/strings.xml`

Añade **solo** estas cadenas nuevas (comprueba antes que no existen: una clave repetida rompe
`mergeDebugResources`):

```xml
<string name="paleta">Paleta</string>
<string name="paleta_salvia">Salvia</string>
<string name="paleta_lavanda">Lavanda</string>
<string name="paleta_ambar">Ámbar</string>
<string name="paleta_oceano">Océano</string>
```

No añadas traducciones a otros idiomas ni toques ninguna otra cadena. Comentarios del código en
castellano, con el motivo.

## Lo que NO debes hacer

- **No modifiques `AGENTS.md`** ni `docs/PLAN_SERENA.md`.
- **No hagas commits** (Hermes revisa el diff, compila y commitea).
- No toques `ControlSesion.kt`, `ServicioSesion.kt`, la base de datos, ni nada de `audio/`.
- No añadas dependencias nuevas ni subas versiones (toolchain anclado).
- No intentes compilar (en el sandbox no hay JDK ni SDK): deja el código correcto y dilo si dudas.
- No implementes el recordatorio diario ni ninguna notificación: eso queda fuera de este encargo.
- No añadas textos en inglés ni en otros idiomas.

## Cómo terminar tu respuesta

1. Lista de ficheros tocados (y si has creado `PreferenciasTema.kt`).
2. Cualquier punto del encargo que no hayas cumplido y por qué.
3. Qué es lo que más te hace dudar de que compile en el primer intento.
