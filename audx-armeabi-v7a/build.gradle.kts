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
    namespace = "com.audx.android.armeabiv7a"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
        ndk {
            abiFilters.addAll(setOf("armeabi-v7a"))
        }
        externalNativeBuild {
            cmake {
                cppFlags("")
                arguments += listOf("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
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
    api(project(":audx-core"))
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.rizukirr.audx-android"
                artifactId = "audx-armeabi-v7a"
                version = findProperty("VERSION_NAME")?.toString() ?: "0.0.1-SNAPSHOT"

                pom {
                    name.set("Audx Android — armeabi-v7a native libraries")
                    description.set("Native libraries for the armeabi-v7a (armv7) ABI. NEON required. Pair with audx-core; not included in the slim default meta `audx` artifact (cherry-pick only).")
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
