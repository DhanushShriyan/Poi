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

rootProject.name = "Poi"

include(
    ":app",
    ":core:model",
    ":core:data",
    ":core:designsystem",
    ":core:update",
    ":feature:discover",
    ":feature:plans",
    ":feature:create",
    ":feature:profile",
)
