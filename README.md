# Natrium Demo

Kotlin Multiplatform Compose demo app for the Natrium SDK — a wrapper around Wire's Kalium messaging library for secure communication.

Targets **Android**, **iOS** (arm64 + simulator), and **Desktop** (JVM).

## Prerequisites

- **JDK 17+**
- **Android SDK** (compileSdk 36, minSdk 26) — install via Android Studio or `sdkmanager`
- **Xcode** (macOS only, required for iOS builds)
- **Git** with submodule support

## First-Time Setup

Natrium Demo consumes the Natrium SDK via a [Gradle composite build](https://docs.gradle.org/current/userguide/composite_builds.html). Both repos must sit side-by-side on disk:

```
parent/
  natrium/         <-- Natrium SDK (with Kalium submodule)
  natrium-demo/    <-- this project
```

### 1. Clone Natrium Demo

```bash
git clone https://github.com/SchwarzDigits/natrium-demo.git
```

### 2. Clone Natrium (sibling directory)

`settings.gradle.kts` expects Natrium at `../natrium` relative to this project. Clone it next to `natrium-demo` and initialize its Kalium submodule:

```bash
# from the parent directory of natrium-demo
git clone https://github.com/SchwarzDigits/natrium.git
cd natrium
git submodule update --init --recursive
cd ..
```

> **Important:** the `submodule update --init --recursive` step is required, not optional — Natrium itself includes Kalium as a nested composite build, and the Gradle sync will fail if `natrium/kalium/` is empty.

> **Custom path?** If Natrium lives elsewhere, adjust the `natrium.compositeBuildPath` path in `gradle.properties`:
> ```properties
> natrium.compositeBuildPath=../path-to-natrium-sdk
> ```

### 3. Configure Backend Properties

Copy the example below into `local.properties` (this file is git-ignored) and fill in the backend URLs for your environment:

```properties
sdk.dir=/path/to/your/Android/sdk

backend.name=staging
backend.api=https://your-api-host
backend.accounts=https://your-accounts-host
backend.webSocket=https://your-websocket-host
backend.teams=https://your-teams-host
backend.blackList=https://your-blacklist-host
backend.website=https://your-website-host
```

These values are code-generated into `BackendProperties.kt` at build time. The build will fail if any `backend.*` key is missing.

## Build & Run

```bash
# Build everything
./gradlew clean build

# Run Desktop (JVM)
./gradlew :composeApp:run

# Assemble Android APK
./gradlew :composeApp:assemble

```

For iOS, open `iosApp/` in Xcode and build from there. The KMP framework is configured as a static framework named `ComposeApp`.

## How the Composite Build Works

`settings.gradle.kts` reads the `natrium.compositeBuildPath` property from `gradle.properties` (default `../natrium`) and calls `includeBuild(...)` on it. A dependency substitution maps the Maven coordinate `schwarz.digits:natrium-core` (referenced from `composeApp/build.gradle.kts`) to the local `:natrium-core` Gradle project, so changes to Natrium are picked up immediately without publishing.

If you hit "module not found: schwarz.digits:natrium-core", check that `../natrium/natrium-core` exists and that `natrium.compositeBuildPath` points at a valid Natrium checkout.

## Project Structure

```
natrium-demo/
  composeApp/
    src/
      commonMain/    # Shared UI (Compose + Material3) and logic
      androidMain/   # Android Activity entry point
      desktopMain/   # JVM desktop window entry point
      iosMain/       # iOS MainViewController
  iosApp/            # Xcode project wrapper
  gradle/            # Gradle wrapper + version catalog
```

Package: `schwarz.digits.showcase`

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
