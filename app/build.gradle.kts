import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.salihoguz.minlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.salihoguz.minlauncher"
        minSdk = 28
        targetSdk = 35
        versionCode = 7
        versionName = "1.0.0"
    }

    signingConfigs {
        // A committed debug key, so every CI build installs over the previous one.
        // Public, so not for releases people rely on.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // The private release key, when the build is given one (see README, "Signing").
        val releaseKeystore = System.getenv("MINLAUNCHER_KEYSTORE")?.let { file(it) }?.takeIf { it.exists() }
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("MINLAUNCHER_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MINLAUNCHER_KEY_ALIAS")
                keyPassword = System.getenv("MINLAUNCHER_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Optimized build for daily use; Compose is noticeably slower in debug builds.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    lint {
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
