export function openModal(title: string, bodyHtml: string): HTMLElement {
  const overlay = document.getElementById("modal-overlay")!;
  const titleEl = document.getElementById("modal-title")!;
  const body = document.getElementById("modal-body")!;
  titleEl.textContent = title;
  body.innerHTML = bodyHtml;
  overlay.classList.remove("hidden");
  const close = document.getElementById("modal-close")!;
  const onOverlayClick = (e: MouseEvent) => {
    if (e.target === overlay) closeModal();
  };
  overlay.addEventListener("click", onOverlayClick, { once: true });
  close.addEventListener("click", closeModal, { once: true });
  return body;
}

export function closeModal(): void {
  document.getElementById("modal-overlay")!.classList.add("hidden");
}
