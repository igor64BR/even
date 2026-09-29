// :data module — implements the repository interfaces declared in :domain using Room
// (local persistence, T7) and a SignalR/HTTP client (sync, future tasks). Depends on
// :domain, never the other way around.
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

    // Library module: "targetSdk" in defaultConfig is deprecated as of AGP 8.7
    // (will be removed in v9) — the target SDK is a decision for :app, which consumes this lib.
    // This only affects this module's lint/tests.
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

    // T12 — minimal HTTP client for POST /auth/google (T11) and secure session storage.
    // Only what's needed for the auth endpoint; other endpoints (synced groups, SignalR)
    // are left for the tasks that introduce them (T19+).
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)

    // T40 — SignalR client (official library) for the real-time notification Hub (T38,
    // constitution.md principle 3: our own system, no third-party push). rxjava3/gson are
    // runtime dependencies of the `signalr` artifact itself (confirmed in its published POM:
    // io.reactivex.rxjava3:rxjava + com.google.code.gson:gson, not RxJava2/Jackson) — declared
    // explicitly because `SignalRGroupRealtimeGateway` references `io.reactivex.rxjava3.core.Single`
    // directly in the `withAccessTokenProvider` API.
    implementation(libs.signalr)
    implementation(libs.rxjava3)
    implementation(libs.gson)

    testImplementation(libs.junit4)
    // In-memory Room doesn't run in a plain unit test (it needs an Android Context) — Robolectric
    // provides that on the JVM, without requiring an emulator/connected device (none was available
    // in this environment). Covers the T7.2 deliverable ("instrumented test or simple unit test").
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
