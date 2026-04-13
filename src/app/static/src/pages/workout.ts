import { api } from "../api";
import { closeModal, openModal } from "../modal";
import { bottomNavHtml, wireNav } from "../nav";
import { createRecord, deleteRecord, listRecords, updateRecord } from "../records";
import { navigate } from "../router";
import { hasDek } from "../session";
import { today } from "../stats";
import type { DecryptedRecord } from "../records";
import type { Exercise, WorkoutSessionPayload, WorkoutSetPayload } from "../types";
import { $, escapeHtml, mount, toast } from "../ui";

let currentDate = today();
let exerciseCache: Exercise[] | null = null;

async function getExercises(): Promise<Exercise[]> {
  if (exerciseCache) return exerciseCache;
  exerciseCache = await api.get<Exercise[]>("/exercises");
  return exerciseCache;
}

function exerciseNameById(id: string, exercises: Exercise[]): string {
  return exercises.find((e) => e.id === id)?.name ?? "(удалено)";
}

export async function render(): Promise<void> {
  if (!hasDek()) { navigate("login"); return; }

  const [sessions, exercises] = await Promise.all([
    listRecords<WorkoutSessionPayload>({ type: "workout_session", from: currentDate, to: currentDate }),
    getExercises(),
  ]);

  mount(`
    <div class="shell">
      <header class="shell-header">
        <h1>Тренировки</h1>
        <input type="date" id="wk-date" value="${currentDate}" class="date-input">
      </header>

      <button id="new-workout-btn" class="primary-btn">+ Новая тренировка</button>

      ${sessions.length === 0
        ? `<p class="hint">Тренировок за день нет</p>`
        : sessions.map((s) => sessionCard(s, exercises)).join("")}
    </div>
    ${bottomNavHtml("workout")}
  `);

  wireNav();

  ($("#wk-date") as HTMLInputElement).addEventListener("change", (e) => {
    currentDate = (e.target as HTMLInputElement).value;
    void render();
  });

  $("#new-workout-btn").addEventListener("click", async () => {
    try {
      const now = new Date().toISOString();
      const payload: WorkoutSessionPayload = { started_at: now, sets: [] };
      const created = await createRecord<WorkoutSessionPayload>("workout_session", currentDate, payload);
      toast("Тренировка начата");
      openSetModal(created, exercises);
    } catch (err) { toast((err as Error).message); }
  });

  document.querySelectorAll<HTMLButtonElement>(".add-set-btn").forEach((b) => {
    b.addEventListener("click", () => {
      const s = sessions.find((x) => x.id === b.dataset.id);
      if (s) openSetModal(s, exercises);
    });
  });

  document.querySelectorAll<HTMLButtonElement>(".del-wk-btn").forEach((b) => {
    b.addEventListener("click", async () => {
      if (!confirm("Удалить тренировку?")) return;
      try {
        await deleteRecord(b.dataset.id!);
        void render();
      } catch (err) { toast((err as Error).message); }
    });
  });

  document.querySelectorAll<HTMLButtonElement>(".finish-wk-btn").forEach((b) => {
    b.addEventListener("click", async () => {
      const s = sessions.find((x) => x.id === b.dataset.id);
      if (!s) return;
      try {
        await updateRecord<WorkoutSessionPayload>(s.id, {
          ...s.payload,
          finished_at: new Date().toISOString(),
        });
        toast("Завершено");
        void render();
      } catch (err) { toast((err as Error).message); }
    });
  });
}

function sessionCard(s: DecryptedRecord<WorkoutSessionPayload>, exercises: Exercise[]): string {
  const p = s.payload;
  const startTime = new Date(p.started_at).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
  const finishTime = p.finished_at ? new Date(p.finished_at).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" }) : null;
  const setsBy: Record<string, WorkoutSetPayload[]> = {};
  for (const st of p.sets) {
    (setsBy[st.exercise_id] ||= []).push(st);
  }
  const setsHtml = Object.entries(setsBy).map(([exId, sets]) => `
    <div class="set-group">
      <div class="set-name">${escapeHtml(exerciseNameById(exId, exercises))}</div>
      <div class="set-list">
        ${sets.map((st) => `<span class="set-chip">${st.reps}×${st.weight_kg}кг</span>`).join("")}
      </div>
    </div>
  `).join("");

  return `
    <div class="card">
      <div class="card-header">
        <h3>${startTime}${finishTime ? `—${finishTime}` : " (идёт)"}</h3>
        <div>
          <button class="text-btn del-wk-btn" data-id="${s.id}" aria-label="delete">✕</button>
        </div>
      </div>
      ${setsHtml || `<p class="hint">Подходов пока нет</p>`}
      <div class="row" style="margin-top:12px">
        <button class="text-btn add-set-btn" data-id="${s.id}">+ Подход</button>
        ${!finishTime ? `<button class="text-btn finish-wk-btn" data-id="${s.id}">Завершить</button>` : ""}
      </div>
    </div>
  `;
}

function openSetModal(session: DecryptedRecord<WorkoutSessionPayload>, exercises: Exercise[]): void {
  const body = openModal("Добавить подход", `
    <form id="set-form" class="auth-form">
      <label>Упражнение
        <select name="exercise" required>
          <option value="">—</option>
          ${exercises.map((e) => `<option value="${e.id}">${escapeHtml(e.name)}</option>`).join("")}
        </select>
      </label>
      <div class="row">
        <label style="flex:1">Повторений
          <input type="number" name="reps" min="1" value="10" required>
        </label>
        <label style="flex:1">Вес, кг
          <input type="number" name="weight" min="0" step="0.5" value="0" required>
        </label>
      </div>
      <button type="submit">Добавить</button>
    </form>
  `);

  const form = body.querySelector("#set-form") as HTMLFormElement;
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const exId = String(fd.get("exercise"));
    if (!exId) { toast("Выбери упражнение"); return; }

    const existingSetsForEx = session.payload.sets.filter((s) => s.exercise_id === exId);
    const newSet: WorkoutSetPayload = {
      exercise_id: exId,
      set_number: existingSetsForEx.length + 1,
      reps: Number(fd.get("reps")),
      weight_kg: Number(fd.get("weight")),
    };
    const updated = { ...session.payload, sets: [...session.payload.sets, newSet] };
    try {
      await updateRecord<WorkoutSessionPayload>(session.id, updated);
      closeModal();
      toast("Подход добавлен");
      void render();
    } catch (err) { toast((err as Error).message); }
  });
}
