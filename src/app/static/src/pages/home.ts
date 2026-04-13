import { listRecords } from "../records";
import { navigate } from "../router";
import { bottomNavHtml, wireNav } from "../nav";
import { hasDek } from "../session";
import { calcTDEE, isoDaysAgo, latestWeight, sumFood, sumWater, today } from "../stats";
import type { FoodEntryPayload, MeasurementPayload, ProfilePayload, WaterEntryPayload } from "../types";
import { mount } from "../ui";

export async function render(): Promise<void> {
  if (!hasDek()) { navigate("login"); return; }

  const t = today();
  const weekAgo = isoDaysAgo(7);

  const [foods, waters, measurements, profileRows] = await Promise.all([
    listRecords<FoodEntryPayload>({ type: "food_entry", from: t, to: t }),
    listRecords<WaterEntryPayload>({ type: "water_entry", from: t, to: t }),
    listRecords<MeasurementPayload>({ type: "measurement", from: weekAgo, to: t }),
    listRecords<ProfilePayload>({ type: "profile" }),
  ]);

  const foodTotals = sumFood(foods.map((r) => r.payload));
  const waterTotal = sumWater(waters.map((r) => r.payload));
  const weight = latestWeight(measurements.map((r) => r.payload));
  const profile = profileRows[0]?.payload;
  const tdee = profile ? calcTDEE(profile) : null;

  const caloriePct = tdee ? Math.min(100, Math.round((foodTotals.calories / tdee) * 100)) : null;

  mount(`
    <div class="shell">
      <header class="shell-header">
        <h1>Сегодня</h1>
      </header>

      <div class="card">
        <h2>Калории</h2>
        <div class="big-number">${Math.round(foodTotals.calories)}</div>
        ${tdee ? `<p class="hint">из ${tdee} ккал • ${caloriePct}%</p>` : `<p class="hint">установи профиль, чтобы видеть TDEE</p>`}
      </div>

      <div class="card">
        <h2>Макронутриенты</h2>
        <div class="macro-row">
          <div><span class="macro-label">Б</span> ${foodTotals.protein_g.toFixed(1)} г</div>
          <div><span class="macro-label">Ж</span> ${foodTotals.fat_g.toFixed(1)} г</div>
          <div><span class="macro-label">У</span> ${foodTotals.carbs_g.toFixed(1)} г</div>
        </div>
      </div>

      <div class="card">
        <h2>Вода</h2>
        <div class="big-number">${waterTotal} мл</div>
      </div>

      ${weight !== null ? `
        <div class="card">
          <h2>Вес</h2>
          <div class="big-number">${weight.toFixed(1)} кг</div>
        </div>
      ` : ""}
    </div>
    ${bottomNavHtml("home")}
  `);
  wireNav();
}
