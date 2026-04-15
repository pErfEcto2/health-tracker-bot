package com.trackhub.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackhub.session.DekManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EnrollBiometricViewModel : ViewModel(), KoinComponent {
    private val dekManager: DekManager by inject()

    sealed class State {
        object Idle : State()
        object Prompting : State()
        data class Error(val message: String) : State()
        object Done : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    fun enroll(activity: FragmentActivity) {
        val dek = dekManager.memoryDek() ?: run {
            _state.value = State.Error("Нет ключа в памяти")
            return
        }
        if (_state.value is State.Prompting) return
        _state.value = State.Prompting
        viewModelScope.launch {
            try {
                dekManager.persistWithBiometric(activity, dek)
                _state.value = State.Done
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Не удалось сохранить")
            }
        }
    }

    fun reset() { _state.value = State.Idle }
}

@Composable
fun EnrollBiometricScreen(
    onDone: () -> Unit,
    onSkip: () -> Unit,
    vm: EnrollBiometricViewModel = koinViewModel(),
) {
    val activity = LocalContext.current as? FragmentActivity
    val state by vm.state.collectAsState()

    LaunchedEffect(state) {
        if (state is EnrollBiometricViewModel.State.Done) onDone()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Биометрия", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Запомним твой ключ шифрования и будем разблокировать отпечатком при следующем входе. Иначе придётся каждый раз вводить пароль.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state is EnrollBiometricViewModel.State.Error) {
            Text(
                (state as EnrollBiometricViewModel.State.Error).message,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = { activity?.let { vm.enroll(it) } },
            modifier = Modifier.padding(top = 24.dp),
            enabled = activity != null && state !is EnrollBiometricViewModel.State.Prompting,
        ) { Text("Включить отпечаток") }
        TextButton(
            onClick = onSkip,
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Пропустить") }
    }
}
