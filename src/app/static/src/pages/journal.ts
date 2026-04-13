import { dayNavHtml, wireDayNav } from "../datepicker";
import { closeModal, openModal } from "../modal";
import { syncNav } from "../nav";
import { createRecord, deleteRecord, listRecords } from "../records";
import { navigate } from "../router";
import { hasDek } from "../session";
import { today } from "../stats";
import type { MeasurementPayload, WaterEntryPayload } from "../types";
import { $, mount, toast } from "../ui";

let currentDate = today();

export async function render(): Promise<void> {
  if (!hasDek()) { navigate("login"); return; }

  const [measurements, waters] = await Promise.all([
    listRecords<MeasurementPayload>({ type: "measurement", from: currentDate, to: currentDate }),
    listRecords<WaterEntryPayload>({ type: "water_entry", from: currentDate, to: currentDate }),
  ]);

  const waterTotalMl = waters.reduce((a, w) => a + (w.payload.amount_ml || 0), 0);

  mount(`
    <div class="shell">
      <header class="shell-header">
        <h1>Журнал</h1>
        ${dayNavHtml("j-date", currentDate)}
      </header>

      <div class="card">
        <div class="card-header">
          <h3>Замеры</h3>
          <button id="add-measurement-btn" class="text-btn">+ Добавить</button>
        </div>
        ${measurements.length === 0
          ? `<p class="hint">Замеров за этот день нет</p>`
          : measurements.map(measurementRow).join("")}
      </div>

      <div class="card">
        <div class="card-header">
          <h3>Вода — ${waterTotalMl} мл</h3>
          <button id="add-water-btn" class="text-btn">+ 250 мл</button>
        </div>
        ${waters.length === 0
          ? `<p class="hint">Записей нет</p>`
          : waters.map(waterRow).join("")}
      </div>
    </div>
  `);

  syncNav("journal");
  wireDayNav("j-date", currentDate, (d) => { currentDate = d; void render(); });

  $("#add-measurement-btn").addEventListener("click", openAddMeasurementModal);

  $("#add-water-btn").addEventListener("click", async () => {
    const payload: WaterEntryPayload = { amount_ml: 250, logged_at: new Date().toISOString() };
    try {
      await createRecord("water_entry", currentDate, payload);
      toast("+250 мл");
      void render();
    } catch (err) { toast((err as Error).message); }
  });

  document.querySelectorAll<HTMLButtonElement>(".del-measurement").forEach((b) => {
    b.addEventListener("click", () => deleteItem(b.dataset.id!, "замер"));
  });
  document.querySelectorAll<HTMLButtonElement>(".del-water").forEach((b) => {
    b.addEventListener("click", () => deleteItem(b.dataset.id!, "запись"));
  });
}

async function deleteItem(id: string, what: string): Promise<void> {
  if (!confirm(`Удалить ${what}?`)) return;
  try {
    await deleteRecord(id);
    void render();
  } catch (err) { toast((err as Error).message); }
}

function measurementRow(r: { id: string; payload: MeasurementPayload }): string {
  const m = r.payload;
  const parts: string[] = [];
  if (m.weight_kg !== undefined) parts.push(`${m.weight_kg} кг`);
  if (m.waist_cm !== undefined) parts.push(`талия ${m.waist_cm}`);
  if (m.bicep_cm !== undefined) parts.push(`бицепс ${m.bicep_cm}`);
  if (m.hip_cm !== undefined) parts.push(`бёдра ${m.hip_cm}`);
  if (m.chest_cm !== undefined) parts.push(`грудь ${m.chest_cm}`);
  const time = new Date(m.measured_at).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
  return `
    <div class="kv">
      <span>${time}</span>
      <span>
        ${parts.join(" • ") || "—"}
        <button class="text-btn del-measurement" data-id="${r.id}" aria-label="удалить" style="margin-left:8px">✕</button>
      </span>
    </div>
  `;
}

function waterRow(r: { id: string; payload: WaterEntryPayload }): string {
  const time = new Date(r.payload.logged_at).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
  return `
    <div class="kv">
      <span>${time}</span>
      <span>
        ${r.payload.amount_ml} мл
        <button class="text-btn del-water" data-id="${r.id}" aria-label="удалить" style="margin-left:8px">✕</button>
      </span>
    </div>
  `;
}

function openAddMeasurementModal(): void {
  const body = openModal("Новый замер", `
    <form id="m-form" class="auth-form">
      <label>Вес, кг <input type="number" name="weight_kg" step="0.1" min="20" max="300"></label>
      <label>Талия, см <input type="number" name="waist_cm" step="0.1" min="30" max="200"></label>
      <label>Бицепс, см <input type="number" name="bicep_cm" step="0.1" min="10" max="80"></label>
      <label>Бёдра, см <input type="number" name="hip_cm" step="0.1" min="30" max="200"></label>
      <label>Грудь, см <input type="number" name="chest_cm" step="0.1" min="30" max="200"></label>
      <button type="submit">Сохранить</button>
    </form>
  `);
  const form = body.querySelector("#m-form") as HTMLFormElement;
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const num = (k: string): number | undefined => fd.get(k) ? Number(fd.get(k)) : undefined;
    const payload: MeasurementPayload = {
      weight_kg: num("weight_kg"),
      waist_cm: num("waist_cm"),
      bicep_cm: num("bicep_cm"),
      hip_cm: num("hip_cm"),
      chest_cm: num("chest_cm"),
      measured_at: new Date().toISOString(),
    };
    if (Object.values(payload).filter((v) => typeof v === "number").length === 0) {
      toast("Введи хотя бы одно значение"); return;
    }
    try {
      await createRecord("measurement", currentDate, payload);
      closeModal();
      toast("Сохранено");
      void render();
    } catch (err) { toast((err as Error).message); }
  });
}
