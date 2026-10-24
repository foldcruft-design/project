package com.example.ui.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.example.model.ConsoleEntry
import com.example.model.LogType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionTerminalSheet(
    logs: List<ConsoleEntry>,
    isRunning: Boolean,
    onRunProject: () -> Unit,
    onStopProject: () -> Unit,
    onClearLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val terminalListState = rememberLazyListState()

    // Auto-scroll when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            terminalListState.animateScrollToItem(logs.size - 1)
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
                .fillMaxHeight(0.72f)
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            // Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (isRunning) Color(0xFFD29922).copy(alpha = 0.2f) else Color(0xFF238636).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isRunning) Color(0xFFD29922) else Color(0xFF238636))
                    ) {
                        Icon(
                            if (isRunning) Icons.Default.Sync else Icons.Default.Terminal,
                            contentDescription = null,
                            tint = if (isRunning) Color(0xFFD29922) else Color(0xFF3FB950),
                            modifier = Modifier.padding(3.dp).size(13.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Терминал",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            if (isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = Color(0xFFD29922)
                                )
                            }
                        }
                        Text(
                            if (isRunning) "Выполняется python3..." else "Вывод программы (${logs.size} строк)",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Header Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (isRunning) {
                        IconButton(
                            onClick = onStopProject,
                            modifier = Modifier.size(28.dp).testTag("stop_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Стоп", tint = Color(0xFFF85149), modifier = Modifier.size(16.dp))
                        }
                    } else {
                        IconButton(
                            onClick = onRunProject,
                            modifier = Modifier.size(28.dp).testTag("rerun_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Запуск", tint = Color(0xFF3FB950), modifier = Modifier.size(16.dp))
                        }
                    }

                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.size(28.dp).testTag("clear_logs_btn")
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Очистить", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val allText = logs.joinToString("\n") { it.text }
                            clipboard.setPrimaryClip(ClipData.newPlainText("logs", allText))
                            Toast.makeText(context, "Логи скопированы", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp).testTag("copy_logs_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("close_execution_terminal")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Console output canvas (Compact Developer Scale)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkBg)
                    .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Терминал пуст", color = TextSecondary, fontSize = 11.5.sp)
                            Text("Нажмите кнопку Запуск для выполнения кода", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        state = terminalListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                color = color
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
