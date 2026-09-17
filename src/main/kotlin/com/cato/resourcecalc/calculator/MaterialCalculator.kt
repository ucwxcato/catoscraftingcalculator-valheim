package com.cato.resourcecalc.calculator

import com.cato.resourcecalc.data.DataIndex

data class TargetQuantity(val itemId: String, val quantity: Long)

data class BreakdownNode(
    val itemId: String,
    val itemName: String,
    val requestedQuantity: Long,
    val batches: Long,
    val outputQuantity: Long,
    val children: List<BreakdownNode>,
)

data class CalculationWarning(val message: String)

data class CalculationResult(
    val totals: Map<String, Long>,
    val breakdown: List<BreakdownNode>,
    val warnings: List<CalculationWarning> = emptyList(),
) {
    fun totalFor(materialId: String): Long = totals[materialId] ?: 0L
}

class CalculationException(message: String) : IllegalArgumentException(message)

class MaterialCalculator(
    private val data: DataIndex,
    private val maxDepth: Int = 128,
) {
    init { require(maxDepth > 0) { "maxDepth must be positive" } }

    fun calculate(targets: List<TargetQuantity>): CalculationResult {
        val totals = linkedMapOf<String, Long>()
        val roots = targets.map { target ->
            validateTarget(target)
            expand(target.itemId, target.quantity, emptySet(), 0, totals)
        }
        return CalculationResult(totals.toSortedMap(), roots)
    }

    private fun validateTarget(target: TargetQuantity) {
        if (target.quantity <= 0) throw CalculationException("Target quantity must be positive: ${target.itemId}")
        if (target.itemId !in data.itemsById) throw CalculationException("Unknown target item: ${target.itemId}")
    }

    private fun expand(
        itemId: String,
        requestedQuantity: Long,
        path: Set<String>,
        depth: Int,
        totals: MutableMap<String, Long>,
    ): BreakdownNode {
        if (depth > maxDepth) throw CalculationException("Maximum recipe depth of $maxDepth exceeded at $itemId")
        if (itemId in path) throw CalculationException("Recipe cycle detected: ${(path + itemId).joinToString(" -> ")}")
        val item = data.itemsById[itemId] ?: throw CalculationException("Unknown craftable item: $itemId")
        val batches = ceilDiv(requestedQuantity, item.outputQuantity)
        val children = item.materials.entries.sortedBy { it.key }.map { (materialId, perBatch) ->
            val needed = checkedMultiply(perBatch, batches, "$itemId -> $materialId")
            val material = data.materialsById[materialId]
                ?: throw CalculationException("Unknown material reference: $materialId")
            val craftableItemId = material.craftableItemId
            if (craftableItemId != null) {
                expand(craftableItemId, needed, path + itemId, depth + 1, totals)
            } else {
                addTotal(totals, materialId, needed)
                null
            }
        }.filterNotNull()
        return BreakdownNode(itemId, item.name, requestedQuantity, batches, item.outputQuantity, children)
    }

    private fun addTotal(totals: MutableMap<String, Long>, id: String, amount: Long) {
        totals[id] = try { Math.addExact(totals[id] ?: 0L, amount) }
        catch (_: ArithmeticException) { throw CalculationException("Material total overflow for $id") }
    }

    private fun checkedMultiply(a: Long, b: Long, context: String): Long = try { Math.multiplyExact(a, b) }
    catch (_: ArithmeticException) { throw CalculationException("Material quantity overflow at $context") }

    private fun ceilDiv(value: Long, divisor: Long): Long = value / divisor + if (value % divisor == 0L) 0L else 1L
}
