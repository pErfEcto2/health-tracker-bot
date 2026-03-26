from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.exercise import WorkoutSession, WorkoutSet
from app.models.food import FoodEntry
from app.models.measurement import BodyMeasurement
from app.models.user import User
from app.models.water import WaterEntry
from app.schemas.user import UserProfileUpdate


async def update_profile(db: AsyncSession, user: User, data: UserProfileUpdate) -> User:
    update_data = data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(user, field, value)
    await db.flush()
    return user


async def delete_user_data(db: AsyncSession, user_id: int) -> None:
    """Delete all user data (GDPR right to erasure). CASCADE handles child rows."""
    result = await db.execute(select(User).where(User.telegram_id == user_id))
    user = result.scalar_one_or_none()
    if user:
        await db.delete(user)


async def export_user_data(db: AsyncSession, user_id: int) -> dict:
    """Export all user data as a dictionary (GDPR data portability)."""
    result = await db.execute(select(User).where(User.telegram_id == user_id))
    user = result.scalar_one_or_none()
    if not user:
        return {}

    # Food entries
    food_rows = await db.execute(
        select(FoodEntry).where(FoodEntry.user_id == user_id).order_by(FoodEntry.logged_at)
    )
    food = [
        {
            "name": e.name,
            "calories": e.calories,
            "protein_g": e.protein_g,
            "fat_g": e.fat_g,
            "carbs_g": e.carbs_g,
            "amount_g": e.amount_g,
            "meal_type": e.meal_type,
            "logged_at": e.logged_at.isoformat() if e.logged_at else None,
        }
        for e in food_rows.scalars().all()
    ]

    # Water entries
    water_rows = await db.execute(
        select(WaterEntry).where(WaterEntry.user_id == user_id).order_by(WaterEntry.logged_at)
    )
    water = [
        {
            "amount_ml": e.amount_ml,
            "logged_at": e.logged_at.isoformat() if e.logged_at else None,
        }
        for e in water_rows.scalars().all()
    ]

    # Body measurements
    meas_rows = await db.execute(
        select(BodyMeasurement).where(BodyMeasurement.user_id == user_id).order_by(BodyMeasurement.measured_at)
    )
    measurements = [
        {
            "weight_kg": m.weight_kg,
            "waist_cm": m.waist_cm,
            "bicep_cm": m.bicep_cm,
            "hip_cm": m.hip_cm,
            "chest_cm": m.chest_cm,
            "measured_at": m.measured_at.isoformat() if m.measured_at else None,
        }
        for m in meas_rows.scalars().all()
    ]

    # Workout sessions with sets
    ws_rows = await db.execute(
        select(WorkoutSession)
        .options(selectinload(WorkoutSession.sets).selectinload(WorkoutSet.exercise))
        .where(WorkoutSession.user_id == user_id)
        .order_by(WorkoutSession.started_at)
    )
    workouts = []
    for s in ws_rows.scalars().all():
        sets = []
        for ws in s.sets:
            ex_name = None
            try:
                ex_name = ws.exercise.name if ws.exercise else None
            except Exception:
                pass
            sets.append({
                "exercise": ex_name,
                "set_number": ws.set_number,
                "reps": ws.reps,
                "weight_kg": ws.weight_kg,
            })
        workouts.append({
            "started_at": s.started_at.isoformat() if s.started_at else None,
            "finished_at": s.finished_at.isoformat() if s.finished_at else None,
            "notes": s.notes,
            "sets": sets,
        })

    return {
        "profile": {
            "telegram_id": user.telegram_id,
            "first_name": user.first_name,
            "last_name": user.last_name,
            "username": user.username,
            "gender": user.gender,
            "weight_kg": user.weight_kg,
            "height_cm": user.height_cm,
            "birth_date": user.birth_date.isoformat() if user.birth_date else None,
            "activity_level": user.activity_level,
            "created_at": user.created_at.isoformat() if user.created_at else None,
        },
        "food_entries": food,
        "water_entries": water,
        "body_measurements": measurements,
        "workouts": workouts,
    }
