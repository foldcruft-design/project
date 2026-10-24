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
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IdeSurface,
        title = {
            Text("Новый файл проекта", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Укажите имя файла (например, handlers.py, utils.py или .env):",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("my_module.py") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_file_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = IdeBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("ШАБЛОН СОДЕРЖИМОГО:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)

                templates.forEach { (id, label, _) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTemplate == id) CyanAccentMuted.copy(alpha = 0.3f) else IdeSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedTemplate == id) CyanAccent else IdeBorder
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
                                colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
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
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = IdeBackground),
                enabled = fileName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_file_btn")
            ) {
                Text("Создать файл", fontWeight = FontWeight.Bold)
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
        containerColor = IdeSurface,
        title = {
            Text("Новый мультифайловый проект", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Название проекта") },
                    placeholder = { Text("My Telegram Bot") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = IdeBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = projectDesc,
                    onValueChange = { projectDesc = it },
                    label = { Text("Описание (опционально)") },
                    placeholder = { Text("Бот для заказов с разделением файлов") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = IdeBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Text("ШАБЛОН АРХИТЕКТУРЫ:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)

                listOf(
                    Pair("telegram", "🤖 Telegram Bot (main.py + handlers.py + keyboards.py + config.py)"),
                    Pair("fastapi", "⚡ FastAPI REST (main.py + routes.py + models.py)"),
                    Pair("blank", "📄 Чистый мультифайловый проект (main.py + utils.py)")
                ).forEach { (id, label) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTemplate == id) CyanAccentMuted.copy(alpha = 0.3f) else IdeSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedTemplate == id) CyanAccent else IdeBorder
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
                                colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
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
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = IdeBackground),
                enabled = projectName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_project_btn")
            ) {
                Text("Создать проект", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}
