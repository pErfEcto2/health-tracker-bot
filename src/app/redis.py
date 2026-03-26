from redis.asyncio import ConnectionPool, Redis

from app.config import settings

pool = ConnectionPool.from_url(settings.REDIS_URL, decode_responses=True)


def get_redis() -> Redis:
    return Redis(connection_pool=pool)
