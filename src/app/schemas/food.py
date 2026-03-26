import uuid
from datetime import datetime

from pydantic import BaseModel, Field, model_validator

from app.models.food import MealType


class FoodEntryCreate(BaseModel):
    """Supports multiple input modes:
    - Full: name + calories + protein + fat + carbs + amount
    - Name + weight: name + amount_g (macros = 0, user fills what they know)
    - Quick: name + calories only
    """
    name: str = Field(..., min_length=1, max_length=255)
    calories: float = Field(0, ge=0, le=50000)
    protein_g: float = Field(0, ge=0, le=5000)
    fat_g: float = Field(0, ge=0, le=5000)
    carbs_g: float = Field(0, ge=0, le=5000)
    amount_g: float = Field(0, ge=0, le=50000)
    meal_type: MealType = MealType.SNACK
    logged_at: datetime | None = None

    @model_validator(mode="after")
    def check_at_least_something(self):
        has_macros = self.calories > 0 or self.protein_g > 0 or self.fat_g > 0 or self.carbs_g > 0
        has_weight = self.amount_g > 0
        if not has_macros and not has_weight:
            raise ValueError("Укажите хотя бы калории или массу продукта")
        return self


class FoodEntryResponse(BaseModel):
    id: uuid.UUID
    name: str
    calories: float
    protein_g: float
    fat_g: float
    carbs_g: float
    amount_g: float
    meal_type: MealType
    logged_at: datetime

    model_config = {"from_attributes": True}


class DailySummary(BaseModel):
    date: str
    total_calories: float
    total_protein_g: float
    total_fat_g: float
    total_carbs_g: float
    entries_count: int
