import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

group = "dev.catosaurluna"
version = "0.1.0-SNAPSHOT"
description = "A compact Valheim crafting-material calculator."

extra["author"] = "catosaurluna"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("net.java.dev.jna:jna-platform:5.19.1")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

compose.desktop {
    application {
        mainClass = "com.cato.resourcecalc.MainKt"
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes[
            "Implementation-Title"
        ] = rootProject.name
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Vendor"] = "catosaurluna"
    }
}
