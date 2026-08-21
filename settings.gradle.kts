pluginManagement {
    buildscript {
        repositories {
            google()
            mavenCentral()
            maven { url = uri("https://storage.googleapis.com/r8-releases/raw") }
        }
        dependencies {
            classpath("com.android.tools:r8:9.1.29")
        }
    }
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
    ":core:auth",
    ":core:cloud",
    ":core:data",
    ":core:designsystem",
    ":core:update",
    ":feature:discover",
    ":feature:plans",
    ":feature:create",
    ":feature:profile",
    ":feature:auth",
    ":feature:admin",
)
