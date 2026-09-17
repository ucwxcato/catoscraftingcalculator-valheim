package com.cato.resourcecalc.data

import kotlinx.serialization.Serializable

@Serializable
data class DataSource(
    val repository: String,
    val commit: String,
)

@Serializable
data class MaterialRecord(
    val id: String,
    val name: String,
    val craftableItemId: String? = null,
)

@Serializable
data class ItemRecord(
    val id: String,
    val name: String,
    val group: String? = null,
    val set: String? = null,
    val type: String? = null,
    val level: Int? = null,
    val materials: Map<String, Long> = emptyMap(),
    val station: Map<String, Int> = emptyMap(),
    val outputQuantity: Long = 1,
)

@Serializable
data class ValheimDataSnapshot(
    val schemaVersion: Int,
    val source: DataSource,
    val outputQuantityRule: String,
    val materials: List<MaterialRecord>,
    val items: List<ItemRecord>,
)

