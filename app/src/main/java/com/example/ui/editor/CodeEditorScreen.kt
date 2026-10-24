package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CodeFile
import com.example.model.EditorSettings
import com.example.model.Project
import com.example.model.SyntaxThemes
import com.example.ui.dialogs.CreateFileDialog
import com.example.ui.dialogs.CreateProjectDialog
import com.example.ui.sheets.*
import com.example.ui.theme.*
import com.example.viewmodel.ActiveSheet
import com.example.viewmodel.CodeStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    viewModel: CodeStudioViewModel
) {
    val projects by viewModel.projects.collectAsState()
    val currentProject = viewModel.currentProject
    val activeFileId by viewModel.activeFileId.collectAsState()
    val openTabIds by viewModel.openTabIds.collectAsState()
    val activeFile = viewModel.activeFile
    val settings by viewModel.editorSettings.collectAsState()
    val logs by viewModel.consoleLogs.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val activeSimulator by viewModel.activeSimulator.collectAsState()
    val activeSheet by viewModel.activeSheet.collectAsState()

    val currentTheme = SyntaxThemes.getById(settings.themeId)

    var showMenu by remember { mutableStateOf(false) }

    // Text field value tracking for cursor positioning
    var textFieldValue by remember(activeFile?.id) {
        mutableStateOf(TextFieldValue(activeFile?.content ?: ""))
    }

    // Keep text in sync if external updates occur
    LaunchedEffect(activeFile?.content) {
        val externalContent = activeFile?.content ?: ""
        if (textFieldValue.text != externalContent) {
            textFieldValue = textFieldValue.copy(text = externalContent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clickable { viewModel.openSheet(ActiveSheet.FILE_EXPLORER) }
                            .testTag("topbar_project_chip")
                    ) {
                        Surface(
                            color = DarkSurfaceHighlight,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    currentProject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                "${currentProject.files.size} файлов • ${currentTheme.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // RUN / STOP BUTTON (Stealth Dark Monochrome)
                    FilledTonalButton(
                        onClick = { viewModel.runProject() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isRunning) DarkSurfaceHighlight else DarkSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isRunning) TextSecondary else DarkBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp).testTag("run_project_btn")
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Стоп", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Запуск", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // FILE EXPLORER ICON
                    IconButton(
                        onClick = { viewModel.openSheet(ActiveSheet.FILE_EXPLORER) },
                        modifier = Modifier.testTag("open_explorer_btn")
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Файлы", tint = TextSecondary)
                    }

                    // OVERFLOW MENU
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("overflow_menu_btn")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = TextSecondary)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Библиотеки (pip)", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.openSheet(ActiveSheet.PACKAGES)
                            },
                            leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null, tint = TextSecondary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Карточки кода (Сниппеты)", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.openSheet(ActiveSheet.SNIPPETS)
                            },
                            leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, tint = TextSecondary) }
                        )
                        DropdownMenuItem(
                            text = { Text("GitHub Actions (Сборка APK)", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.openSheet(ActiveSheet.GITHUB_ACTIONS)
                            },
                            leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = TextSecondary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Темные темы кода", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.openSheet(ActiveSheet.THEMES)
                            },
                            leadingIcon = { Icon(Icons.Default.FormatPaint, contentDescription = null, tint = TextSecondary) }
                        )
                        HorizontalDivider(color = DarkBorder)
                        DropdownMenuItem(
                            text = { Text("+ Создать файл", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.openSheet(ActiveSheet.CREATE_FILE_DIALOG)
                            },
                            leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null, tint = TextPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("+ Создать проект", color = TextSecondary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.openSheet(ActiveSheet.CREATE_PROJECT_DIALOG)
                            },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = TextSecondary) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(DarkSurface)) {
                // MOBILE CODING ACCESSORY BAR (Stealth Dark)
                MobileCodingToolbar(
                    onInsertSymbol = { symbol ->
                        val currentText = textFieldValue.text
                        val selection = textFieldValue.selection
                        val newText = currentText.substring(0, selection.min) + symbol + currentText.substring(selection.max)
                        val newCursor = selection.min + symbol.length
                        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
                        if (activeFile != null) {
                            viewModel.updateFileContent(activeFile.id, newText)
                        }
                    },
                    onTabIndent = {
                        val indent = "    " // 4 spaces
                        val currentText = textFieldValue.text
                        val selection = textFieldValue.selection
                        val newText = currentText.substring(0, selection.min) + indent + currentText.substring(selection.max)
                        val newCursor = selection.min + indent.length
                        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
                        if (activeFile != null) {
                            viewModel.updateFileContent(activeFile.id, newText)
                        }
                    }
                )

                HorizontalDivider(color = DarkBorder)

                // BOTTOM STATUS BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            activeFile?.name ?: "No file",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${textFieldValue.text.lines().size} строк",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Snippets quick trigger
                        FilledTonalButton(
                            onClick = { viewModel.openSheet(ActiveSheet.SNIPPETS) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = DarkSurfaceElevated,
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("quick_snippets_btn")
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(12.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Карточки", fontSize = 11.sp, fontWeight = FontWeight.Normal)
                        }

                        // Terminal console trigger
                        FilledTonalButton(
                            onClick = { viewModel.openSheet(ActiveSheet.CONSOLE) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (logs.isNotEmpty()) DarkSurfaceHighlight else DarkSurfaceElevated,
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (logs.isNotEmpty()) TextSecondary else DarkBorder),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("quick_console_btn")
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(12.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Консоль (${logs.size})", fontSize = 11.sp, fontWeight = FontWeight.Normal)
                        }
                    }
                }
            }
        },
        containerColor = currentTheme.editorBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // MULTI-FILE TAB BAR
            MultiFileTabBar(
                project = currentProject,
                openTabIds = openTabIds,
                activeFileId = activeFileId,
                onSelectTab = { fileId ->
                    viewModel.openFile(fileId)
                },
                onCloseTab = { fileId ->
                    viewModel.closeTab(fileId)
                },
                onAddNewFile = {
                    viewModel.openSheet(ActiveSheet.CREATE_FILE_DIALOG)
                }
            )

            HorizontalDivider(color = DarkBorder)

            // CODE EDITOR CANVAS
            if (activeFile != null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Line numbers gutter
                    if (settings.showLineNumbers) {
                        val lineCount = remember(textFieldValue.text) { textFieldValue.text.lines().size }

                        Column(
                            modifier = Modifier
                                .width(38.dp)
                                .fillMaxHeight()
                                .background(DarkSurface)
                                .border(width = 0.5.dp, color = DarkBorder)
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = "$i",
                                    color = currentTheme.lineNumbersColor,
                                    fontSize = (settings.fontSizeSp - 2).sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = (settings.fontSizeSp * 1.5f).sp
                                )
                            }
                        }
                    }

                    // Code Editor Text Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(currentTheme.editorBackground)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                textFieldValue = newValue
                                viewModel.updateFileContent(activeFile.id, newValue.text)
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = settings.fontSizeSp.sp,
                                color = currentTheme.variableColor,
                                lineHeight = (settings.fontSizeSp * 1.5f).sp
                            ),
                            cursorBrush = SolidColor(currentTheme.cursorColor),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .then(
                                    if (!settings.wordWrap) Modifier.horizontalScroll(rememberScrollState())
                                    else Modifier
                                )
                                .testTag("code_editor_field")
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(currentTheme.editorBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Нет открытых файлов", color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.openSheet(ActiveSheet.FILE_EXPLORER) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Text("Открыть проводник")
                        }
                    }
                }
            }
        }
    }

    // SHEETS & DIALOGS
    when (activeSheet) {
        ActiveSheet.PACKAGES -> {
            PackageManagerSheet(
                installedPackages = currentProject.installedPackages,
                onInstallPackage = { pkg -> viewModel.installPackage(pkg) },
                onUninstallPackage = { pkg -> viewModel.uninstallPackage(pkg) },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.THEMES -> {
            ThemeCustomizerSheet(
                settings = settings,
                onSettingsChanged = { viewModel.updateSettings(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.SNIPPETS -> {
            SnippetCardsSheet(
                activeFileName = activeFile?.name ?: "main.py",
                onInsertSnippet = { code, target -> viewModel.insertSnippet(code, target) },
                onCreateAsNewFile = { name, content -> viewModel.createFile(name, content) },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.GITHUB_ACTIONS -> {
            GitHubActionsSheet(
                onAddWorkflowToProject = { viewModel.addGitHubWorkflowToProject() },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.FILE_EXPLORER -> {
            FileExplorerSheet(
                project = currentProject,
                allProjects = projects,
                activeFileId = activeFileId,
                onSelectFile = { viewModel.openFile(it) },
                onSetEntrypoint = { viewModel.setAsEntrypoint(it) },
                onDeleteFile = { viewModel.deleteFile(it) },
                onSwitchProject = { viewModel.switchProject(it) },
                onCreateNewFileClicked = { viewModel.openSheet(ActiveSheet.CREATE_FILE_DIALOG) },
                onCreateNewProjectClicked = { viewModel.openSheet(ActiveSheet.CREATE_PROJECT_DIALOG) },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.CONSOLE, ActiveSheet.BOT_SIMULATOR -> {
            ExecutionConsoleSheet(
                logs = logs,
                isRunning = isRunning,
                simulator = activeSimulator,
                onRunProject = { viewModel.runProject() },
                onClearLogs = { viewModel.clearConsole() },
                onSendBotMessage = { viewModel.sendBotMessage(it) },
                onSendBotCallback = { viewModel.sendBotCallback(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.CREATE_FILE_DIALOG -> {
            CreateFileDialog(
                onConfirm = { name, content -> viewModel.createFile(name, content) },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.CREATE_PROJECT_DIALOG -> {
            CreateProjectDialog(
                onConfirm = { name, desc, tpl -> viewModel.createProject(name, desc, tpl) },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        null -> {}
    }
}

@Composable
fun MultiFileTabBar(
    project: Project,
    openTabIds: List<String>,
    activeFileId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onAddNewFile: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(openTabIds) { fileId ->
            val file = project.files.find { it.id == fileId }
            if (file != null) {
                val isActive = file.id == activeFileId

                Surface(
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    color = if (isActive) DarkSurfaceElevated else Color.Transparent,
                    border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, DarkBorder) else null,
                    modifier = Modifier.clickable { onSelectTab(file.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (isActive) TextPrimary else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = file.name,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) TextPrimary else TextSecondary
                        )
                        if (file.isEntrypoint) {
                            Text("★", fontSize = 10.sp, color = TextSecondary)
                        }
                        if (openTabIds.size > 1) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Закрыть вкладку",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onCloseTab(file.id) }
                            )
                        }
                    }
                }
            }
        }

        item {
            IconButton(
                onClick = onAddNewFile,
                modifier = Modifier.size(28.dp).testTag("tabbar_add_file_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить файл", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun MobileCodingToolbar(
    onInsertSymbol: (String) -> Unit,
    onTabIndent: () -> Unit
) {
    val quickSymbols = listOf(
        ":", "(", ")", "[", "]", "{", "}", "\"", "'", "=",
        "_", "->", "@", "#", "def ", "class ", "import ", "async ", "await ", "return "
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceElevated)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab key
        item {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = DarkSurfaceHighlight,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.clickable { onTabIndent() }
            ) {
                Text(
                    "Tab ⇥",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        items(quickSymbols) { symbol ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = DarkBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.clickable { onInsertSymbol(symbol) }
            ) {
                Text(
                    symbol.trim(),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
