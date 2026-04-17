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
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

// Work-around for KMP composite builds
// See: https://youtrack.jetbrains.com/issue/KT-56536
// rootProject.name = "showcase"

val compositeBuildPath = providers.gradleProperty("natrium.compositeBuildPath").get()
includeBuild(compositeBuildPath) {
    dependencySubstitution {
        substitute(module("schwarz.digits:natrium-core")).using(project(":natrium-core"))
    }
}

include(":composeApp")
