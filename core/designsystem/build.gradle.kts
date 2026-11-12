import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "chat.stoat.core.designsystem"
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

            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.resources)
            implementation(libs.compose.multiplatform.ui.tooling.preview)
        }

        androidMain.dependencies {
            implementation(libs.android.core.ktx)
        }
    }
}

// Process compose multiplatform resources so we can keep downloading icon files verbatim from Google Fonts
val composeResourceSources = tasks.register<Sync>("prepareComposeResourceSources") {
    description =
        "Prepares a copy of the compose resource sources for processing by the compose multiplatform plugin."
    val output = layout.buildDirectory.dir("composeResourceSources/commonMain")
    from("src/commonMain/composeResources")
    into(output)
    filesMatching("drawable*/*.xml") {
        filter { line ->
            line.replace("@android:color/white", "#FFFFFFFF")
                .replace("@android:color/black", "#FF000000")
        }
    }
    doLast {
        val unsupportedColour =
            Regex("""android:(fillColor|strokeColor|color|startColor|centerColor|endColor)="[?@]""")
        val offenders = output.get().asFile.walk()
            .filter { it.extension == "xml" && it.parentFile.name.startsWith("drawable") }
            .filter { unsupportedColour.containsMatchIn(it.readText()) }
            .map { it.name }
            .toList()
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Vector drawables with theme or resource colours, which compose-resources can't draw: " +
                        offenders.joinToString()
            )
        }
    }
}

tasks.matching {
    it.name == "convertXmlValueResourcesForCommonMain" || it.name == "copyNonXmlValueResourcesForCommonMain"
}.configureEach {
    dependsOn(composeResourceSources)
}

compose.resources {
    packageOfResClass = "chat.stoat.core.designsystem.resources"
    publicResClass = true
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = composeResourceSources.flatMap { layout.buildDirectory.dir("composeResourceSources/commonMain") },
    )
}
