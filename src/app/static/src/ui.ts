// Tiny DOM helpers. No framework.

export function $(sel: string, root: ParentNode = document): HTMLElement {
  const el = root.querySelector(sel);
  if (!el) throw new Error(`not found: ${sel}`);
  return el as HTMLElement;
}

export function mount(html: string): void {
  const root = document.getElementById("app");
  if (!root) throw new Error("#app missing");
  root.innerHTML = html;
  // Restart the enter animation each render.
  const child = root.firstElementChild as HTMLElement | null;
  if (child) {
    const dir = document.documentElement.dataset.navDir;
    child.classList.remove("page-enter", "page-enter-left", "page-enter-right");
    // Force reflow so re-adding triggers the animation.
    void child.offsetWidth;
    if (dir === "left") child.classList.add("page-enter-left");
    else if (dir === "right") child.classList.add("page-enter-right");
    else child.classList.add("page-enter");
  }
  delete document.documentElement.dataset.navDir;
}

export function toast(msg: string, ms = 2500): void {
  const el = document.getElementById("toast");
  if (!el) return;
  el.textContent = msg;
  el.classList.add("show");
  setTimeout(() => el.classList.remove("show"), ms);
}

export function escapeHtml(s: string): string {
  return s
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
