plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.furini.rokidchatgptbridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.furini.rokidchatgptbridge"
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "0.2"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("com.github.Anezium.Rokid-Nexus:bus-client:sdk-v0.13.0")
}
