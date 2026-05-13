plugins {
    `java-library`
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(project(":audx-core"))
    api(project(":audx-arm64-v8a"))
    api(project(":audx-x86_64"))
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

            groupId = "com.github.rizukirr"
            artifactId = "audx"
            version = findProperty("VERSION_NAME")?.toString() ?: "0.0.1-SNAPSHOT"

            pom {
                name.set("Audx Android (meta)")
                description.set(
                    "Meta artifact for Audx Android — pulls in audx-core plus the default ABI " +
                        "set (arm64-v8a + x86_64). Cherry-pick alternative: depend on audx-core " +
                        "plus only the audx-<abi> modules you need."
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
