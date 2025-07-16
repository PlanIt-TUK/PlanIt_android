import java.util.Properties
import java.io.FileInputStream

val keysPropertiesFile = rootProject.file("keys.properties")
val keysProperties = Properties()
keysProperties.load(FileInputStream(keysPropertiesFile))

val kakaoNativeKey: String = keysProperties["KAKAO_NATIVE_KEY"] as String

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

    buildFeatures {
        buildConfig = true
    }
    // 🔹 BuildConfig에 앱 키 전달
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "KAKAO_NATIVE_KEY", "\"$kakaoNativeKey\"")
        }
        getByName("release") {
            buildConfigField("String", "KAKAO_NATIVE_KEY", "\"$kakaoNativeKey\"")
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
    implementation(libs.kakao.user)
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
<<<<<<< HEAD
    implementation("com.kakao.sdk:v2-user:2.19.0") // 최신 안정 버전
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

=======
    // ✅ Retrofit2
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")

    // ✅ Retrofit + Gson Converter (서버 응답을 JSON → 객체로 자동 변환)
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // ✅ Kotlin Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
>>>>>>> origin/branch_sangyun
}