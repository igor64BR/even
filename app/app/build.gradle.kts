// Módulo :app — UI (Compose), navegação e DI wiring. Depende de :domain e :data; nunca o
// contrário (Dependency Rule).
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// T12 — client id OAuth real ainda não existe (T11 documentou o mesmo problema do lado do
// backend, ver `backend/src/Rateio.Api/appsettings.Development.json`). Lido de
// `local.properties` (arquivo por-desenvolvedor, já no .gitignore) com fallback pra um
// placeholder óbvio — nunca hardcoded como valor "real" no código-fonte. Trocar
// `RATEIO_GOOGLE_WEB_CLIENT_ID` em `local.properties` assim que houver um projeto Google Cloud.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localOrDefault(key: String, default: String): String =
    (localProperties.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() } ?: default

android {
    namespace = "com.rateio.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rateio.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localOrDefault("RATEIO_GOOGLE_WEB_CLIENT_ID", "PLACEHOLDER-CLIENT-ID.apps.googleusercontent.com")}\"",
        )
        // 10.0.2.2 é o alias do host a partir do emulador Android (loopback da máquina que roda
        // o backend, perfil "http" de `backend/src/Rateio.Api/Properties/launchSettings.json`,
        // porta 5134) — não funciona em dispositivo físico na mesma rede, que precisaria do IP
        // real da máquina.
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${localOrDefault("RATEIO_API_BASE_URL", "http://10.0.2.2:5134/")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }

    // Robolectric (T8 smoke test do GroupListViewModel contra Room de verdade, mesmo padrão do
    // RateioDatabaseTest de T7 em :data) precisa dos recursos do módulo (strings, manifest) no
    // classpath de teste unitário.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    // :data já usa Room, mas como `implementation` (não `api`) — AppContainer, que monta o
    // Room.databaseBuilder da composição manual de DI, precisa do artefato direto aqui também.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // T12 — login opcional via Google. Credential Manager é a API atual (substitui o
    // GoogleSignIn deprecated); credentials-play-services-auth + googleid são o motor concreto
    // que sabe conversar com a conta Google instalada no aparelho.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit4)
    // Mesmo racional do :data (ver RateioDatabaseTest): Room em memória precisa de um Context
    // Android, que só existe em teste unitário puro via Robolectric (sem emulador disponível
    // neste ambiente).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
