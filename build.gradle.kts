import proguard.gradle.ProGuardTask

defaultTasks("buildOfficial")

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.4.2")
    }
}

plugins {
    id("fabric-loom") version "1.5-SNAPSHOT"
}

base {
    archivesName = "Jerinin-addon-1.20.4"
    version = "1.20.4-beta3"
    group = "com.example"
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.meteordev.org/releases") }
    maven { url = uri("https://maven.meteordev.org/snapshots") }
}

dependencies {
    minecraft("com.mojang:minecraft:1.20.4")
    mappings("net.fabricmc:yarn:1.20.4+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.15.11")
    modImplementation("meteordevelopment:meteor-client:0.5.6")
    modCompileOnly("meteordevelopment:baritone:1.20.4-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("Jerinin-addon-1.20.4-Internal-${project.version}.jar")
    from("LICENSE") {
        rename { "${it}_Jerinin-addon" }
    }
}

tasks.remapJar {
    archiveFileName.set("Jerinin-addon-1.20.4-Personal-${project.version}.jar")
}

tasks.register<ProGuardTask>("obfuscate") {
    dependsOn(tasks.remapJar)
    val input = tasks.remapJar.get().archiveFile.get().asFile
    injars(input)
    outjars(file("build/libs/Jerinin-addon-1.20.4-Official-${project.version}.jar"))
    libraryjars(files(sourceSets.main.get().compileClasspath))
    configuration(file("proguard.pro"))
}

tasks.register("buildPersonal") {
    group = "build"
    dependsOn(tasks.remapJar)
}

tasks.register("buildOfficial") {
    group = "build"
    dependsOn(tasks.named("obfuscate"))
}
