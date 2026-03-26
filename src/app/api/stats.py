import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.services.stats import (
    calories_by_day,
    exercise_progress,
    macros_by_day,
    measurements_history,
    water_by_day,
    weight_history,
    workout_frequency,
)

router = APIRouter(prefix="/stats", tags=["stats"])


@router.get("/calories")
async def stats_calories(
    period: str = Query("week", pattern="^(week|month|3months)$"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await calories_by_day(db, user.telegram_id, period)


@router.get("/macros")
async def stats_macros(
    period: str = Query("week", pattern="^(week|month|3months)$"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await macros_by_day(db, user.telegram_id, period)


@router.get("/weight")
async def stats_weight(
    period: str = Query("month", pattern="^(month|3months|year)$"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await weight_history(db, user.telegram_id, period)


@router.get("/exercise/{exercise_id}")
async def stats_exercise(
    exercise_id: uuid.UUID,
    period: str = Query("month", pattern="^(month|3months)$"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await exercise_progress(db, user.telegram_id, str(exercise_id), period)


@router.get("/workouts")
async def stats_workouts(
    period: str = Query("month", pattern="^(week|month|3months)$"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await workout_frequency(db, user.telegram_id, period)


@router.get("/measurements")
async def stats_measurements(
    period: str = Query("month", pattern="^(month|3months|year)$"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await measurements_history(db, user.telegram_id, period)


@router.get("/water")
async def stats_water(
    period: str = Query("week", pattern="^(week|month|3months)$"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await water_by_day(db, user.telegram_id, period)
