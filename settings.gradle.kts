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
    }
}

rootProject.name = "Banyg"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":feature:inbox")
include(":feature:accounts")
include(":feature:budget")
include(":feature:reports")
include(":feature:categories")
