package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorSettings
import com.example.model.SyntaxThemes
import com.example.ui.editor.SyntaxHighlighter
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizerSheet(
    settings: EditorSettings,
    onSettingsChanged: (EditorSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val currentTheme = SyntaxThemes.getById(settings.themeId)

    val previewCode = """# Демо синтаксиса (Dark Stealth)
@bot.message(Command("start"))
def handle_user(name: str) -> dict:
    active = True
    delay_ms = 42
    print(f"User: {name}")
    return {"status": "ok"}
"""

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FormatPaint, contentDescription = null, tint = TextPrimary)
                    Text(
                        "Темные палитры кода",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_theme_sheet")) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme selector cards
            Text(
                "ТЕМНЫЕ СТИЛИ СИНТАКСИСА",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SyntaxThemes.allThemes) { theme ->
                    val isSelected = theme.id == settings.themeId
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.editorBackground)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) TextPrimary else DarkBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onSettingsChanged(settings.copy(themeId = theme.id))
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    theme.name,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Monochromatic swatches
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(theme.keywordColor))
                                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(theme.stringColor))
                                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(theme.commentColor))
                                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(theme.lineNumbersColor))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Code Preview Box
            Text(
                "ПРЕДПРОСМОТР КОДА",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(currentTheme.editorBackground)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = SyntaxHighlighter.highlight(previewCode, currentTheme),
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.fontSizeSp.sp,
                    lineHeight = (settings.fontSizeSp * 1.5f).sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Font size slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Размер шрифта", color = TextPrimary, fontSize = 14.sp)
                Text("${settings.fontSizeSp} sp", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Slider(
                value = settings.fontSizeSp.toFloat(),
                onValueChange = { onSettingsChanged(settings.copy(fontSizeSp = it.toInt())) },
                valueRange = 11f..22f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = TextPrimary,
                    activeTrackColor = TextPrimary,
                    inactiveTrackColor = DarkBorder
                ),
                modifier = Modifier.testTag("font_size_slider")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Options toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Номера строк", color = TextPrimary, fontSize = 14.sp)
                    Text("Отображать колонку нумерации слева", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.showLineNumbers,
                    onCheckedChange = { onSettingsChanged(settings.copy(showLineNumbers = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBg,
                        checkedTrackColor = TextPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Перенос строк (Word Wrap)", color = TextPrimary, fontSize = 14.sp)
                    Text("Переносить длинный код на новую строку", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.wordWrap,
                    onCheckedChange = { onSettingsChanged(settings.copy(wordWrap = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBg,
                        checkedTrackColor = TextPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
