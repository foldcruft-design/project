package com.example.engine

import com.example.model.CodeFile
import com.example.model.ConsoleEntry
import com.example.model.LogType
import com.example.model.Project
import kotlinx.coroutines.delay

data class BotMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "bot"
    val text: String,
    val inlineButtons: List<List<InlineButton>> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class InlineButton(
    val text: String,
    val callbackData: String
)

class TelegramBotSimulator(
    private val project: Project
) {
    private val _messages = mutableListOf<BotMessage>()
    val messages: List<BotMessage> get() = _messages.toList()

    private var botName: String = "PocketBot"

    init {
        // Dynamic extraction from config.py
        val configFile = project.files.find { it.name == "config.py" }
        configFile?.content?.lines()?.forEach { line ->
            if (line.contains("BOT_NAME") && line.contains("=")) {
                val namePart = line.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                if (namePart.isNotEmpty()) {
                    botName = namePart
                }
            }
        }

        // Welcome message with dynamic multi-file status
        _messages.add(
            BotMessage(
                sender = "bot",
                text = "🤖 Симулятор Telegram Бота запущен!\nБот: $botName\nПодключено модулей: ${project.files.size} файлов\n\nОтправьте команду /start или выберите действие ниже:",
                inlineButtons = getDynamicButtons()
            )
        )
    }

    private fun getDynamicButtons(): List<List<InlineButton>> {
        // Try extracting buttons from keyboards.py if user edited it
        val keyboardsFile = project.files.find { it.name == "keyboards.py" }
        if (keyboardsFile != null && keyboardsFile.content.isNotEmpty()) {
            val extracted = mutableListOf<List<InlineButton>>()
            val currentRow = mutableListOf<InlineButton>()

            keyboardsFile.content.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.contains("text") && trimmed.contains("callback_data")) {
                    val textPart = trimmed.substringAfter("text").substringAfter(":").substringBefore(",").trim().removeSurrounding("\"").removeSurrounding("'")
                    val cbPart = trimmed.substringAfter("callback_data").substringAfter(":").substringBefore("}").trim().removeSurrounding("\"").removeSurrounding("'")
                    if (textPart.isNotEmpty() && cbPart.isNotEmpty()) {
                        currentRow.add(InlineButton(textPart, cbPart))
                        if (currentRow.size >= 2) {
                            extracted.add(currentRow.toList())
                            currentRow.clear()
                        }
                    }
                }
            }
            if (currentRow.isNotEmpty()) extracted.add(currentRow.toList())
            if (extracted.isNotEmpty()) return extracted
        }

        // Fallback default buttons
        return listOf(
            listOf(
                InlineButton("📊 Статистика", "action_stats"),
                InlineButton("🏓 Пинг", "action_ping")
            ),
            listOf(
                InlineButton("🛠 Помощь", "action_help"),
                InlineButton("⚙️ Настройки", "action_settings")
            )
        )
    }

    suspend fun handleUserMessage(userText: String, onLog: (ConsoleEntry) -> Unit): BotMessage {
        onLog(ConsoleEntry(type = LogType.BOT_INCOMING, text = "Telegram API ➔ [Incoming Message] from User: '$userText'"))
        delay(200)

        val handlersFile = project.files.find { it.name == "handlers.py" }
        val handlersContent = handlersFile?.content ?: ""

        val botReplyText: String
        var inlineKeyboard: List<List<InlineButton>> = emptyList()

        val trimmed = userText.trim()
        when {
            trimmed.startsWith("/start") -> {
                // Check if user has custom greeting in handlers.py
                val customGreeting = extractCustomString(handlersContent, "greeting")
                botReplyText = if (customGreeting.isNotEmpty() && customGreeting.length > 5 && !customGreeting.startsWith("(")) {
                    customGreeting.replace("{BOT_NAME}", botName).replace("{username}", "User")
                } else {
                    "👋 Привет! Я бот $botName, работающий на PocketCode.\n\n" +
                            "Архитектура проекта:\n" +
                            "• main.py: ядро и цикл опроса\n" +
                            "• handlers.py: роутинг сообщений\n" +
                            "• keyboards.py: инлайн кнопки\n" +
                            "• config.py: настройки и секреты\n\n" +
                            "Выберите действие:"
                }
                inlineKeyboard = getDynamicButtons()
            }
            trimmed.startsWith("/help") -> {
                val customHelp = extractCustomReturnText(handlersContent, "handle_help")
                botReplyText = if (customHelp.isNotEmpty()) {
                    customHelp
                } else {
                    "📖 Справка бота $botName:\n" +
                            "/start - Главное меню\n" +
                            "/help - Это справочное сообщение\n" +
                            "/calc <выражение> - Подсчет выражения\n" +
                            "/status - Статус модулей бота"
                }
            }
            trimmed.startsWith("/ping") -> {
                botReplyText = "🏓 Pong! Задержка: 32ms (Локальный движок PocketCode)"
            }
            trimmed.startsWith("/status") -> {
                botReplyText = "⚙️ Статус мультифайлового бота:\n" +
                        "• Файлов в проекте: ${project.files.size}\n" +
                        "• Исполняемый файл: main.py\n" +
                        "• Модули: handlers.py, keyboards.py, config.py\n" +
                        "• Установлено библиотек: ${project.installedPackages.size}\n" +
                        "• Сервер: Online"
            }
            trimmed.startsWith("/calc") -> {
                val expr = trimmed.removePrefix("/calc").trim()
                botReplyText = try {
                    val result = evaluateMath(expr)
                    "🧮 Результат вычисления '$expr' = $result"
                } catch (e: Exception) {
                    "❌ Ошибка в выражении: $expr"
                }
            }
            else -> {
                // Dynamic handler detection from user's handlers.py
                val cmdName = trimmed.removePrefix("/").substringBefore(" ").lowercase()
                val customCmdText = extractCustomReturnText(handlersContent, "handle_$cmdName")
                if (customCmdText.isNotEmpty()) {
                    botReplyText = "✉️ [handlers.py -> handle_$cmdName()]:\n$customCmdText"
                } else {
                    botReplyText = "✉️ [handlers.py]: Получено сообщение:\n\"$trimmed\"\n\nДлина: ${trimmed.length} симв. Обработано модулем handlers.py."
                }
            }
        }

        val responseMsg = BotMessage(
            sender = "bot",
            text = botReplyText,
            inlineButtons = inlineKeyboard
        )
        _messages.add(BotMessage(sender = "user", text = userText))
        _messages.add(responseMsg)

        onLog(ConsoleEntry(type = LogType.BOT_OUTGOING, text = "Telegram API ⬅ [Bot Reply]: '${botReplyText.lines().first()}'"))
        return responseMsg
    }

    suspend fun handleCallbackClick(callbackData: String, onLog: (ConsoleEntry) -> Unit): String {
        onLog(ConsoleEntry(type = LogType.BOT_INCOMING, text = "Telegram API ➔ [Callback Query]: '$callbackData'"))
        delay(150)

        val handlersFile = project.files.find { it.name == "handlers.py" }
        val handlersContent = handlersFile?.content ?: ""

        val replyText = when (callbackData) {
            "action_stats" -> "📊 Статистика:\n• Загружено модулей: ${project.files.size}\n• Активных соединений: 1\n• Память: 16.4 MB\n• Статус: Online"
            "action_ping" -> "🏓 Pong! Задержка сети: 28ms"
            "action_help" -> "🛠 Модули handlers.py и keyboards.py можно редактировать в соседних вкладках!"
            "action_settings" -> "⚙️ Настройки: Режим отладки ВКЛ, .env загружен."
            else -> {
                if (handlersContent.contains(callbackData)) {
                    "🔘 [handlers.py]: Обработан callback '$callbackData'"
                } else {
                    "Клик по кнопке: $callbackData"
                }
            }
        }

        _messages.add(BotMessage(sender = "bot", text = replyText))
        onLog(ConsoleEntry(type = LogType.BOT_OUTGOING, text = "Telegram API ⬅ [Callback Answer]: '${replyText.lines().first()}'"))
        return replyText
    }

    private fun extractCustomString(code: String, varName: String): String {
        var found = ""
        val lines = code.lines()
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("$varName =") || line.contains("$varName=")) {
                val right = line.substringAfter("=").trim()
                if (right.startsWith("(") || right.isEmpty()) {
                    val sb = StringBuilder()
                    for (j in (i + 1) until lines.size) {
                        val subLine = lines[j].trim()
                        if (subLine.startsWith(")")) break
                        if (subLine.isNotEmpty()) {
                            val clean = subLine.removePrefix("f").removeSurrounding("\"").removeSurrounding("'")
                            sb.append(clean).append("\n")
                        }
                    }
                    val res = sb.toString().trim()
                    if (res.isNotEmpty()) {
                        found = res
                        break
                    }
                } else {
                    val candidate = right.removePrefix("f").removeSurrounding("\"").removeSurrounding("'")
                    if (candidate.length > 1 && !candidate.startsWith("(")) {
                        found = candidate
                        break
                    }
                }
            }
        }
        return found
    }

    private fun extractCustomReturnText(code: String, funcName: String): String {
        val lines = code.lines()
        var insideFunc = false
        for (line in lines) {
            if (line.trim().startsWith("def $funcName")) {
                insideFunc = true
                continue
            }
            if (insideFunc) {
                if (line.trim().startsWith("def ")) break // new function
                if (line.contains("return")) {
                    val ret = line.substringAfter("return").trim().removeSurrounding("\"").removeSurrounding("'")
                    if (ret.isNotEmpty()) return ret
                }
            }
        }
        return ""
    }

    private fun evaluateMath(expr: String): Double {
        val sanitized = expr.replace(" ", "")
        return when {
            sanitized.contains("+") -> {
                val parts = sanitized.split("+")
                parts[0].toDouble() + parts[1].toDouble()
            }
            sanitized.contains("-") -> {
                val parts = sanitized.split("-")
                parts[0].toDouble() - parts[1].toDouble()
            }
            sanitized.contains("*") -> {
                val parts = sanitized.split("*")
                parts[0].toDouble() * parts[1].toDouble()
            }
            sanitized.contains("/") -> {
                val parts = sanitized.split("/")
                parts[0].toDouble() / parts[1].toDouble()
            }
            else -> sanitized.toDouble()
        }
    }
}
