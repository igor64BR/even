// :app module — UI (Compose), navigation and DI wiring. Depends on :domain and :data; never the
// other way around (Dependency Rule).
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// T12 — a real OAuth client id doesn't exist yet (T11 documented the same problem on the
// backend side, see `backend/src/Tally.Api/appsettings.Development.json`). Read from
// `local.properties` (a per-developer file, already in .gitignore) with a fallback to an
// obvious placeholder — never hardcoded as a "real" value in source code. Swap
// `TALLY_GOOGLE_WEB_CLIENT_ID` in `local.properties` once a Google Cloud project exists.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localOrDefault(key: String, default: String): String =
    (localProperties.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() } ?: default

android {
    namespace = "com.tally.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tally.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localOrDefault("TALLY_GOOGLE_WEB_CLIENT_ID", "PLACEHOLDER-CLIENT-ID.apps.googleusercontent.com")}\"",
        )
        // 10.0.2.2 is the host alias from the Android emulator (loopback to the machine
        // running the backend, "http" profile from
        // `backend/src/Tally.Api/Properties/launchSettings.json`, port 5134) — doesn't work on a
        // physical device on the same network, which would need the machine's real IP.
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${localOrDefault("TALLY_API_BASE_URL", "http://10.0.2.2:5134/")}\"",
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

    // Robolectric (T8 smoke test of GroupListViewModel against real Room, same pattern as
    // :data's TallyDatabaseTest from T7) needs the module's resources (strings, manifest) on
    // the unit test classpath.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    // :data already uses Room, but as `implementation` (not `api`) — AppContainer, which builds
    // the Room.databaseBuilder in the manual DI composition, needs the artifact directly here too.
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

    // T12 — optional login via Google. Credential Manager is the current API (replaces the
    // deprecated GoogleSignIn); credentials-play-services-auth + googleid are the concrete
    // engine that knows how to talk to the Google account installed on the device.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit4)
    // Same reasoning as :data (see TallyDatabaseTest): in-memory Room needs an Android
    // Context, which only exists in a plain unit test via Robolectric (no emulator available
    // in this environment).
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
