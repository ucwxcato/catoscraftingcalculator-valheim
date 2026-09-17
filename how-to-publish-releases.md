# How to publish CatosResourceCalc releases automatically

This project is configured so you do not need to create releases manually on the GitHub website. Pushing a version tag starts GitHub Actions, which builds and publishes the Windows installer and runnable Java JAR.

Repository: https://github.com/ucwxcato/craftingcalculator-valheim

## Current Alpha milestone

The first Alpha source tag has already been pushed:

```text
v0.1.0-alpha.1
```

That tag points to the icon-enabled Alpha source. The GitHub Release and its MSI/JAR assets still need to be created manually while the account's GitHub Actions billing lock is active. Once the release is published, its assets can be downloaded from the repository's Releases page and used to test the updater flow.

## What gets published

Every tagged release produces two downloadable files:

- **MSI installer** - recommended for most Windows users. It includes a bundled Java runtime.
- **Runnable JAR** - portable Java application. Users need Java 25 installed and run it with `java -jar`.

The workflow is stored at `.github/workflows/release.yml` and runs on tags beginning with `v`.

## First-time setup

You need:

- Git installed and authenticated for the repository
- Permission to push to the repository
- A clean checkout on the `main` branch

The GitHub Actions workflow uses the repository's automatic `GITHUB_TOKEN`; no separate release-token setup is required. Its workflow permission is already set to allow writing releases.

## Publish a release

Open PowerShell in the project folder:

```powershell
cd "C:\Users\magni\Documents\BotsnCoding\Valheim\CatosResourceCalc"
```

### 1. Update the version

Edit `build.gradle.kts` and update both version values when changing versions:

```kotlin
version = "0.1.0"
```

```kotlin
packageVersion = "0.1.0"
```

Use a three-part version such as `0.1.0`, `0.1.1`, or `1.0.0`. Commit and push the version change:

```powershell
git switch main
git pull --ff-only
.\gradlew.bat test build
git add build.gradle.kts
git commit -m "chore: prepare release 0.1.0"
git push origin main
```

Replace `0.1.0` in the commit message with the version you are actually releasing.

### 2. Create and push the release tag

Create an annotated tag whose name starts with `v`:

```powershell
git tag -a v0.1.0 -m "CatosResourceCalc 0.1.0"
git push origin v0.1.0
```

The tag push is the publish action. You do not need to open the GitHub release form first. Tags containing a hyphen, such as `v0.1.0-alpha.1`, are published as GitHub pre-releases automatically.

### 3. Watch the automated build

Open the repository's **Actions** tab and select **Release Windows artifacts**. The workflow will:

1. Check out the tagged source.
2. Install Java 25 on a Windows runner.
3. Run the test suite.
4. Build the MSI with `packageMsi`.
5. Build the runnable JAR with `packageUberJarForCurrentOS`.
6. Create a GitHub Release for the tag.
7. Upload both files to that release.

When it finishes successfully, open the **Releases** tab. The release will contain files similar to:

```text
CatosResourceCalc-0.1.0.msi
CatosResourceCalc-windows-x64-0.1.0.jar
```

## Verify artifacts locally before tagging

You can build the same files locally:

```powershell
.\gradlew.bat test packageMsi packageUberJarForCurrentOS --console=plain
```

The outputs are written to:

```text
build/compose/binaries/main/msi/
build/compose/jars/
```

The MSI bundles the runtime. To launch the JAR locally, use Java 25:

```powershell
java -jar .\build\compose\jars\CatosResourceCalc-windows-x64-0.1.0.jar
```

## Release checklist

- [ ] Update `version` and `packageVersion`.
- [ ] Review the README and release notes.
- [ ] Run `.\gradlew.bat test build` successfully.
- [ ] Build and smoke-test the MSI and JAR locally if practical.
- [ ] Commit and push the version change to `main`.
- [ ] Create an annotated `vX.Y.Z` tag.
- [ ] Push the tag.
- [ ] Confirm the GitHub Actions job succeeds.
- [ ] Download the MSI from Releases and test installation.
- [ ] Download the JAR from Releases and test it with Java 25.

## Common problems

### The workflow does not start

The tag must start with `v`, for example `v0.1.0`. A tag named `0.1.0` will not match the workflow trigger.

### The workflow fails while publishing

Check the workflow job permissions and confirm that the repository allows GitHub Actions to write contents. The workflow declares `contents: write` and normally works with the automatic repository token.

### A tag already exists

Do not reuse a published version number. Pick the next version, such as `v0.1.1`. If a tag was created locally but not pushed yet, inspect it with:

```powershell
git tag --list
git show v0.1.0
```

### The build fails before publishing

The release workflow intentionally stops before creating a release if tests or packaging fail. Reproduce the failure locally with:

```powershell
.\gradlew.bat test packageMsi packageUberJarForCurrentOS --console=plain
```

Fix the issue, push the fix to `main`, and create a new version tag.

## Important distinction

Pushing to `main` updates the source repository but does **not** create a public release. Pushing a `vX.Y.Z` tag is what starts the release workflow and publishes the downloadable artifacts.

## Planned in-app updater

The first updater implementation should be an opt-in **Check for updates** action in the About dialog:

1. Request the public GitHub Releases metadata over HTTPS.
2. Compare the newest compatible release version with the app's current version.
3. Show the version, release notes, and the matching MSI/JAR download link when an update is available.
4. Let the user open the download page or download the selected asset.

The app should remain fully usable offline when the check fails or is declined. Silent self-replacement is intentionally deferred: an MSI update can require Windows elevation, and a running JAR cannot safely overwrite itself. Before downloading updates directly, add HTTPS-only checks, repository/asset-name validation, and published SHA-256 checksums.
