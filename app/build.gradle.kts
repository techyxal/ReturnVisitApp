plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.xa.rv0"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xa.rv0"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }
    composeOptions {kotlinCompilerExtensionVersion = "1.5.15"}


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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // implementation(libs.androidx.activity) // Only if you explicitly need the non-ktx version and defined it
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.play.services.maps)
    implementation(libs.gson)
    implementation(libs.androidx.activity.ktx) // Corrected and only one instance
    implementation(libs.androidx.room.runtime)
    //implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // implementation(libs.play.services.maps) // This was a duplicate of a line above, ensure BOM is used if this is for maps
    implementation(libs.play.services.location)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    ksp(libs.androidx.room.compiler) // Ensure this alias is correct in your TOML
    // implementation(libs.play.services.maps) // This one will use the BOM version, the one above might be redundant if this is present
    implementation(libs.gms.play.services.maps)
    implementation (libs.material) // Use the latest version
    implementation (platform(libs.firebase.bom))
    implementation(libs.androidx.glance.appwidget)
}