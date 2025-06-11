plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services) // This must be at the very top
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.syedsaifhossain.g_chatapplication"
    compileSdk = 35
    buildFeatures {
        viewBinding = true // Ensure viewBinding is enabled
        dataBinding = true
    }
    defaultConfig {
        applicationId = "com.syedsaifhossain.g_chatapplication"
        minSdk = 24
        targetSdk = 35
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
}

dependencies {
    // AndroidX and Material Design
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.ui.text.android)
    implementation(libs.filament.android)
    implementation("androidx.activity:activity-ktx:1.8.0") // Specific version for activity-ktx
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    // Custom Image Views
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.mikhaellopez:circularimageview:4.3.1")

    // Firebase BOM (Platform) - Only the latest version is kept
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))

    // Firebase Authentication (using BOM version via libs.firebase.auth)
    implementation(libs.firebase.auth)

    // Firebase Cloud Functions
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-firestore:24.9.0")

    // Agora SDK
    implementation(libs.full.sdk)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // Networking & JSON
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson:2.10")

    // Other utilities
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.15")
    implementation("com.vanniktech:emoji-google:0.8.0")
    implementation("com.journeyapps:zxing-android-embedded:4.1.0")
    implementation("com.stripe:stripe-android:20.45.0")
    implementation ("com.google.android.material:material:1.12.0")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}