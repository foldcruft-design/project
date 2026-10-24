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
        // Extract bot name from config.py if present
        val configFile = project.files.find { it.name == "config.py" }
        configFile?.content?.lines()?.forEach { line ->
            if (line.contains("BOT_NAME") && line.contains("=")) {
                val namePart = line.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                if (namePart.isNotEmpty()) {
                    botName = namePart
                }
            }
        }
        
        // Welcome message in simulator
        _messages.add(
            BotMessage(
                sender = "bot",
                text = "🤖 Симулятор Telegram Бота готов!\nБот: $botName\nФайлы подключены: ${project.files.size} шт.\n\nОтправьте команду /start или выберите кнопку ниже:",
                inlineButtons = listOf(
                    listOf(
                        InlineButton("🚀 /start", "/start"),
                        InlineButton("ℹ️ /help", "/help")
                    )
                )
            )
        )
    }

    suspend fun handleUserMessage(userText: String, onLog: (ConsoleEntry) -> Unit): BotMessage {
        onLog(ConsoleEntry(type = LogType.BOT_INCOMING, text = "Telegram API ➔ [Incoming Message] from User: '$userText'"))
        delay(250) // Realistic network delay

        val handlersFile = project.files.find { it.name == "handlers.py" }
        val keyboardsFile = project.files.find { it.name == "keyboards.py" }

        val botReplyText: String
        var inlineKeyboard: List<List<InlineButton>> = emptyList()

        val trimmed = userText.trim()
        when {
            trimmed.startsWith("/start") -> {
                botReplyText = "👋 Привет! Я бот $botName, работающий на PocketCode.\n\n" +
                        "Архитектура проекта:\n" +
                        "• main.py: ядро и цикл опроса\n" +
                        "• handlers.py: роутинг сообщений\n" +
                        "• keyboards.py: инлайн кнопки\n" +
                        "• config.py: настройки и секреты\n\n" +
                        "Выберите действие:"
                
                inlineKeyboard = listOf(
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
            trimmed.startsWith("/help") -> {
                botReplyText = "📖 Справка бота $botName:\n" +
                        "/start - Главное меню\n" +
                        "/help - Это справочное сообщение\n" +
                        "/calc <числа> - Простой подсчет\n" +
                        "/ping - Проверка отклика\n\n" +
                        "💡 Любое другое сообщение бот обработает через handlers.py!"
            }
            trimmed.startsWith("/ping") -> {
                botReplyText = "🏓 Pong! Задержка: 38ms (Локальный движок PocketCode)"
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
                botReplyText = "✉️ [handlers.py]: Получено сообщение:\n\"$trimmed\"\n\nДлина: ${trimmed.length} симв. Статус: Обработано успешно."
            }
        }

        val responseMsg = BotMessage(
            sender = "bot",
            text = botReplyText,
            inlineButtons = inlineKeyboard
        )
        _messages.add(BotMessage(sender = "user", text = userText))
        _messages.add(responseMsg)

        onLog(ConsoleEntry(type = LogType.BOT_OUTGOING, text = "Telegram API ⬅ [Bot Reply] to User: '${botReplyText.lines().first()}'"))
        return responseMsg
    }

    suspend fun handleCallbackClick(callbackData: String, onLog: (ConsoleEntry) -> Unit): String {
        onLog(ConsoleEntry(type = LogType.BOT_INCOMING, text = "Telegram API ➔ [Callback Query]: '$callbackData'"))
        delay(200)

        val replyText = when (callbackData) {
            "action_stats" -> "📊 Статистика:\n• Загружено модулей: ${project.files.size}\n• Активных соединений: 1\n• Память: 14.2 MB\n• Статус: Online"
            "action_ping" -> "🏓 Pong! Симулятор бота работает в реальном времени."
            "action_help" -> "🛠 Модули handlers.py и keyboards.py можно редактировать на соседних вкладках!"
            "action_settings" -> "⚙️ Настройки: Режим отладки ВКЛ, .env загружен."
            "/start" -> "Перезапуск диалога..."
            "/help" -> "Справка открыта."
            else -> "Клик по кнопке: $callbackData"
        }

        _messages.add(BotMessage(sender = "bot", text = replyText))
        onLog(ConsoleEntry(type = LogType.BOT_OUTGOING, text = "Telegram API ⬅ [Callback Answer]: '${replyText.lines().first()}'"))
        return replyText
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
