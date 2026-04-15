package com.trackhub.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class PlaceholderHomeViewModel : ViewModel(), KoinComponent {
    private val auth: AuthRepository by inject()
    private val _username = MutableStateFlow<String?>(null)
    val username = _username.asStateFlow()

    init {
        viewModelScope.launch {
            _username.value = runCatching { auth.me()?.username }.getOrNull()
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.logout()
            onDone()
        }
    }
}

@Composable
fun PlaceholderHome(onLoggedOut: () -> Unit, vm: PlaceholderHomeViewModel = koinViewModel()) {
    val username by vm.username.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "TrackHub",
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
        )
        Text("Привет, ${username ?: "—"}", modifier = Modifier.padding(top = 8.dp))
        Text(
            "Полный UI приедет в фазе 3.",
            modifier = Modifier.padding(top = 4.dp),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
        Button(onClick = { vm.logout(onLoggedOut) }, modifier = Modifier.padding(top = 24.dp)) {
            Text("Выйти")
        }
    }
}
