// Stack anclado del proyecto (verificado en dispositivo real):
// AGP 8.7.3 + Kotlin 2.0.21 + Gradle 8.9.
//
// Nota para el futuro: AGP 9.x integra el soporte de Kotlin y PROHIBE aplicar
// 'org.jetbrains.kotlin.android' (el build falla con "no longer required since AGP 9.0").
// Migrar a AGP 9 es un cambio aparte; no se mezcla con el desarrollo de la app.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // KSP tiene que coincidir EXACTAMENTE con la version de Kotlin: 2.0.21 -> 2.0.21-1.0.28.
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
