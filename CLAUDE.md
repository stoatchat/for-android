# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config in zekobuild.properties)
./gradlew assembleRelease

# Run lint checks
./gradlew lint

# Run Kotlin linting (kotlinter)
./gradlew lintKotlin
./gradlew formatKotlin  # Auto-fix lint issues

# Run tests
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests (requires device/emulator)

# Clean build
./gradlew clean

# Generate SQLDelight classes
./gradlew generateSqlDelightInterface
```

## Architecture Overview

**Zeko Chat** (formerly Peptide) is a Kotlin/Jetpack Compose chat application connecting to the Peptide platform.

### Tech Stack
- **UI**: Jetpack Compose 2025.03.00 with Material3
- **DI**: Dagger Hilt 2.52
- **Networking**: Ktor 3.0.0-beta-2 (HTTP + WebSocket for real-time)
- **Database**: SQLDelight 2.0.1 (type-safe SQL)
- **State**: Kotlin Coroutines + StateFlow

### Code Organization

```
app/src/main/java/chat/zekochat/
├── activities/       # Entry points (MainActivity, DeepLinkActivity, etc.)
├── api/              # Peptide API client
│   ├── routes/       # API endpoint definitions
│   ├── schemas/      # Serializable data classes
│   ├── realtime/     # WebSocket handling
│   └── settings/     # Feature flags and experiments
├── composables/      # Reusable Compose UI components
│   ├── screens/      # Full-screen composables
│   ├── chat/         # Chat-specific components
│   └── sheets/       # Bottom sheets
├── screens/          # Screen ViewModels
├── persistence/      # KVStorage (DataStore), SqlStorage (SQLDelight)
├── di/               # Hilt modules
├── ndk/              # JNI bindings
└── PeptideApplication.kt  # @HiltAndroidApp entry
```

### Key Patterns

- **MVVM with StateFlow**: ViewModels expose `StateFlow<UiState>` consumed by Composables
- **Single Activity**: Navigation via Jetpack Navigation Compose with typed routes
- **API Layer**: `AppAPI.kt` is the central HTTP/WebSocket client; routes organized by domain
- **Feature Flags**: `Experiments` system for toggling features (e.g., "usePolar" for alternate UI)

### Native Code (NDK)

Located in `app/src/main/cpp/`:
- **stendal**: JNI library binding
- **cmark**: CommonMark markdown parser (external submodule)
- Built with CMake 3.22.1; requires 16KB page alignment for Android 15+

### SQLDelight

Schema files in `app/src/main/sqldelight/`. Generated code goes to `com.zekochat.persistence`.

### Build Configuration

- **Debug**: App suffix `.debug`, pseudo-locales enabled
- **Release**: ProGuard minification, requires keystore config in `zekobuild.properties`
- **Signing**: Configure in `zekobuild.properties`:
  ```properties
  keystore.file=/path/to/keystore.jks
  keystore.password=xxx
  key.alias=xxx
  key.password=xxx
  ```

### Navigation Routes

Auth flow: `choose-platform` → `login/greeting` → `login` → `mfa` → `onboarding`
Main screens: `chat`, `main` (Polar experiment), `settings/*`, `discover`

### Deep Links

Handles `app.peptide.chat/invite/*`, `rvlt.gg/*`, `peptide.chat/*` via intent filters in AndroidManifest.xml.
