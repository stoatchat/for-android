import com.mikepenz.aboutlibraries.plugin.StrictMode
import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("io.sentry.android.gradle") version "4.12.0"
    id("app.cash.sqldelight") version "2.0.1"
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

val composeBomVersion = "2025.03.00"
val accompanistVersion = "0.34.0"
val okhttpVersion = "4.12.0"
val navVersion = "2.9.0"
val hiltVersion = "2.57"
val glideVersion = "4.16.0"
val ktorVersion = "3.0.0-beta-2"
val media3Version = "1.7.1"
val material3Version = "1.4.0-alpha15"
val androidXTestVersion = "1.6.1"

object LivekitVersion {
    val core = "2.16.0"
    val componentsCompose = "1.3.1"
}

fun property(fileName: String, propertyName: String, fallbackEnv: String? = null): String? {
    val propsFile = rootProject.file(fileName)
    if (propsFile.exists()) {
        val props = Properties()
        props.load(FileInputStream(propsFile))
        if (props[propertyName] != null) {
            return props[propertyName] as String?
        } else {
            logger.warn("Property '$propertyName' not found in '$fileName'. Attempting to use environment variable '$fallbackEnv'")
            if (fallbackEnv != null) {
                val env = System.getenv(fallbackEnv)
                if (env != null) {
                    return env
                } else {
                    logger.warn("Environment variable '$fallbackEnv' not found either. Returning null")
                }
            }
            return null
        }
    } else {
        logger.warn("Properties file '$fileName' not found. Attempting to use environment variable '$fallbackEnv'")
        if (fallbackEnv != null) {
            val env = System.getenv(fallbackEnv)
            if (env != null) {
                return env
            } else {
                logger.warn("Environment variable '$fallbackEnv' not found either. Returning null")
            }
        }
        return null
    }
}

// Calls property but with revoltbuild.properties as the first argument
fun buildproperty(propertyName: String, fallbackEnv: String? = null): String? {
    return property("revoltbuild.properties", propertyName, fallbackEnv)
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "chat.revolt"
        minSdk = 24
        targetSdk = 35
        versionCode = Integer.parseInt("001_003_206".replace("_", ""), 10)
        versionName = "1.3.6b"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
                cppFlags("")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField(
                "String",
                "SENTRY_DSN",
                "\"${buildproperty("sentry.dsn", "RVX_SENTRY_DSN")}\""
            )
            buildConfigField(
                "String",
                "FLAVOUR_ID",
                "\"${buildproperty("build.flavour_id", "RVX_BUILD_FLAVOUR_ID")}\""
            )
        }

        debug {
            isPseudoLocalesEnabled = true

            applicationIdSuffix = ".debug"
            versionNameSuffix = "+debug"
            resValue(
                "string",
                "app_name",
                buildproperty("build.debug.app_name", "RVX_DEBUG_APP_NAME")!!
            )

            buildConfigField(
                "String",
                "SENTRY_DSN",
                "\"${buildproperty("sentry.dsn", "RVX_SENTRY_DSN")}\""
            )
            buildConfigField(
                "String",
                "FLAVOUR_ID",
                "\"${buildproperty("build.flavour_id", "RVX_BUILD_FLAVOUR_ID")}\""
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    namespace = "chat.revolt"
    externalNativeBuild {
        cmake {
            path(file("src/main/cpp/CMakeLists.txt"))
            version = "3.22.1"
        }
    }
    lint {
        abortOnError = false
        disable += "MissingTranslation"
    }
}

sentry {
    autoUploadProguardMapping =
        buildproperty("sentry.upload_mappings", "RVX_SENTRY_UPLOAD_MAPPINGS") == "true"

    tracingInstrumentation {
        enabled = true

        logcat {
            enabled = true
            minLevel = LogcatLevel.WARNING
        }
    }
}

dependencies {
    // Android/Kotlin Core
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.20")

    // Kotlinx - various first-party extensions for Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:$composeBomVersion")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.8.2")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.material3:material3:$material3Version")
    implementation("androidx.compose.material3:material3-window-size-class:$material3Version")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Accompanist - Jetpack Compose Extensions
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion")

    // KTOR - HTTP+WebSocket Library
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")

    // Screen Navigation
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // Jetpack Compose Tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Hilt - Dependency Injection
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")

    // Glide - Image Loading
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    ksp("com.github.bumptech.glide:ksp:$glideVersion")

    // AboutLibraries - automated OSS library attribution
    implementation("com.mikepenz:aboutlibraries-core:11.3.0-rc02")

    // Sentry - crash reporting
    implementation("io.sentry:sentry-android:8.13.2")
    implementation("io.sentry:sentry-compose-android:8.13.2")

    // Other AndroidX libraries
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("androidx.core:core-splashscreen:1.2.0-beta02")
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Libraries used for legacy View-based UI
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")

    // hCaptcha - captcha provider
    implementation("com.github.hcaptcha:hcaptcha-android-sdk:3.8.1")

    // JDK Desugaring - polyfill for new Java APIs
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // AndroidX Media3 w/ ExoPlayer
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // Compose libraries
    implementation("me.saket.telephoto:zoomable-image:1.0.0-alpha02")
    implementation("me.saket.telephoto:zoomable-image-glide:1.0.0-alpha02")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")
    implementation("dev.chrisbanes.haze:haze:1.6.4")
    implementation("dev.chrisbanes.haze:haze-materials:1.6.4")

    // QR code related
    implementation("com.google.zxing:core:3.5.3")
    implementation("io.github.g00fy2.quickie:quickie-bundled:1.11.0")

    // Persistence
    implementation("app.cash.sqldelight:android-driver:2.0.1")
    implementation("androidx.datastore:datastore:1.1.7")
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Markup
    implementation("org.jetbrains:markdown:0.7.3")
    implementation("dev.snipme:highlights:1.0.0")

    // Livekit - Commented out for now to not inflate the app size.
    /*implementation("io.livekit:livekit-android:${LivekitVersion.core}")
    implementation("io.livekit:livekit-android-compose-components:${LivekitVersion.componentsCompose}")*/

    // Firebase - Cloud Messaging
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Shimmer - loading animations
    implementation("com.valentinilk.shimmer:compose-shimmer:1.3.1")

    // Chucker - HTTP inspector
    debugImplementation("com.github.chuckerteam.chucker:library:4.0.0")
    releaseImplementation("com.github.chuckerteam.chucker:library-no-op:4.0.0")

    // Square Logcat
    implementation("com.squareup.logcat:logcat:0.1")

    // Librevolt
    implementation("librevolt:librevolt-jvm:0.1.0")

    // Testing
    androidTestImplementation("androidx.test:runner:$androidXTestVersion")
    androidTestImplementation("androidx.test:rules:$androidXTestVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

aboutLibraries {
    additionalLicenses += listOf("ofl")
    includePlatform = true
    strictMode = StrictMode.FAIL
    allowedLicenses += listOf(
        "Apache-2.0",
        "OFL",
        "MIT",
        "ASDKL",
        "BSD-2-Clause",
        "cmark",
        "EPL-1.0",
        "BSD-3-Clause",
        "BSD License",
        "ML Kit Terms of Service"
    )
    configPath = "compliance"
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("chat.revolt.persistence")
        }
    }
}