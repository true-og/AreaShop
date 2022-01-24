pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "paper-repo"
            url = uri("https://papermc.io/repo/repository/maven-public/")
        }
    }
}

buildCache {
    local {
        directory = File(rootDir, "build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}

rootProject.name = "areashop-parent"

// Interfaces
include(":areashop-bukkit-1_13")
include(":areashop-interface")
include(":areashop-nms")

// Adapters
include(":adapters:platform:bukkit-1-17")
include(":adapters:platform:bukkit-1-18")
include(":adapters:plugins:worldedit")
include(":adapters:plugins:worldguard")
include(":adapters:plugins:fastasyncworldedit")

// Main project
include(":areashop")

project(":areashop").projectDir = file("AreaShop")

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")