# TrackHub

Telegram Mini App для трекинга здоровья: питание, тренировки, прогресс тела.

## Стек

| Компонент | Технология |
|-----------|-----------|
| Backend | Python 3.12, FastAPI (async) |
| Telegram бот | aiogram 3 |
| БД | PostgreSQL 16 |
| ORM | SQLAlchemy 2.0 (async) + Alembic |
| Кэш | Redis 7 |
| Frontend | Vanilla HTML/CSS/JS, Chart.js |
| Auth | Telegram initData + HMAC-SHA256 |
| Инфра | Docker Compose, UV, Ruff |

## Запуск

```bash
cp .env.example .env              # заполнить переменные
docker compose -f docker/docker-compose.yml --env-file .env up --build -d
```

Миграции (внутри контейнера):
```bash
docker compose -f docker/docker-compose.yml --env-file .env exec web uv run alembic upgrade head
```

Логи:
```bash
docker compose -f docker/docker-compose.yml --env-file .env logs -f
```

Остановка:
```bash
docker compose -f docker/docker-compose.yml --env-file .env down
```

Приложение доступно на `http://localhost:3000`.

## Структура

```
src/
├── app/                    # FastAPI приложение
│   ├── main.py             # App factory, middleware, lifespan
│   ├── config.py           # Pydantic Settings
│   ├── database.py         # Async SQLAlchemy engine
│   ├── redis.py            # Redis connection pool
│   ├── security.py         # Telegram initData HMAC валидация
│   ├── dependencies.py     # FastAPI dependencies (auth, db)
│   ├── models/             # SQLAlchemy модели
│   ├── schemas/            # Pydantic schemas
│   ├── api/                # API роуты (/api/v1/*)
│   ├── services/           # Бизнес-логика, TDEE, статистика
│   └── static/             # Mini App frontend (HTML/CSS/JS)
├── bot/                    # aiogram 3 бот
│   ├── main.py             # Точка входа, polling
│   └── handlers/           # Обработчики команд
└── alembic/                # Миграции БД
```

## Возможности

### Реализовано

- [x] Личный профиль (пол, вес, рост, дата рождения, уровень активности)
- [x] TDEE и BMR (Mifflin-St Jeor) с прогресс-барами на главной
- [x] Трекер калорий и КБЖУ (логирование еды, 3 режима ввода)
- [x] Поиск продуктов (встроенная база ~100 продуктов + OpenFoodFacts API)
- [x] Кэширование поиска в Redis (24 часа)
- [x] Трекер тренировок (сессии, подходы, повторы, веса)
- [x] Трекер воды (быстрые кнопки 150/250/330/500 мл, прогресс-бар, норма 2л)
- [x] Замеры тела (вес, талия, бицепс, бёдра, грудь)
- [x] Графики (Chart.js): калории, БЖУ, вес, замеры, прогресс упражнений, частота тренировок, вода
- [x] Telegram initData HMAC-SHA256 аутентификация
- [x] Rate limiting (Redis, 100 req/min)
- [x] Адаптивная тема (light/dark из Telegram + prefers-color-scheme)
- [x] DEV_MODE для локального тестирования без Telegram

### TODO

- [ ] Шаблоны и чеклисты тренировок (готовые программы на день)
- [ ] Общая база знаний (техника упражнений, питание)
- [ ] Личная база знаний (заметки пользователя)
- [ ] Таймер отдыха между подходами
- [ ] Напоминания через бота (еда, тренировки, вода)
- [ ] Прогресс-фото (загрузка фото с привязкой к дате)
- [ ] Стрики (серия дней без пропуска)
- [ ] HTTPS + домен (сейчас localhost)

## API

Swagger: `http://localhost:3000/docs`

Все эндпоинты под `/api/v1/`, требуют заголовок `X-Telegram-Init-Data` (кроме DEV_MODE).

| Группа | Эндпоинты |
|--------|-----------|
| Auth | `POST /auth/validate` |
| Профиль | `GET/PUT /profile`, `GET /profile/tdee` |
| Питание | `GET/POST /food`, `DELETE /food/{id}`, `GET /food/summary` |
| Поиск | `GET /food-search?q=...` |
| Тренировки | `GET/POST /workouts`, `GET/DELETE /workouts/{id}`, `POST /workouts/{id}/sets` |
| Упражнения | `GET /exercises` |
| Вода | `GET/POST /water`, `DELETE /water/{id}`, `GET /water/summary` |
| Замеры | `GET/POST /measurements` |
| Статистика | `GET /stats/calories\|macros\|weight\|exercise/{id}\|workouts\|measurements\|water` |
| Debug | `GET /debug/health` |

## Docker контейнеры

| Сервис | Образ | Порт |
|--------|-------|------|
| web | python:3.12-slim + uv | 3000 |
| bot | python:3.12-slim + uv | — |
| db | postgres:16-alpine | 5432 |
| redis | redis:7-alpine | 6379 |

## Безопасность

- Telegram initData HMAC-SHA256 на каждый API запрос
- Rate limiting через Redis (100 req/min)
- Pydantic валидация всех входных данных
- Параметризованные SQL запросы (SQLAlchemy)
- Security headers (X-Content-Type-Options, X-XSS-Protection)
- Секреты через переменные окружения (SecretStr)
