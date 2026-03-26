import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class WaterEntryCreate(BaseModel):
    amount_ml: float = Field(..., gt=0, le=5000)
    logged_at: datetime | None = None


class WaterEntryResponse(BaseModel):
    id: uuid.UUID
    amount_ml: float
    logged_at: datetime

    model_config = {"from_attributes": True}


class WaterDailySummary(BaseModel):
    date: str
    total_ml: float
    glasses: float  # total_ml / 250
    entries_count: int
