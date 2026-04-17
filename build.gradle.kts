plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.jetbrains) apply false
}

// Workaround: Kover 0.7.6 in kalium registers outgoing variants with only
// {kotlinx.kover.marker=Kover} attributes and no org.gradle.usage. Gradle's lenient
// attribute matching selects these as the sole "compatible" variant for Compose MP
// resource resolution (kotlin-multiplatformresources), causing the resource task to
// try expanding kover's text-based default.artifact files as ZIPs.
// Fix: replace kover artifacts with empty ZIPs before resource resolution runs.
allprojects {
    tasks.configureEach {
        if (name.contains("ResolveResourcesFromDependencies")) {
            doFirst {
                val emptyZip = byteArrayOf(
                    0x50, 0x4B, 0x05, 0x06,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                )
                val compositeBuildPath = providers.gradleProperty("natrium.compositeBuildPath").get()
                rootDir.resolve("$compositeBuildPath/kalium").walkTopDown()
                    .filter { it.name == "default.artifact" && it.parentFile.name == "kover" }
                    .forEach { it.writeBytes(emptyZip) }
            }
        }
    }
}
