// Interactive swipe navigation — page follows the finger.

import { navigate } from "./router";
import type { Route } from "./router";

const NAV_ORDER: Route[] = ["food", "workout", "home", "journal", "profile"];

const H_THRESHOLD_FRACTION = 0.25; // 25% of viewport width triggers page change
const DIR_LOCK = 10;               // px moved before deciding horizontal vs vertical
const EDGE_DAMPING = 3;            // divisor for over-scroll at nav bounds

let wired = false;

export function installSwipeNav(getActive: () => Route): void {
  if (wired) return;
  wired = true;

  const app = document.getElementById("app");
  if (!app) return;

  let startX = 0;
  let startY = 0;
  let tracking: "undecided" | "horizontal" | "vertical" | "ignored" = "ignored";
  let lastDx = 0;

  const shouldIgnoreTarget = (el: EventTarget | null): boolean => {
    if (!(el instanceof Element)) return false;
    return !!el.closest("input, textarea, select, button, .modal-overlay, #bottom-nav, .day-nav");
  };

  document.addEventListener("touchstart", (e) => {
    if (e.touches.length !== 1) { tracking = "ignored"; return; }
    if (shouldIgnoreTarget(e.target)) { tracking = "ignored"; return; }
    startX = e.touches[0]!.clientX;
    startY = e.touches[0]!.clientY;
    lastDx = 0;
    tracking = "undecided";
    app.style.transition = "none";
  }, { passive: true });

  document.addEventListener("touchmove", (e) => {
    if (tracking === "ignored") return;
    const t = e.touches[0]!;
    const dx = t.clientX - startX;
    const dy = t.clientY - startY;
    if (tracking === "undecided") {
      if (Math.abs(dx) < DIR_LOCK && Math.abs(dy) < DIR_LOCK) return;
      if (Math.abs(dx) > Math.abs(dy)) {
        tracking = "horizontal";
      } else {
        tracking = "vertical";
        return;
      }
    }
    if (tracking !== "horizontal") return;

    // Damp at bounds.
    const i = NAV_ORDER.indexOf(getActive());
    const atLeftEdge = i === 0 && dx > 0;
    const atRightEdge = i === NAV_ORDER.length - 1 && dx < 0;
    const effectiveDx = atLeftEdge || atRightEdge ? dx / EDGE_DAMPING : dx;

    lastDx = effectiveDx;
    app.style.transform = `translate3d(${effectiveDx}px, 0, 0)`;
  }, { passive: true });

  document.addEventListener("touchend", () => {
    if (tracking !== "horizontal") { tracking = "ignored"; return; }
    tracking = "ignored";

    const dx = lastDx;
    const threshold = window.innerWidth * H_THRESHOLD_FRACTION;
    const active = getActive();
    const i = NAV_ORDER.indexOf(active);
    const swipingLeft = dx < 0;
    const next = swipingLeft ? i + 1 : i - 1;

    if (Math.abs(dx) >= threshold && next >= 0 && next < NAV_ORDER.length) {
      // Slide current page the rest of the way off-screen, then navigate.
      const off = swipingLeft ? -window.innerWidth : window.innerWidth;
      app.style.transition = "transform 0.15s ease-out, opacity 0.15s ease-out";
      app.style.transform = `translate3d(${off}px, 0, 0)`;
      app.style.opacity = "0";
      const handler = () => {
        app.removeEventListener("transitionend", handler);
        document.documentElement.dataset.navDir = swipingLeft ? "left" : "right";
        // Reset transform before rendering new page so its entrance animation starts fresh.
        app.style.transition = "none";
        app.style.transform = "";
        app.style.opacity = "";
        navigate(NAV_ORDER[next]!);
      };
      app.addEventListener("transitionend", handler);
    } else {
      // Snap back.
      app.style.transition = "transform 0.18s ease-out";
      app.style.transform = "translate3d(0, 0, 0)";
      const cleanup = () => {
        app.removeEventListener("transitionend", cleanup);
        app.style.transition = "none";
        app.style.transform = "";
      };
      app.addEventListener("transitionend", cleanup);
    }
  }, { passive: true });

  document.addEventListener("touchcancel", () => {
    tracking = "ignored";
    app.style.transition = "transform 0.18s ease-out";
    app.style.transform = "translate3d(0, 0, 0)";
  }, { passive: true });
}
