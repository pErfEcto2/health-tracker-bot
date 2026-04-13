import enum
import uuid

from sqlalchemy import Enum, String, Text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class MuscleGroup(str, enum.Enum):
    CHEST = "chest"
    BACK = "back"
    SHOULDERS = "shoulders"
    ARMS = "arms"
    LEGS = "legs"
    CORE = "core"
    CARDIO = "cardio"


class Exercise(Base):
    __tablename__ = "exercises"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    name: Mapped[str] = mapped_column(String(255), unique=True)
    muscle_group: Mapped[str] = mapped_column(
        Enum(MuscleGroup, name="muscle_group_enum")
    )
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
