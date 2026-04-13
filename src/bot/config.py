from app.config import settings

BOT_TOKEN = settings.TELEGRAM_BOT_TOKEN.get_secret_value()
WEBSITE_URL = settings.MINIAPP_URL
