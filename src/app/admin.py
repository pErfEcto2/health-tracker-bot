"""Admin CLI for managing users. Run inside web_server container:

  docker compose exec web python -m app.admin create-user alice
  docker compose exec web python -m app.admin list-users
  docker compose exec web python -m app.admin reset-password alice
  docker compose exec web python -m app.admin delete-user alice
"""

import argparse
import asyncio
import sys

from sqlalchemy import delete, select

from app.crypto import (
    derive_auth_key_hex,
    generate_salt,
    generate_temp_password,
    hash_auth_key,
)
from app.database import async_session
from app.models.record import Record
from app.models.session import Session
from app.models.user import User


async def _cmd_create_user(username: str) -> int:
    username = username.lower()
    async with async_session() as db:
        existing = await db.execute(select(User).where(User.username == username))
        if existing.scalar_one_or_none() is not None:
            print(f"error: user '{username}' already exists", file=sys.stderr)
            return 1

        temp_password = generate_temp_password(16)
        salt = generate_salt()
        auth_key_hex = derive_auth_key_hex(temp_password, salt)
        db.add(
            User(
                username=username,
                salt=salt,
                auth_hash=hash_auth_key(auth_key_hex),
                must_change_password=True,
            )
        )
        await db.commit()

    print(f"created user: {username}")
    print(f"temp password: {temp_password}")
    print("share both via Telegram DM. user must change password on first login.")
    return 0


async def _cmd_list_users() -> int:
    async with async_session() as db:
        result = await db.execute(select(User).order_by(User.username))
        users = result.scalars().all()
    if not users:
        print("no users")
        return 0
    print(f"{'username':<20} {'id':<6} {'must_change':<12} {'has_dek':<8} {'created_at'}")
    for u in users:
        has_dek = "yes" if u.wrapped_dek_password is not None else "no"
        mc = "yes" if u.must_change_password else "no"
        print(f"{u.username:<20} {u.id:<6} {mc:<12} {has_dek:<8} {u.created_at.isoformat()}")
    return 0


async def _cmd_delete_user(username: str, yes: bool) -> int:
    username = username.lower()
    if not yes:
        ok = input(f"DELETE user '{username}' and ALL their data? [y/N]: ").strip().lower()
        if ok != "y":
            print("aborted")
            return 1
    async with async_session() as db:
        result = await db.execute(select(User).where(User.username == username))
        user = result.scalar_one_or_none()
        if user is None:
            print(f"error: user '{username}' not found", file=sys.stderr)
            return 1
        await db.delete(user)
        await db.commit()
    print(f"deleted user: {username}")
    return 0


async def _cmd_reset_password(username: str, yes: bool) -> int:
    username = username.lower()
    if not yes:
        print(f"WARNING: resetting password wipes wrapped DEK — user's data becomes")
        print(f"unreadable permanently.")
        ok = input(f"proceed with reset for '{username}'? [y/N]: ").strip().lower()
        if ok != "y":
            print("aborted")
            return 1
    async with async_session() as db:
        result = await db.execute(select(User).where(User.username == username))
        user = result.scalar_one_or_none()
        if user is None:
            print(f"error: user '{username}' not found", file=sys.stderr)
            return 1
        temp_password = generate_temp_password(16)
        user.salt = generate_salt()
        user.auth_hash = hash_auth_key(derive_auth_key_hex(temp_password, user.salt))
        user.wrapped_dek_password = None
        user.wrapped_dek_recovery = None
        user.recovery_auth_hash = None
        user.must_change_password = True
        # Invalidate all sessions for this user
        await db.execute(delete(Session).where(Session.user_id == user.id))
        # Wipe their encrypted records — unreadable anyway
        await db.execute(delete(Record).where(Record.user_id == user.id))
        await db.commit()

    print(f"reset user: {username}")
    print(f"new temp password: {temp_password}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(prog="app.admin", description="TrackHub admin CLI")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p = sub.add_parser("create-user")
    p.add_argument("username")

    sub.add_parser("list-users")

    p = sub.add_parser("delete-user")
    p.add_argument("username")
    p.add_argument("-y", "--yes", action="store_true")

    p = sub.add_parser("reset-password")
    p.add_argument("username")
    p.add_argument("-y", "--yes", action="store_true")

    args = parser.parse_args()

    if args.cmd == "create-user":
        return asyncio.run(_cmd_create_user(args.username))
    if args.cmd == "list-users":
        return asyncio.run(_cmd_list_users())
    if args.cmd == "delete-user":
        return asyncio.run(_cmd_delete_user(args.username, args.yes))
    if args.cmd == "reset-password":
        return asyncio.run(_cmd_reset_password(args.username, args.yes))
    return 1


if __name__ == "__main__":
    sys.exit(main())
