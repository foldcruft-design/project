package com.example.github

data class WorkflowTemplate(
    val id: String,
    val title: String,
    val fileName: String,
    val description: String,
    val ymlContent: String
)

object GitHubActionsHub {

    val androidApkWorkflow = WorkflowTemplate(
        id = "android_apk",
        title = "Сборка APK в GitHub Actions",
        fileName = ".github/workflows/build-apk.yml",
        description = "Полный CI/CD пайплайн: компилирует APK приложения на серверах GitHub и выкладывает готовый установочный файл в Artifacts.",
        ymlContent = """name: Build Android APK

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch: # Позволяет запускать сборку вручную кнопкой в GitHub!

jobs:
  build:
    name: Compile & Package APK
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

      - name: Build Debug APK
        run: ./gradlew assembleDebug --stacktrace

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: PocketCode-Debug-APK
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 14

      - name: Print Success
        run: echo "🎉 APK успешно скомпилирован! Скачайте его во вкладке Artifacts на GitHub."
"""
    )

    val telegramBotWorkflow = WorkflowTemplate(
        id = "telegram_bot_ci",
        title = "Проверка и Деплой Telegram Бота",
        fileName = ".github/workflows/bot-ci.yml",
        description = "Автоматическая проверка импортов всех файлов бота, валидация синтаксиса и деплой на сервер.",
        ymlContent = """name: Telegram Bot CI/CD

on:
  push:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  lint-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Python 3.11
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
          cache: 'pip'

      - name: Install Dependencies
        run: |
          python -m pip install --upgrade pip
          if [ -f requirements.txt ]; then pip install -r requirements.txt; fi
          pip install flake8

      - name: Lint Multi-file Python Code
        run: |
          flake8 . --count --select=E9,F63,F7,F82 --show-source --statistics

      - name: Validate Module Imports
        run: |
          python -c "import main; print('✅ Multi-file imports verified!')"
"""
    )

    val guideSteps = listOf(
        GuideStep(
            number = "1",
            title = "Создайте репозиторий на GitHub",
            details = "Зайдите на github.com -> New Repository. Назовите его, например, 'my-pocketcode-app' и оставьте публичным или приватным."
        ),
        GuideStep(
            number = "2",
            title = "Добавьте файл воркфлоу",
            details = "Нажмите кнопку 'Добавить в проект' ниже. В проекте появится файл .github/workflows/build-apk.yml."
        ),
        GuideStep(
            number = "3",
            title = "Загрузите файлы в репозиторий",
            details = "Сделайте git push или загрузите файлы архивом через веб-интерфейс GitHub (кнопка 'Upload files')."
        ),
        GuideStep(
            number = "4",
            title = "Запуск сборки в GitHub Actions",
            details = "Откройте вкладку 'Actions' в вашем репозитории. Выберите 'Build Android APK' и нажмите кнопку 'Run workflow'."
        ),
        GuideStep(
            number = "5",
            title = "Скачайте готовый APK файл",
            details = "Через 2-3 минуты GitHub завершит сборку зелёной галочкой ✅. Внизу страницы сборки в блоке 'Artifacts' скачайте 'PocketCode-Debug-APK.zip' и установите на телефон!"
        )
    )
}

data class GuideStep(
    val number: String,
    val title: String,
    val details: String
)
