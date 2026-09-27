# Encargo 03 — Biblioteca elegible (selector de carpeta) y su sección en Ajustes

Contexto: lee `AGENTS.md` y `docs/PLAN_SERENA.md`. Este encargo **sustituye** la decisión del
encargo 02 sobre la localización de los audios.

## El problema que hay que resolver

La versión actual busca en la subcarpeta fija `Music/Serena` del volumen principal. El usuario
la biblioteca puede estar en la **tarjeta SD**, en otra carpeta, y entonces no ve nada. Además el mensaje
de biblioteca vacía nombraba la carpeta como "Música" (traducida), cuando la carpeta real del
sistema se llama `Music`: eso confundió al usuario con razón. **Nunca traduzcas nombres de rutas
reales en los textos.**

## Objetivo

Que el usuario elija, una sola vez, la carpeta donde tiene sus audios, y que esa elección
sobreviva a reinicios.

## Tareas

1. **Selector de carpeta con SAF** (`ACTION_OPEN_DOCUMENT_TREE`), lanzado con
   `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree())`.
   Al recibir la URI hay que llamar a `contentResolver.takePersistableUriPermission(uri,
   Intent.FLAG_GRANT_READ_URI_PERMISSION)`; sin eso la elección se pierde al reiniciar.

2. **Guardar la elección** (URI en forma de cadena) y su **nombre legible** para mostrarlo.
   Usa el mismo almacenamiento de preferencias que ya se usa para el volumen del fondo
   (`ControlSesion`). Añade lo que haga falta sin cambiar la API existente.

3. **Escaneo de la carpeta elegida** con `DocumentFile` (`androidx.documentfile:documentfile`):
   recorre la carpeta y una subcarpeta de profundidad, igual que ahora, para que los grupos
   salgan de las subcarpetas (`guiadas`, `fondo`, `podcasts` y demás). La duración se lee del
   fichero con `MediaMetadataRetriever` (`setDataSource(contexto, uri)`), nunca estimada.
   La ruta de cada pista sera su `content://` (ExoPlayer lo reproduce sin problema) y el `id`
   tiene que ser estable.

4. **Quitar el permiso `READ_MEDIA_AUDIO` y el escaneo por `MediaStore`**: con SAF no hace
   falta ningún permiso de almacenamiento. El objetivo es que el APK no pida ni un permiso de
   lectura de ficheros. Ajusta también el aviso de permiso de la pantalla de biblioteca: ya no
   se pide ningún permiso del sistema, solo la carpeta.

5. **Pantalla de biblioteca sin carpeta elegida**: mensaje claro ("Elige la carpeta donde
   tienes tus meditaciones") y un botón grande para elegirla. Nada de rutas inventadas en el
   texto. Si el usuario elige una carpeta que no tiene audios, hay que decirlo sin dramatizar
   ("No he encontrado audios en esa carpeta") y dejar elegir otra.

6. **Ajustes — sección "Biblioteca"**: carpeta elegida (su nombre legible), botón "Elegir"
   carpeta", botón "Volver a escanear" (relanza el escaneo sin cambiar de carpeta) y el número
   de pistas encontradas. Esto va donde ahora solo esta el selector de tema.

7. **El volumen del fondo que hoy vive en la pantalla de biblioteca** se mueve a la sección
   "Sonido" de Ajustes (mas su propio deslizador). En la biblioteca no debe quedar.

## Restricciones que no se negocian

- **Sin permiso `INTERNET`** y sin dependencias de red.
- No toques `AGENTS.md` ni `ui/Dial.kt`.
- De `MainActivity.kt` solo lo mínimo para que lo nuevo sea alcanzable.
- Textos de interfaz en castellano con tildes, en `strings.xml`.
- **No traduzcas nombres reales de rutas** (`Music` es `Music`).
- **No hagas commits** y no dejes ficheros de prueba.
- **No intentes compilar** (en tu sandbox no hay JDK ni Android SDK): deja el código listo y
  di lo que no tengas claro.
- Cuidado con los encadenados sobre tipos anulables.
- Al terminar, responde con: ficheros creados y modificados, versión exacta de
  `androidx.documentfile` que uses (comprobada, no inventada), permisos que quedan en el
  manifest, y todo lo que no hayas podido hacer o no tengas claro.
