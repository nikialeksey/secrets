pluginManagement {
    repositories {
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
        create("libs") {
            // relative path to the version catalog file holding your tool versions
            from(files("./../gradle/libs.versions.toml")) // load versions
        }
    }
}

rootProject.name = "secrets-gradle-plugin"

 