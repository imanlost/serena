# Encargo 04 — Registro de las sesiones (datos reales en la app)

Lee `AGENTS.md`, `docs/PLAN_SERENA.md` y `docs/ENCARGO_03_biblioteca_elegible.md` (contexto de lo
que ya existe). Este encargo es la **capa de datos** que hoy no existe: la app reproduce, pero
no guarda nada, por eso Progreso y el resumen de Hoy están a cero con textos y datos falsos.

Estado de partida que debes conocer:
- La sesión la gestiona `audio/ServicioSesion.kt` (un `MediaSessionService`) y arranca desde
  `audio/ControlSesion.kt`. El estado que ve la interfaz es `ControlSesion.estado`
  (`EstadoSesion`: activa, pausada, finalizando, restanteMs, totalMs, titulo).
- `ui/Graficos.kt` ya tiene los composables de calendario, barras por semana y tarjeta de
  racha, y `res/values/strings_graficos.xml` sus textos. **Hoy no se usan**: están escritos
  esperando estos datos. Léelos antes de tocar nada y conéctalos; si alguna firma no encaja,
  cámbiala y dilo en el informe.
- En `MainActivity.kt`, `PantallaHoy` muestra `R.string.resumen_hoy` con ceros y
  `PantallaProgreso` muestra ceros con `R.string.sin_datos`.

## Tareas

1. **Base de datos local con Room** (una sola tabla, `sesiones`), campos:
   `id`, `inicioMs` (epoch), `finMs` (epoch), `duracionMs` (tiempo REAL escuchado),
   `pista` (titulo), `grupo` (guiada/fondo/podcast), `completada` (boolean: llego al final),
   `nota` (texto opcional), `idSalud` (texto opcional, para el identificador de Health Connect
   en el encargo siguiente: déjalo ya en el esquema y sin usar).
   Comprueba las versiones reales de `room-runtime`, `room-ktx` y del plugin KSP compatibles
   con **Kotlin 2.0.21 y compileSdk 35** (mira el `gradle/libs.versions.toml` o las versiones
   del `build.gradle.kts`; no inventes números: si no puedes comprobarlo, dilo).

2. **Registrar la sesión al terminar**, en `ServicioSesion.finalizar()`: se guarda la sesión
   con el tiempo realmente escuchado (`totalMs - restanteMs`), el titulo de la pista, su grupo
   y si se completo. **Si el usuario corta antes, se guarda igual** con `completada = false`
   y la duración escuchada. Nada de escribir desde la interfaz: el servicio es el único que
   sabe cuanto se ha escuchado de verdad.

3. **Reglas de racha (estrictas, ya acordadas)**:
   - Una sesión cuenta para la racha si su `duracionMs` es **igual o mayor a 60 segundos**.
   - La racha son **días naturales consecutivos** con al menos una sesión que cuente; si hoy
     todavía no hay sesión, la racha sigue viva desde ayer (no se rompe hasta que pase el día
     entero sin practicar).
   - También hay que calcular la **mejor racha histórica** y los **minutos totales**.

4. **Consultas que necesitas** (en el DAO): racha actual, mejor racha, minutos totales,
   minutos por semana de las ultimas 8 semanas, y por día del mes para el calendario de
   constancia (con el nivel de intensidad por minutos).

5. **Conectar la interfaz a los datos reales**:
   - `PantallaProgreso` (en `MainActivity.kt`): usa `ui/Graficos.kt` con datos reales: tarjeta
     de racha, calendario de constancia y minutos por semana. Cuando no haya datos, el mensaje
     `R.string.sin_datos` y no gráficos vacíos.
   - El resumen de `PantallaHoy` (`R.string.resumen_hoy`, que ya tiene el formato
     `%1$d dias seguidos · %2$d minutos`): racha actual y minutos totales de verdad.
   - Los datos se leen de la base de datos en un `StateFlow`/consulta reactiva, de forma que al
     terminar una sesión la interfaz se actualice sola al volver.

6. **Rendimiento**: nada de consultar la base de datos en el hilo principal ni en cada
   recomposición.

## Restricciones que no se negocian

- **Sin permiso `INTERNET`** y sin dependencias de red.
- **No toques `AGENTS.md` ni `ui/Dial.kt`**. De `MainActivity.kt` y `ui/Graficos.kt` solo lo
  necesario para conectar los datos.
- No cambies la lógica de reproducción, de la biblioteca ni del temporizador que ya funciona:
  este encargo solo añade el registro.
- Textos de interfaz en castellano con tildes, en `strings.xml` (o `strings_graficos.xml`).
- **No hagas commits** y no dejes ficheros de prueba.
- **No intentes compilar** (en tu sandbox no hay JDK ni Android SDK): deja el código listo.
- Al terminar, responde con: ficheros creados y modificados, versiones EXACTAS de Room y KSP
  que hayas puesto (comprobadas), y todo lo que no hayas podido hacer o no tengas claro.
