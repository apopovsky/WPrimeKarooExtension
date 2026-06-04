pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // For local publishes of karoo-ext
        mavenLocal()
        google()
        mavenCentral()
        // mapbox
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
        }
        // karoo-ext
        maven {
            url = uri("https://maven.pkg.github.com/hammerheadnav/karoo-ext")
            val localProperties = java.util.Properties().apply {
                val localFile = rootDir.resolve("local.properties")
                if (localFile.exists()) {
                    load(localFile.inputStream())
                }
            }
            val gprUser = localProperties.getProperty("gpr.user")
                ?: providers.gradleProperty("gpr.user").getOrNull()
                ?: System.getenv("GPR_USER")
                ?: System.getenv("GITHUB_ACTOR")
                ?: System.getenv("USERNAME")
            val gprKey = localProperties.getProperty("gpr.key")
                ?: providers.gradleProperty("gpr.key").getOrNull()
                ?: System.getenv("GPR_API_KEY")
                ?: System.getenv("GITHUB_TOKEN")
                ?: System.getenv("TOKEN")

            if (gprUser != null && gprKey != null) {
                credentials {
                    username = gprUser
                    password = gprKey
                }
            }
            content {
                includeGroup("io.hammerhead")
            }
        }
    }
}

rootProject.name = "WPrimeExtension"
include(":app")
