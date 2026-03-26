import uuid
from datetime import date, datetime, time, timezone

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.exercise import Exercise, WorkoutSession, WorkoutSet
from app.schemas.exercise import WorkoutSessionCreate, WorkoutSetCreate


def _day_range(d: date) -> tuple[datetime, datetime]:
    start = datetime.combine(d, time.min, tzinfo=timezone.utc)
    end = datetime.combine(d, time.max, tzinfo=timezone.utc)
    return start, end


async def get_exercises(db: AsyncSession) -> list[Exercise]:
    result = await db.execute(select(Exercise).order_by(Exercise.muscle_group, Exercise.name))
    return list(result.scalars().all())


async def get_workouts_by_date(db: AsyncSession, user_id: int, day: date) -> list[WorkoutSession]:
    start, end = _day_range(day)
    result = await db.execute(
        select(WorkoutSession)
        .options(selectinload(WorkoutSession.sets).selectinload(WorkoutSet.exercise))
        .where(WorkoutSession.user_id == user_id)
        .where(WorkoutSession.started_at.between(start, end))
        .order_by(WorkoutSession.started_at.desc())
    )
    return list(result.scalars().all())


async def get_workout_by_id(db: AsyncSession, user_id: int, workout_id: uuid.UUID) -> WorkoutSession | None:
    result = await db.execute(
        select(WorkoutSession)
        .options(selectinload(WorkoutSession.sets).selectinload(WorkoutSet.exercise))
        .where(WorkoutSession.id == workout_id, WorkoutSession.user_id == user_id)
    )
    return result.scalar_one_or_none()


async def create_workout(db: AsyncSession, user_id: int, data: WorkoutSessionCreate) -> WorkoutSession:
    session = WorkoutSession(
        user_id=user_id,
        notes=data.notes,
    )
    if data.started_at:
        session.started_at = data.started_at
    db.add(session)
    await db.flush()
    return session


async def add_workout_set(db: AsyncSession, user_id: int, workout_id: uuid.UUID, data: WorkoutSetCreate) -> WorkoutSet | None:
    workout = await get_workout_by_id(db, user_id, workout_id)
    if workout is None:
        return None

    workout_set = WorkoutSet(
        session_id=workout_id,
        exercise_id=data.exercise_id,
        set_number=data.set_number,
        reps=data.reps,
        weight_kg=data.weight_kg,
    )
    db.add(workout_set)
    await db.flush()
    return workout_set


async def delete_workout(db: AsyncSession, user_id: int, workout_id: uuid.UUID) -> bool:
    workout = await get_workout_by_id(db, user_id, workout_id)
    if workout is None:
        return False
    await db.delete(workout)
    return True
