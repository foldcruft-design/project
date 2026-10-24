package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.CodeFile
import com.example.model.Project
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerSheet(
    project: Project,
    allProjects: List<Project>,
    activeFileId: String,
    onSelectFile: (String) -> Unit,
    onSetEntrypoint: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onSwitchProject: (String) -> Unit,
    onCreateNewFileClicked: () -> Unit,
    onCreateNewProjectClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    var showProjectDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = TextPrimary)
                    Column {
                        Text(
                            "Проводник файлов",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Мультифайловая структура проекта",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_file_explorer")) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Project Selector Card
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProjectDropdown = true }
                    .testTag("project_selector_btn")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("АКТИВНЫЙ ПРОЕКТ", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text(project.name, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(project.description, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                    }
                    Icon(Icons.Default.UnfoldMore, contentDescription = null, tint = TextSecondary)
                }
            }

            // Dropdown menu for switching projects
            DropdownMenu(
                expanded = showProjectDropdown,
                onDismissRequest = { showProjectDropdown = false },
                modifier = Modifier
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            ) {
                allProjects.forEach { p ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(p.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("${p.files.size} файлов", color = TextSecondary, fontSize = 11.sp)
                            }
                        },
                        onClick = {
                            onSwitchProject(p.id)
                            showProjectDropdown = false
                        },
                        leadingIcon = {
                            Icon(
                                if (p.id == project.id) Icons.Default.CheckCircle else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (p.id == project.id) TextPrimary else TextMuted
                            )
                        }
                    )
                }
                HorizontalDivider(color = DarkBorder)
                DropdownMenuItem(
                    text = { Text("+ Создать новый проект", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    onClick = {
                        showProjectDropdown = false
                        onCreateNewProjectClicked()
                    },
                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = TextPrimary) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions row: New file button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ФАЙЛЫ ПРОЕКТА (${project.files.size})",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onCreateNewFileClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = DarkBg),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("add_file_btn")
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Новый файл", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Files list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(project.files) { file ->
                    val isActive = file.id == activeFileId
                    val icon = when {
                        file.isPython -> Icons.Default.Code
                        file.isEnv -> Icons.Default.Lock
                        file.isRequirements -> Icons.Default.ListAlt
                        else -> Icons.Default.Description
                    }

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) DarkSurfaceHighlight else DarkSurfaceElevated
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isActive) TextSecondary else DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectFile(file.id)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                com.example.ui.components.FileLanguageIcon(file.name)
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            file.name,
                                            color = TextPrimary,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp
                                        )
                                        if (file.isEntrypoint) {
                                            Surface(
                                                color = DarkSurfaceHighlight,
                                                shape = RoundedCornerShape(4.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                            ) {
                                                Text(
                                                    "★ main",
                                                    color = TextPrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        "${file.content.lines().size} строк • ${file.content.length} байт",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!file.isEntrypoint && file.isPython) {
                                    IconButton(
                                        onClick = { onSetEntrypoint(file.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.StarBorder,
                                            contentDescription = "Сделать точкой входа",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (project.files.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteFile(file.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Удалить файл",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
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
