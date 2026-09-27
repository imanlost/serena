# Serena

App Android personal y **sin conexión** para reproducir una biblioteca local de meditaciones
guiadas en castellano: temporizador por tipo de pista, sonido de fondo en bucle, ritual de gong y
registro de la práctica.

## Qué hace

- **Biblioteca local**: se elige la carpeta de audios con el selector del sistema y la app la
  recuerda. Árbol de carpetas navegable, duración de cada pista y marca en las que ya se han
  escuchado. Desde una carpeta de fondos, una pista suena sola.
- **Sesión guiada**: preparación con cuenta atrás, gong al empezar y al terminar, temporizador que
  cierra la voz cuando toca, fondo sonoro en bucle con su propia mezcla y pausas que devuelven sin
  perder el hilo.
- **Seguimiento**: racha actual y mejor racha, minutos totales, calendario de constancia, barras de
  las últimas ocho semanas y **diario de sesiones** agrupado por día (hora, meditación, minutos) con
  nota por sesión, corrección de minutos y borrado con confirmación. Se puede **añadir a mano** una
  sesión que no se registró.
- **Exportar el historial a CSV** desde Ajustes, al fichero que elija el usuario.
- **Cuatro paletas de color** (Salvia, Lavanda, Ámbar y Océano) con tema claro, oscuro o el del
  sistema, recordado entre arranques.

Tres criterios del proyecto, que no son detalles:

- **Sin permiso de `INTERNET`**: sin cuentas, sin telemetría, sin analítica. Se comprueba sobre el
  APK compilado (`aapt dump permissions`), no sobre el manifiesto fuente.
- **Sin permisos de almacenamiento**: la carpeta de audios la elige el usuario con el selector del
  sistema (SAF, permiso persistente) y la app la recuerda entre reinicios.
- **El audio no está en el repositorio**: la app lee la carpeta elegida en el teléfono, así que
  aquí solo hay código. Los audios de terceros y las grabaciones propias se quedan fuera.

## Pendiente

- **Health Connect**: previsto (escribir cada sesión como `MindfulnessSessionRecord`), **todavía no
  implementado**. El registro local es la fuente de verdad.
- **Importar** el historial (hoy solo se exporta a CSV), un atajo para repetir una meditación desde
  el diario y un widget.

## Cómo se compila

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew :app:assembleDebug
```

Toolchain anclado (no se sube sin necesidad): AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9,
compileSdk/targetSdk 35, minSdk 26. La ruta del SDK de Android va en `local.properties`
(`sdk.dir=...`), que no se versiona.

## Pruebas

```bash
./gradlew :app:testDebugUnitTest
```

## Cómo se trabaja en este repositorio

- `AGENTS.md` — reglas del repositorio (toolchain anclado, sin red, textos en castellano con
  tildes).
- `docs/PLAN_SERENA.md` — plan técnico: alcance, esquema de datos, fases y criterios de «hecho».
- `docs/ENCARGO_*.md` — encargos de trabajo cerrados, con lo que pedía cada bloque.

## Licencia

**GPL-3.0** (ver `LICENSE`).
