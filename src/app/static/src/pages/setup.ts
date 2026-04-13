import { loadProfile, updateRecord } from "../records";
import { navigate } from "../router";
import { hasDek } from "../session";
import { activityLabel } from "../stats";
import type { ActivityLevel, Gender, ProfilePayload } from "../types";
import { $, mount, toast } from "../ui";

export async function render(): Promise<void> {
  if (!hasDek()) { navigate("login"); return; }

  const rec = await loadProfile<ProfilePayload>({});
  const p = rec.payload;

  mount(`
    <div class="auth-page">
      <h1>Заполни профиль</h1>
      <p class="hint">Нужно для расчёта твоей нормы калорий (TDEE). Все поля — обязательные. Данные хранятся зашифрованно, сервер их не видит.</p>
      <form id="setup-form" class="auth-form">
        <label>Пол
          <select name="gender" required>
            <option value="" ${!p.gender ? "selected" : ""}>—</option>
            <option value="male" ${p.gender === "male" ? "selected" : ""}>Мужской</option>
            <option value="female" ${p.gender === "female" ? "selected" : ""}>Женский</option>
          </select>
        </label>
        <label>Рост, см
          <input type="number" name="height_cm" min="50" max="250" step="0.1" value="${p.height_cm ?? ""}" required>
        </label>
        <label>Вес, кг
          <input type="number" name="weight_kg" min="20" max="300" step="0.1" value="${p.weight_kg ?? ""}" required>
        </label>
        <label>Дата рождения
          <input type="date" name="birth_date" value="${p.birth_date ?? ""}" required>
        </label>
        <label>Уровень активности
          <select name="activity_level" required>
            <option value="" ${!p.activity_level ? "selected" : ""}>—</option>
            ${(["sedentary", "light", "moderate", "active", "very_active"] as ActivityLevel[])
              .map((a) => `<option value="${a}" ${p.activity_level === a ? "selected" : ""}>${activityLabel(a)}</option>`).join("")}
          </select>
        </label>
        <button type="submit">Сохранить и продолжить</button>
      </form>
    </div>
  `);

  const form = $("#setup-form") as HTMLFormElement;
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const payload: ProfilePayload = {
      gender: (fd.get("gender") as Gender) || undefined,
      height_cm: Number(fd.get("height_cm")),
      weight_kg: Number(fd.get("weight_kg")),
      birth_date: (fd.get("birth_date") as string) || undefined,
      activity_level: (fd.get("activity_level") as ActivityLevel) || undefined,
    };
    const btn = form.querySelector("button")!;
    btn.disabled = true;
    try {
      await updateRecord<ProfilePayload>(rec.id, payload);
      navigate("home");
    } catch (err) {
      toast((err as Error).message || "Failed");
      btn.disabled = false;
    }
  });
}
