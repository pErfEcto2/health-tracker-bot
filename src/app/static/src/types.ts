// Plaintext record metadata (from server).
export interface EncryptedRecord {
  id: string;
  type: RecordType;
  record_date: string; // ISO date
  nonce_hex: string;
  ciphertext_hex: string;
  created_at: string;
  updated_at: string;
}

export type RecordType =
  | "profile"
  | "food_entry"
  | "water_entry"
  | "measurement"
  | "workout_session";

export type MealType = "breakfast" | "lunch" | "dinner" | "snack";

export interface FoodEntryPayload {
  name: string;
  calories: number;       // kcal for the portion
  protein_g: number;
  fat_g: number;
  carbs_g: number;
  grams: number;
  meal_type: MealType;
  logged_at: string;      // ISO timestamp
}

export interface WaterEntryPayload {
  amount_ml: number;
  logged_at: string;
}

export interface MeasurementPayload {
  weight_kg?: number;
  waist_cm?: number;
  bicep_cm?: number;
  hip_cm?: number;
  chest_cm?: number;
  measured_at: string;
}

export type ActivityLevel = "sedentary" | "light" | "moderate" | "active" | "very_active";
export type Gender = "male" | "female";

export interface ProfilePayload {
  gender?: Gender;
  weight_kg?: number;
  height_cm?: number;
  birth_date?: string;    // ISO date
  activity_level?: ActivityLevel;
}

export interface WorkoutSetPayload {
  exercise_id: string;
  set_number: number;
  reps: number;
  weight_kg: number;
}

export interface WorkoutSessionPayload {
  started_at: string;
  finished_at?: string;
  notes?: string;
  sets: WorkoutSetPayload[];
}

// Plaintext Exercise catalog entries.
export interface Exercise {
  id: string;
  name: string;
  muscle_group: string;
  description: string | null;
}

// Plaintext food-search results (OpenFoodFacts / local).
export interface FoodSearchItem {
  name: string;
  calories_per_100g: number;
  protein_per_100g: number;
  fat_per_100g: number;
  carbs_per_100g: number;
  serving_size: string;
  source: string;
}
