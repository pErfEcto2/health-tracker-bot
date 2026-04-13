import logging
from datetime import datetime, timedelta, timezone

from fastapi import Cookie, Depends, Header, HTTPException, Request
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.crypto import constant_time_eq, hash_session_token
from app.database import get_db
from app.models.session import Session
from app.models.user import User
from app.redis import get_redis

logger = logging.getLogger(__name__)

SESSION_COOKIE = "session"
CSRF_COOKIE = "csrf_token"
CSRF_HEADER = "X-CSRF-Token"
SESSION_TTL_DAYS = 7


async def _load_session(
    token: str | None, db: AsyncSession
) -> tuple[Session, User] | None:
    if not token:
        return None
    th = hash_session_token(token)
    result = await db.execute(select(Session).where(Session.token_hash == th))
    sess = result.scalar_one_or_none()
    if sess is None:
        return None
    now = datetime.now(timezone.utc)
    if sess.expires_at < now:
        await db.delete(sess)
        return None
    user = await db.get(User, sess.user_id)
    if user is None:
        await db.delete(sess)
        return None
    # Sliding refresh
    sess.expires_at = now + timedelta(days=SESSION_TTL_DAYS)
    return sess, user


async def get_current_user(
    request: Request,
    session: str | None = Cookie(default=None, alias=SESSION_COOKIE),
    csrf_cookie: str | None = Cookie(default=None, alias=CSRF_COOKIE),
    csrf_header: str | None = Header(default=None, alias=CSRF_HEADER),
    db: AsyncSession = Depends(get_db),
) -> User:
    """Authenticate via session cookie. Verify CSRF on state-changing methods."""
    loaded = await _load_session(session, db)
    if loaded is None:
        raise HTTPException(status_code=401, detail="Not authenticated")
    _, user = loaded

    if request.method not in {"GET", "HEAD", "OPTIONS"}:
        if not csrf_cookie or not csrf_header or not constant_time_eq(csrf_cookie, csrf_header):
            raise HTTPException(status_code=403, detail="CSRF token missing or invalid")

    return user


async def get_current_user_allow_must_change(
    request: Request,
    session: str | None = Cookie(default=None, alias=SESSION_COOKIE),
    csrf_cookie: str | None = Cookie(default=None, alias=CSRF_COOKIE),
    csrf_header: str | None = Header(default=None, alias=CSRF_HEADER),
    db: AsyncSession = Depends(get_db),
) -> User:
    """Same as get_current_user but does not check must_change_password flag —
    used for the change-password endpoint itself."""
    return await get_current_user(request, session, csrf_cookie, csrf_header, db)


def rate_limit_endpoint(limit: int = 3, window: int = 60):
    """Per-endpoint rate limit (Redis-backed, skips if Redis unavailable)."""

    async def _check(request: Request):
        ip = request.client.host if request.client else "unknown"
        key = f"rl:{request.url.path}:{ip}"
        try:
            redis = get_redis()
            count = await redis.incr(key)
            if count == 1:
                await redis.expire(key, window)
            if count > limit:
                raise HTTPException(
                    status_code=429,
                    detail="Too many requests for this endpoint",
                )
        except HTTPException:
            raise
        except Exception:
            pass

    return _check
