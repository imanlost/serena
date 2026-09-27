# Serena — Plan técnico de la v1 (uso personal)

> Documento para aprobar antes de escribir código. Cambiar el diseño después del primer
> build cuesta el doble; cambiarlo aquí cuesta cero.

**Objetivo:** una app Android propia, en castellano y sin conexión, que reproduzca la biblioteca
local de meditaciones guiadas (con sonido de fondo en bucle), registre cada sesión y muestre
racha, minutos y calendario de práctica. **Health Connect está previsto y aún no implementado.**

**Decisiones ya cerradas** (aportadas por el usuario):

| Tema | Decisión |
|---|---|
| Nombre | Serena |
| Paquete | `com.imanlost.serena` |
| Health Connect | Sí, entra en la v1 |
| Criterio de racha | Estricta (un día sin sesión la rompe); sin congelaciones automáticas |
| Red | La app NO pide permiso de INTERNET. Cero cuentas, cero telemetría |
| Contenido | No se empaqueta audio en el APK: la app lee una carpeta del dispositivo |
| Repo Git | Privado en GitHub; público más adelante |

**Dispositivo de referencia:** un móvil real con **Android 16 / API 36**.
Health Connect viene integrado en el sistema a partir de Android 14; en versiones anteriores se
instala su aplicación aparte.

---

## 1. Alcance de la v1

Dentro:

1. **Biblioteca**: lista los audios de una carpeta elegida por el usuario (guiadas + fondos), con
   agrupación por tipo, duración real y marca de "ya escuchado".
2. **Reproductor**: reproducción de la meditación elegida + **fondo en bucle** (naturaleza o
   meditativo generado) que suena por debajo, con desvanecido de entrada y salida.
3. **Temporizador de sesión**: objetivo de minutos configurable, campana de inicio y de fin,
   campana intermedia opcional, botón de +5 minutos al terminar (patrón Insight Timer).
4. **Registro**: cada sesión queda guardada (inicio, fin, duración, pista, si se completó);
   alta manual de sesión (fecha, minutos, tipo) para lo meditado sin el teléfono.
5. **Estadísticas**: racha actual, racha máxima, minutos totales, número de sesiones, media por
   sesión, calendario mensual de práctica, minutos de cada uno de los últimos 7 días.
6. **Health Connect**: escribe cada sesión como `MindfulnessSessionRecord`.
7. **Ajustes**: carpeta de audio, volumen relativo del fondo, campanas, objetivo diario, exportar
   e importar los datos en JSON.

Fuera de la v1 (para no desmadrar el alcance): podcasts y RSS, cuentas o sincronización, widget,
contenido propio o TTS, gráficas más allá del calendario y las barras de 7 días, material design
dinámico por colores del sistema.

---

## 2. Stack y versiones (comprobadas hoy en Google Maven)

- Kotlin 2.0.21 + AGP 8.7.3 + Gradle 8.9 (wrapper), combinación ya probada en dispositivo real.
- `compileSdk`/`targetSdk` **36** (móvil Android 16), `minSdk` 26, `jvmTarget` 17.
- Compose BOM 2024.10.01 + Material 3 (tema propio en tonos pastel, sin transparencias).
- `androidx.media3:media3-exoplayer:1.11.1` y `media3-session:1.11.1` (reproducción + controles).
- `androidx.room:room-runtime:2.8.5` + `room-ktx` + KSP (registro de sesiones).
- `androidx.datastore:datastore-preferences:1.2.1` (ajustes).
- `androidx.health.connect:connect-client:1.1.0` (Health Connect estable).
- `androidx.activity:activity-compose:1.13.0`, `lifecycle-viewmodel-compose`.
- Toolchain: un **JDK 21** en `JAVA_HOME` y el SDK de Android con su ruta en
  `sdk.dir` dentro de `local.properties` (ese fichero no se versiona). **Hay que instalar**
  `platforms;android-36` y `build-tools;36.0.0` con `sdkmanager` (hoy solo está el 35).

Si alguna versión rompe el build por incompatibilidad con AGP 8.7.3, la tarea 1.2 incluye el
criterio de ajuste (subir AGP o bajar la librería), no se deja "a ver si compila".

---

## 3. Estructura del proyecto

```
<raiz del repositorio>/
├── settings.gradle.kts, build.gradle.kts, gradle.properties, local.properties
├── gradle/wrapper/{gradle-wrapper.properties,gradle-wrapper.jar}, gradlew
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/imanlost/serena/
        │   ├── SerenaApp.kt                  application + contenedor de dependencias
        │   ├── MainActivity.kt               única Activity (Compose)
        │   ├── datos/
        │   │   ├── Sesion.kt                 entidad Room
        │   │   ├── SesionDao.kt              consultas (racha, minutos, calendario)
        │   │   ├── SerenaDb.kt               base de datos + migraciones
        │   │   └── Ajustes.kt                DataStore (carpeta, campanas, objetivo)
        │   ├── audio/
        │   │   ├── Biblioteca.kt             escaneo de la carpeta de audio
        │   │   ├── Reproductor.kt            Media3: pista + fondo en bucle + fade
        │   │   └── Cmpanas.kt                campana de inicio/fin/intermedia
        │   ├── salud/
        │   │   └── HealthConnect.kt          escritura de MindfulnessSessionRecord
        │   ├── estadisticas/
        │   │   └── Calculos.kt               racha actual/máxima, totales, series
        │   └── ui/
        │       ├── Tema.kt                   colores pastel, tipografía, sin alpha
        │       ├── PantallaBiblioteca.kt
        │       ├── PantallaReproductor.kt
        │       ├── PantallaEstadisticas.kt
        │       ├── PantallaAjustes.kt
        │       └── Componentes.kt            tarjetas, barras de 7 días, calendario
        └── res/  (icono adaptive, colores, strings.xml en castellano)
```

---

## 4. Modelo de datos

Una sola tabla evita desincronizaciones; los agregados se calculan con consultas.

```kotlin
@Entity(tableName = "sesiones")
data class Sesion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inicioMs: Long,          // epoch millis
    val finMs: Long,
    val duracionSeg: Int,        // duración efectiva practicada
    val audio: String?,          // fichero o null si es sesión manual
    val fondo: String?,          // pista de fondo usada, o null
    val tipo: String,            // guiada | temporizador | respiracion | manual
    val completada: Boolean,     // llegó al final del objetivo
    val nota: String? = null,
    val healthConnectId: String? = null   // id devuelto por Health Connect al escribir
)
```

Consultas clave (en `SesionDao`):

- `minutosTotales`, `numSesiones`, `mediaPorSesion`: agregados SQL simples.
- `diasConPractica`: `SELECT DISTINCT date(inicioMs/1000,'unixepoch','localtime')` → base del calendario.
- Racha: se calcula en `Calculos.kt` con la lista de días ordenada descendentemente (racha actual =
  días consecutivos desde hoy o ayer; racha máxima = mayor secuencia histórica). Se hace en Kotlin
  y no en SQL porque la lógica de "hoy cuenta, ayer tolera el día en curso" es más clara y testeable así.
- Serie de 7 días para las barras: minutos agrupados por día, últimos 7.

Regla estricta: una sesión cuenta para la racha si `duracionSeg >= 60` (evita que un toque
accidental mantenga la racha viva). Umbral en ajustes.

---

## 5. Audio: reproducción y fondo en bucle

- Lista de pistas: escaneo de la carpeta elegida con `DocumentFile`/SAF (árbol persistente) o
  `MediaStore` filtrando por carpeta. Decisión: **SAF con permiso persistente** sobre la carpeta de
  audios que hay en el teléfono (evita permisos de almacenamiento amplios y funciona en las versiones
  de Android donde `READ_MEDIA_AUDIO` solo alcanza lo indexado).
  El recorrido es **recursivo** (tope de 8 niveles): el grupo de cada pista es la ruta relativa de
  su carpeta contenedora (`guiadas`, `fondo`, `curso/nivel 2`) y la raíz es `sueltas`. La
  duración se lee con 6 lecturas en paralelo y se cachea en memoria, porque la pantalla de Audios
  vuelve a escanear cada vez que se entra y la biblioteca puede tener cientos de audios en
  la tarjeta SD.
- Reproductor: un `ExoPlayer` para la voz y **otro para el fondo**, con `setVolume` distinto
  (voz 1.0, fondo configurable 0,15–0,35 por defecto).
- **Fondo en bucle con corte por duración** (requisito explícito del usuario): el fondo se activa
  en bucle y termina con un desvanecido calculado, no con un corte seco:
  - al arrancar: fade-in de 3 s;
  - al terminar la sesión (o al pulsar parar): fade-out de 5 s y parada;
  - si la sesión dura más que la pista, el bucle continúa sin costura.
  Se implementa con un `ValueAnimator` sobre el volumen o con `VolumeShaper` de Media3.
- Campanas: tono corto generado o fichero propio, reproducido en un tercer canal para no
  mezclarse con el fondo.
- Reproducción en segundo plano: `MediaSessionService` + notificación (foreground service de tipo
  `mediaPlayback`), para que no se corte al apagar la pantalla. Es lo único que exige permiso de
  primer plano, no de Internet.

---

## 6. Health Connect

- Permiso en el manifiesto: `android.permission.health.WRITE_MINDFULNESS`.
- Comprobación previa: `HealthConnectClient.getSdkStatus()`; si no está disponible, el botón de
  conexión se muestra desactivado con explicación, sin romper nada.
- Escritura: al cerrar una sesión, `MindfulnessSessionRecord` con `startTime`, `endTime`,
  `startZoneOffset`, `endZoneOffset`, tipo `MINDFULNESS_SESSION_TYPE_MEDITATION` (o `_UNGUIDED` si
  es temporizador), `title` con el nombre de la pista y `notes` opcional.
- El `clientRecordId` se guarda en la tabla (`healthConnectId`) para no duplicar al reescribir.
- Si el usuario deniega el permiso, la app funciona igual: el registro local es la fuente de verdad
  y Health Connect es un extra, nunca al revés.

---

## 7. Tareas, en orden (cada una con su criterio de "hecho")

### Fase 0 — Preparar el entorno (una sesión corta)
1. Instalar en el SDK `platforms;android-36` y `build-tools;36.0.0`.
   *Hecho:* `ls $ANDROID_HOME/platforms` muestra `android-36`.
2. Crear el proyecto (settings/build/gradle wrapper) con `local.properties` apuntando al SDK.
   *Hecho:* `./gradlew :app:assembleDebug --no-daemon` genera un APK, aunque solo muestre "Serena".
3. Instalarlo en el móvil por ADB y abrirlo.
   *Hecho:* la pantalla en blanco con el nombre aparece en el dispositivo real.

### Fase 1 — Biblioteca y reproducción
4. `Biblioteca.kt`: escaneo de carpeta vía SAF persistente.
   *Hecho:* la lista muestra las 10 pistas con su duración real y no muestra avisos de permiso.
5. `PantallaBiblioteca.kt`: lista agrupada (Guiadas / Fondos / Podcasts) con duración y "escuchado".
   *Hecho:* se ve en el móvil con nombres en castellano y sin cortes de texto.
6. `Reproductor.kt` + `PantallaReproductor.kt`: reproducir una pista con play/pausa y barra de
   progreso.
   *Hecho:* suena por el altavoz y por auriculares, y sobrevive al apagado de pantalla.
7. Fondo en bucle con fade-in/fade-out y control de volumen relativo.
   *Hecho:* con el fondo al 25 %, se oye la voz claramente por encima y el fondo no corta en seco
   al terminar.

### Fase 2 — Registro y estadísticas
8. Entidad + DAO + base de datos Room, con consultas verificadas por un test unitario de la racha.
   *Hecho:* `CalculosTest` pasa con los casos: sin sesiones, un día, 5 días seguidos, hueco de un
   día, dos sesiones el mismo día (cuenta una vez).
9. Registrar la sesión al terminar y ofrecer alta manual.
   *Hecho:* aparece en la lista de días del calendario con los minutos correctos.
10. `PantallaEstadisticas.kt`: racha actual y máxima, minutos, sesiones, media, calendario y barras.
    *Hecho:* los números cuadran con el CSV exportado (comprobación cruzada).

### Fase 3 — Health Connect y exportación
11. `HealthConnect.kt` con permiso, escritura y control de disponibilidad.
    *Hecho:* la sesión aparece en Health Connect del móvil con la hora y duración correctas.
12. Exportar/importar JSON.
    *Hecho:* exportar, borrar datos y reimportar deja las estadísticas idénticas.

### Fase 4 — Afinado y entrega
13. Tema, icono adaptive, textos definitivos en castellano, revisión de contraste.
    *Hecho:* captura revisada en pantalla real a pleno sol y en modo oscuro.
14. Ajustes completos (carpeta, umbral de racha, objetivo diario, campanas, volumen del fondo).
15. APK de release firmado y prueba de uso real de una semana.
    *Hecho:* el APK instala en el móvil, los datos sobreviven a la actualización y no pide
    Internet en el manifest.

---

## 8. Verificación

- Build limpio: `./gradlew clean :app:assembleDebug --no-daemon`.
- Test de la lógica de racha: `./gradlew :app:testDebugUnitTest`.
- Prueba en **dispositivo real** (Android 16) además del emulador antes de
  entregar: un build limpio no garantiza que funcione en pantalla.
- Comprobación de privacidad: `aapt2 dump permissions` (o revisar el manifest) — no debe aparecer
  `android.permission.INTERNET` ni ningún SDK de analítica.
- Comprobación cruzada de estadísticas contra `docs/` exportado.

## 9. Riesgos y trampas conocidas

- **Permisos de audio en Android 16**: no pedir almacenamiento amplio; SAF sobre la carpeta concreta.
- **Room 2.8.5 con KSP**: el plugin KSP debe coincidir con la versión de Kotlin (2.0.21-1.0.28).
- **Media3 en segundo plano**: sin `MediaSessionService` con notificación, Android mata el audio.
- **Health Connect**: si el permiso se deniega o el servicio no está, la app debe seguir funcionando;
  nunca condicionar el registro local a Health Connect.
- **Nada de transparencias ni de decimales en la interfaz**: son requisitos puntuales de otros
  proyectos del usuario, no de este; aquí se aplican solo si él lo pide.
- **Herramientas**: `patch` bloquea config.yaml/.env (no aplica aquí); en el build Android hay que
  exportar JAVA_HOME a 21 antes de Gradle o el build falla con el Java 8 del sistema.

## 10. Experiencia, estética y adherencia (requisitos añadidos por el usuario)

Petición explícita: estética cuidada y minimalista, modo claro/oscuro, recordatorio por
notificación a la hora elegida, **diales para fijar la duración**, y todo lo que ayude a crear
hábito. Diseño de la v1 con ese objetivo:

### 10.1 Estética e interfaz
- Material 3 con **tema propio**: claro y oscuro diseñados por separado (no invertir colores),
  siguiendo el sistema por defecto y con opción manual (sistema / claro / oscuro) en ajustes.
- Paleta sobria de fondos y un único color de acento; tipografía con jerarquía clara; aire entre
  bloques; sin transparencias ni sombras decorativas. Nada de iconos de relleno.
- **Dial principal**: selector circular de minutos (rueda que se arrastra) para fijar la duración,
  con pasos rápidos (5, 10, 15, 20, 30) y ajuste fino; en la misma pantalla, un segundo dial para
  la mezcla del fondo (voz/fondo). Es el gesto central de la app: elegir cuánto y empezar.
- Pantalla de sesión deliberadamente desnuda: tiempo restante grande, un anillo de progreso y
  pausa/terminar. La pantalla se atenúa y no muestra nada más.
- Botones grandes y alcanzables con una mano; la app debe poder usarse sin mirar (campanas).

### 10.2 Adherencia: qué funciona de verdad y qué no
Lo que se implementa, con el motivo:
1. **Sesión mínima de 1 minuto que cuenta para la racha** (umbral ya previsto en 60 s). El mayor
   enemigo del hábito es el "todo o nada": un día malo se convierte en una semana perdida. Poder
   salvar el día con un minuto real es lo que mantiene la cadena viva.
2. **Anclaje a una rutina existente**: al configurar el recordatorio, la app pregunta "¿después de
   qué sueles hacer esto?" (el café, la ducha, llegar del trabajo) y el aviso lo menciona. Es
   implementación de intenciones (Gollwitzer): anclar a una señal existente funciona bastante mejor
   que "medita a las 8".
3. **Un solo recordatorio diario, amable y específico**, con acciones en la propia notificación:
   "Empezar 10 min" y "Más tarde" (1 h). Sin mensajes culpabilizantes ni avisos repetidos: las
   notificaciones insistentes se acaban desactivando y matan el hábito.
4. **Progreso visible**: racha actual, mejor racha y total de minutos siempre a la vista en la
   pantalla principal, más calendario mensual. Lo que se ve, se mantiene.
5. **Antídoto contra el "ya la he roto"**: tras perder la racha, la app muestra el total acumulado
   y la mejor racha en lugar de un cero rotundo. Romper un día no debe parecer empezar de cero.
6. **Hitos discretos** (1, 7, 21, 30, 50, 100, 365 días): un aviso breve al alcanzarlos, sin
   monedas, puntos ni confeti.
7. **Cierre de sesión con resumen**: al acabar, tres datos (duración, total acumulado, racha) y
   nada más.
8. **Fondo recordado por pista**: si elegiste lluvia, la próxima vez que abras esa meditación
   vuelve con lluvia y al volumen que fijaste. Menos fricción.
9. **Respiración de un minuto** accesible desde la notificación o el atajo: un recurso para
   momentos de estrés, que además hace que la app se abra fuera del ritual diario.
10. **Silencio durante la sesión**: la app no muestra notificaciones propias mientras suena, y se
    ofrece activar "No molestar" (permiso `ACCESS_NOTIFICATION_POLICY`, opcional y explicado).
11. **Widget de una pulsación** (fase final, RemoteViews puros, nunca Glance por sus problemas de
    clics): racha + botón "meditar 10 min" que abre la sesión directamente.
12. **Exportación de datos** en JSON/CSV: la sensación de que tus datos son tuyos sostiene el uso a
    largo plazo más que cualquier recompensa artificial.

Lo que **no** se implementa, y por qué: puntos, monedas, niveles, rankings o comparación social. La
gamificación agresiva empuja a usar la app para alimentar el marcador y no para meditar, y desplaza
la motivación intrínseca (el motivo por el que meditas) hacia la extrínseca. Aquí no hay cuentas ni
competición: el único marcador es tu constancia real.

### 10.3 Notificaciones: cómo y cuándo
- Permiso `POST_NOTIFICATIONS` (Android 13+) pedido con explicación, no al arrancar en frío: se
  pide cuando el usuario configura el recordatorio.
- Recordatorio diario con `AlarmManager` en modo inexacto (no justifica un permiso de alarmas
  exactas ni gasta batería) a la hora elegida, y programado al reiniciar el dispositivo
  (`RECEIVE_BOOT_COMPLETED`).
- Canal de notificación propio y silenciable desde los ajustes del sistema, más interruptor dentro
  de la app. Que el usuario pueda apagarlo sin odiar la app.

### 10.4 Tareas añadidas al plan
- **Fase 1 extra**: dial circular de minutos y dial de mezcla, con vista previa del tiempo.
- **Fase 2 extra**: pantalla principal con racha/minutos/calendario y anclaje de hábito en ajustes.
- **Fase 3 extra**: recordatorio diario (canal, permiso, reprogramación al reiniciar, acciones
  "Empezar" y "Más tarde", y respiración de un minuto desde la notificación).
- **Fase 4 extra**: tema claro/oscuro manual + sistema, cierre de sesión con resumen, hitos e
  interruptor de silencio durante la sesión.
- **Fase 5 (nueva, opcional)**: widget de una pulsación con RemoteViews puros.

---

## 11. Estimación honesta (revisada con la ampliación de experiencia)

Cuatro tandas de trabajo de mi parte (entorno + biblioteca/reproductor, registro/estadísticas,
Health Connect/exportación, afinado y APK), más tus pruebas de uso real. No es un fin de semana,
y el orden importa: cada fase deja algo que se puede probar en el móvil.
