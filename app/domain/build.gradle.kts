// Módulo :domain — Kotlin puro (plugin "jvm", não "android"): não pode depender de nada do
// Android SDK. É aqui que entra o motor de simplificação de dívidas (T33) e as interfaces de
// repositório que :data implementa (Dependency Inversion) — :domain nunca depende de :app nem
// de :data.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Alvo de bytecode 17 sem pinar um toolchain específico — a máquina só tem JDK 21 instalado e
// não há repositório de auto-provisionamento de toolchain configurado (precisaria de rede). O
// mesmo padrão é usado em :app e :data via kotlinOptions.jvmTarget.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
