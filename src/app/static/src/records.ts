// Client-side record CRUD with transparent encryption.

import { api } from "./api";
import { decryptRecord, encryptRecord } from "./crypto";
import { getDek } from "./session";
import type { EncryptedRecord, RecordType } from "./types";

function requireDek(): Uint8Array {
  const dek = getDek();
  if (!dek) throw new Error("Not authenticated (no DEK)");
  return dek;
}

export interface DecryptedRecord<T> {
  id: string;
  type: RecordType;
  record_date: string;
  payload: T;
  created_at: string;
  updated_at: string;
}

interface ListOptions {
  type?: RecordType;
  from?: string;
  to?: string;
}

export async function listRecords<T = unknown>(opts: ListOptions = {}): Promise<DecryptedRecord<T>[]> {
  const dek = requireDek();
  const q = new URLSearchParams();
  if (opts.type) q.set("type", opts.type);
  if (opts.from) q.set("from", opts.from);
  if (opts.to) q.set("to", opts.to);
  const qs = q.toString();
  const raw = await api.get<EncryptedRecord[]>(`/records${qs ? "?" + qs : ""}`);
  const out: DecryptedRecord<T>[] = [];
  for (const r of raw) {
    try {
      const payload = await decryptRecord<T>({ nonceHex: r.nonce_hex, ciphertextHex: r.ciphertext_hex }, dek);
      out.push({
        id: r.id, type: r.type, record_date: r.record_date,
        payload, created_at: r.created_at, updated_at: r.updated_at,
      });
    } catch {
      // Skip undecryptable records — log to console for debugging
      console.warn(`failed to decrypt record ${r.id} of type ${r.type}`);
    }
  }
  return out;
}

export async function createRecord<T>(type: RecordType, recordDate: string, payload: T): Promise<DecryptedRecord<T>> {
  const dek = requireDek();
  const blob = await encryptRecord(payload, dek);
  const raw = await api.post<EncryptedRecord>("/records", {
    type, record_date: recordDate,
    nonce_hex: blob.nonceHex, ciphertext_hex: blob.ciphertextHex,
  });
  return {
    id: raw.id, type: raw.type, record_date: raw.record_date,
    payload, created_at: raw.created_at, updated_at: raw.updated_at,
  };
}

export async function updateRecord<T>(id: string, payload: T, recordDate?: string): Promise<DecryptedRecord<T>> {
  const dek = requireDek();
  const blob = await encryptRecord(payload, dek);
  const body: Record<string, unknown> = {
    nonce_hex: blob.nonceHex, ciphertext_hex: blob.ciphertextHex,
  };
  if (recordDate) body.record_date = recordDate;
  const raw = await api.put<EncryptedRecord>(`/records/${id}`, body);
  return {
    id: raw.id, type: raw.type, record_date: raw.record_date,
    payload, created_at: raw.created_at, updated_at: raw.updated_at,
  };
}

export async function deleteRecord(id: string): Promise<void> {
  await api.del(`/records/${id}`);
}

/** Fetch or create the single "profile" record for the user. */
export async function loadProfile<T>(defaults: T): Promise<DecryptedRecord<T>> {
  const rows = await listRecords<T>({ type: "profile" });
  if (rows.length > 0) return rows[0]!;
  // Create empty profile for today
  const today = new Date().toISOString().slice(0, 10);
  return createRecord<T>("profile", today, defaults);
}
