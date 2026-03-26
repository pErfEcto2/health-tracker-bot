from fastapi import APIRouter

from app.api.auth import router as auth_router
from app.api.debug import router as debug_router
from app.api.exercise import router as exercise_router
from app.api.food import router as food_router
from app.api.measurement import router as measurement_router
from app.api.profile import router as profile_router
from app.api.food_search import router as food_search_router
from app.api.stats import router as stats_router
from app.api.water import router as water_router

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(auth_router)
api_router.include_router(debug_router)
api_router.include_router(profile_router)
api_router.include_router(food_router)
api_router.include_router(food_search_router)
api_router.include_router(exercise_router)
api_router.include_router(measurement_router)
api_router.include_router(stats_router)
api_router.include_router(water_router)
