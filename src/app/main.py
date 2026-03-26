import logging
import time
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, Request, Response
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles

from app.api import api_router
from app.config import settings
from app.redis import get_redis

logger = logging.getLogger(__name__)

STATIC_DIR = Path(__file__).parent / "static"

# In-memory rate limit fallback when Redis is unavailable
_mem_rate: dict[str, tuple[int, float]] = {}
_mem_rate_last_cleanup: float = 0.0
_redis_warned = False


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting TrackHub API")
    yield
    logger.info("Shutting down TrackHub API")


app = FastAPI(
    title="TrackHub",
    version="0.1.0",
    lifespan=lifespan,
    docs_url="/docs" if settings.DEV_MODE else None,
    redoc_url="/redoc" if settings.DEV_MODE else None,
)

# CORS
_dev_origins = ["http://localhost:3000", "http://127.0.0.1:3000"] if settings.DEV_MODE else []
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://web.telegram.org"] + _dev_origins,
    allow_origin_regex=r"https://\w+\.telegram\.org",
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["Content-Type", "X-Telegram-Init-Data"],
)


# Request logging
@app.middleware("http")
async def request_logging(request: Request, call_next):
    start = time.time()
    response: Response = await call_next(request)
    duration_ms = round((time.time() - start) * 1000)
    logger.info(
        "%s %s %s %dms",
        request.method,
        request.url.path,
        response.status_code,
        duration_ms,
    )
    return response


# Security headers
@app.middleware("http")
async def security_headers(request: Request, call_next):
    response: Response = await call_next(request)
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
    response.headers["Strict-Transport-Security"] = (
        "max-age=31536000; includeSubDomains"
    )
    response.headers["Content-Security-Policy"] = (
        "default-src 'self'; script-src 'self' 'unsafe-inline'; "
        "style-src 'self' 'unsafe-inline'; connect-src 'self' https://*.telegram.org; "
        "img-src 'self' data:; frame-ancestors https://*.telegram.org"
    )
    return response


# Rate limiting middleware
def _check_mem_rate(key: str, limit: int, window: int) -> bool:
    """In-memory rate limit fallback (single-process only)."""
    global _mem_rate_last_cleanup
    now = time.time()

    # Periodic cleanup of expired entries to prevent memory leak
    if now - _mem_rate_last_cleanup > 60:
        _mem_rate_last_cleanup = now
        expired = [k for k, (_, exp) in _mem_rate.items() if now > exp]
        for k in expired:
            del _mem_rate[k]

    entry = _mem_rate.get(key)
    if entry is None or now > entry[1]:
        _mem_rate[key] = (1, now + window)
        return True
    if entry[0] >= limit:
        return False
    _mem_rate[key] = (entry[0] + 1, entry[1])
    return True


@app.middleware("http")
async def rate_limit(request: Request, call_next):
    if request.url.path.startswith("/api/"):
        ip = request.client.host if request.client else "unknown"
        key = f"rate:ip:{ip}"
        try:
            redis = get_redis()
            count = await redis.incr(key)
            if count == 1:
                await redis.expire(key, 60)
            if count > 100:
                return Response(
                    content='{"detail":"Rate limit exceeded"}',
                    status_code=429,
                    media_type="application/json",
                )
        except Exception:
            global _redis_warned
            if not _redis_warned:
                logger.warning("Redis unavailable — using in-memory rate limiter")
                _redis_warned = True
            if not _check_mem_rate(key, 100, 60):
                return Response(
                    content='{"detail":"Rate limit exceeded"}',
                    status_code=429,
                    media_type="application/json",
                )
    return await call_next(request)


# Validation error handler — return readable messages
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = []
    for err in exc.errors():
        field = " → ".join(str(loc) for loc in err["loc"] if loc != "body")
        errors.append(f"{field}: {err['msg']}" if field else err["msg"])
    return JSONResponse(
        status_code=422,
        content={"detail": "; ".join(errors)},
    )


# API routes
app.include_router(api_router)

# Static files (Mini App frontend)
app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")


# Serve index.html at root
@app.get("/")
async def serve_index():
    index_path = STATIC_DIR / "index.html"
    if not index_path.is_file():
        return Response(content="Not found", status_code=404)
    return Response(
        content=index_path.read_text(encoding="utf-8"),
        media_type="text/html",
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.HTTP_LISTEN_HOST,
        port=settings.HTTP_LISTEN_PORT,
        reload=True,
    )
