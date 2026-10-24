package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SyntaxThemes
import com.example.snippets.SnippetCard
import com.example.snippets.SnippetLibrary
import com.example.ui.editor.SyntaxHighlighter
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetCardsSheet(
    activeFileName: String,
    onInsertSnippet: (code: String, targetFileName: String) -> Unit,
    onCreateAsNewFile: (fileName: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Все") }
    var searchQuery by remember { mutableStateOf("") }
    var showGenerateDialog by remember { mutableStateOf(false) }

    // User-generated dynamic cards
    var customCards by remember { mutableStateOf<List<SnippetCard>>(emptyList()) }

    val allCards = remember(customCards) {
        customCards + SnippetLibrary.cards
    }

    val categories = listOf("Все", "Telegram Handlers", "Keyboards", "Security", "Database", "Config", "FastAPI & Web")

    val filteredCards = remember(searchQuery, selectedCategory, allCards) {
        allCards.filter { card ->
            val matchesCategory = selectedCategory == "Все" || card.category == selectedCategory
            val matchesSearch = card.title.contains(searchQuery, ignoreCase = true) ||
                    card.description.contains(searchQuery, ignoreCase = true) ||
                    card.code.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val defaultTheme = SyntaxThemes.OneDark

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IdeSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = IdeBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = CyanAccent)
                    Column {
                        Text(
                            "Карточки готового кода",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Вставка готовых функций в бот и файлы проекта",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_snippets_sheet")) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: Generate custom card + Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск сниппетов...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = IdeBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = { showGenerateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, contentColor = IdeBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("generate_custom_card_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Создать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Categories row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccentMuted,
                            selectedLabelColor = TextPrimary,
                            containerColor = IdeSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) CyanAccent else IdeBorder,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cards list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredCards) { card ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = IdeSurfaceVariant),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(IdeBorder)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Title row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    card.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = IdeBackground,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder)
                                ) {
                                    Text(
                                        card.targetFileSuggestion,
                                        color = CyanAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                card.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Code preview box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(defaultTheme.editorBackground)
                                    .border(1.dp, IdeBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = SyntaxHighlighter.highlight(card.code.trim(), defaultTheme),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onInsertSnippet(card.code, card.targetFileSuggestion) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanAccent,
                                        contentColor = IdeBackground
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("insert_card_${card.id}")
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Вставить в код", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onCreateAsNewFile(card.targetFileSuggestion, card.code) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(IdeBorder)
                                    )
                                ) {
                                    Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = IndigoAccent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Создать файл", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for creating custom snippet card
    if (showGenerateDialog) {
        var cardTitle by remember { mutableStateOf("") }
        var cardCategory by remember { mutableStateOf("Telegram Handlers") }
        var cardTargetFile by remember { mutableStateOf("handlers.py") }
        var cardSnippet by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            containerColor = IdeSurface,
            title = {
                Text("Генератор карточки кода", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Создайте готовую карточку для быстрой вставки:", color = TextSecondary, fontSize = 13.sp)

                    OutlinedTextField(
                        value = cardTitle,
                        onValueChange = { cardTitle = it },
                        label = { Text("Название карточки (напр. Обработчик фото)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = IdeBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = cardTargetFile,
                        onValueChange = { cardTargetFile = it },
                        label = { Text("Целевой файл (напр. handlers.py)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = IdeBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = cardSnippet,
                        onValueChange = { cardSnippet = it },
                        label = { Text("Python код карточки") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = IdeBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cardTitle.isNotBlank() && cardSnippet.isNotBlank()) {
                            val newCard = SnippetCard(
                                id = "custom_" + System.currentTimeMillis(),
                                title = cardTitle,
                                description = "Пользовательский сниппет для $cardTargetFile",
                                category = cardCategory,
                                targetFileSuggestion = cardTargetFile,
                                code = cardSnippet
                            )
                            customCards = listOf(newCard) + customCards
                            showGenerateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = IdeBackground)
                ) {
                    Text("Сохранить карточку", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }
}
