import groovy.json.JsonBuilder
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    id("signing")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.publisher)
}

version = "2.5.7"

android {
    namespace = "com.superwall.sdk.composable"
    compileSdk = 35

    defaultConfig {
        minSdk = 22

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val gitSha =
            project
                .exec {
                    commandLine("git", "rev-parse", "--short", "HEAD")
                    standardOutput = ByteArrayOutputStream()
                }.toString()
                .trim()
        buildConfigField("String", "GIT_SHA", "\"${gitSha}\"")

        val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date())
        buildConfigField("String", "BUILD_TIME", "\"${currentTime}\"")

        buildConfigField("String", "SDK_VERSION", "\"${version}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("consumer-rules.pro")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "../proguard-rules.pro",
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
}

mavenPublishing {
    coordinates(group.toString(), "superwall-compose", version.toString())

    pom {
        name.set("Superwall Compose")
        description.set("Remotely configure paywalls without shipping app updates - Jetpack Compose support")
        inceptionYear.set("2020")
        url.set("https://superwall.com")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/superwall/Superwall-Android?tab=MIT-1-ov-file#")
            }
        }

        developers {
            developer {
                id.set("ianrumac")
                name.set("Ian Rumac")
                email.set("ian@superwall.com")
            }
        }

        scm {
            url.set("https://github.com/superwall/Superwall-Android.git")
            connection.set("scm:git:git://github.com/superwall/Superwall-Android.git")
            developerConnection.set("scm:git:ssh://git@github.com/superwall/Superwall-Android.git")
        }
    }
}

tasks.register("generateBuildInfo") {
    doLast {
        var buildInfo = mapOf("version" to version)
        val jsonOutput = JsonBuilder(buildInfo).toPrettyString()
        val outputFile = File("${project.buildDir}/version.json")
        outputFile.writeText(jsonOutput)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)

    // Compose
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(project(":superwall"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
