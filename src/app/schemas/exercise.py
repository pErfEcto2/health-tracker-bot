import uuid
from datetime import datetime

from pydantic import BaseModel, Field

from app.models.exercise import MuscleGroup


class ExerciseResponse(BaseModel):
    id: uuid.UUID
    name: str
    muscle_group: MuscleGroup
    description: str | None = None

    model_config = {"from_attributes": True}


class WorkoutSetCreate(BaseModel):
    exercise_id: uuid.UUID
    set_number: int = Field(..., ge=1)
    reps: int = Field(..., ge=0)
    weight_kg: float = Field(0, ge=0)


class WorkoutSetResponse(BaseModel):
    id: uuid.UUID
    exercise_id: uuid.UUID
    exercise_name: str | None = None
    set_number: int
    reps: int
    weight_kg: float

    model_config = {"from_attributes": True}


class WorkoutSessionCreate(BaseModel):
    notes: str | None = Field(None, max_length=2000)
    started_at: datetime | None = None


class WorkoutSessionResponse(BaseModel):
    id: uuid.UUID
    started_at: datetime
    finished_at: datetime | None = None
    notes: str | None = None
    sets: list[WorkoutSetResponse] = []

    model_config = {"from_attributes": True}
