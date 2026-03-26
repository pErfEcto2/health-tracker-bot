import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class MeasurementCreate(BaseModel):
    weight_kg: float | None = Field(None, gt=0, le=500)
    waist_cm: float | None = Field(None, gt=0, le=300)
    bicep_cm: float | None = Field(None, gt=0, le=100)
    hip_cm: float | None = Field(None, gt=0, le=300)
    chest_cm: float | None = Field(None, gt=0, le=300)
    measured_at: datetime | None = None


class MeasurementResponse(BaseModel):
    id: uuid.UUID
    weight_kg: float | None
    waist_cm: float | None
    bicep_cm: float | None
    hip_cm: float | None
    chest_cm: float | None
    measured_at: datetime

    model_config = {"from_attributes": True}
