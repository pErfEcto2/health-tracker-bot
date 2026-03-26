import logging

from aiogram import Router
from aiogram.filters import CommandStart
from aiogram.types import (
    InlineKeyboardButton,
    InlineKeyboardMarkup,
    KeyboardButton,
    Message,
    ReplyKeyboardMarkup,
    WebAppInfo,
)

from bot.config import MINIAPP_URL

logger = logging.getLogger(__name__)

router = Router()


@router.message(CommandStart())
async def cmd_start(message: Message) -> None:
    # WebApp buttons require HTTPS — fall back to plain text in dev
    if not MINIAPP_URL.startswith("https://"):
        logger.warning("MINIAPP_URL is not HTTPS (%s) — WebApp buttons disabled", MINIAPP_URL)
        await message.answer(
            "Привет! Я — TrackHub, твой трекер здоровья.\n\n"
            f"Приложение доступно по адресу: {MINIAPP_URL}\n\n"
            "⚠️ WebApp-кнопки недоступны в dev-режиме (нужен HTTPS).",
        )
        return

    webapp = WebAppInfo(url=MINIAPP_URL)

    reply_kb = ReplyKeyboardMarkup(
        keyboard=[[KeyboardButton(text="TrackHub", web_app=webapp)]],
        resize_keyboard=True,
    )
    await message.answer(
        "Привет! Я — TrackHub, твой трекер здоровья.\n\n"
        "Нажми кнопку на клавиатуре, чтобы открыть приложение:",
        reply_markup=reply_kb,
    )

    inline_kb = InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text="Открыть TrackHub", web_app=webapp)]
        ]
    )
    await message.answer(
        "Или используй кнопку под этим сообщением:",
        reply_markup=inline_kb,
    )
