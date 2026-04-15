package com.trackhub.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackhub.api.ApiService
import com.trackhub.api.Exercise
import com.trackhub.api.RecordType
import com.trackhub.api.WorkoutSessionPayload
import com.trackhub.api.WorkoutSetPayload
import com.trackhub.data.RecordsRepository
import com.trackhub.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WorkoutViewModel : ViewModel(), KoinComponent {
    private val records: RecordsRepository by inject()
    private val api: ApiService by inject()

    data class Session(val id: String, val payload: WorkoutSessionPayload)

    data class State(
        val date: String = DateUtils.today(),
        val sessions: List<Session> = emptyList(),
        val exercises: List<Exercise> = emptyList(),
        val loading: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init { refresh() }

    fun setDate(date: String) {
        _state.value = _state.value.copy(date = date)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val exercises = if (_state.value.exercises.isEmpty()) api.exercises() else _state.value.exercises
                val raw = records.list<WorkoutSessionPayload>(
                    type = RecordType.WORKOUT_SESSION,
                    from = _state.value.date, to = _state.value.date,
                )
                _state.value = _state.value.copy(
                    sessions = raw.map { Session(it.id, it.payload) },
                    exercises = exercises,
                    loading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Ошибка")
            }
        }
    }

    /** Start new session. Returns the created record id. */
    suspend fun newSession(): String {
        val payload = WorkoutSessionPayload(
            startedAt = java.time.Instant.now().toString(),
            sets = emptyList(),
        )
        val created = records.create<WorkoutSessionPayload>(
            RecordType.WORKOUT_SESSION, _state.value.date, payload,
        )
        refresh()
        return created.id
    }

    suspend fun addSet(sessionId: String, exerciseId: String, reps: Int, weightKg: Double) {
        val s = _state.value.sessions.firstOrNull { it.id == sessionId } ?: return
        val existing = s.payload.sets.count { it.exerciseId == exerciseId }
        val updated = s.payload.copy(
            sets = s.payload.sets + WorkoutSetPayload(
                exerciseId = exerciseId,
                setNumber = existing + 1,
                reps = reps,
                weightKg = weightKg,
            ),
        )
        records.update<WorkoutSessionPayload>(sessionId, updated)
        refresh()
    }

    suspend fun finishSession(sessionId: String) {
        val s = _state.value.sessions.firstOrNull { it.id == sessionId } ?: return
        records.update<WorkoutSessionPayload>(
            sessionId,
            s.payload.copy(finishedAt = java.time.Instant.now().toString()),
        )
        refresh()
    }

    suspend fun delete(sessionId: String) {
        records.delete(sessionId)
        refresh()
    }

    fun exerciseNameById(id: String): String =
        _state.value.exercises.firstOrNull { it.id == id }?.name ?: "(удалено)"
}
