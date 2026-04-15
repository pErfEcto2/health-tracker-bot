package com.trackhub.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trackhub.api.ProfilePayload
import com.trackhub.ui.common.KeyValueRow
import com.trackhub.ui.common.SectionCard
import com.trackhub.util.Stats
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val activityLevels = listOf(
    "sedentary" to "Сидячий — почти без движения",
    "light" to "Лёгкая — 1–3 тренировки в неделю",
    "moderate" to "Умеренная — 3–5 тренировок в неделю",
    "active" to "Активный — 6–7 тренировок в неделю",
    "very_active" to "Очень активный — 2 раза в день / физический труд",
)

@Composable
fun ProfileScreen(onLoggedOut: () -> Unit, vm: ProfileViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val exportJson by vm.exportJson.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showEdit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(exportJson) {
        val payload = exportJson ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "TrackHub export")
            putExtra(Intent.EXTRA_TEXT, payload)
        }
        context.startActivity(Intent.createChooser(intent, "Экспорт данных"))
        vm.consumeExport()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Профиль", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { vm.logout(onLoggedOut) }) { Text("Выйти") }
        }
        state.username?.let {
            Text(
                "@$it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        SectionCard(
            title = "Параметры",
            trailing = { TextButton(onClick = { showEdit = true }) { Text("Редактировать") } },
        ) {
            KeyValueRow("Пол", Stats.genderLabel(state.profile.gender))
            KeyValueRow("Рост", state.profile.heightCm?.let { "$it см" } ?: "—")
            KeyValueRow("Вес (профиль)", state.profile.weightKg?.let { "$it кг" } ?: "—")
            KeyValueRow("Последний вес", state.latestWeight?.let { "%.1f кг".format(it) } ?: "—")
            KeyValueRow("Дата рождения", state.profile.birthDate ?: "—")
            KeyValueRow("Активность", Stats.activityLabel(state.profile.activityLevel))
            if (state.tdee != null) {
                KeyValueRow("TDEE", "${state.tdee} ккал/день")
            }
        }

        SectionCard(title = "Данные") {
            TextButton(onClick = { vm.buildExport() }) {
                Text("Скачать все данные (расшифровано)")
            }
        }

        SectionCard(title = "Опасная зона") {
            Button(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Удалить аккаунт и все данные") }
        }

        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showEdit) {
        EditProfileDialog(
            initial = state.profile,
            onDismiss = { showEdit = false },
            onSave = { updated ->
                scope.launch {
                    vm.save(updated)
                    showEdit = false
                }
            },
        )
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; vm.deleteAccount(onLoggedOut) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    initial: ProfilePayload,
    onDismiss: () -> Unit,
    onSave: (ProfilePayload) -> Unit,
) {
    var gender by remember { mutableStateOf(initial.gender) }
    var heightCm by remember { mutableStateOf(initial.heightCm?.toString() ?: "") }
    var weightKg by remember { mutableStateOf(initial.weightKg?.toString() ?: "") }
    var birthDate by remember { mutableStateOf(initial.birthDate ?: "") }
    var activity by remember { mutableStateOf(initial.activityLevel) }
    var genderExpanded by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        confirmButton = {
            Button(onClick = {
                onSave(ProfilePayload(
                    gender = gender,
                    heightCm = heightCm.replace(",", ".").toDoubleOrNull(),
                    weightKg = weightKg.replace(",", ".").toDoubleOrNull(),
                    birthDate = birthDate.takeIf { it.isNotBlank() },
                    activityLevel = activity,
                ))
            }) { Text("Сохранить") }
        },
        title = { Text("Редактировать профиль") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = it }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = Stats.genderLabel(gender),
                        onValueChange = {},
                        label = { Text("Пол") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                        DropdownMenuItem(text = { Text("—") }, onClick = { gender = null; genderExpanded = false })
                        DropdownMenuItem(text = { Text("Мужской") }, onClick = { gender = "male"; genderExpanded = false })
                        DropdownMenuItem(text = { Text("Женский") }, onClick = { gender = "female"; genderExpanded = false })
                    }
                }
                OutlinedTextField(
                    value = heightCm, onValueChange = { heightCm = it },
                    label = { Text("Рост, см") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = weightKg, onValueChange = { weightKg = it },
                    label = { Text("Вес, кг") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = birthDate, onValueChange = { birthDate = it },
                    label = { Text("Дата рождения (YYYY-MM-DD)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = activityLevels.firstOrNull { it.first == activity }?.second ?: "",
                        onValueChange = {},
                        label = { Text("Активность") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().padding(top = 8.dp),
                    )
                    ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                        activityLevels.forEach { (id, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { activity = id; activityExpanded = false })
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = typed == "УДАЛИТЬ",
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text("Удалить безвозвратно") }
        },
        title = { Text("Точно удалить?") },
        text = {
            Column {
                Text("Аккаунт и все записи будут уничтожены. Восстановить нельзя.")
                OutlinedTextField(
                    value = typed, onValueChange = { typed = it },
                    label = { Text("Напиши «УДАЛИТЬ»") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
    )
}
