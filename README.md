# Natrium Demo

Kotlin Multiplatform Compose demo app for the Natrium SDK — a wrapper around Wire's Kalium messaging library for secure communication.

Targets **Android**, **iOS** (arm64 + simulator), and **Desktop** (JVM).

## Prerequisites

- **JDK 21+** (Kalium's JVM/Android artifacts are Java 21 bytecode)
- **Android SDK** (compileSdk 36, minSdk 26) — install via Android Studio or `sdkmanager`
- **Xcode** (macOS only, required for iOS builds)
- **Git**

## First-Time Setup

The Natrium SDK is consumed as a regular Gradle dependency (`schwarz.opensource.natrium:natrium-core`) resolved from Maven Central.

### 1. Clone Natrium Demo

```bash
git clone https://github.com/SchwarzDigits/natrium-demo.git
```

### 2. Configure Backend Properties

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

For iOS, open `iosApp/` in Xcode and build from there. The KMP framework is configured as a
**dynamic** framework named `ComposeApp` (dynamic so the native dependencies are fully linked into a
self-contained framework — see [iOS: linking AVS](#ios-linking-avs)).

## iOS: linking AVS

**iOS only** — Android and Desktop need nothing here.

Kalium's calling module references Wire's proprietary **`avs`** framework, which on iOS/Kotlin-Native
must be resolved at **compile/link time** (on Android it ships as a `.so` via Maven). `avs` is not on
Maven, so an iOS build otherwise fails with `ld: framework 'avs' not found`.

natrium doesn't use calling, so this demo links a small **stub** `avs.framework` (no-op symbols)
instead of the real binary. It lives under [`composeApp/avs-stub/`](composeApp/avs-stub/) and is wired
per iOS target in `composeApp/build.gradle.kts` via `linkerOpts("-F", …)`. To replicate in your own
app, copy that folder and add the `-F` line — see
[`composeApp/avs-stub/README.md`](composeApp/avs-stub/README.md).

## Project Structure

```
natrium-demo/
  composeApp/
    src/
      commonMain/    # Shared UI (Compose + Material3) and logic
      androidMain/   # Android Activity entry point
      desktopMain/   # JVM desktop window entry point
      iosMain/       # iOS MainViewController
    avs-stub/        # Stub avs.framework for iOS linking (see "iOS: linking AVS")
  iosApp/            # Xcode project wrapper
  gradle/            # Gradle wrapper + version catalog
```

Package: `schwarz.digits.showcase`

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
