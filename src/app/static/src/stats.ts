// Client-side aggregations — all stats happen here since server can't see ciphertext.

import type { ActivityLevel, FoodEntryPayload, Gender, MeasurementPayload, ProfilePayload, WaterEntryPayload } from "./types";

export function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}

export interface MacroTotals {
  calories: number;
  protein_g: number;
  fat_g: number;
  carbs_g: number;
}

export function sumFood(entries: FoodEntryPayload[]): MacroTotals {
  return entries.reduce<MacroTotals>(
    (acc, e) => ({
      calories: acc.calories + (e.calories || 0),
      protein_g: acc.protein_g + (e.protein_g || 0),
      fat_g: acc.fat_g + (e.fat_g || 0),
      carbs_g: acc.carbs_g + (e.carbs_g || 0),
    }),
    { calories: 0, protein_g: 0, fat_g: 0, carbs_g: 0 },
  );
}

export function sumWater(entries: WaterEntryPayload[]): number {
  return entries.reduce((a, e) => a + (e.amount_ml || 0), 0);
}

// TDEE = BMR * activity multiplier (Mifflin-St Jeor).
export function calcTDEE(p: ProfilePayload): number | null {
  if (!p.gender || !p.weight_kg || !p.height_cm || !p.birth_date || !p.activity_level) return null;
  const age = Math.floor((Date.now() - new Date(p.birth_date).getTime()) / (365.25 * 24 * 3600 * 1000));
  const bmr =
    p.gender === "male"
      ? 10 * p.weight_kg + 6.25 * p.height_cm - 5 * age + 5
      : 10 * p.weight_kg + 6.25 * p.height_cm - 5 * age - 161;
  const mult: Record<ActivityLevel, number> = {
    sedentary: 1.2, light: 1.375, moderate: 1.55, active: 1.725, very_active: 1.9,
  };
  return Math.round(bmr * mult[p.activity_level]);
}

export function latestWeight(measurements: MeasurementPayload[]): number | null {
  const withWeight = measurements.filter((m) => m.weight_kg !== undefined && m.weight_kg !== null);
  withWeight.sort((a, b) => new Date(b.measured_at).getTime() - new Date(a.measured_at).getTime());
  return withWeight[0]?.weight_kg ?? null;
}

export function genderLabel(g: Gender | undefined): string {
  if (g === "male") return "Male";
  if (g === "female") return "Female";
  return "—";
}

export function isProfileComplete(p: ProfilePayload | undefined): boolean {
  if (!p) return false;
  return !!(p.gender && p.weight_kg && p.height_cm && p.birth_date && p.activity_level);
}

export function activityLabel(a: ActivityLevel | undefined): string {
  switch (a) {
    case "sedentary": return "Sedentary";
    case "light": return "Light";
    case "moderate": return "Moderate";
    case "active": return "Active";
    case "very_active": return "Very active";
    default: return "—";
  }
}
