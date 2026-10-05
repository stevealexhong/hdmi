plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)   // 关键：应用 Kotlin 插件
}

android {
    namespace = "com.alex.hdmi"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.alex.hdmi"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Kotlin 编译选项（JDK 11 兼容）
    kotlinOptions {
        jvmTarget = "11"
    }
    // 启用 ViewBinding
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    // UVCAndroid 库
    implementation("com.herohan:UVCAndroid:1.0.10")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}