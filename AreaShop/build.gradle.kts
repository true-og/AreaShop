plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

description = "AreaShop"

dependencies {
    // Platform
    compileOnlyApi(libs.spigot)
    compileOnlyApi(libs.worldeditCore) {
        exclude("com.google.guava", "guava")
    }
    compileOnlyApi(libs.worldeditBukkit) {
        exclude("com.google.guava", "guava")
    }
    compileOnlyApi(libs.worldguardCore) {
        exclude("com.google.guava", "guava")
    }
    compileOnlyApi(libs.worldguardBukkit) {
        exclude("com.google.guava", "guava")
    }
    compileOnlyApi("com.github.MilkBowl:VaultAPI:1.7") {
        exclude("com.google.guava", "guava")
    }

    // 3rd party libraries
    implementation(libs.findbugs)
    implementation("io.papermc:paperlib:1.0.7")
    implementation("com.github.NLthijs48:InteractiveMessenger:e7749258ca")
    implementation("com.github.NLthijs48:BukkitDo:819d51ec2b")
    implementation("io.github.baked-libs:dough-data:1.0.3")
    implementation("com.google.inject:guice:5.1.0") {
        exclude("com.google.guava", "guava")
    }
    implementation("com.google.inject.extensions:guice-assistedinject:5.1.0") {
        exclude("com.google.guava", "guava")
    }

    // Project submodules
    implementation(projects.areashopInterface)
    implementation(projects.areashopNms)
    implementation(projects.adapters.platform.platformInterface)
    implementation(projects.adapters.platform.paper)
    // Adapters
    runtimeOnly(projects.adapters.plugins.worldedit)
    runtimeOnly(projects.adapters.plugins.worldguard)
    runtimeOnly(projects.adapters.plugins.fastasyncworldedit)
    runtimeOnly(project(":adapters:platform:bukkit-1-17", "reobf"))
    runtimeOnly(project(":adapters:platform:bukkit-1-18", "reobf"))
    runtimeOnly(project(":adapters:platform:bukkit-1-19", "reobf"))
    runtimeOnly(project(":adapters:platform:bukkit-1-19-3", "reobf"))
}

repositories {
    mavenCentral()
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    assemble {
        dependsOn(shadowJar)
    }

    jar {
        archiveClassifier.set("original")
    }

    java {
        withSourcesJar()
    }

    val javaComponent = project.components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
        skip()
    }

    shadowJar {
        archiveClassifier.set("")
        base {
            archiveBaseName.set("AreaShop")
        }
        val base = "me.wiefferink.areashop.libraries"
        relocate("me.wiefferink.interactivemessenger", "${base}.interactivemessenger")
        relocate("me.wiefferink.bukkitdo", "${base}.bukkitdo")
        relocate("io.papermc.lib", "${base}.paperlib")
        relocate("io.github.bakedlibs.dough", "${base}.dough")
        relocate("com.google.inject", "${base}.inject")
        relocate("org.aopalliance", "${base}.aopalliance")
        relocate("javax.annotation", "${base}.javax.annotation")
        relocate("javax.inject", "${base}.javax.inject")
    }
}
