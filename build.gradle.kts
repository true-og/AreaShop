import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsPlugin

plugins {
    java
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.5.10" apply false
    id("com.github.spotbugs") version "5.1.3"
    idea
    eclipse
}

group = "me.wiefferink"
version = "2.7.17-SNAPSHOT"

val targetJavaVersion = 17
val encoding = Charsets.UTF_8
val encodingName: String = encoding.name()

java.toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))

subprojects {

    group = rootProject.group
    version = rootProject.version

    apply {
        plugin<JavaPlugin>()
        plugin<JavaLibraryPlugin>()
        if (project.path.contains("platform").not()) {
            plugin<MavenPublishPlugin>()
        }
        plugin<IdeaPlugin>()
        plugin<EclipsePlugin>()
        // plugin<SpotBugsPlugin>()
    }
    
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven {
            name = "jitpack"
            url = uri("https://jitpack.io")
            content {
                includeGroupByRegex("com\\.github.*")
            }
        }
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://maven.enginehub.org/repo/")
    }

    java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

    tasks {
        withType(JavaCompile::class) {
            options.release.set(targetJavaVersion)
            options.encoding = encodingName
            options.isFork = true
            options.isDeprecation = true
        }

        withType(Javadoc::class) {
            options.encoding = encodingName
        }

        withType(ProcessResources::class) {
            filteringCharset = encodingName
        }
    }
}
