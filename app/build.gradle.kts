import com.mikepenz.aboutlibraries.plugin.StrictMode
import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.extensions.SentryPluginExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sentry.android) apply false
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.google.services)
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

val sentryEnabled = buildproperty("sentry.dsn", "RVX_SENTRY_DSN") != "";
if(sentryEnabled) {
    apply<SentryPlugin>();
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

// Calls property but with stoatbuild.properties as the first argument
fun buildproperty(propertyName: String, fallbackEnv: String? = null): String? {
    return property("stoatbuild.properties", propertyName, fallbackEnv)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "chat.stoat"

    defaultConfig {
        applicationId = "chat.revolt"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = Integer.parseInt("001_003_409".replace("_", ""), 10)
        versionName = "1.3.9a"

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

if(sentryEnabled) {
    configure<SentryPluginExtension> {
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
}

dependencies {
    implementation(project(":core:model"))

    implementation(libs.android.core.ktx)
    implementation(libs.kotlin.reflect)

    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.serialization.cbor)
    implementation(libs.kotlin.datetime)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.windowsizeclass)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)

    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.contentnegotiation)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.navigation.compose)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    implementation(libs.glide)
    implementation(libs.glide.compose)
    ksp(libs.glide.ksp)

    implementation(libs.aboutlibraries.core)

    implementation(libs.sentry.android)
    implementation(libs.sentry.compose.android)

    implementation(libs.android.profileinstaller)
    implementation(libs.android.documentfile)
    implementation(libs.android.browser)
    implementation(libs.android.webkit)
    implementation(libs.android.palette)
    implementation(libs.android.core.telecom)
    implementation(libs.android.core.splashscreen)
    implementation(libs.android.constraintlayout)
    implementation(libs.android.constraintlayout.compose)
    implementation(libs.android.appcompat)
    implementation(libs.android.material)
    implementation(libs.android.datastore)
    implementation(libs.android.datastore.preferences)

    implementation(libs.hcaptcha)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.okhttp)
    implementation(libs.media3.ui)

    implementation(libs.zoomable.image)
    implementation(libs.zoomable.image.glide)
    implementation(libs.haze)
    implementation(libs.haze.materials)

    implementation(libs.zxing.core)
    implementation(libs.quickie.bundled)

    implementation(libs.sqldelight.android.driver)

    implementation(libs.jetbrains.markdown)
    implementation(libs.highlights)

    // implementation(libs.livekit.android)
    // implementation(libs.livekit.android.camerax)
    // implementation(libs.livekit.android.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.shimmer)

    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.noop)

    implementation(libs.square.logcat)

    androidTestImplementation(libs.android.test.core)
    androidTestImplementation(libs.android.test.rules)
    androidTestImplementation(libs.compose.ui.test.junit4)
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
            packageName.set("chat.stoat.persistence")
        }
    }
}