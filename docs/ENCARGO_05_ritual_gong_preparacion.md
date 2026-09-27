# Encargo 05 — Ritual de entrada y salida de la sesión (gong y preparación)

Lee `AGENTS.md`, `docs/PLAN_SERENA.md` y `docs/ENCARGO_04_registro_sesiones.md` (contexto). Ahora
mismo una sesión arranca de golpe (la voz y el fondo empiezan a la vez, sin aviso) y al terminar
solo hay 5 segundos de desvanecido y una campana aguda. Falta el ritual: **prepararse al empezar
y volver a la realidad al acabar**. Esto lo pide el usuario con estas palabras: «echo en falta
gong de inicio, 5-10 seg para preparar, eso como base en todas las meditaciones para hacer unas
respiraciones de preparación, y quizá al finalizar también, algo más larga la pausa para volver
a la realidad».

## Comportamiento que hay que implementar

**Al arrancar una sesión** (en `audio/ServicioSesion.kt`, `iniciar`):
1. Se publica el estado con la sesión activa **en fase de preparación**.
2. Suena el **gong de apertura**.
3. Silencio (ni voz ni fondo) durante los **segundos de preparación** (por defecto **10**),
   que la pantalla de sesión muestra como cuenta atrás («Prepárate»).
4. Terminada la preparación: arrancan la voz y el fondo y **arranca el temporizador**. Los
   segundos de preparación **no cuentan** como práctica: `duracionMs` sigue midiendo solo lo
   escuchado, y el temporizador empieza aquí, no antes.
5. Caso borde: si el usuario corta **durante la preparación**, la sesión se cierra sin
   registrar nada (no ha habido práctica) y sin gong de cierre.

**Al terminar la sesión** (en `finalizar`):
1. Suena el **gong de cierre** (marca el final de la práctica).
2. **Pausa de vuelta**: el fondo sigue sonando y se desvanece a lo largo de los **segundos de
   cierre** (por defecto **30**, ahora son 5 fijos en `FADE_SALIDA_MS`). Después se para todo.
   Esa pausa tampoco cuenta como práctica.
3. El registro en la base de datos se hace **igual que ahora**, con los minutos de práctica.

**Ajustes → Sonido** (en `ui/PantallaAjustes.kt`): tres ajustes nuevos, guardados donde ya se
guardan las preferencias de la app (`audio/ControlSesion.kt`):
- Segundos de preparación: 0, 5, 10, 15, 20, 30 (por defecto **10**).
- Segundos de vuelta al terminar: 0, 10, 20, 30, 45, 60 (por defecto **30**).
- Gong: sí/no (por defecto **sí**). Si está apagado, no suena ni al empezar ni al acabar
  (la preparación y la pausa se mantienen).

## El gong: se sintetiza, no se empaqueta

El APK no lleva contenido de audio (criterio del proyecto) y la campana actual ya se sintetiza
en `audio/Cmpanas.kt` con `AudioTrack`. Pero **esa campana no sirve como gong**: es un tono
agudo de 660 Hz. Un gong es grave y con cola larga. Añade un gong con estas características:

- **Renombra** el fichero y el objeto a `audio/Sonidos.kt` / `object Sonidos` (el nombre
  `Cmpanas` es un error mío y no lleva tilde), con dos funciones: `campana()` (el sonido actual,
  que se conserva) y `gong()`.
- `gong()`: fundamental grave de **110 Hz** más parciales **inarmónicos** (no múltiplos
  exactos, que es lo que da el timbre metálico): factores aproximados 1.0, 2.37, 3.61, 5.12,
  6.94, 8.6, con amplitudes decrecientes. Cada parcial con su propio decaimiento exponencial,
  **más rápido cuanto más agudo** (los agudos se apagan antes; el fundamental dura toda la
  cola). Ataque rápido (2-3 ms) y cola total de **unos 6 segundos**, con un desvanecido final
  suave para que no chasquee al cortar.
- Volumen prudente: es un aviso, no un sobresalto. Que no sature (deja margen) y que no
  reviente al empezar la voz después.
- Se reproduce en un hilo propio, como la campana actual, sin bloquear el hilo principal. El
  servicio tiene que **esperar la duración real del gong** antes de arrancar la meditación
  (como ya hace con `Cmpanas.DURACION_MS`), y esa espera **se suma** a los segundos de
  preparación.

## Restricciones que no se negocian

- **No toques `AGENTS.md` ni `ui/Dial.kt`**, ni la lógica de reproducción de
  `audio/Reproductor.kt` (los dos reproductores y los desvanecidos funcionan; solo cambia
  **cuándo** se llaman).
- **Sin permisos nuevos**, sin `INTERNET`, sin dependencias nuevas.
- Textos de interfaz en castellano **con tildes**, en `strings.xml`.
- **No hagas commits** y no dejes ficheros de prueba.
- **No intentes compilar** (en tu sandbox no hay Android SDK): deja el código listo.
- Al terminar responde con: ficheros creados y modificados, los valores por defecto que has
  puesto, y todo lo que no tengas claro o no hayas podido hacer.
