import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.jetbrains)
}

val backendProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun backendProp(key: String): String =
    backendProps.getProperty(key) ?: error("Missing '$key' in local.properties")

val generateBackendConfig by tasks.registering {
    inputs.file(rootProject.file("local.properties")).optional()
    val outputDir = layout.buildDirectory.dir("generated/backendConfig")
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("schwarz/digits/showcase")
        dir.mkdirs()
        dir.resolve("BackendProperties.kt").writeText(
            """
            |package schwarz.digits.showcase
            |
            |internal object BackendProperties {
            |    const val NAME = "${backendProp("backend.name")}"
            |    const val API = "${backendProp("backend.api")}"
            |    const val ACCOUNTS = "${backendProp("backend.accounts")}"
            |    const val WEB_SOCKET = "${backendProp("backend.webSocket")}"
            |    const val TEAMS = "${backendProp("backend.teams")}"
            |    const val BLACK_LIST = "${backendProp("backend.blackList")}"
            |    const val WEBSITE = "${backendProp("backend.website")}"
            |}
            """.trimMargin()
        )
    }
}

kotlin {
    jvmToolchain(17)

    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBackendConfig.map { it.outputs.files.singleFile })
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)

            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation("schwarz.digits:natrium-core")
            implementation(libs.datetime)
            implementation(libs.filekit.compose)
            implementation(libs.okio.core)
        }

        androidMain.dependencies {
            implementation(libs.activity.compose)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${libs.versions.coroutines.get()}")
            }
        }
    }
}

android {
    namespace = "schwarz.digits.showcase"
    compileSdk = 36

    defaultConfig {
        applicationId = "schwarz.digits.showcase"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
}

compose.desktop {
    application {
        mainClass = "schwarz.digits.showcase.MainKt"
    }
}
