# Encargo 02 — Reproducción y biblioteca

Bloque del plan: secciones 3, 4 y 6 de `docs/PLAN_SERENA.md`.
Antes de empezar: lee `AGENTS.md` y `docs/PLAN_SERENA.md`.

## Objetivo del bloque

Que la app reproduzca las meditaciones guiadas con un temporizador y una pista de sonido
de fondo en bucle. Nada de registro ni estadísticas todavía: eso es el bloque siguiente.

## Tareas

1. **Modelo de pista**: `data class Pista(id: Long, titulo: String, ruta: String,
   duracionMs: Long, grupo: String)`. El `grupo` sale de la subcarpeta donde este el
   fichero (por ejemplo `guiadas`, `fondo`, `podcasts`).

2. **Escaneo de la biblioteca local**: carpeta `Serena` dentro de la música del
   dispositivo. Usa `MediaStore.Audio` en API 29+ y `File` en API 26-28. Titulo = nombre
   de fichero sin extension y sin el prefijo numérico (`01_`, `02_`...). Duración real
   leída del propio fichero, no estimada.

3. **Permisos de audio** en el manifest, con el criterio de siempre (el mínimo necesario):
   `READ_MEDIA_AUDIO` (API 33+) y `READ_EXTERNAL_STORAGE` con `maxSdkVersion="32"`.
   Se piden en tiempo de ejecución **solo al abrir la biblioteca**, nunca al arrancar.

4. **Pantalla Biblioteca**: lista de pistas con título y duración (`mm:ss`), agrupadas por
   grupo, sin carátulas ni imágenes. Tocar una pista abre la pantalla de sesión con el dial
   ya cargado en la duración de esa pista (o en 10 minutos si no se conoce).

5. **Reproductor con media3 ExoPlayer**: **dos** reproductores en paralelo, la voz al
   100 % y el fondo entre el 15 % y el 35 % (valor configurable con un ajuste simple).
   Comprueba en Google Maven (https://dl.google.com/dl/android/maven2/androidx/media3/media3-exoplayer/maven-metadata.xml)
   que la versión que uses existe de verdad y es compatible con `compileSdk 35`. **Dilo en
   la respuesta final**: versión exacta y de donde la has sacado. No inventes versiones.

6. **Fondo en bucle sin costura**: `LoopMode.ALL` infinito. Entra con un desvanecido de
   3 segundos al empezar la sesión y sale con un desvanecido de 5 segundos al terminar.
   Nunca un corte seco.

7. **Temporizador**: la sesión dura los minutos que marque el dial. Al agotarse: desvanecido
   del fondo (5 s), parada de la voz y una campana suave de cierre antes de salir.

8. **Servicio en primer plano**: para que el audio siga con la pantalla apagada, usa el
   `MediaSessionService` de media3. Declara `FOREGROUND_SERVICE` y
   `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, y el tipo `mediaPlayback` en el servicio.

9. **Pantalla de sesión**: tiempo restante grande, un anillo de progreso y dos botones
   (pausa y terminar). Deliberadamente desnuda: nada más.

## Restricciones que no se negocian

- **Prohibido el permiso `INTERNET`** y cualquier dependencia que pida red.
- No toques `AGENTS.md`.
- No modifiques `ui/Dial.kt`: el dial ya está afinado. De `MainActivity.kt` solo lo mínimo
  para que la pantalla de sesión y la biblioteca sean alcanzables.
- Textos de interfaz en castellano con tildes, en `strings.xml`.
- **No hagas commits**.
- No dejes ficheros de prueba ni residuos.
- **No intentes compilar**: dentro de tu sandbox no hay JDK ni Android SDK. Deja el código
  correcto; la compilación y la prueba en el móvil las hace Hermes.
- Cuidado con los encadenados sobre tipos anulables (`x?.trim().ifEmpty { null }` no compila).
- Si algo del plan no encaja con lo que encuentras en el código, dilo en la respuesta final
  en lugar de improvisar.

## Respuesta final que espero (stdout)

1. Ficheros creados y ficheros modificados, con su ruta.
2. Versión exacta de media3 y comprobación de que existe.
3. Permisos añadidos al manifest (lista literal).
4. Que NO has podido hacer, o que dudas tienes. Mejor decir "esto no lo tengo claro" que
   entregar algo que parezca correcto y no lo sea.
