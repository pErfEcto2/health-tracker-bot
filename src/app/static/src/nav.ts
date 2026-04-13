import { navigate } from "./router";
import type { Route } from "./router";

export function bottomNavHtml(active: Route): string {
  return `
    <nav id="bottom-nav">
      ${navBtn("food", active, "Питание", iconFood)}
      ${navBtn("workout", active, "Тренировки", iconWorkout)}
      ${navBtn("home", active, "Главная", iconHome)}
      ${navBtn("journal", active, "Журнал", iconJournal)}
      ${navBtn("profile", active, "Профиль", iconProfile)}
    </nav>
  `;
}

function navBtn(route: Route, active: Route, label: string, icon: string): string {
  const cls = route === active ? "nav-btn active" : "nav-btn";
  return `<button class="${cls}" data-route="${route}">${icon}<span>${label}</span></button>`;
}

export function wireNav(): void {
  document.querySelectorAll<HTMLButtonElement>("#bottom-nav .nav-btn").forEach((btn) => {
    btn.addEventListener("click", () => navigate(btn.dataset.route as Route));
  });
}

const iconHome = `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`;
const iconFood = `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8h1a4 4 0 0 1 0 8h-1"/><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"/><line x1="6" y1="1" x2="6" y2="4"/><line x1="10" y1="1" x2="10" y2="4"/><line x1="14" y1="1" x2="14" y2="4"/></svg>`;
const iconWorkout = `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6.5 6.5h11"/><path d="M6.5 17.5h11"/><path d="M4 10V4h2v16H4v-6"/><path d="M20 10V4h-2v16h2v-6"/><rect x="2" y="9" width="4" height="6" rx="1"/><rect x="18" y="9" width="4" height="6" rx="1"/></svg>`;
const iconJournal = `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16v16H4z"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>`;
const iconProfile = `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`;
