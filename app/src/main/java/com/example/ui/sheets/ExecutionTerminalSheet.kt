package com.example.ui.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BotMessage
import com.example.engine.TelegramBotSimulator
import com.example.model.ConsoleEntry
import com.example.model.LogType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionTerminalSheet(
    logs: List<ConsoleEntry>,
    isRunning: Boolean,
    simulator: TelegramBotSimulator?,
    onRunProject: () -> Unit,
    onStopProject: () -> Unit,
    onClearLogs: () -> Unit,
    onSendBotMessage: (String) -> Unit,
    onSendBotCallback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(if (simulator != null) 1 else 0) }
    val terminalListState = rememberLazyListState()
    val botListState = rememberLazyListState()

    var userMessageInput by remember { mutableStateOf("") }

    // Auto-scroll when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            terminalListState.animateScrollToItem(logs.size - 1)
        }
    }

    // Auto-scroll when bot messages arrive
    LaunchedEffect(simulator?.messages?.size) {
        val size = simulator?.messages?.size ?: 0
        if (size > 0) {
            botListState.animateScrollToItem(size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
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
                    Surface(
                        color = if (isRunning) Color(0xFFD29922).copy(alpha = 0.2f) else Color(0xFF3FB950).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isRunning) Color(0xFFD29922) else Color(0xFF3FB950))
                    ) {
                        Icon(
                            if (isRunning) Icons.Default.Sync else Icons.Default.Terminal,
                            contentDescription = null,
                            tint = if (isRunning) Color(0xFFD29922) else Color(0xFF3FB950),
                            modifier = Modifier.padding(4.dp).size(16.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Терминал запуска программы",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            if (isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFD29922)
                                )
                            }
                        }
                        Text(
                            if (isRunning) "Выполняется python3 main.py (мультифайловый режим)" else "Процесс завершен",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRunning) {
                        IconButton(onClick = onStopProject) {
                            Icon(Icons.Default.Stop, contentDescription = "Остановить", tint = Color(0xFFF85149))
                        }
                    } else {
                        IconButton(onClick = onRunProject) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Перезапуск", tint = Color(0xFF3FB950))
                        }
                    }
                    IconButton(onClick = onClearLogs) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Очистить", tint = TextSecondary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_execution_terminal")) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab bar (GitHub Dark)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurfaceElevated,
                contentColor = TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TextPrimary
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Text("Вывод программы (${logs.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == 0) TextPrimary else TextSecondary)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Text("Чат-симулятор бота", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == 1) TextPrimary else TextSecondary)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // PROGRAM LOGS OUTPUT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBg)
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    if (logs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Вывод пуст", color = TextSecondary, fontSize = 13.sp)
                                Text("Нажмите кнопку запуска для исполнения проекта", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            state = terminalListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logs) { entry ->
                                val color = when (entry.type) {
                                    LogType.STDOUT -> TextPrimary
                                    LogType.STDERR -> Color(0xFFF85149)
                                    LogType.SYSTEM -> TextSecondary
                                    LogType.SUCCESS -> Color(0xFF3FB950)
                                    LogType.BOT_INCOMING -> Color(0xFF58A6FF)
                                    LogType.BOT_OUTGOING -> Color(0xFFD29922)
                                }
                                Text(
                                    text = entry.text,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = color
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${logs.size} строк лога • Multi-file engine active",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val allText = logs.joinToString("\n") { it.text }
                            clipboard.setPrimaryClip(ClipData.newPlainText("logs", allText))
                            Toast.makeText(context, "Логи скопированы!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Скопировать все", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            } else {
                // CHAT BOT SIMULATOR
                val messages = simulator?.messages ?: emptyList()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBg)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        LazyColumn(
                            state = botListState,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(messages) { msg ->
                                val isUser = msg.sender == "user"
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                ) {
                                    Surface(
                                        color = if (isUser) DarkSurfaceHighlight else DarkSurfaceElevated,
                                        shape = RoundedCornerShape(
                                            topStart = 10.dp,
                                            topEnd = 10.dp,
                                            bottomStart = if (isUser) 10.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 10.dp
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    if (!isUser && msg.inlineButtons.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.widthIn(max = 280.dp)
                                        ) {
                                            msg.inlineButtons.forEach { row ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    row.forEach { btn ->
                                                        FilledTonalButton(
                                                            onClick = { onSendBotCallback(btn.callbackData) },
                                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                                containerColor = DarkSurfaceElevated,
                                                                contentColor = TextPrimary
                                                            ),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                            modifier = Modifier.weight(1f).height(34.dp)
                                                        ) {
                                                            Text(
                                                                btn.text,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val quickCommands = listOf("/start", "/help", "/status", "/ping", "/calc 50*4", "Тестовое сообщение")
                        items(quickCommands) { cmd ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.clickable {
                                    onSendBotMessage(cmd)
                                }
                            ) {
                                Text(
                                    cmd,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userMessageInput,
                            onValueChange = { userMessageInput = it },
                            placeholder = { Text("Напишите боту команду...", fontSize = 12.sp, color = TextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bot_chat_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TextPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )

                        IconButton(
                            onClick = {
                                val trimmed = userMessageInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSendBotMessage(trimmed)
                                    userMessageInput = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = TextPrimary,
                                contentColor = DarkBg
                            ),
                            enabled = userMessageInput.isNotBlank(),
                            modifier = Modifier.testTag("send_bot_msg_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Отправить", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
