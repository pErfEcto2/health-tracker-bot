import logging

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.exercise import Exercise
from app.models.user import User
from app.schemas.exercise import ExerciseResponse

logger = logging.getLogger(__name__)

router = APIRouter(tags=["exercise"])


@router.get("/exercises", response_model=list[ExerciseResponse])
async def list_exercises(
    _user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(Exercise).order_by(Exercise.name))
    return list(result.scalars().all())
