package com.trackhub.ui.journal

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.trackhub.api.MeasurementPayload
import com.trackhub.ui.common.DayPicker
import com.trackhub.ui.common.SectionCard
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun JournalScreen(vm: JournalViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showMeasurement by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Журнал", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            DayPicker(currentDate = state.date, onDateChange = vm::setDate)
        }

        SectionCard(
            title = "Замеры",
            trailing = { TextButton(onClick = { showMeasurement = true }) { Text("+ Добавить") } },
        ) {
            if (state.measurements.isEmpty()) {
                Text(
                    "Замеров за день нет",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                state.measurements.forEach { m ->
                    MeasurementRow(m, onDelete = { scope.launch { vm.delete(m.id) } })
                }
            }
        }

        SectionCard(
            title = "Вода — ${state.waterTotal} мл",
            trailing = {
                TextButton(onClick = { scope.launch { vm.addWater(250) } }) { Text("+ 250 мл") }
            },
        ) {
            if (state.waters.isEmpty()) {
                Text(
                    "Записей нет",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                state.waters.forEach { w ->
                    WaterRow(w, onDelete = { scope.launch { vm.delete(w.id) } })
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showMeasurement) {
        MeasurementDialog(
            onDismiss = { showMeasurement = false },
            onSubmit = { payload ->
                scope.launch {
                    vm.addMeasurement(payload)
                    showMeasurement = false
                }
            },
        )
    }
}

@Composable
private fun MeasurementRow(r: JournalViewModel.MRow, onDelete: () -> Unit) {
    val m = r.payload
    val parts = buildList {
        m.weightKg?.let { add("${it} кг") }
        m.waistCm?.let { add("талия ${it}") }
        m.bicepCm?.let { add("бицепс ${it}") }
        m.hipCm?.let { add("бёдра ${it}") }
        m.chestCm?.let { add("грудь ${it}") }
    }
    val time = runCatching {
        OffsetDateTime.parse(m.measuredAt).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("—")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(parts.joinToString(" • ").ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun WaterRow(r: JournalViewModel.WRow, onDelete: () -> Unit) {
    val time = runCatching {
        OffsetDateTime.parse(r.payload.loggedAt).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("—")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${r.payload.amountMl} мл", style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MeasurementDialog(onDismiss: () -> Unit, onSubmit: (MeasurementPayload) -> Unit) {
    var weight by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var bicep by remember { mutableStateOf("") }
    var hip by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        confirmButton = {
            Button(onClick = {
                val p = MeasurementPayload(
                    weightKg = weight.replace(",", ".").toDoubleOrNull(),
                    waistCm = waist.replace(",", ".").toDoubleOrNull(),
                    bicepCm = bicep.replace(",", ".").toDoubleOrNull(),
                    hipCm = hip.replace(",", ".").toDoubleOrNull(),
                    chestCm = chest.replace(",", ".").toDoubleOrNull(),
                    measuredAt = java.time.Instant.now().toString(),
                )
                if (listOfNotNull(p.weightKg, p.waistCm, p.bicepCm, p.hipCm, p.chestCm).isNotEmpty()) {
                    onSubmit(p)
                }
            }) { Text("Сохранить") }
        },
        title = { Text("Новый замер") },
        text = {
            Column {
                listOf(
                    Triple("Вес, кг", weight) { v: String -> weight = v },
                    Triple("Талия, см", waist) { v: String -> waist = v },
                    Triple("Бицепс, см", bicep) { v: String -> bicep = v },
                    Triple("Бёдра, см", hip) { v: String -> hip = v },
                    Triple("Грудь, см", chest) { v: String -> chest = v },
                ).forEach { (label, value, setter) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { setter(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
                        label = { Text(label) }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
        },
    )
}
