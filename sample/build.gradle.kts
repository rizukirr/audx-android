import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_0
        jvmTarget = JvmTarget.JVM_21
    }
}

android {
    namespace = "com.audx.example"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.audx.example"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Optional: clamp the APK to arm64-v8a. The audx cherry-pick alone
        // would already produce arm64-only audx binaries, but transitive
        // deps (e.g. androidx.graphics.path) ship universal native libs;
        // abiFilters strips the unused ABIs from those too.
        ndk {
            abiFilters.addAll(setOf("arm64-v8a"))
        }
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    // Cherry-pick arm64-v8a only — drops x86_64 native payload from the APK.
    // Swap in additional `audx-<abi>` lines (or use the meta `audx` artifact)
    // if you want emulator or armv7 support back.
    implementation("com.github.rizukirr.audx-android:audx-core:0.0.1-SNAPSHOT")
    implementation("com.github.rizukirr.audx-android:audx-arm64-v8a:0.0.1-SNAPSHOT")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
