pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Maven Central yoğunken (429) yedek ayna
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
}
rootProject.name = "KISS"
include(":app")
