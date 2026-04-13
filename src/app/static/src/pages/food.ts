import { api } from "../api";
import { dayNavHtml, wireDayNav } from "../datepicker";
import { closeModal, openModal } from "../modal";
import { bottomNavHtml, wireNav } from "../nav";
import { createRecord, deleteRecord, listRecords } from "../records";
import { navigate } from "../router";
import { hasDek } from "../session";
import { sumFood, today } from "../stats";
import type { DecryptedRecord } from "../records";
import type { FoodEntryPayload, FoodSearchItem, MealType } from "../types";
import { escapeHtml, mount, toast } from "../ui";

const MEAL_LABELS: Record<MealType, string> = {
  breakfast: "Завтрак",
  lunch: "Обед",
  dinner: "Ужин",
  snack: "Перекус",
};

let currentDate = today();

export async function render(): Promise<void> {
  if (!hasDek()) { navigate("login"); return; }

  const entries = await listRecords<FoodEntryPayload>({
    type: "food_entry", from: currentDate, to: currentDate,
  });
  const totals = sumFood(entries.map((r) => r.payload));

  mount(`
    <div class="shell">
      <header class="shell-header">
        <h1>Питание</h1>
        ${dayNavHtml("food-date", currentDate)}
      </header>

      <div class="card">
        <h2>Итого за день</h2>
        <div class="big-number">${Math.round(totals.calories)} ккал</div>
        <div class="macro-row">
          <div><span class="macro-label">Б</span> ${totals.protein_g.toFixed(1)} г</div>
          <div><span class="macro-label">Ж</span> ${totals.fat_g.toFixed(1)} г</div>
          <div><span class="macro-label">У</span> ${totals.carbs_g.toFixed(1)} г</div>
        </div>
      </div>

      ${(["breakfast", "lunch", "dinner", "snack"] as MealType[]).map((mt) => mealSection(mt, entries)).join("")}
    </div>
    ${bottomNavHtml("food")}
  `);

  wireNav();
  wireDayNav("food-date", currentDate, (d) => { currentDate = d; void render(); });

  document.querySelectorAll<HTMLButtonElement>(".add-food-btn").forEach((b) => {
    b.addEventListener("click", () => openAddFoodModal(b.dataset.meal as MealType));
  });

  document.querySelectorAll<HTMLButtonElement>(".del-food-btn").forEach((b) => {
    b.addEventListener("click", async () => {
      if (!confirm("Удалить запись?")) return;
      try {
        await deleteRecord(b.dataset.id!);
        void render();
      } catch (err) { toast((err as Error).message); }
    });
  });
}

function mealSection(mt: MealType, entries: DecryptedRecord<FoodEntryPayload>[]): string {
  const rows = entries.filter((r) => r.payload.meal_type === mt);
  const total = sumFood(rows.map((r) => r.payload));
  return `
    <div class="card">
      <div class="card-header">
        <h3>${MEAL_LABELS[mt]}</h3>
        <button class="text-btn add-food-btn" data-meal="${mt}">+ Добавить</button>
      </div>
      ${rows.length === 0
        ? `<p class="hint">Ничего не добавлено</p>`
        : rows.map(foodRow).join("")}
      ${rows.length > 0 ? `<p class="hint">Итого: ${Math.round(total.calories)} ккал</p>` : ""}
    </div>
  `;
}

function foodRow(r: DecryptedRecord<FoodEntryPayload>): string {
  const p = r.payload;
  return `
    <div class="food-row">
      <div>
        <div class="food-name">${escapeHtml(p.name)}</div>
        <div class="hint">${p.grams} г • ${Math.round(p.calories)} ккал • Б ${p.protein_g.toFixed(1)} / Ж ${p.fat_g.toFixed(1)} / У ${p.carbs_g.toFixed(1)}</div>
      </div>
      <button class="del-food-btn text-btn" data-id="${r.id}" aria-label="delete">✕</button>
    </div>
  `;
}

export function openAddFoodModal(mealType: MealType, date?: string, onAdded?: () => void): void {
  const effectiveDate = date ?? currentDate;
  openAddFoodModalImpl(mealType, effectiveDate, onAdded ?? (() => { void render(); }));
}

function openAddFoodModalImpl(mealType: MealType, effectiveDate: string, onAdded: () => void): void {
  const body = openModal(`Добавить: ${MEAL_LABELS[mealType]}`, `
    <div class="add-food-form">
      <input type="text" id="food-search" placeholder="Название продукта" autocomplete="off">
      <div id="food-search-results" class="food-search-results"></div>
      <hr class="divider">
      <p class="hint">или вручную:</p>
      <form id="manual-food-form" class="auth-form">
        <input type="text" name="name" placeholder="Название" required>
        <div class="row">
          <input type="number" name="grams" placeholder="г" min="1" value="100" required style="flex:1">
          <input type="number" name="calories" placeholder="ккал на 100г" min="0" step="0.1" required style="flex:1">
        </div>
        <div class="row">
          <input type="number" name="protein" placeholder="Б на 100г" min="0" step="0.1" value="0" style="flex:1">
          <input type="number" name="fat" placeholder="Ж на 100г" min="0" step="0.1" value="0" style="flex:1">
          <input type="number" name="carbs" placeholder="У на 100г" min="0" step="0.1" value="0" style="flex:1">
        </div>
        <button type="submit">Добавить</button>
      </form>
    </div>
  `);

  const searchInput = body.querySelector("#food-search") as HTMLInputElement;
  const resultsBox = body.querySelector("#food-search-results") as HTMLElement;

  let searchTimer: number | undefined;
  searchInput.addEventListener("input", () => {
    window.clearTimeout(searchTimer);
    const q = searchInput.value.trim();
    if (q.length < 2) { resultsBox.innerHTML = ""; return; }
    searchTimer = window.setTimeout(async () => {
      try {
        const items = await api.get<FoodSearchItem[]>(`/food-search?q=${encodeURIComponent(q)}`);
        resultsBox.innerHTML = items.map((it, i) => `
          <button type="button" class="search-result" data-idx="${i}">
            <div>${escapeHtml(it.name)}</div>
            <div class="hint">${it.calories_per_100g} ккал • Б${it.protein_per_100g} Ж${it.fat_per_100g} У${it.carbs_per_100g}</div>
          </button>
        `).join("") || `<p class="hint">Ничего не найдено</p>`;
        resultsBox.querySelectorAll<HTMLButtonElement>(".search-result").forEach((btn) => {
          btn.addEventListener("click", () => {
            const idx = Number(btn.dataset.idx);
            openQuantityModal(items[idx]!, mealType, effectiveDate, onAdded);
          });
        });
      } catch (err) { toast((err as Error).message); }
    }, 350);
  });

  const manualForm = body.querySelector("#manual-food-form") as HTMLFormElement;
  manualForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(manualForm);
    const grams = Number(fd.get("grams"));
    const factor = grams / 100;
    const payload: FoodEntryPayload = {
      name: String(fd.get("name")),
      calories: Number(fd.get("calories")) * factor,
      protein_g: Number(fd.get("protein")) * factor,
      fat_g: Number(fd.get("fat")) * factor,
      carbs_g: Number(fd.get("carbs")) * factor,
      grams,
      meal_type: mealType,
      logged_at: new Date().toISOString(),
    };
    try {
      await createRecord("food_entry", effectiveDate, payload);
      closeModal();
      toast("Добавлено");
      onAdded();
    } catch (err) { toast((err as Error).message); }
  });
}

function openQuantityModal(item: FoodSearchItem, mealType: MealType, effectiveDate: string, onAdded: () => void): void {
  const body = openModal(`Сколько: ${item.name}`, `
    <form id="qty-form" class="auth-form">
      <p class="hint">На 100г: ${item.calories_per_100g} ккал • Б ${item.protein_per_100g} / Ж ${item.fat_per_100g} / У ${item.carbs_per_100g}</p>
      <label>Граммы
        <input type="number" name="grams" min="1" value="100" required autofocus>
      </label>
      <button type="submit">Добавить</button>
    </form>
  `);
  const form = body.querySelector("#qty-form") as HTMLFormElement;
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const grams = Number((form.elements.namedItem("grams") as HTMLInputElement).value);
    const factor = grams / 100;
    const payload: FoodEntryPayload = {
      name: item.name,
      calories: item.calories_per_100g * factor,
      protein_g: item.protein_per_100g * factor,
      fat_g: item.fat_per_100g * factor,
      carbs_g: item.carbs_per_100g * factor,
      grams,
      meal_type: mealType,
      logged_at: new Date().toISOString(),
    };
    try {
      await createRecord("food_entry", effectiveDate, payload);
      closeModal();
      toast("Добавлено");
      onAdded();
    } catch (err) { toast((err as Error).message); }
  });
}
