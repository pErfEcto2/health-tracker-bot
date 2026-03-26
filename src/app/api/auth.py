from fastapi import APIRouter, Depends

from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.user import UserProfile

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/validate", response_model=UserProfile)
async def validate(user: User = Depends(get_current_user)):
    """Validate initData and return/create user profile."""
    return user
