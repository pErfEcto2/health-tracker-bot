import { deleteAccount, logout } from "../auth";
import { closeModal, openModal } from "../modal";
import { bottomNavHtml, wireNav } from "../nav";
import { listRecords, loadProfile, updateRecord } from "../records";
import { navigate } from "../router";
import { hasDek } from "../session";
import { activityLabel, calcTDEE, genderLabel, latestWeight, today } from "../stats";
import type { ActivityLevel, FoodEntryPayload, Gender, MeasurementPayload, ProfilePayload, WaterEntryPayload, WorkoutSessionPayload } from "../types";
import { $, mount, toast } from "../ui";

export async function render(): Promise<void> {
  if (!hasDek()) { navigate("login"); return; }

  const [profileRec, measurementsAll] = await Promise.all([
    loadProfile<ProfilePayload>({}),
    listRecords<MeasurementPayload>({ type: "measurement" }),
  ]);

  const profile = profileRec.payload;
  const tdee = calcTDEE(profile);
  const weight = latestWeight(measurementsAll.map((m) => m.payload));

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

  $("#logout-btn").addEventListener("click", async () => {
    await logout();
    navigate("login");
  });

  $("#edit-profile-btn").addEventListener("click", () => openEditModal(profileRec.id, profile));
  $("#export-btn").addEventListener("click", exportAllData);

  $("#delete-account-btn").addEventListener("click", async () => {
    if (!window.confirm("Удалить аккаунт и ВСЕ данные безвозвратно? Восстановление невозможно.")) return;
    const confirm2 = window.prompt('Напиши "УДАЛИТЬ" для подтверждения:');
    if (confirm2 !== "УДАЛИТЬ") { toast("Отменено"); return; }
    try {
      await deleteAccount();
      toast("Аккаунт удалён");
      navigate("login");
    } catch (err) { toast((err as Error).message); }
  });
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
