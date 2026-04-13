from uuid import UUID

from pydantic import BaseModel

from app.models.exercise import MuscleGroup


class ExerciseResponse(BaseModel):
    id: UUID
    name: str
    muscle_group: MuscleGroup
    description: str | None = None

    model_config = {"from_attributes": True}
