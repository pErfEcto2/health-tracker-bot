import logging
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, Request, Response, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.crypto import (
    generate_csrf_token,
    generate_session_token,
    hash_auth_key,
    hash_session_token,
    verify_auth_key,
)
from app.database import get_db
from app.dependencies import (
    CSRF_COOKIE,
    SESSION_COOKIE,
    SESSION_TTL_DAYS,
    get_current_user_allow_must_change,
    rate_limit_endpoint,
)
from app.models.session import Session as DbSession
from app.models.user import User
from app.schemas.auth import (
    ChangePasswordRequest,
    LoginRequest,
    LoginResponse,
    MeResponse,
    RecoverCompleteRequest,
    RecoverStartRequest,
    RecoverStartResponse,
    SaltRequest,
    SaltResponse,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/auth", tags=["auth"])


def _set_auth_cookies(
    response: Response, session_token: str, csrf_token: str, secure: bool
) -> None:
    max_age = SESSION_TTL_DAYS * 86400
    response.set_cookie(
        key=SESSION_COOKIE,
        value=session_token,
        max_age=max_age,
        httponly=True,
        secure=secure,
        samesite="strict",
        path="/",
    )
    response.set_cookie(
        key=CSRF_COOKIE,
        value=csrf_token,
        max_age=max_age,
        httponly=False,
        secure=secure,
        samesite="strict",
        path="/",
    )


def _clear_auth_cookies(response: Response) -> None:
    response.delete_cookie(SESSION_COOKIE, path="/")
    response.delete_cookie(CSRF_COOKIE, path="/")


async def _issue_session(
    db: AsyncSession, user_id: int, response: Response, request: Request
) -> None:
    token, token_hash = generate_session_token()
    csrf = generate_csrf_token()
    sess = DbSession(
        user_id=user_id,
        token_hash=token_hash,
        expires_at=datetime.now(timezone.utc) + timedelta(days=SESSION_TTL_DAYS),
    )
    db.add(sess)
    await db.commit()
    _set_auth_cookies(response, token, csrf, secure=request.url.scheme == "https")


@router.post(
    "/salt",
    response_model=SaltResponse,
    dependencies=[Depends(rate_limit_endpoint(limit=20, window=60))],
)
async def get_salt(body: SaltRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(User).where(User.username == body.username.lower())
    )
    user = result.scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return SaltResponse(
        salt_hex=user.salt.hex(),
        must_change_password=user.must_change_password,
    )


@router.post(
    "/login",
    response_model=LoginResponse,
    dependencies=[Depends(rate_limit_endpoint(limit=5, window=60))],
)
async def login(
    body: LoginRequest,
    request: Request,
    response: Response,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(User).where(User.username == body.username.lower())
    )
    user = result.scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    if not verify_auth_key(user.auth_hash, body.auth_key_hex):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    await _issue_session(db, user.id, response, request)

    wrapped_hex = (
        user.wrapped_dek_password.hex()
        if user.wrapped_dek_password is not None
        else None
    )
    return LoginResponse(
        must_change_password=user.must_change_password,
        wrapped_dek_password_hex=wrapped_hex,
    )


@router.delete("/account", status_code=status.HTTP_204_NO_CONTENT)
async def delete_account(
    response: Response,
    user: User = Depends(get_current_user_allow_must_change),
    db: AsyncSession = Depends(get_db),
):
    """Delete the authenticated user and all their data (records + sessions cascade)."""
    await db.delete(user)
    await db.commit()
    _clear_auth_cookies(response)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
async def logout(
    request: Request,
    response: Response,
    db: AsyncSession = Depends(get_db),
):
    token = request.cookies.get(SESSION_COOKIE)
    if token:
        th = hash_session_token(token)
        result = await db.execute(select(DbSession).where(DbSession.token_hash == th))
        sess = result.scalar_one_or_none()
        if sess is not None:
            await db.delete(sess)
            await db.commit()
    _clear_auth_cookies(response)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get("/me", response_model=MeResponse)
async def me(user: User = Depends(get_current_user_allow_must_change)):
    return MeResponse(
        username=user.username,
        must_change_password=user.must_change_password,
    )


@router.post("/change-password", status_code=status.HTTP_204_NO_CONTENT)
async def change_password(
    body: ChangePasswordRequest,
    user: User = Depends(get_current_user_allow_must_change),
    db: AsyncSession = Depends(get_db),
):
    try:
        new_salt = bytes.fromhex(body.new_salt_hex)
        wrapped_password = bytes.fromhex(body.wrapped_dek_password_hex)
    except ValueError as e:
        raise HTTPException(status_code=400, detail="Invalid hex encoding") from e

    user.salt = new_salt
    user.auth_hash = hash_auth_key(body.new_auth_key_hex)
    user.wrapped_dek_password = wrapped_password

    if user.must_change_password:
        if not body.wrapped_dek_recovery_hex or not body.recovery_auth_key_hex:
            raise HTTPException(
                status_code=400,
                detail="Recovery key material required on first password set",
            )
        try:
            wrapped_recovery = bytes.fromhex(body.wrapped_dek_recovery_hex)
        except ValueError as e:
            raise HTTPException(status_code=400, detail="Invalid hex encoding") from e
        user.wrapped_dek_recovery = wrapped_recovery
        user.recovery_auth_hash = hash_auth_key(body.recovery_auth_key_hex)
        user.must_change_password = False

    await db.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post(
    "/recover-start",
    response_model=RecoverStartResponse,
    dependencies=[Depends(rate_limit_endpoint(limit=3, window=3600))],
)
async def recover_start(body: RecoverStartRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(User).where(User.username == body.username.lower())
    )
    user = result.scalar_one_or_none()
    if user is None or user.wrapped_dek_recovery is None:
        raise HTTPException(status_code=404, detail="Recovery not available")
    return RecoverStartResponse(wrapped_dek_recovery_hex=user.wrapped_dek_recovery.hex())


@router.post(
    "/recover-complete",
    response_model=LoginResponse,
    dependencies=[Depends(rate_limit_endpoint(limit=3, window=3600))],
)
async def recover_complete(
    body: RecoverCompleteRequest,
    request: Request,
    response: Response,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(User).where(User.username == body.username.lower())
    )
    user = result.scalar_one_or_none()
    if user is None or user.recovery_auth_hash is None:
        raise HTTPException(status_code=404, detail="Recovery not available")

    if not verify_auth_key(user.recovery_auth_hash, body.recovery_auth_key_hex):
        raise HTTPException(status_code=401, detail="Invalid recovery key")

    try:
        new_salt = bytes.fromhex(body.new_salt_hex)
        wrapped_password = bytes.fromhex(body.wrapped_dek_password_hex)
    except ValueError as e:
        raise HTTPException(status_code=400, detail="Invalid hex encoding") from e

    user.salt = new_salt
    user.auth_hash = hash_auth_key(body.new_auth_key_hex)
    user.wrapped_dek_password = wrapped_password
    user.must_change_password = False

    await _issue_session(db, user.id, response, request)
    return LoginResponse(
        must_change_password=False,
        wrapped_dek_password_hex=wrapped_password.hex(),
    )
