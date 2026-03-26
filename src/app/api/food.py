import uuid
from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.food import DailySummary, FoodEntryCreate, FoodEntryResponse
from app.services.food import (
    create_food_entry,
    delete_food_entry,
    get_daily_summary,
    get_food_entries,
)

router = APIRouter(prefix="/food", tags=["food"])


@router.get("", response_model=list[FoodEntryResponse])
async def list_food(
    day: date | None = Query(None, alias="date"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await get_food_entries(db, user.telegram_id, day or date.today())


@router.post("", response_model=FoodEntryResponse, status_code=status.HTTP_201_CREATED)
async def add_food(
    data: FoodEntryCreate,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await create_food_entry(db, user.telegram_id, data)


@router.delete("/{entry_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_food(
    entry_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    deleted = await delete_food_entry(db, user.telegram_id, str(entry_id))
    if not deleted:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Запись не найдена")


@router.get("/summary", response_model=DailySummary)
async def food_summary(
    day: date | None = Query(None, alias="date"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await get_daily_summary(db, user.telegram_id, day or date.today())
