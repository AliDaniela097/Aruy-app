import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// local.properties no se sube al repositorio: ahí va la IP del PC para probar con un celular físico
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

// keystore.properties tampoco se sube: guarda la llave de firma del release (ver keystore.properties.example)
val propiedadesFirma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    // Bloqueamos a la API 36 definida para el curso
    namespace = "ec.edu.puce.aruy"
    compileSdk = 36

    defaultConfig {
        // MinSdk 26 asegura compatibilidad con el 90% del mercado actual
        applicationId = "ec.edu.puce.aruy"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (propiedadesFirma.isNotEmpty()) {
                storeFile = rootProject.file(propiedadesFirma.getProperty("storeFile"))
                storePassword = propiedadesFirma.getProperty("storePassword")
                keyAlias = propiedadesFirma.getProperty("keyAlias")
                keyPassword = propiedadesFirma.getProperty("keyPassword")
            }
        }
    }

    // Dónde corre el backend: en el emulador o en un celular físico de la misma red
    flavorDimensions += "entorno"
    productFlavors {
        create("emulador") {
            dimension = "entorno"
            // 10.0.2.2 es el localhost del PC visto desde el emulador de Android
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/\"")
        }
        create("celular") {
            dimension = "entorno"
            // En local.properties: aruy.baseUrl=http://192.168.0.10:3000/ (IP del PC en la red local)
            val urlCelular = propiedadesLocales.getProperty("aruy.baseUrl") ?: "http://192.168.0.10:3000/"
            buildConfigField("String", "BASE_URL", "\"$urlCelular\"")
        }
        create("produccion") {
            dimension = "entorno"
            // En local.properties: aruy.prodUrl=https://tu-backend.onrender.com/ (HTTPS obligatorio)
            val urlProduccion = propiedadesLocales.getProperty("aruy.prodUrl") ?: ""
            buildConfigField("String", "BASE_URL", "\"$urlProduccion\"")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Forzamos Java 21 para evitar errores de bytecode
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended) // Íconos de la interfaz
    implementation(libs.androidx.navigation.compose) // Navegación entre pantallas
    implementation(libs.androidx.datastore.preferences) // Sesión (JWT) y preferencias
    implementation(libs.androidx.exifinterface) // Orientación de las fotos antes de convertirlas a WebP
    implementation(libs.coil.compose) // Carga de imágenes desde Supabase Storage
    implementation(libs.mlkit.image.labeling) // Etiquetas de IA en el teléfono
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime) // Room: persistencia local
    implementation(libs.androidx.room.ktx) // Soporte para corrutinas y Flow
    ksp(libs.androidx.room.compiler) // KSP: procesamiento de anotaciones de Room
    implementation(libs.retrofit.core) // Retrofit: comunicación con API REST
    implementation(libs.retrofit.gson) // Gson: conversión de JSON a objetos Kotlin
    implementation(libs.okhttp.logging) // OkHttp: registro de solicitudes HTTP

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Evita subir a Play un AAB sin backend https o sin llave de firma
gradle.taskGraph.whenReady {
    if (hasTask(":app:bundleProduccionRelease")) {
        val url = propiedadesLocales.getProperty("aruy.prodUrl") ?: ""
        if (!url.startsWith("https://")) {
            throw GradleException("Define aruy.prodUrl=https://... en local.properties antes de generar el AAB")
        }
        if (propiedadesFirma.isEmpty()) {
            throw GradleException("Falta keystore.properties en la raiz del proyecto (ver keystore.properties.example)")
        }
    }
}
