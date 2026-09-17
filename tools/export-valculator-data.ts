/**
 * Creates the versioned JSON snapshot consumed by CatosResourceCalc.
 *
 * Run from the CatosResourceCalc repository:
 *   corepack yarn dlx tsx tools/export-valculator-data.ts
 *
 * Optional overrides:
 *   --source <path-to-valculator>
 *   --output <path-to-json>
 */

import { execFileSync } from "node:child_process";
import { mkdir, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

type SourceMaterial = {
    id: string;
    name: string;
};

type SourceItem = {
    id: string;
    name: string;
    group: string;
    set: string;
    type: string;
    level?: number;
    materials: Record<string, number>;
    station?: Record<string, number>;
    crafts?: number;
    resultQuantity?: number;
    stats?: Record<string, unknown>;
};

type ExportMaterial = SourceMaterial & {
    craftableItemId?: string;
};

type ExportItem = {
    id: string;
    name: string;
    group: string;
    set: string;
    type: string;
    level?: number;
    materials: Record<string, number>;
    station?: Record<string, number>;
    outputQuantity: number;
};

const schemaVersion = 1;
const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(scriptDirectory, "..");

function argumentValue(flag: string): string | undefined {
    const index = process.argv.indexOf(flag);
    if (index === -1) return undefined;

    const value = process.argv[index + 1];
    if (!value || value.startsWith("--")) {
        throw new Error(`Expected a value after ${flag}.`);
    }
    return value;
}

function normalizeName(name: string): string {
    return name.trim().toLowerCase();
}

function requireString(value: unknown, description: string): string {
    if (typeof value !== "string" || value.trim().length === 0) {
        throw new Error(`${description} must be a non-empty string.`);
    }
    return value;
}

function requirePositiveWholeNumber(value: unknown, description: string): number {
    if (typeof value !== "number" || !Number.isSafeInteger(value) || value <= 0) {
        throw new Error(`${description} must be a positive safe integer.`);
    }
    return value;
}

function runGit(sourceRoot: string, args: string[]): string {
    return execFileSync("git", ["-C", sourceRoot, ...args], {
        encoding: "utf8",
        stdio: ["ignore", "pipe", "pipe"],
    }).trim();
}

function outputQuantityFor(item: SourceItem): number {
    const statsCrafts = item.stats?.crafts;
    const candidates = [item.resultQuantity, item.crafts, statsCrafts];
    const quantity = candidates.find((candidate) => typeof candidate === "number");

    return quantity === undefined
        ? 1
        : requirePositiveWholeNumber(quantity, `Output quantity for ${item.id}`);
}

function assertUniqueIds(records: ReadonlyArray<{ id: string }>, recordType: string) {
    const seen = new Set<string>();
    for (const record of records) {
        if (seen.has(record.id)) {
            throw new Error(`Duplicate ${recordType} ID: ${record.id}`);
        }
        seen.add(record.id);
    }
}

async function loadSourceData(sourceRoot: string): Promise<{
    items: SourceItem[];
    materials: SourceMaterial[];
}> {
    const dataRoot = join(sourceRoot, "packages", "data", "src", "data");
    const itemsModule = await import(pathToFileURL(join(dataRoot, "allItems.data.ts")).href);
    const materialsModule = await import(pathToFileURL(join(dataRoot, "materials.data.ts")).href);

    if (!Array.isArray(itemsModule.allItemsData)) {
        throw new Error("Valculator allItemsData export is not an array.");
    }
    if (!Array.isArray(materialsModule.materialsData)) {
        throw new Error("Valculator materialsData export is not an array.");
    }

    return {
        items: itemsModule.allItemsData as SourceItem[],
        materials: materialsModule.materialsData as SourceMaterial[],
    };
}

async function main() {
    const sourceRoot = resolve(argumentValue("--source") ?? join(projectRoot, "..", "valculator"));
    const outputPath = resolve(
        argumentValue("--output") ?? join(projectRoot, "src", "main", "resources", "data", "valheim-data.json"),
    );

    const { items: sourceItems, materials: sourceMaterials } = await loadSourceData(sourceRoot);
    assertUniqueIds(sourceItems, "item");
    assertUniqueIds(sourceMaterials, "material");

    const materialByName = new Map<string, SourceMaterial>();
    for (const material of sourceMaterials) {
        requireString(material.id, "Material ID");
        const name = requireString(material.name, `Material name for ${material.id}`);
        const normalizedName = normalizeName(name);
        if (materialByName.has(normalizedName)) {
            throw new Error(`Duplicate material name: ${name}`);
        }
        materialByName.set(normalizedName, material);
    }

    const itemCandidatesByName = new Map<string, SourceItem[]>();
    for (const item of sourceItems) {
        requireString(item.id, "Item ID");
        const name = requireString(item.name, `Item name for ${item.id}`);
        if (!item.materials || typeof item.materials !== "object") {
            throw new Error(`Item ${item.id} has no material map.`);
        }

        const normalizedName = normalizeName(name);
        itemCandidatesByName.set(normalizedName, [
            ...(itemCandidatesByName.get(normalizedName) ?? []),
            item,
        ]);
    }

    const items: ExportItem[] = sourceItems
        .map((item) => {
            const materials: Record<string, number> = {};
            for (const [materialName, quantity] of Object.entries(item.materials)) {
                const material = materialByName.get(normalizeName(materialName));
                if (!material) {
                    throw new Error(`Item ${item.id} references unknown material: ${materialName}`);
                }
                materials[material.id] = requirePositiveWholeNumber(
                    quantity,
                    `Quantity of ${materialName} in ${item.id}`,
                );
            }

            return {
                id: item.id,
                name: item.name,
                group: item.group,
                set: item.set,
                type: item.type,
                ...(item.level === undefined ? {} : { level: item.level }),
                materials: Object.fromEntries(Object.entries(materials).sort(([left], [right]) => left.localeCompare(right))),
                ...(item.station === undefined ? {} : { station: item.station }),
                outputQuantity: outputQuantityFor(item),
            };
        })
        .sort((left, right) => left.id.localeCompare(right.id));

    const materials: ExportMaterial[] = sourceMaterials
        .map((material) => {
            const candidates = (itemCandidatesByName.get(normalizeName(material.name)) ?? [])
                .filter((item) => item.level === undefined);

            return {
                id: material.id,
                name: material.name,
                ...(candidates.length === 1 ? { craftableItemId: candidates[0].id } : {}),
            };
        })
        .sort((left, right) => left.id.localeCompare(right.id));

    const document = {
        schemaVersion,
        source: {
            repository: runGit(sourceRoot, ["remote", "get-url", "origin"]),
            commit: runGit(sourceRoot, ["rev-parse", "HEAD"]),
        },
        outputQuantityRule: "resultQuantity, then crafts, then stats.crafts, otherwise 1",
        materials,
        items,
    };

    await mkdir(dirname(outputPath), { recursive: true });
    await writeFile(outputPath, `${JSON.stringify(document, null, 2)}\n`, "utf8");

    console.log(`Exported ${items.length} items and ${materials.length} materials.`);
    console.log(`Source commit: ${document.source.commit}`);
    console.log(`Output: ${outputPath}`);
}

main().catch((error: unknown) => {
    const message = error instanceof Error ? error.stack ?? error.message : String(error);
    console.error(message);
    process.exitCode = 1;
});
