import { deleteAccount, logout } from "../auth";
import { dayNavHtml, wireDayNav } from "../datepicker";
import { closeModal, openModal } from "../modal";
import { bottomNavHtml, wireNav } from "../nav";
import { createRecord, deleteRecord, listRecords, loadProfile, updateRecord } from "../records";
import { navigate } from "../router";
import { hasDek } from "../session";
import { activityLabel, calcTDEE, genderLabel, latestWeight, today } from "../stats";
import type { ActivityLevel, FoodEntryPayload, Gender, MeasurementPayload, ProfilePayload, WaterEntryPayload, WorkoutSessionPayload } from "../types";
import { $, mount, toast } from "../ui";

let currentDate = today();

export async function render(): Promise<void> {
  if (!hasDek()) { navigate("login"); return; }

  const [profileRec, measurementsDay, watersDay, measurementsAll] = await Promise.all([
    loadProfile<ProfilePayload>({}),
    listRecords<MeasurementPayload>({ type: "measurement", from: currentDate, to: currentDate }),
    listRecords<WaterEntryPayload>({ type: "water_entry", from: currentDate, to: currentDate }),
    listRecords<MeasurementPayload>({ type: "measurement" }),
  ]);

  const profile = profileRec.payload;
  const tdee = calcTDEE(profile);
  const weight = latestWeight(measurementsAll.map((m) => m.payload));
  const waterTotalMl = watersDay.reduce((a, w) => a + (w.payload.amount_ml || 0), 0);

  mount(`
    <div class="shell">
      <header class="shell-header">
        <h1>Профиль</h1>
        <button id="logout-btn" class="text-btn">Выйти</button>
      </header>

      <div class="card">
        <div class="card-header">
          <h2>Параметры</h2>
          <button id="edit-profile-btn" class="text-btn">Редактировать</button>
        </div>
        <div class="kv"><span>Пол</span><span>${genderLabel(profile.gender)}</span></div>
        <div class="kv"><span>Рост</span><span>${profile.height_cm ? `${profile.height_cm} см` : "—"}</span></div>
        <div class="kv"><span>Вес (профиль)</span><span>${profile.weight_kg ? `${profile.weight_kg} кг` : "—"}</span></div>
        <div class="kv"><span>Последний вес</span><span>${weight !== null ? `${weight.toFixed(1)} кг` : "—"}</span></div>
        <div class="kv"><span>Дата рождения</span><span>${profile.birth_date ?? "—"}</span></div>
        <div class="kv"><span>Активность</span><span>${activityLabel(profile.activity_level)}</span></div>
        ${tdee !== null ? `<div class="kv"><span>TDEE</span><span><strong>${tdee}</strong> ккал/день</span></div>` : ""}
      </div>

      <div class="card">
        <div class="card-header">
          <h2>Журнал</h2>
          ${dayNavHtml("prof-date", currentDate)}
        </div>
        <p class="hint">Замеры и вода за выбранный день</p>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>Замеры</h3>
          <button id="add-measurement-btn" class="text-btn">+ Добавить</button>
        </div>
        ${measurementsDay.length === 0
          ? `<p class="hint">Замеров за этот день нет</p>`
          : measurementsDay.map(measurementRow).join("")}
      </div>

      <div class="card">
        <div class="card-header">
          <h3>Вода — ${waterTotalMl} мл</h3>
          <button id="add-water-btn" class="text-btn">+ 250 мл</button>
        </div>
        ${watersDay.length === 0
          ? `<p class="hint">Записей нет</p>`
          : watersDay.map(waterRow).join("")}
      </div>

      <div class="card">
        <h2>Данные</h2>
        <button id="export-btn" class="text-btn">Скачать все данные (расшифровано)</button>
      </div>

      <div class="card danger-zone">
        <h2>Опасная зона</h2>
        <button id="delete-account-btn" class="danger-btn">Удалить аккаунт и все данные</button>
      </div>
    </div>
    ${bottomNavHtml("profile")}
  `);

  wireNav();
  wireDayNav("prof-date", currentDate, (d) => { currentDate = d; void render(); });

  $("#logout-btn").addEventListener("click", async () => {
    await logout();
    navigate("login");
  });

  $("#edit-profile-btn").addEventListener("click", () => openEditModal(profileRec.id, profile));
  $("#add-measurement-btn").addEventListener("click", openAddMeasurementModal);

  $("#add-water-btn").addEventListener("click", async () => {
    const payload: WaterEntryPayload = { amount_ml: 250, logged_at: new Date().toISOString() };
    try {
      await createRecord("water_entry", currentDate, payload);
      toast("+250 мл");
      void render();
    } catch (err) { toast((err as Error).message); }
  });

  $("#export-btn").addEventListener("click", exportAllData);

  $("#delete-account-btn").addEventListener("click", async () => {
    const confirm1 = window.confirm(
      "Удалить аккаунт и ВСЕ данные безвозвратно? Восстановление невозможно."
    );
    if (!confirm1) return;
    const confirm2 = window.prompt('Напиши "УДАЛИТЬ" для подтверждения:');
    if (confirm2 !== "УДАЛИТЬ") { toast("Отменено"); return; }
    try {
      await deleteAccount();
      toast("Аккаунт удалён");
      navigate("login");
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
  const date = new Date(m.measured_at).toLocaleDateString();
  return `
    <div class="kv">
      <span>${date}</span>
      <span>
        ${parts.join(" • ") || "—"}
        <button class="text-btn del-measurement" data-id="${r.id}" aria-label="удалить" style="margin-left:8px">✕</button>
      </span>
    </div>
  `;
}

function waterRow(r: { id: string; payload: WaterEntryPayload }): string {
  const dt = new Date(r.payload.logged_at);
  const label = `${dt.toLocaleDateString()} ${dt.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" })}`;
  return `
    <div class="kv">
      <span>${label}</span>
      <span>
        ${r.payload.amount_ml} мл
        <button class="text-btn del-water" data-id="${r.id}" aria-label="удалить" style="margin-left:8px">✕</button>
      </span>
    </div>
  `;
}

function openEditModal(id: string, profile: ProfilePayload): void {
  const body = openModal("Редактировать профиль", `
    <form id="profile-form" class="auth-form">
      <label>Пол
        <select name="gender">
          <option value="">—</option>
          <option value="male" ${profile.gender === "male" ? "selected" : ""}>Мужской</option>
          <option value="female" ${profile.gender === "female" ? "selected" : ""}>Женский</option>
        </select>
      </label>
      <label>Рост, см
        <input type="number" name="height_cm" min="50" max="250" step="0.1" value="${profile.height_cm ?? ""}">
      </label>
      <label>Вес, кг
        <input type="number" name="weight_kg" min="20" max="300" step="0.1" value="${profile.weight_kg ?? ""}">
      </label>
      <label>Дата рождения
        <input type="date" name="birth_date" value="${profile.birth_date ?? ""}">
      </label>
      <label>Активность
        <select name="activity_level">
          <option value="">—</option>
          ${(["sedentary","light","moderate","active","very_active"] as ActivityLevel[])
            .map((a) => `<option value="${a}" ${profile.activity_level === a ? "selected" : ""}>${activityLabel(a)}</option>`).join("")}
        </select>
      </label>
      <button type="submit">Сохранить</button>
    </form>
  `);
  const form = body.querySelector("#profile-form") as HTMLFormElement;
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const payload: ProfilePayload = {
      gender: (fd.get("gender") as Gender) || undefined,
      height_cm: fd.get("height_cm") ? Number(fd.get("height_cm")) : undefined,
      weight_kg: fd.get("weight_kg") ? Number(fd.get("weight_kg")) : undefined,
      birth_date: (fd.get("birth_date") as string) || undefined,
      activity_level: (fd.get("activity_level") as ActivityLevel) || undefined,
    };
    try {
      await updateRecord<ProfilePayload>(id, payload);
      closeModal();
      toast("Сохранено");
      void render();
    } catch (err) { toast((err as Error).message); }
  });
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

async function exportAllData(): Promise<void> {
  try {
    const [profile, food, water, measurements, workouts] = await Promise.all([
      listRecords<ProfilePayload>({ type: "profile" }),
      listRecords<FoodEntryPayload>({ type: "food_entry" }),
      listRecords<WaterEntryPayload>({ type: "water_entry" }),
      listRecords<MeasurementPayload>({ type: "measurement" }),
      listRecords<WorkoutSessionPayload>({ type: "workout_session" }),
    ]);
    const data = {
      exported_at: new Date().toISOString(),
      profile: profile.map(decryptedToExport),
      food_entries: food.map(decryptedToExport),
      water_entries: water.map(decryptedToExport),
      measurements: measurements.map(decryptedToExport),
      workout_sessions: workouts.map(decryptedToExport),
    };
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `trackhub-export-${today()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    toast("Экспорт готов");
  } catch (err) { toast((err as Error).message); }
}

function decryptedToExport<T>(r: { id: string; record_date: string; payload: T; created_at: string; updated_at: string }) {
  return { id: r.id, record_date: r.record_date, created_at: r.created_at, updated_at: r.updated_at, ...r.payload };
}
