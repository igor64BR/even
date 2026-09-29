// Build root — no logic here besides declaring the plugins used by the modules (with apply
// false: each module applies what it needs). Versions all come from the version catalog
// (gradle/libs.versions.toml), never hardcoded in this file or in the modules' build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
