pluginManagement { repositories { gradlePluginPortal() } }

plugins {
    // Resolves JDK toolchains (needed to provision the GraalVM toolchain that the
    // DiamondBank-OG submodule build requests).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

buildCache { local { directory = File(rootDir, "build-cache") } }

rootProject.name = "AreaShop-OG"

// Interfaces
include(":areashop-interface")

include(":areashop-nms")

// Adapters
include(":adapters:platform:paper")

include(":adapters:platform:platform-interface")

include(":adapters:plugins:worldedit")

include(":adapters:plugins:worldguard")

include(":adapters:plugins:essentials")

include(":adapters:platform:bukkit-modern")

// Submodule libraries (git submodules under libs/, like Template-OG)
include(":libs:DiamondBank-OG")

project(":libs:DiamondBank-OG").projectDir = file("libs/DiamondBank-OG")

// Main project
include(":areashop")

project(":areashop").projectDir = file("AreaShop-OG")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
