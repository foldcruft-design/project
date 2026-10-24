package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CreateFileDialog(
    onConfirm: (name: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("blank") }

    val templates = listOf(
        Triple("blank", "Пустой файл", "# Модуль проекта\n"),
        Triple(
            "handler",
            "Модуль обработчиков (handlers.py)",
            "# Обработчики событий и команд\nfrom config import BOT_NAME\n\ndef handle_event(event_type, data):\n    print(f'Event: {event_type}')\n    return {'status': 'processed'}\n"
        ),
        Triple(
            "keyboards",
            "Модуль клавиатур (keyboards.py)",
            "# Генератор кнопок и меню\ndef get_menu():\n    return [\n        [{'text': '⚡ Старт', 'callback_data': 'start'}],\n        [{'text': 'ℹ️ Помощь', 'callback_data': 'help'}]\n    ]\n"
        ),
        Triple(
            "config",
            "Модуль настроек (config.py)",
            "# Настройки проекта\nimport os\n\nTOKEN = os.getenv('BOT_TOKEN', 'dev_token')\nDEBUG = True\n"
        ),
        Triple(
            "github_action",
            "GitHub Actions воркфлоу (android.yml)",
            """name: Android CI - Build APK
on: [push, workflow_dispatch]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - run: chmod +x gradlew
      - run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: APK
          path: app/build/outputs/apk/debug/*.apk
"""
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Новый файл проекта", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Укажите имя файла (например, handlers.py, utils.py или android.yml):",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("my_module.py", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_file_name_input"),
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

                Text("ШАБЛОН СОДЕРЖИМОГО:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)

                templates.forEach { (id, label, _) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTemplate == id) DarkSurfaceHighlight else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedTemplate == id) TextPrimary else DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTemplate = id
                                if (fileName.isEmpty()) {
                                    fileName = when (id) {
                                        "handler" -> "handlers.py"
                                        "keyboards" -> "keyboards.py"
                                        "config" -> "config.py"
                                        "github_action" -> "android.yml"
                                        else -> ""
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedTemplate == id,
                                onClick = { selectedTemplate = id },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TextPrimary,
                                    unselectedColor = TextMuted
                                )
                            )
                            Text(label, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = fileName.trim()
                    if (trimmed.isNotEmpty()) {
                        val chosenContent = templates.find { it.first == selectedTemplate }?.third ?: ""
                        onConfirm(trimmed, chosenContent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = DarkBg),
                enabled = fileName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_file_btn")
            ) {
                Text("Создать", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}

@Composable
fun CreateProjectDialog(
    onConfirm: (name: String, description: String, template: String) -> Unit,
    onDismiss: () -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var projectDesc by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("telegram") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Новый мультифайловый проект", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Название проекта", color = TextSecondary) },
                    placeholder = { Text("My Telegram Bot", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                OutlinedTextField(
                    value = projectDesc,
                    onValueChange = { projectDesc = it },
                    label = { Text("Описание (опционально)", color = TextSecondary) },
                    placeholder = { Text("Бот для заказов", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Text("ШАБЛОН АРХИТЕКТУРЫ:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)

                listOf(
                    Pair("telegram", "🤖 Telegram Bot (main.py + handlers.py + keyboards.py + config.py + android.yml)"),
                    Pair("fastapi", "⚡ FastAPI REST (main.py + routes.py + models.py)"),
                    Pair("blank", "📄 Чистый проект (main.py + utils.py)")
                ).forEach { (id, label) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTemplate == id) DarkSurfaceHighlight else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedTemplate == id) TextPrimary else DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTemplate = id }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedTemplate == id,
                                onClick = { selectedTemplate = id },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TextPrimary,
                                    unselectedColor = TextMuted
                                )
                            )
                            Text(label, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = projectName.trim()
                    if (trimmedName.isNotEmpty()) {
                        onConfirm(trimmedName, projectDesc.trim(), selectedTemplate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = DarkBg),
                enabled = projectName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_project_btn")
            ) {
                Text("Создать", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}
