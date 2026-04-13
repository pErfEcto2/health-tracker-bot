/** Reusable day navigator: «prev [date input] next». */

export function dayNavHtml(id: string, date: string): string {
  return `
    <div class="day-nav">
      <button type="button" class="day-nav-btn" data-dir="-1" aria-label="предыдущий день">‹</button>
      <input type="date" id="${id}" value="${date}" class="date-input">
      <button type="button" class="day-nav-btn" data-dir="1" aria-label="следующий день">›</button>
    </div>
  `;
}

export function wireDayNav(inputId: string, current: string, onChange: (date: string) => void): void {
  const input = document.getElementById(inputId) as HTMLInputElement | null;
  if (!input) return;
  const container = input.closest(".day-nav");
  if (!container) return;
  input.addEventListener("change", () => onChange(input.value));
  container.querySelectorAll<HTMLButtonElement>(".day-nav-btn").forEach((b) => {
    b.addEventListener("click", () => {
      const delta = Number(b.dataset.dir ?? "0");
      onChange(shiftDate(current, delta));
    });
  });
}

/** Shift an ISO date (YYYY-MM-DD) by N days, staying in the local calendar. */
export function shiftDate(iso: string, deltaDays: number): string {
  const [y, m, d] = iso.split("-").map(Number);
  if (!y || !m || !d) return iso;
  // Construct in local time; JS Date normalizes day overflow across months.
  const dt = new Date(y, m - 1, d + deltaDays);
  return formatLocalDate(dt);
}

export function formatLocalDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${dd}`;
}
