import { navigate } from "./router";
import type { Route } from "./router";

const MAIN_ROUTES: Route[] = ["food", "workout", "home", "journal", "profile"];

let wired = false;

/** Call once at app boot. Wires the persistent bottom-nav markup in index.html. */
export function wireNavOnce(): void {
  if (wired) return;
  wired = true;
  document.querySelectorAll<HTMLButtonElement>("#bottom-nav .nav-btn").forEach((btn) => {
    btn.addEventListener("click", () => navigate(btn.dataset.route as Route));
  });
}

import { isPreviewMode } from "./ui";

/** Show nav and set active button for the given main route. Hide for auth pages.
 *  No-op when called during a preview render (to keep active state on the real page). */
export function syncNav(active: Route): void {
  if (isPreviewMode()) return;
  const nav = document.getElementById("bottom-nav");
  if (!nav) return;
  const isMain = (MAIN_ROUTES as readonly Route[]).includes(active);
  nav.classList.toggle("hidden", !isMain);
  nav.querySelectorAll<HTMLButtonElement>(".nav-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.route === active);
  });
}
