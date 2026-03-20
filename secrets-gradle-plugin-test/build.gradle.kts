import com.alexeycode.secrets.SecretsHardeningMethod

plugins {
    alias(libs.plugins.android.application)
    id("com.alexeycode.secrets")
}

secrets {
    key("secret1") { "hello, secret 1" }
    key("secret2") { "hello, secret 2" }
    flavor("production") {
        key("secret3") { "hello, secret prod" }
    }
    flavor("development") {
        key("secret3") { "hello, secret dev" }
    }
    method = SecretsHardeningMethod.ZLIB
}

android {
    namespace = "com.alexeycode.secrets"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.alexeycode.secrets"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug") // that is a test app
            isMinifyEnabled = true
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

    buildFeatures {
        viewBinding = true
    }

    flavorDimensions.add("env")
    productFlavors {
        create("production") {
            dimension = "env"
        }
        create("development") {
            dimension = "env"
        }
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}