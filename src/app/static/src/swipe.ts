// Interactive swipe navigation: page follows the finger and the adjacent page
// is live-rendered into #app-preview so the next page is visible during the swipe.

import { navigate, routeHandler } from "./router";
import type { Route } from "./router";
import { setMountTarget, setPreviewMode } from "./ui";

const NAV_ORDER: Route[] = ["food", "workout", "home", "journal", "profile"];

const H_THRESHOLD_FRACTION = 0.25;
const DIR_LOCK = 10;
const EDGE_DAMPING = 3;

let wired = false;

export function installSwipeNav(getActive: () => Route): void {
  if (wired) return;
  wired = true;

  const app = document.getElementById("app");
  const preview = document.getElementById("app-preview");
  if (!app || !preview) return;

  let startX = 0;
  let startY = 0;
  let tracking: "undecided" | "horizontal" | "vertical" | "ignored" = "ignored";
  let lastDx = 0;
  let previewRoute: Route | null = null;

  const resetPreview = (): void => {
    preview.innerHTML = "";
    preview.style.transform = "";
    preview.style.transition = "none";
    previewRoute = null;
  };

  const shouldIgnoreTarget = (el: EventTarget | null): boolean => {
    if (!(el instanceof Element)) return false;
    return !!el.closest("input, textarea, select, button, .modal-overlay, #bottom-nav, .day-nav");
  };

  const positionFor = (target: Route, currentRoute: Route): "left" | "right" => {
    const ci = NAV_ORDER.indexOf(currentRoute);
    const ti = NAV_ORDER.indexOf(target);
    return ti > ci ? "right" : "left";
  };

  const ensurePreviewRendered = async (target: Route, currentRoute: Route): Promise<void> => {
    if (previewRoute === target) return;
    previewRoute = target;
    // Position before render so the new DOM appears off-screen.
    const side = positionFor(target, currentRoute);
    const off = side === "right" ? window.innerWidth : -window.innerWidth;
    preview.style.transition = "none";
    preview.style.transform = `translate3d(${off}px, 0, 0)`;

    const handler = routeHandler(target);
    if (!handler) return;
    setMountTarget(preview);
    setPreviewMode(true);
    try {
      await handler();
    } finally {
      setMountTarget(null);
      setPreviewMode(false);
    }
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

    const active = getActive();
    const i = NAV_ORDER.indexOf(active);
    const swipingLeft = dx < 0;
    const targetIdx = swipingLeft ? i + 1 : i - 1;
    const targetRoute = NAV_ORDER[targetIdx];

    const atLeftEdge = i === 0 && dx > 0;
    const atRightEdge = i === NAV_ORDER.length - 1 && dx < 0;
    const effectiveDx = atLeftEdge || atRightEdge ? dx / EDGE_DAMPING : dx;
    lastDx = effectiveDx;

    app.style.transform = `translate3d(${effectiveDx}px, 0, 0)`;

    if (targetRoute !== undefined && !atLeftEdge && !atRightEdge) {
      void ensurePreviewRendered(targetRoute, active);
      const side = positionFor(targetRoute, active);
      const base = side === "right" ? window.innerWidth : -window.innerWidth;
      preview.style.transition = "none";
      preview.style.transform = `translate3d(${base + effectiveDx}px, 0, 0)`;
    }
  }, { passive: true });

  document.addEventListener("touchend", () => {
    if (tracking !== "horizontal") { tracking = "ignored"; resetPreview(); return; }
    tracking = "ignored";

    const dx = lastDx;
    const threshold = window.innerWidth * H_THRESHOLD_FRACTION;
    const active = getActive();
    const i = NAV_ORDER.indexOf(active);
    const swipingLeft = dx < 0;
    const next = swipingLeft ? i + 1 : i - 1;

    if (Math.abs(dx) >= threshold && next >= 0 && next < NAV_ORDER.length) {
      const side = swipingLeft ? "left" : "right";
      const offApp = side === "left" ? -window.innerWidth : window.innerWidth;
      app.style.transition = "transform 0.2s ease-out";
      app.style.transform = `translate3d(${offApp}px, 0, 0)`;
      preview.style.transition = "transform 0.2s ease-out";
      preview.style.transform = "translate3d(0, 0, 0)";

      const finish = () => {
        app.removeEventListener("transitionend", finish);
        // Reset both containers, then do a real navigate so listeners attach in #app.
        app.style.transition = "none";
        app.style.transform = "";
        resetPreview();
        // Suppress the default enter animation since the swipe already moved it in.
        document.documentElement.dataset.navDir = "";
        navigate(NAV_ORDER[next]!);
      };
      app.addEventListener("transitionend", finish);
    } else {
      app.style.transition = "transform 0.18s ease-out";
      app.style.transform = "translate3d(0, 0, 0)";
      if (previewRoute) {
        const side = positionFor(previewRoute, active);
        const off = side === "right" ? window.innerWidth : -window.innerWidth;
        preview.style.transition = "transform 0.18s ease-out";
        preview.style.transform = `translate3d(${off}px, 0, 0)`;
      }
      const cleanup = () => {
        app.removeEventListener("transitionend", cleanup);
        app.style.transition = "none";
        app.style.transform = "";
        resetPreview();
      };
      app.addEventListener("transitionend", cleanup);
    }
  }, { passive: true });

  document.addEventListener("touchcancel", () => {
    tracking = "ignored";
    app.style.transition = "transform 0.18s ease-out";
    app.style.transform = "translate3d(0, 0, 0)";
    resetPreview();
  }, { passive: true });
}
