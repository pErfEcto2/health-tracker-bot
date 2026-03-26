# Built-in database of common foods (per 100g)
# This eliminates API dependency for popular products
COMMON_FOODS: list[dict] = [
    # Фрукты
    {"name": "Яблоко", "calories": 52, "protein": 0.3, "fat": 0.2, "carbs": 14},
    {"name": "Банан", "calories": 89, "protein": 1.1, "fat": 0.3, "carbs": 23},
    {"name": "Апельсин", "calories": 47, "protein": 0.9, "fat": 0.1, "carbs": 12},
    {"name": "Груша", "calories": 57, "protein": 0.4, "fat": 0.1, "carbs": 15},
    {"name": "Виноград", "calories": 67, "protein": 0.6, "fat": 0.4, "carbs": 17},
    {"name": "Клубника", "calories": 32, "protein": 0.7, "fat": 0.3, "carbs": 8},
    {"name": "Арбуз", "calories": 30, "protein": 0.6, "fat": 0.2, "carbs": 8},
    {"name": "Мандарин", "calories": 53, "protein": 0.8, "fat": 0.3, "carbs": 13},
    {"name": "Персик", "calories": 39, "protein": 0.9, "fat": 0.3, "carbs": 10},
    {"name": "Слива", "calories": 46, "protein": 0.7, "fat": 0.3, "carbs": 11},
    {"name": "Киви", "calories": 61, "protein": 1.1, "fat": 0.5, "carbs": 15},
    {"name": "Ананас", "calories": 50, "protein": 0.5, "fat": 0.1, "carbs": 13},
    {"name": "Манго", "calories": 60, "protein": 0.8, "fat": 0.4, "carbs": 15},
    {"name": "Авокадо", "calories": 160, "protein": 2, "fat": 15, "carbs": 9},
    {"name": "Грейпфрут", "calories": 42, "protein": 0.8, "fat": 0.1, "carbs": 11},

    # Овощи
    {"name": "Огурец", "calories": 15, "protein": 0.7, "fat": 0.1, "carbs": 3.6},
    {"name": "Помидор", "calories": 18, "protein": 0.9, "fat": 0.2, "carbs": 3.9},
    {"name": "Картофель", "calories": 77, "protein": 2, "fat": 0.1, "carbs": 17},
    {"name": "Морковь", "calories": 41, "protein": 0.9, "fat": 0.2, "carbs": 10},
    {"name": "Капуста белокочанная", "calories": 25, "protein": 1.3, "fat": 0.1, "carbs": 6},
    {"name": "Брокколи", "calories": 34, "protein": 2.8, "fat": 0.4, "carbs": 7},
    {"name": "Лук репчатый", "calories": 40, "protein": 1.1, "fat": 0.1, "carbs": 9},
    {"name": "Перец болгарский", "calories": 27, "protein": 1.3, "fat": 0, "carbs": 5.3},
    {"name": "Свёкла", "calories": 43, "protein": 1.6, "fat": 0.2, "carbs": 10},
    {"name": "Кабачок", "calories": 17, "protein": 1.2, "fat": 0.3, "carbs": 3.1},
    {"name": "Баклажан", "calories": 25, "protein": 1, "fat": 0.2, "carbs": 6},
    {"name": "Шпинат", "calories": 23, "protein": 2.9, "fat": 0.4, "carbs": 3.6},
    {"name": "Салат листовой", "calories": 15, "protein": 1.4, "fat": 0.2, "carbs": 2.9},

    # Мясо
    {"name": "Куриная грудка", "calories": 165, "protein": 31, "fat": 3.6, "carbs": 0},
    {"name": "Куриное бедро", "calories": 209, "protein": 26, "fat": 11, "carbs": 0},
    {"name": "Говядина (вырезка)", "calories": 250, "protein": 26, "fat": 15, "carbs": 0},
    {"name": "Свинина (вырезка)", "calories": 242, "protein": 27, "fat": 14, "carbs": 0},
    {"name": "Фарш говяжий", "calories": 254, "protein": 17, "fat": 20, "carbs": 0},
    {"name": "Фарш куриный", "calories": 143, "protein": 17, "fat": 8, "carbs": 0},
    {"name": "Индейка (грудка)", "calories": 135, "protein": 30, "fat": 1, "carbs": 0},
    {"name": "Баранина", "calories": 294, "protein": 25, "fat": 21, "carbs": 0},
    {"name": "Печень куриная", "calories": 136, "protein": 19, "fat": 6, "carbs": 1},
    {"name": "Колбаса варёная", "calories": 260, "protein": 12, "fat": 23, "carbs": 2},
    {"name": "Сосиски", "calories": 277, "protein": 11, "fat": 24, "carbs": 2},

    # Рыба
    {"name": "Лосось", "calories": 208, "protein": 20, "fat": 13, "carbs": 0},
    {"name": "Тунец", "calories": 132, "protein": 29, "fat": 1, "carbs": 0},
    {"name": "Треска", "calories": 82, "protein": 18, "fat": 0.7, "carbs": 0},
    {"name": "Минтай", "calories": 72, "protein": 16, "fat": 0.9, "carbs": 0},
    {"name": "Скумбрия", "calories": 262, "protein": 18, "fat": 21, "carbs": 0},
    {"name": "Креветки", "calories": 99, "protein": 24, "fat": 0.3, "carbs": 0.2},
    {"name": "Сельдь", "calories": 217, "protein": 19, "fat": 15, "carbs": 0},

    # Молочное
    {"name": "Молоко 2.5%", "calories": 52, "protein": 2.8, "fat": 2.5, "carbs": 4.7},
    {"name": "Молоко 3.2%", "calories": 59, "protein": 2.9, "fat": 3.2, "carbs": 4.7},
    {"name": "Кефир 1%", "calories": 40, "protein": 3, "fat": 1, "carbs": 4},
    {"name": "Кефир 2.5%", "calories": 53, "protein": 2.9, "fat": 2.5, "carbs": 4},
    {"name": "Творог 5%", "calories": 121, "protein": 17, "fat": 5, "carbs": 1.8},
    {"name": "Творог 9%", "calories": 159, "protein": 16, "fat": 9, "carbs": 2},
    {"name": "Творог обезжиренный", "calories": 86, "protein": 18, "fat": 0.6, "carbs": 1.5},
    {"name": "Сметана 15%", "calories": 162, "protein": 2.6, "fat": 15, "carbs": 3.6},
    {"name": "Сметана 20%", "calories": 206, "protein": 2.8, "fat": 20, "carbs": 3.2},
    {"name": "Йогурт натуральный", "calories": 60, "protein": 4, "fat": 1.5, "carbs": 7},
    {"name": "Сыр твёрдый", "calories": 350, "protein": 26, "fat": 27, "carbs": 0},
    {"name": "Масло сливочное", "calories": 717, "protein": 0.9, "fat": 81, "carbs": 0.1},

    # Яйца
    {"name": "Яйцо куриное (целое)", "calories": 155, "protein": 13, "fat": 11, "carbs": 1.1},
    {"name": "Яйцо куриное (белок)", "calories": 52, "protein": 11, "fat": 0.2, "carbs": 0.7},
    {"name": "Яйцо куриное (желток)", "calories": 322, "protein": 16, "fat": 27, "carbs": 3.6},

    # Крупы и гарниры (сухие)
    {"name": "Рис белый (сухой)", "calories": 344, "protein": 6.7, "fat": 0.7, "carbs": 79},
    {"name": "Рис варёный", "calories": 130, "protein": 2.7, "fat": 0.3, "carbs": 28},
    {"name": "Гречка (сухая)", "calories": 313, "protein": 12, "fat": 3.3, "carbs": 62},
    {"name": "Гречка варёная", "calories": 110, "protein": 4.2, "fat": 1.1, "carbs": 21},
    {"name": "Овсянка (сухая)", "calories": 352, "protein": 12, "fat": 6, "carbs": 60},
    {"name": "Овсянка на воде", "calories": 88, "protein": 3, "fat": 1.7, "carbs": 15},
    {"name": "Макароны (сухие)", "calories": 350, "protein": 12, "fat": 1.8, "carbs": 72},
    {"name": "Макароны варёные", "calories": 131, "protein": 5, "fat": 1.1, "carbs": 27},
    {"name": "Булгур (сухой)", "calories": 342, "protein": 12, "fat": 1.3, "carbs": 76},
    {"name": "Киноа (сухая)", "calories": 368, "protein": 14, "fat": 6, "carbs": 64},
    {"name": "Перловка (сухая)", "calories": 320, "protein": 9.3, "fat": 1.1, "carbs": 74},

    # Хлеб и выпечка
    {"name": "Хлеб белый", "calories": 265, "protein": 9, "fat": 3.2, "carbs": 49},
    {"name": "Хлеб чёрный (ржаной)", "calories": 174, "protein": 6.6, "fat": 1.2, "carbs": 33},
    {"name": "Хлеб цельнозерновой", "calories": 247, "protein": 13, "fat": 3.4, "carbs": 41},
    {"name": "Лаваш тонкий", "calories": 275, "protein": 9.1, "fat": 1.2, "carbs": 56},

    # Масла и жиры
    {"name": "Масло оливковое", "calories": 884, "protein": 0, "fat": 100, "carbs": 0},
    {"name": "Масло подсолнечное", "calories": 884, "protein": 0, "fat": 100, "carbs": 0},

    # Орехи
    {"name": "Грецкий орех", "calories": 654, "protein": 15, "fat": 65, "carbs": 14},
    {"name": "Миндаль", "calories": 579, "protein": 21, "fat": 50, "carbs": 22},
    {"name": "Арахис", "calories": 567, "protein": 26, "fat": 49, "carbs": 16},
    {"name": "Кешью", "calories": 553, "protein": 18, "fat": 44, "carbs": 30},

    # Бобовые
    {"name": "Чечевица (сухая)", "calories": 352, "protein": 25, "fat": 1.1, "carbs": 60},
    {"name": "Нут (сухой)", "calories": 364, "protein": 19, "fat": 6, "carbs": 61},
    {"name": "Фасоль (сухая)", "calories": 333, "protein": 21, "fat": 2, "carbs": 60},

    # Сладкое
    {"name": "Сахар", "calories": 387, "protein": 0, "fat": 0, "carbs": 100},
    {"name": "Мёд", "calories": 304, "protein": 0.3, "fat": 0, "carbs": 82},
    {"name": "Шоколад молочный", "calories": 535, "protein": 7.6, "fat": 30, "carbs": 59},
    {"name": "Шоколад тёмный (70%)", "calories": 598, "protein": 7.8, "fat": 43, "carbs": 46},

    # Напитки
    {"name": "Кофе чёрный (без сахара)", "calories": 2, "protein": 0.3, "fat": 0, "carbs": 0},
    {"name": "Чай без сахара", "calories": 1, "protein": 0, "fat": 0, "carbs": 0},
    {"name": "Сок апельсиновый", "calories": 45, "protein": 0.7, "fat": 0.2, "carbs": 10},
    {"name": "Кока-Кола", "calories": 42, "protein": 0, "fat": 0, "carbs": 11},

    # Готовые блюда (примерно)
    {"name": "Борщ", "calories": 49, "protein": 2.7, "fat": 1.8, "carbs": 5.4},
    {"name": "Щи", "calories": 32, "protein": 2, "fat": 1.2, "carbs": 3.6},
    {"name": "Окрошка", "calories": 52, "protein": 2.5, "fat": 1.9, "carbs": 6.3},
    {"name": "Плов с курицей", "calories": 150, "protein": 10, "fat": 5, "carbs": 18},
    {"name": "Пельмени", "calories": 275, "protein": 12, "fat": 14, "carbs": 25},
    {"name": "Блины", "calories": 233, "protein": 6, "fat": 12, "carbs": 26},
    {"name": "Оливье", "calories": 198, "protein": 5, "fat": 16, "carbs": 8},
    {"name": "Пицца Маргарита", "calories": 250, "protein": 11, "fat": 10, "carbs": 28},
    {"name": "Шаурма куриная", "calories": 210, "protein": 12, "fat": 10, "carbs": 18},
]


def _normalize(text: str) -> str:
    """Normalize for fuzzy matching: lowercase, strip common suffixes."""
    return text.lower().replace("ё", "е").strip()


def _matches(query: str, name: str) -> int:
    """Return match score (0 = no match, higher = better)."""
    q = _normalize(query)
    n = _normalize(name)

    # Exact match in name
    if q in n:
        return 100 if n.startswith(q) else 50

    # Match each word of query against name
    q_words = q.split()
    matched = sum(1 for w in q_words if any(w in part for part in n.split()))
    if matched == len(q_words):
        return 40

    # Match word stems (first 3+ chars)
    for w in q_words:
        stem = w[:min(len(w), 4)] if len(w) > 3 else w
        if stem in n:
            return 20

    return 0


def search_local(query: str) -> list[dict]:
    scored = []
    for food in COMMON_FOODS:
        score = _matches(query, food["name"])
        if score > 0:
            scored.append((score, food))

    scored.sort(key=lambda x: -x[0])

    return [
        {
            "name": food["name"],
            "calories_per_100g": food["calories"],
            "protein_per_100g": food["protein"],
            "fat_per_100g": food["fat"],
            "carbs_per_100g": food["carbs"],
            "serving_size": "",
            "source": "local",
        }
        for _, food in scored
    ]
