// Client-side crypto primitives. Must match server-side constants in src/app/crypto.py.
//
// Parameters:
//   PBKDF2-SHA256, 600 000 iterations, 256-bit master key
//   HKDF-SHA256 sub-keys with empty salt and string info
//   AES-GCM 256-bit with 12-byte random nonces
//
// Architecture:
//   MK = PBKDF2(password, salt)
//   authKey  = HKDF(MK, "trackhub-auth")           → sent to server (hex)
//   KEK      = HKDF(MK, "trackhub-enc")            → wraps DEK client-only
//   recoveryKEK      = HKDF(recoveryKey, "trackhub-recovery")
//   recoveryAuthKey  = HKDF(recoveryKey, "trackhub-recovery-auth")  → sent during recovery
//   DEK is random 256-bit, generated once at first password set.
//   Records are encrypted as nonce(12) || AES-GCM(DEK, JSON(record)).

const KDF_ITERATIONS = 600_000;

// ===== Hex =====

export function toHex(bytes: Uint8Array): string {
  let out = "";
  for (const b of bytes) out += b.toString(16).padStart(2, "0");
  return out;
}

export function fromHex(hex: string): Uint8Array {
  if (hex.length % 2) throw new Error("invalid hex length");
  const out = new Uint8Array(hex.length / 2);
  for (let i = 0; i < out.length; i++) {
    out[i] = parseInt(hex.substr(i * 2, 2), 16);
  }
  return out;
}

export function randomBytes(n: number): Uint8Array {
  const a = new Uint8Array(n);
  crypto.getRandomValues(a);
  return a;
}

// ===== KDF =====

async function pbkdf2(password: string, salt: Uint8Array, iterations: number, keyLen: number): Promise<Uint8Array> {
  const passKey = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(password) as BufferSource,
    { name: "PBKDF2" },
    false,
    ["deriveBits"],
  );
  const bits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt: salt as BufferSource, iterations, hash: "SHA-256" },
    passKey,
    keyLen * 8,
  );
  return new Uint8Array(bits);
}

async function hkdf(ikm: Uint8Array, info: string, length: number): Promise<Uint8Array> {
  const ikmKey = await crypto.subtle.importKey(
    "raw",
    ikm as BufferSource,
    { name: "HKDF" },
    false,
    ["deriveBits"],
  );
  const bits = await crypto.subtle.deriveBits(
    {
      name: "HKDF",
      hash: "SHA-256",
      salt: new Uint8Array(32),
      info: new TextEncoder().encode(info),
    },
    ikmKey,
    length * 8,
  );
  return new Uint8Array(bits);
}

// ===== Public derivation API =====

export interface DerivedKeys {
  authKeyHex: string;   // sent to server
  kek: Uint8Array;      // wraps DEK
}

export async function deriveKeysFromPassword(password: string, salt: Uint8Array): Promise<DerivedKeys> {
  const mk = await pbkdf2(password, salt, KDF_ITERATIONS, 32);
  const authKey = await hkdf(mk, "trackhub-auth", 32);
  const kek = await hkdf(mk, "trackhub-enc", 32);
  return { authKeyHex: toHex(authKey), kek };
}

export async function deriveRecoveryKeys(recoveryKey: Uint8Array): Promise<{ kek: Uint8Array; authKeyHex: string }> {
  const kek = await hkdf(recoveryKey, "trackhub-recovery", 32);
  const authKey = await hkdf(recoveryKey, "trackhub-recovery-auth", 32);
  return { kek, authKeyHex: toHex(authKey) };
}

// ===== AES-GCM wrap/unwrap =====

async function importAesKey(raw: Uint8Array): Promise<CryptoKey> {
  return crypto.subtle.importKey("raw", raw as BufferSource, { name: "AES-GCM" }, false, ["encrypt", "decrypt"]);
}

export async function wrapBytes(plaintext: Uint8Array, key: Uint8Array): Promise<Uint8Array> {
  const aesKey = await importAesKey(key);
  const nonce = randomBytes(12);
  const ct = new Uint8Array(
    await crypto.subtle.encrypt({ name: "AES-GCM", iv: nonce as BufferSource }, aesKey, plaintext as BufferSource),
  );
  const out = new Uint8Array(nonce.length + ct.length);
  out.set(nonce, 0);
  out.set(ct, nonce.length);
  return out;
}

export async function unwrapBytes(blob: Uint8Array, key: Uint8Array): Promise<Uint8Array> {
  if (blob.length < 12 + 16) throw new Error("blob too short");
  const nonce = blob.subarray(0, 12);
  const ct = blob.subarray(12);
  const aesKey = await importAesKey(key);
  return new Uint8Array(
    await crypto.subtle.decrypt({ name: "AES-GCM", iv: nonce as BufferSource }, aesKey, ct as BufferSource),
  );
}

// ===== Record encryption =====

export interface EncryptedBlob {
  nonceHex: string;
  ciphertextHex: string;
}

export async function encryptRecord(obj: unknown, dek: Uint8Array): Promise<EncryptedBlob> {
  const pt = new TextEncoder().encode(JSON.stringify(obj));
  const aesKey = await importAesKey(dek);
  const nonce = randomBytes(12);
  const ct = new Uint8Array(
    await crypto.subtle.encrypt({ name: "AES-GCM", iv: nonce as BufferSource }, aesKey, pt as BufferSource),
  );
  return { nonceHex: toHex(nonce), ciphertextHex: toHex(ct) };
}

export async function decryptRecord<T = unknown>(blob: EncryptedBlob, dek: Uint8Array): Promise<T> {
  const aesKey = await importAesKey(dek);
  const nonce = fromHex(blob.nonceHex);
  const ct = fromHex(blob.ciphertextHex);
  const pt = new Uint8Array(
    await crypto.subtle.decrypt({ name: "AES-GCM", iv: nonce as BufferSource }, aesKey, ct as BufferSource),
  );
  return JSON.parse(new TextDecoder().decode(pt)) as T;
}

// ===== Bootstrap helpers (first password set) =====

export function genDEK(): Uint8Array {
  return randomBytes(32);
}

export function genRecoveryKey(): Uint8Array {
  return randomBytes(32);
}

/** Format recovery key as 8 groups of 8 hex chars, space-separated, for easy transcription. */
export function formatRecoveryKey(key: Uint8Array): string {
  const hex = toHex(key);
  const groups: string[] = [];
  for (let i = 0; i < hex.length; i += 8) groups.push(hex.slice(i, i + 8));
  return groups.join(" ");
}

export function parseRecoveryKey(input: string): Uint8Array {
  const clean = input.replace(/\s+/g, "").toLowerCase();
  if (!/^[0-9a-f]{64}$/.test(clean)) {
    throw new Error("Recovery key must be 64 hex characters");
  }
  return fromHex(clean);
}
