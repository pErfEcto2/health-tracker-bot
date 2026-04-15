package com.trackhub.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trackhub.api.FoodSearchItem
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Food-add flow: search + manual entry. Shares one ViewModel, but we use a
 * separate instance for each open of the dialog so state resets cleanly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun openFoodAddDialog(
    mealType: String,
    date: String,
    onDismiss: () -> Unit,
    onAdded: () -> Unit,
    vm: FoodViewModel = koinViewModel(),
) {
    val search by vm.search.collectAsState()
    val scope = rememberCoroutineScope()

    var pickedItem by remember { mutableStateOf<FoodSearchItem?>(null) }
    var grams by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text("Добавить: ${MEAL_LABELS[mealType] ?: mealType}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
            ) {
                if (pickedItem == null) {
                    OutlinedTextField(
                        value = search.query,
                        onValueChange = vm::onSearchQueryChange,
                        label = { Text("Название продукта") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.padding(top = 8.dp))

                    if (search.loading) Text("…", style = MaterialTheme.typography.bodySmall)
                    search.results.forEach { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { pickedItem = item },
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text(item.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "${item.caloriesPer100g.toInt()} ккал • Б${item.proteinPer100g} Ж${item.fatPer100g} У${item.carbsPer100g}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    if (search.error != null) {
                        Text(search.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.padding(top = 12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.padding(top = 12.dp))
                    Text("или вручную:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    ManualFoodForm(
                        onSubmit = { name, g, cal, prot, fat, carb ->
                            scope.launch {
                                vm.addManual(name, g, cal, prot, fat, carb, mealType, date)
                                onAdded()
                            }
                        },
                    )
                } else {
                    val item = pickedItem!!
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "На 100г: ${item.caloriesPer100g.toInt()} ккал • Б ${item.proteinPer100g} / Ж ${item.fatPer100g} / У ${item.carbsPer100g}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = grams, onValueChange = { grams = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text("Граммы") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { pickedItem = null }) { Text("Назад") }
                        Button(
                            onClick = {
                                val g = grams.replace(",", ".").toDoubleOrNull() ?: return@Button
                                scope.launch {
                                    vm.addFromSearch(item, g, mealType, date)
                                    onAdded()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Добавить") }
                    }
                }
            }
        },
    )
}

@Composable
private fun ManualFoodForm(
    onSubmit: (name: String, grams: Double, cal: Double, prot: Double, fat: Double, carb: Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var cal by remember { mutableStateOf("") }
    var prot by remember { mutableStateOf("0") }
    var fat by remember { mutableStateOf("0") }
    var carb by remember { mutableStateOf("0") }

    OutlinedTextField(
        value = name, onValueChange = { name = it },
        label = { Text("Название") }, singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumField("г", grams, { grams = it }, Modifier.weight(1f))
        NumField("ккал/100г", cal, { cal = it }, Modifier.weight(1f))
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumField("Б/100г", prot, { prot = it }, Modifier.weight(1f))
        NumField("Ж/100г", fat, { fat = it }, Modifier.weight(1f))
        NumField("У/100г", carb, { carb = it }, Modifier.weight(1f))
    }
    Button(
        onClick = {
            val g = grams.replace(",", ".").toDoubleOrNull() ?: return@Button
            val c = cal.replace(",", ".").toDoubleOrNull() ?: return@Button
            val p = prot.replace(",", ".").toDoubleOrNull() ?: 0.0
            val f = fat.replace(",", ".").toDoubleOrNull() ?: 0.0
            val cb = carb.replace(",", ".").toDoubleOrNull() ?: 0.0
            if (name.isNotBlank()) onSubmit(name, g, c, p, f, cb)
        },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) { Text("Добавить") }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
