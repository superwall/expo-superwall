plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.superwall.superapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.superwall.superapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default build config fields
        buildConfigField("String", "SUPERWALL_ENV", "\"release\"")
        buildConfigField("String", "SUPERWALL_ENDPOINT", "\"NONE\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Local Superwall SDK
    implementation(project(":superwall"))
    implementation(project(":superwall-compose"))

    // Core Android dependencies
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)

    // Navigation for routing between test screens
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // For handling purchases in tests
    implementation("com.revenuecat.purchases:purchases:9.2.0")

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

// Maestro test tasks
tasks.register("runMaestroTests") {
    group = "verification"
    description =
        "Build and run all Maestro tests for Android. Usage: ./gradlew runMaestroTests -Penv=[release|dev|custom] -Pendpoint=[custom_endpoint] -Psdk-version=[version]"

    dependsOn("buildForMaestro")

    doLast {
        val env = project.findProperty("env")?.toString() ?: "release"
        val customEndpoint = project.findProperty("endpoint")?.toString()

        val appId = "com.superwall.superapp"
        val apkPath =
            layout.buildDirectory
                .file("outputs/apk/debug/test_app-debug.apk")
                .get()
                .asFile.absolutePath

        // Install APK
        exec {
            commandLine("adb", "install", "-r", apkPath)
        }

        // Prepare environment variables for maestro
        val maestroEnvVars =
            mutableListOf(
                "PLATFORM_ID=$appId",
                "ENV=$env",
            )

        if (env == "custom" && customEndpoint != null) {
            maestroEnvVars.add("CUSTOM_ENDPOINT=$customEndpoint")
        }

        // Run Maestro tests
        val testFiles =
            listOf(
                "maestro/handler/flow.yaml",
                "maestro/flow.yaml",
                "maestro/delegate/flow.yaml",
                "maestro/purchasecontroller/test_pc_purchases.yaml",
                "maestro/purchasecontroller/no_pc_purchases.yaml",
            )

        testFiles.forEach { testFile ->
            println("Running Maestro test: $testFile")
            val maestroCommand = mutableListOf("maestro", "test")
            maestroEnvVars.forEach { envVar ->
                maestroCommand.addAll(listOf("-e", envVar))
            }
            maestroCommand.add(testFile)

            exec {
                commandLine(maestroCommand)
                workingDir = projectDir
            }
        }

        println("All Maestro tests completed successfully!")
    }
}

tasks.register("buildForMaestro") {
    group = "build"
    description = "Build APK with environment and SDK version configuration for Maestro tests"

    doLast {
        val env = project.findProperty("env")?.toString() ?: "release"
        val customEndpoint = project.findProperty("endpoint")?.toString()
        val sdkVersion = project.findProperty("sdk-version")?.toString()

        println("Building test app with configuration:")
        println("  Environment: $env")
        if (env == "custom" && customEndpoint != null) {
            println("  Custom endpoint: $customEndpoint")
        }
        if (sdkVersion != null) {
            println("  SDK version: $sdkVersion")
        }

        // Handle configuration changes (environment and/or SDK version)
        var buildFileModified = false
        val buildFile = file("build.gradle.kts")
        var buildContent = buildFile.readText()
        val originalBuildContent = buildContent

        // Handle SDK version replacement if provided
        if (sdkVersion != null) {
            println("Replacing project dependency with external SDK version $sdkVersion...")

            // Parse Superwall version to determine compatible RevenueCat version
            fun parseVersion(version: String): Triple<Int, Int, Int> {
                val parts = version.split(".")
                return Triple(
                    parts.getOrNull(0)?.toIntOrNull() ?: 0,
                    parts.getOrNull(1)?.toIntOrNull() ?: 0,
                    parts.getOrNull(2)?.toIntOrNull() ?: 0,
                )
            }

            fun isVersionLessThan(
                version: String,
                major: Int,
                minor: Int,
                patch: Int,
            ): Boolean {
                val (vMajor, vMinor, vPatch) = parseVersion(version)
                return when {
                    vMajor < major -> true
                    vMajor > major -> false
                    vMinor < minor -> true
                    vMinor > minor -> false
                    vPatch < patch -> true
                    else -> false
                }
            }

            // Determine RevenueCat version based on Superwall version
            val revenueCatVersion =
                if (isVersionLessThan(sdkVersion, 2, 5, 0)) {
                    "8.22.0"
                } else {
                    "9.2.0"
                }

            println("Using RevenueCat version $revenueCatVersion for Superwall $sdkVersion")

            // Replace project dependencies with external SDK version
            buildContent =
                buildContent.replace(
                    "implementation(project(\":superwall\"))",
                    "implementation(\"com.superwall.sdk:superwall-android:$sdkVersion\")",
                )
            buildContent =
                buildContent.replace(
                    "implementation(project(\":superwall-compose\"))",
                    "implementation(\"com.superwall.sdk:superwall-compose:$sdkVersion\")",
                )

            // Replace RevenueCat version
            buildContent =
                buildContent.replace(
                    Regex("implementation\\(\"com\\.revenuecat\\.purchases:purchases:[^\"]*\"\\)"),
                    "implementation(\"com.revenuecat.purchases:purchases:$revenueCatVersion\")",
                )

            buildFileModified = true
        }

        // Update build config fields for environment configuration
        buildContent =
            buildContent.replace(
                Regex("buildConfigField\\(\"String\", \"SUPERWALL_ENV\", \"[^\"]*\"\\)"),
                "buildConfigField(\"String\", \"SUPERWALL_ENV\", \"\\\"$env\\\"\")",
            )

        if (customEndpoint != null) {
            buildContent =
                buildContent.replace(
                    Regex("buildConfigField\\(\"String\", \"SUPERWALL_ENDPOINT\", [^)]*\\)"),
                    "buildConfigField(\"String\", \"SUPERWALL_ENDPOINT\", \"\\\"$customEndpoint\\\"\")",
                )
        }
        buildFileModified = true

        if (buildFileModified) {
            // Write updated build.gradle.kts
            buildFile.writeText(buildContent)
            println("Updated build.gradle.kts with new configuration")

            if (sdkVersion != null) {
                println("Running gradle sync...")
                // Run gradle sync
                exec {
                    commandLine("./gradlew", "--refresh-dependencies")
                    workingDir = projectDir.parentFile
                }
            }

            // Build with new configuration
            exec {
                commandLine("./gradlew", ":test_app:assembleDebug")
                workingDir = projectDir.parentFile
            }

            // Restore original build.gradle.kts
            println("Restoring original build.gradle.kts...")
            buildFile.writeText(originalBuildContent)
            println("Restored original build.gradle.kts")
        } else {
            // No modifications needed, just build normally
            exec {
                commandLine("./gradlew", ":test_app:assembleDebug")
                workingDir = projectDir.parentFile
            }
        }

        println("Build completed successfully!")
    }
}

tasks.register("installAndRunMaestroTests") {
    group = "verification"
    description = "Install APK and run Maestro tests (skips build if APK exists)"

    doLast {
        val appId = "com.superwall.superapp"
        val apkPath =
            layout.buildDirectory
                .file("outputs/apk/debug/test_app-debug.apk")
                .get()
                .asFile.absolutePath

        // Install APK
        exec {
            commandLine("adb", "install", "-r", apkPath)
        }

        // Run Maestro tests
        val testFiles =
            listOf(
                "maestro/handler/flow.yaml",
                "maestro/flow.yaml",
                "maestro/delegate/flow.yaml",
                "maestro/purchasecontroller/test_pc_purchases.yaml",
                "maestro/purchasecontroller/no_pc_purchases.yaml",
            )

        testFiles.forEach { testFile ->
            println("Running Maestro test: $testFile")
            exec {
                commandLine("maestro", "test", "-e", "PLATFORM_ID=$appId", testFile)
                workingDir = projectDir
            }
        }

        println("All Maestro tests completed successfully!")
    }
}
