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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

class BiometricUnlockViewModel : ViewModel(), KoinComponent {
    private val dekManager: DekManager by inject()

    sealed class State {
        object Idle : State()
        object Prompting : State()
        data class Error(val message: String) : State()
        object Done : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    fun unlock(activity: FragmentActivity) {
        if (_state.value is State.Prompting) return
        _state.value = State.Prompting
        viewModelScope.launch {
            try {
                dekManager.unlockWithBiometric(activity)
                _state.value = State.Done
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Не удалось разблокировать")
            }
        }
    }

    fun reset() { _state.value = State.Idle }
}

@Composable
fun BiometricUnlockScreen(
    onUnlocked: () -> Unit,
    onFallbackLogin: () -> Unit,
    vm: BiometricUnlockViewModel = koinViewModel(),
) {
    val activity = LocalContext.current as? FragmentActivity
    val state by vm.state.collectAsState()

    LaunchedEffect(activity) {
        if (activity != null && state is BiometricUnlockViewModel.State.Idle) {
            vm.unlock(activity)
        }
    }
    LaunchedEffect(state) {
        if (state is BiometricUnlockViewModel.State.Done) onUnlocked()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("TrackHub", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = when (val s = state) {
                is BiometricUnlockViewModel.State.Error -> s.message
                BiometricUnlockViewModel.State.Prompting -> "Подтверди отпечатком..."
                else -> "Разблокировка"
            },
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state is BiometricUnlockViewModel.State.Error) {
            Button(
                onClick = { activity?.let { vm.unlock(it) } },
                modifier = Modifier.padding(top = 24.dp),
            ) { Text("Повторить") }
            TextButton(
                onClick = onFallbackLogin,
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Войти паролем") }
        }
    }
}
