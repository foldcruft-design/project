package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

data class CodeFile(
    val id: String,
    val projectId: String,
    val name: String,
    val content: String,
    val isEntrypoint: Boolean = false
) {
    val extension: String
        get() = name.substringAfterLast('.', "")
        
    val isPython: Boolean
        get() = extension.lowercase() == "py"
        
    val isRequirements: Boolean
        get() = name.lowercase() == "requirements.txt"
        
    val isEnv: Boolean
        get() = name.lowercase() == ".env"
}

data class Project(
    val id: String,
    val name: String,
    val description: String,
    val language: String = "python",
    val activeFileId: String,
    val files: List<CodeFile> = emptyList(),
    val installedPackages: List<String> = emptyList(),
    val envVariables: Map<String, String> = emptyMap()
)

data class PackageItem(
    val name: String,
    val version: String,
    val description: String,
    val category: String,
    val isInstalled: Boolean = false,
    val importSnippet: String = "",
    val docsUrl: String = ""
)

enum class LogType {
    STDOUT, STDERR, SYSTEM, BOT_INCOMING, BOT_OUTGOING, SUCCESS
}

data class ConsoleEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: LogType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SyntaxTheme(
    val id: String,
    val name: String,
    val editorBackground: Color,
    val lineNumbersColor: Color,
    val cursorColor: Color,
    val keywordColor: Color,
    val stringColor: Color,
    val functionColor: Color,
    val numberColor: Color,
    val commentColor: Color,
    val decoratorColor: Color,
    val operatorColor: Color,
    val variableColor: Color
)

object SyntaxThemes {
    val OneDark = SyntaxTheme(
        id = "one_dark",
        name = "One Dark Pro",
        editorBackground = Color(0xFF1E222B),
        lineNumbersColor = Color(0xFF5C6370),
        cursorColor = Color(0xFF528BFF),
        keywordColor = Color(0xFFE06C75),
        stringColor = Color(0xFF98C379),
        functionColor = Color(0xFF61AFEF),
        numberColor = Color(0xFFD19A66),
        commentColor = Color(0xFF5C6370),
        decoratorColor = Color(0xFFC678DD),
        operatorColor = Color(0xFF56B6C2),
        variableColor = Color(0xFFABB2BF)
    )

    val Dracula = SyntaxTheme(
        id = "dracula",
        name = "Dracula Midnight",
        editorBackground = Color(0xFF21222C),
        lineNumbersColor = Color(0xFF6272A4),
        cursorColor = Color(0xFFFF79C6),
        keywordColor = Color(0xFFFF79C6),
        stringColor = Color(0xFFF1FA8C),
        functionColor = Color(0xFF50FA7B),
        numberColor = Color(0xFFBD93F9),
        commentColor = Color(0xFF6272A4),
        decoratorColor = Color(0xFFFFB86C),
        operatorColor = Color(0xFFFF79C6),
        variableColor = Color(0xFFF8F8F2)
    )

    val Monokai = SyntaxTheme(
        id = "monokai",
        name = "Monokai Retro",
        editorBackground = Color(0xFF191919),
        lineNumbersColor = Color(0xFF75715E),
        cursorColor = Color(0xFFF8F8F0),
        keywordColor = Color(0xFFF92672),
        stringColor = Color(0xFFE6DB74),
        functionColor = Color(0xFFA6E22E),
        numberColor = Color(0xFFAE81FF),
        commentColor = Color(0xFF75715E),
        decoratorColor = Color(0xFF66D9EF),
        operatorColor = Color(0xFFF92672),
        variableColor = Color(0xFFF8F8F2)
    )

    val Cyberpunk = SyntaxTheme(
        id = "cyberpunk",
        name = "Cyberpunk Neon",
        editorBackground = Color(0xFF0F111A),
        lineNumbersColor = Color(0xFF495166),
        cursorColor = Color(0xFF00FFFF),
        keywordColor = Color(0xFFFF007F),
        stringColor = Color(0xFF00FFA3),
        functionColor = Color(0xFF00E5FF),
        numberColor = Color(0xFFFFD600),
        commentColor = Color(0xFF495166),
        decoratorColor = Color(0xFFB388FF),
        operatorColor = Color(0xFFFF5252),
        variableColor = Color(0xFFE0E6ED)
    )

    val Nord = SyntaxTheme(
        id = "nord",
        name = "Nord Frost",
        editorBackground = Color(0xFF2E3440),
        lineNumbersColor = Color(0xFF4C566A),
        cursorColor = Color(0xFF88C0D0),
        keywordColor = Color(0xFF81A1C1),
        stringColor = Color(0xFFA3BE8C),
        functionColor = Color(0xFF88C0D0),
        numberColor = Color(0xFFB48EAD),
        commentColor = Color(0xFF616E88),
        decoratorColor = Color(0xFFD08770),
        operatorColor = Color(0xFF81A1C1),
        variableColor = Color(0xFFECEFF4)
    )

    val allThemes = listOf(OneDark, Dracula, Monokai, Cyberpunk, Nord)
    
    fun getById(id: String): SyntaxTheme = allThemes.find { it.id == id } ?: OneDark
}

data class EditorSettings(
    val themeId: String = "one_dark",
    val fontSizeSp: Int = 14,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val tabSize: Int = 4,
    val autoIndent: Boolean = true
)
