import { fetchMe } from "./auth";
import { registerRoute, start } from "./router";
import type { Route } from "./router";
import { hasDek } from "./session";

import * as login from "./pages/login";
import * as changePassword from "./pages/change-password";
import * as recovery from "./pages/recovery";
import * as home from "./pages/home";
import * as food from "./pages/food";
import * as workout from "./pages/workout";
import * as profile from "./pages/profile";

registerRoute("login", login.render);
registerRoute("change-password", changePassword.render);
registerRoute("recover", recovery.render);
registerRoute("home", home.render);
registerRoute("food", food.render);
registerRoute("workout", workout.render);
registerRoute("profile", profile.render);

function setHash(route: Route): void {
  const target = `#/${route}`;
  if (window.location.hash !== target) {
    // Replace history entry so back-button doesn't revisit transient routes.
    window.history.replaceState(null, "", target);
  }
}

async function boot(): Promise<void> {
  const me = await fetchMe();
  if (!me) {
    setHash("login");
  } else if (me.must_change_password) {
    setHash("change-password");
  } else if (!hasDek()) {
    // Session valid but DEK gone (fresh tab / reload cleared sessionStorage).
    // Can't decrypt anything — must re-enter password to re-derive DEK.
    setHash("login");
  } else if (!window.location.hash || window.location.hash === "#/" || window.location.hash === "#") {
    setHash("home");
  }
  start();
}

void boot();
