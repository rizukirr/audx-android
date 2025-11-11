pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Audx Example"

// Include audx-android library module from parent project
// We need to reference the parent project's settings to properly include the library
includeBuild("../..") {
    dependencySubstitution {
        substitute(module("com.github.rizukirr:audx-android")).using(project(":app"))
    }
}

include(":app")
