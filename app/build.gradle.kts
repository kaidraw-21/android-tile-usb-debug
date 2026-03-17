plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.snap.tiles"
    compileSdk = 34

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("SIGNING_STORE_FILE") ?: "${rootProject.projectDir}/.keystore/release.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: "quicktiles123"
            keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "release"
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: "quicktiles123"
        }
    }

    defaultConfig {
        applicationId = "com.snap.tiles"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Networking (OkHttp — zero extra deps, already transitive in many libs)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
