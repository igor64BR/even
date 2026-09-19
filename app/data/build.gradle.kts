// Módulo :data — implementa as interfaces de repositório declaradas em :domain usando Room
// (persistência local, T7) e um client SignalR/HTTP (sincronização, tasks futuras). Depende de
// :domain, nunca o contrário.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.rateio.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Módulo de biblioteca: "targetSdk" no defaultConfig está deprecado a partir do AGP 8.7
    // (será removido na v9) — o SDK alvo é decisão do :app, que consome esta lib. Isso afeta só
    // lint/testes deste módulo.
    lint {
        targetSdk = 34
    }
    testOptions {
        targetSdk = 34
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
