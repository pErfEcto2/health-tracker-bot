// Generate crypto fixtures using Web Crypto. Run with: node scripts/gen-fixtures.mjs
// Output: app/src/test/resources/crypto-fixtures.json
//
// Reproduces algorithms in src/app/static/src/crypto.ts so Android tests can
// verify byte-for-byte match.

import { webcrypto as crypto } from "node:crypto";
import { writeFileSync } from "node:fs";

const KDF_ITERATIONS = 600_000;

function hex(bytes) {
  return Array.from(new Uint8Array(bytes)).map(b => b.toString(16).padStart(2, "0")).join("");
}
function unhex(h) {
  const out = new Uint8Array(h.length / 2);
  for (let i = 0; i < out.length; i++) out[i] = parseInt(h.substr(i * 2, 2), 16);
  return out;
}

async function pbkdf2(password, salt, iterations, len) {
  const k = await crypto.subtle.importKey("raw", new TextEncoder().encode(password), { name: "PBKDF2" }, false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", salt, iterations, hash: "SHA-256" }, k, len * 8);
  return new Uint8Array(bits);
}

async function hkdf(ikm, info, len) {
  const k = await crypto.subtle.importKey("raw", ikm, { name: "HKDF" }, false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits({
    name: "HKDF", hash: "SHA-256",
    salt: new Uint8Array(32),
    info: new TextEncoder().encode(info),
  }, k, len * 8);
  return new Uint8Array(bits);
}

async function aesGcmEncrypt(plaintext, key, nonce) {
  const k = await crypto.subtle.importKey("raw", key, { name: "AES-GCM" }, false, ["encrypt", "decrypt"]);
  const ct = await crypto.subtle.encrypt({ name: "AES-GCM", iv: nonce }, k, plaintext);
  return new Uint8Array(ct);
}

const password = "MyStrongPassword1!";
const salt = unhex("0102030405060708090a0b0c0d0e0f10");
const dek = unhex("11121314151617181920212223242526272829303132333435363738394041");
const dekFixed = unhex("1112131415161718192021222324252627282930313233343536373839404142");
const nonce = unhex("a1a2a3a4a5a6a7a8a9aaabac");

const mk = await pbkdf2(password, salt, KDF_ITERATIONS, 32);
const authKey = await hkdf(mk, "trackhub-auth", 32);
const encKey = await hkdf(mk, "trackhub-enc", 32);

const recoveryKey = unhex("ffeeddccbbaa99887766554433221100ffeeddccbbaa99887766554433221100");
const recKek = await hkdf(recoveryKey, "trackhub-recovery", 32);
const recAuth = await hkdf(recoveryKey, "trackhub-recovery-auth", 32);

// AES-GCM encrypt sample plaintext (JSON of a food entry)
const plaintext = new TextEncoder().encode(JSON.stringify({
  name: "Apple", calories: 52, protein_g: 0.3, fat_g: 0.2, carbs_g: 14,
  grams: 100, meal_type: "breakfast", logged_at: "2026-04-13T08:00:00Z",
}));
const wrappedDek = await aesGcmEncrypt(dekFixed, encKey, nonce);
const ciphertext = await aesGcmEncrypt(plaintext, dekFixed, nonce);

const fixtures = {
  pbkdf2: {
    password, salt_hex: hex(salt), iterations: KDF_ITERATIONS, len: 32,
    expected_hex: hex(mk),
  },
  hkdf_auth: { ikm_hex: hex(mk), info: "trackhub-auth", len: 32, expected_hex: hex(authKey) },
  hkdf_enc: { ikm_hex: hex(mk), info: "trackhub-enc", len: 32, expected_hex: hex(encKey) },
  hkdf_recovery: { ikm_hex: hex(recoveryKey), info: "trackhub-recovery", len: 32, expected_hex: hex(recKek) },
  hkdf_recovery_auth: { ikm_hex: hex(recoveryKey), info: "trackhub-recovery-auth", len: 32, expected_hex: hex(recAuth) },
  aes_gcm_wrap_dek: {
    key_hex: hex(encKey), nonce_hex: hex(nonce),
    plaintext_hex: hex(dekFixed), expected_ciphertext_hex: hex(wrappedDek),
  },
  aes_gcm_record: {
    key_hex: hex(dekFixed), nonce_hex: hex(nonce),
    plaintext_utf8: new TextDecoder().decode(plaintext),
    expected_ciphertext_hex: hex(ciphertext),
  },
};

writeFileSync(
  new URL("../app/src/test/resources/crypto-fixtures.json", import.meta.url),
  JSON.stringify(fixtures, null, 2),
);
console.log("wrote crypto-fixtures.json");
