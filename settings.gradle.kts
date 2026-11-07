pluginManagement {
    includeBuild("build-logic")
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Ledger"

// 宿主工程
include(":app")

// 核心支撑层 (Core)
include(":core:model")
include(":core:domain")
include(":core:navigation")
include(":core:testing")
include(":core:common")
include(":core:designsystem")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:network")
include(":core:parser")
include(":core:security")

// 业务特性层 (Feature)
include(":feature:transaction")
include(":feature:account")
include(":feature:statistics")
include(":feature:category")
include(":feature:importer")
include(":feature:backup")
include(":feature:settings")

// 同步与后台调度
include(":sync")
