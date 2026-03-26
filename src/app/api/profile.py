from datetime import date

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import JSONResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user, rate_limit_endpoint
from app.models.user import User
from app.schemas.user import TDEEResponse, UserProfile, UserProfileUpdate
from app.services.tdee import calculate_tdee
from app.services.user import delete_user_data, export_user_data, update_profile

router = APIRouter(prefix="/profile", tags=["profile"])


@router.get("", response_model=UserProfile)
async def get_profile(user: User = Depends(get_current_user)):
    return user


@router.put("", response_model=UserProfile)
async def update_user_profile(
    data: UserProfileUpdate,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if data.birth_date and data.birth_date > date.today():
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Дата рождения не может быть в будущем",
        )
    return await update_profile(db, user, data)


@router.get("/tdee", response_model=TDEEResponse)
async def get_tdee(user: User = Depends(get_current_user)):
    result = calculate_tdee(user)
    if result is None:
        return TDEEResponse(bmr=0, tdee=0, activity_level="unknown")
    bmr, tdee = result
    return TDEEResponse(
        bmr=round(bmr, 1),
        tdee=round(tdee, 1),
        activity_level=user.activity_level or "unknown",
    )


@router.get("/export", dependencies=[Depends(rate_limit_endpoint(3, 60))])
async def export_profile(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Export all user data (GDPR data portability)."""
    data = await export_user_data(db, user.telegram_id)
    return JSONResponse(content=data)


@router.delete(
    "",
    status_code=status.HTTP_204_NO_CONTENT,
    dependencies=[Depends(rate_limit_endpoint(3, 60))],
)
async def delete_profile(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Delete user account and all associated data (GDPR right to erasure)."""
    await delete_user_data(db, user.telegram_id)
