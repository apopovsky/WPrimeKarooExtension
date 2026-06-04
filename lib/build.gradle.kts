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

    constraints {
        implementation(libs.netty.codec.http2) { because("Fixes GHSA-f6hv-jmp6-3vwv, GHSA-prj3-ccx8-p6x4, GHSA-xpw8-rcwv-8f8p") }
        implementation(libs.netty.codec.http) { because("Fixes GHSA-5jpm-x58v-624v") }
        implementation(libs.netty.common) { because("Fixes GHSA-389x-839f-4rhx, GHSA-xq3w-v528-46rv") }
        implementation(libs.netty.handler) { because("Fixes GHSA-4g8c-wm8x-jfhw, GHSA-6mjq-h674-j845") }
        implementation(libs.netty.codec) { because("Fixes GHSA-25hv-jmp6-3vwv") }
        implementation(libs.netty.handler.proxy) { because("Fixes GHSA-22hv-jmp6-3vwv") }
        implementation(libs.apache.commons.lang3) { because("Fixes GHSA-j288-q9x7-2f5v") }
        implementation(libs.apache.httpclient) { because("Fixes GHSA-7r82-7xv7-xcpj") }
        implementation(libs.bouncycastle.bcprov) { because("Fixes GHSA-20hv-jmp6-3vwv, GHSA-19hv-jmp6-3vwv") }
        implementation(libs.bouncycastle.bcpkix) { because("Fixes GHSA-18hv-jmp6-3vwv") }
        implementation(libs.jackson.core) { because("Fixes GHSA-15hv-jmp6-3vwv") }
        implementation(libs.jose4j) { because("Fixes GHSA-14hv-jmp6-3vwv") }
        implementation(libs.jdom2) { because("Fixes GHSA-12hv-jmp6-3vwv") }
    }
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

// Suppress "annotation applied to value parameter only" warning (Kotlin future behavior opt-in)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

