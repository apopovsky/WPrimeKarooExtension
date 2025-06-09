plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.itl.wprimeextension"
    compileSdk = 34  // Necesario para dependencias AndroidX

    defaultConfig {
        applicationId = "com.itl.wprimeextension"
        minSdk = 23
        targetSdk = 34  // Updated to match latest template
        versionCode = 9
        versionName = "1.0.1"

        // Configurar nombre del archivo APK
        setProperty("archivesBaseName", "WPrimeKarooExtension-v${versionName}")
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            // Desactivar optimizaciones que pueden causar problemas
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
        release {
            isMinifyEnabled = false
            isDebuggable = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            // Usamos debug signing para evitar problemas de firma
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Karoo Extension library
    implementation(libs.hammerhead.karoo.ext)

    // Core android
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)

    // compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.activity.compose)

    // DataStore for configuration storage
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    // allows retrieving viewmodels from within a composable
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // allows usage of `StateFlow#collectAsStateWithLifecycle()`
    implementation(libs.androidx.lifecycle.runtime.compose)

    // coroutines
    implementation(libs.kotlinx.coroutines.android)

    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.0") {
            because("kotlin-stdlib-jdk7 is now a part of kotlin-stdlib")
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0") {
            because("kotlin-stdlib-jdk8 is now a part of kotlin-stdlib")
        }
    }
}