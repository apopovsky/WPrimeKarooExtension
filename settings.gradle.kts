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
            credentials {
                username = providers.gradleProperty("gpr.user").getOrElse(System.getenv("USERNAME"))
                password = providers.gradleProperty("gpr.key").getOrElse(System.getenv("TOKEN"))
            }
            content {
                includeGroup("io.hammerhead")
            }
        }
    }
}

rootProject.name = "WPrimeExtension"
include(":lib", ":app")
