// Maven Central answers HTTP 429 to busy shared IPs (CI runners, cloud sandboxes). Google's
// mirror of Central is tried before mavenCentral(), which stays as the fallback. Same URL in both blocks
// because pluginManagement can't see values declared elsewhere in this file.
pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "MavenCentralMirror" }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "MavenCentralMirror" }
        mavenCentral()
    }
}

rootProject.name = "Wigglebun"
include(":app")
