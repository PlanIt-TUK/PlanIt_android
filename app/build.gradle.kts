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
}