plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.kotlin.compose)
}

configure<com.android.build.api.dsl.LibraryExtension> {
    namespace = "com.itl.wprimeext.shared"
    compileSdk = 37

    defaultConfig { minSdk = 23 }

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
    // External karoo-ext dependency
    api(libs.karoo.ext)

    // Other dependencies
    api(libs.timber)

    // Core android
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.activity.ktx)
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.kotlinx.serialization.json)

    // compose
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.ui.tooling.preview)
    api(libs.androidx.material3)
    api(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    api(libs.androidx.activity.compose)

    // glance for extension views
    api(libs.androidx.glance.appwidget)
    api(libs.androidx.glance.preview)
    api(libs.androidx.glance.appwidget.preview)

    api(libs.androidx.lifecycle.runtime.ktx)
    // allows retrieving viewmodels from within a composable
    api(libs.androidx.lifecycle.viewmodel.compose)
    // allows usage of `StateFlow#collectAsStateWithLifecycle()`
    api(libs.androidx.lifecycle.runtime.compose)

    // coroutines
    api(libs.kotlinx.coroutines.android)

    // datastore
    api(libs.androidx.datastore.preferences)


    // Hilt
    ksp(libs.kotlinMetadataJvm)
    ksp(libs.hilt.android.compiler)
    api(libs.hilt.android)


    constraints {
        api(libs.kotlinStdlibJdk7) {
            because("kotlin-stdlib-jdk7 is now a part of kotlin-stdlib")
        }
        api(libs.kotlinStdlibJdk8) {
            because("kotlin-stdlib-jdk8 is now a part of kotlin-stdlib")
        }

    }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
