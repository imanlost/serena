# Serena — reglas de trabajo en este repositorio

App Android personal para reproducir una biblioteca local de meditaciones guiadas en castellano,
con registro de la práctica, racha y estadísticas. Sin conexión, sin cuenta, sin telemetría.
Paquete `com.imanlost.serena`.

## Contexto que debes leer antes de tocar nada

- `docs/PLAN_SERENA.md` — plan técnico completo: alcance, esquema de datos, fases y criterios
  de "hecho". **Es el contrato**: si tu trabajo contradice el plan, dilo en la respuesta final
  en lugar de improvisar.
- `AGENTS.md` (este fichero) — reglas del repositorio. **No lo modifiques.**

## Decisiones ya tomadas (no las cambies sin decirlo)

- **Toolchain anclado**: AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9, compileSdk/targetSdk 35,
  minSdk 26, JDK 21 para compilar. **No subas versiones de AGP ni de Kotlin.** (AGP 9.x
  integra Kotlin y prohíbe aplicar el plugin `org.jetbrains.kotlin.android`; migrar es una
  tarea aparte.)
- **Dependencias ancladas al SDK 35**: Compose BOM 2024.10.01, activity-compose 1.9.3,
  lifecycle 2.8.7, core-ktx 1.15.0.
- **Sin permiso de INTERNET en el manifest.** Es un criterio de aceptación, no un detalle:
  si tu cambio necesita red para algo, la respuesta es "no" y lo dices.
- **Racha estricta**: se rompe si un día no hay práctica. Una sesión cuenta a partir de
  **60 segundos**. Sin congelaciones por defecto.
- **Salud (pendiente)**: está previsto escribir también cada sesión en Health Connect, pero **hoy
  no está implementado**; el registro local es la fuente de verdad y no se ha de asumir lo contrario.
- **Textos de la interfaz en castellano**, con tildes correctas. Nada de textos en inglés.
- **Estética**: Material 3, paleta propia (`ui/Tema.kt`), modo claro y oscuro diseñados por
  separado, sin transparencias. Botones grandes y alcanzables con una mano.

## Cómo se compila (lo hace Hermes, no tú)

```bash
cd <ruta-del-repositorio>
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew assembleDebug
```

**Dentro de tu sandbox no hay JDK ni Android SDK**: no intentes compilar. Tu trabajo es dejar
el código correcto y listo; la compilación y la prueba en el móvil las hace Hermes y son la
única verificación válida. Si dudas de si algo compila, dilo explícitamente.

La versión de publicación (firmada, `assembleRelease`) se compila solo donde está el almacén de
claves: no es tu tarea.

## Reglas del repositorio

- **No borres, muevas ni sobrescribas** nada sin decirlo. Pregunta antes.
- Trabaja **solo dentro de este directorio**. Fuera de aquí no hay nada tuyo.
- **No dejes residuos**: si creas ficheros de prueba, bórralos al terminar.
- **No hagas commits** salvo que el encargo lo pida expresamente.
- No añadas dependencias nuevas sin justificarlo en la respuesta final y sin comprobar que la
  versión existe y es compatible con compileSdk 35.
- Comenta el código en castellano, breve y con el motivo (no lo que hace, sino por qué). Los
  comentarios del código van sin tildes, como el resto de ficheros `.kt`; los textos de la
  interfaz, con ellas.

## Pitfalls conocidos en este proyecto

- **Encadenados sobre tipos anulables**: `x?.trim().ifEmpty { null }` no compila (el segundo
  encadenado necesita `?.`). Revísalo antes de dar algo por terminado.
- **Lint en la versión de publicación**: `lintVitalRelease` corre solo al compilar la release y
  **aborta el build**. Un recurso declarado únicamente en `values-night` (o solo en `values`) sin
  valor en la otra carpeta lo hace fallar, y con razón: puede provocar un fallo en tiempo de
  ejecución. El mismo nombre debe existir en las dos.
- El alcance de las variables de Compose en pantallas grandes: usa `rememberSaveable` para lo
  que deba sobrevivir a un giro de pantalla.
