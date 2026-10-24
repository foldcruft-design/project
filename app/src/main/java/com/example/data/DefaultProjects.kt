package com.example.data

import com.example.model.CodeFile
import com.example.model.Project
import java.util.UUID

object DefaultProjects {

    val telegramBotProject: Project by lazy {
        val projId = "proj_tg_bot"
        val mainFileId = "file_tg_main"
        val handlersFileId = "file_tg_handlers"
        val keyboardsFileId = "file_tg_keyboards"
        val configFileId = "file_tg_config"
        val reqFileId = "file_tg_requirements"
        val envFileId = "file_tg_env"

        val mainPy = CodeFile(
            id = mainFileId,
            projectId = projId,
            name = "main.py",
            isEntrypoint = true,
            content = """# ==========================================
# PocketCode: Telegram Bot Multi-File Runner
# ==========================================
import asyncio
from config import BOT_TOKEN, BOT_NAME, ADMINS
import handlers
import keyboards

print(f"🚀 Starting {BOT_NAME}...")
print(f"🔑 Loaded Token: {BOT_TOKEN[:10]}***")
print(f"🛡 Admin IDs: {ADMINS}")

# Register all modular command & message handlers
handlers.register_routes()

print("⚡ Bot polling started successfully!")
print("💡 Open 'Bot Tester' tab to send /start or click inline buttons.")
"""
        )

        val handlersPy = CodeFile(
            id = handlersFileId,
            projectId = projId,
            name = "handlers.py",
            isEntrypoint = false,
            content = """# ==========================================
# Handlers Module (Routes & User Commands)
# ==========================================
from config import BOT_NAME
import keyboards

def handle_start(user_id, username):
    print(f"👤 [Update] User @{username} (ID: {user_id}) sent /start")
    greeting = (
        f"👋 Привет, {username}!\n"
        f"Я бот {BOT_NAME}, написанный на PocketCode в несколько файлов.\n"
        f"Используй меню ниже для навигации:"
    )
    kb = keyboards.get_main_keyboard()
    return {"text": greeting, "keyboard": kb}

def handle_help(user_id):
    print(f"ℹ️ [Update] User {user_id} requested /help")
    return {
        "text": (
            "📖 Доступные команды:\n"
            "/start - Перезапуск и главное меню\n"
            "/help - Справка и документация\n"
            "/calc <выражение> - Быстрый калькулятор\n"
            "/status - Статус модулей бота"
        )
    }

def handle_callback(callback_data, user_id):
    print(f"🔘 [Update] User {user_id} pressed button: {callback_data}")
    if callback_data == "action_stats":
        return "📊 Статистика:\n• Запущенных файлов: 4\n• Время работы: 99.9%\n• Архитектура: Модульная"
    elif callback_data == "action_ping":
        return "🏓 Pong! Задержка сети: 42ms"
    return f"Вы выбрали: {callback_data}"

def register_routes():
    print("✅ Handlers registered: /start, /help, /status, callbacks")
"""
        )

        val keyboardsPy = CodeFile(
            id = keyboardsFileId,
            projectId = projId,
            name = "keyboards.py",
            isEntrypoint = false,
            content = """# ==========================================
# Keyboards Module (Inline & Reply Buttons)
# ==========================================

def get_main_keyboard():
    # Structured buttons for interactive simulator
    return [
        [
            {"text": "📊 Статистика бота", "callback_data": "action_stats"},
            {"text": "🏓 Пинг", "callback_data": "action_ping"}
        ],
        [
            {"text": "🛠 Помощь по коду", "callback_data": "action_help"},
            {"text": "⚙️ Настройки", "callback_data": "action_settings"}
        ]
    ]

def get_reply_keyboard():
    return [
        ["/start", "/help"],
        ["/status", "📞 Поддержка"]
    ]
"""
        )

        val configPy = CodeFile(
            id = configFileId,
            projectId = projId,
            name = "config.py",
            isEntrypoint = false,
            content = """# ==========================================
# Configuration & Environment Variables
# ==========================================
import os

# Telegram Bot Token (reads from .env file or default dev token)
BOT_TOKEN = os.getenv("BOT_TOKEN", "748920184:AAHq_m9vK8zY4X109j4k")
BOT_NAME = "PocketBot v2.0"
ADMINS = [12345678, 87654321]
DEBUG_MODE = True
"""
        )

        val requirementsTxt = CodeFile(
            id = reqFileId,
            projectId = projId,
            name = "requirements.txt",
            isEntrypoint = false,
            content = """aiogram==3.4.1
python-dotenv==1.0.1
aiohttp==3.9.3
pydantic==2.6.4
"""
        )

        val envFile = CodeFile(
            id = envFileId,
            projectId = projId,
            name = ".env",
            isEntrypoint = false,
            content = """# PocketCode Bot Secrets
BOT_TOKEN=748920184:AAHq_m9vK8zY4X109j4k
DEBUG=True
ENVIRONMENT=development
"""
        )

        val androidYmlFile = CodeFile(
            id = "file_tg_android_yml",
            projectId = projId,
            name = "android.yml",
            isEntrypoint = false,
            content = """name: Android CI - Build APK

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build:
    name: Build & Package APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build Debug APK with Gradle
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: PocketCode-Debug-APK
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 30
"""
        )

        Project(
            id = projId,
            name = "Telegram Bot Pro",
            description = "Модульный бот с разделением на config.py, handlers.py, keyboards.py",
            language = "python",
            activeFileId = mainFileId,
            files = listOf(mainPy, handlersPy, keyboardsPy, configPy, requirementsTxt, envFile, androidYmlFile),
            installedPackages = listOf("aiogram", "python-dotenv", "aiohttp", "pydantic")
        )
    }

    val fastApiProject: Project by lazy {
        val projId = "proj_fastapi"
        val mainFileId = "file_api_main"
        val routesFileId = "file_api_routes"
        val modelsFileId = "file_api_models"
        val reqFileId = "file_api_req"

        val mainPy = CodeFile(
            id = mainFileId,
            projectId = projId,
            name = "main.py",
            isEntrypoint = true,
            content = """# ==========================================
# FastAPI Microservice Multi-File
# ==========================================
import routes
import models

print("🚀 Starting FastAPI Server on http://127.0.0.1:8000")
print(f"📦 Registered {len(routes.ENDPOINTS)} endpoints")

# Simulated server health check
routes.dispatch_request("GET", "/health")
routes.dispatch_request("GET", "/api/items")
routes.dispatch_request("POST", "/api/items", {"name": "PocketCode Pro", "price": 0.0})
"""
        )

        val routesPy = CodeFile(
            id = routesFileId,
            projectId = projId,
            name = "routes.py",
            isEntrypoint = false,
            content = """# ==========================================
# API Routes Module
# ==========================================
import models

ENDPOINTS = ["/health", "/api/items", "/api/users"]

def dispatch_request(method, path, body=None):
    print(f"🌐 [{method}] {path}")
    if path == "/health":
        print("  ↪ 200 OK: {'status': 'healthy', 'uptime': '99.9%'}")
    elif path == "/api/items" and method == "GET":
        items = models.get_sample_items()
        print(f"  ↪ 200 OK: Returning {len(items)} items")
    elif path == "/api/items" and method == "POST":
        print(f"  ↪ 201 Created item: {body}")
"""
        )

        val modelsPy = CodeFile(
            id = modelsFileId,
            projectId = projId,
            name = "models.py",
            isEntrypoint = false,
            content = """# ==========================================
# Data Models & Schemas
# ==========================================

def get_sample_items():
    return [
        {"id": 1, "title": "Мобильный редактор", "tag": "dev"},
        {"id": 2, "title": "GitHub Actions CI", "tag": "ci/cd"},
        {"id": 3, "title": "Telegram Bot Engine", "tag": "bot"}
    ]
"""
        )

        val reqPy = CodeFile(
            id = reqFileId,
            projectId = projId,
            name = "requirements.txt",
            isEntrypoint = false,
            content = """fastapi==0.110.0
uvicorn==0.28.0
pydantic==2.6.4
"""
        )

        Project(
            id = projId,
            name = "FastAPI Service",
            description = "REST API с роутами, Pydantic моделями и uvicorn",
            language = "python",
            activeFileId = mainFileId,
            files = listOf(mainPy, routesPy, modelsPy, reqPy),
            installedPackages = listOf("fastapi", "uvicorn", "pydantic")
        )
    }

    val defaultProjects = listOf(telegramBotProject, fastApiProject)
}
