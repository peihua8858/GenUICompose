pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.aliyun.com/repository/public/")
        }
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
        maven {
            url = uri("https://maven.aliyun.com/repository/public/")
        }
    }
}

rootProject.name = "GenUICompose"
include(":app")
include(":genui")
include(":genai_primitives")
include(":genui_a2a")
include(":json_schema_builder")
