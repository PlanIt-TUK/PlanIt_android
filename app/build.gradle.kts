plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "kr.ac.tukorea.planit"
    compileSdk = 35
    viewBinding { enable=true }
    defaultConfig {
        applicationId = "kr.ac.tukorea.planit"
        minSdk = 28
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //캘린더 뷰 라이브러리
    implementation(libs.calendar)
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    // ✅ Retrofit2
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")

    // ✅ Retrofit + Gson Converter (서버 응답을 JSON → 객체로 자동 변환)
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // ✅ Kotlin Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}