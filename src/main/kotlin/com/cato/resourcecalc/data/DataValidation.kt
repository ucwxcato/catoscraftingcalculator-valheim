package com.cato.resourcecalc.data

class DataValidationException(val diagnostics: List<String>) :
    IllegalArgumentException(diagnostics.joinToString(separator = "\n", prefix = "Invalid Valheim dataset:\n- "))

object DataValidator {
    private val shaPattern = Regex("[0-9a-fA-F]{40}")

    fun validate(snapshot: ValheimDataSnapshot) {
        val errors = buildList {
            if (snapshot.schemaVersion != 1) add("unsupported schemaVersion ${snapshot.schemaVersion}; expected 1")
            if (snapshot.source.repository.isBlank()) add("source.repository is empty")
            if (!shaPattern.matches(snapshot.source.commit)) add("source.commit must be a 40-character hexadecimal SHA")
            if (snapshot.outputQuantityRule.isBlank()) add("outputQuantityRule is empty")

            val duplicateMaterialIds = snapshot.materials.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
            if (duplicateMaterialIds.isNotEmpty()) add("duplicate material IDs: ${duplicateMaterialIds.sorted().joinToString()}")
            val duplicateMaterialNames = snapshot.materials.groupingBy { normalize(it.name) }.eachCount().filterValues { it > 1 }.keys
            if (duplicateMaterialNames.isNotEmpty()) add("duplicate material names: ${duplicateMaterialNames.sorted().joinToString()}")
            snapshot.materials.forEachIndexed { index, material ->
                if (material.id.isBlank()) add("materials[$index].id is empty")
                if (material.name.isBlank()) add("materials[$index].name is empty")
                if (material.craftableItemId != null && material.craftableItemId.isBlank()) {
                    add("materials[$index].craftableItemId is empty")
                }
            }

            val duplicateItemIds = snapshot.items.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
            if (duplicateItemIds.isNotEmpty()) add("duplicate item IDs: ${duplicateItemIds.sorted().joinToString()}")
            val materialIds = snapshot.materials.map { it.id }.toSet()
            val itemIds = snapshot.items.map { it.id }.toSet()
            snapshot.items.forEachIndexed { index, item ->
                if (item.id.isBlank()) add("items[$index].id is empty")
                if (item.name.isBlank()) add("items[$index].name is empty")
                if (item.outputQuantity <= 0) add("items[$index] (${item.id}) outputQuantity must be positive")
                item.materials.forEach { (materialId, quantity) ->
                    if (materialId !in materialIds) add("items[$index] (${item.id}) references unknown material $materialId")
                    if (quantity <= 0) add("items[$index] (${item.id}) has non-positive quantity for $materialId")
                }
            }
            snapshot.materials.forEach { material ->
                if (material.craftableItemId != null && material.craftableItemId !in itemIds) {
                    add("material ${material.id} points to unknown craftable item ${material.craftableItemId}")
                }
            }
        }
        if (errors.isNotEmpty()) throw DataValidationException(errors)
    }

    internal fun normalize(value: String): String = value
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}

