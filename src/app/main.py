import logging
import time
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, Request, Response
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles

from app.api import api_router
from app.config import settings
from app.redis import get_redis

logger = logging.getLogger(__name__)

STATIC_DIR = Path(__file__).parent / "static"
DIST_DIR = STATIC_DIR / "dist"

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
    version="0.2.0",
    lifespan=lifespan,
    docs_url="/docs" if settings.DEV_MODE else None,
    redoc_url="/redoc" if settings.DEV_MODE else None,
)


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


@app.middleware("http")
async def security_headers(request: Request, call_next):
    response: Response = await call_next(request)
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
    response.headers["Strict-Transport-Security"] = (
        "max-age=31536000; includeSubDomains; preload"
    )
    response.headers["Permissions-Policy"] = (
        "accelerometer=(), camera=(), geolocation=(), gyroscope=(), "
        "magnetometer=(), microphone=(), payment=(), usb=()"
    )
    response.headers["Content-Security-Policy"] = (
        "default-src 'self'; "
        "script-src 'self'; "
        "style-src 'self'; "
        "connect-src 'self'; "
        "img-src 'self' data:; "
        "font-src 'self'; "
        "frame-ancestors 'none'; "
        "base-uri 'none'; "
        "form-action 'self'; "
        "object-src 'none'"
    )
    return response


def _check_mem_rate(key: str, limit: int, window: int) -> bool:
    global _mem_rate_last_cleanup
    now = time.time()

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


app.include_router(api_router)

# Serve Vite-built frontend from dist/
if DIST_DIR.is_dir():
    app.mount("/assets", StaticFiles(directory=str(DIST_DIR / "assets")), name="assets")


@app.get("/{full_path:path}")
async def serve_spa(full_path: str):
    """SPA fallback: serve dist/index.html for any non-API path."""
    if full_path.startswith("api/") or full_path.startswith("assets/"):
        return Response(status_code=404)
    index_path = DIST_DIR / "index.html"
    if not index_path.is_file():
        return Response(
            content=(
                "Frontend not built. Run `npm ci && npm run build` in "
                "src/app/static/."
            ),
            status_code=503,
            media_type="text/plain",
        )
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
