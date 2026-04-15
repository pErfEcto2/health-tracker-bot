package com.trackhub.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trackhub.api.MealType
import com.trackhub.ui.common.BigNumber
import com.trackhub.ui.common.DayPicker
import com.trackhub.ui.common.MacroRow
import com.trackhub.ui.common.SectionCard
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun FoodScreen(vm: FoodViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var addMeal by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Питание", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            DayPicker(currentDate = state.date, onDateChange = vm::setDate)
        }

        SectionCard(title = "Итого за день") {
            BigNumber(value = state.totals.calories.toInt().toString(), unit = "ккал")
            Spacer(Modifier.height(8.dp))
            MacroRow(state.totals.proteinG, state.totals.fatG, state.totals.carbsG)
        }

        listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK).forEach { meal ->
            MealSection(
                label = MEAL_LABELS[meal] ?: meal,
                entries = vm.entriesOfMeal(meal),
                onAdd = { addMeal = meal },
                onDelete = { id -> scope.launch { vm.delete(id) } },
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    val current = addMeal
    if (current != null) {
        openFoodAddDialog(
            mealType = current,
            date = state.date,
            onDismiss = { addMeal = null },
            onAdded = { addMeal = null; vm.refresh() },
        )
    }
}

@Composable
private fun MealSection(
    label: String,
    entries: List<FoodViewModel.Entry>,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
) {
    SectionCard(
        title = label,
        trailing = { TextButton(onClick = onAdd) { Text("+ Добавить") } },
    ) {
        if (entries.isEmpty()) {
            Text(
                "Ничего не добавлено",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            entries.forEach { e ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(e.payload.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            "${e.payload.grams.toInt()} г • ${e.payload.calories.toInt()} ккал • Б ${"%.1f".format(e.payload.proteinG)} / Ж ${"%.1f".format(e.payload.fatG)} / У ${"%.1f".format(e.payload.carbsG)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    IconButton(onClick = { onDelete(e.id) }) {
                        Icon(Icons.Default.Close, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            val total = e_totals(entries)
            Spacer(Modifier.height(4.dp))
            Text(
                "Итого: ${total.toInt()} ккал",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

private fun e_totals(entries: List<FoodViewModel.Entry>): Double = entries.sumOf { it.payload.calories }
