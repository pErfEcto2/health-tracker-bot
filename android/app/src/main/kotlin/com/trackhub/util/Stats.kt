package com.trackhub.util

import com.trackhub.api.FoodEntryPayload
import com.trackhub.api.MeasurementPayload
import com.trackhub.api.ProfilePayload
import com.trackhub.api.WaterEntryPayload
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

data class MacroTotals(
    val calories: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
)

object Stats {
    fun sumFood(entries: List<FoodEntryPayload>): MacroTotals {
        var c = 0.0; var p = 0.0; var f = 0.0; var cb = 0.0
        for (e in entries) { c += e.calories; p += e.proteinG; f += e.fatG; cb += e.carbsG }
        return MacroTotals(c, p, f, cb)
    }

    fun sumWater(entries: List<WaterEntryPayload>): Int = entries.sumOf { it.amountMl }

    fun latestWeight(measurements: List<MeasurementPayload>): Double? {
        return measurements
            .filter { it.weightKg != null }
            .sortedByDescending { it.measuredAt }
            .firstOrNull()?.weightKg
    }

    /** Mifflin-St Jeor. Returns null when any required field is missing. */
    fun calcTdee(p: ProfilePayload): Int? {
        val g = p.gender ?: return null
        val w = p.weightKg ?: return null
        val h = p.heightCm ?: return null
        val birth = p.birthDate ?: return null
        val activity = p.activityLevel ?: return null
        val age = try {
            val bd = LocalDate.parse(birth, DateTimeFormatter.ISO_LOCAL_DATE)
            Period.between(bd, LocalDate.now()).years
        } catch (_: Exception) { return null }
        val bmr = if (g == "male") 10 * w + 6.25 * h - 5 * age + 5
                  else 10 * w + 6.25 * h - 5 * age - 161
        val mul = when (activity) {
            "sedentary" -> 1.2; "light" -> 1.375; "moderate" -> 1.55
            "active" -> 1.725; "very_active" -> 1.9
            else -> return null
        }
        return (bmr * mul).toInt()
    }

    fun activityLabel(a: String?): String = when (a) {
        "sedentary" -> "Сидячий"
        "light" -> "Лёгкая"
        "moderate" -> "Умеренная"
        "active" -> "Активный"
        "very_active" -> "Очень активный"
        else -> "—"
    }

    fun genderLabel(g: String?): String = when (g) {
        "male" -> "Мужской"; "female" -> "Женский"; else -> "—"
    }
}
