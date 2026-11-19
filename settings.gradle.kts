pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Fallback mirror of Maven Central run by Google (only used if the primary repos answer "not found").
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
}
rootProject.name = "SilentGateVPN"
include(":app")
