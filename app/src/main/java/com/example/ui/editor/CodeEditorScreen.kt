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
                // COMPACT REPO & BREADCRUMBS BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clickable { viewModel.openSheet(ActiveSheet.FILE_EXPLORER) }
                            .testTag("topbar_project_chip")
                    ) {
                        Surface(
                            color = GitHubDarkCard,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.ForkRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                                Text("main", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Text("/", color = TextMuted, fontSize = 11.sp)

                        Text(
                            currentProject.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )

                        Text("/", color = TextMuted, fontSize = 11.sp)

                        if (activeFile != null) {
                            FileLanguageIcon(activeFile.name)
                            Text(
                                activeFile.name,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Compact Top Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.openSheet(ActiveSheet.FILE_EXPLORER) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Файлы", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp).testTag("overflow_menu_btn")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(GitHubDarkCard)
                                .border(1.dp, GitHubDarkBorder, RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("📦 Терминал Pip (Библиотеки)", color = TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSheet(ActiveSheet.PIP_TERMINAL)
                                },
                                leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("⚡ Терминал Выполнения", color = TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSheet(ActiveSheet.EXECUTION_TERMINAL)
                                },
                                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("🚀 GitHub Actions (.yml)", color = TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSheet(ActiveSheet.GITHUB_ACTIONS)
                                },
                                leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("🎨 Стили синтаксиса", color = TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSheet(ActiveSheet.THEMES)
                                },
                                leadingIcon = { Icon(Icons.Default.FormatPaint, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) }
                            )
                            HorizontalDivider(color = GitHubDarkBorder)
                            DropdownMenuItem(
                                text = { Text("+ Создать файл", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSheet(ActiveSheet.CREATE_FILE_DIALOG)
                                },
                                leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("+ Новый проект", color = TextSecondary, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSheet(ActiveSheet.CREATE_PROJECT_DIALOG)
                                },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = GitHubDarkBorder, thickness = 0.5.dp)

                // COMPACT TABS
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
                // COMPACT MOBILE CODING ACCESSORY BAR
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

                HorizontalDivider(color = GitHubDarkBorder, thickness = 0.5.dp)

                // COMPACT BOTTOM STATUS BAR & NAVIGATION ROW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (activeFile != null) {
                            FileLanguageIcon(activeFile.name)
                            Text(
                                "${textFieldValue.text.lines().size} строк • ${textFieldValue.text.length} B",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Compact Navigation Pills
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GitHubDarkCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder),
                            modifier = Modifier.clickable { viewModel.openSheet(ActiveSheet.PIP_TERMINAL) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("📦 pip", fontSize = 10.5.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GitHubDarkCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder),
                            modifier = Modifier.clickable { viewModel.openSheet(ActiveSheet.EXECUTION_TERMINAL) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                                Text("Терминал", fontSize = 10.5.sp, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GitHubDarkCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder),
                            modifier = Modifier.clickable { viewModel.openSheet(ActiveSheet.SNIPPETS) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                                Text("Карточки", fontSize = 10.5.sp, color = TextSecondary)
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
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Compact Line numbers gutter
                    if (settings.showLineNumbers) {
                        val lineCount = remember(textFieldValue.text) { textFieldValue.text.lines().size }

                        Column(
                            modifier = Modifier
                                .width(30.dp)
                                .fillMaxHeight()
                                .background(GitHubDarkSurface)
                                .border(width = 0.5.dp, color = GitHubDarkBorder)
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = "$i",
                                    color = currentTheme.lineNumbersColor,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 17.sp
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
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                textFieldValue = newValue
                                viewModel.updateFileContent(activeFile.id, newValue.text)
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = currentTheme.variableColor,
                                lineHeight = 17.sp
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
                        Icon(Icons.Default.NoteAdd, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Нет открытых файлов", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.openSheet(ActiveSheet.FILE_EXPLORER) },
                            colors = ButtonDefaults.buttonColors(containerColor = GitHubDarkCard, contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder)
                        ) {
                            Text("Открыть проводник файлов", fontSize = 12.sp)
                        }
                    }
                }
            }

            // REDESIGNED SLEEK GLASSMORPHIC RUN BUTTON AT BOTTOM RIGHT
            GlassmorphicRunButton(
                isRunning = isRunning,
                onClick = {
                    if (isRunning) viewModel.stopProject() else viewModel.runProject()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .testTag("run_project_btn")
            )
        }
    }

    // SHEETS & DIALOGS
    when (activeSheet) {
        ActiveSheet.EXECUTION_TERMINAL -> {
            ExecutionTerminalSheet(
                logs = executionLogs,
                isRunning = isRunning,
                onRunProject = { viewModel.runProject() },
                onStopProject = { viewModel.stopProject() },
                onClearLogs = { viewModel.clearExecutionLogs() },
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

// REDESIGNED COMPACT GLASSMORPHIC DEVELOPER RUN BUTTON
@Composable
fun GlassmorphicRunButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF0161B22),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                colors = if (isRunning) listOf(
                    Color(0xFFD29922).copy(alpha = 0.8f),
                    Color(0xFF8B5CF6).copy(alpha = 0.4f)
                ) else listOf(
                    Color(0xFF238636).copy(alpha = 0.9f),
                    Color(0xFF2EA043).copy(alpha = 0.6f)
                )
            )
        ),
        shadowElevation = 6.dp,
        modifier = modifier
            .height(36.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = Color.Black)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 1.8.dp,
                    color = Color(0xFFD29922)
                )
                Text(
                    "Стоп",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp
                )
            } else {
                Surface(
                    color = Color(0xFF238636).copy(alpha = 0.35f),
                    shape = CircleShape,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF3FB950),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    "Запуск",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
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
            .padding(horizontal = 4.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(openTabIds) { fileId ->
            val file = project.files.find { it.id == fileId }
            if (file != null) {
                val isActive = file.id == activeFileId

                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                    color = if (isActive) GitHubDarkBg else Color.Transparent,
                    border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder) else null,
                    modifier = Modifier.clickable { onSelectTab(file.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FileLanguageIcon(file.name)
                        Text(
                            text = file.name,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) TextPrimary else TextSecondary
                        )
                        if (file.isEntrypoint) {
                            Text("★", fontSize = 9.sp, color = TextMuted)
                        }
                        if (openTabIds.size > 1) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(12.dp)
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
                modifier = Modifier.size(24.dp).testTag("tabbar_add_file_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить файл", tint = TextSecondary, modifier = Modifier.size(14.dp))
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
        "_", "->", "@", "#", "def ", "import ", "return ", "for ", "in ", "if "
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(GitHubDarkCard)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = GitHubDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder),
                modifier = Modifier.clickable { onTabIndent() }
            ) {
                Text(
                    "Tab ⇥",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        items(quickSymbols) { symbol ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = GitHubDarkBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, GitHubDarkBorder),
                modifier = Modifier.clickable { onInsertSymbol(symbol) }
            ) {
                Text(
                    symbol.trim(),
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
