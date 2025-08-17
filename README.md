# Peptide on Android

## Description

This is the official Android app for the [Peptide](https://peptide.chat) chat platform.  
The codebase includes the app itself, as well as an internal library for interacting with the Peptide
API.

| Module | Package       | Description          |
|--------|---------------|----------------------|
| `:app` | `chat.peptide` | The main app module. |

The API library is part of the `app` module, and is not intended to be used as a standalone library,
as it makes liberal use of Android-specific APIs for reactivity.

The app is written in Kotlin, and uses
the [Jetpack Compose](https://developer.android.com/jetpack/compose) UI toolkit, the current state
of the art for Android UI development.

## Stack

- [Kotlin](https://kotlinlang.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
    - For some Material components, the View-based
      [Material Components Android](https://github.com/material-components/material-components-android)
      (MDC-Android) library is used.
- [Ktor](https://ktor.io/)
- [Dagger](https://dagger.dev/) with [Hilt](https://dagger.dev/hilt/)

## Resources

### Peptide on Android

- [Peptide on Android Technical Documentation](https://peptidechat.github.io/android/)
- [Android-specific Contribution Guide](https://peptidechat.github.io/android/contributing/guidelines/)
  &mdash;**read carefully before contributing!**

### Peptide

- [Peptide Project Board](https://github.com/peptidechat/peptide/discussions) (Submit feature requests
  here)
- [Peptide Testers Server](https://app.peptide.chat/invite/Testers)
- [General Peptide Contribution Guide](https://developers.peptide.chat/contributing)

## Quick Start

Open the project in Android Studio. You can then run the app on an emulator or a physical device by
running the `app` module.

In-depth setup instructions can be found
at [Setting up your Development Environment](https://peptidechat.github.io/android/contributing/setup/)