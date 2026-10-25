plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "vea.raceremote"

    defaultConfig {
        applicationId = "vea.raceremote"
        // The lost SSD also held the installed APK's signing key; preserve that installation during recovery tests.
        val recoveryInstall = providers.gradleProperty("recoveryInstall").orNull == "true"
        if (recoveryInstall) applicationId = "vea.raceremote.recovery"
        manifestPlaceholders["appLabel"] = if (recoveryInstall) "RaceRemote восстановлено" else "@string/app_name"
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
    testImplementation("org.json:json:20230227")
    androidTestImplementation(AppDependencies.androidTestLibraries)
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:3.14.9")
}
