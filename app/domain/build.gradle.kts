// :domain module — pure Kotlin (the "jvm" plugin, not "android"): cannot depend on anything
// from the Android SDK. This is where the debt-simplification engine (T33) and the repository
// interfaces that :data implements (Dependency Inversion) live — :domain never depends on :app
// or :data.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Targets bytecode 17 without pinning a specific toolchain — this machine only has JDK 21
// installed and there's no toolchain auto-provisioning repository configured (would need
// network access). The same pattern is used in :app and :data via kotlinOptions.jvmTarget.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Flow is the only coroutines type the repository contracts expose — kotlinx-coroutines-core
    // is a pure Kotlin lib (no Android), so it doesn't break the rule that :domain can't depend on the Android SDK.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
