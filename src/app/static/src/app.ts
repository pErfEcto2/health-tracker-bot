import { fetchMe } from "./auth";
import { currentRoute, registerRoute, start } from "./router";
import type { Route } from "./router";
import { loadProfile } from "./records";
import { hasDek } from "./session";
import { isProfileComplete } from "./stats";
import { installSwipeNav } from "./swipe";
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

async function boot(): Promise<void> {
  const me = await fetchMe();
  if (!me) {
    setHash("login");
  } else if (me.must_change_password) {
    setHash("change-password");
  } else if (!hasDek()) {
    setHash("login");
  } else {
    // Check profile completeness — force onboarding if not filled.
    try {
      const profileRec = await loadProfile<ProfilePayload>({});
      if (!isProfileComplete(profileRec.payload)) {
        setHash("setup");
      } else if (!window.location.hash || window.location.hash === "#/" || window.location.hash === "#") {
        setHash("home");
      }
    } catch {
      setHash("login");
    }
  }
  installSwipeNav(currentRoute);
  start();
}

void boot();
