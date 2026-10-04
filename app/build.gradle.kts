import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.kotlin.compose)
}

configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.itl.wprimeext"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.itl.wprimeext"
        minSdk = 23
        targetSdk = 37
        versionCode = 14
        versionName = "1.2.0"
        base.archivesName.set("WPrimeExtension-v${versionName}")
    }

    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(keystoreProperties::load)
    }
    val releaseKeystore = providers.environmentVariable("RELEASE_STORE_FILE").orNull
        ?: keystoreProperties.getProperty("storeFile")
    val releaseStorePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
        ?: keystoreProperties.getProperty("storePassword")
    val releaseKeyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
        ?: keystoreProperties.getProperty("keyAlias")
    val releaseKeyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
        ?: keystoreProperties.getProperty("keyPassword")
    val releaseSigningValues = listOf(releaseKeystore, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    require(releaseSigningValues.all { it.isNullOrBlank() } || releaseSigningValues.all { !it.isNullOrBlank() }) {
        "Release signing requires storeFile, storePassword, keyAlias and keyPassword via keystore.properties or RELEASE_* environment variables"
    }
    if (!releaseKeystore.isNullOrBlank()) {
        signingConfigs.create("release") {
            storeFile = rootProject.file(releaseKeystore)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            if (!releaseKeystore.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation(libs.karoo.ext)
    implementation(libs.hilt.android)
    ksp(libs.kotlinMetadataJvm)
    ksp(libs.hilt.android.compiler)
}
