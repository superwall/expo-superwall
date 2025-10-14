plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.superwall.superapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.superwall.superapp"
        minSdk = 22
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("default") {
            dimension = "version"
        }
        create("entitlements") {
            dimension = "version"
        }
        create("observer") {
            dimension = "version"
        }
        create("purchase") {
            dimension = "version"
        }
        create("revenuecat") {
            dimension = "version"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packagingOptions {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":superwall"))
    implementation(project(":superwall-compose"))

    // Billing
    implementation(libs.billing)
    implementation(libs.revenue.cat)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.material3)

    // Core
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
}
