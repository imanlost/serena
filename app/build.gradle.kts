plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.imanlost.serena"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.imanlost.serena"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Firma con la clave de depuracion: el APK de release queda instalable sin
            // configurar un keystore propio (uso personal, no va a Play Store).
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Versionado coordinado con compileSdk 35. No se fuerza lo ultimo de 2026: mezclar
    // librerias nuevas que exigen API 36 con un toolchain conocido solo trae fallos.
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    // Media3 1.9.4 es la ultima version cuyo AAR declara minCompileSdk=35 (comprobado
    // en Google Maven). A partir de 1.10.0 exige compileSdk 36 y romperia el build actual.
    implementation("androidx.media3:media3-exoplayer:1.9.4")
    implementation("androidx.media3:media3-session:1.9.4")

    // DocumentFile 1.1.0 (comprobado en Google Maven): su AAR declara minCompileSdk=34
    // y minAGP 8.1.1, compatible con compileSdk 35 / AGP 8.7.3. Es lo que permite
    // recorrer la carpeta elegida por SAF sin ningun permiso de almacenamiento.
    implementation("androidx.documentfile:documentfile:1.1.0")

    // Room 2.8.5, comprobado en Google Maven: su AAR de Android declara minCompileSdk=34
    // y minAGP 8.1.1, compatible con compileSdk 35 / AGP 8.7.3. El runtime declara
    // metadatos de Kotlin 2.1.0, que Kotlin 2.0.21 acepta (tolera hasta 2.1.0); el
    // procesador KSP pide symbol-processing-api 2.0.10-1.0.24, que satisface KSP
    // 2.0.21-1.0.28. room-ktx ya casi no aporta nada (sus APIs se integraron en
    // room-runtime en 2.7.0), pero se mantiene porque la documentacion la recomienda.
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // Test unitario de la logica de racha (Fase 2 del plan). JUnit 4.13.2 es la
    // version estable de siempre para pruebas JVM locales, sin relacion con compileSdk.
    testImplementation("junit:junit:4.13.2")
}
