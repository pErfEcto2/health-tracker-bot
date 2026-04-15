package com.trackhub.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackhub.api.ProfilePayload
import com.trackhub.api.RecordType
import com.trackhub.data.RecordsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

class SetupViewModel : ViewModel(), KoinComponent {
    private val records: RecordsRepository by inject()

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Error(val message: String) : State()
        object Done : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    fun save(payload: ProfilePayload) {
        if (payload.gender == null || payload.heightCm == null || payload.weightKg == null
            || payload.birthDate.isNullOrBlank() || payload.activityLevel == null) {
            _state.value = State.Error("Заполни все поля"); return
        }
        _state.value = State.Loading
        viewModelScope.launch {
            try {
                // Look for existing profile record (admin-created accounts have none).
                val existing = records.list<ProfilePayload>(type = RecordType.PROFILE).firstOrNull()
                if (existing == null) {
                    records.create(RecordType.PROFILE, LocalDate.now().toString(), payload)
                } else {
                    records.update(existing.id, payload, existing.recordDate)
                }
                _state.value = State.Done
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Ошибка")
            }
        }
    }

    fun consumeError() { if (_state.value is State.Error) _state.value = State.Idle }
}

private val activityLevels = listOf(
    "sedentary" to "Сидячий — почти без движения",
    "light" to "Лёгкая — 1–3 тренировки в неделю",
    "moderate" to "Умеренная — 3–5 тренировок в неделю",
    "active" to "Активный — 6–7 тренировок в неделю",
    "very_active" to "Очень активный — 2 раза в день / физический труд",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onDone: () -> Unit, vm: SetupViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }

    var gender by remember { mutableStateOf<String?>(null) }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf<String?>(null) }

    var genderExpanded by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when (val s = state) {
            is SetupViewModel.State.Done -> onDone()
            is SetupViewModel.State.Error -> { snack.showSnackbar(s.message); vm.consumeError() }
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Заполни профиль", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Нужно для расчёта TDEE. Хранится зашифрованно — сервер не видит.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )

        ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = it }) {
            OutlinedTextField(
                readOnly = true,
                value = when (gender) { "male" -> "Мужской"; "female" -> "Женский"; else -> "" },
                onValueChange = {},
                label = { Text("Пол") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth().padding(top = 16.dp),
            )
            ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                DropdownMenuItem(text = { Text("Мужской") }, onClick = { gender = "male"; genderExpanded = false })
                DropdownMenuItem(text = { Text("Женский") }, onClick = { gender = "female"; genderExpanded = false })
            }
        }

        OutlinedTextField(
            value = heightCm, onValueChange = { heightCm = it },
            label = { Text("Рост, см") }, singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        OutlinedTextField(
            value = weightKg, onValueChange = { weightKg = it },
            label = { Text("Вес, кг") }, singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        OutlinedTextField(
            value = birthDate, onValueChange = { birthDate = it },
            label = { Text("Дата рождения (YYYY-MM-DD)") }, singleLine = true,
            placeholder = { Text("1990-01-01") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
            OutlinedTextField(
                readOnly = true,
                value = activityLevels.firstOrNull { it.first == activity }?.second ?: "",
                onValueChange = {},
                label = { Text("Уровень активности") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth().padding(top = 12.dp),
            )
            ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                activityLevels.forEach { (id, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { activity = id; activityExpanded = false })
                }
            }
        }

        Button(
            onClick = {
                vm.save(
                    ProfilePayload(
                        gender = gender,
                        heightCm = heightCm.replace(",", ".").toDoubleOrNull(),
                        weightKg = weightKg.replace(",", ".").toDoubleOrNull(),
                        birthDate = birthDate.takeIf { it.isNotBlank() },
                        activityLevel = activity,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            enabled = state !is SetupViewModel.State.Loading,
        ) { Text("Сохранить и продолжить") }
        SnackbarHost(snack, modifier = Modifier.padding(top = 16.dp)) {
            Snackbar { Text(it.visuals.message) }
        }
    }
}
