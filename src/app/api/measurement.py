from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.measurement import BodyMeasurement
from app.models.user import User
from app.schemas.measurement import MeasurementCreate, MeasurementResponse

router = APIRouter(prefix="/measurements", tags=["measurements"])


@router.get("", response_model=list[MeasurementResponse])
async def list_measurements(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    limit: int = Query(50, ge=1, le=200),
):
    result = await db.execute(
        select(BodyMeasurement)
        .where(BodyMeasurement.user_id == user.telegram_id)
        .order_by(BodyMeasurement.measured_at.desc())
        .limit(limit)
    )
    return list(result.scalars().all())


@router.post("", response_model=MeasurementResponse, status_code=status.HTTP_201_CREATED)
async def add_measurement(
    data: MeasurementCreate,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    measurement = BodyMeasurement(
        user_id=user.telegram_id,
        weight_kg=data.weight_kg,
        waist_cm=data.waist_cm,
        bicep_cm=data.bicep_cm,
        hip_cm=data.hip_cm,
        chest_cm=data.chest_cm,
    )
    if data.measured_at:
        measurement.measured_at = data.measured_at
    db.add(measurement)
    await db.flush()
    return measurement
