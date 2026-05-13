import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

kotlin {
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_0
        jvmTarget = JvmTarget.JVM_21
    }
}

android {
    namespace = "com.audx.android"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Maven publishing configuration for JitPack.
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.rizukirr.audx-android"
                artifactId = "audx-core"
                version = findProperty("VERSION_NAME")?.toString() ?: "0.0.1-SNAPSHOT"

                pom {
                    name.set("Audx Android — core")
                    description.set(
                        "Kotlin API and JNI bridge sources for Audx. Requires at least one " +
                            "audx-<abi> companion module (or the meta `audx` artifact) for native libraries."
                    )
                    url.set("https://github.com/rizukirr/audx-android")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("rizukirr")
                            name.set("Rizki")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/rizukirr/audx-android.git")
                        developerConnection.set("scm:git:ssh://github.com/rizukirr/audx-android.git")
                        url.set("https://github.com/rizukirr/audx-android")
                    }
                }
            }
        }
    }
}
