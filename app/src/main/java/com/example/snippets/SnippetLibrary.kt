package com.example.snippets

data class SnippetCard(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val targetFileSuggestion: String,
    val code: String
)

object SnippetLibrary {
    val cards = listOf(
        SnippetCard(
            id = "tg_command_start",
            title = "Команда /start и /help",
            description = "Обработчик стартового приветствия с клавиатурой и форматированным текстом.",
            category = "Telegram Handlers",
            targetFileSuggestion = "handlers.py",
            code = """@dp.message(Command("start"))
async def cmd_start(message: types.Message):
    user_name = message.from_user.first_name
    await message.answer(
        f"👋 Привет, {user_name}!\nДобро пожаловать в нашего модульного бота.",
        reply_markup=get_main_keyboard()
    )
"""
        ),
        SnippetCard(
            id = "tg_inline_keyboard",
            title = "Инлайн клавиатура (Кнопки под сообщением)",
            description = "Генератор удобных кликабельных кнопок с callback_data.",
            category = "Keyboards",
            targetFileSuggestion = "keyboards.py",
            code = """def get_main_keyboard():
    return InlineKeyboardMarkup(inline_keyboard=[
        [
            InlineKeyboardButton(text="⚡ Запустить анализ", callback_data="btn_run"),
            InlineKeyboardButton(text="📊 Статистика", callback_data="btn_stats")
        ],
        [
            InlineKeyboardButton(text="💬 Написать админу", url="https://t.me/admin")
        ]
    ])
"""
        ),
        SnippetCard(
            id = "tg_reply_keyboard",
            title = "Reply клавиатура (Кнопки внизу экрана)",
            description = "Фиксированные кнопки быстрого доступа в нижней панели чата.",
            category = "Keyboards",
            targetFileSuggestion = "keyboards.py",
            code = """def get_reply_menu():
    return ReplyKeyboardMarkup(
        keyboard=[
            [KeyboardButton(text="📱 Главное меню"), KeyboardButton(text="⚙️ Настройки")],
            [KeyboardButton(text="ℹ️ О боте"), KeyboardButton(text="📞 Поддержка")]
        ],
        resize_keyboard=True,
        one_time_keyboard=False
    )
"""
        ),
        SnippetCard(
            id = "tg_callback_handler",
            title = "Обработчик нажатия кнопок",
            description = "Перехватывает клики по inline кнопкам и обновляет текст сообщения.",
            category = "Telegram Handlers",
            targetFileSuggestion = "handlers.py",
            code = """@dp.callback_query(lambda c: c.data.startswith("btn_"))
async def process_callback(callback: types.CallbackQuery):
    action = callback.data.split("_")[1]
    if action == "stats":
        await callback.message.edit_text("📈 Статистика: 1,420 запросов обработано!")
    await callback.answer() # Убирает часики загрузки
"""
        ),
        SnippetCard(
            id = "tg_admin_guard",
            title = "Проверка на Администратора",
            description = "Защита секретных команд бота от обычных пользователей.",
            category = "Security",
            targetFileSuggestion = "handlers.py",
            code = """from config import ADMINS

def is_admin(user_id: int) -> bool:
    return user_id in ADMINS

@dp.message(Command("admin"))
async def cmd_admin(message: types.Message):
    if not is_admin(message.from_user.id):
        await message.reply("⛔ Доступ запрещён! Вы не администратор.")
        return
    await message.reply("👑 Панель управления ботом: всё работает штатно.")
"""
        ),
        SnippetCard(
            id = "db_sqlite_helper",
            title = "SQLite База Данных пользователей",
            description = "Сохранение пользователей, даты входа и подсчёт зарегистрированных.",
            category = "Database",
            targetFileSuggestion = "database.py",
            code = """import sqlite3

def init_db():
    with sqlite3.connect("bot_users.db") as conn:
        conn.execute(\"\"\"
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY,
                username TEXT,
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        \"\"\")

def add_user(user_id: int, username: str):
    with sqlite3.connect("bot_users.db") as conn:
        conn.execute("INSERT OR IGNORE INTO users (id, username) VALUES (?, ?)", (user_id, username))
"""
        ),
        SnippetCard(
            id = "config_env_loader",
            title = "Загрузка .env переменных",
            description = "Безопасное чтение токенов и параметров из файла окружения.",
            category = "Config",
            targetFileSuggestion = "config.py",
            code = """import os
from dotenv import load_dotenv

load_dotenv()

BOT_TOKEN = os.getenv("BOT_TOKEN")
if not BOT_TOKEN:
    raise ValueError("❌ Токен бота не найден в файле .env!")

ADMIN_ID = int(os.getenv("ADMIN_ID", "0"))
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///bot.db")
"""
        ),
        SnippetCard(
            id = "fastapi_route_card",
            title = "FastAPI REST Endpoint",
            description = "Асинхронный эндпоинт с JSON ответом и валидацией параметров.",
            category = "FastAPI & Web",
            targetFileSuggestion = "routes.py",
            code = """from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

router = APIRouter()

class BotStatus(BaseModel):
    is_active: bool
    version: str
    active_users: int

@router.get("/status", response_model=BotStatus)
async def get_status():
    return BotStatus(is_active=True, version="2.0", active_users=350)
"""
        )
    )
}
