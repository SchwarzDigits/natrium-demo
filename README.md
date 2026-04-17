# Showcase

Kotlin Multiplatform Compose demo app for the Natrium SDK — a wrapper around Wire's Kalium messaging library for secure communication.

Targets **Android**, **iOS** (arm64 + simulator), and **Desktop** (JVM).

## Prerequisites

- **JDK 17+**
- **Android SDK** (compileSdk 36, minSdk 26) — install via Android Studio or `sdkmanager`
- **Xcode** (macOS only, required for iOS builds)
- **Git** with submodule support

## First-Time Setup

### 1. Clone Natrium (sibling directory)

Showcase includes Natrium as a [Gradle composite build](https://docs.gradle.org/current/userguide/composite_builds.html). The `settings.gradle.kts` expects Natrium at `../natrium` relative to this project:

```
parent/
  natrium/      <-- Natrium SDK (with Kalium submodule)
  showcase/     <-- this project
```

Clone Natrium next to the showcase directory and initialize its Kalium submodule:

```bash
# from the parent directory of showcase
git clone [Natrium Repo URL] natrium
cd natrium
git submodule update --init --recursive
cd ..
```

> **Custom path?** If Natrium lives elsewhere, adjust the `natrium.compositeBuildPath` path in `gradle.properties`:
> ```properties
> natrium.compositeBuildPath=../path-to-natrium-sdk
> ```

### 2. Clone Showcase

```bash
git clone git@ssh.dev.azure.com:v3/schwarzit-chicago/schwarzit.civicseal/showcase showcase
cd showcase
```

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

For iOS, open `iosApp/` in Xcode and build from there. The KMP framework is configurSced as a static framework named `ComposeApp`.

## Project Structure

```
showcase/
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
