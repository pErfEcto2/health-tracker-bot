// DEK lifecycle — survives reload via sessionStorage (cleared on tab close).

import { fromHex, toHex } from "./crypto";

const KEY = "trackhub_dek";

let cachedDek: Uint8Array | null = null;

export function setDek(dek: Uint8Array): void {
  cachedDek = dek;
  sessionStorage.setItem(KEY, toHex(dek));
}

export function getDek(): Uint8Array | null {
  if (cachedDek) return cachedDek;
  const hex = sessionStorage.getItem(KEY);
  if (!hex) return null;
  cachedDek = fromHex(hex);
  return cachedDek;
}

export function clearDek(): void {
  cachedDek = null;
  sessionStorage.removeItem(KEY);
}

export function hasDek(): boolean {
  return getDek() !== null;
}
