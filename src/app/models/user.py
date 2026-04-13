from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, Integer, LargeBinary, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    username: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)

    salt: Mapped[bytes] = mapped_column(LargeBinary(16), nullable=False)
    auth_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    recovery_auth_hash: Mapped[str | None] = mapped_column(String(255), nullable=True)
    wrapped_dek_password: Mapped[bytes | None] = mapped_column(
        LargeBinary, nullable=True
    )
    wrapped_dek_recovery: Mapped[bytes | None] = mapped_column(
        LargeBinary, nullable=True
    )
    must_change_password: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=True
    )

    telegram_chat_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    sessions = relationship(
        "Session", back_populates="user", cascade="all, delete-orphan", lazy="noload"
    )
    records = relationship(
        "Record", back_populates="user", cascade="all, delete-orphan", lazy="noload"
    )
