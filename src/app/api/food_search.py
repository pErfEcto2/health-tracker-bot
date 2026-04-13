import json
import logging
from urllib.parse import quote

import httpx
from fastapi import APIRouter, Depends, Query

from app.dependencies import get_current_user
from app.models.user import User
from app.redis import get_redis
from app.services.food_db import search_local

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/food-search", tags=["food-search"])

CACHE_TTL = 86400


@router.get("")
async def search_food(
    q: str = Query(..., min_length=2, max_length=100),
    _user: User = Depends(get_current_user),
):
    """Search food: local DB first, then OpenFoodFacts with Redis cache."""

    local = search_local(q)

    cache_key = f"food_search:{q.lower().strip()}"
    redis = get_redis()
    cached = None
    try:
        cached = await redis.get(cache_key)
    except Exception:
        logger.warning("Redis read failed for food search cache", exc_info=True)

    if cached:
        remote = json.loads(cached)
    else:
        remote = await _search_openfoodfacts(q)
        try:
            await redis.set(cache_key, json.dumps(remote, ensure_ascii=False), ex=CACHE_TTL)
        except Exception:
            logger.warning("Redis write failed for food search cache", exc_info=True)

    seen = set()
    results = []
    for item in local + remote:
        key = item["name"].lower()
        if key in seen:
            continue
        seen.add(key)
        if (item["calories_per_100g"] == 0 and item["protein_per_100g"] == 0
                and item["fat_per_100g"] == 0 and item["carbs_per_100g"] == 0):
            continue
        results.append(item)

    return results[:12]


async def _search_openfoodfacts(query: str) -> list[dict]:
    for locale in ("ru", "world"):
        results = await _fetch_off(query, locale)
        if results:
            return results
    return []


async def _fetch_off(query: str, locale: str) -> list[dict]:
    url = (
        f"https://{locale}.openfoodfacts.org/cgi/search.pl"
        f"?search_terms={quote(query)}"
        "&search_simple=1&action=process&json=1&page_size=15"
        "&fields=product_name,product_name_ru,nutriments,serving_size"
    )

    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            resp = await client.get(url, headers={"User-Agent": "TrackHub/1.0"})
            resp.raise_for_status()
            data = resp.json()
    except Exception:
        logger.warning("OpenFoodFacts request failed (locale=%s)", locale, exc_info=True)
        return []

    results = []
    seen = set()

    for product in data.get("products", []):
        name = (
            product.get("product_name_ru", "").strip()
            or product.get("product_name", "").strip()
        )
        if not name or name.lower() in seen:
            continue
        seen.add(name.lower())

        n = product.get("nutriments", {})
        calories = float(n.get("energy-kcal_100g", 0) or 0)
        protein = float(n.get("proteins_100g", 0) or 0)
        fat = float(n.get("fat_100g", 0) or 0)
        carbs = float(n.get("carbohydrates_100g", 0) or 0)

        if calories == 0 and (protein > 0 or fat > 0 or carbs > 0):
            calories = round(protein * 4 + fat * 9 + carbs * 4, 1)

        results.append({
            "name": name,
            "calories_per_100g": round(calories, 1),
            "protein_per_100g": round(protein, 1),
            "fat_per_100g": round(fat, 1),
            "carbs_per_100g": round(carbs, 1),
            "serving_size": product.get("serving_size", ""),
            "source": "openfoodfacts",
        })

    return results
