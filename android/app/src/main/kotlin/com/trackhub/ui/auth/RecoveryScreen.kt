package com.trackhub.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackhub.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RecoveryViewModel : ViewModel(), KoinComponent {
    private val auth: AuthRepository by inject()

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Error(val message: String) : State()
        object Done : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    fun submit(username: String, recoveryKey: String, newPassword: String, confirm: String) {
        if (newPassword != confirm) { _state.value = State.Error("Пароли не совпадают"); return }
        if (newPassword.length < 12) { _state.value = State.Error("Минимум 12 символов"); return }
        if (username.isBlank() || recoveryKey.isBlank()) { _state.value = State.Error("Заполни все поля"); return }
        _state.value = State.Loading
        viewModelScope.launch {
            try {
                auth.recoverAccount(username.trim().lowercase(), recoveryKey, newPassword)
                _state.value = State.Done
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Ошибка восстановления")
            }
        }
    }

    fun consumeError() { if (_state.value is State.Error) _state.value = State.Idle }
}

@Composable
fun RecoveryScreen(
    onRecovered: () -> Unit,
    onBack: () -> Unit,
    vm: RecoveryViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var pw1 by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        when (val s = state) {
            is RecoveryViewModel.State.Done -> onRecovered()
            is RecoveryViewModel.State.Error -> { snack.showSnackbar(s.message); vm.consumeError() }
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Восстановление", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Введи ключ восстановления (64 hex). Это сбрасывает пароль; данные остаются.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Имя пользователя") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
        )
        OutlinedTextField(
            value = key, onValueChange = { key = it },
            label = { Text("Ключ восстановления") },
            placeholder = { Text("a3f2 9c8d ...") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            minLines = 2,
        )
        OutlinedTextField(
            value = pw1, onValueChange = { pw1 = it },
            label = { Text("Новый пароль") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        OutlinedTextField(
            value = pw2, onValueChange = { pw2 = it },
            label = { Text("Подтверди") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Button(
            onClick = { vm.submit(username, key, pw1, pw2) },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            enabled = state !is RecoveryViewModel.State.Loading,
        ) { Text("Восстановить") }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Назад к входу")
        }
        SnackbarHost(snack, modifier = Modifier.padding(top = 16.dp)) {
            Snackbar { Text(it.visuals.message) }
        }
    }
}
