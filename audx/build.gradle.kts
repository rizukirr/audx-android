// Meta artifact for audx-android.
//
// Publishes only a POM declaring three runtime dependencies (audx-core,
// audx-arm64-v8a, audx-x86_64). No source, no .jar, no .aar.
//
// We deliberately do NOT apply `java-library`: that plugin's `api(project(...))`
// pulls in Android-library variant attributes (AgpVersionAttr, BuildTypeAttr,
// VariantAttr) that a plain Java consumer doesn't know how to match, so resolution
// fails. By skipping `java-library` and synthesizing the POM via `withXml`, we
// avoid the variant-matching machinery entirely and ship exactly what we want.
plugins {
    `maven-publish`
}

group = "com.github.rizukirr.audx-android"
version = findProperty("VERSION_NAME")?.toString() ?: "0.0.1-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = "audx"
            // No `from(components[...])` — POM-only artifact.

            pom {
                packaging = "pom"

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

                // Hand-write the <dependencies> block so consumers transitively
                // get the three child artifacts at the same version as this meta.
                withXml {
                    val deps = asNode().appendNode("dependencies")
                    listOf("audx-core", "audx-arm64-v8a", "audx-x86_64").forEach { artifact ->
                        deps.appendNode("dependency").apply {
                            appendNode("groupId", "com.github.rizukirr.audx-android")
                            appendNode("artifactId", artifact)
                            appendNode("version", project.version.toString())
                            appendNode("scope", "compile")
                        }
                    }
                }
            }
        }
    }
}
