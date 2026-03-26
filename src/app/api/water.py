import uuid
from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.water import WaterDailySummary, WaterEntryCreate, WaterEntryResponse
from app.services.water import (
    create_water_entry,
    delete_water_entry,
    get_water_entries,
    get_water_summary,
)

router = APIRouter(prefix="/water", tags=["water"])


@router.get("", response_model=list[WaterEntryResponse])
async def list_water(
    day: date | None = Query(None, alias="date"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await get_water_entries(db, user.telegram_id, day or date.today())


@router.post("", response_model=WaterEntryResponse, status_code=status.HTTP_201_CREATED)
async def add_water(
    data: WaterEntryCreate,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await create_water_entry(db, user.telegram_id, data)


@router.delete("/{entry_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_water(
    entry_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    deleted = await delete_water_entry(db, user.telegram_id, str(entry_id))
    if not deleted:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Запись не найдена")


@router.get("/summary", response_model=WaterDailySummary)
async def water_summary(
    day: date | None = Query(None, alias="date"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await get_water_summary(db, user.telegram_id, day or date.today())
