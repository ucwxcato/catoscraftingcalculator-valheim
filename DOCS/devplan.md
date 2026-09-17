# CatosResourceCalc - Development Plan

> **Status:** Partially implemented. The Kotlin/Compose Desktop shell, mocha theme, bundled Minecraft font loading, deterministic Valculator exporter, bundled schema-v1 dataset, pure Kotlin calculation core, and functional MVP interactions now build and test successfully on Windows. Packaging and optional polish remain planned.
>
> **Purpose:** Build a compact, friendly desktop utility that lets a player choose Valheim items and quantities, then immediately see the total materials required to craft them.
>
> **Authority:** This document owns the standalone CatosResourceCalc application, its data-import boundary, calculation rules, UI behavior, tests, and packaging. The Valculator repository remains an upstream data source and is not modified by this project.
>
> **Target:** Kotlin/JVM desktop application for Windows first, using Compose for Desktop. Exact JDK, Kotlin, Compose, and Gradle versions remain a Phase 0 compatibility decision.

## 0. Outcome

The user launches one small window, searches for an item, adds it to a build list with a quantity, and sees an understandable shopping list of required materials. Multiple targets are combined, quantities can be changed without losing the selection, and the result can be copied for use in-game.

Complete when:

```text
launch app -> search/select an item -> set quantity -> add to plan
-> view recursively expanded material totals -> copy/export or clear safely
```

The calculator must work offline after the generated Valculator data has been bundled with the application.

## 1. Locked decisions

- **Standalone boundary:** CatosResourceCalc is a separate application. It must not import or duplicate Valculator's React UI, routing, web deployment, or image pipeline.
- **No scraping:** Valheim item and recipe data is imported from the checked-out Valculator source under `valculator/packages/data/src/data`; HTML scraping is explicitly out of scope.
- **Generated-data boundary:** Kotlin consumes a versioned JSON snapshot generated from Valculator's TypeScript data. Kotlin must not parse TypeScript at runtime.
- **Pinned source:** Every generated dataset records the upstream repository URL and exact source commit. A data update is deliberate and reviewable, not an implicit network fetch at application startup.
- **UI-first utility shape:** The MVP is a single-window desktop utility with fast search, a selected-target list, quantity controls, and a totals panel. No multi-page navigation is planned.
- **UI-neutral engine:** Material expansion and aggregation live in a pure Kotlin module with no desktop/UI dependencies. The UI calls a small calculator API and renders its result.
- **Offline runtime:** The released application does not require network access, accounts, telemetry, or a Valheim server connection.
- **No image dependency in MVP:** Item icons and Valculator image assets are not required for calculation or the first UI. This also avoids the upstream image filenames that Windows cannot represent because they contain `:`.
- **Mocha visual direction:** Use a warm, low-saturation mocha palette with soft cream text, restrained terracotta accents, and no pure black or pure white. The interface should feel calm during long planning sessions while keeping controls and results visually distinct.
- **Minecraft font asset:** Use `Minecraft.otf` as the primary branded UI font, bundled at `src/main/resources/fonts/Minecraft.otf`. Keep a readable fallback font for unsupported glyphs, dense helper text, and accessibility failures.
- **License preservation:** Reused/generated data ships with the upstream Apache 2.0 license and copyright attribution. Any generated-data changes are documented.
- **UI toolkit:** Compose for Desktop is the locked UI toolkit (2026-09-17). It matches the desired compact, polished utility UI and the owner's existing experience. The engine and data contracts remain UI-neutral; switching toolkits requires an explicit plan revision.

## 2. Goals and non-goals

### Goals

- Search Valheim items by name with case-insensitive, normalized matching.
- Add one or more target items to a build plan.
- Set, increment, decrement, and remove target quantities.
- Recursively expand craftable ingredients into total base-material requirements.
- Combine duplicate materials across all selected targets deterministically.
- Show useful context where available: item category, crafting station, level, batch/output quantity, and intermediate components.
- Make the main result legible at a glance in a compact window.
- Copy a plain-text shopping list and optionally export machine-readable JSON/CSV after the MVP is stable.
- Validate the imported dataset before it can be used by the application.
- Provide unit and fixture tests for the calculation rules and data-import boundary.

### Explicitly out of scope

- Recreating Valculator's website, React components, web routing, or hosted service.
- Runtime web scraping, automatic crawling, or network-dependent data updates.
- Editing or publishing Valculator's upstream data from CatosResourceCalc.
- A multiplayer inventory sync, server plugin, account system, cloud saves, or social sharing service.
- Mobile and web builds in the MVP.
- Rendering or shipping Valculator's item images as a prerequisite for calculation.
- Automatic knowledge of the player's current in-game inventory until a separate inventory feature is designed.
- Recipe optimization, alternate-recipe selection, price/economy calculations, or build-time scheduling unless explicitly added later.

## 3. User experience / operational flow

### 3.1 Main window

Proposed compact layout:

```text
+---------------------------------------------------------------+
| CatosResourceCalc     [ Search items... ] [Filters] [Clear]   |
+----------------------------+----------------------------------+
| Matching items              | Build plan / required materials |
| - item name                 | Target A       [-] 1 [+]       |
|   category, station         | Target B       [-] 2 [+]       |
| - item name                 |----------------------------------|
|                             | Total materials                  |
|                             | 20 Wood                           |
|                             | 10 Iron                           |
|                             |  4 Fine Wood                      |
|                             | [Copy list] [Export]              |
+----------------------------+----------------------------------+
| status/help text; data version and source commit              |
+---------------------------------------------------------------+
```

The exact pixel size is a tuning point, but the first usable window should fit on a normal laptop without requiring horizontal scrolling.

The implemented MVP uses a wider three-column variant of this layout: item search on the left, the selected build plan in the middle, and an always-visible total-materials shopping list on the right. The build-plan rows are intentionally compact so multiple targets fit before scrolling.

### 3.2 Happy path

1. Launch the application; bundled data loads and the data-source/version indicator appears.
2. Focus the search field automatically. Typing filters the item list without a network request.
3. Select an item. The result row shows its name and enough context to distinguish variants and levels.
4. Add it to the build plan. If it is already present, focus the existing row instead of creating a duplicate.
5. Change quantity with plus/minus controls, direct numeric entry, or keyboard shortcuts. Quantity must be a positive whole number.
6. The totals panel recomputes immediately and shows aggregated materials. Intermediate craftables may be expandable for explanation.
7. Copy produces a stable plain-text list headed `Materials Needed`, with the selected craft targets listed before their aggregated materials. Export, if enabled, writes the currently displayed calculation only.
8. Clear removes the current plan only after the user confirms if it contains entries; no source data is changed.

### 3.3 Interaction rules

- Empty search shows a useful starter list or a short instruction, not a blank unexplained panel.
- No-match search shows `No items found` and preserves the current plan.
- Keyboard navigation is intentionally out of MVP scope; mouse and standard text-field input remain the primary interaction model.
- Quantity input rejects zero, negatives, decimals, non-numeric text, and overflow with an inline message while retaining the last valid value.
- Removing an item updates totals immediately and cannot alter another target.
- Decreasing a target from quantity one removes it from the build plan, matching the compact stepper behavior.
- Loading, invalid-data, and calculation-error states are explicit and actionable; no silent empty result is allowed.
- The UI must remain usable with keyboard navigation and readable at a modest text scale.

### 3.4 Visual design system

The following palette is the starting point, not a claim that it has already been implemented:

| Token | Proposed color | Use |
|---|---|---|
| `mocha.background` | `#211916` | Main window background |
| `mocha.surface` | `#2B211E` | Panels, search area, build-plan cards |
| `mocha.surfaceElevated` | `#382A25` | Focused cards, menus, dialogs |
| `mocha.border` | `#4B3831` | Subtle separators and card outlines |
| `mocha.textPrimary` | `#F2E7DF` | Main labels and totals |
| `mocha.textSecondary` | `#CDBBB1` | Hints, metadata, station labels |
| `mocha.accent` | `#C98F73` | Primary actions, selected result, quantity controls |
| `mocha.accentHover` | `#DEA98E` | Hover and keyboard-focus accent |
| `mocha.success` | `#A8BEA0` | Valid/export confirmation feedback |
| `mocha.warning` | `#D7B477` | Non-blocking data warnings |
| `mocha.error` | `#D98787` | Invalid input and fatal data errors |

Design rules:

- Keep the background and surfaces warm but dark; avoid high-saturation gradients, glow effects, and noisy decoration.
- Use one clear accent for the primary action so the eye knows where to add an item or copy totals.
- Make the totals panel the strongest visual anchor through spacing and typography, not excessive color.
- Use rounded corners and restrained elevation consistently; do not make every row look like a separate floating card.
- Show keyboard focus with a visible accent outline, not color change alone.
- Verify text, icons, disabled controls, errors, and focus indicators meet an accessibility contrast target before release.
- Keep dense metadata smaller and secondary, but never below the chosen minimum readable text size.

Typography rules:

- Use the bundled Minecraft font for the app title, section headings, item names, quantity controls, and total-material labels.
- Use the Minecraft font at a tested readable size for normal labels where possible; use the fallback font for long diagnostics or characters the font does not support.
- Define explicit font sizes and line heights for title, heading, item, total, metadata, helper, and error text. Do not rely on toolkit defaults.
- Verify punctuation, accented characters, numbers, minus/plus controls, and Valheim item names render correctly across the imported dataset.
- Record the font's provenance and redistribution permission before shipping it; do not distribute an unverified font license.

## 4. Architecture and ownership

### 4.1 Proposed project tree

```text
CatosResourceCalc/
  build.gradle.kts                         # Proposed Kotlin/JVM build
  settings.gradle.kts
  gradle/                                  # Wrapper, if selected
  DOCS/
    devplan.md
  src/main/kotlin/com/cato/resourcecalc/
    Main.kt                                # App entry point
    data/
      DataModels.kt                        # JSON-facing serializable models
      DataLoader.kt                        # Bundled/override dataset loading
      DataValidator.kt                     # Import validation and diagnostics
    domain/
      Item.kt
      Material.kt
      BuildPlan.kt
      CalculationResult.kt
    calculator/
      MaterialCalculator.kt                # Pure recursive expansion
      CalculationErrors.kt
    ui/
      AppState.kt
      MainWindow.kt
      SearchPanel.kt
      BuildPlanPanel.kt
      TotalsPanel.kt
      UiComponents.kt
  src/main/resources/
    data/valheim-data.json                 # Generated, pinned snapshot
    fonts/Minecraft.otf                    # Bundled UI font, license/provenance to verify
    LICENSE.valculator.txt                # Required upstream license copy
  tools/
    export-valculator-data.mjs             # Proposed Node exporter
  src/test/kotlin/...                      # Unit and fixture tests
```

Paths and package names are proposed because the target project currently has no source files.

### 4.2 Ownership boundaries

| Area | Owner | Contract |
|---|---|---|
| Canonical item/recipe facts | Valculator `packages/data` | Upstream TypeScript arrays and types; imported at a pinned commit |
| Data conversion | CatosResourceCalc exporter | Deterministic JSON plus schema/source metadata; fails on invalid references |
| Domain model and calculations | CatosResourceCalc Kotlin core | Pure, deterministic API; no UI or filesystem assumptions |
| Search, selection, controls, rendering | CatosResourceCalc UI | Owns presentation state and user feedback only |
| Copy/export | CatosResourceCalc application layer | Exports the current result; never mutates source data |
| Images/web hosting/upstream releases | Valculator | Not duplicated by this app |

### 4.3 Core API (proposed)

```kotlin
interface MaterialCalculator {
    fun calculate(targets: List<TargetQuantity>): CalculationResult
}

data class TargetQuantity(val itemId: String, val quantity: Long)

data class CalculationResult(
    val totals: Map<String, Long>,
    val breakdown: Map<String, BreakdownNode>,
    val warnings: List<CalculationWarning>
)
```

The UI should depend on this API rather than reaching into JSON maps or recursively calculating inside composables.

## 5. Data, lifecycle, and failure handling

### 5.1 Upstream data facts to preserve

The verified Valculator data package exposes `allItemsData`, `materialsData`, and item/recipe records containing fields such as `id`, `name`, `group`, `set`, `type`, `level`, `materials`, `station`, `stats`, and optional `crafts`. Recipe records include categories such as base, cooked food, eitr, feasts, healing, potions, prepared food, resistance, and stamina.

The exporter must treat the following as source facts rather than silently reinterpret them:

- Material quantities are numeric and may be keyed by display names in the TypeScript source.
- A craftable item can itself be a material for another item, so expansion is a graph traversal rather than a one-level lookup.
- Batch/output fields (`crafts` and recipe stats such as `stats.crafts`) require an explicit output-quantity rule and fixture tests.
- Item variants and levels can share display names; IDs must remain the primary identity.
- Names containing punctuation such as `:` are valid data values even though they must not become Windows filenames.

### 5.2 Generated JSON contract (proposed schema v1)

```json
{
  "schemaVersion": 1,
  "source": {
    "repository": "https://github.com/charlotte-hues/valculator.git",
    "commit": "<40-character commit SHA>"
  },
  "materials": [
    { "id": "m-wood", "name": "wood" }
  ],
  "items": [
    {
      "id": "item-fine-wood",
      "name": "Fine Wood",
      "group": "crafting",
      "set": "fine wood",
      "type": "crafting",
      "level": null,
      "materials": { "m-wood": 1 },
      "crafts": 1,
      "station": { "workbench": 1 }
    }
  ]
}
```

The final schema may retain additional upstream fields, but it must be deterministic, UTF-8, and stable for Kotlin deserialization. Do not include generated timestamps that cause unnecessary diffs.

### 5.3 Import and validation rules

- Fail the export if an item ID is duplicated, a required field is malformed, a quantity is non-positive, or a material reference cannot be resolved.
- Normalize names only for search and reference matching; preserve the original display name for output.
- Preserve upstream IDs when present. If an ID must be regenerated, implement and test the upstream normalization behavior (lowercase, spaces/colon normalization, and level suffix handling) in one place.
- Report source commit, item count, material count, and validation warnings in the exporter output.
- The application refuses to calculate against an invalid or unsupported schema version and shows the user how to regenerate data.

### 5.4 Calculation rules

- Treat each requested target as `(itemId, positive quantity)`.
- Expand an item recursively through its material map and aggregate leaf/base materials by material ID.
- Apply batch/output quantities according to a locked rule from Phase 0: required crafts are rounded up to whole batches, then ingredient quantities are multiplied by the number of batches.
- Keep intermediate nodes in the breakdown so totals can be explained, even if the default view shows only leaf materials.
- Detect cycles with a recursion-path set and return a visible data error identifying the cycle; never recurse indefinitely.
- Use `Long` quantities and checked arithmetic. Overflow returns an error instead of wrapping.
- Preserve deterministic ordering: sort search results and total materials by normalized display name, with ID as a tie-breaker.

### 5.5 Lifecycle and recovery

- Data load occurs once at startup; the immutable dataset is shared by read-only calculations.
- Build-plan edits are in-memory and may be discarded on exit in the MVP. Persistence is a later tuning point.
- A malformed bundled dataset prevents normal startup but provides a clear error and source/schema details.
- A single calculation failure leaves the previous valid result visible until the user changes or removes the offending target.
- Copy/export failures show an actionable message and do not clear or alter the current plan.

## 6. Configuration, permissions, and integrations

### 6.1 Build and data integration

- Proposed Kotlin/JVM build uses Gradle Kotlin DSL and a pinned Kotlin serialization library; exact versions are selected after checking installed JDK/tooling in Phase 0.
- The exporter runs in the Valculator checkout using its Node/Yarn toolchain. The current Valculator root declares Node `>=22.11` and Yarn `4.18.0`; verify those prerequisites before automating export.
- The generated JSON is copied into CatosResourceCalc resources by an explicit command or Gradle task. Runtime startup must not invoke `git`, Node, Yarn, or the network.
- The Windows sparse checkout omission applies to Valculator image filenames only; the `packages/data` source needed by this app must remain included and validated.

### 6.2 User-facing configuration

MVP configuration is intentionally minimal:

```text
bundledDataOnly = true
showIntermediateBreakdown = false
theme = system
```

If a developer data override is added later, it must be an explicit command-line flag or settings option, must validate before use, and must never silently replace the bundled dataset for normal users.

### 6.3 Permissions

No elevated permissions are required. Clipboard access is used only when the user presses Copy. File access is used only when the user explicitly chooses an export destination.

## 7. Safety, security, and product constraints

- Treat imported data as untrusted input at the exporter boundary: validate IDs, names, references, quantities, and recursion depth.
- Never execute code from the JSON dataset or from item names.
- Bound search-result size and recursion depth so a malformed dataset cannot freeze the UI.
- Keep calculations deterministic and local; do not transmit selected items, inventory, or copied text.
- Escape/quote item names safely in text, CSV, and JSON exports. Newlines and punctuation in display names must not corrupt the export format.
- Use a single immutable data snapshot per application run to avoid results changing halfway through a calculation.
- Keep upstream Apache 2.0 attribution with the distributed generated data; document the source commit and local modifications.
- Do not treat the calculator as authoritative for game-version changes until the dataset has been regenerated and validated against a selected upstream commit.

## 8. Verification matrix

| Scenario | Expected result | Evidence required |
|---|---|---|
| Export a pinned Valculator commit | Deterministic JSON is generated and validates | Export command output, item/material counts, repeat-run byte comparison |
| Load valid bundled data | App starts and shows source commit/version | Startup smoke test or test log |
| Search exact and partial names | Matching items appear quickly and deterministically | UI test/manual check with screenshots or recorded steps |
| Add one simple craftable item | Correct direct ingredients are shown | Unit fixture with expected totals |
| Add nested craftable item | Base materials are recursively expanded and intermediate breakdown is available | Unit test and visible UI result |
| Add two targets sharing ingredients | Shared materials are combined exactly once per required quantity | Aggregation unit test |
| Batch-producing recipe | Whole-batch rounding and output quantity follow the locked rule | Dedicated fixture tests for crafts/output fields |
| Duplicate target selection | Existing row quantity is updated; duplicate row is not created | UI interaction test |
| Zero, negative, decimal, text, and huge quantity | Input is rejected without corrupting the last valid result | Validation tests and manual UI check |
| Unknown item/material reference | Calculation refuses with a readable diagnostic | Validator/engine test |
| Cyclic data fixture | Calculation stops with a cycle error and UI remains responsive | Cycle-detection unit test |
| Arithmetic overflow fixture | Calculation fails safely rather than wrapping | Checked-arithmetic test |
| Empty search/no matches | Helpful empty state appears; build plan remains intact | UI manual check |
| Copy result | Clipboard contains stable plain text matching visible totals | Manual clipboard verification |
| Export result | Chosen JSON/CSV file contains only the current calculation | File-output test/manual inspection |
| Restart app | Bundled data loads again; no stale partial calculation is displayed | Restart smoke test |
| Windows packaging | App launches on a clean Windows machine without requiring Node/Yarn | Packaged-artifact smoke test |
| License/source audit | Distributed artifact includes required upstream attribution and commit metadata | Release checklist review |

## 9. Phased checklist

### Phase 0 - Design lock and prerequisites

Phase 0 findings as of 2026-09-17:

- **Verified:** The Valculator checkout is on `main` at commit `0820b7aea090a6d91c8c14868c3d0c70a095456d`; its remote is `https://github.com/charlotte-hues/valculator.git`, and the working tree is clean.
- **Verified:** Node `v24.16.0` is available and satisfies Valculator's declared Node `>=22.11` requirement.
- **Verified:** Corepack provides Yarn `4.18.0`, matching the Valculator repository's bundled Yarn release.
- **Verified:** Java `25.0.3` is installed.
- **Verified:** The Valculator data source currently contains 90 TypeScript data files under `packages/data/src/data`.
- **Verified:** `Minecraft.otf` is copied into `CatosResourceCalc/src/main/resources/fonts/Minecraft.otf` and matches the source file by SHA-256.
- **Verified:** The Gradle `9.6.1` wrapper is present and `gradlew.bat --version` launches successfully on Java `25.0.3`. Standalone `gradle` and `kotlinc` commands remain unavailable, which is acceptable because the project will use the wrapper.
- **Pending:** Exact batch/output semantics and the font's redistribution permission still require an explicit decision or verification.

- [x] Confirm the first release is a Windows desktop utility with a UI-neutral Kotlin/JVM core.
- [x] Verify available JDK, Kotlin/Gradle, Node, and Yarn versions on the development machine; record missing standalone Gradle/Kotlin commands as a prerequisite.
- [x] Lock Compose for Desktop as the UI toolkit; retain a UI-neutral engine so a future toolkit change is contained.
- [x] Pin the Valculator source commit used for the first dataset: `0820b7aea090a6d91c8c14868c3d0c70a095456d`.
- [ ] Inspect all `packages/data` exports and document the exact batch/output interpretation for `crafts` and recipe stats.
- [ ] Confirm the intended Apache 2.0 attribution format for generated data and add it to the project checklist.
- [ ] **Verify:** A small design note records toolkit, toolchain versions, source commit, schema version, and batch rule.

### Phase 1 - Create the standalone project skeleton

- [x] Add the Gradle `9.6.1` wrapper (`gradlew`, `gradlew.bat`, and `gradle/wrapper/*`) and verify `gradlew.bat --version` on Windows.
- [x] Create `CatosResourceCalc/settings.gradle.kts` and `build.gradle.kts` with Kotlin/JVM `2.4.20`, Compose Desktop `1.12.0`, and the Kotlin Compose compiler plugin.
- [x] Add the initial package structure, mocha theme, bundled Minecraft font loading, and a launchable static `Main.kt` utility-window shell.
- [x] Add a test task and deterministic `gradlew.bat build` command; the initial project build succeeds.
- [x] Add `.gitignore` entries for Gradle/build output, local tooling scratch files, IDE state, local settings/secrets, logs, and OS clutter while keeping the wrapper and bundled assets versioned.
- [x] Add `src/main/resources/LICENSE.valculator.txt` and source-attribution documentation linking the upstream Valculator repository.
- [x] Keep the copied `src/main/resources/fonts/Minecraft.otf` in the standalone repository.
- [x] Record the font's provenance and redistribution-license status before a distributable release is produced (`IdreesInc/Minecraft-Font`, SIL Open Font License 1.1).
- [x] **Verify:** The static application builds and launches on the target Windows development machine with `gradlew.bat build` and `gradlew.bat run`.

### Phase 2 - Build the Valculator data exporter

- [x] Add `tools/export-valculator-data.ts` and the on-demand `gradlew.bat exportValculatorData` task.
- [x] Import `allItemsData` and `materialsData` from the pinned Valculator `packages/data` source using `corepack yarn dlx tsx`.
- [x] Convert material-name references to stable material IDs while preserving display names, category fields, station data, level data, and output quantity.
- [x] Emit schema-v1 JSON with repository URL, commit SHA, output-quantity rule, counts, and deterministic ordering.
- [x] Validate duplicate IDs, duplicate material names, missing material references, empty names, absent material maps, and non-positive/non-integral quantities.
- [ ] Add fixtures for punctuation, variants, levels, direct materials, nested materials, and batch-producing recipes.
- [x] Copy the validated output to `src/main/resources/data/valheim-data.json` through an explicit repeatable task.
- [x] **Verify:** Two exports from commit `0820b7aea090a6d91c8c14868c3d0c70a095456d` are byte-identical (SHA-256 `48EF6CFA052168554E81B264B1748F9D31E2C09BAC4EC0206886FD8379BA0603`) and the bundled snapshot contains 1,124 items and 464 materials.
- [ ] **Verify:** Invalid fixture data fails with actionable diagnostics once exporter fixtures are added.

### Phase 3 - Implement the pure Kotlin data and calculation core

- [x] Implement serializable Kotlin models for the locked JSON schema.
- [x] Implement `DataLoader` for the bundled resource and clear schema/version errors.
- [x] Implement `DataValidator` for runtime defense-in-depth checks.
- [x] Implement ID-based item/material indexes and normalized search fields.
- [x] Implement recursive material expansion with batch rounding, `Long` checked arithmetic, cycle detection, depth bounds, and deterministic aggregation.
- [x] Implement breakdown nodes and warnings so the UI can explain how totals were produced.
- [x] Add unit tests for direct, nested, multiple-target, variant/level, batch, unknown-reference, cycle, overflow, and zero-quantity cases.
- [x] **Verify:** Core tests pass without any UI toolkit, filesystem writes, network access, or Valheim installation (`gradlew.bat test`, 5 tests).

### Phase 4 - Implement the compact utility UI

- [x] Create a single-window app shell with a compact default size and sensible minimum size.
- [x] Add the search field, normalized filtering, result list, and no-match state.
- [x] Collapse same-name level variants into one search result and expose level selection in the build-plan row.
- [x] Add the selected-target/build-plan list with add, remove, plus, minus, direct quantity entry, and duplicate-target behavior.
- [x] Add the totals panel with sorted material totals and a UI-ready breakdown API.
- [x] Add explicit startup, invalid-data, validation, and calculation-error states.
- [x] Add Copy-to-Clipboard using stable plain-text formatting.
- [x] Implement the mocha color tokens, typography hierarchy, spacing scale, borders, corner radius, and focus/hover/pressed states from Section 3.4.
- [x] Register `Minecraft.otf` as the primary UI font.
- [x] Style empty, warning, success, and error states using the same palette without relying on color alone to communicate meaning.
- [x] Keep calculation and data state outside composables; composables dispatch user actions and render state.
- [ ] Add accessible labels/tooltips, readable contrast, and system-theme support. Keyboard navigation is intentionally out of MVP scope.
- [ ] **Verify:** Complete a manual happy-path run and finish contrast, focus, font rendering, and fallback checks for the mocha theme.

### Phase 5 - Utility polish and optional exports

- [ ] Add JSON and CSV export of the current calculation only, with safe escaping and a user-selected destination.
- [ ] Add a compact-mode preference only if the default layout cannot remain clear at the target size.
- [x] Add a crafting-station section to the visible and copied result, showing each required bench/workstation and its required upgrade level (for example, Workbench Level 3).
- [ ] Add optional display toggles for stations, intermediate components, and source version.
- [x] Add a small About/source panel containing Valculator attribution and dataset commit.
- [ ] Profile search and recalculation with the complete dataset; avoid premature caching that makes state stale.
- [x] Add a non-blocking GitHub Releases update check on launch and a manual About-dialog action; show the release assets and keep offline use unaffected.
- [ ] **Verify:** Export files round-trip through parser tests, clipboard text matches visible totals, and the UI remains responsive with a full build plan.

### Phase 6 - Packaging and release readiness

- [x] Choose the Windows distribution formats: a Compose MSI with bundled JVM and a runnable Compose uber-JAR for Java users.
- [x] Build release artifacts that include the bundled JSON, font, and license attribution (`packageMsi`, `packageUberJarForCurrentOS`).
- [x] Configure the branded CatosResourceCalc icon for the Compose window and Windows native packages.
- [x] Add a tag-triggered GitHub Actions workflow that publishes both artifacts to the GitHub Releases page.
- [ ] Test on a clean Windows environment without Node, Yarn, Git, or the Valculator checkout installed.
- [ ] Document how to regenerate data from a new Valculator commit and how to review the generated diff.
- [ ] Record the upstream commit, schema version, app version, and known data limitations in release metadata.
- [ ] **Verify:** Clean-machine launch, calculation smoke test, copy/export test, and license/source audit all pass.

## 10. Open tuning points

- **Initial target platform:** Windows is the first target; Linux/macOS packaging can follow after the core is stable.
- **Persistence:** MVP may reset the build plan on exit. Add a local saved plan only if repeated-session use requires it.
- **Inventory subtraction:** Decide later whether the totals panel should accept owned quantities and show `needed = required - owned`.
- **Intermediate display:** Default to leaf-material totals with an optional breakdown; revisit after usability testing.
- **Data update cadence:** Manual pinned-commit regeneration is the safe default; automation can be added after the schema and validation are stable.
- **Image support:** Revisit only if icons materially improve search speed and a cross-platform-safe, licensed asset strategy exists.
- **Theme variants:** Keep the mocha theme as the product identity. Add a light or alternate theme only if accessibility testing or user feedback demonstrates a concrete need.
- **Font coverage:** Keep Minecraft as the visual identity, but tune the fallback boundary after testing names, punctuation, and diagnostic text across the full imported dataset.
