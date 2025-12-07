// Import yang dibutuhkan untuk membaca local.properties
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.project_mobileapps"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.project_mobileapps"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        buildConfigField("String", "NEWS_API_KEY", "\"${localProperties.getProperty("NEWS_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    implementation(libs.androidx.compose.material.icons.extended)

    // Networking Libraries
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)

    // TAMBAHKAN BARIS INI
    testImplementation(libs.junit)

    // Anda sudah punya 2 baris ini dari perbaikan sebelumnya
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)

    val cameraxVersion = "1.5.2"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // 2. ML Kit Barcode Scanning (Untuk membaca QR)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // 3. ZXing (Untuk membuat/generate gambar QR)
    implementation("com.google.zxing:core:3.5.4")

    // 4. Accompanist Permissions (Opsional, tapi sangat memudahkan handling permission di Compose)
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")
}