from datetime import date

from app.models.user import ActivityLevel, Gender, User

ACTIVITY_MULTIPLIERS = {
    ActivityLevel.SEDENTARY: 1.2,
    ActivityLevel.LIGHT: 1.375,
    ActivityLevel.MODERATE: 1.55,
    ActivityLevel.ACTIVE: 1.725,
    ActivityLevel.VERY_ACTIVE: 1.9,
}


def calculate_age(birth_date: date) -> int:
    today = date.today()
    return today.year - birth_date.year - (
        (today.month, today.day) < (birth_date.month, birth_date.day)
    )


def calculate_bmr(user: User) -> float | None:
    """Mifflin-St Jeor equation."""
    if not all([user.weight_kg, user.height_cm, user.birth_date, user.gender]):
        return None

    age = calculate_age(user.birth_date)

    if user.gender == Gender.MALE:
        return 10 * user.weight_kg + 6.25 * user.height_cm - 5 * age + 5
    else:
        return 10 * user.weight_kg + 6.25 * user.height_cm - 5 * age - 161


def calculate_tdee(user: User) -> tuple[float, float] | None:
    """Returns (BMR, TDEE) or None if insufficient data."""
    bmr = calculate_bmr(user)
    if bmr is None or not user.activity_level:
        return None

    multiplier = ACTIVITY_MULTIPLIERS.get(user.activity_level, 1.2)
    return bmr, bmr * multiplier
