pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://natrium-repository.object.storage.eu01.onstackit.cloud") {
            content { includeGroup("schwarz.opensource.natrium") }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

include(":composeApp")
