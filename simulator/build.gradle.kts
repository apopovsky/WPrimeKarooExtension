plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.kotlin.compose)
}

configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.itl.wprimeext.simulator"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.itl.wprimeext.simulator"
        minSdk = 23
        targetSdk = 37
        versionCode = 14
        versionName = "1.2.0"
        base.archivesName.set("WPrimeSimulator-v${versionName}")
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    testImplementation(libs.junit)
    implementation(libs.hilt.android)
    ksp(libs.kotlinMetadataJvm)
    ksp(libs.hilt.android.compiler)
}
// Installation must go through the launcher, which checks an explicit emulator serial.
tasks.configureEach {
    if (name.startsWith("install")) {
        enabled = false
        description = "Disabled: use scripts/start-simulator.ps1 with an emulator serial."
    }
}
