package com.trackhub.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackhub.api.MeasurementPayload
import com.trackhub.api.RecordType
import com.trackhub.api.WaterEntryPayload
import com.trackhub.data.RecordsRepository
import com.trackhub.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class JournalViewModel : ViewModel(), KoinComponent {
    private val records: RecordsRepository by inject()

    data class MRow(val id: String, val payload: MeasurementPayload)
    data class WRow(val id: String, val payload: WaterEntryPayload)

    data class State(
        val date: String = DateUtils.today(),
        val measurements: List<MRow> = emptyList(),
        val waters: List<WRow> = emptyList(),
        val waterTotal: Int = 0,
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
                val meas = records.list<MeasurementPayload>(
                    type = RecordType.MEASUREMENT,
                    from = _state.value.date, to = _state.value.date,
                )
                val waters = records.list<WaterEntryPayload>(
                    type = RecordType.WATER_ENTRY,
                    from = _state.value.date, to = _state.value.date,
                )
                _state.value = _state.value.copy(
                    measurements = meas.map { MRow(it.id, it.payload) },
                    waters = waters.map { WRow(it.id, it.payload) },
                    waterTotal = waters.sumOf { it.payload.amountMl },
                    loading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Ошибка")
            }
        }
    }

    suspend fun addWater(ml: Int = 250) {
        records.create<WaterEntryPayload>(
            RecordType.WATER_ENTRY, _state.value.date,
            WaterEntryPayload(amountMl = ml, loggedAt = java.time.Instant.now().toString()),
        )
        refresh()
    }

    suspend fun addMeasurement(payload: MeasurementPayload) {
        records.create<MeasurementPayload>(RecordType.MEASUREMENT, _state.value.date, payload)
        refresh()
    }

    suspend fun delete(id: String) {
        records.delete(id)
        refresh()
    }
}
