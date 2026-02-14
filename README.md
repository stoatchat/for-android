<div align="center">
    <h1>Stoat for Android</h1>
    <p>Official <a href="https://stoat.chat">Stoat</a> Android app.</p>
    <br/><br/>
    <div>
        <a href="https://play.google.com/store/apps/details?id=chat.revolt"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" width="200"></a>
        <br/>
    </div>
    <small>Google Play is a trademark of Google LLC.</small>
    <br/><br/><br/>
</div>

## Description

The codebase includes the app itself, as well as an internal library for interacting with the Stoat
API. The app is written in Kotlin, and wholly
uses [Jetpack Compose](https://developer.android.com/jetpack/compose).

## Stack

- [Kotlin](https://kotlinlang.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
    - For some Material components, the View-based
      [Material Components Android](https://github.com/material-components/material-components-android)
      (MDC-Android) library is used.
- [Ktor](https://ktor.io/)
- [Dagger](https://dagger.dev/) with [Hilt](https://dagger.dev/hilt/)

## Resources

### Stoat for Android

- [Roadmap](https://op.stoatinternal.com/projects/revolt-for-android/work_packages)
- [Stoat for Android Technical Documentation](https://stoatchat.github.io/for-android/)
- [Android-specific Contribution Guide](https://stoatchat.github.io/for-android/contributing/guidelines/)
  &mdash;**read carefully before contributing!**

### Stoat

- [Stoat Project Board](https://github.com/orgs/stoatchat/discussions) (Submit feature requests
  here)
- [Stoat Development Server](https://app.revolt.chat/invite/API)
- [Stoat Server](https://app.revolt.chat/invite/Testers)
- [General Stoat Contribution Guide](https://developers.stoat.chat/developing/contrib/)

## Quick Start

1. Open the project in Android Studio.
2. Copy `stoatbuild.properties.example`, `sentry.properties.example`, and
   `app/google-services.json.example`. The new filenames should have the
    ".example" suffix removed.
3. Fill out the three new files.
4. Run the app on an emulator or a physical device by
   running the `app` module.

In-depth setup instructions can be found
at [Setting up your Development Environment](https://stoatchat.github.io/for-android/contributing/setup/)
