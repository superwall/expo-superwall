plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.serialization)
    alias(libs.plugins.dropshot)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.superwall.superapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.superwall.superapp"
        minSdk = 22
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        testInstrumentationRunnerArguments["no-isolated-storage"] = "1"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
        resultsDir
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
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
    }

    packaging {
        resources.excludes += "META-INF/{AL2.0,LGPL2.1}"
        resources.excludes += "META-INF/LICENSE.md"
        resources.excludes += "META-INF/LICENSE-notice.md"
    }
}

dependencies {
    // Billing
    implementation(libs.billing)
    implementation(libs.revenue.cat)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)

    // Core
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // Superwall
    implementation(project(":superwall"))
    implementation(project(":superwall-compose"))

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Test (Android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.uiautomator)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.core)
    androidTestUtil(libs.orchestrator)

    // Debug
    // debugImplementation(libs.leakcanary.android)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
