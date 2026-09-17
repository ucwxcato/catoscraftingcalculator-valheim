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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cato.resourcecalc.ui.CatosResourceCalcTheme
import com.cato.resourcecalc.ui.MochaColors
import com.cato.resourcecalc.platform.WindowsTitleBar
import java.awt.Color as AwtColor
import java.awt.Dimension

private const val APP_TITLE = "CatosResourceCalc"

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 1_060.dp, height = 700.dp),
        title = APP_TITLE,
    ) {
        window.minimumSize = Dimension(860, 560)
        window.background = AwtColor(0x1E, 0x16, 0x14)

        CatosResourceCalcTheme {
            LaunchedEffect(window) {
                WindowsTitleBar.apply(window)
            }
            ResourceCalcApp()
        }
    }
}

@Composable
private fun ResourceCalcApp() {
    var searchQuery by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MochaColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
        Header(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SearchPanel(
                query = searchQuery,
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight(),
            )
            PlanAndTotalsPanel(
                modifier = Modifier
                    .weight(1.08f)
                    .fillMaxHeight(),
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Created by catosaurluna  ·  Valculator data import pending",
            style = MaterialTheme.typography.bodyMedium,
            color = MochaColors.TextSecondary,
        )
        }
    }
}

@Composable
private fun Header(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CATOS RESOURCE CALC",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "A compact Valheim crafting planner",
                style = MaterialTheme.typography.bodyMedium,
                color = MochaColors.TextSecondary,
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.width(320.dp),
            singleLine = true,
            label = { Text("Search items") },
            placeholder = { Text("Type an item name...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MochaColors.TextPrimary,
                unfocusedTextColor = MochaColors.TextPrimary,
                focusedContainerColor = MochaColors.Surface,
                unfocusedContainerColor = MochaColors.Surface,
                focusedPlaceholderColor = MochaColors.TextSecondary,
                unfocusedPlaceholderColor = MochaColors.TextSecondary,
                focusedBorderColor = MochaColors.AccentHover,
                unfocusedBorderColor = MochaColors.Border,
                focusedLabelColor = MochaColors.AccentHover,
                unfocusedLabelColor = MochaColors.TextSecondary,
                cursorColor = MochaColors.AccentHover,
            ),
        )
    }
}

@Composable
private fun SearchPanel(
    query: String,
    modifier: Modifier = Modifier,
) {
    MochaPanel(
        title = "ITEM SEARCH",
        subtitle = "Valculator data will populate this panel.",
        modifier = modifier,
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (query.isBlank()) "Ready when the data is." else "No imported data to search yet.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "The next phase connects the pinned Valculator dataset.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MochaColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun PlanAndTotalsPanel(modifier: Modifier = Modifier) {
    MochaPanel(
        title = "BUILD PLAN",
        subtitle = "Add targets to calculate your material totals.",
        modifier = modifier,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Your build plan is empty",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Choose an item from search, set its quantity, and add it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MochaColors.TextSecondary,
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = MochaColors.Border)
        Spacer(Modifier.height(18.dp))

        Text(
            text = "TOTAL MATERIALS",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "No materials to show yet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MochaColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(
                containerColor = MochaColors.Accent,
                contentColor = MochaColors.Background,
                disabledContainerColor = MochaColors.SurfaceHighest,
                disabledContentColor = MochaColors.TextSecondary,
            ),
        ) {
            Text("COPY LIST")
        }
    }
}

@Composable
private fun MochaPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MochaColors.Surface),
        border = BorderStroke(1.dp, MochaColors.Border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MochaColors.TextSecondary,
            )
            content()
        }
    }
}
