import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val requiredKeystoreProperties = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val hasReleaseKeystore = requiredKeystoreProperties.all {
    !keystoreProperties.getProperty(it).isNullOrBlank()
} && rootProject.file(keystoreProperties.getProperty("storeFile", "")).exists()

fun keystoreProperty(name: String): String =
    keystoreProperties.getProperty(name)
        ?: error("Missing `$name` in keystore.properties")

android {
    namespace = "com.example.next_contest.wear"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.next_contest"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0-watch"

        buildConfigField(
            "String",
            "TMAP_API_KEY",
            "\"${localProperties.getProperty("TMAP_API_KEY", "")}\""
        )
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperty("storeFile"))
                storePassword = keystoreProperty("storePassword")
                keyAlias = keystoreProperty("keyAlias")
                keyPassword = keystoreProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.configureEach {
    if (name.contains("Release")) {
        doFirst {
            check(hasReleaseKeystore) {
                "Release signing requires keystore.properties and the referenced keystore file."
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
}
