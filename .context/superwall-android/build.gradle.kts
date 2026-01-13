import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("dev.testify") version "3.0.0" apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.publisher) apply true
    alias(libs.plugins.dokka)
}
true

buildscript {
    apply(from = "./scripts/old-agp-auto-downgrade.gradle.kts")
}

val superwallVersionFromEnv = System.getenv("SUPERWALL_VERSION")
val superwallVersionFromFile =
    rootDir.resolve("version.env").let { file ->
        if (!file.exists()) return@let null

        Properties().run {
            file.inputStream().use { load(it) }
            getProperty("SUPERWALL_VERSION")
        }
    }

extra["superwallVersion"] =
    superwallVersionFromEnv
        ?: superwallVersionFromFile
        ?: error("Missing SUPERWALL_VERSION. Set environment variable or provide version.env.")
