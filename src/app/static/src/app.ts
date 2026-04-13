import { fetchMe } from "./auth";
import { navigate, registerRoute, start } from "./router";
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

async function boot(): Promise<void> {
  const me = await fetchMe();
  if (!me) { navigate("login"); return; }
  if (me.must_change_password) { navigate("change-password"); return; }
  if (!hasDek()) {
    // Session cookie valid but DEK is gone (new tab / page reload cleared it).
    // User must re-enter password to re-derive DEK.
    navigate("login");
    return;
  }
  if (!window.location.hash) navigate("home");
  start();
}

void boot();
