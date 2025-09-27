plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Keep Compose plugin if you might use it later
}

android {
    namespace = "com.example.cameracontrolapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.cameracontrolapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true // Keep Compose enabled if you plan to use it
        viewBinding = true // Enable viewBinding
    }
}

dependencies {

    implementation(libs.androidx.core.ktx) 
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // For Compose, if used
    implementation(platform(libs.androidx.compose.bom)) // For Compose, if used
    implementation(libs.androidx.ui) // For Compose, if used
    implementation(libs.androidx.ui.graphics) // For Compose, if used
    implementation(libs.androidx.ui.tooling.preview) // For Compose, if used
    implementation(libs.androidx.material3) // For Compose Material 3, if used

    // Added dependencies for AppCompat, Material Components, ConstraintLayout, and WebView
    implementation("androidx.appcompat:appcompat:1.7.0") // For AppCompatActivity
    implementation("com.google.android.material:material:1.12.0") // For Material Theming and Components
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // For ConstraintLayout
    implementation("androidx.webkit:webkit:1.11.0") // For WebView

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // For Compose, if used
    androidTestImplementation(libs.androidx.ui.test.junit4) // For Compose, if used
    debugImplementation(libs.androidx.ui.tooling) // For Compose, if used
    debugImplementation(libs.androidx.ui.test.manifest) // For Compose, if used
}
