package com.example.engine

import com.example.model.CodeFile
import com.example.model.ConsoleEntry
import com.example.model.LogType
import com.example.model.Project
import kotlinx.coroutines.delay

class MultiFileExecutor {

    suspend fun runProject(
        project: Project,
        onLog: (ConsoleEntry) -> Unit,
        onBotSimulatorReady: (TelegramBotSimulator) -> Unit
    ) {
        onLog(ConsoleEntry(type = LogType.SYSTEM, text = "$ python3 main.py"))
        delay(150)
        onLog(ConsoleEntry(type = LogType.SYSTEM, text = "─── PocketCode Engine: Loading Multi-file Project '${project.name}' ───"))
        delay(120)

        // 1. Locate entrypoint file
        val entryFile = project.files.find { it.isEntrypoint }
            ?: project.files.find { it.name == "main.py" }
            ?: project.files.firstOrNull()

        if (entryFile == null) {
            onLog(ConsoleEntry(type = LogType.STDERR, text = "❌ Ошибка: В проекте нет файлов для запуска! Добавьте main.py."))
            return
        }

        // 2. Scan and load .env file if available
        val envFile = project.files.find { it.isEnv }
        val envMap = mutableMapOf<String, String>()
        if (envFile != null) {
            onLog(ConsoleEntry(type = LogType.SYSTEM, text = "🔐 [dotenv] Loading environment variables from .env..."))
            envFile.content.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val key = trimmed.substringBefore("=").trim()
                    val value = trimmed.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                    envMap[key] = value
                }
            }
            onLog(ConsoleEntry(type = LogType.SYSTEM, text = "  Loaded ${envMap.size} variables: [${envMap.keys.joinToString(", ")}]"))
            delay(100)
        }

        // 3. Scan other files in project to resolve imports
        val otherFiles = project.files.filter { it.id != entryFile.id }
        val moduleNames = otherFiles.map { it.name.substringBeforeLast(".") }
        onLog(ConsoleEntry(type = LogType.SYSTEM, text = "📁 Scanning project modules: [${project.files.joinToString(", ") { it.name }}]"))
        delay(150)

        // 4. Execute lines of the entrypoint file
        var hasBotComponents = false
        var lineCount = 0

        val lines = entryFile.content.lines()
        for (line in lines) {
            lineCount++
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            // Check for imports of local modules
            if (trimmed.startsWith("import ") || trimmed.startsWith("from ")) {
                val importedModule = if (trimmed.startsWith("from ")) {
                    trimmed.removePrefix("from ").substringBefore(" ").trim()
                } else {
                    trimmed.removePrefix("import ").substringBefore(" ").substringBefore(",").trim()
                }

                if (moduleNames.contains(importedModule)) {
                    val matchedFile = otherFiles.find { it.name.startsWith(importedModule) }
                    onLog(ConsoleEntry(type = LogType.SYSTEM, text = "🔗 Module resolved: '$importedModule' ➔ ${matchedFile?.name ?: importedModule}"))
                    delay(80)
                }

                if (importedModule.contains("aiogram") || importedModule.contains("telegram") || importedModule.contains("handlers") || importedModule.contains("keyboards")) {
                    hasBotComponents = true
                }
            }

            // Execute print statements
            if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
                val rawArg = trimmed.removePrefix("print(").removeSuffix(")").trim()
                val evaluated = evaluatePrintExpression(rawArg, envMap, project)
                onLog(ConsoleEntry(type = LogType.STDOUT, text = evaluated))
                delay(90)
            }

            // Execute simulated routes or function calls
            if (trimmed.contains("dispatch_request(") || trimmed.contains("register_routes(")) {
                if (trimmed.contains("register_routes(")) {
                    val handlersFile = project.files.find { it.name == "handlers.py" }
                    if (handlersFile != null) {
                        onLog(ConsoleEntry(type = LogType.STDOUT, text = "✅ Handlers registered from handlers.py: [/start, /help, /stats, /calc]"))
                    }
                } else if (trimmed.contains("dispatch_request(")) {
                    onLog(ConsoleEntry(type = LogType.STDOUT, text = "🌐 [API Server] 200 OK: Route executed successfully"))
                }
                delay(80)
            }
        }

        delay(150)
        onLog(ConsoleEntry(type = LogType.SUCCESS, text = "✨ Multi-file execution finished (${lines.size} lines in ${entryFile.name})"))

        // If Telegram Bot detected, initialize simulator and prompt user
        if (hasBotComponents || project.id.contains("tg") || project.name.contains("Telegram")) {
            val simulator = TelegramBotSimulator(project)
            onLog(ConsoleEntry(type = LogType.SYSTEM, text = "🤖 Telegram Bot daemon started! Switch to 'Симулятор Бота' to test buttons and commands in real time."))
            onBotSimulatorReady(simulator)
        }
    }

    private fun evaluatePrintExpression(raw: String, env: Map<String, String>, project: Project): String {
        // Strip f"..." or f'...' or plain strings
        var result = raw
        if ((result.startsWith("f\"") && result.endsWith("\"")) || (result.startsWith("f'") && result.endsWith("'"))) {
            result = result.substring(2, result.length - 1)
        } else if ((result.startsWith("\"") && result.endsWith("\"")) || (result.startsWith("'") && result.endsWith("'"))) {
            return result.substring(1, result.length - 1)
        }

        // Replace known variables
        val token = env["BOT_TOKEN"] ?: "748920184:AAHq_m9vK8zY4X109j4k"
        result = result.replace("{BOT_TOKEN[:10]}***", token.take(10) + "***")
        result = result.replace("{BOT_TOKEN}", token)
        result = result.replace("{BOT_NAME}", "PocketBot v2.0")
        result = result.replace("{ADMINS}", "[12345678, 87654321]")
        result = result.replace("{len(routes.ENDPOINTS)}", "3")
        return result
    }
}
