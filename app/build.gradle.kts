plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "vea.raceremote"

    defaultConfig {
        applicationId = "vea.raceremote"
        compileSdk = AppConfig.compileSdk
        minSdk = AppConfig.minSdk
        targetSdk = AppConfig.targetSdk
        versionCode = AppConfig.versionCode
        versionName = AppConfig.versionName

        testInstrumentationRunner = AppConfig.androidTestInstrumentation
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeCompiler
        kotlinCompilerVersion = Versions.kotlin
    }
}

dependencies {
    implementation(AppDependencies.appLibraries)
    implementation(AppDependencies.jetpackCompose)
    implementation(AppDependencies.koinLibraries)

    testImplementation(AppDependencies.testLibraries)
    testImplementation("com.squareup.okhttp3:mockwebserver:3.14.9")
    androidTestImplementation(AppDependencies.androidTestLibraries)
}
