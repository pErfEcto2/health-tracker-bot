// Tiny DOM helpers. No framework.

let _altTarget: HTMLElement | null = null;
let _previewMode = false;

export function setMountTarget(t: HTMLElement | null): void {
  _altTarget = t;
}

export function setPreviewMode(on: boolean): void {
  _previewMode = on;
}

export function isPreviewMode(): boolean {
  return _previewMode;
}

export function $(sel: string, root: ParentNode = document): HTMLElement {
  const el = root.querySelector(sel);
  if (!el) throw new Error(`not found: ${sel}`);
  return el as HTMLElement;
}

export function mount(html: string): HTMLElement {
  const root = _altTarget ?? document.getElementById("app");
  if (!root) throw new Error("mount target missing");
  root.innerHTML = html;

  // Entry animation is skipped when rendering a preview (it's pre-positioned off-screen).
  if (!_previewMode) {
    const child = root.firstElementChild as HTMLElement | null;
    if (child) {
      const dir = document.documentElement.dataset.navDir;
      child.classList.remove("page-enter", "page-enter-left", "page-enter-right");
      void child.offsetWidth;
      if (dir === "left") child.classList.add("page-enter-left");
      else if (dir === "right") child.classList.add("page-enter-right");
      else child.classList.add("page-enter");
    }
    delete document.documentElement.dataset.navDir;
  }
  return root;
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
