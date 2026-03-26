import logging

from fastapi import Depends, Header, HTTPException, Request
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import settings
from app.database import get_db
from app.redis import get_redis
from app.models.user import User
from app.security import validate_init_data

logger = logging.getLogger(__name__)

DEV_USER = {
    "id": 1,
    "first_name": "Dev",
    "last_name": "User",
    "username": "dev",
}

_dev_mode_warned = False


async def get_current_user(
    x_telegram_init_data: str = Header(default=""),
    db: AsyncSession = Depends(get_db),
) -> User:
    """Validate Telegram initData and return or create the user."""
    if settings.DEV_MODE:
        global _dev_mode_warned
        if not _dev_mode_warned:
            logger.warning("DEV_MODE is ON — authentication is disabled!")
            _dev_mode_warned = True
        user_data = DEV_USER
    else:
        user_data = validate_init_data(x_telegram_init_data)

    telegram_id = user_data["id"]

    result = await db.execute(select(User).where(User.telegram_id == telegram_id))
    user = result.scalar_one_or_none()

    if user is None:
        user = User(
            telegram_id=telegram_id,
            first_name=user_data.get("first_name", ""),
            last_name=user_data.get("last_name"),
            username=user_data.get("username"),
        )
        db.add(user)
        await db.flush()

    return user


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
