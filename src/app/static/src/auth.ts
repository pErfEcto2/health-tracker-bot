import { api } from "./api";
import {
  deriveKeysFromPassword,
  deriveRecoveryKeys,
  fromHex,
  genDEK,
  genRecoveryKey,
  parseRecoveryKey,
  toHex,
  unwrapBytes,
  wrapBytes,
} from "./crypto";
import { clearDek, setDek } from "./session";

interface SaltResponse { salt_hex: string; must_change_password: boolean; }
interface LoginResponse { must_change_password: boolean; wrapped_dek_password_hex: string | null; }
interface MeResponse { username: string; must_change_password: boolean; }

export async function fetchMe(): Promise<MeResponse | null> {
  try { return await api.get<MeResponse>("/auth/me"); }
  catch { return null; }
}

export interface LoginResult {
  mustChangePassword: boolean;
  /** Raw DEK decrypted from server blob — null if first-login. Caller stores via setDek. */
  dek: Uint8Array | null;
}

/**
 * Log in with username + password. Returns the login result.
 * On must_change_password=true and no wrapped DEK: DEK is null; caller routes to change-password.
 * On normal login: DEK is unwrapped and stored in session.
 */
export async function login(username: string, password: string): Promise<LoginResult> {
  const salt = await api.post<SaltResponse>("/auth/salt", { username });
  const { authKeyHex, kek } = await deriveKeysFromPassword(password, fromHex(salt.salt_hex));
  const resp = await api.post<LoginResponse>("/auth/login", { username, auth_key_hex: authKeyHex });

  if (resp.wrapped_dek_password_hex) {
    const dek = await unwrapBytes(fromHex(resp.wrapped_dek_password_hex), kek);
    setDek(dek);
    return { mustChangePassword: resp.must_change_password, dek };
  }
  return { mustChangePassword: resp.must_change_password, dek: null };
}

/**
 * First-login bootstrap: generate DEK + recovery key, upload wrapped copies.
 * Returns the (single-use) recovery key to show the user.
 */
export async function bootstrapAccount(newPassword: string): Promise<{ recoveryKey: Uint8Array }> {
  const newSalt = crypto.getRandomValues(new Uint8Array(16));
  const { authKeyHex: newAuth, kek: newKek } = await deriveKeysFromPassword(newPassword, newSalt);

  const dek = genDEK();
  const recoveryKey = genRecoveryKey();
  const { kek: recKek, authKeyHex: recAuth } = await deriveRecoveryKeys(recoveryKey);

  const wrappedPw = await wrapBytes(dek, newKek);
  const wrappedRec = await wrapBytes(dek, recKek);

  await api.post("/auth/change-password", {
    new_salt_hex: toHex(newSalt),
    new_auth_key_hex: newAuth,
    wrapped_dek_password_hex: toHex(wrappedPw),
    wrapped_dek_recovery_hex: toHex(wrappedRec),
    recovery_auth_key_hex: recAuth,
  });

  setDek(dek);
  return { recoveryKey };
}

/** Subsequent password change — does NOT regenerate recovery key. */
export async function changePassword(currentDek: Uint8Array, newPassword: string): Promise<void> {
  const newSalt = crypto.getRandomValues(new Uint8Array(16));
  const { authKeyHex: newAuth, kek: newKek } = await deriveKeysFromPassword(newPassword, newSalt);
  const wrappedPw = await wrapBytes(currentDek, newKek);
  await api.post("/auth/change-password", {
    new_salt_hex: toHex(newSalt),
    new_auth_key_hex: newAuth,
    wrapped_dek_password_hex: toHex(wrappedPw),
  });
}

export async function recoverAccount(
  username: string,
  recoveryKeyInput: string,
  newPassword: string,
): Promise<void> {
  const recoveryKey = parseRecoveryKey(recoveryKeyInput);
  const { kek: recKek, authKeyHex: recAuth } = await deriveRecoveryKeys(recoveryKey);

  const start = await api.post<{ wrapped_dek_recovery_hex: string }>("/auth/recover-start", { username });
  const dek = await unwrapBytes(fromHex(start.wrapped_dek_recovery_hex), recKek);

  const newSalt = crypto.getRandomValues(new Uint8Array(16));
  const { authKeyHex: newAuth, kek: newKek } = await deriveKeysFromPassword(newPassword, newSalt);
  const wrappedPw = await wrapBytes(dek, newKek);

  await api.post("/auth/recover-complete", {
    username,
    recovery_auth_key_hex: recAuth,
    new_salt_hex: toHex(newSalt),
    new_auth_key_hex: newAuth,
    wrapped_dek_password_hex: toHex(wrappedPw),
  });

  setDek(dek);
}

export async function logout(): Promise<void> {
  try { await api.post("/auth/logout"); } catch { /* best effort */ }
  clearDek();
}

export async function deleteAccount(): Promise<void> {
  await api.del("/auth/account");
  clearDek();
}
