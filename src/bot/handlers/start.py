import logging

from aiogram import Router
from aiogram.filters import CommandStart
from aiogram.types import Message

from bot.config import WEBSITE_URL

logger = logging.getLogger(__name__)

router = Router()


@router.message(CommandStart())
async def cmd_start(message: Message) -> None:
    await message.answer(
        "Привет! TrackHub теперь — обычный сайт.\n\n"
        f"Открой {WEBSITE_URL} и войди с логином и паролем, которые тебе прислали.\n\n"
        "Если пароля ещё нет — напиши хозяину, он создаст аккаунт."
    )
