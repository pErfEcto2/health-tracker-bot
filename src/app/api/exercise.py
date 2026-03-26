import logging
import uuid
from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.exercise import (
    ExerciseResponse,
    WorkoutSessionCreate,
    WorkoutSessionResponse,
    WorkoutSetCreate,
    WorkoutSetResponse,
)
from app.services.exercise import (
    add_workout_set,
    create_workout,
    delete_workout,
    get_exercises,
    get_workout_by_id,
    get_workouts_by_date,
)

logger = logging.getLogger(__name__)

router = APIRouter(tags=["exercise"])


@router.get("/exercises", response_model=list[ExerciseResponse])
async def list_exercises(
    _user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await get_exercises(db)


@router.get("/workouts", response_model=list[WorkoutSessionResponse])
async def list_workouts(
    day: date | None = Query(None, alias="date"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    sessions = await get_workouts_by_date(db, user.telegram_id, day or date.today())
    return [_session_to_response(s) for s in sessions]


@router.post("/workouts", response_model=WorkoutSessionResponse, status_code=status.HTTP_201_CREATED)
async def new_workout(
    data: WorkoutSessionCreate,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    session = await create_workout(db, user.telegram_id, data)
    # Return directly without accessing lazy relationships
    return WorkoutSessionResponse(
        id=session.id,
        started_at=session.started_at,
        finished_at=session.finished_at,
        notes=session.notes,
        sets=[],
    )


@router.get("/workouts/{workout_id}", response_model=WorkoutSessionResponse)
async def get_workout(
    workout_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    session = await get_workout_by_id(db, user.telegram_id, workout_id)
    if session is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Тренировка не найдена")
    return _session_to_response(session)


@router.post("/workouts/{workout_id}/sets", response_model=WorkoutSetResponse, status_code=status.HTTP_201_CREATED)
async def add_set(
    workout_id: uuid.UUID,
    data: WorkoutSetCreate,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    # Validate exercise exists
    exercises = await get_exercises(db)
    exercise_ids = {e.id for e in exercises}
    if data.exercise_id not in exercise_ids:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Упражнение не найдено")

    ws = await add_workout_set(db, user.telegram_id, workout_id, data)
    if ws is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Тренировка не найдена")

    exercise_name = next((e.name for e in exercises if e.id == data.exercise_id), None)
    return WorkoutSetResponse(
        id=ws.id,
        exercise_id=ws.exercise_id,
        exercise_name=exercise_name,
        set_number=ws.set_number,
        reps=ws.reps,
        weight_kg=ws.weight_kg,
    )


@router.delete("/workouts/{workout_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_workout(
    workout_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    deleted = await delete_workout(db, user.telegram_id, workout_id)
    if not deleted:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Тренировка не найдена")


def _session_to_response(session) -> WorkoutSessionResponse:
    sets = []
    try:
        loaded_sets = session.sets or []
    except Exception:
        logger.warning("Failed to load sets for session %s", session.id, exc_info=True)
        loaded_sets = []
    for s in loaded_sets:
        exercise_name = None
        try:
            exercise_name = s.exercise.name if s.exercise else None
        except Exception:
            logger.warning("Failed to load exercise for set %s", s.id, exc_info=True)
        sets.append(WorkoutSetResponse(
            id=s.id,
            exercise_id=s.exercise_id,
            exercise_name=exercise_name,
            set_number=s.set_number,
            reps=s.reps,
            weight_kg=s.weight_kg,
        ))
    return WorkoutSessionResponse(
        id=session.id,
        started_at=session.started_at,
        finished_at=session.finished_at,
        notes=session.notes,
        sets=sets,
    )
