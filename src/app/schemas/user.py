from datetime import date, datetime

from pydantic import BaseModel, Field

from app.models.user import ActivityLevel, Gender


class UserProfile(BaseModel):
    telegram_id: int
    first_name: str
    last_name: str | None = None
    username: str | None = None
    gender: Gender | None = None
    weight_kg: float | None = None
    height_cm: float | None = None
    birth_date: date | None = None
    activity_level: ActivityLevel | None = None
    created_at: datetime

    model_config = {"from_attributes": True}


class UserProfileUpdate(BaseModel):
    gender: Gender | None = None
    weight_kg: float | None = Field(None, gt=0, le=500)
    height_cm: float | None = Field(None, gt=0, le=300)
    birth_date: date | None = None
    activity_level: ActivityLevel | None = None


class TDEEResponse(BaseModel):
    bmr: float
    tdee: float
    activity_level: str
