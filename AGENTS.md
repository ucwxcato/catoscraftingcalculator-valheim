# CatosResourceCalc Agent Notes

## Update system

- Update checking is implemented in `src/main/kotlin/com/cato/resourcecalc/updates/ReleaseChecker.kt`.
- The app checks the public GitHub Releases API for `ucwxcato/craftingcalculator-valheim` once at launch on a background coroutine.
- The footer's **CHECK FOR UPDATES** action performs a manual check and opens the About dialog, where the status and release link are shown.
- Update checks are non-blocking and failures are safe for offline use; the app never replaces files silently.
- `Main.kt` contains the embedded `APP_VERSION`. Bump it for every published prerelease/release so it matches the tag (for example, `0.1.0-alpha.2` with `v0.1.0-alpha.2`).
- Update comparison uses semantic versions and ignores draft GitHub releases.
- The native distribution explicitly includes the `java.net.http` module; keep this when changing packaging or the updater will fail at startup.

## Release verification

- Run `./gradlew test` before publishing.
- Build distributables with `./gradlew packageMsi packageUberJarForCurrentOS`.
- The Windows MSI and runnable JAR are written under `build/compose/` and are uploaded to the matching GitHub Release.
- Keep updater behavior documented in `how-to-publish-releases.md` when the release flow changes.
