package com.example.engine

import com.example.model.ConsoleEntry
import com.example.model.LogType
import com.example.model.Project
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

class MultiFileExecutor {

    suspend fun runProject(
        project: Project,
        onLog: (ConsoleEntry) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        val entryFile = project.files.find { it.isEntrypoint }
            ?: project.files.find { it.name == "main.py" }
            ?: project.files.firstOrNull()

        if (entryFile == null) {
            onLog(ConsoleEntry(type = LogType.STDERR, text = "❌ Ошибка: В проекте нет файлов для запуска. Создайте main.py."))
            return
        }

        val df = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = df.format(Date(startTime))

        onLog(ConsoleEntry(type = LogType.SYSTEM, text = "[$timeStr] $ python3 ${entryFile.name}"))
        delay(60)

        if (entryFile.content.isBlank()) {
            onLog(ConsoleEntry(type = LogType.SYSTEM, text = "ℹ️ Файл ${entryFile.name} пуст. Напишите код (например: print('Hello, World!')) и запустите снова."))
            onLog(ConsoleEntry(type = LogType.SUCCESS, text = "✨ [Process exited with code 0 in 12ms]"))
            return
        }

        // Environment variables
        val envMap = mutableMapOf<String, String>()
        val envFile = project.files.find { it.name == ".env" }
        if (envFile != null && envFile.content.isNotBlank()) {
            envFile.content.lines().forEach { line ->
                val tr = line.trim()
                if (tr.isNotEmpty() && !tr.startsWith("#") && tr.contains("=")) {
                    val k = tr.substringBefore("=").trim()
                    val v = tr.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                    envMap[k] = v
                }
            }
        }

        // Available local modules in project (only other files created by user)
        val otherFiles = project.files.filter { it.id != entryFile.id }
        val localModules = otherFiles.associateBy { it.name.substringBeforeLast(".") }

        // Context state for execution
        val variables = mutableMapOf<String, Any>()
        val functions = mutableMapOf<String, UserFunction>()

        // Preload built-ins & standard library symbols
        variables["__name__"] = "__main__"
        variables["__file__"] = entryFile.name
        variables["PI"] = Math.PI
        variables["True"] = true
        variables["False"] = false
        variables["None"] = "None"

        // Also extract functions from imported local files if referenced
        val importedModules = mutableSetOf<String>()

        val rawLines = entryFile.content.lines()
        var lineIndex = 0
        var hasError = false
        var executedStatements = 0

        try {
            while (lineIndex < rawLines.size) {
                val line = rawLines[lineIndex]
                val trimmed = line.trim()

                // Skip comments and empty lines
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    lineIndex++
                    continue
                }

                // Function definitions
                if (trimmed.startsWith("def ")) {
                    val header = trimmed.removePrefix("def ").trim()
                    val funcName = header.substringBefore("(").trim()
                    val paramsStr = header.substringAfter("(").substringBefore(")").trim()
                    val params = if (paramsStr.isEmpty()) emptyList() else paramsStr.split(",").map { it.trim() }

                    val bodyLines = mutableListOf<String>()
                    lineIndex++
                    while (lineIndex < rawLines.size) {
                        val bodyLine = rawLines[lineIndex]
                        if (bodyLine.startsWith("    ") || bodyLine.startsWith("\t") || bodyLine.isBlank()) {
                            bodyLines.add(bodyLine.trim())
                            lineIndex++
                        } else {
                            break
                        }
                    }
                    functions[funcName] = UserFunction(funcName, params, bodyLines)
                    continue
                }

                // Handle imports
                if (trimmed.startsWith("import ") || trimmed.startsWith("from ")) {
                    val moduleName = if (trimmed.startsWith("from ")) {
                        trimmed.removePrefix("from ").substringBefore(" ").trim()
                    } else {
                        trimmed.removePrefix("import ").substringBefore(" ").substringBefore(",").trim()
                    }

                    importedModules.add(moduleName)

                    // Check if local file
                    if (localModules.containsKey(moduleName)) {
                        val localModFile = localModules[moduleName]!!
                        parseLocalModule(localModFile.content, moduleName, variables, functions)
                        onLog(ConsoleEntry(type = LogType.SYSTEM, text = "🔗 [Import] Loaded module '$moduleName' from ${localModFile.name}"))
                    } else if (isStandardModule(moduleName)) {
                        loadStandardModule(moduleName, variables)
                    } else if (project.installedPackages.any { it.equals(moduleName, ignoreCase = true) }) {
                        onLog(ConsoleEntry(type = LogType.SYSTEM, text = "📦 [VirtualEnv] Loaded package '$moduleName' (v1.0.0)"))
                    } else {
                        // Uninstalled package warning/error
                        onLog(ConsoleEntry(type = LogType.STDERR, text = "ModuleNotFoundError: No module named '$moduleName'"))
                        onLog(ConsoleEntry(type = LogType.SYSTEM, text = "💡 Подсказка: откройте вкладку 'pip' снизу и выполните: pip install $moduleName"))
                        hasError = true
                        break
                    }
                    lineIndex++
                    continue
                }

                // For loop: for i in range(N):
                if (trimmed.startsWith("for ") && trimmed.endsWith(":")) {
                    val forHeader = trimmed.removePrefix("for ").removeSuffix(":").trim()
                    val varName = forHeader.substringBefore(" in ").trim()
                    val iterableExpr = forHeader.substringAfter(" in ").trim()

                    val loopBody = mutableListOf<String>()
                    lineIndex++
                    while (lineIndex < rawLines.size) {
                        val bodyLine = rawLines[lineIndex]
                        if (bodyLine.startsWith("    ") || bodyLine.startsWith("\t") || bodyLine.isBlank()) {
                            if (bodyLine.isNotBlank()) loopBody.add(bodyLine.trim())
                            lineIndex++
                        } else {
                            break
                        }
                    }

                    val items = evaluateIterable(iterableExpr, variables)
                    for (item in items.take(100)) { // safety limit 100 iterations
                        variables[varName] = item
                        for (bodyStmt in loopBody) {
                            executeStatement(bodyStmt, variables, functions, envMap, onLog)
                            delay(15)
                        }
                    }
                    continue
                }

                // If condition
                if (trimmed.startsWith("if ") && trimmed.endsWith(":")) {
                    val condExpr = trimmed.removePrefix("if ").removeSuffix(":").trim()
                    val isTrue = evaluateCondition(condExpr, variables)

                    val ifBody = mutableListOf<String>()
                    val elseBody = mutableListOf<String>()
                    var inElse = false

                    lineIndex++
                    while (lineIndex < rawLines.size) {
                        val bodyLine = rawLines[lineIndex]
                        val trBody = bodyLine.trim()
                        if (trBody == "else:" || trBody.startsWith("else:")) {
                            inElse = true
                            lineIndex++
                            continue
                        }

                        if (bodyLine.startsWith("    ") || bodyLine.startsWith("\t") || bodyLine.isBlank()) {
                            if (bodyLine.isNotBlank()) {
                                if (inElse) elseBody.add(trBody) else ifBody.add(trBody)
                            }
                            lineIndex++
                        } else {
                            break
                        }
                    }

                    val stmtsToRun = if (isTrue) ifBody else elseBody
                    for (stmt in stmtsToRun) {
                        executeStatement(stmt, variables, functions, envMap, onLog)
                        delay(20)
                    }
                    continue
                }

                // Single line statement execution
                executeStatement(trimmed, variables, functions, envMap, onLog)
                executedStatements++
                delay(30)
                lineIndex++
            }
        } catch (e: Exception) {
            hasError = true
            onLog(ConsoleEntry(type = LogType.STDERR, text = "Traceback (most recent call last):"))
            onLog(ConsoleEntry(type = LogType.STDERR, text = "  File \"${entryFile.name}\", line ${lineIndex + 1}"))
            onLog(ConsoleEntry(type = LogType.STDERR, text = "${e.javaClass.simpleName}: ${e.message ?: "Execution error"}"))
        }

        val duration = System.currentTimeMillis() - startTime
        delay(60)

        if (!hasError) {
            onLog(ConsoleEntry(type = LogType.SUCCESS, text = "✨ [Process finished with exit code 0 in ${duration}ms]"))
        } else {
            onLog(ConsoleEntry(type = LogType.STDERR, text = "❌ [Process finished with exit code 1 in ${duration}ms]"))
        }
    }

    private suspend fun executeStatement(
        statement: String,
        variables: MutableMap<String, Any>,
        functions: Map<String, UserFunction>,
        env: Map<String, String>,
        onLog: (ConsoleEntry) -> Unit
    ) {
        val trimmed = statement.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return

        // Print statement: print(...)
        if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
            val content = trimmed.removePrefix("print(").removeSuffix(")").trim()
            val output = evaluatePrint(content, variables, functions, env)
            onLog(ConsoleEntry(type = LogType.STDOUT, text = output))
            return
        }

        // Variable assignment: x = ...
        if (trimmed.contains("=") && !trimmed.contains("==") && !trimmed.contains("<=") && !trimmed.contains(">=")) {
            val varName = trimmed.substringBefore("=").trim()
            val valExpr = trimmed.substringAfter("=").trim()

            // Function call assignment: stats = calculate_stats(...)
            if (valExpr.contains("(") && valExpr.endsWith(")")) {
                val funcName = valExpr.substringBefore("(").trim()
                val argStr = valExpr.substringAfter("(").removeSuffix(")").trim()
                if (functions.containsKey(funcName)) {
                    val result = callUserFunction(functions[funcName]!!, argStr, variables)
                    if (result != null) {
                        variables[varName] = result
                        return
                    }
                }
            }

            val evaluatedVal = evaluateExpression(valExpr, variables)
            variables[varName] = evaluatedVal
            return
        }
    }

    private fun splitTopLevelCommas(str: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        val sb = StringBuilder()
        for (i in str.indices) {
            val c = str[i]
            if ((c == '"' || c == '\'') && (i == 0 || str[i - 1] != '\\')) {
                if (inQuotes && quoteChar == c) inQuotes = false
                else if (!inQuotes) { inQuotes = true; quoteChar = c }
            }
            if (!inQuotes) {
                if (c == '(' || c == '[' || c == '{') depth++
                else if (c == ')' || c == ']' || c == '}') depth--
                else if (c == ',' && depth == 0) {
                    val trimmed = sb.toString().trim()
                    if (trimmed.isNotEmpty()) parts.add(trimmed)
                    sb.clear()
                    continue
                }
            }
            sb.append(c)
        }
        val trimmed = sb.toString().trim()
        if (trimmed.isNotEmpty()) parts.add(trimmed)
        return parts
    }

    private fun evaluatePrint(
        expr: String,
        variables: Map<String, Any>,
        functions: Map<String, UserFunction>,
        env: Map<String, String>
    ): String {
        var str = expr

        // Comma separated prints: print("A", "B", 123)
        if (!str.startsWith("f\"") && !str.startsWith("f'") && !str.startsWith("[") && !str.startsWith("{")) {
            val parts = splitTopLevelCommas(str)
            if (parts.size > 1) {
                return parts.joinToString(" ") { evaluatePrint(it, variables, functions, env) }
            }
        }

        // F-string: f"Hello, {name}!"
        if ((str.startsWith("f\"") && str.endsWith("\"")) || (str.startsWith("f'") && str.endsWith("'"))) {
            val inner = str.substring(2, str.length - 1)
            val regex = Regex("\\{([^}]+)\\}")
            return regex.replace(inner) { match ->
                val placeholder = match.groupValues[1].trim()
                resolvePlaceholder(placeholder, variables, functions, env)
            }
        }

        // Plain string: "Hello"
        if ((str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("'") && str.endsWith("'"))) {
            return str.substring(1, str.length - 1)
                .replace("\\n", "\n")
                .replace("\\t", "\t")
        }

        // Expression evaluation
        return resolvePlaceholder(str, variables, functions, env)
    }

    private fun resolvePlaceholder(
        placeholder: String,
        variables: Map<String, Any>,
        functions: Map<String, UserFunction>,
        env: Map<String, String>
    ): String {
        var target = placeholder.trim()

        // Formatting like :.2f
        var formatSpec: String? = null
        if (target.contains(":") && !target.startsWith("{")) {
            formatSpec = target.substringAfter(":").trim()
            target = target.substringBefore(":").trim()
        }

        // Direct variable lookup
        if (variables.containsKey(target)) {
            val v = variables[target]
            return formatValue(v, formatSpec)
        }

        // Dictionary lookup: stats['sum'] or stats["avg"]
        if (target.contains("['") || target.contains("[\"")) {
            val mapName = target.substringBefore("[").trim()
            val key = target.substringAfter("[").removeSuffix("]").trim().removeSurrounding("\"").removeSurrounding("'")
            val mapObj = variables[mapName]
            if (mapObj is Map<*, *>) {
                val value = mapObj[key]
                return formatValue(value, formatSpec)
            }
        }

        // List indexing: data[0] or data[i]
        if (target.contains("[") && target.endsWith("]")) {
            val listName = target.substringBefore("[").trim()
            val indexExpr = target.substringAfter("[").removeSuffix("]").trim()
            val idx = (variables[indexExpr] as? Number)?.toInt() ?: indexExpr.toIntOrNull() ?: 0
            val listObj = variables[listName]
            if (listObj is List<*>) {
                val v = listObj.getOrNull(idx)
                return formatValue(v, formatSpec)
            }
        }

        // Built-in functions: len(data), sum(data), max(data), min(data)
        if (target.startsWith("len(") && target.endsWith(")")) {
            val arg = target.removePrefix("len(").removeSuffix(")").trim()
            val obj = variables[arg]
            val len = if (obj is List<*>) obj.size else if (obj is Map<*, *>) obj.size else obj?.toString()?.length ?: 0
            return len.toString()
        }
        if (target.startsWith("sum(") && target.endsWith(")")) {
            val arg = target.removePrefix("sum(").removeSuffix(")").trim()
            val list = (variables[arg] as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
            val sum = list.sum()
            return if (sum % 1.0 == 0.0) sum.toLong().toString() else sum.toString()
        }
        if (target.startsWith("max(") && target.endsWith(")")) {
            val arg = target.removePrefix("max(").removeSuffix(")").trim()
            val list = (variables[arg] as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
            val max = list.maxOrNull() ?: 0.0
            return if (max % 1.0 == 0.0) max.toLong().toString() else max.toString()
        }
        if (target.startsWith("min(") && target.endsWith(")")) {
            val arg = target.removePrefix("min(").removeSuffix(")").trim()
            val list = (variables[arg] as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
            val min = list.minOrNull() ?: 0.0
            return if (min % 1.0 == 0.0) min.toLong().toString() else min.toString()
        }

        // User function call
        if (target.contains("(") && target.endsWith(")")) {
            val funcName = target.substringBefore("(").trim()
            val arg = target.substringAfter("(").removeSuffix(")").trim()
            if (functions.containsKey(funcName)) {
                val res = callUserFunction(functions[funcName]!!, arg, variables)
                return formatValue(res, formatSpec)
            }
        }

        // Simple math expression: 2 + 2, 10 * 5, i + 1
        val mathResult = tryEvaluateMath(target, variables)
        if (mathResult != null) {
            return formatValue(mathResult, formatSpec)
        }

        // Env lookup
        if (env.containsKey(target)) {
            return env[target]!!
        }

        return target
    }

    private fun formatValue(value: Any?, spec: String?): String {
        if (value == null) return "None"
        if (spec != null && value is Number) {
            return try {
                val d = value.toDouble()
                if (spec.endsWith("f")) {
                    val decimals = spec.removeSuffix("f").removePrefix(".").toIntOrNull() ?: 2
                    String.format(Locale.US, "%.${decimals}f", d)
                } else {
                    value.toString()
                }
            } catch (e: Exception) {
                value.toString()
            }
        }
        if (value is Double && value % 1.0 == 0.0) {
            return value.toLong().toString()
        }
        return value.toString()
    }

    private fun evaluateExpression(expr: String, variables: Map<String, Any>): Any {
        val trimmed = expr.trim()

        // Number literals
        val asInt = trimmed.toLongOrNull()
        if (asInt != null) return asInt
        val asDouble = trimmed.toDoubleOrNull()
        if (asDouble != null) return asDouble

        // String literals
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        // Booleans
        if (trimmed == "True") return true
        if (trimmed == "False") return false

        // Lists: [1, 2, 3] or [x for x in ...]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            if (inner.isEmpty()) return emptyList<Any>()
            val items = inner.split(",").map { item ->
                evaluateExpression(item.trim(), variables)
            }
            return items
        }

        // Math
        val math = tryEvaluateMath(trimmed, variables)
        if (math != null) return math

        // Existing variable
        if (variables.containsKey(trimmed)) return variables[trimmed]!!

        return trimmed
    }

    private fun evaluateIterable(expr: String, variables: Map<String, Any>): List<Any> {
        val trimmed = expr.trim()

        // range(n) or range(start, stop)
        if (trimmed.startsWith("range(") && trimmed.endsWith(")")) {
            val argsStr = trimmed.removePrefix("range(").removeSuffix(")").trim()
            val args = argsStr.split(",").map { evaluateExpression(it.trim(), variables) }
            return when (args.size) {
                1 -> {
                    val stop = (args[0] as? Number)?.toInt() ?: 0
                    (0 until stop).map { it }
                }
                2 -> {
                    val start = (args[0] as? Number)?.toInt() ?: 0
                    val stop = (args[1] as? Number)?.toInt() ?: 0
                    (start until stop).map { it }
                }
                else -> emptyList()
            }
        }

        // List variable: data or data[:4]
        if (variables.containsKey(trimmed)) {
            val v = variables[trimmed]
            if (v is List<*>) return v.filterNotNull()
        }

        // Sliced list: data[:4]
        if (trimmed.contains("[:")) {
            val varName = trimmed.substringBefore("[:").trim()
            val limit = trimmed.substringAfter("[:").removeSuffix("]").toIntOrNull() ?: 10
            val v = variables[varName]
            if (v is List<*>) return v.take(limit).filterNotNull()
        }

        return emptyList()
    }

    private fun evaluateCondition(expr: String, variables: Map<String, Any>): Boolean {
        val trimmed = expr.trim()
        return when {
            trimmed.contains("==") -> {
                val left = evaluateExpression(trimmed.substringBefore("==").trim(), variables)
                val right = evaluateExpression(trimmed.substringAfter("==").trim(), variables)
                left == right
            }
            trimmed.contains("!=") -> {
                val left = evaluateExpression(trimmed.substringBefore("!=").trim(), variables)
                val right = evaluateExpression(trimmed.substringAfter("!=").trim(), variables)
                left != right
            }
            trimmed.contains(">=") -> {
                val left = (evaluateExpression(trimmed.substringBefore(">=").trim(), variables) as? Number)?.toDouble() ?: 0.0
                val right = (evaluateExpression(trimmed.substringAfter(">=").trim(), variables) as? Number)?.toDouble() ?: 0.0
                left >= right
            }
            trimmed.contains("<=") -> {
                val left = (evaluateExpression(trimmed.substringBefore("<=").trim(), variables) as? Number)?.toDouble() ?: 0.0
                val right = (evaluateExpression(trimmed.substringAfter("<=").trim(), variables) as? Number)?.toDouble() ?: 0.0
                left <= right
            }
            trimmed.contains(">") -> {
                val left = (evaluateExpression(trimmed.substringBefore(">").trim(), variables) as? Number)?.toDouble() ?: 0.0
                val right = (evaluateExpression(trimmed.substringAfter(">").trim(), variables) as? Number)?.toDouble() ?: 0.0
                left > right
            }
            trimmed.contains("<") -> {
                val left = (evaluateExpression(trimmed.substringBefore("<").trim(), variables) as? Number)?.toDouble() ?: 0.0
                val right = (evaluateExpression(trimmed.substringAfter("<").trim(), variables) as? Number)?.toDouble() ?: 0.0
                left < right
            }
            trimmed == "True" -> true
            trimmed == "False" -> false
            variables.containsKey(trimmed) -> {
                val v = variables[trimmed]
                (v as? Boolean) ?: (v != null && v != 0 && v != "")
            }
            else -> false
        }
    }

    private fun callUserFunction(func: UserFunction, argsStr: String, parentVars: Map<String, Any>): Any? {
        val localVars = parentVars.toMutableMap()
        if (argsStr.isNotEmpty() && func.params.isNotEmpty()) {
            val passedArgs = splitTopLevelCommas(argsStr).map { evaluateExpression(it.trim(), parentVars) }
            func.params.forEachIndexed { i, p ->
                if (i < passedArgs.size) localVars[p] = passedArgs[i]
            }
        }

        for (line in func.body) {
            val trimmed = line.trim()
            if (trimmed.startsWith("return")) {
                val retExpr = trimmed.removePrefix("return").trim()

                // Dictionary return: { "count": count, ... }
                if (retExpr.startsWith("{") && retExpr.endsWith("}")) {
                    val map = mutableMapOf<String, Any>()
                    val pairs = retExpr.removeSurrounding("{", "}").split(",")
                    for (pair in pairs) {
                        if (pair.contains(":")) {
                            val k = pair.substringBefore(":").trim().removeSurrounding("\"").removeSurrounding("'")
                            val vExpr = pair.substringAfter(":").trim()
                            map[k] = evaluateExpression(vExpr, localVars)
                        }
                    }
                    return map
                }

                // F-string return
                if (retExpr.startsWith("f\"") || retExpr.startsWith("f'")) {
                    return evaluatePrint(retExpr, localVars, emptyMap(), emptyMap())
                }

                return evaluateExpression(retExpr, localVars)
            } else if (trimmed.contains("=")) {
                val k = trimmed.substringBefore("=").trim()
                val v = trimmed.substringAfter("=").trim()
                localVars[k] = evaluateExpression(v, localVars)
            }
        }
        return null
    }

    private fun tryEvaluateMath(expr: String, variables: Map<String, Any>): Double? {
        val clean = expr.trim()
        val operators = listOf("+", "-", "*", "/")
        for (op in operators) {
            if (clean.contains(op) && !clean.startsWith(op) && !clean.endsWith(op)) {
                val leftStr = clean.substringBefore(op).trim()
                val rightStr = clean.substringAfter(op).trim()
                val leftVal = (variables[leftStr] as? Number)?.toDouble() ?: leftStr.toDoubleOrNull()
                val rightVal = (variables[rightStr] as? Number)?.toDouble() ?: rightStr.toDoubleOrNull()
                if (leftVal != null && rightVal != null) {
                    return when (op) {
                        "+" -> leftVal + rightVal
                        "-" -> leftVal - rightVal
                        "*" -> leftVal * rightVal
                        "/" -> if (rightVal != 0.0) leftVal / rightVal else Double.NaN
                        else -> null
                    }
                }
            }
        }
        return null
    }

    private fun isStandardModule(name: String): Boolean {
        return name in listOf("math", "time", "random", "os", "sys", "json", "datetime", "re", "collections")
    }

    private fun loadStandardModule(name: String, variables: MutableMap<String, Any>) {
        when (name) {
            "math" -> {
                variables["math.pi"] = Math.PI
                variables["math.e"] = Math.E
            }
            "os" -> {
                variables["os.name"] = "posix"
            }
            "sys" -> {
                variables["sys.version"] = "3.11.8 (PocketCode Mobile Engine)"
            }
        }
    }

    private fun parseLocalModule(
        content: String,
        moduleName: String,
        variables: MutableMap<String, Any>,
        functions: MutableMap<String, UserFunction>
    ) {
        content.lines().forEach { line ->
            val tr = line.trim()
            if (tr.startsWith("def ")) {
                val header = tr.removePrefix("def ").trim()
                val funcName = header.substringBefore("(").trim()
                val paramsStr = header.substringAfter("(").substringBefore(")").trim()
                val params = if (paramsStr.isEmpty()) emptyList() else paramsStr.split(",").map { it.trim() }
                val userFunc = UserFunction(funcName, params, listOf("return '$funcName called'"))
                functions["$moduleName.$funcName"] = userFunc
                functions[funcName] = userFunc
            } else if (tr.contains("=") && !tr.contains("==")) {
                val k = tr.substringBefore("=").trim()
                val v = tr.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                variables["$moduleName.$k"] = v
            }
        }
    }
}

data class UserFunction(
    val name: String,
    val params: List<String>,
    val body: List<String>
)
