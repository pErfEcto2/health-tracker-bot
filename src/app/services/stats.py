from datetime import date, datetime, time, timedelta, timezone

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.exercise import WorkoutSession, WorkoutSet
from app.models.food import FoodEntry
from app.models.measurement import BodyMeasurement
from app.models.water import WaterEntry


PERIOD_DAYS = {
    "week": 7,
    "month": 30,
    "3months": 90,
    "year": 365,
}


def _period_start(period: str) -> datetime:
    days = PERIOD_DAYS.get(period, 30)
    d = date.today() - timedelta(days=days)
    return datetime.combine(d, time.min, tzinfo=timezone.utc)


async def calories_by_day(db: AsyncSession, user_id: int, period: str) -> list[dict]:
    start = _period_start(period)
    result = await db.execute(
        select(
            func.date(FoodEntry.logged_at).label("day"),
            func.sum(FoodEntry.calories).label("total"),
        )
        .where(FoodEntry.user_id == user_id, FoodEntry.logged_at >= start)
        .group_by(func.date(FoodEntry.logged_at))
        .order_by(func.date(FoodEntry.logged_at))
    )
    return [{"date": str(row.day), "calories": float(row.total)} for row in result.all()]


async def macros_by_day(db: AsyncSession, user_id: int, period: str) -> list[dict]:
    start = _period_start(period)
    result = await db.execute(
        select(
            func.date(FoodEntry.logged_at).label("day"),
            func.sum(FoodEntry.protein_g).label("protein"),
            func.sum(FoodEntry.fat_g).label("fat"),
            func.sum(FoodEntry.carbs_g).label("carbs"),
        )
        .where(FoodEntry.user_id == user_id, FoodEntry.logged_at >= start)
        .group_by(func.date(FoodEntry.logged_at))
        .order_by(func.date(FoodEntry.logged_at))
    )
    return [
        {
            "date": str(row.day),
            "protein": float(row.protein),
            "fat": float(row.fat),
            "carbs": float(row.carbs),
        }
        for row in result.all()
    ]


async def weight_history(db: AsyncSession, user_id: int, period: str) -> list[dict]:
    start = _period_start(period)
    result = await db.execute(
        select(BodyMeasurement)
        .where(
            BodyMeasurement.user_id == user_id,
            BodyMeasurement.measured_at >= start,
            BodyMeasurement.weight_kg.is_not(None),
        )
        .order_by(BodyMeasurement.measured_at)
    )
    return [
        {"date": m.measured_at.isoformat(), "weight": m.weight_kg}
        for m in result.scalars().all()
    ]


async def exercise_progress(db: AsyncSession, user_id: int, exercise_id: str, period: str) -> list[dict]:
    start = _period_start(period)
    result = await db.execute(
        select(
            func.date(WorkoutSession.started_at).label("day"),
            func.max(WorkoutSet.weight_kg).label("max_weight"),
            func.sum(WorkoutSet.reps * WorkoutSet.weight_kg).label("volume"),
        )
        .join(WorkoutSet, WorkoutSet.session_id == WorkoutSession.id)
        .where(
            WorkoutSession.user_id == user_id,
            WorkoutSet.exercise_id == exercise_id,
            WorkoutSession.started_at >= start,
        )
        .group_by(func.date(WorkoutSession.started_at))
        .order_by(func.date(WorkoutSession.started_at))
    )
    return [
        {
            "date": str(row.day),
            "max_weight": float(row.max_weight) if row.max_weight else 0,
            "volume": float(row.volume) if row.volume else 0,
        }
        for row in result.all()
    ]


async def workout_frequency(db: AsyncSession, user_id: int, period: str) -> list[dict]:
    start = _period_start(period)
    result = await db.execute(
        select(
            func.date(WorkoutSession.started_at).label("day"),
            func.count(WorkoutSession.id).label("count"),
        )
        .where(WorkoutSession.user_id == user_id, WorkoutSession.started_at >= start)
        .group_by(func.date(WorkoutSession.started_at))
        .order_by(func.date(WorkoutSession.started_at))
    )
    return [{"date": str(row.day), "count": row.count} for row in result.all()]


async def measurements_history(db: AsyncSession, user_id: int, period: str) -> list[dict]:
    start = _period_start(period)
    result = await db.execute(
        select(BodyMeasurement)
        .where(BodyMeasurement.user_id == user_id, BodyMeasurement.measured_at >= start)
        .order_by(BodyMeasurement.measured_at)
    )
    return [
        {
            "date": m.measured_at.isoformat(),
            "weight_kg": m.weight_kg,
            "waist_cm": m.waist_cm,
            "bicep_cm": m.bicep_cm,
            "hip_cm": m.hip_cm,
            "chest_cm": m.chest_cm,
        }
        for m in result.scalars().all()
    ]


async def water_by_day(db: AsyncSession, user_id: int, period: str) -> list[dict]:
    start = _period_start(period)
    result = await db.execute(
        select(
            func.date(WaterEntry.logged_at).label("day"),
            func.sum(WaterEntry.amount_ml).label("total_ml"),
        )
        .where(WaterEntry.user_id == user_id, WaterEntry.logged_at >= start)
        .group_by(func.date(WaterEntry.logged_at))
        .order_by(func.date(WaterEntry.logged_at))
    )
    return [
        {"date": str(row.day), "total_ml": float(row.total_ml), "glasses": round(float(row.total_ml) / 250, 1)}
        for row in result.all()
    ]
