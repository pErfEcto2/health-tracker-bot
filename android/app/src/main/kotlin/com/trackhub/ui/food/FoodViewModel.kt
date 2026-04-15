package com.trackhub.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackhub.api.FoodEntryPayload
import com.trackhub.api.FoodSearchItem
import com.trackhub.api.MealType
import com.trackhub.api.RecordType
import com.trackhub.data.RecordsRepository
import com.trackhub.util.DateUtils
import com.trackhub.util.MacroTotals
import com.trackhub.util.Stats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FoodViewModel : ViewModel(), KoinComponent {
    private val records: RecordsRepository by inject()
    // ApiService injected for /food-search without going through records.
    private val api: com.trackhub.api.ApiService by inject()

    data class Entry(
        val id: String,
        val payload: FoodEntryPayload,
    )

    data class State(
        val date: String = DateUtils.today(),
        val entries: List<Entry> = emptyList(),
        val totals: MacroTotals = MacroTotals(0.0, 0.0, 0.0, 0.0),
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
                val list = records.list<FoodEntryPayload>(
                    type = RecordType.FOOD_ENTRY,
                    from = _state.value.date, to = _state.value.date,
                )
                val entries = list.map { Entry(it.id, it.payload) }
                _state.value = _state.value.copy(
                    entries = entries,
                    totals = Stats.sumFood(entries.map { it.payload }),
                    loading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Ошибка")
            }
        }
    }

    fun entriesOfMeal(meal: String): List<Entry> = _state.value.entries.filter { it.payload.mealType == meal }

    suspend fun delete(id: String) {
        records.delete(id)
        refresh()
    }

    // ---- food-search with debounce ----

    data class SearchState(
        val query: String = "",
        val results: List<FoodSearchItem> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
    )
    private val _search = MutableStateFlow(SearchState())
    val search = _search.asStateFlow()
    private var searchJob: Job? = null

    fun onSearchQueryChange(q: String) {
        _search.value = _search.value.copy(query = q, error = null)
        searchJob?.cancel()
        if (q.trim().length < 2) {
            _search.value = _search.value.copy(results = emptyList(), loading = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _search.value = _search.value.copy(loading = true)
            try {
                val items = api.foodSearch(q.trim())
                _search.value = SearchState(query = q, results = items, loading = false)
            } catch (e: Exception) {
                _search.value = _search.value.copy(loading = false, error = e.message ?: "Ошибка поиска")
            }
        }
    }

    /** Add a food entry. `targetDate` overrides the screen date (for home quick-add). */
    suspend fun addFromSearch(
        item: FoodSearchItem, grams: Double, mealType: String,
        targetDate: String = _state.value.date,
    ) {
        val factor = grams / 100.0
        records.create<FoodEntryPayload>(
            RecordType.FOOD_ENTRY, targetDate,
            FoodEntryPayload(
                name = item.name,
                calories = item.caloriesPer100g * factor,
                proteinG = item.proteinPer100g * factor,
                fatG = item.fatPer100g * factor,
                carbsG = item.carbsPer100g * factor,
                grams = grams,
                mealType = mealType,
                loggedAt = java.time.Instant.now().toString(),
            ),
        )
        if (targetDate == _state.value.date) refresh()
    }

    suspend fun addManual(
        name: String, grams: Double, caloriesPer100: Double,
        proteinPer100: Double, fatPer100: Double, carbsPer100: Double, mealType: String,
        targetDate: String = _state.value.date,
    ) {
        val factor = grams / 100.0
        records.create<FoodEntryPayload>(
            RecordType.FOOD_ENTRY, targetDate,
            FoodEntryPayload(
                name = name, grams = grams,
                calories = caloriesPer100 * factor,
                proteinG = proteinPer100 * factor,
                fatG = fatPer100 * factor,
                carbsG = carbsPer100 * factor,
                mealType = mealType,
                loggedAt = java.time.Instant.now().toString(),
            ),
        )
        if (targetDate == _state.value.date) refresh()
    }
}

val MEAL_LABELS: Map<String, String> = mapOf(
    MealType.BREAKFAST to "Завтрак",
    MealType.LUNCH to "Обед",
    MealType.DINNER to "Ужин",
    MealType.SNACK to "Перекус",
)
