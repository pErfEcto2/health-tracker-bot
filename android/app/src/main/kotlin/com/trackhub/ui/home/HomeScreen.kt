package com.trackhub.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trackhub.api.MealType
import com.trackhub.ui.common.BigNumber
import com.trackhub.ui.common.MacroRow
import com.trackhub.ui.common.QuickActionTile
import com.trackhub.ui.common.SectionCard
import com.trackhub.ui.food.openFoodAddDialog
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(vm: HomeViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showWeight by remember { mutableStateOf(false) }
    var addMeal by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
    ) {
        Text(
            "Сегодня",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        SectionCard(title = "Калории") {
            BigNumber(value = state.totals.calories.toInt().toString(), unit = "ккал")
            if (state.tdee != null) {
                val pct = (state.totals.calories / state.tdee!! * 100).toInt().coerceAtMost(999)
                Text(
                    "из ${state.tdee} ккал • $pct%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                Text(
                    "Заполни профиль, чтобы видеть TDEE",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        SectionCard(title = "Макронутриенты") {
            MacroRow(state.totals.proteinG, state.totals.fatG, state.totals.carbsG)
        }

        SectionCard(title = "Быстрое добавление") {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile("Завтрак", "🍳", onClick = { addMeal = MealType.BREAKFAST }, modifier = Modifier.weight(1f))
                    QuickActionTile("Обед", "🥗", onClick = { addMeal = MealType.LUNCH }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile("Ужин", "🍽", onClick = { addMeal = MealType.DINNER }, modifier = Modifier.weight(1f))
                    QuickActionTile("Перекус", "🍎", onClick = { addMeal = MealType.SNACK }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile(
                        "+250 мл", "💧",
                        onClick = { scope.launch { vm.addWater(250) } },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
                        "Вес", "⚖️",
                        onClick = { showWeight = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        SectionCard(title = "Вода") {
            BigNumber(value = state.waterMl.toString(), unit = "мл")
        }

        if (state.weightKg != null) {
            SectionCard(title = "Вес") {
                BigNumber(value = "%.1f".format(state.weightKg), unit = "кг")
            }
        }

        if (state.error != null) {
            Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showWeight) {
        WeightDialog(
            onDismiss = { showWeight = false },
            onSubmit = { kg -> scope.launch { vm.addWeight(kg); showWeight = false } },
        )
    }

    val currentAddMeal = addMeal
    if (currentAddMeal != null) {
        openFoodAddDialog(
            mealType = currentAddMeal,
            date = com.trackhub.util.DateUtils.today(),
            onDismiss = { addMeal = null },
            onAdded = { addMeal = null; vm.refresh() },
        )
    }
}

@Composable
private fun WeightDialog(onDismiss: () -> Unit, onSubmit: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val kg = text.replace(",", ".").toDoubleOrNull()
                if (kg != null && kg in 20.0..300.0) onSubmit(kg)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text("Вес") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("кг") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
    )
}
