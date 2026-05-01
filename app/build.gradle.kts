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
        versionCode = 10
        versionName = "1.1.0"
        base.archivesName.set("WPrimeExtension-v${versionName}")
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {
    // Source lib dependency (use this while developing):
    implementation(project(":lib"))

    // Other dependencies
    implementation(libs.timber)

    // Core android
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.serialization.json)

    // compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // glance for extension views
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.glance.appwidget.preview)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    // allows retrieving viewmodels from within a composable
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // allows usage of `StateFlow#collectAsStateWithLifecycle()`
    implementation(libs.androidx.lifecycle.runtime.compose)

    // coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.rx2)

    // datastore
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.nordic.ble.client)

    // Hilt
    ksp(libs.kotlinMetadataJvm)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.mapbox.sdk.turf)

    constraints {
        implementation(libs.kotlinStdlibJdk7) {
            because("kotlin-stdlib-jdk7 is now a part of kotlin-stdlib")
        }
        implementation(libs.kotlinStdlibJdk8) {
            because("kotlin-stdlib-jdk8 is now a part of kotlin-stdlib")
        }
    }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}