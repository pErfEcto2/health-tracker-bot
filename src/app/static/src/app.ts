import { fetchMe } from "./auth";
import { isMainRoute, MAIN_ROUTES, showAuth, showCarousel, slideEl } from "./carousel";
import { wireNavOnce, syncNav } from "./nav";
import { currentRoute, registerRoute, routeHandler } from "./router";
import type { Route } from "./router";
import { loadProfile } from "./records";
import { hasDek } from "./session";
import { isProfileComplete } from "./stats";
import { installSwipeNav } from "./swipe";
import { setMountTarget, setPreviewMode } from "./ui";
import type { ProfilePayload } from "./types";

import * as login from "./pages/login";
import * as changePassword from "./pages/change-password";
import * as recovery from "./pages/recovery";
import * as setup from "./pages/setup";
import * as home from "./pages/home";
import * as food from "./pages/food";
import * as workout from "./pages/workout";
import * as journal from "./pages/journal";
import * as profile from "./pages/profile";

registerRoute("login", login.render);
registerRoute("change-password", changePassword.render);
registerRoute("recover", recovery.render);
registerRoute("setup", setup.render);
registerRoute("home", home.render);
registerRoute("food", food.render);
registerRoute("workout", workout.render);
registerRoute("journal", journal.render);
registerRoute("profile", profile.render);

function setHash(route: Route): void {
  const target = `#/${route}`;
  if (window.location.hash !== target) {
    window.history.replaceState(null, "", target);
  }
}

/** Render a main page into its slide. */
async function renderMain(route: Route): Promise<void> {
  const slide = slideEl(route);
  if (!slide) return;
  const handler = routeHandler(route);
  if (!handler) return;
  setMountTarget(slide);
  try { await handler(); }
  finally { setMountTarget(null); }
}

/** Render all main pages into their slides on first entry to a main route. */
let preRendered = false;
async function ensurePreRendered(): Promise<void> {
  if (preRendered) return;
  preRendered = true;
  // Parallel pre-render — each page's render is independent (own fetches, own DOM slide).
  // The mount target is set via a module-level var, so calls must serialize.
  setPreviewMode(true);  // suppress entry animations during warm-up
  try {
    for (const r of MAIN_ROUTES) {
      await renderMain(r);
    }
  } finally {
    setPreviewMode(false);
  }
}

async function dispatchRoute(route: Route): Promise<void> {
  if (isMainRoute(route)) {
    await ensurePreRendered();
    // Always re-render the target page on explicit nav/direct visit so its data is fresh.
    await renderMain(route);
    showCarousel(route, true);
    syncNav(route);
  } else {
    // Auth pages render into #app as before.
    setMountTarget(null);
    showAuth();
    await routeHandler(route)?.();
  }
}

async function boot(): Promise<void> {
  wireNavOnce();
  installSwipeNav(currentRoute);

  const me = await fetchMe();
  let target: Route;

  if (!me) {
    target = "login";
  } else if (me.must_change_password) {
    target = "change-password";
  } else if (!hasDek()) {
    target = "login";
  } else {
    try {
      const profileRec = await loadProfile<ProfilePayload>({});
      if (!isProfileComplete(profileRec.payload)) {
        target = "setup";
      } else {
        const hash = currentRoute();
        target = isMainRoute(hash) ? hash : "home";
      }
    } catch {
      target = "login";
    }
  }
  setHash(target);
  await dispatchRoute(target);

  window.addEventListener("hashchange", () => {
    void dispatchRoute(currentRoute());
  });
}

void boot();
