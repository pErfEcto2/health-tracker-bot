// Left/right swipe between the main nav pages.

import { navigate } from "./router";
import type { Route } from "./router";

const NAV_ORDER: Route[] = ["food", "workout", "home", "journal", "profile"];

const H_THRESHOLD = 60;   // px horizontal to count as a swipe
const V_TOLERANCE = 40;   // px vertical — above this, treat as scroll, ignore

let wired = false;

/** Install global touch handlers once. Idempotent. */
export function installSwipeNav(getActive: () => Route): void {
  if (wired) return;
  wired = true;

  let startX = 0;
  let startY = 0;
  let tracking = false;

  const shouldIgnoreTarget = (el: EventTarget | null): boolean => {
    if (!(el instanceof Element)) return false;
    // Don't swipe when user interacts with inputs, modals, or the nav itself.
    return !!el.closest("input, textarea, select, button, .modal-overlay, #bottom-nav, .day-nav");
  };

  document.addEventListener("touchstart", (e) => {
    if (e.touches.length !== 1) { tracking = false; return; }
    if (shouldIgnoreTarget(e.target)) { tracking = false; return; }
    startX = e.touches[0]!.clientX;
    startY = e.touches[0]!.clientY;
    tracking = true;
  }, { passive: true });

  document.addEventListener("touchend", (e) => {
    if (!tracking) return;
    tracking = false;
    const t = e.changedTouches[0];
    if (!t) return;
    const dx = t.clientX - startX;
    const dy = t.clientY - startY;
    if (Math.abs(dy) > V_TOLERANCE) return;
    if (Math.abs(dx) < H_THRESHOLD) return;

    const active = getActive();
    const i = NAV_ORDER.indexOf(active);
    if (i < 0) return;
    const swipingLeft = dx < 0;
    const next = swipingLeft ? i + 1 : i - 1;
    if (next < 0 || next >= NAV_ORDER.length) return;
    // Swiping left pulls the next page in from the right; swiping right pulls prev from the left.
    document.documentElement.dataset.navDir = swipingLeft ? "left" : "right";
    navigate(NAV_ORDER[next]!);
  }, { passive: true });
}
