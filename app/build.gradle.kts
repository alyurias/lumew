plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("androidx.navigation.safeargs") version "2.7.7" apply false
}

android {
    namespace = "com.example.lumew"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lumew"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add this if you're using vector drawables
        vectorDrawables.useSupportLibrary = true
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
        isCoreLibraryDesugaringEnabled = true
    }

    // Enable view binding (recommended over findViewById)
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Lifecycle components (updated versions)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.6.2")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Preferences
    implementation(libs.preference)
    implementation(libs.preference.ktx)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))  // Updated version
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")  // Updated version

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")  // Updated version
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // UI Components
    implementation("com.github.yuyakaido:CardStackView:2.3.4")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Core library desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Multidex support
    implementation("androidx.multidex:multidex:2.0.1")

    // LEGACY Material Components (not Material 3)
    implementation("com.google.android.material:material:1.11.0")
    // Core Material 3
    implementation("androidx.compose.material3:material3:1.2.0")


    // Navigation Component (recommended for fragment transactions)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")


        // Material 3 (must be version 1.1.0 or higher)



        // Core dependencies
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("androidx.core:core-ktx:1.12.0")

        // If using Material Components Widgets
        implementation("androidx.compose.material:material:1.6.0")


    implementation ("com.loopj.android:android-async-http:1.4.11")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

}

apply(plugin = "com.google.gms.google-services")
apply(plugin = "androidx.navigation.safeargs")  // For safe args if using navigation