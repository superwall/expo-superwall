
import groovy.json.JsonBuilder
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date

buildscript {
    extra["awsAccessKeyId"] = System.getenv("AWS_ACCESS_KEY_ID") ?: findProperty("aws_access_key_id")
    extra["awsSecretAccessKey"] = System.getenv("AWS_SECRET_ACCESS_KEY") ?: findProperty("aws_secret_access_key")
    extra["sonatypeUsername"] = System.getenv("SONATYPE_USERNAME") ?: findProperty("sonatype_username")
    extra["sonatypePassword"] = System.getenv("SONATYPE_PASSWORD") ?: findProperty("sonatype_password")
    extra["signingKeyId"] = System.getenv("GPG_SIGNING_KEY_ID") ?: findProperty("gpg_signing_key_id")
    extra["signingPassword"] = System.getenv("GPG_SIGNING_KEY_PASSPHRASE") ?: findProperty("gpg_signing_key_passphrase")
    extra["signingKey"] = System.getenv("GPG_SIGNING_KEY") ?: findProperty("gpg_signing_key")
}

plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization) // Maven publishing
    id("signing")
    alias(libs.plugins.publisher)
}

version = "2.5.7"

android {
    compileSdk = 35
    namespace = "com.superwall.sdk"

    defaultConfig {
        minSdkVersion(22)
        targetSdkVersion(33)

        aarMetadata {
            minCompileSdk = 26
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

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
        debug {
            consumerProguardFile("proguard-rules.pro")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = false
            consumerProguardFile("proguard-rules.pro")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "META-INF/LICENSE.md"
        resources.excludes += "META-INF/LICENSE-notice.md"
    }
}

mavenPublishing {
    coordinates(group.toString(), "superwall-android", version.toString())
    pom {
        name.set("Superwall")
        description.set("Remotely configure paywalls without shipping app updates")
        inceptionYear.set("2020")
        url.set("https://superwall.com")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/superwall/Superwall-Android?tab=MIT-1-ov-file#")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("yusuftor")
                name.set("Yusuf Tor")
                email.set("yusuf@superwall.com")
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
    implementation(libs.work.runtime.ktx)
    implementation(libs.lifecycle.process)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.threetenbp)
    // Billing
    implementation(libs.billing)
    implementation(libs.supercel)

    // Browser
    implementation(libs.browser)

    // Core
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.install.referrer)

    // Google Play Review
    implementation(libs.play.review.ktx)

    // Google Ads identifiers
    implementation(libs.play.services.appset)
    implementation(libs.play.services.ads.identifier)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.core)

    // Test (Android)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.lifecycle.testing)
}
