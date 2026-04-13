// Swipe carousel: translates #slides as a strip of pre-rendered pages.

import { MAIN_ROUTES, isMainRoute, showCarousel } from "./carousel";
import { syncNav } from "./nav";
import type { Route } from "./router";

const H_THRESHOLD_FRACTION = 0.25;
const DIR_LOCK = 10;
const EDGE_DAMPING = 3;

let wired = false;

export function installSwipeNav(getActive: () => Route): void {
  if (wired) return;
  wired = true;

  const slides = document.getElementById("slides");
  if (!slides) return;

  let startX = 0;
  let startY = 0;
  let tracking: "undecided" | "horizontal" | "vertical" | "ignored" = "ignored";
  let baseIdx = 0;
  let lastDx = 0;
  const viewportWidth = () => window.innerWidth;

  const shouldIgnoreTarget = (el: EventTarget | null): boolean => {
    if (!(el instanceof Element)) return false;
    return !!el.closest("input, textarea, select, button, .modal-overlay, #bottom-nav, .day-nav");
  };

  document.addEventListener("touchstart", (e) => {
    if (!isMainRoute(getActive())) { tracking = "ignored"; return; }
    if (e.touches.length !== 1) { tracking = "ignored"; return; }
    if (shouldIgnoreTarget(e.target)) { tracking = "ignored"; return; }
    startX = e.touches[0]!.clientX;
    startY = e.touches[0]!.clientY;
    lastDx = 0;
    baseIdx = MAIN_ROUTES.indexOf(getActive());
    tracking = "undecided";
    slides.style.transition = "none";
  }, { passive: true });

  document.addEventListener("touchmove", (e) => {
    if (tracking === "ignored") return;
    const t = e.touches[0]!;
    const dx = t.clientX - startX;
    const dy = t.clientY - startY;
    if (tracking === "undecided") {
      if (Math.abs(dx) < DIR_LOCK && Math.abs(dy) < DIR_LOCK) return;
      if (Math.abs(dx) > Math.abs(dy)) tracking = "horizontal";
      else { tracking = "vertical"; return; }
    }
    if (tracking !== "horizontal") return;

    const atLeftEdge = baseIdx === 0 && dx > 0;
    const atRightEdge = baseIdx === MAIN_ROUTES.length - 1 && dx < 0;
    const effectiveDx = atLeftEdge || atRightEdge ? dx / EDGE_DAMPING : dx;
    lastDx = effectiveDx;

    const base = -baseIdx * viewportWidth();
    slides.style.transform = `translate3d(${base + effectiveDx}px, 0, 0)`;
  }, { passive: true });

  document.addEventListener("touchend", () => {
    if (tracking !== "horizontal") { tracking = "ignored"; return; }
    tracking = "ignored";

    const dx = lastDx;
    const threshold = viewportWidth() * H_THRESHOLD_FRACTION;
    const swipingLeft = dx < 0;
    const nextIdx = swipingLeft ? baseIdx + 1 : baseIdx - 1;
    const shouldCommit = Math.abs(dx) >= threshold && nextIdx >= 0 && nextIdx < MAIN_ROUTES.length;

    const targetIdx = shouldCommit ? nextIdx : baseIdx;
    const targetRoute = MAIN_ROUTES[targetIdx]!;

    // Animate slides to the new position. Use showCarousel for consistency.
    showCarousel(targetRoute, true);

    if (shouldCommit) {
      syncNav(targetRoute);
      window.history.replaceState(null, "", `#/${targetRoute}`);
    }
  }, { passive: true });

  document.addEventListener("touchcancel", () => {
    if (tracking !== "horizontal") { tracking = "ignored"; return; }
    tracking = "ignored";
    showCarousel(MAIN_ROUTES[baseIdx]!, true);
  }, { passive: true });
}
