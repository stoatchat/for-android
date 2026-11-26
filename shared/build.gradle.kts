import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "chat.stoat.shared"
        compileSdk {
            version = release(libs.versions.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.minSdk.get().toInt()

        withHostTest {}

        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:api"))
            implementation(project(":core:designsystem"))

            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.resources)
            implementation(libs.compose.multiplatform.ui.tooling.preview)
            implementation(libs.shimmer)
            api(libs.navigation3.runtime)
            implementation(libs.navigationevent.compose)
            implementation(libs.kotlin.serialization.core)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlin.datetime)
            implementation(libs.jetbrains.markdown)
            api(libs.multiplatform.markdown)
            implementation(libs.multiplatform.markdown.m3)
            implementation(libs.multiplatform.markdown.coil3)
            implementation(libs.multiplatform.markdown.code)
            implementation(libs.highlights)
            implementation(libs.ratex)
            implementation(libs.reorderable)
            implementation(libs.kermit)
            implementation(libs.ktor.http)
            api(libs.filekit.dialogs.compose)
        }

        androidMain.dependencies {
            implementation(libs.android.core.ktx)
            implementation(libs.android.browser)
            implementation(libs.zxing.core)
        }
    }
}

compose.resources {
    packageOfResClass = "chat.stoat.shared.resources"
    publicResClass = true
}
