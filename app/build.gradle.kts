import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystoreProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

val adminProperties = Properties().apply {
    val propertiesFile = rootProject.file("admin.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

val supabaseProperties = Properties().apply {
    val propertiesFile = rootProject.file("supabase.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun signingValue(environmentName: String, propertyName: String): String? =
    providers.environmentVariable(environmentName).orNull
        ?: keystoreProperties.getProperty(propertyName)

fun adminValue(environmentName: String, propertyName: String): String =
    providers.environmentVariable(environmentName).orNull
        ?: adminProperties.getProperty(propertyName).orEmpty()

fun cloudValue(environmentName: String, propertyName: String): String =
    providers.environmentVariable(environmentName).orNull
        ?: supabaseProperties.getProperty(propertyName).orEmpty()

fun kotlinStringLiteral(value: String): String = "\"" + value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"") + "\""

val releaseStoreFile = signingValue("POI_KEYSTORE_PATH", "storeFile")
val releaseStorePassword = signingValue("POI_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("POI_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("POI_KEY_PASSWORD", "keyPassword")
val debugStoreFile = providers.environmentVariable("POI_DEBUG_KEYSTORE_PATH").orNull
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

val poiVersionCode = providers.environmentVariable("POI_VERSION_CODE").orNull?.toIntOrNull() ?: 1
val poiVersionName = providers.environmentVariable("POI_VERSION_NAME").orNull ?: "0.1.0"
val adminEmail = adminValue("POI_ADMIN_EMAIL", "email")
val adminCodeSha256 = adminValue("POI_ADMIN_CODE_SHA256", "codeSha256")
val supabaseUrl = cloudValue("POI_SUPABASE_URL", "url")
val supabasePublishableKey = cloudValue("POI_SUPABASE_PUBLISHABLE_KEY", "publishableKey")
val phoneAuthEnabled = cloudValue("POI_PHONE_AUTH_ENABLED", "phoneAuthEnabled")
    .equals("true", ignoreCase = true)

android {
    namespace = "com.dhanushshriyan.poi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dhanushshriyan.poi"
        minSdk = 26
        targetSdk = 36
        versionCode = poiVersionCode
        versionName = poiVersionName

        buildConfigField("String", "ADMIN_EMAIL", kotlinStringLiteral(adminEmail))
        buildConfigField("String", "ADMIN_CODE_SHA256", kotlinStringLiteral(adminCodeSha256))
        buildConfigField("String", "SUPABASE_URL", kotlinStringLiteral(supabaseUrl))
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            kotlinStringLiteral(supabasePublishableKey),
        )
        buildConfigField("boolean", "PHONE_AUTH_ENABLED", phoneAuthEnabled.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        getByName("debug") {
            debugStoreFile?.let { path ->
                storeFile = rootProject.file(path)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(checkNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-test"
        }
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:cloud"))
    implementation(project(":core:data"))
    implementation(project(":core:auth"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:update"))
    implementation(project(":feature:discover"))
    implementation(project(":feature:plans"))
    implementation(project(":feature:create"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:admin"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
