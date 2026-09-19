// Build root — sem lógica aqui além de declarar os plugins usados pelos módulos (com apply
// false: cada módulo aplica o que precisa). Versões vêm todas do version catalog
// (gradle/libs.versions.toml), nunca soltas neste arquivo ou nos build.gradle.kts dos módulos.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
