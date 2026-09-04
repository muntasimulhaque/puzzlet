plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.puzzlet"
    compileSdk = 37

    defaultConfig {
        // Must never change: this is the future Play Store package ID.
        applicationId = "app.puzzlet"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        // Instrumented tests (the emulator screenshot capture, arriving with
        // M1 gameplay) use AndroidX's runner.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 code shrinking + resource shrinking. Safe here: no reflection,
            // serialization, or JNI, only framework APIs (the insets controller)
            // and Compose, both of which ship their own keep rules.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        // Full lint runs in CI next to the unit tests (:app:lintRelease), not
        // just the vital subset that rides along with assembleRelease.
        abortOnError = true
        checkDependencies = false
    }
}

dependencies {
    // The Compose BOM governs every Compose artifact. M0 is deliberately
    // minimal: the brand screen needs nothing beyond the core. Lifecycle,
    // ViewModel and the rest arrive with the M1 host.
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    // The host is a ViewModel; collectAsStateWithLifecycle needs its runtime.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // One small preference file for the sound switch and a saved picture.
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")

    // Instrumented (emulator) screenshot capture: a bare ComponentActivity
    // hosts each state and PixelCopy grabs the window. No compose test rule,
    // no Espresso, no injection machinery: rendering states and copying
    // pixels needs none of it, so captures keep working on whatever image
    // the app targets.
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
