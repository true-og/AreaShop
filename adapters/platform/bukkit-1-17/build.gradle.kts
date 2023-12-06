plugins {
    id("io.papermc.paperweight.userdev") apply true
}

dependencies {
    paperweight.paperDevBundle("1.17.1-R0.1-SNAPSHOT")
    compileOnly(projects.areashopInterface)
    compileOnly(projects.areashopNms)
}


description = "AreaShop NMS Helper 1-17"

tasks {

    assemble {
        dependsOn("reobfJar")
    }

    named("reobfJar", io.papermc.paperweight.tasks.RemapJar::class) {
        outputJar.set(file("build/libs/${project.name}-${project.version}.jar"))
    }

}