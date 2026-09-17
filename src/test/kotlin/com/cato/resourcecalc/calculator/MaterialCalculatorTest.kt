package com.cato.resourcecalc.calculator

import com.cato.resourcecalc.data.DataIndex
import com.cato.resourcecalc.data.DataValidationException
import com.cato.resourcecalc.data.DataValidator
import com.cato.resourcecalc.data.DataSource
import com.cato.resourcecalc.data.ItemRecord
import com.cato.resourcecalc.data.MaterialRecord
import com.cato.resourcecalc.data.ValheimDataSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MaterialCalculatorTest {
    @Test
    fun expandsNestedRecipesAndRoundsBatches() {
        val data = index(
            materials = listOf(
                MaterialRecord("m-ore", "Ore"),
                MaterialRecord("m-ingot", "Ingot", craftableItemId = "ingot"),
            ),
            items = listOf(
                ItemRecord("ingot", "Ingot", materials = mapOf("m-ore" to 3), outputQuantity = 2),
                ItemRecord("sword", "Sword", materials = mapOf("m-ingot" to 2)),
            ),
        )

        val result = MaterialCalculator(data).calculate(listOf(TargetQuantity("sword", 3)))

        // Three swords need six ingots; each batch makes two, so nine ore.
        assertEquals(9L, result.totalFor("m-ore"))
        assertEquals(1, result.totals.size)
        assertEquals(1, result.breakdown.size)
        assertEquals(3L, result.breakdown.single().requestedQuantity)
        assertEquals(3L, result.breakdown.single().children.single().batches)
    }

    @Test
    fun combinesMultipleTargetsDeterministically() {
        val data = index(
            materials = listOf(MaterialRecord("m-wood", "Wood"), MaterialRecord("m-stone", "Stone")),
            items = listOf(
                ItemRecord("axe", "Axe", materials = mapOf("m-wood" to 2, "m-stone" to 1)),
                ItemRecord("hoe", "Hoe", materials = mapOf("m-wood" to 1)),
            ),
        )
        val result = MaterialCalculator(data).calculate(
            listOf(TargetQuantity("hoe", 2), TargetQuantity("axe", 1)),
        )
        assertEquals(listOf("m-stone", "m-wood"), result.totals.keys.toList())
        assertEquals(4L, result.totalFor("m-wood"))
        assertEquals(1L, result.totalFor("m-stone"))
    }

    @Test
    fun keepsVariantIdsDistinctWhileSearchingByName() {
        val data = index(
            materials = listOf(MaterialRecord("m-wood", "Wood")),
            items = listOf(
                ItemRecord("club1", "Club", level = 1, materials = mapOf("m-wood" to 5)),
                ItemRecord("club2", "Club", level = 2, materials = mapOf("m-wood" to 8)),
            ),
        )
        assertEquals(listOf("club1", "club2"), data.search(" CLUB ").map { it.id })
        assertEquals(listOf("club1"), data.searchDistinct("club").map { it.id })
        assertEquals(listOf("club1", "club2"), data.variantsFor("club1").map { it.id })
        assertEquals(8L, MaterialCalculator(data).calculate(listOf(TargetQuantity("club2", 1))).totalFor("m-wood"))
    }

    @Test
    fun rejectsUnknownZeroAndOverflowInputs() {
        val data = index(
            materials = listOf(MaterialRecord("m-wood", "Wood")),
            items = listOf(ItemRecord("log", "Log", materials = mapOf("m-wood" to Long.MAX_VALUE))),
        )
        val calculator = MaterialCalculator(data)
        assertFailsWith<CalculationException> { calculator.calculate(listOf(TargetQuantity("missing", 1))) }
        assertFailsWith<CalculationException> { calculator.calculate(listOf(TargetQuantity("log", 0))) }
        assertFailsWith<CalculationException> { calculator.calculate(listOf(TargetQuantity("log", 2))) }
    }

    @Test
    fun detectsCycles() {
        val data = index(
            materials = listOf(
                MaterialRecord("m-a", "A", craftableItemId = "a"),
                MaterialRecord("m-b", "B", craftableItemId = "b"),
            ),
            items = listOf(
                ItemRecord("a", "A", materials = mapOf("m-b" to 1)),
                ItemRecord("b", "B", materials = mapOf("m-a" to 1)),
            ),
        )
        val error = assertFailsWith<CalculationException> {
            MaterialCalculator(data).calculate(listOf(TargetQuantity("a", 1)))
        }
        assertTrue(error.message!!.contains("cycle", ignoreCase = true))
    }

    @Test
    fun validatesBrokenReferencesAndLoadsBundledData() {
        val invalid = ValheimDataSnapshot(
            1,
            DataSource("repo", "not-a-sha"),
            "rule",
            listOf(MaterialRecord("m", "Wood")),
            listOf(ItemRecord("i", "Item", materials = mapOf("missing" to 1))),
        )
        assertFailsWith<DataValidationException> { DataValidator.validate(invalid) }
        val bundled = com.cato.resourcecalc.data.DataLoader().loadBundled()
        assertTrue(bundled.itemsById.isNotEmpty())
        assertTrue(bundled.search("wood").isNotEmpty())
    }

    private fun index(materials: List<MaterialRecord>, items: List<ItemRecord>): DataIndex = DataIndex(
        ValheimDataSnapshot(
            schemaVersion = 1,
            source = DataSource("https://example.test", "0123456789abcdef0123456789abcdef01234567"),
            outputQuantityRule = "test",
            materials = materials,
            items = items,
        ).also(DataValidator::validate),
    )
}
