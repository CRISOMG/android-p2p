plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
//    alias(libs.plugins.ksp)

    alias(libs.plugins.hilt)

    id("kotlin-kapt")
}
//ksp {
//    arg("ksp.incremental", "true")
//}
kapt {
    correctErrorTypes = true
}
android {
    namespace = "com.devcrisomg.wifip2p_custom_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.devcrisomg.wifip2p_custom_app"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.2"

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
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_1_8
//        targetCompatibility = JavaVersion.VERSION_1_8
//        isCoreLibraryDesugaringEnabled = true
//    }
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//        isCoreLibraryDesugaringEnabled = true
//    }
//    kotlinOptions {
//        jvmTarget = "11"
//    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
//    buildToolsVersion = "34.0.0"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.runtime.livedata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)

    // Hilt Core
//    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
//    kapt(libs.hilt.android.compiler)
//    implementation(libs.hilt.compiler)
    kapt(libs.hilt.compiler)

//    implementation(libs.dagger)
//    implementation(libs.dagger.android)
//    implementation(libs.dagger.android.support)
//    kapt(libs.dagger.compiler)
//    kapt(libs.dagger.android.processor)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
//    implementation("com.google.dagger:hilt-android:2.51.1")
//    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
}
