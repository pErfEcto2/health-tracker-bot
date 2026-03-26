from datetime import date, datetime, time, timezone

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.water import WaterEntry
from app.schemas.water import WaterDailySummary, WaterEntryCreate


def _day_range(d: date) -> tuple[datetime, datetime]:
    start = datetime.combine(d, time.min, tzinfo=timezone.utc)
    end = datetime.combine(d, time.max, tzinfo=timezone.utc)
    return start, end


async def get_water_entries(db: AsyncSession, user_id: int, day: date) -> list[WaterEntry]:
    start, end = _day_range(day)
    result = await db.execute(
        select(WaterEntry)
        .where(WaterEntry.user_id == user_id, WaterEntry.logged_at.between(start, end))
        .order_by(WaterEntry.logged_at)
    )
    return list(result.scalars().all())


async def create_water_entry(db: AsyncSession, user_id: int, data: WaterEntryCreate) -> WaterEntry:
    entry = WaterEntry(user_id=user_id, amount_ml=data.amount_ml)
    if data.logged_at:
        entry.logged_at = data.logged_at
    db.add(entry)
    await db.flush()
    return entry


async def delete_water_entry(db: AsyncSession, user_id: int, entry_id: str) -> bool:
    result = await db.execute(
        select(WaterEntry).where(WaterEntry.id == entry_id, WaterEntry.user_id == user_id)
    )
    entry = result.scalar_one_or_none()
    if entry is None:
        return False
    await db.delete(entry)
    return True


async def get_water_summary(db: AsyncSession, user_id: int, day: date) -> WaterDailySummary:
    start, end = _day_range(day)
    result = await db.execute(
        select(
            func.coalesce(func.sum(WaterEntry.amount_ml), 0),
            func.count(WaterEntry.id),
        )
        .where(WaterEntry.user_id == user_id, WaterEntry.logged_at.between(start, end))
    )
    row = result.one()
    total_ml = float(row[0])
    return WaterDailySummary(
        date=day.isoformat(),
        total_ml=total_ml,
        glasses=round(total_ml / 250, 1),
        entries_count=row[1],
    )
