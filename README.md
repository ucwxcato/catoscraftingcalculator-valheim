# CatosResourceCalc

A compact, offline Valheim crafting-material calculator for Windows. Search for an item, add it to your build plan, choose its recipe level, and get a combined shopping list of the materials you need.

![CatosResourceCalc screenshot](DOCS/screenshots/resourcecalc.png)

## Download and install

You do not need to clone this repository to use the app. Open the [GitHub Releases page](https://github.com/ucwxcato/craftingcalculator-valheim/releases) and choose the newest release.

### Recommended: Windows installer

1. Download the file ending in `.msi`.
2. Open it and follow the Windows installation prompts.
3. Launch **CatosResourceCalc** from the Start menu or desktop shortcut.

The installer includes its own Java runtime. You do not need to install Java, Git, Node, Yarn, or the Valculator repository. If Windows shows a SmartScreen prompt, select **More info**, verify that the app is CatosResourceCalc, and choose **Run anyway** if you trust the download source.

### Portable option: runnable JAR

The file ending in `.jar` is a portable Java version. Use this if Java 25 is already installed or if you prefer not to install the app.

1. Install Java 25 if needed, for example from [Eclipse Adoptium](https://adoptium.net/temurin/releases/).
2. Download the `CatosResourceCalc-windows-x64-*.jar` file from the release.
3. Open PowerShell in the folder where you downloaded it.
4. Run:

   ```powershell
   java -jar .\CatosResourceCalc-windows-x64-0.1.0.jar
   ```

Do not download **Source code (zip)** or **Source code (tar.gz)** unless you want the project files for development. Those archives are not the ready-to-run application.

## Features

- Warm mocha desktop UI with the bundled Minecraft font
- Three-column layout: item search, build plan, and total materials
- Search results collapse same-name recipe levels into one item
- Level selector for upgradeable items such as pickaxes and weapons
- Quantity controls with direct positive-integer validation
- Minus at quantity one removes an item from the plan
- Recursive expansion of craftable ingredients into base materials
- Crafting-station and workstation level details for selected recipes
- Stable `Materials Needed` clipboard output for sharing with other players
- Fully offline at runtime using a versioned bundled data snapshot

## Quick start

Requirements:

- Windows 10 or newer
- Java 25 (the project uses the Gradle toolchain configured in `build.gradle.kts`)

From PowerShell:

```powershell
.\gradlew.bat run
```

To run the verification suite and build the project:

```powershell
.\gradlew.bat test build
```

The application does not need Node, Yarn, Git, or the Valculator checkout after the generated data snapshot has been bundled.

## Release artifacts

Tagged releases publish two Windows downloads automatically through GitHub Actions:

- **MSI installer**: the recommended option for most players. It installs a native Windows application and bundles its Java runtime, so Java does not need to be installed separately.
- **Runnable JAR**: a portable Java executable containing the app, data snapshot, and font. It requires Java 25 and can be launched with:

```powershell
java -jar CatosResourceCalc-windows-x64-0.1.0.jar
```

To create a release, push a version tag such as `v0.1.0`. The workflow runs the tests, builds both artifacts, and attaches them to the GitHub Release automatically.

## Data source

CatosResourceCalc is a separate application. It does not reuse Valculator's website, React UI, routing, or hosting code. The crafting data is imported from Valculator's TypeScript data package, converted into a deterministic schema-v1 JSON snapshot, and bundled into this repository at:

```text
src/main/resources/data/valheim-data.json
```

The snapshot records the upstream repository URL and exact source commit. The checked-in snapshot is what the application uses at runtime; no network access or scraping occurs.

If the sibling checkout at `..\valculator` is available, regenerate the snapshot with:

```powershell
.\gradlew.bat exportValculatorData
```

The exporter validates IDs, material references, quantities, and deterministic output before writing the snapshot. A custom checkout can be supplied with `-PvalculatorSource=C:\path\to\valculator`.

## Project structure

```text
src/main/kotlin/com/cato/resourcecalc/
  calculator/       Pure Kotlin recursive calculation engine
  data/             JSON models, loader, validator, and search index
  platform/         Windows DWM title-bar integration
  ui/               Mocha theme and typography
tools/              Valculator-to-JSON data exporter
DOCS/devplan.md     Implementation plan and release checklist
```

The calculation core has no Compose, filesystem, network, or Valheim-installation dependency, which keeps the data and business rules testable independently from the desktop UI.

## Attribution

The application is authored by **catosaurluna**. The material and recipe records are imported from the [Valculator repository](https://github.com/charlotte-hues/valculator); Valculator is the upstream data source, not this application. The exact imported commit is preserved in `src/main/resources/data/valheim-data.json`, and the upstream Apache 2.0 notice is bundled at `src/main/resources/LICENSE.valculator.txt`.

The bundled `Minecraft.otf` is the [Minecraft Font by Idrees Hassan](https://github.com/IdreesInc/Minecraft-Font), licensed under the SIL Open Font License 1.1. Its copyright notice and license are included at `src/main/resources/LICENSE.minecraft-font.txt`.

## Development status

The core calculator and functional MVP UI are implemented. Remaining release work is focused on final attribution files, packaging a standalone Windows artifact, clean-machine testing, and release polish.
