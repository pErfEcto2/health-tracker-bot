import logging
import uuid
from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.record import Record
from app.models.user import User
from app.schemas.record import (
    ALLOWED_TYPES,
    RecordCreate,
    RecordResponse,
    RecordUpdate,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/records", tags=["records"])


def _to_response(r: Record) -> RecordResponse:
    return RecordResponse(
        id=r.id,
        type=r.type,
        record_date=r.record_date,
        nonce_hex=r.nonce.hex(),
        ciphertext_hex=r.ciphertext.hex(),
        created_at=r.created_at,
        updated_at=r.updated_at,
    )


def _decode_hex(nonce_hex: str, ciphertext_hex: str) -> tuple[bytes, bytes]:
    try:
        return bytes.fromhex(nonce_hex), bytes.fromhex(ciphertext_hex)
    except ValueError as e:
        raise HTTPException(status_code=400, detail="Invalid hex encoding") from e


@router.get("", response_model=list[RecordResponse])
async def list_records(
    type: str | None = Query(default=None, max_length=32),
    date_from: date | None = Query(default=None, alias="from"),
    date_to: date | None = Query(default=None, alias="to"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    stmt = select(Record).where(Record.user_id == user.id)
    if type is not None:
        if type not in ALLOWED_TYPES:
            raise HTTPException(status_code=400, detail="Unknown record type")
        stmt = stmt.where(Record.type == type)
    if date_from is not None:
        stmt = stmt.where(Record.record_date >= date_from)
    if date_to is not None:
        stmt = stmt.where(Record.record_date <= date_to)
    stmt = stmt.order_by(Record.record_date.desc(), Record.created_at.desc())
    result = await db.execute(stmt)
    return [_to_response(r) for r in result.scalars().all()]


@router.get("/{record_id}", response_model=RecordResponse)
async def get_record(
    record_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    r = await db.get(Record, record_id)
    if r is None or r.user_id != user.id:
        raise HTTPException(status_code=404, detail="Record not found")
    return _to_response(r)


@router.post("", response_model=RecordResponse, status_code=status.HTTP_201_CREATED)
async def create_record(
    body: RecordCreate,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if body.type not in ALLOWED_TYPES:
        raise HTTPException(status_code=400, detail="Unknown record type")
    nonce, ciphertext = _decode_hex(body.nonce_hex, body.ciphertext_hex)
    r = Record(
        user_id=user.id,
        type=body.type,
        record_date=body.record_date,
        nonce=nonce,
        ciphertext=ciphertext,
    )
    db.add(r)
    await db.commit()
    await db.refresh(r)
    return _to_response(r)


@router.put("/{record_id}", response_model=RecordResponse)
async def update_record(
    record_id: uuid.UUID,
    body: RecordUpdate,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    r = await db.get(Record, record_id)
    if r is None or r.user_id != user.id:
        raise HTTPException(status_code=404, detail="Record not found")
    nonce, ciphertext = _decode_hex(body.nonce_hex, body.ciphertext_hex)
    r.nonce = nonce
    r.ciphertext = ciphertext
    if body.record_date is not None:
        r.record_date = body.record_date
    await db.commit()
    await db.refresh(r)
    return _to_response(r)


@router.delete("/{record_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_record(
    record_id: uuid.UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    r = await db.get(Record, record_id)
    if r is None or r.user_id != user.id:
        raise HTTPException(status_code=404, detail="Record not found")
    await db.delete(r)
    await db.commit()
