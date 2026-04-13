import hashlib
import hmac
import secrets

from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError

_hasher = PasswordHasher()

# PBKDF2 params — MUST match client-side constants in crypto.ts
KDF_ITERATIONS = 600_000
KDF_HASH = "sha256"
KDF_KEYLEN = 32  # 256 bits


def derive_auth_key_hex(password: str, salt: bytes) -> str:
    """PBKDF2-SHA256 → HKDF "trackhub-auth" replica, matching client-side.

    For the admin CLI we skip the HKDF split: we use the PBKDF2 output directly
    as the master key AND as the auth key (client will HKDF-derive an auth
    subkey — must use the same fn). Keep both sides aligned with this helper.
    """
    mk = hashlib.pbkdf2_hmac(KDF_HASH, password.encode("utf-8"), salt, KDF_ITERATIONS, KDF_KEYLEN)
    auth_key = _hkdf(mk, b"trackhub-auth", 32)
    return auth_key.hex()


def _hkdf(ikm: bytes, info: bytes, length: int) -> bytes:
    """HKDF-SHA256 with empty salt."""
    salt = b"\x00" * 32
    prk = hmac.new(salt, ikm, hashlib.sha256).digest()
    t = b""
    okm = b""
    i = 1
    while len(okm) < length:
        t = hmac.new(prk, t + info + bytes([i]), hashlib.sha256).digest()
        okm += t
        i += 1
    return okm[:length]


def hash_auth_key(auth_key_hex: str) -> str:
    """Argon2id hash of the client-derived auth key (hex)."""
    return _hasher.hash(auth_key_hex)


def verify_auth_key(stored_hash: str, auth_key_hex: str) -> bool:
    try:
        _hasher.verify(stored_hash, auth_key_hex)
        return True
    except VerifyMismatchError:
        return False
    except Exception:
        return False


def generate_session_token() -> tuple[str, bytes]:
    """Return (token_base64url, sha256(token_bytes))."""
    raw = secrets.token_bytes(32)
    token = secrets.token_urlsafe(32)
    # token_urlsafe already gives us a random string; hash its bytes for DB storage
    del raw
    token_hash = hashlib.sha256(token.encode("ascii")).digest()
    return token, token_hash


def hash_session_token(token: str) -> bytes:
    return hashlib.sha256(token.encode("ascii")).digest()


def generate_csrf_token() -> str:
    return secrets.token_urlsafe(32)


def constant_time_eq(a: str, b: str) -> bool:
    return hmac.compare_digest(a.encode("utf-8"), b.encode("utf-8"))


def generate_temp_password(length: int = 16) -> str:
    """URL-safe random temp password for admin-created accounts."""
    return secrets.token_urlsafe(length)[:length]


def generate_salt() -> bytes:
    return secrets.token_bytes(16)
