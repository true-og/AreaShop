plugins {
    id("com.gradleup.shadow") version "8.3.9"
    id("xyz.jpenilla.run-paper") version "2.2.3"
}

idea { module { isDownloadSources = true } }

description = "AreaShop-OG"

// The gradle project is still called 'areashop', name every archive after the plugin instead.
base { archivesName.set("AreaShop-OG") }

dependencies {
    // Platform
    compileOnlyApi(libs.spigot)
    compileOnlyApi(libs.worldeditCore)
    compileOnlyApi(libs.worldeditBukkit)
    compileOnlyApi(libs.worldguardCore)
    compileOnlyApi(libs.worldguardBukkit)
    // Economy (DiamondBank-OG API, from the git submodule) and permissions (LuckPerms)
    compileOnlyApi(project(":libs:DiamondBank-OG"))
    compileOnly("net.luckperms:api:5.5")

    // 3rd party libraries
    api("io.papermc:paperlib:1.0.8")
    api("com.github.NLthijs48:InteractiveMessenger:e7749258ca")
    api("com.github.NLthijs48:BukkitDo:819d51ec2b")
    api("io.github.baked-libs:dough-data:1.2.0")
    api("com.google.inject:guice:7.0.0") { exclude("com.google.guava") }
    api("com.google.inject.extensions:guice-assistedinject:7.0.0") { exclude("com.google.guava") }
    implementation("org.incendo:cloud-paper:2.0.0-beta.5") { exclude("com.google.guava") }
    implementation("org.incendo:cloud-processors-confirmation:1.0.0-beta.2") { exclude("com.google.guava") }
    implementation("net.kyori:adventure-text-minimessage:4.16.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.2")
    implementation("org.spongepowered:configurate-yaml:4.1.2")

    // Project submodules
    api(projects.areashopInterface)
    api(projects.areashopNms)
    api(projects.adapters.platform.platformInterface)
    api(projects.adapters.platform.paper)

    if (!providers.environmentVariable("JITPACK").isPresent) {
        // We don't need these adapters if we are only publishing an api jar
        runtimeOnly(projects.adapters.plugins.worldedit)
        runtimeOnly(projects.adapters.plugins.worldguard)
        runtimeOnly(projects.adapters.plugins.essentials)
        runtimeOnly(projects.adapters.platform.bukkitModern)
    }
    testImplementation("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.86.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

repositories { mavenCentral() }

tasks {
    processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }

    assemble {
        if (!providers.environmentVariable("JITPACK").isPresent) {
            dependsOn(shadowJar)
        }
    }

    jar {
        archiveBaseName.set("AreaShop-OG")
        if (!providers.environmentVariable("JITPACK").isPresent) {
            archiveClassifier.set("original")
        } else {
            archiveClassifier.set("")
        }
    }

    if (providers.environmentVariable("JITPACK").isPresent) {
        artifacts { archives(jar) }
    }

    java { withSourcesJar() }

    val javaComponent = project.components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }

    shadowJar {
        archiveClassifier.set("")
        base { archiveBaseName.set("AreaShop-OG") }
        val base = "me.wiefferink.areashop.libraries"
        relocate("org.incendo.cloud", "${base}.cloud")
        relocate("me.wiefferink.interactivemessenger", "${base}.interactivemessenger")
        relocate("me.wiefferink.bukkitdo", "${base}.bukkitdo")
        relocate("io.papermc.lib", "${base}.paperlib")
        relocate("io.github.bakedlibs.dough", "${base}.dough")
        relocate("com.google.inject", "${base}.inject")
        relocate("com.google.errorprone", "${base}.errorprone")
        relocate("org.aopalliance", "${base}.aopalliance")
        relocate("javax.annotation", "${base}.javax.annotation")
        relocate("jakarta.inject", "${base}.jakarta.inject")
        relocate("org.jetbrains.annotations", "${base}.jetbrains.annotations")
        relocate("io.leangen.geantyref", "${base}.geantyref")
        relocate("net.kyori", "${base}.kyori")
        relocate("org.checkerframework", "${base}.checkerframework")
        relocate("org.intellij", "${base}.intellij")
        relocate("org.spongepowered", "${base}.spongepowered")
        relocate("org.yaml.snakeyaml", "${base}.snakeyaml")
    }

    runServer {
        // Configure the Minecraft version for the task.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.19.4")

        downloadPlugins {
            github("EssentialsX", "essentials", "2.20.1", "EssentialsX-2.20.1.jar")
            // LuckPerms (permissions provider). DiamondBank-OG must be installed manually in the
            // test server since it also needs a Postgres + Redis/KeyDB backend to run.
            url("https://download.luckperms.net/1587/bukkit/loader/LuckPerms-Bukkit-5.5.15.jar")
            // WorldEdit 7.2.19
            url("https://mediafilez.forgecdn.net/files/5077/477/worldedit-bukkit-7.2.19.jar")
            // WorldGuard 7.0.7
            url("https://mediafilez.forgecdn.net/files/3677/516/worldguard-bukkit-7.0.7-dist.jar")
        }
    }

    // Register the custom task without the `tasks.` prefix
    register("runCopyJarScript", Exec::class) {
        group = "build"
        description = "Runs the copyjar.sh script after build completion."
        workingDir(rootDir)
        commandLine("sh", "copyjar.sh", project.version.toString())
    }

    // Configure the build task to finalize with the custom task without the `tasks.` prefix
    named("build") { finalizedBy("runCopyJarScript") }
}
