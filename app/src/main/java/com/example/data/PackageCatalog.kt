package com.example.data

import com.example.model.PackageItem

object PackageCatalog {
    val curatedPackages = listOf(
        PackageItem(
            name = "aiogram",
            version = "3.4.1",
            description = "Современный и быстрый асинхронный фреймворк для Telegram Bot API (Python 3.8+).",
            category = "Telegram & Bots",
            importSnippet = "from aiogram import Bot, Dispatcher, types\nfrom aiogram.filters import Command",
            docsUrl = "https://docs.aiogram.dev"
        ),
        PackageItem(
            name = "python-telegram-bot",
            version = "20.8",
            description = "Популярная и стабильная библиотека для создания ботов Telegram с поддержкой ptb-coroutine.",
            category = "Telegram & Bots",
            importSnippet = "from telegram.ext import ApplicationBuilder, CommandHandler",
            docsUrl = "https://python-telegram-bot.org"
        ),
        PackageItem(
            name = "python-dotenv",
            version = "1.0.1",
            description = "Загрузка переменных окружения из файла .env для безопасного хранения токенов.",
            category = "Config & Security",
            importSnippet = "from dotenv import load_dotenv\nimport os\nload_dotenv()",
            docsUrl = "https://github.com/theskumar/python-dotenv"
        ),
        PackageItem(
            name = "requests",
            version = "2.31.0",
            description = "Простая и элегантная HTTP-библиотека для отправки GET/POST запросов к внешним API.",
            category = "Web & API",
            importSnippet = "import requests\nresponse = requests.get('https://api.github.com')",
            docsUrl = "https://requests.readthedocs.io"
        ),
        PackageItem(
            name = "aiohttp",
            version = "3.9.3",
            description = "Асинхронный HTTP клиент и сервер на базе asyncio для высокой скорости работы бота.",
            category = "Web & API",
            importSnippet = "import aiohttp\nasync with aiohttp.ClientSession() as session: ...",
            docsUrl = "https://docs.aiohttp.org"
        ),
        PackageItem(
            name = "fastapi",
            version = "0.110.0",
            description = "Сверхбыстрый фреймворк для создания REST API и микросервисов с автоматической Swagger документацией.",
            category = "Web & API",
            importSnippet = "from fastapi import FastAPI\napp = FastAPI()",
            docsUrl = "https://fastapi.tiangolo.com"
        ),
        PackageItem(
            name = "uvicorn",
            version = "0.28.0",
            description = "Молниеносный ASGI веб-сервер для запуска приложений на FastAPI и Starlette.",
            category = "Web & API",
            importSnippet = "import uvicorn\nuvicorn.run('main:app', host='127.0.0.1', port=8000)",
            docsUrl = "https://www.uvicorn.org"
        ),
        PackageItem(
            name = "pydantic",
            version = "2.6.4",
            description = "Валидация данных и управление настройками с помощью аннотаций типов Python.",
            category = "Data & Typing",
            importSnippet = "from pydantic import BaseModel\nclass User(BaseModel): id: int",
            docsUrl = "https://docs.pydantic.dev"
        ),
        PackageItem(
            name = "colorama",
            version = "0.4.6",
            description = "Цветной вывод и стилизация текста в консоли и терминале.",
            category = "CLI & Styling",
            importSnippet = "from colorama import Fore, Style\nprint(Fore.GREEN + 'Готово!' + Style.RESET_ALL)",
            docsUrl = "https://pypi.org/project/colorama"
        ),
        PackageItem(
            name = "beautifulsoup4",
            version = "4.12.3",
            description = "Парсер HTML и XML для парсинга сайтов и сбора данных в боте.",
            category = "Data & Typing",
            importSnippet = "from bs4 import BeautifulSoup\nsoup = BeautifulSoup(html, 'html.parser')",
            docsUrl = "https://www.crummy.com/software/BeautifulSoup"
        ),
        PackageItem(
            name = "peewee",
            version = "3.17.1",
            description = "Легковесная и простая ORM база данных (SQLite, Postgres) для хранения пользователей бота.",
            category = "Database",
            importSnippet = "from peewee import SqliteDatabase, Model, CharField\ndb = SqliteDatabase('bot.db')",
            docsUrl = "https://docs.peewee-orm.com"
        )
    )
}
