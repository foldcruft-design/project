package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.FileLanguageIcon
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
    val executionLogs by viewModel.executionLogs.collectAsState()
    val pipLogs by viewModel.pipTerminalLogs.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val activeSimulator by viewModel.activeSimulator.collectAsState()
    val activeSheet by viewModel.activeSheet.collectAsState()

    val currentTheme = SyntaxThemes.getById(settings.themeId)
    var showMenu by remember { mutableStateOf(false) }

    var textFieldValue by remember(activeFile?.id) {
        mutableStateOf(TextFieldValue(activeFile?.content ?: ""))
    }

    LaunchedEffect(activeFile?.content) {
        val externalContent = activeFile?.content ?: ""
        if (textFieldValue.text != externalContent) {
            textFieldValue = textFieldValue.copy(text = externalContent)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(GitHubDarkSurface)) {
                // GITHUB REPO & BREADCRUMBS BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clickable { viewModel.openSheet(ActiveSheet.FILE_EXPLORER) }
                            .testTag("topbar_project_chip")
                    ) {
                        // GitHub Octocat / Branch pill
                        Surface(
                            color = GitHubDarkCard,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ForkRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                Text("main", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Text("/", color = TextMuted, fontSize = 13.sp)

                        Text(
                            currentProject.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )

                        Text("/", color = TextMuted, fontSize = 13.sp)

                        if (activeFile != null) {
                            FileLanguageIcon(activeFile.name)
                            Text(
                                activeFile.name,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Top Action Icons (GitHub Raw / Settings / Menu)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.openSheet(ActiveSheet.FILE_EXPLORER) }) {
                            Icon(Icons.Default.Folder, contentDescription = "Файлы", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.testTag("overflow_menu_btn")) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(GitHubDarkCard)
                                .border(1.dp, GitHubDarkBorder, RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("📦 Терминал Pip (Библиотеки)", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSheet(ActiveSheet.PIP_TERMINAL)
                                },
                                leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null, tint = TextSecondary) }
                            )
                            DropdownMenuItem(
                                text = { Text("⚡ Терминал Выполнения (Бот)", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSheet(ActiveSheet.EXECUTION_TERMINAL)
                                },
                                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, tint = TextSecondary) }
                            )
                            DropdownMenuItem(
                                text = { Text("🚀 GitHub Actions (.yml)", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSheet(ActiveSheet.GITHUB_ACTIONS)
                                },
                                leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = TextSecondary) }
                            )
                            DropdownMenuItem(
                                text = { Text("🎨 Стили синтаксиса", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSheet(ActiveSheet.THEMES)
                                },
                                leadingIcon = { Icon(Icons.Default.FormatPaint, contentDescription = null, tint = TextSecondary) }
                            )
                            HorizontalDivider(color = GitHubDarkBorder)
                            DropdownMenuItem(
                                text = { Text("+ Создать файл (пустой)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
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
                    }
                }

                HorizontalDivider(color = GitHubDarkBorder)

                // GITHUB MULTI-FILE TABS
                MultiFileTabBar(
                    project = currentProject,
                    openTabIds = openTabIds,
                    activeFileId = activeFileId,
                    onSelectTab = { fileId -> viewModel.openFile(fileId) },
                    onCloseTab = { fileId -> viewModel.closeTab(fileId) },
                    onAddNewFile = { viewModel.openSheet(ActiveSheet.CREATE_FILE_DIALOG) }
                )
            }
        },
        bottomBar = {
            Column(modifier = Modifier.background(GitHubDarkSurface)) {
                // MOBILE CODING ACCESSORY BAR
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
                        val indent = "    "
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

                HorizontalDivider(color = GitHubDarkBorder)

                // BOTTOM STATUS BAR & NAVIGATION ROW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (activeFile != null) {
                            FileLanguageIcon(activeFile.name)
                            Text(
                                "${textFieldValue.text.lines().size} строк • ${textFieldValue.text.length} B",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Bottom Navigation Buttons
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Dedicated Pip Terminal button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GitHubDarkCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder),
                            modifier = Modifier.clickable { viewModel.openSheet(ActiveSheet.PIP_TERMINAL) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("📦 pip", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Execution Terminal button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GitHubDarkCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder),
                            modifier = Modifier.clickable { viewModel.openSheet(ActiveSheet.EXECUTION_TERMINAL) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                Text("Терминал", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        // Snippet Cards
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GitHubDarkCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder),
                            modifier = Modifier.clickable { viewModel.openSheet(ActiveSheet.SNIPPETS) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                Text("Карточки", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        containerColor = currentTheme.editorBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // GITHUB CODE EDITOR CANVAS
            if (activeFile != null) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Line numbers gutter (GitHub Dark)
                    if (settings.showLineNumbers) {
                        val lineCount = remember(textFieldValue.text) { textFieldValue.text.lines().size }

                        Column(
                            modifier = Modifier
                                .width(38.dp)
                                .fillMaxHeight()
                                .background(GitHubDarkSurface)
                                .border(width = 0.5.dp, color = GitHubDarkBorder)
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
                            colors = ButtonDefaults.buttonColors(containerColor = GitHubDarkCard, contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder)
                        ) {
                            Text("Открыть проводник файлов")
                        }
                    }
                }
            }

            // GLASSMORPHISM FLOATING RUN BUTTON AT BOTTOM RIGHT
            GlassmorphicRunButton(
                isRunning = isRunning,
                onClick = {
                    if (isRunning) viewModel.stopProject() else viewModel.runProject()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .testTag("run_project_btn")
            )
        }
    }

    // SHEETS & DIALOGS
    when (activeSheet) {
        ActiveSheet.EXECUTION_TERMINAL, ActiveSheet.BOT_SIMULATOR -> {
            ExecutionTerminalSheet(
                logs = executionLogs,
                isRunning = isRunning,
                simulator = activeSimulator,
                onRunProject = { viewModel.runProject() },
                onStopProject = { viewModel.stopProject() },
                onClearLogs = { viewModel.clearExecutionLogs() },
                onSendBotMessage = { viewModel.sendBotMessage(it) },
                onSendBotCallback = { viewModel.sendBotCallback(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.PIP_TERMINAL -> {
            PipTerminalSheet(
                logs = pipLogs,
                installedPackages = currentProject.installedPackages,
                onRunPipCommand = { viewModel.runPipCommand(it) },
                onInstallPackage = { viewModel.installPackage(it) },
                onClearLogs = { viewModel.clearPipLogs() },
                onDismiss = { viewModel.closeSheet() }
            )
        }
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

// GLASSMORPHISM RUN BUTTON AT THE BOTTOM
@Composable
fun GlassmorphicRunButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = GlassBackgroundElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.12f)
                )
            )
        ),
        shadowElevation = 8.dp,
        modifier = modifier
            .height(46.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black, spotColor = Color.Black)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFFD29922)
                )
                Text(
                    "Остановить",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            } else {
                Surface(
                    color = Color(0xFF3FB950).copy(alpha = 0.25f),
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF3FB950),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    "Запустить",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
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
            .background(GitHubDarkSurface)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(openTabIds) { fileId ->
            val file = project.files.find { it.id == fileId }
            if (file != null) {
                val isActive = file.id == activeFileId

                Surface(
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                    color = if (isActive) GitHubDarkBg else Color.Transparent,
                    border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder) else null,
                    modifier = Modifier.clickable { onSelectTab(file.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FileLanguageIcon(file.name)
                        Text(
                            text = file.name,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) TextPrimary else TextSecondary
                        )
                        if (file.isEntrypoint) {
                            Text("★", fontSize = 10.sp, color = TextMuted)
                        }
                        if (openTabIds.size > 1) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Закрыть",
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
            .background(GitHubDarkCard)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = GitHubDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder),
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
                color = GitHubDarkBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder),
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
