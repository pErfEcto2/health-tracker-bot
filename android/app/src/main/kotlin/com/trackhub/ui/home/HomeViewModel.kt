package com.trackhub.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackhub.api.FoodEntryPayload
import com.trackhub.api.MeasurementPayload
import com.trackhub.api.ProfilePayload
import com.trackhub.api.RecordType
import com.trackhub.api.WaterEntryPayload
import com.trackhub.data.RecordsRepository
import com.trackhub.util.DateUtils
import com.trackhub.util.MacroTotals
import com.trackhub.util.Stats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeViewModel : ViewModel(), KoinComponent {
    private val records: RecordsRepository by inject()

    data class State(
        val loading: Boolean = true,
        val totals: MacroTotals = MacroTotals(0.0, 0.0, 0.0, 0.0),
        val waterMl: Int = 0,
        val weightKg: Double? = null,
        val tdee: Int? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val today = DateUtils.today()
                val weekAgo = DateUtils.daysAgo(7)
                val food = records.list<FoodEntryPayload>(type = RecordType.FOOD_ENTRY, from = today, to = today)
                val water = records.list<WaterEntryPayload>(type = RecordType.WATER_ENTRY, from = today, to = today)
                val meas = records.list<MeasurementPayload>(type = RecordType.MEASUREMENT, from = weekAgo, to = today)
                val profile = records.list<ProfilePayload>(type = RecordType.PROFILE).firstOrNull()?.payload
                _state.value = State(
                    loading = false,
                    totals = Stats.sumFood(food.map { it.payload }),
                    waterMl = Stats.sumWater(water.map { it.payload }),
                    weightKg = Stats.latestWeight(meas.map { it.payload }),
                    tdee = profile?.let { Stats.calcTdee(it) },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Ошибка")
            }
        }
    }

    suspend fun addWater(ml: Int = 250) {
        records.create<WaterEntryPayload>(
            RecordType.WATER_ENTRY,
            DateUtils.today(),
            WaterEntryPayload(amountMl = ml, loggedAt = java.time.Instant.now().toString()),
        )
        refresh()
    }

    suspend fun addWeight(kg: Double) {
        records.create<MeasurementPayload>(
            RecordType.MEASUREMENT,
            DateUtils.today(),
            MeasurementPayload(weightKg = kg, measuredAt = java.time.Instant.now().toString()),
        )
        refresh()
    }
}
