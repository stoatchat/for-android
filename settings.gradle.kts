pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
        maven {
            name = "snapshot"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
        maven {
            name = "compose-dev"
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
        maven {
            name = "revolt"
            url = uri("https://git.revolt.chat/api/packages/librevolt/maven")
        }
    }
}
rootProject.name = "ZekoChat"
include(":app")
