# Encargo 08 — Diario de sesiones en Seguimiento (con detalle, notas y alta manual)

## Objetivo

Añadir al final de la pestaña **Seguimiento** (hoy `PantallaProgreso` en `MainActivity.kt`) un
**diario de las meditaciones**, lo más reciente arriba, con fecha, nombre de la meditación y minutos.
Cada entrada se puede abrir para ver el detalle, ponerle una **nota**, corregir los minutos o
**borrarla** (con confirmación). Además, un botón para **añadir a mano** una sesión que la app no
registró (una meditación hecha sin el móvil).

Todo el trabajo es **local**: la app no declara INTERNET y no debe declararlo. Ni cuentas, ni red.

## Lo que ya existe (léelo antes de escribir)

- `datos/Sesion.kt` — entidad `Sesion`: `id`, `inicioMs`, `finMs`, `duracionMs`, `pista`, `grupo`,
  `completada`, `nota` (nullable), `idSalud`. **Tabla `sesiones`, versión 1.**
- `datos/SesionDao.kt` — solo tres consultas agregadas (`Flow`): minutos totales, días con práctica y
  minutos por día. Las sumas se hacen en SQL, nunca en el hilo principal.
- `datos/RepositorioSesiones.kt` — punto único de acceso: `registrar()`, `resumen()` y el umbral
  `UMBRAL_RACHA_MS = 60_000L` (una sesión cuenta para la racha desde un minuto real).
- `estadisticas/Calculos.kt` — racha, mejor racha y barras semanales. **No lo toques** salvo que sea
  imprescindible; este encargo no cambia ninguna regla de estadística.
- `MainActivity.kt` — `PantallaProgreso()` pinta hoy, dentro de un `Column` con `verticalScroll`:
  `TarjetaRacha`, `DatoProgreso` (minutos totales), `CalendarioConstancia` y `BarrasSemanales`.
- `audio/Biblioteca.kt` — catálogo de pistas en el dispositivo (lo usarás solo para leer la lista de
  nombres y grupos si decides ofrecerla en el alta manual; no lo modifiques).

## Qué hay que construir

### 1. Consultas nuevas (SesionDao.kt + RepositorioSesiones.kt)

- Lista completa de sesiones, la más reciente primero: `SELECT * FROM sesiones ORDER BY inicioMs DESC`
  devuelta como `Flow<List<Sesion>>`.
- Fijar la nota de una sesión (`UPDATE ... SET nota = :nota WHERE id = :id`). **Normaliza la cadena
  vacía a `null`** antes de guardar.
- Cambiar la duración de una sesión (`UPDATE ... SET duracionMs = :duracionMs WHERE id = :id`).
- Borrar una sesión (`DELETE FROM sesiones WHERE id = :id`).
- Envuelve las tres escrituras en `RepositorioSesiones` con `suspend fun`, en la misma línea que
  `registrar()`, para que la interfaz no hable nunca con el DAO directamente.

**La versión de la base de datos NO cambia** (sigue siendo la 1): el esquema no se toca, la columna
`nota` ya existe. No añadas migraciones ni toques `SerenaDb.kt` salvo que sea estrictamente necesario.

### 2. Pantalla de Seguimiento: de `Column` a `LazyColumn`

`PantallaProgreso` pasa a `LazyColumn` **conservando exactamente** lo que ya muestra (racha, minutos
totales, calendario y barras semanales, en ese orden) y añadiendo el diario al final. Motivo: el
diario puede crecer sin límite y una lista larga dentro de un `Column` con `verticalScroll` da altura
infinita y error en tiempo de ejecución.

### 3. El diario

- **Sección con título** ("Diario") y, debajo, las sesiones agrupadas **por día natural local**, de lo
  más reciente a lo más antiguo. Cabecera de grupo: `Hoy` y `Ayer` para esos dos días, y para el resto
  la fecha larga en castellano (`lunes, 22 de septiembre`, con `Locale("es", "ES")`).
- **Cada fila**: hora (`HH:mm`), nombre de la pista y minutos (p. ej. `32 min`). Si la sesión no llegó
  a completarse, que se note de forma discreta (por ejemplo `incompleta` en texto secundario). Si la
  sesión tiene nota, un indicador discreto de que la tiene.
- **Sin sesiones**: en lugar de la lista, el texto de que todavía no hay práctica registrada (la
  pantalla ya tiene un aviso para ese caso: reutilízalo o añade uno propio para el diario).
- **Rendimiento**: pinta el diario como ítems de la `LazyColumn`, no como una lista dentro de un ítem.
  No metas la lista entera dentro de una `Card` única con un `Column` interno que recorra todas las
  sesiones.

### 4. Detalle de la sesión (al tocar una fila)

Diálogo con: la pista (título), la fecha y la hora completas, los **minutos editables** (campo
numérico, mínimo 1), un campo de **nota** (varias líneas, opcional) y dos acciones:

- **Guardar** — aplica minutos y nota y cierra.
- **Borrar** — nunca borres al primer toque: pide confirmación en un diálogo aparte
  (`¿Borrar esta sesión? No se puede deshacer.`). Al borrar, las estadísticas y la racha se recalculan
  solas porque todo son `Flow` de Room: no fuerces ninguna recarga a mano.

No inventes un botón de "volver a poner esta meditación": abrir la pista desde el diario queda fuera
de este encargo (lo decidirá el usuario más adelante).

### 5. Alta manual de una sesión

Botón discreto en la cabecera del diario ("Añadir sesión") que abre un diálogo con:

- **Fecha** (selector de fecha de material3) y **hora** (selector de hora), por defecto hoy y ahora.
- **Meditación**: campo de texto con la lista de pistas de la biblioteca para elegir (si el catálogo
  tiene 200 y pico entradas, un desplegable con esas entradas está bien, pero debe poder escribirse
  también un nombre libre). Si la pista sale del catálogo, guarda **también su grupo**; si es libre, el
  grupo queda vacío.
- **Minutos** (campo numérico, mínimo 1).
- Al guardar: inserta una `Sesion` con `inicioMs` = fecha y hora elegidas, `duracionMs` =
  `minutos * 60_000`, `finMs = inicioMs + duracionMs`, `completada = true`, `nota = null`, `idSalud =
  null`. Insértala con `RepositorioSesiones.registrar()`; **no** pases por `ControlSesion` (ese camino
  arranca audio y aquí no hay que reproducir nada).

## Restricciones

- **No cambies la versión de la base de datos** ni el esquema.
- **No modifiques `AGENTS.md`** (si lo tocas sin querer, revierte con `git checkout -- AGENTS.md`).
- **No hagas commits.** No compiles (en el sandbox no hay JDK ni SDK): deja el código correcto y di lo
  que no puedas validar.
- Sin dependencias nuevas y sin subir versiones (Kotlin 2.0.21, Compose BOM 2024.10.01, material3 1.3.1).
- Nada de INTERNET ni de permisos nuevos en el manifiesto.
- **Todo en castellano y con tildes correctas.** Los textos de la interfaz van en
  `res/values/strings.xml` (no literales sueltos en Kotlin), con nombres en minúscula y sin acentos,
  siguiendo los que ya hay: `diario`, `anadir_sesion`, `minutos`, `nota`, `guardar`, `borrar`,
  `borrar_sesion_pregunta`, `hoy`, `ayer`… Añade solo las que necesites y **evita duplicar** alguna ya
  existente (mira antes el fichero entero).
- Cuida el formato de los números y las fechas con el idioma del dispositivo.
- Comentarios del código: en el estilo del proyecto (castellano, sin tildes dentro del código, como el
  resto de ficheros).

## Ficheros que puedes tocar

`datos/SesionDao.kt`, `datos/RepositorioSesiones.kt`, `MainActivity.kt` (solo `PantallaProgreso` y su
llamada), `res/values/strings.xml`, y un fichero nuevo `ui/Diario.kt` (o el nombre que encaje mejor)
para la lista, los diálogos y la agrupación por día. **No toques** la Biblioteca, Ajustes, el audio,
`Calculos.kt` ni la base de datos.

## Qué informar al final

1. Qué ficheros has tocado y qué has añadido en cada uno.
2. Si has dejado algún punto del encargo sin cumplir, dicho sin adornos.
3. Dónde dudas: lo que no has podido verificar sin compilar (firmas de API, imports) y qué te haría
   falta.
4. Confirmación explícita de que no has tocado `AGENTS.md`, de que no has hecho commits y de que la
   versión de la base de datos sigue siendo la 1.
