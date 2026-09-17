import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.Exec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
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

        nativeDistributions {
            // The updater uses java.net.http.HttpClient; Compose's minimized
            // runtime does not infer this module reliably from the app graph.
            modules("java.net.http")
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "CatosResourceCalc"
            packageVersion = "0.1.0"
            description = "A compact offline Valheim crafting-material calculator."
            vendor = "catosaurluna"
            copyright = "Copyright © 2026 catosaurluna"

            windows {
                menuGroup = "CatosResourceCalc"
                shortcut = true
                dirChooser = true
                iconFile.set(project.file("src/main/resources/icons/CatosResourceCalc.ico"))
            }
        }
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

val valculatorSourceDirectory = providers.gradleProperty("valculatorSource")
    .map(::file)
    .orElse(rootProject.projectDir.resolve("../valculator"))
val generatedValheimData = layout.projectDirectory.file("src/main/resources/data/valheim-data.json")
val corepackCommand = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
    "corepack.cmd"
} else {
    "corepack"
}

tasks.register<Exec>("exportValculatorData") {
    group = "data"
    description = "Exports a pinned Valculator TypeScript snapshot to the bundled JSON resource."

    workingDir = projectDir
    commandLine(
        corepackCommand,
        "yarn",
        "dlx",
        "tsx",
        "tools/export-valculator-data.ts",
        "--source",
        valculatorSourceDirectory.get().absolutePath,
        "--output",
        generatedValheimData.asFile.absolutePath,
    )

    inputs.dir(valculatorSourceDirectory)
    inputs.file(projectDir.resolve("tools/export-valculator-data.ts"))
    // The snapshot is committed source data. Keep this task explicit so a
    // standalone application checkout can build without a sibling Valculator clone.
    outputs.upToDateWhen { false }
}
