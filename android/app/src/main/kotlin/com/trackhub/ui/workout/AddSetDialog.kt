package com.trackhub.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trackhub.api.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSetDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onConfirm: (exerciseId: String, reps: Int, weightKg: Double) -> Unit,
) {
    var exerciseId by remember { mutableStateOf<String?>(null) }
    var exerciseLabel by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        confirmButton = {
            Button(onClick = {
                val id = exerciseId ?: return@Button
                val r = reps.toIntOrNull() ?: return@Button
                val w = weight.replace(",", ".").toDoubleOrNull() ?: 0.0
                onConfirm(id, r, w)
            }) { Text("Добавить") }
        },
        title = { Text("Новый подход") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = exerciseLabel,
                        onValueChange = {},
                        label = { Text("Упражнение") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        exercises.forEach { ex ->
                            DropdownMenuItem(
                                text = { Text(ex.name) },
                                onClick = {
                                    exerciseId = ex.id
                                    exerciseLabel = ex.name
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reps, onValueChange = { reps = it.filter { c -> c.isDigit() } },
                        label = { Text("Повторений") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text("Вес, кг") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}
