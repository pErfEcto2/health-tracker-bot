@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.trackhub.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.trackhub.ui.common.DayPicker
import com.trackhub.ui.common.SectionCard
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(vm: WorkoutViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var addSetFor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Тренировки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            DayPicker(currentDate = state.date, onDateChange = vm::setDate)
        }

        Button(
            onClick = {
                scope.launch {
                    val id = vm.newSession()
                    addSetFor = id
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) { Text("+ Новая тренировка") }

        if (state.sessions.isEmpty()) {
            Text(
                "Тренировок за день нет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            state.sessions.forEach { session ->
                SessionCard(
                    session = session,
                    nameOf = vm::exerciseNameById,
                    onAddSet = { addSetFor = session.id },
                    onFinish = { scope.launch { vm.finishSession(session.id) } },
                    onDelete = { scope.launch { vm.delete(session.id) } },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    val target = addSetFor
    if (target != null) {
        AddSetDialog(
            exercises = state.exercises,
            onDismiss = { addSetFor = null },
            onConfirm = { exerciseId, reps, weight ->
                scope.launch {
                    vm.addSet(target, exerciseId, reps, weight)
                    addSetFor = null
                }
            },
        )
    }
}

@Composable
private fun SessionCard(
    session: WorkoutViewModel.Session,
    nameOf: (String) -> String,
    onAddSet: () -> Unit,
    onFinish: () -> Unit,
    onDelete: () -> Unit,
) {
    val startFmt = runCatching {
        OffsetDateTime.parse(session.payload.startedAt).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("—")
    val finishFmt = session.payload.finishedAt?.let {
        runCatching {
            OffsetDateTime.parse(it).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull()
    }
    val title = if (finishFmt != null) "$startFmt — $finishFmt" else "$startFmt (идёт)"

    SectionCard(
        title = title,
        trailing = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        },
    ) {
        val byEx = session.payload.sets.groupBy { it.exerciseId }
        if (byEx.isEmpty()) {
            Text(
                "Подходов пока нет",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            byEx.forEach { (exId, sets) ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(nameOf(exId), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        sets.forEach { s ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    "${s.reps}×${s.weightKg.toInt()}кг",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onAddSet) { Text("+ Подход") }
            if (session.payload.finishedAt == null) {
                TextButton(onClick = onFinish) { Text("Завершить") }
            }
        }
    }
}
