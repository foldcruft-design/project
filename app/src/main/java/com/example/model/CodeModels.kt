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
    val ObsidianDark = SyntaxTheme(
        id = "obsidian_dark",
        name = "Obsidian Dark",
        editorBackground = Color(0xFF090A0F),
        lineNumbersColor = Color(0xFF4B5563),
        cursorColor = Color(0xFFF3F4F6),
        keywordColor = Color(0xFFF9FAFB),
        stringColor = Color(0xFF9CA3AF),
        functionColor = Color(0xFFE5E7EB),
        numberColor = Color(0xFFD1D5DB),
        commentColor = Color(0xFF6B7280),
        decoratorColor = Color(0xFFE5E7EB),
        operatorColor = Color(0xFF9CA3AF),
        variableColor = Color(0xFFF3F4F6)
    )

    val JetBlackOLED = SyntaxTheme(
        id = "jet_black_oled",
        name = "Jet Black OLED",
        editorBackground = Color(0xFF000000),
        lineNumbersColor = Color(0xFF404040),
        cursorColor = Color(0xFFFFFFFF),
        keywordColor = Color(0xFFFFFFFF),
        stringColor = Color(0xFFA3A3A3),
        functionColor = Color(0xFFE5E5E5),
        numberColor = Color(0xFFD4D4D4),
        commentColor = Color(0xFF525252),
        decoratorColor = Color(0xFFFFFFFF),
        operatorColor = Color(0xFFA3A3A3),
        variableColor = Color(0xFFF5F5F5)
    )

    val CharcoalStealth = SyntaxTheme(
        id = "charcoal_stealth",
        name = "Charcoal Stealth",
        editorBackground = Color(0xFF11141A),
        lineNumbersColor = Color(0xFF475569),
        cursorColor = Color(0xFFE2E8F0),
        keywordColor = Color(0xFFF8FAFC),
        stringColor = Color(0xFF94A3B8),
        functionColor = Color(0xFFCBD5E1),
        numberColor = Color(0xFFCBD5E1),
        commentColor = Color(0xFF64748B),
        decoratorColor = Color(0xFFF1F5F9),
        operatorColor = Color(0xFF94A3B8),
        variableColor = Color(0xFFE2E8F0)
    )

    val GraphiteMinimal = SyntaxTheme(
        id = "graphite_minimal",
        name = "Graphite Minimal",
        editorBackground = Color(0xFF151821),
        lineNumbersColor = Color(0xFF4B5563),
        cursorColor = Color(0xFFF9FAFB),
        keywordColor = Color(0xFFFFFFFF),
        stringColor = Color(0xFFA1A1AA),
        functionColor = Color(0xFFE4E4E7),
        numberColor = Color(0xFFD4D4D8),
        commentColor = Color(0xFF71717A),
        decoratorColor = Color(0xFFF4F4F5),
        operatorColor = Color(0xFFA1A1AA),
        variableColor = Color(0xFFFAFAFA)
    )

    val SlateDark = SyntaxTheme(
        id = "slate_dark",
        name = "Slate Dark",
        editorBackground = Color(0xFF0E1117),
        lineNumbersColor = Color(0xFF475569),
        cursorColor = Color(0xFFF8FAFC),
        keywordColor = Color(0xFFF1F5F9),
        stringColor = Color(0xFF94A3B8),
        functionColor = Color(0xFFE2E8F0),
        numberColor = Color(0xFFCBD5E1),
        commentColor = Color(0xFF64748B),
        decoratorColor = Color(0xFFF8FAFC),
        operatorColor = Color(0xFF94A3B8),
        variableColor = Color(0xFFF1F5F9)
    )

    val allThemes = listOf(ObsidianDark, JetBlackOLED, CharcoalStealth, GraphiteMinimal, SlateDark)
    
    fun getById(id: String): SyntaxTheme = allThemes.find { it.id == id } ?: ObsidianDark
}

data class EditorSettings(
    val themeId: String = "obsidian_dark",
    val fontSizeSp: Int = 14,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val tabSize: Int = 4,
    val autoIndent: Boolean = true
)
