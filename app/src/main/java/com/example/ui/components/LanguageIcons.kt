package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class LanguageBadge(
    val label: String,
    val iconColor: Color,
    val textColor: Color = Color.White
)

object FileTypeHelper {

    fun getLanguageBadge(fileName: String): LanguageBadge {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".py") -> LanguageBadge("Py", LangPython)
            lower.endsWith(".java") -> LanguageBadge("Java", LangJava)
            lower.endsWith(".cpp") || lower.endsWith(".hpp") || lower.endsWith(".cc") -> LanguageBadge("C++", LangCpp)
            lower.endsWith(".c") || lower.endsWith(".h") -> LanguageBadge("C", LangC)
            lower.endsWith(".js") -> LanguageBadge("JS", LangJs, Color.Black)
            lower.endsWith(".ts") -> LanguageBadge("TS", LangTs)
            lower.endsWith(".json") -> LanguageBadge("{}", LangJson, Color.Black)
            lower.endsWith(".yml") || lower.endsWith(".yaml") -> LanguageBadge("YAML", LangYaml)
            lower.endsWith(".sh") || lower.endsWith(".bash") -> LanguageBadge("$", LangShell, Color.Black)
            lower.endsWith(".md") -> LanguageBadge("MD", LangMarkdown)
            lower == "requirements.txt" -> LanguageBadge("pip", Color(0xFF3872A5))
            lower == ".env" -> LanguageBadge(".env", Color(0xFF6E7681))
            else -> {
                val ext = fileName.substringAfterLast(".", "TXT").uppercase().take(3)
                LanguageBadge(ext, Color(0xFF484F58))
            }
        }
    }
}

@Composable
fun FileLanguageIcon(
    fileName: String,
    modifier: Modifier = Modifier
) {
    val badge = FileTypeHelper.getLanguageBadge(fileName)
    Box(
        modifier = modifier
            .size(15.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(badge.iconColor.copy(alpha = 0.25f))
            .border(0.8.dp, badge.iconColor.copy(alpha = 0.7f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badge.label,
            fontSize = if (badge.label.length > 2) 6.sp else 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = badge.iconColor
        )
    }
}
