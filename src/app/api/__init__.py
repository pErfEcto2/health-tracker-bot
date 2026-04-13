from fastapi import APIRouter

from app.api.auth import router as auth_router
from app.api.debug import router as debug_router
from app.api.exercise import router as exercise_router
from app.api.food_search import router as food_search_router
from app.api.records import router as records_router

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(auth_router)
api_router.include_router(debug_router)
api_router.include_router(records_router)
api_router.include_router(exercise_router)
api_router.include_router(food_search_router)
