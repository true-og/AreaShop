pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "paper-repo"
            url = uri("https://papermc.io/repo/repository/maven-public/")
        }
    }
}

rootProject.name = "areashop-parent"
include(":areashop-bukkit-1_13")
include(":areashop-worldguard-7")
include(":areashop-interface")
include(":areashop-nms")
include(":areashop-nms-1-17")
include(":areashop-nms-1-18")
include(":areashop-worldedit-7")
include(":areashop-fastasyncworldedit")
include(":areashop")
project(":areashop").projectDir = file("AreaShop")
project(":areashop-nms").projectDir = file("hooks/areashop-nms")
project(":areashop-nms-1-17").projectDir = file("hooks/areashop-nms-1-17")
project(":areashop-nms-1-18").projectDir = file("hooks/areashop-nms-1-18")
project(":areashop-worldedit-7").projectDir = file("hooks/areashop-worldedit-7")
project(":areashop-worldguard-7").projectDir = file("hooks/areashop-worldguard-7")
project(":areashop-fastasyncworldedit").projectDir = file("hooks/areashop-fastasyncworldedit")

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")