plugins {
    java
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.5.11" apply false
    id("com.github.spotbugs") version "5.1.3"
    id("com.diffplug.spotless") version "8.1.0"
    idea
    eclipse
}

group = "me.wiefferink"

version = "2.8.0"

val targetJavaVersion = 17
val encoding = Charsets.UTF_8
val encodingName: String = encoding.name()

java.toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))

repositories { mavenCentral() }

spotless {
    kotlinGradle {
        ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) }
        target("build.gradle.kts", "settings.gradle.kts")
    }
}

subprojects {

    // The libs/ submodules (e.g. DiamondBank-OG) are self-contained builds with their own
    // Kotlin/GraalVM toolchain and plugins; do not apply AreaShop's Java/publish config to them.
    if (project.path.startsWith(":libs")) {
        return@subprojects
    }

    group = rootProject.group
    version = rootProject.version

    apply {
        plugin<JavaPlugin>()
        plugin<JavaLibraryPlugin>()
        plugin<IdeaPlugin>()
        plugin<EclipsePlugin>()
        plugin<MavenPublishPlugin>()
        plugin("com.diffplug.spotless")
        // plugin<SpotBugsPlugin>()
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            eclipse().configFile(rootProject.file("config/formatter/eclipse-java-formatter.xml"))
            leadingTabsToSpaces()
            removeUnusedImports()
        }
        kotlinGradle {
            ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) }
            target("build.gradle.kts")
        }
    }

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven {
            name = "jitpack"
            url = uri("https://jitpack.io")
            content { includeGroupByRegex("com\\.github.*") }
        }
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://maven.enginehub.org/repo/")
    }

    dependencies { implementation("org.jetbrains:annotations:24.0.1") }

    java.toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))

    tasks {
        named("compileJava") { dependsOn("spotlessApply") }

        named("spotlessCheck") { dependsOn("spotlessApply") }

        withType(JavaCompile::class) {
            options.release.set(targetJavaVersion)
            options.encoding = encodingName
            options.isFork = true
            options.isDeprecation = true
        }

        withType(Javadoc::class) { options.encoding = encodingName }

        withType(ProcessResources::class) { filteringCharset = encodingName }
    }

    publishing {
        publications {
            create<MavenPublication>(project.name) {
                from(components["java"])
                pom {
                    scm {
                        connection.set("scm:git:git://github.com/md5sha256/AreaShop.git")
                        developerConnection.set("scm:git:ssh://github.com/md5sha256/AreaShop.git")
                        url.set("https://github.com/md5sha256/AreaShop/tree/dev/bleeding")
                    }
                    licenses {
                        license {
                            name.set("GNU General Public License v3.0")
                            url.set("https://github.com/md5sha256/AreaShop/blob/dev/bleeding/LICENSE")
                        }
                    }
                }
            }
        }
    }
}
