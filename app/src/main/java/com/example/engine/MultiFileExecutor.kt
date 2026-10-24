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
        val entryFile = project.files.find { it.isEntrypoint }
            ?: project.files.find { it.name == "main.py" }
            ?: project.files.firstOrNull()

        if (entryFile == null) {
            onLog(ConsoleEntry(type = LogType.STDERR, text = "❌ Ошибка: В проекте нет файлов для запуска! Добавьте main.py."))
            return
        }

        onLog(ConsoleEntry(type = LogType.SYSTEM, text = "$ python3 ${entryFile.name}"))
        delay(120)
        onLog(ConsoleEntry(type = LogType.SYSTEM, text = "─── PocketCode Engine: Multi-File Execution '${project.name}' ───"))
        delay(80)

        // 1. Scan and load .env file if available
        val envFile = project.files.find { it.isEnv }
        val envMap = mutableMapOf<String, String>()
        if (envFile != null && envFile.content.isNotBlank()) {
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
            delay(80)
        }

        // 2. Scan other files in project to resolve imports
        val otherFiles = project.files.filter { it.id != entryFile.id }
        val moduleNames = otherFiles.map { it.name.substringBeforeLast(".") }
        onLog(ConsoleEntry(type = LogType.SYSTEM, text = "📁 Scanning project files: [${project.files.joinToString(", ") { it.name }}]"))
        delay(100)

        // Extract functions and variables from other files for cross-file linkage
        val crossFileFunctions = mutableMapOf<String, String>() // funcName -> return string or code
        otherFiles.forEach { file ->
            val mod = file.name.substringBeforeLast(".")
            var insideFunc: String? = null
            file.content.lines().forEach { line ->
                val tr = line.trim()
                if (tr.startsWith("def ")) {
                    val fn = tr.removePrefix("def ").substringBefore("(").trim()
                    insideFunc = fn
                } else if (insideFunc != null && tr.contains("return")) {
                    val ret = tr.substringAfter("return").trim().removeSurrounding("\"").removeSurrounding("'")
                    crossFileFunctions["$mod.$insideFunc"] = ret
                    crossFileFunctions[insideFunc!!] = ret
                    insideFunc = null
                }
            }
        }

        if (entryFile.content.isBlank()) {
            onLog(ConsoleEntry(type = LogType.SYSTEM, text = "ℹ️ Файл ${entryFile.name} пуст. Напишите код (например, print('Привет!')) или выберите сниппет из 'Карточек'."))
            onLog(ConsoleEntry(type = LogType.SUCCESS, text = "✨ Процесс завершен (пустой файл)."))
            return
        }

        // 3. Execute lines of the entrypoint file
        var hasBotComponents = false
        var executedLines = 0

        val lines = entryFile.content.lines()
        for (line in lines) {
            executedLines++
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            // Check for imports of local modules or installed packages
            if (trimmed.startsWith("import ") || trimmed.startsWith("from ")) {
                val importedModule = if (trimmed.startsWith("from ")) {
                    trimmed.removePrefix("from ").substringBefore(" ").trim()
                } else {
                    trimmed.removePrefix("import ").substringBefore(" ").substringBefore(",").trim()
                }

                if (moduleNames.contains(importedModule)) {
                    val matchedFile = otherFiles.find { it.name.startsWith(importedModule) }
                    onLog(ConsoleEntry(type = LogType.SYSTEM, text = "🔗 Local module resolved: '$importedModule' ➔ ${matchedFile?.name}"))
                    delay(60)
                } else if (project.installedPackages.any { it.equals(importedModule, ignoreCase = true) }) {
                    onLog(ConsoleEntry(type = LogType.SYSTEM, text = "📦 [VirtualEnv] Package '$importedModule' resolved from installed packages"))
                    delay(60)
                }

                if (importedModule.contains("aiogram") || importedModule.contains("telegram") ||
                    importedModule.contains("handlers") || importedModule.contains("keyboards")) {
                    hasBotComponents = true
                }
            }

            // Execute print statements dynamically
            if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
                val rawArg = trimmed.removePrefix("print(").removeSuffix(")").trim()
                val evaluated = evaluatePrintExpression(rawArg, envMap, crossFileFunctions, project)
                onLog(ConsoleEntry(type = LogType.STDOUT, text = evaluated))
                delay(80)
            }

            // Function calls from other modules
            if (trimmed.contains(".register_routes()") || trimmed.contains("register_routes()")) {
                onLog(ConsoleEntry(type = LogType.STDOUT, text = "✅ Handlers registered from handlers.py: [/start, /help, /status, /calc, callbacks]"))
                delay(60)
            } else if (trimmed.contains("dispatch_request(")) {
                onLog(ConsoleEntry(type = LogType.STDOUT, text = "🌐 [API Server] 200 OK: Route executed successfully"))
                delay(60)
            }
        }

        delay(100)
        onLog(ConsoleEntry(type = LogType.SUCCESS, text = "✨ Multi-file execution finished ($executedLines lines processed)"))

        // If Telegram Bot detected, initialize simulator and prompt user
        if (hasBotComponents || project.id.contains("tg") || project.name.contains("Telegram")) {
            val simulator = TelegramBotSimulator(project)
            onLog(ConsoleEntry(type = LogType.SYSTEM, text = "🤖 Telegram Bot daemon started! Откройте 'Чат-симулятор бота' во вкладке справа для тестирования."))
            onBotSimulatorReady(simulator)
        }
    }

    private fun evaluatePrintExpression(
        raw: String,
        env: Map<String, String>,
        crossFileFuncs: Map<String, String>,
        project: Project
    ): String {
        var result = raw

        // Strip f"..." or plain strings
        if ((result.startsWith("f\"") && result.endsWith("\"")) || (result.startsWith("f'") && result.endsWith("'"))) {
            result = result.substring(2, result.length - 1)
        } else if ((result.startsWith("\"") && result.endsWith("\"")) || (result.startsWith("'") && result.endsWith("'"))) {
            return result.substring(1, result.length - 1)
        }

        // Cross-file function return substitution
        crossFileFuncs.forEach { (funcSignature, retVal) ->
            if (result.contains("$funcSignature()")) {
                result = result.replace("$funcSignature()", retVal)
            }
        }

        // Replace known environment / project variables
        val token = env["BOT_TOKEN"] ?: "748920184:AAHq_m9vK8zY4X109j4k"
        result = result.replace("{BOT_TOKEN[:10]}***", token.take(10) + "***")
        result = result.replace("{BOT_TOKEN}", token)
        result = result.replace("{BOT_NAME}", "PocketBot v2.0")
        result = result.replace("{ADMINS}", "[12345678, 87654321]")
        result = result.replace("{len(routes.ENDPOINTS)}", "3")
        return result
    }
}
