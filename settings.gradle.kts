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

// Lokales Natrium-SDK als Composite Build: ersetzt die Maven-Abhaengigkeit
// schwarz.opensource.natrium:natrium-core durch ../natrium/natrium-core
includeBuild("../natrium")

include(":composeApp")
