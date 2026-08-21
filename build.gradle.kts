import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.android") {
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }
}

// Windows cloud-synced folders can briefly lock Gradle outputs. Local validation may
// opt into an external disposable build root; CI keeps Gradle's standard directories.
providers.environmentVariable("POI_BUILD_ROOT").orNull
    ?.takeIf(String::isNotBlank)
    ?.let { externalRoot ->
        allprojects {
            val moduleName = path.trim(':').replace(':', '-').ifBlank { "root" }
            layout.buildDirectory.set(file("$externalRoot/$moduleName"))
        }
    }
