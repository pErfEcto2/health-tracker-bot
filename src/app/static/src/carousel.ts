// Carousel of pre-rendered main pages. Auth pages render into #app; main pages
// live in their own slide under #slides and swipe/nav just translates the strip.

import type { Route } from "./router";

export const MAIN_ROUTES: Route[] = ["food", "workout", "home", "journal", "profile"];

export function isMainRoute(r: Route): boolean {
  return (MAIN_ROUTES as readonly Route[]).includes(r);
}

export function slideEl(route: Route): HTMLElement | null {
  return document.getElementById(`slide-${route}`);
}

export function showAuth(): void {
  document.getElementById("carousel")?.classList.add("hidden");
  document.getElementById("app")?.classList.remove("hidden");
  document.getElementById("bottom-nav")?.classList.add("hidden");
}

export function showCarousel(active: Route, animate: boolean = true): void {
  const idx = MAIN_ROUTES.indexOf(active);
  if (idx < 0) return;
  document.getElementById("app")?.classList.add("hidden");
  document.getElementById("carousel")?.classList.remove("hidden");
  const slides = document.getElementById("slides");
  if (slides) {
    slides.style.transition = animate ? "transform 0.2s ease-out" : "none";
    slides.style.transform = `translate3d(${-idx * 100}vw, 0, 0)`;
  }
}

export function currentSlideTransform(): { idx: number } {
  const slides = document.getElementById("slides");
  if (!slides) return { idx: 0 };
  const m = slides.style.transform.match(/translate3d\((-?\d+(?:\.\d+)?)vw/);
  if (!m) return { idx: 0 };
  return { idx: Math.round(-Number(m[1]) / 100) };
}
