# Encargo 06 — Elegir el fondo de cada meditación

Lee `AGENTS.md`, `docs/PLAN_SERENA.md`, `docs/ENCARGO_04_registro_sesiones.md` y
`docs/ENCARGO_05_ritual_gong_preparacion.md` (contexto de lo que ya existe). Este encargo es
independiente: **no cambies nada del ritual de entrada y salida** del encargo 05.

## El problema

Hoy, al elegir una meditación guiada, **la app escoge el fondo sola** (el primero que encuentra
en la carpeta `fondo` de la biblioteca), sin preguntar. El usuario lo pide así: «¿cómo elijo el
fondo que se pone a cada meditación? Lo suyo sería que me pregunte: esta guiada, ¿con qué fondo?
ninguno, bosque…». Se da el caso de que algunos de sus ruidos no le sirven (uno tiene cantos de
pájaros que le sacan de la meditación) y ahora mismo no puede evitarlo.

## Comportamiento que hay que implementar

Cuando el usuario toca una meditación **guiada** (cualquier pista que no sea del grupo de fondos),
antes de arrancar la sesión aparece una pregunta: **«¿Con qué fondo quieres acompañarla?»** con:

1. **«Sin fondo»** como primera opción (la meditación suena sola).
2. **Cada fondo disponible** de la biblioteca, por su nombre visible (los ficheros de la carpeta
   `fondo`), en orden alfabético.
3. La opción elegida la última vez **preseleccionada**, para que confirmar sea un toque. Ese
   recuerdo se guarda donde ya se guardan las preferencias (`audio/ControlSesion.kt`). Si no hay
   ninguna guardada, se usa el **fondo por defecto** de Ajustes → Sonido si existe, y si no,
   «Sin fondo».
4. Un atajo para empezar de inmediato con lo preseleccionado (que no haya que pasar siempre por
   el diálogo).

Reglas:
- Si el usuario toca una pista del **grupo de fondos** (una sesión de solo ruido), **no se
  pregunta nada**: el fondo es la propia pista, como ahora.
- Si no hay ningún fondo en la biblioteca (o la carpeta `fondo` no existe), se arranca sin fondo
  y **sin diálogo**.
- Al confirmar, la sesión arranca con **ese** fondo (`fondo = null` si eligió «Sin fondo»), con
  la misma duración y el mismo volumen de fondo que ya se calculan hoy. **No cambies** la regla
  del temporizador: en una guiada manda la meditación y el dial es el tiempo mínimo.
- El diálogo tiene que respetar el tema oscuro y claro de la app y no tapar la lista con
  demasiadas opciones si mañana hay veinte fondos (que se pueda desplazar).

## Dónde

- La elección ocurre en el flujo que ya existe: `ui/PantallaBiblioteca.kt` y el punto donde
  `MainActivity.kt` llama a `ControlSesion.iniciar`. Si te resulta más limpio un estado
  compartido (como `ControlSesion`), adelante, pero dilo en el informe.
- Textos nuevos en `res/values/strings.xml`, en castellano **con tildes**.
- Revisa los textos que ya existen y **corrige cualquier falta de tilde** que veas en la
  interfaz de esta funcionalidad; el usuario es profesor y no las tolera.

## Restricciones que no se negocian

- **No toques `AGENTS.md` ni `ui/Dial.kt`**, ni `audio/Sonidos.kt`/`Cmpanas.kt`, ni la lógica de
  reproducción de `audio/Reproductor.kt`.
- **Sin permisos nuevos**, sin `INTERNET`, sin dependencias nuevas.
- **No hagas commits** y no dejes ficheros de prueba.
- **No intentes compilar** (en tu sandbox no hay Android SDK): deja el código listo.
- Al terminar responde con: ficheros creados y modificados, cómo has resuelto la
  preselección y el recuerdo, y todo lo que no tengas claro o no hayas podido hacer.
