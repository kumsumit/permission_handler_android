import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("android")
}

group = "com.baseflow.permissionhandler"
version = "1.0"

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.baseflow.permissionhandler"
    compileSdk = 37
    ndkVersion = "30.0.14904198"

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}
