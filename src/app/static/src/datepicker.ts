/** Reusable day navigator: «prev [date input] next». */

export function dayNavHtml(id: string, date: string): string {
  return `
    <div class="day-nav">
      <button type="button" class="day-nav-btn" data-dir="-1" aria-label="предыдущий день">‹</button>
      <input type="date" id="${id}" value="${date}" class="date-input">
      <button type="button" class="day-nav-btn" data-dir="+1" aria-label="следующий день">›</button>
    </div>
  `;
}

/**
 * Wire prev/next arrows + date input inside the nearest `.day-nav` container
 * holding the given input id.
 */
export function wireDayNav(inputId: string, current: string, onChange: (date: string) => void): void {
  const input = document.getElementById(inputId) as HTMLInputElement | null;
  if (!input) return;
  const container = input.closest(".day-nav");
  if (!container) return;
  input.addEventListener("change", () => onChange(input.value));
  container.querySelectorAll<HTMLButtonElement>(".day-nav-btn").forEach((b) => {
    b.addEventListener("click", () => {
      const delta = Number(b.dataset.dir || "0");
      onChange(shiftDate(current, delta));
    });
  });
}

export function shiftDate(iso: string, deltaDays: number): string {
  const d = new Date(iso + "T00:00:00");
  d.setDate(d.getDate() + deltaDays);
  return d.toISOString().slice(0, 10);
}
