pluginManagement {
    repositories {
        google()
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
    versionCatalogs {
        libs {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Superwall Example App"
include(":app")

include(":superwall")
project(":superwall").projectDir = file("../superwall")
