import java.time.LocalDateTime

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    `maven-publish`
}

val moduleName = "karoo-ext"
val libVersion = "1.1.5"


configure<com.android.build.api.dsl.LibraryExtension> {
    namespace = "io.hammerhead.karooext"
    compileSdk = 36

    defaultConfig {
        minSdk = 23

        buildConfigField("String", "LIB_VERSION", "\"$libVersion\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        aidl = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dokka {
    moduleName.set("karoo-ext")
    moduleVersion.set(libVersion)

    pluginsConfiguration.html {
        val assetsDir = rootDir.resolve("assets")
        homepageLink = "https://github.com/hammerheadnav/karoo-ext"
        footerMessage = "© ${LocalDateTime.now().year} SRAM LLC."
        customAssets.from(assetsDir.resolve("logo-icon.svg"))
        customStyleSheets.from(assetsDir.resolve("hammerhead-style.css"))
    }

    dokkaPublications.html {
        suppressInheritedMembers.set(true)
    }

    dokkaSourceSets.configureEach {
        // A bug exists in dokka for Android libraries that prevents this from being generated
        // https://github.com/Kotlin/dokka/issues/2876
        sourceLink {
            localDirectory.set(projectDir.resolve("lib/src/main/kotlin"))
            remoteUrl("https://github.com/hammerheadnav/karoo-ext/blob/${libVersion}/lib")
            remoteLineSuffix.set("#L")
        }
        skipEmptyPackages.set(true)
        includes.from("Module.md")
        samples.from("src/test/kotlin/samples.kt")
    }
}

tasks.dokkaGeneratePublicationHtml {
    outputDirectory.set(rootDir.resolve("docs"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)

    dokkaPlugin(libs.jetbrains.dokka.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}

// To build an publish locally: gradle lib:assemblerelease lib:publishtomavenlocal
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/hammerheadnav/karoo-ext")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("karoo-ext") {
            artifactId = moduleName
            groupId = "io.hammerhead"
            version = libVersion

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}

