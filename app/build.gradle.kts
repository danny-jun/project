plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.emergencysystem"
    compileSdk = 36  // Required by androidx.activity:activity:1.12.1

    defaultConfig {
        applicationId = "com.example.emergencysystem"
        minSdk = 26
        targetSdk = 35  // Keep targetSdk at 35 for stability
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

    buildFeatures {
        viewBinding = true  // Add this line
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.gridlayout)
    implementation(libs.recyclerview)

    // Location Services
    implementation(libs.play.services.location)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)

    // Cloudinary
    implementation(libs.cloudinary.android)

    // Glide
//    implementation(libs.glide)
//    annotationProcessor(libs.glide.compiler)
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    // Add these missing dependencies
    implementation("androidx.core:core:1.17.0")  // For compatibility
    implementation("com.google.code.gson:gson:2.13.2")  // For JSON parsing

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}