// Módulo :data — implementa as interfaces de repositório declaradas em :domain usando Room
// (persistência local, T7) e um client SignalR/HTTP (sincronização, tasks futuras). Depende de
// :domain, nunca o contrário.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // T12 — client HTTP mínimo pra POST /auth/google (T11) e armazenamento seguro da sessão.
    // Só o necessário pro endpoint de auth; outros endpoints (grupos sincronizados, SignalR)
    // ficam para as tasks que os introduzem (T19+).
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)

    // T40 — cliente SignalR (biblioteca oficial) pro Hub de notificações em tempo real (T38,
    // constitution.md princípio 3: sistema próprio, sem push de terceiros). rxjava3/gson são
    // dependências de tempo de execução do artefato `signalr` em si (confirmado no POM publicado:
    // io.reactivex.rxjava3:rxjava + com.google.code.gson:gson, não RxJava2/Jackson) — declaradas
    // explicitamente porque `SignalRGroupRealtimeGateway` referencia `io.reactivex.rxjava3.core.Single`
    // diretamente na API de `withAccessTokenProvider`.
    implementation(libs.signalr)
    implementation(libs.rxjava3)
    implementation(libs.gson)

    testImplementation(libs.junit4)
    // Room em memória não roda em teste unitário puro (precisa de um Context Android) — Robolectric
    // fornece isso na JVM, sem exigir emulador/dispositivo conectado (nenhum estava disponível neste
    // ambiente). Cobre o entregável de T7.2 ("teste instrumentado ou unitário simples").
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
