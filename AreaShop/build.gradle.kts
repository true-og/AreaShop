plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

version = "2.7.3"

dependencies {
    implementation("com.github.NLthijs48:InteractiveMessenger:e7749258ca")
    implementation("com.github.NLthijs48:BukkitDo:819d51ec2b")
    implementation("io.papermc:paperlib:1.0.6")
    implementation("io.github.baked-libs:dough-data:1.0.3")
    implementation("com.google.inject:guice:5.0.1") {
        exclude("com.google.guava", "guava")
    }
    implementation("com.google.inject.extensions:guice-assistedinject:5.0.1") {
        exclude("com.google.guava", "guava")
    }
    implementation(project(":areashop-interface"))
    implementation(project(":areashop-bukkit-1_13"))
    implementation(project(":areashop-nms"))
    runtimeOnly(project(":areashop-nms-1-17", "reobf"))
    runtimeOnly(project(":areashop-nms-1-18", "reobf"))
    implementation(project(":areashop-worldedit-7"))
    implementation(project(":areashop-worldguard-7"))
    implementation(project(":areashop-fastasyncworldedit"))
    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.8") {
        exclude("com.google.guava", "guava")
    }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.6") {
        exclude("com.google.guava", "guava")
    }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    assemble {
        dependsOn(shadowJar)
        dependsOn(publishToMavenLocal)
    }

    jar {
        archiveClassifier.set("original")
    }

    java {
        withSourcesJar()
    }

    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("AreaShop")
        val base = "me.wiefferink.areashop.libraries"
        relocate("me.wiefferink.interactivemessenger", "${base}.interactivemessenger")
        relocate("me.wiefferink.bukkitdo", "${base}.bukkitdo")
        relocate("io.papermc.lib", "${base}.paperlib")
        relocate("io.github.bakedlibs.dough", "${base}.dough")
        relocate("com.google.inject", "${base}.inject")
        relocate("org.aopalliance", "${base}.aopalliance")
        relocate("javax.inject", "${base}.javax.inject")
    }

}

description = "AreaShop"
