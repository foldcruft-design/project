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
fun PipTerminalSheet(
    logs: List<ConsoleEntry>,
    installedPackages: List<String>,
    onRunPipCommand: (String) -> Unit,
    onInstallPackage: (String) -> Unit,
    onClearLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputCommand by remember { mutableStateOf("") }

    // Auto-scroll on new pip log
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val quickPackages = listOf("aiogram", "python-telegram-bot", "fastapi", "uvicorn", "requests", "aiohttp", "pydantic", "colorama")

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
                        color = Color(0xFF3572A5).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3572A5))
                    ) {
                        Text(
                            "pip",
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Column {
                        Text(
                            "Терминал библиотек (Pip)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Установлено: ${installedPackages.size} пакетов в virtualenv",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClearLogs) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Очистить", tint = TextSecondary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_pip_terminal")) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick pip install chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurfaceHighlight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.clickable { onRunPipCommand("pip list") }
                    ) {
                        Text(
                            "📋 pip list",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                items(quickPackages) { pkg ->
                    val isInstalled = installedPackages.contains(pkg)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isInstalled) DarkSurfaceElevated else DarkSurfaceHighlight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isInstalled) Color(0xFF3FB950) else DarkBorder),
                        modifier = Modifier.clickable { onInstallPackage(pkg) }
                    ) {
                        Text(
                            if (isInstalled) "✓ $pkg" else "+ $pkg",
                            fontSize = 11.sp,
                            color = if (isInstalled) Color(0xFF3FB950) else TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pip Terminal Output Canvas
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
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(logs) { entry ->
                        val color = when (entry.type) {
                            LogType.SUCCESS -> Color(0xFF3FB950)
                            LogType.STDERR -> Color(0xFFF85149)
                            LogType.STDOUT -> TextPrimary
                            LogType.SYSTEM -> TextSecondary
                            else -> TextSecondary
                        }
                        Text(
                            text = entry.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Command input row: `pip install <pkg>`
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    placeholder = { Text("pip install <библиотека>", fontSize = 12.sp, color = TextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pip_terminal_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        val trimmed = inputCommand.trim()
                        if (trimmed.isNotEmpty()) {
                            val cmd = if (!trimmed.startsWith("pip")) "pip install $trimmed" else trimmed
                            onRunPipCommand(cmd)
                            inputCommand = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = DarkBg),
                    shape = RoundedCornerShape(8.dp),
                    enabled = inputCommand.isNotBlank(),
                    modifier = Modifier.testTag("pip_terminal_submit_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Выполнить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
