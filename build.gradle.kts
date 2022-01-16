plugins {
    java
    `java-library`
    `maven-publish`

    id("io.papermc.paperweight.userdev") version "1.3.3" apply false

    idea
    eclipse
}

group = "me.wiefferink"
version = "2.7.3"

apply {
    plugin<MavenPublishPlugin>()
}

subprojects {

    version = "2.7.3"

    apply {
        plugin<JavaPlugin>()
        plugin<JavaLibraryPlugin>()
        plugin<MavenPublishPlugin>()

        plugin<IdeaPlugin>()
        plugin<EclipsePlugin>()
    }

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://papermc.io/repo/repository/maven-public/")
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

    tasks {
        withType(JavaCompile::class) {
            java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
            options.release.set(17)
            options.encoding = Charsets.UTF_8.name()
            options.isFork = true
            options.isDeprecation = true
        }

        withType(Javadoc::class) {
            options.encoding = Charsets.UTF_8.name()
        }

        withType(ProcessResources::class) {
            filteringCharset = Charsets.UTF_8.name()
        }

        publishing {
            publications {
                create<MavenPublication>(project.name) {
                    from(project.components["java"])
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
}

