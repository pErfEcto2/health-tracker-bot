import enum
from datetime import date, datetime

from sqlalchemy import BigInteger, Date, DateTime, Enum, Float, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class Gender(str, enum.Enum):
    MALE = "male"
    FEMALE = "female"


class ActivityLevel(str, enum.Enum):
    SEDENTARY = "sedentary"
    LIGHT = "light"
    MODERATE = "moderate"
    ACTIVE = "active"
    VERY_ACTIVE = "very_active"


class User(Base):
    __tablename__ = "users"

    telegram_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    first_name: Mapped[str] = mapped_column(String(255), default="")
    last_name: Mapped[str | None] = mapped_column(String(255), nullable=True)
    username: Mapped[str | None] = mapped_column(String(255), nullable=True)

    # Profile
    gender: Mapped[str | None] = mapped_column(
        Enum(Gender, name="gender_enum"), nullable=True
    )
    weight_kg: Mapped[float | None] = mapped_column(Float, nullable=True)
    height_cm: Mapped[float | None] = mapped_column(Float, nullable=True)
    birth_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    activity_level: Mapped[str | None] = mapped_column(
        Enum(ActivityLevel, name="activity_level_enum"), nullable=True
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    # Relationships
    food_entries = relationship("FoodEntry", back_populates="user", lazy="noload")
    workout_sessions = relationship(
        "WorkoutSession", back_populates="user", lazy="noload"
    )
    body_measurements = relationship(
        "BodyMeasurement", back_populates="user", lazy="noload"
    )
    water_entries = relationship(
        "WaterEntry", back_populates="user", lazy="noload"
    )
