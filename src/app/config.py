from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    # Telegram
    TELEGRAM_BOT_TOKEN: SecretStr
    MINIAPP_URL: str = "http://localhost:3000"

    # Database
    DATABASE_URL: str = "postgresql+asyncpg://trackhub:trackhub@db:5432/trackhub"

    # Redis
    REDIS_URL: str = "redis://redis:6379/0"

    # Web server
    HTTP_LISTEN_HOST: str = "0.0.0.0"
    HTTP_LISTEN_PORT: int = 3000

    # Security
    SECRET_KEY: SecretStr

    # Auth
    INIT_DATA_MAX_AGE_SECONDS: int = 300

    # Database pool
    DB_POOL_TIMEOUT: int = 30
    DB_POOL_RECYCLE: int = 1800

    # Dev mode: skip initData validation (NEVER enable in production)
    DEV_MODE: bool = False


settings = Settings()
