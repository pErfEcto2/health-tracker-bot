from datetime import date, datetime, time, timezone

from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.food import FoodEntry
from app.schemas.food import DailySummary, FoodEntryCreate


def _day_range(d: date) -> tuple[datetime, datetime]:
    start = datetime.combine(d, time.min, tzinfo=timezone.utc)
    end = datetime.combine(d, time.max, tzinfo=timezone.utc)
    return start, end


async def get_food_entries(db: AsyncSession, user_id: int, day: date) -> list[FoodEntry]:
    start, end = _day_range(day)
    result = await db.execute(
        select(FoodEntry)
        .where(FoodEntry.user_id == user_id)
        .where(FoodEntry.logged_at.between(start, end))
        .order_by(FoodEntry.logged_at)
    )
    return list(result.scalars().all())


async def create_food_entry(db: AsyncSession, user_id: int, data: FoodEntryCreate) -> FoodEntry:
    entry = FoodEntry(
        user_id=user_id,
        name=data.name,
        calories=data.calories,
        protein_g=data.protein_g,
        fat_g=data.fat_g,
        carbs_g=data.carbs_g,
        amount_g=data.amount_g,
        meal_type=data.meal_type,
    )
    if data.logged_at:
        entry.logged_at = data.logged_at
    db.add(entry)
    await db.flush()
    return entry


async def delete_food_entry(db: AsyncSession, user_id: int, entry_id: str) -> bool:
    result = await db.execute(
        select(FoodEntry).where(FoodEntry.id == entry_id, FoodEntry.user_id == user_id)
    )
    entry = result.scalar_one_or_none()
    if entry is None:
        return False
    await db.delete(entry)
    return True


async def get_daily_summary(db: AsyncSession, user_id: int, day: date) -> DailySummary:
    start, end = _day_range(day)
    result = await db.execute(
        select(
            func.coalesce(func.sum(FoodEntry.calories), 0),
            func.coalesce(func.sum(FoodEntry.protein_g), 0),
            func.coalesce(func.sum(FoodEntry.fat_g), 0),
            func.coalesce(func.sum(FoodEntry.carbs_g), 0),
            func.count(FoodEntry.id),
        )
        .where(FoodEntry.user_id == user_id)
        .where(FoodEntry.logged_at.between(start, end))
    )
    row = result.one()
    return DailySummary(
        date=day.isoformat(),
        total_calories=row[0],
        total_protein_g=row[1],
        total_fat_g=row[2],
        total_carbs_g=row[3],
        entries_count=row[4],
    )
