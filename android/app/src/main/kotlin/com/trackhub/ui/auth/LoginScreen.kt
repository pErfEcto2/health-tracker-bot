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

class LoginViewModel : ViewModel(), KoinComponent {
    private val auth: AuthRepository by inject()

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Error(val message: String) : State()
        object Authenticated : State()
        object MustChangePassword : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = State.Error("Введи имя и пароль")
            return
        }
        _state.value = State.Loading
        viewModelScope.launch {
            try {
                when (auth.saltAndLogin(username.trim().lowercase(), password)) {
                    is AuthRepository.LoginResult.Authenticated -> _state.value = State.Authenticated
                    AuthRepository.LoginResult.MustChangePassword -> _state.value = State.MustChangePassword
                }
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Ошибка входа")
            }
        }
    }

    fun consumeError() { if (_state.value is State.Error) _state.value = State.Idle }
}

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onMustChangePassword: () -> Unit,
    onRecover: () -> Unit,
    vm: LoginViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        when (val s = state) {
            is LoginViewModel.State.Authenticated -> onAuthenticated()
            is LoginViewModel.State.MustChangePassword -> onMustChangePassword()
            is LoginViewModel.State.Error -> {
                snack.showSnackbar(s.message)
                vm.consumeError()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("TrackHub", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Вход",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Имя пользователя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        Button(
            onClick = { vm.login(username, password) },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            enabled = state !is LoginViewModel.State.Loading,
        ) {
            Text(if (state is LoginViewModel.State.Loading) "..." else "Войти")
        }
        TextButton(
            onClick = onRecover,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Забыл пароль?") }
        SnackbarHost(snack, modifier = Modifier.padding(top = 16.dp)) { Snackbar { Text(it.visuals.message) } }
    }
}
