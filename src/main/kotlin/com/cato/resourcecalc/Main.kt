package com.cato.resourcecalc

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cato.resourcecalc.calculator.CalculationResult
import com.cato.resourcecalc.calculator.MaterialCalculator
import com.cato.resourcecalc.calculator.TargetQuantity
import com.cato.resourcecalc.data.DataIndex
import com.cato.resourcecalc.data.DataLoader
import com.cato.resourcecalc.data.ItemRecord
import com.cato.resourcecalc.platform.WindowsTitleBar
import com.cato.resourcecalc.ui.CatosResourceCalcTheme
import com.cato.resourcecalc.ui.MochaColors
import java.awt.Color as AwtColor
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private const val APP_TITLE = "CatosResourceCalc"

fun main() = application {
    Window(onCloseRequest = ::exitApplication, state = rememberWindowState(width = 1_060.dp, height = 700.dp), title = APP_TITLE) {
        window.minimumSize = Dimension(860, 560)
        window.background = AwtColor(0x1E, 0x16, 0x14)
        CatosResourceCalcTheme {
            LaunchedEffect(window) { WindowsTitleBar.apply(window) }
            ResourceCalcApp()
        }
    }
}

private data class PlanEntry(val itemId: String, val quantity: Long)

@Composable
private fun ResourceCalcApp() {
    val loaded = remember { runCatching { DataLoader().loadBundled() } }
    if (loaded.isFailure) {
        ErrorPanel(loaded.exceptionOrNull()?.message ?: "The bundled dataset could not be loaded.")
        return
    }
    val data = loaded.getOrThrow()
    val calculator = remember(data) { MaterialCalculator(data) }
    var query by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf(emptyList<PlanEntry>()) }
    var showClear by remember { mutableStateOf(false) }
    var copyFeedback by remember { mutableStateOf<String?>(null) }
    val results = remember(query, data) { data.search(query, 80) }
    val calculation = remember(plan, calculator) { runCatching { calculator.calculate(plan.map { TargetQuantity(it.itemId, it.quantity) }) } }

    Surface(Modifier.fillMaxSize(), color = MochaColors.Background) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Header(query) { query = it }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SearchPanel(query, results, plan.mapTo(mutableSetOf()) { it.itemId }, { item -> plan = plan.addOrIncrement(item.id); copyFeedback = null }, Modifier.weight(.92f).fillMaxHeight())
                PlanPanel(
                    data = data,
                    plan = plan,
                    calculation = calculation.getOrNull(),
                    calculationError = calculation.exceptionOrNull(),
                    copyFeedback = copyFeedback,
                    onIncrease = { plan = plan.changeQuantity(it, 1) },
                    onDecrease = { plan = plan.changeQuantity(it, -1) },
                    onSetQuantity = { id, quantity -> plan = plan.setQuantity(id, quantity) },
                    onRemove = { id -> plan = plan.filterNot { it.itemId == id } },
                    onClear = { showClear = true },
                    onCopy = { calculation.getOrNull()?.let { copyToClipboard(formatTotals(data, it)); copyFeedback = "Copied totals to clipboard" } },
                    Modifier.weight(1.08f).fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("Created by catosaurluna  ·  Valculator ${data.snapshot.source.commit.take(8)}", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
        }
    }
    if (showClear) AlertDialog(
        onDismissRequest = { showClear = false },
        containerColor = MochaColors.SurfaceElevated,
        title = { Text("Clear build plan?") },
        text = { Text("This removes the selected targets but keeps the imported data.") },
        confirmButton = { TextButton(onClick = { plan = emptyList(); copyFeedback = null; showClear = false }) { Text("CLEAR", color = MochaColors.AccentHover) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("CANCEL") } },
    )
}

private fun List<PlanEntry>.addOrIncrement(id: String): List<PlanEntry> = if (any { it.itemId == id }) map { if (it.itemId == id) it.copy(quantity = it.quantity.safelyAdd(1)) else it } else this + PlanEntry(id, 1)
private fun List<PlanEntry>.changeQuantity(id: String, delta: Long): List<PlanEntry> = map {
    if (it.itemId != id) it
    else it.copy(quantity = if (delta > 0) it.quantity.safelyAdd(delta) else (it.quantity + delta).coerceAtLeast(1))
}
private fun List<PlanEntry>.setQuantity(id: String, quantity: Long): List<PlanEntry> = map { if (it.itemId == id && quantity > 0) it.copy(quantity = quantity) else it }
private fun Long.safelyAdd(value: Long): Long = if (this == Long.MAX_VALUE) this else this + value

@Composable
private fun Header(query: String, onQueryChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text("CATOS RESOURCE CALC", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(3.dp))
            Text("A compact Valheim crafting planner", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
        }
        OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.width(320.dp), singleLine = true, label = { Text("Search items") }, placeholder = { Text("Try: iron, bread, torch...") }, colors = fieldColors())
    }
}

@Composable
private fun SearchPanel(query: String, results: List<ItemRecord>, selectedIds: Set<String>, onAdd: (ItemRecord) -> Unit, modifier: Modifier) {
    MochaPanel("ITEM SEARCH", "${results.size} matching recipes and items", modifier) {
        Spacer(Modifier.height(12.dp))
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (query.isBlank()) "No items available" else "No items found", style = MaterialTheme.typography.titleMedium)
                    Text(if (query.isBlank()) "Try searching for a Valheim item." else "Try a shorter or different name.", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
                }
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(results, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MochaColors.SurfaceElevated), border = BorderStroke(1.dp, MochaColors.Border), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            val details = listOfNotNull(item.type, item.level?.let { "Lv.$it" }, item.group).joinToString(" · ")
                            if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, color = MochaColors.TextSecondary)
                        }
                        Button(onClick = { onAdd(item) }, enabled = item.id !in selectedIds, colors = ButtonDefaults.buttonColors(containerColor = MochaColors.Accent, contentColor = MochaColors.Background, disabledContainerColor = MochaColors.SurfaceHighest, disabledContentColor = MochaColors.TextSecondary)) { Text(if (item.id in selectedIds) "ADDED" else "ADD") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanPanel(data: DataIndex, plan: List<PlanEntry>, calculation: CalculationResult?, calculationError: Throwable?, copyFeedback: String?, onIncrease: (String) -> Unit, onDecrease: (String) -> Unit, onSetQuantity: (String, Long) -> Unit, onRemove: (String) -> Unit, onClear: () -> Unit, onCopy: () -> Unit, modifier: Modifier) {
    MochaPanel("BUILD PLAN", "${plan.size} selected target${if (plan.size == 1) "" else "s"}", modifier) {
        if (plan.isEmpty()) {
            Spacer(Modifier.height(22.dp)); Text("Your build plan is empty", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp)); Text("Choose an item from search to begin.", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
        } else {
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(plan, key = { it.itemId }) { entry -> PlanRow(data.itemsById.getValue(entry.itemId), entry, onIncrease, onDecrease, onSetQuantity, onRemove) } }
        }
        Spacer(Modifier.height(12.dp)); HorizontalDivider(color = MochaColors.Border); Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("TOTAL MATERIALS", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); if (plan.isNotEmpty()) TextButton(onClick = onClear) { Text("CLEAR", color = MochaColors.TextSecondary) } }
        if (calculationError != null) Text(calculationError.message ?: "Calculation failed", style = MaterialTheme.typography.bodySmall, color = MochaColors.Error)
        else if (calculation == null || calculation.totals.isEmpty()) Text("No base materials required.", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
        else Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) { calculation.totals.forEach { (id, quantity) -> Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(data.materialsById[id]?.name ?: id, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)); Text(quantity.toString(), style = MaterialTheme.typography.titleMedium, color = MochaColors.AccentHover) } } }
        Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(copyFeedback ?: "Totals update as you edit quantities.", style = MaterialTheme.typography.bodySmall, color = if (copyFeedback != null) MochaColors.Success else MochaColors.TextSecondary, modifier = Modifier.weight(1f)); Button(onClick = onCopy, enabled = calculation != null && calculation.totals.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = MochaColors.Accent, contentColor = MochaColors.Background, disabledContainerColor = MochaColors.SurfaceHighest, disabledContentColor = MochaColors.TextSecondary)) { Text("COPY LIST") } }
    }
}

@Composable
private fun PlanRow(item: ItemRecord, entry: PlanEntry, onIncrease: (String) -> Unit, onDecrease: (String) -> Unit, onSetQuantity: (String, Long) -> Unit, onRemove: (String) -> Unit) {
    var draft by remember(entry.itemId) { mutableStateOf(entry.quantity.toString()) }
    var invalid by remember(entry.itemId) { mutableStateOf(false) }
    LaunchedEffect(entry.quantity) { if (!invalid) draft = entry.quantity.toString() }
    Card(colors = CardDefaults.cardColors(containerColor = MochaColors.SurfaceElevated), border = BorderStroke(1.dp, MochaColors.Border), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.name, style = MaterialTheme.typography.titleMedium); Text(item.type ?: "Crafting item", style = MaterialTheme.typography.bodySmall, color = MochaColors.TextSecondary) }; TextButton(onClick = { onRemove(item.id) }) { Text("REMOVE", color = MochaColors.Error) } }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Button(onClick = { onDecrease(item.id) }, contentPadding = ButtonDefaults.ContentPadding) { Text("−") }
                OutlinedTextField(value = draft, onValueChange = { value -> draft = value; val parsed = value.toLongOrNull(); invalid = parsed == null || parsed <= 0; if (!invalid) onSetQuantity(item.id, parsed!!) }, modifier = Modifier.width(90.dp), singleLine = true, isError = invalid, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors())
                Button(onClick = { onIncrease(item.id); draft = entry.quantity.safelyAdd(1).toString() }, contentPadding = ButtonDefaults.ContentPadding) { Text("+") }
                if (invalid) Text("Positive whole number", style = MaterialTheme.typography.bodySmall, color = MochaColors.Error)
            }
        }
    }
}

@Composable
private fun ErrorPanel(message: String) { Surface(Modifier.fillMaxSize(), color = MochaColors.Background) { Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("DATA LOAD ERROR", style = MaterialTheme.typography.headlineSmall, color = MochaColors.Error); Spacer(Modifier.height(12.dp)); Text(message, style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextPrimary) } } }

@Composable
private fun MochaPanel(title: String, subtitle: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) { Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MochaColors.Surface), border = BorderStroke(1.dp, MochaColors.Border)) { Column(Modifier.fillMaxSize().padding(20.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(5.dp)); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary); content() } } }

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedTextColor = MochaColors.TextPrimary, unfocusedTextColor = MochaColors.TextPrimary, errorTextColor = MochaColors.TextPrimary, focusedContainerColor = MochaColors.Surface, unfocusedContainerColor = MochaColors.Surface, focusedPlaceholderColor = MochaColors.TextSecondary, unfocusedPlaceholderColor = MochaColors.TextSecondary, focusedBorderColor = MochaColors.AccentHover, unfocusedBorderColor = MochaColors.Border, focusedLabelColor = MochaColors.AccentHover, unfocusedLabelColor = MochaColors.TextSecondary, cursorColor = MochaColors.AccentHover, errorBorderColor = MochaColors.Error, errorLabelColor = MochaColors.Error)

private fun formatTotals(data: DataIndex, result: CalculationResult): String = buildString { appendLine("CatosResourceCalc materials"); result.totals.forEach { (id, quantity) -> appendLine("$quantity × ${data.materialsById[id]?.name ?: id}") } }
private fun copyToClipboard(text: String) { runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) } }
