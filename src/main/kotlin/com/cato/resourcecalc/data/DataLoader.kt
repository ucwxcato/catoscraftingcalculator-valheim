package com.cato.resourcecalc.data

import kotlinx.serialization.json.Json

class DataLoader(
    private val json: Json = Json { ignoreUnknownKeys = false }
) {
    fun loadBundled(resourcePath: String = "/data/valheim-data.json"): DataIndex {
        val stream = DataLoader::class.java.getResourceAsStream(resourcePath)
            ?: throw DataLoadException("Bundled dataset not found at $resourcePath")
        val snapshot = stream.use { input ->
            try {
                json.decodeFromString<ValheimDataSnapshot>(input.readBytes().toString(Charsets.UTF_8))
            } catch (error: Exception) {
                throw DataLoadException("Could not parse bundled dataset: ${error.message}", error)
            }
        }
        DataValidator.validate(snapshot)
        return DataIndex(snapshot)
    }
}

class DataLoadException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

class DataIndex internal constructor(val snapshot: ValheimDataSnapshot) {
    val itemsById: Map<String, ItemRecord> = snapshot.items.associateBy { it.id }
    val materialsById: Map<String, MaterialRecord> = snapshot.materials.associateBy { it.id }

    init {
        require(itemsById.size == snapshot.items.size) { "Dataset contains duplicate item IDs" }
        require(materialsById.size == snapshot.materials.size) { "Dataset contains duplicate material IDs" }
    }

    fun search(query: String, limit: Int = 100): List<ItemRecord> {
        require(limit > 0) { "limit must be positive" }
        val normalizedQuery = DataValidator.normalize(query)
        return itemsById.values
            .asSequence()
            .filter { normalizedQuery.isEmpty() || DataValidator.normalize(it.name).contains(normalizedQuery) }
            .sortedWith(compareBy<ItemRecord> { DataValidator.normalize(it.name) }.thenBy { it.id })
            .take(limit)
            .toList()
    }

    /** Returns one searchable entry per display name, retaining level variants for the plan selector. */
    fun searchDistinct(query: String, limit: Int = 100): List<ItemRecord> {
        require(limit > 0) { "limit must be positive" }
        val normalizedQuery = DataValidator.normalize(query)
        return itemsById.values
            .asSequence()
            .filter { normalizedQuery.isEmpty() || DataValidator.normalize(it.name).contains(normalizedQuery) }
            .groupBy { DataValidator.normalize(it.name) }
            .values
            .asSequence()
            .sortedBy { group -> group.minOf { DataValidator.normalize(it.name) } }
            .flatMap { group ->
                if (group.size > 1 && group.any { it.level != null }) {
                    sequenceOf(group.sortedWith(compareBy<ItemRecord> { it.level ?: Int.MAX_VALUE }.thenBy { it.id }).first())
                } else {
                    group.asSequence().sortedBy { it.id }
                }
            }
            .take(limit)
            .toList()
    }

    fun variantsFor(itemId: String): List<ItemRecord> {
        val item = itemsById[itemId] ?: return emptyList()
        return itemsById.values
            .filter { DataValidator.normalize(it.name) == DataValidator.normalize(item.name) && it.level != null }
            .sortedWith(compareBy<ItemRecord> { it.level }.thenBy { it.id })
    }
}
