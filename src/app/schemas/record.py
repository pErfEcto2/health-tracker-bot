from datetime import date, datetime
from uuid import UUID

from pydantic import BaseModel, Field

ALLOWED_TYPES = {
    "profile",
    "food_entry",
    "water_entry",
    "measurement",
    "workout_session",
}


class RecordCreate(BaseModel):
    type: str = Field(min_length=1, max_length=32)
    record_date: date
    nonce_hex: str = Field(min_length=24, max_length=24)  # 12 bytes hex
    ciphertext_hex: str = Field(min_length=1)


class RecordUpdate(BaseModel):
    record_date: date | None = None
    nonce_hex: str = Field(min_length=24, max_length=24)
    ciphertext_hex: str = Field(min_length=1)


class RecordResponse(BaseModel):
    id: UUID
    type: str
    record_date: date
    nonce_hex: str
    ciphertext_hex: str
    created_at: datetime
    updated_at: datetime
