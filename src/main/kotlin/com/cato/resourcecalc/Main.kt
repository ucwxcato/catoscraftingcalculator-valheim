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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

private const val APP_TITLE = "CatosResourceCalc"

fun main() = application {
    Window(onCloseRequest = ::exitApplication, state = rememberWindowState(width = 1_280.dp, height = 720.dp), title = APP_TITLE) {
        window.minimumSize = Dimension(1_080, 560)
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
    var showAbout by remember { mutableStateOf(false) }
    var copyFeedback by remember { mutableStateOf<String?>(null) }
    val results = remember(query, data) { data.searchDistinct(query, 80) }
    val calculation = remember(plan, calculator) { runCatching { calculator.calculate(plan.map { TargetQuantity(it.itemId, it.quantity) }) } }

    Surface(Modifier.fillMaxSize(), color = MochaColors.Background) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Header(query, onAbout = { showAbout = true }) { query = it }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SearchPanel(query, results, plan.flatMap { data.variantsFor(it.itemId) + data.itemsById.getValue(it.itemId) }.mapTo(mutableSetOf()) { it.id }, { item ->
                    val variants = data.variantsFor(item.id).map { it.id }.toSet() + item.id
                    val selected = plan.firstOrNull { it.itemId in variants }
                    plan = if (selected == null) plan.addOrIncrement(item.id) else plan.changeQuantity(selected.itemId, 1)
                    copyFeedback = null
                }, Modifier.weight(.92f).fillMaxHeight())
                BuildPlanPanel(
                    data = data,
                    plan = plan,
                    onIncrease = { plan = plan.changeQuantity(it, 1) },
                    onDecrease = { plan = plan.changeQuantity(it, -1) },
                    onSetQuantity = { id, quantity -> plan = plan.setQuantity(id, quantity) },
                    onSelectVariant = { oldId, newId -> plan = plan.map { if (it.itemId == oldId) it.copy(itemId = newId) else it } },
                    onClear = { showClear = true },
                    Modifier.weight(.92f).fillMaxHeight(),
                )
                TotalsPanel(
                    data = data,
                    plan = plan,
                    calculation = calculation.getOrNull(),
                    calculationError = calculation.exceptionOrNull(),
                    copyFeedback = copyFeedback,
                    onCopy = { calculation.getOrNull()?.let { copyToClipboard(formatTotals(data, plan, it)); copyFeedback = "Copied totals to clipboard" } },
                    Modifier.weight(1.02f).fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("Created by catosaurluna  ·  Crafting data imported from Valculator  ·  source ${data.snapshot.source.commit.take(8)}", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
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
    if (showAbout) AboutDialog(data) { showAbout = false }
}

private fun List<PlanEntry>.addOrIncrement(id: String): List<PlanEntry> = if (any { it.itemId == id }) map { if (it.itemId == id) it.copy(quantity = it.quantity.safelyAdd(1)) else it } else this + PlanEntry(id, 1)
private fun List<PlanEntry>.changeQuantity(id: String, delta: Long): List<PlanEntry> {
    if (delta < 0 && firstOrNull { it.itemId == id }?.quantity == 1L) return filterNot { it.itemId == id }
    return map {
        if (it.itemId != id) it
        else it.copy(quantity = if (delta > 0) it.quantity.safelyAdd(delta) else (it.quantity + delta).coerceAtLeast(1))
    }
}
private fun List<PlanEntry>.setQuantity(id: String, quantity: Long): List<PlanEntry> = map { if (it.itemId == id && quantity > 0) it.copy(quantity = quantity) else it }
private fun Long.safelyAdd(value: Long): Long = if (this == Long.MAX_VALUE) this else this + value

@Composable
private fun Header(query: String, onAbout: () -> Unit, onQueryChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text("CATOS RESOURCE CALC", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(3.dp))
            Text("A compact Valheim crafting planner", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
        }
        TextButton(onClick = onAbout) { Text("ABOUT", color = MochaColors.AccentHover) }
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
private fun BuildPlanPanel(data: DataIndex, plan: List<PlanEntry>, onIncrease: (String) -> Unit, onDecrease: (String) -> Unit, onSetQuantity: (String, Long) -> Unit, onSelectVariant: (String, String) -> Unit, onClear: () -> Unit, modifier: Modifier) {
    MochaPanel("BUILD PLAN", "${plan.size} selected target${if (plan.size == 1) "" else "s"}", modifier) {
        if (plan.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your build plan is empty", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Choose an item from search to begin.", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
                }
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text("SELECTED TARGETS", style = MaterialTheme.typography.labelMedium, color = MochaColors.TextSecondary)
            Spacer(Modifier.height(5.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(plan, key = { it.itemId }) { entry ->
                    PlanRow(data.itemsById.getValue(entry.itemId), data.variantsFor(entry.itemId), entry, onIncrease, onDecrease, onSetQuantity, onSelectVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) { Text("CLEAR PLAN", color = MochaColors.TextSecondary) }
        }
    }
}

@Composable
private fun TotalsPanel(data: DataIndex, plan: List<PlanEntry>, calculation: CalculationResult?, calculationError: Throwable?, copyFeedback: String?, onCopy: () -> Unit, modifier: Modifier) {
    MochaPanel("TOTAL MATERIALS", "${calculation?.totals?.size ?: 0} base materials", modifier) {
        if (calculationError != null) {
            Text(calculationError.message ?: "Calculation failed", style = MaterialTheme.typography.bodySmall, color = MochaColors.Error)
        } else if (calculation == null || calculation.totals.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(if (plan.isEmpty()) "Add items to see what you need." else "No base materials required.", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary) }
        } else {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                calculation.totals.forEach { (id, quantity) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(data.materialsById[id]?.name ?: id, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(quantity.toString(), style = MaterialTheme.typography.titleMedium, color = MochaColors.AccentHover)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(copyFeedback ?: "Totals update as you edit quantities.", style = MaterialTheme.typography.bodySmall, color = if (copyFeedback != null) MochaColors.Success else MochaColors.TextSecondary, modifier = Modifier.weight(1f))
            Button(onClick = onCopy, enabled = calculation != null && calculation.totals.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = MochaColors.Accent, contentColor = MochaColors.Background, disabledContainerColor = MochaColors.SurfaceHighest, disabledContentColor = MochaColors.TextSecondary)) { Text("COPY LIST") }
        }
    }
}

@Composable
private fun PlanRow(item: ItemRecord, variants: List<ItemRecord>, entry: PlanEntry, onIncrease: (String) -> Unit, onDecrease: (String) -> Unit, onSetQuantity: (String, Long) -> Unit, onSelectVariant: (String, String) -> Unit) {
    var draft by remember(entry.itemId) { mutableStateOf(entry.quantity.toString()) }
    var invalid by remember(entry.itemId) { mutableStateOf(false) }
    var levelMenuExpanded by remember(entry.itemId) { mutableStateOf(false) }
    LaunchedEffect(entry.quantity) { draft = entry.quantity.toString(); invalid = false }
    Card(colors = CardDefaults.cardColors(containerColor = MochaColors.SurfaceElevated), border = BorderStroke(1.dp, MochaColors.Border), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    if (variants.isNotEmpty()) {
                        Box {
                            TextButton(onClick = { levelMenuExpanded = true }, contentPadding = ButtonDefaults.ContentPadding) { Text("Level ${item.level ?: 1} [v]", color = MochaColors.AccentHover) }
                            DropdownMenu(expanded = levelMenuExpanded, onDismissRequest = { levelMenuExpanded = false }) {
                                variants.forEach { variant ->
                                    DropdownMenuItem(
                                        text = { Text("Level ${variant.level}") },
                                        onClick = { onSelectVariant(item.id, variant.id); levelMenuExpanded = false },
                                    )
                                }
                            }
                        }
                    }
                    stationSummary(item)?.let { station ->
                        Text(station, style = MaterialTheme.typography.bodySmall, color = MochaColors.TextSecondary, maxLines = 1)
                    }
                }
                Button(onClick = { onDecrease(item.id) }, contentPadding = ButtonDefaults.ContentPadding, modifier = Modifier.height(38.dp)) { Text("-") }
                OutlinedTextField(value = draft, onValueChange = { value -> draft = value; val parsed = value.toLongOrNull(); invalid = parsed == null || parsed <= 0; if (!invalid) onSetQuantity(item.id, parsed!!) }, modifier = Modifier.width(72.dp), singleLine = true, isError = invalid, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors())
                Button(onClick = { onIncrease(item.id) }, contentPadding = ButtonDefaults.ContentPadding, modifier = Modifier.height(38.dp)) { Text("+") }
            }
            if (invalid) Text("Enter a positive whole number", style = MaterialTheme.typography.bodySmall, color = MochaColors.Error)
        }
    }
}

@Composable
private fun AboutDialog(data: DataIndex, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MochaColors.SurfaceElevated,
        title = { Text("ABOUT CATOS RESOURCE CALC") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A compact, offline Valheim crafting-material calculator.", style = MaterialTheme.typography.bodyMedium)
                Text("Author: catosaurluna", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
                HorizontalDivider(color = MochaColors.Border)
                Text("Material data source", style = MaterialTheme.typography.titleMedium)
                Text("Crafting and recipe data is imported from Valculator. This is a separate application; Valculator supplies data only.", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
                Text("Source commit: ${data.snapshot.source.commit}", style = MaterialTheme.typography.bodySmall, color = MochaColors.TextSecondary)
                TextButton(onClick = { openUrl(data.snapshot.source.repository) }) { Text("OPEN VALCULATOR SOURCE", color = MochaColors.AccentHover) }
                HorizontalDivider(color = MochaColors.Border)
                Text("Font", style = MaterialTheme.typography.titleMedium)
                Text("Minecraft Font by Idrees Hassan, licensed under SIL Open Font License 1.1.", style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary)
                TextButton(onClick = { openUrl("https://github.com/IdreesInc/Minecraft-Font") }) { Text("OPEN FONT SOURCE", color = MochaColors.AccentHover) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } },
    )
}

@Composable
private fun ErrorPanel(message: String) { Surface(Modifier.fillMaxSize(), color = MochaColors.Background) { Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("DATA LOAD ERROR", style = MaterialTheme.typography.headlineSmall, color = MochaColors.Error); Spacer(Modifier.height(12.dp)); Text(message, style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextPrimary) } } }

@Composable
private fun MochaPanel(title: String, subtitle: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) { Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MochaColors.Surface), border = BorderStroke(1.dp, MochaColors.Border)) { Column(Modifier.fillMaxSize().padding(20.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(5.dp)); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MochaColors.TextSecondary); content() } } }

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedTextColor = MochaColors.TextPrimary, unfocusedTextColor = MochaColors.TextPrimary, errorTextColor = MochaColors.TextPrimary, focusedContainerColor = MochaColors.Surface, unfocusedContainerColor = MochaColors.Surface, focusedPlaceholderColor = MochaColors.TextSecondary, unfocusedPlaceholderColor = MochaColors.TextSecondary, focusedBorderColor = MochaColors.AccentHover, unfocusedBorderColor = MochaColors.Border, focusedLabelColor = MochaColors.AccentHover, unfocusedLabelColor = MochaColors.TextSecondary, cursorColor = MochaColors.AccentHover, errorBorderColor = MochaColors.Error, errorLabelColor = MochaColors.Error)

private fun formatTotals(data: DataIndex, plan: List<PlanEntry>, result: CalculationResult): String = buildString {
    appendLine("Materials Needed")
    appendLine()
    appendLine("Craft:")
    plan.forEach { target ->
        appendLine("- ${data.itemsById[target.itemId]?.name ?: target.itemId} x${target.quantity}")
    }
    val stations = plan.mapNotNull { target ->
        data.itemsById[target.itemId]?.let { item -> stationSummary(item)?.let { "${item.name}: $it" } }
    }
    if (stations.isNotEmpty()) {
        appendLine()
        appendLine("Crafting Stations:")
        stations.forEach { appendLine("- $it") }
    }
    appendLine()
    appendLine("Materials:")
    result.totals.forEach { (id, quantity) ->
        appendLine("- $quantity x ${data.materialsById[id]?.name ?: id}")
    }
}

private fun stationSummary(item: ItemRecord): String? = item.station.entries
    .sortedBy { it.key.lowercase() }
    .joinToString(" · ") { (station, level) -> "$station Level $level" }
    .takeIf { it.isNotBlank() }
private fun copyToClipboard(text: String) { runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) } }
private fun openUrl(url: String) { runCatching { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url)) } }
