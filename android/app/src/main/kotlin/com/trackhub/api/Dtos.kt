package com.trackhub.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class SaltRequest(val username: String)
@Serializable data class SaltResponse(
    @SerialName("salt_hex") val saltHex: String,
    @SerialName("must_change_password") val mustChangePassword: Boolean,
)

@Serializable data class LoginRequest(
    val username: String,
    @SerialName("auth_key_hex") val authKeyHex: String,
)
@Serializable data class LoginResponse(
    @SerialName("must_change_password") val mustChangePassword: Boolean,
    @SerialName("wrapped_dek_password_hex") val wrappedDekPasswordHex: String?,
)

@Serializable data class MeResponse(
    val username: String,
    @SerialName("must_change_password") val mustChangePassword: Boolean,
)

@Serializable data class ChangePasswordRequest(
    @SerialName("new_salt_hex") val newSaltHex: String,
    @SerialName("new_auth_key_hex") val newAuthKeyHex: String,
    @SerialName("wrapped_dek_password_hex") val wrappedDekPasswordHex: String,
    @SerialName("wrapped_dek_recovery_hex") val wrappedDekRecoveryHex: String? = null,
    @SerialName("recovery_auth_key_hex") val recoveryAuthKeyHex: String? = null,
)

@Serializable data class RecoverStartRequest(val username: String)
@Serializable data class RecoverStartResponse(
    @SerialName("wrapped_dek_recovery_hex") val wrappedDekRecoveryHex: String,
)
@Serializable data class RecoverCompleteRequest(
    val username: String,
    @SerialName("recovery_auth_key_hex") val recoveryAuthKeyHex: String,
    @SerialName("new_salt_hex") val newSaltHex: String,
    @SerialName("new_auth_key_hex") val newAuthKeyHex: String,
    @SerialName("wrapped_dek_password_hex") val wrappedDekPasswordHex: String,
)

@Serializable data class EncryptedRecord(
    val id: String,
    val type: String,
    @SerialName("record_date") val recordDate: String,
    @SerialName("nonce_hex") val nonceHex: String,
    @SerialName("ciphertext_hex") val ciphertextHex: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable data class RecordCreateRequest(
    val type: String,
    @SerialName("record_date") val recordDate: String,
    @SerialName("nonce_hex") val nonceHex: String,
    @SerialName("ciphertext_hex") val ciphertextHex: String,
)

@Serializable data class RecordUpdateRequest(
    @SerialName("record_date") val recordDate: String? = null,
    @SerialName("nonce_hex") val nonceHex: String,
    @SerialName("ciphertext_hex") val ciphertextHex: String,
)

@Serializable data class Exercise(
    val id: String,
    val name: String,
    @SerialName("muscle_group") val muscleGroup: String,
    val description: String? = null,
)

@Serializable data class FoodSearchItem(
    val name: String,
    @SerialName("calories_per_100g") val caloriesPer100g: Double,
    @SerialName("protein_per_100g") val proteinPer100g: Double,
    @SerialName("fat_per_100g") val fatPer100g: Double,
    @SerialName("carbs_per_100g") val carbsPer100g: Double,
    @SerialName("serving_size") val servingSize: String? = "",
    val source: String,
)

// ---- record payload shapes (JSON inside ciphertext) ----

@Serializable data class FoodEntryPayload(
    val name: String,
    val calories: Double,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("fat_g") val fatG: Double,
    @SerialName("carbs_g") val carbsG: Double,
    val grams: Double,
    @SerialName("meal_type") val mealType: String,
    @SerialName("logged_at") val loggedAt: String,
)

@Serializable data class WaterEntryPayload(
    @SerialName("amount_ml") val amountMl: Int,
    @SerialName("logged_at") val loggedAt: String,
)

@Serializable data class MeasurementPayload(
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("waist_cm") val waistCm: Double? = null,
    @SerialName("bicep_cm") val bicepCm: Double? = null,
    @SerialName("hip_cm") val hipCm: Double? = null,
    @SerialName("chest_cm") val chestCm: Double? = null,
    @SerialName("measured_at") val measuredAt: String,
)

@Serializable data class ProfilePayload(
    val gender: String? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("activity_level") val activityLevel: String? = null,
)

@Serializable data class WorkoutSetPayload(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("set_number") val setNumber: Int,
    val reps: Int,
    @SerialName("weight_kg") val weightKg: Double,
)

@Serializable data class WorkoutSessionPayload(
    @SerialName("started_at") val startedAt: String,
    @SerialName("finished_at") val finishedAt: String? = null,
    val notes: String? = null,
    val sets: List<WorkoutSetPayload> = emptyList(),
)

object RecordType {
    const val PROFILE = "profile"
    const val FOOD_ENTRY = "food_entry"
    const val WATER_ENTRY = "water_entry"
    const val MEASUREMENT = "measurement"
    const val WORKOUT_SESSION = "workout_session"
}

object MealType {
    const val BREAKFAST = "breakfast"
    const val LUNCH = "lunch"
    const val DINNER = "dinner"
    const val SNACK = "snack"
}
