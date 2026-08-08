plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.furini.rokidchatgptbridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.furini.rokidchatgptbridge"
        minSdk = 31
        targetSdk = 36
        versionCode = 6
        versionName = "0.6"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.rokid.cxr:client-l:1.0.1")
}
