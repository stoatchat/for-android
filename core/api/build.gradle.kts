import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "chat.stoat.core.api"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(libs.kotlin.serialization.json)
            implementation(libs.kotlin.serialization.cbor)
            implementation(libs.kotlin.datetime)
            implementation(libs.kermit)
            implementation(libs.sentry.kotlin.multiplatform)
            implementation(libs.android.datastore.preferences.core)
        }

        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.compose.bom))
            implementation(libs.compose.runtime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.contentnegotiation)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.android.core.ktx)
            implementation(libs.lifecycle.process)
            implementation(libs.android.datastore.preferences)
        }
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("chat.stoat.persistence")
        }
    }
}
