# Encargo 09 — Pistas ya escuchadas en la Biblioteca y exportar el historial a CSV

## Objetivo

Dos cosas independientes, ambas aprobadas por el usuario:

1. **Biblioteca**: marcar de forma discreta las pistas que ya se han escuchado, para saber de un
   vistazo cuáles quedan por descubrir y cuáles se repiten.
2. **Exportar el historial a CSV** desde Ajustes, para poder ver los datos por fuera de la app.

Todo **local**: la app no declara INTERNET y no debe declararlo. La exportación escribe un fichero
elegido por el usuario con el selector de documentos del sistema; nada sale del teléfono.

## Lo que ya existe (léelo antes de escribir, no lo reescribas a ciegas)

- `datos/SesionDao.kt` — ya tiene, del encargo anterior: `insertar`, `observarSesiones`,
  `fijarNota`, `cambiarDuracion`, `borrar`, `observarMinutosTotales`, `observarDiasConPractica` y
  `observarMinutosPorDia` (todas `Flow` salvo las de escritura).
- `datos/RepositorioSesiones.kt` — punto único de acceso: `sesiones()`, `anotar()`, `cambiarDuracion()`,
  `borrar()`, `registrar()`, `resumen()`.
- `datos/Sesion.kt` — entidad `Sesion` (`inicioMs`, `finMs`, `duracionMs`, `pista`, `grupo`,
  `completada`, `nota`, `idSalud`). **Tabla `sesiones`, versión 1. No cambies la versión.**
- `ui/PantallaBiblioteca.kt` — la lista de pistas del catálogo, con sus filas y carpetas.
- `ui/PantallaAjustes.kt` — ajustes, con el selector de carpeta de la biblioteca (SAF) y la sección de
  sonido. Aquí va el botón de exportar.
- `audio/EstadoBiblioteca.kt` / `audio/Biblioteca.kt` — catálogo en el dispositivo (`Pista` con
  `titulo` y `carpeta`).
- `ui/Diario.kt` — del encargo anterior; es un buen ejemplo del estilo de diálogos y de formato de
  fechas del proyecto (Locale("es", "ES")). **No lo toques** salvo que sea imprescindible.

## 1. Pistas ya escuchadas

- **Consulta nueva** en el DAO + método en el repositorio: los nombres de pista que tienen al menos
  una sesión registrada con duración real, como `Flow<List<String>>`
  (`SELECT DISTINCT pista FROM sesiones WHERE duracionMs > 0`). Sigue el estilo de las que ya hay.
- **En la Biblioteca**: en cada fila, si esa pista ya se ha escuchado, una **marca discreta**
  (un icono de visto pequeño o un texto corto en color secundario, junto al título o bajo él, sin
  cambiar el tamaño ni la altura de la fila). Que se distinga sin gritar y sin romper la alineación de
  la lista. La comparación es **por nombre de pista** (es lo que guarda la tabla).
- **Nada de filtros nuevos** ni de contadores: solo la marca. Si el catálogo tiene 200 y pico entradas,
  la lista debe seguir igual de fluida (no calcules nada costoso dentro de cada fila: pasa el conjunto
  ya preparado).
- Al terminar una sesión nueva, la marca debe aparecer **sola** (los datos son `Flow` de Room; no
  fuerces recargas).

## 2. Exportar el historial a CSV

- **Consulta nueva** para leer todas las sesiones ordenadas por fecha (puedes reutilizar
  `observarSesiones()`, que ya existe; no dupliques).
- **Botón en Ajustes**: «Exportar historial (CSV)», en una sección propia y con una línea breve que
  explique qué hace. Al pulsarlo, el sistema pregunta dónde guardar el fichero
  (`ActivityResultContracts.CreateDocument("text/csv")` con
  `rememberLauncherForActivityResult`), con nombre sugerido `serena-sesiones-AAAA-MM-DD.csv` (la
  fecha del día).
- **Contenido**: cabecera y una fila por sesión, la más reciente primero.
  Columnas exactas: `fecha,hora,pista,grupo,minutos,completada,nota`
  - `fecha` en ISO (`AAAA-MM-DD`) y `hora` en `HH:mm`, **siempre en la zona local del dispositivo**.
  - `minutos` como número entero (los mismos que muestra la app; redondeo igual que en la interfaz).
  - `completada`: `si` o `no`.
  - `nota`: el texto de la nota tal cual, **entre comillas dobles, con las comillas internas
    duplicadas** (`"` → `""`) y los saltos de línea convertidos en espacio; vacío si no hay nota.
  - **Escapa también** `pista` y `grupo` con la misma regla (pueden llevar comas o comillas).
  - Codificación UTF-8, con salto de línea `\n`, y separador por comas (sin punto y coma).
- Escribe con `contentResolver.openOutputStream(uri)`; **no** pidas permisos de almacenamiento (el
  selector del sistema no los necesita, y la app no debe declarar ninguno nuevo).
- **Informa del resultado en pantalla**: si se guarda, un mensaje breve de confirmación; si falla la
  escritura o el usuario cancela el selector, no debe quedar la sensación de que se ha hecho (trata el
  caso y no revientes).

## Restricciones

- **No cambies la versión de la base de datos** ni el esquema (la versión sigue siendo la 1).
- **No modifiques `AGENTS.md`**; si lo tocas sin querer, revierte con `git checkout -- AGENTS.md`.
- **No hagas commits.** No compiles (en el sandbox no hay SDK de Android): deja el código correcto y di
  lo que no puedas validar.
- Sin dependencias nuevas ni subida de versiones (Kotlin 2.0.21, Compose BOM 2024.10.01, material3 1.3.1).
- **Nada de INTERNET ni permisos nuevos** en el manifiesto.
- **Todo en castellano y con tildes correctas.** Los textos de interfaz van en
  `res/values/strings.xml` (nombres en minúscula y sin tildes en la clave), sin duplicar ninguno de los
  que ya existen: mira el fichero entero antes de añadir.
- Comentarios del código en el estilo del proyecto (castellano, sin tildes dentro del código).

## Ficheros que puedes tocar

`datos/SesionDao.kt`, `datos/RepositorioSesiones.kt`, `ui/PantallaBiblioteca.kt`,
`ui/PantallaAjustes.kt`, `MainActivity.kt` (solo si hay que pasar datos nuevos a esas pantallas),
`res/values/strings.xml` y, si lo ves más limpio, un fichero nuevo `ui/ExportadorCsv.kt` (o similar)
con la generación del CSV y el launcher. **No toques** `ui/Diario.kt`, `estadisticas/Calculos.kt`,
`SerenaDb.kt`, ni nada del audio o de la base de datos.

## Qué informar al final

1. Qué ficheros has tocado y qué has añadido en cada uno.
2. Si algo del encargo queda sin cumplir, dicho sin adornos.
3. Dónde dudas por no poder compilar (firmas de API, imports) y qué te haría falta.
4. Confirmación explícita de que no has tocado `AGENTS.md`, de que no has hecho commits, de que la
   versión de la base de datos sigue siendo la 1 y de que no has añadido permisos ni dependencias.
