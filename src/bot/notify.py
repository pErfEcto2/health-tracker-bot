"""Notification helper — send plain-text messages to a user by telegram_chat_id.

Usage:
    from bot.notify import send_notification
    await send_notification(chat_id=user.telegram_chat_id, text="Не забудь зафиксировать обед")

Never include user data — notifications are plaintext and Telegram servers see them.
"""

import logging
from contextlib import asynccontextmanager
from typing import AsyncIterator

from aiogram import Bot

from bot.config import BOT_TOKEN

logger = logging.getLogger(__name__)


@asynccontextmanager
async def _bot() -> AsyncIterator[Bot]:
    bot = Bot(token=BOT_TOKEN)
    try:
        yield bot
    finally:
        await bot.session.close()


async def send_notification(chat_id: int, text: str) -> bool:
    """Send a plain-text message to the given chat. Returns True on success."""
    try:
        async with _bot() as bot:
            await bot.send_message(chat_id=chat_id, text=text)
        return True
    except Exception:
        logger.warning("failed to deliver notification to %s", chat_id, exc_info=True)
        return False
