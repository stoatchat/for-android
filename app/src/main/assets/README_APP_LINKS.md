# Android App Links Setup Instructions

This document provides instructions for setting up Android App Links for the Revolt app.

## What are Android App Links?

Android App Links are HTTP URLs that bring users directly to specific content in your Android app. When a user clicks an App Link, the app opens immediately if it's installed, without showing the app chooser dialog.

## Requirements

1. Your app must have an intent filter that handles the specific URLs
2. Your website must have a Digital Asset Links JSON file that verifies the association with your app

## Steps to Complete the Setup

### 1. Get your app's signing certificate fingerprint

Run the following command to get your app's signing certificate fingerprint:

```bash
keytool -list -v -keystore your_keystore.jks -alias your_alias
```

Look for the SHA-256 fingerprint in the output. Remove all colons (:) from the fingerprint.

### 2. Update the assetlinks.json file

Open the `assetlinks.json` file in this directory and replace the placeholder with your actual SHA-256 fingerprint:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "chat.peptide",
    "sha256_cert_fingerprints": [
      "YOUR_SHA256_FINGERPRINT_HERE"
    ]
  }
}]
```

### 3. Host the assetlinks.json file on your server

Upload the `assetlinks.json` file to the following location on your server:

```
https://revolt.chat/.well-known/assetlinks.json
https://app.revolt.chat/.well-known/assetlinks.json
```

Make sure the file is accessible via HTTPS and returns with Content-Type: application/json.

### 4. Test your App Links

Use the Android Debug Bridge (ADB) to test your App Links:

```bash
adb shell am start -a android.intent.action.VIEW -d "https://revolt.chat/channels/CHANNEL_ID" chat.peptide
```

## Troubleshooting

If your App Links aren't working:

1. Verify that the assetlinks.json file is accessible via HTTPS
2. Check that the SHA-256 fingerprint in the assetlinks.json file matches your app's signing certificate
3. Make sure your app is using the correct signing certificate
4. Use the App Links Assistant in Android Studio to verify your App Links setup

For more information, see the [Android App Links documentation](https://developer.android.com/training/app-links).
