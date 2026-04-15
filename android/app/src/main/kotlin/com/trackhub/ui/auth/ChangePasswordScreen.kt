package com.trackhub.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackhub.data.AuthRepository
import com.trackhub.session.DekManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ChangePasswordViewModel : ViewModel(), KoinComponent {
    private val auth: AuthRepository by inject()
    private val dekManager: DekManager by inject()

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Error(val message: String) : State()
        data class ShowRecoveryKey(val formatted: String) : State()
        object Done : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    /** firstTime=true triggers full bootstrap (DEK gen + recovery key reveal). */
    fun submit(newPassword: String, confirm: String, firstTime: Boolean) {
        if (newPassword != confirm) { _state.value = State.Error("Пароли не совпадают"); return }
        if (newPassword.length < 12) { _state.value = State.Error("Минимум 12 символов"); return }
        _state.value = State.Loading
        viewModelScope.launch {
            try {
                if (firstTime) {
                    val r = auth.bootstrapAccount(newPassword)
                    _state.value = State.ShowRecoveryKey(r.recoveryFormatted)
                } else {
                    val dek = dekManager.memoryDek() ?: error("DEK не разблокирован")
                    auth.changePassword(dek, newPassword)
                    _state.value = State.Done
                }
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Ошибка")
            }
        }
    }

    fun finishRecoveryReveal() { _state.value = State.Done }
    fun consumeError() { if (_state.value is State.Error) _state.value = State.Idle }
}

@Composable
fun ChangePasswordScreen(
    firstTime: Boolean,
    onDone: () -> Unit,
    vm: ChangePasswordViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsState()
    var pw1 by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        when (val s = state) {
            is ChangePasswordViewModel.State.Done -> onDone()
            is ChangePasswordViewModel.State.Error -> { snack.showSnackbar(s.message); vm.consumeError() }
            else -> {}
        }
    }

    when (val s = state) {
        is ChangePasswordViewModel.State.ShowRecoveryKey ->
            RecoveryKeyDialog(formatted = s.formatted, onContinue = vm::finishRecoveryReveal)
        else ->
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    if (firstTime) "Установи пароль" else "Сменить пароль",
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (firstTime) {
                    Text(
                        "Это также ключ шифрования. Минимум 12 символов.",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedTextField(
                    value = pw1, onValueChange = { pw1 = it },
                    label = { Text("Новый пароль") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                OutlinedTextField(
                    value = pw2, onValueChange = { pw2 = it },
                    label = { Text("Подтверди") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Button(
                    onClick = { vm.submit(pw1, pw2, firstTime) },
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    enabled = state !is ChangePasswordViewModel.State.Loading,
                ) {
                    Text(if (firstTime) "Создать аккаунт" else "Сохранить")
                }
                SnackbarHost(snack, modifier = Modifier.padding(top = 16.dp)) {
                    Snackbar { Text(it.visuals.message) }
                }
            }
    }
}

@Composable
private fun RecoveryKeyDialog(formatted: String, onContinue: () -> Unit) {
    var saved by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Сохрани ключ восстановления", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Это единственный способ доступа к данным, если забудешь пароль. Запиши и спрячь.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = formatted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = {
                val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                cm.setPrimaryClip(android.content.ClipData.newPlainText("recovery", formatted))
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("Скопировать") }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = saved, onCheckedChange = { saved = it })
            Text("Я сохранил ключ", modifier = Modifier.padding(start = 4.dp))
        }
        Button(
            onClick = onContinue,
            enabled = saved,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Продолжить") }
    }
}

@Composable
private fun Row(modifier: Modifier, verticalAlignment: Alignment.Vertical, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(modifier = modifier, verticalAlignment = verticalAlignment, content = content)
}
