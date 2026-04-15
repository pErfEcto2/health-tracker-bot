package com.trackhub.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackhub.api.FoodEntryPayload
import com.trackhub.api.MeasurementPayload
import com.trackhub.api.ProfilePayload
import com.trackhub.api.RecordType
import com.trackhub.api.WaterEntryPayload
import com.trackhub.api.WorkoutSessionPayload
import com.trackhub.data.AuthRepository
import com.trackhub.data.RecordsRepository
import com.trackhub.util.Stats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProfileViewModel : ViewModel(), KoinComponent {
    private val records: RecordsRepository by inject()
    private val auth: AuthRepository by inject()

    data class State(
        val profileId: String? = null,
        val profile: ProfilePayload = ProfilePayload(),
        val latestWeight: Double? = null,
        val tdee: Int? = null,
        val username: String? = null,
        val loading: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson = _exportJson.asStateFlow()
    fun consumeExport() { _exportJson.value = null }

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val me = runCatching { auth.me()?.username }.getOrNull()
                val profRec = records.list<ProfilePayload>(type = RecordType.PROFILE).firstOrNull()
                val meas = records.list<MeasurementPayload>(type = RecordType.MEASUREMENT)
                val profile = profRec?.payload ?: ProfilePayload()
                _state.value = State(
                    profileId = profRec?.id,
                    profile = profile,
                    latestWeight = Stats.latestWeight(meas.map { it.payload }),
                    tdee = Stats.calcTdee(profile),
                    username = me,
                    loading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Ошибка")
            }
        }
    }

    suspend fun save(updated: ProfilePayload) {
        val id = _state.value.profileId
        if (id != null) {
            records.update<ProfilePayload>(id, updated)
        } else {
            records.create<ProfilePayload>(RecordType.PROFILE, com.trackhub.util.DateUtils.today(), updated)
        }
        refresh()
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch { auth.logout(); onDone() }
    }

    fun deleteAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                auth.deleteAccount()
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Не удалось удалить")
            }
        }
    }

    fun buildExport() {
        viewModelScope.launch {
            try {
                val prof = records.list<ProfilePayload>(type = RecordType.PROFILE)
                val food = records.list<FoodEntryPayload>(type = RecordType.FOOD_ENTRY)
                val water = records.list<WaterEntryPayload>(type = RecordType.WATER_ENTRY)
                val meas = records.list<MeasurementPayload>(type = RecordType.MEASUREMENT)
                val wk = records.list<WorkoutSessionPayload>(type = RecordType.WORKOUT_SESSION)

                val out = ExportBundle(
                    exportedAt = java.time.Instant.now().toString(),
                    profile = prof.map { ExportRow(it.id, it.recordDate, it.createdAt, it.updatedAt, json(it.payload)) },
                    foodEntries = food.map { ExportRow(it.id, it.recordDate, it.createdAt, it.updatedAt, json(it.payload)) },
                    waterEntries = water.map { ExportRow(it.id, it.recordDate, it.createdAt, it.updatedAt, json(it.payload)) },
                    measurements = meas.map { ExportRow(it.id, it.recordDate, it.createdAt, it.updatedAt, json(it.payload)) },
                    workoutSessions = wk.map { ExportRow(it.id, it.recordDate, it.createdAt, it.updatedAt, json(it.payload)) },
                )
                _exportJson.value = Json { prettyPrint = true }.encodeToString(
                    ExportBundle.serializer(), out,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Экспорт не удался")
            }
        }
    }
}

private val jsonFmt = Json { encodeDefaults = true; explicitNulls = false }
private inline fun <reified T> json(v: T): kotlinx.serialization.json.JsonElement =
    jsonFmt.encodeToJsonElement(kotlinx.serialization.serializer(), v)

@kotlinx.serialization.Serializable
data class ExportRow(
    val id: String,
    val record_date: String,
    val created_at: String,
    val updated_at: String,
    val payload: kotlinx.serialization.json.JsonElement,
)

@kotlinx.serialization.Serializable
data class ExportBundle(
    val exportedAt: String,
    val profile: List<ExportRow>,
    val foodEntries: List<ExportRow>,
    val waterEntries: List<ExportRow>,
    val measurements: List<ExportRow>,
    val workoutSessions: List<ExportRow>,
)
