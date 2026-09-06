// © ILINE TECH BY FERAK ALADDIN
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ilinetech.emergency"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ilinetech.emergency"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-scaffold"

        // See core/security/AppKeyProvider.kt for why this MUST come from a
        // gitignored local.properties value, never a literal here.
        val serialKeyHex = project.findProperty("ILINE_SERIAL_KEY_HEX") as String? ?: ""
        buildConfigField("String", "SERIAL_KEY_HEX", "\"$serialKeyHex\"")

        // See fcm/RemoteAlertPublisher.kt / CLOUD_FUNCTION_NOTES.md — empty
        // by default so the app degrades to SMS-only until configured.
        val cloudFunctionUrl = project.findProperty("CLOUD_FUNCTION_URL") as String? ?: ""
        buildConfigField("String", "CLOUD_FUNCTION_URL", "\"$cloudFunctionUrl\"")
        val cloudFunctionSecret = project.findProperty("CLOUD_FUNCTION_SHARED_SECRET") as String? ?: ""
        buildConfigField("String", "CLOUD_FUNCTION_SHARED_SECRET", "\"$cloudFunctionSecret\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false // TODO: enable + configure ProGuard/R8 rules before any real release
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1") // .await() on Firebase Tasks

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
