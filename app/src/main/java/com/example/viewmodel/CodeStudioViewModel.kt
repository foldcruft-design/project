package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultProjects
import com.example.engine.MultiFileExecutor
import com.example.github.GitHubActionsHub
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class ActiveSheet {
    PACKAGES,
    THEMES,
    SNIPPETS,
    GITHUB_ACTIONS,
    FILE_EXPLORER,
    EXECUTION_TERMINAL,
    PIP_TERMINAL,
    CREATE_FILE_DIALOG,
    CREATE_PROJECT_DIALOG
}

class CodeStudioViewModel : ViewModel() {

    private val executor = MultiFileExecutor()

    private val _projects = MutableStateFlow<List<Project>>(DefaultProjects.defaultProjects)
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _currentProjectId = MutableStateFlow(DefaultProjects.starterProject.id)
    val currentProjectId: StateFlow<String> = _currentProjectId.asStateFlow()

    // Tabs currently open in editor (starts with exactly 1 starter file)
    private val _openTabIds = MutableStateFlow<List<String>>(
        DefaultProjects.starterProject.files.map { it.id }
    )
    val openTabIds: StateFlow<List<String>> = _openTabIds.asStateFlow()

    private val _activeFileId = MutableStateFlow(DefaultProjects.starterProject.activeFileId)
    val activeFileId: StateFlow<String> = _activeFileId.asStateFlow()

    private val _editorSettings = MutableStateFlow(EditorSettings(themeId = "github_dark", fontSizeSp = 12))
    val editorSettings: StateFlow<EditorSettings> = _editorSettings.asStateFlow()

    // Separate Terminal 1: Code Execution Logs
    private val _executionLogs = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val executionLogs: StateFlow<List<ConsoleEntry>> = _executionLogs.asStateFlow()

    // Separate Terminal 2: Pip & Libraries Installation Logs
    private val _pipTerminalLogs = MutableStateFlow<List<ConsoleEntry>>(
        listOf(
            ConsoleEntry(type = LogType.SYSTEM, text = "[PocketCode VirtualEnv] Python 3.11.8 environment active"),
            ConsoleEntry(type = LogType.SYSTEM, text = "[Pip] Run 'pip install <package>' or click quick tags to install libraries.")
        )
    )
    val pipTerminalLogs: StateFlow<List<ConsoleEntry>> = _pipTerminalLogs.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _activeSheet = MutableStateFlow<ActiveSheet?>(null)
    val activeSheet: StateFlow<ActiveSheet?> = _activeSheet.asStateFlow()

    val currentProject: Project
        get() = _projects.value.find { it.id == _currentProjectId.value }
            ?: _projects.value.first()

    val activeFile: CodeFile?
        get() = currentProject.files.find { it.id == _activeFileId.value }
            ?: currentProject.files.firstOrNull()

    fun openSheet(sheet: ActiveSheet?) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = null
    }

    fun switchProject(projectId: String) {
        val proj = _projects.value.find { it.id == projectId } ?: return
        _currentProjectId.value = projectId
        _activeFileId.value = proj.activeFileId
        _openTabIds.value = proj.files.take(4).map { it.id }
    }

    fun createProject(name: String, description: String, template: String) {
        val newProjId = "proj_" + UUID.randomUUID().toString().take(6)
        val mainFileId = "file_" + UUID.randomUUID().toString().take(6)

        val files = listOf(
            CodeFile(
                id = mainFileId,
                projectId = newProjId,
                name = "main.py",
                isEntrypoint = true,
                content = "# $name\nprint('Hello from $name!')\n"
            )
        )

        val newProject = Project(
            id = newProjId,
            name = name,
            description = description,
            activeFileId = files.first().id,
            files = files,
            installedPackages = emptyList()
        )

        _projects.update { it + newProject }
        switchProject(newProjId)
        closeSheet()
    }

    fun openFile(fileId: String) {
        _activeFileId.value = fileId
        if (!_openTabIds.value.contains(fileId)) {
            _openTabIds.update { it + fileId }
        }
    }

    fun closeTab(fileId: String) {
        val currentTabs = _openTabIds.value
        val newTabs = currentTabs.filter { it != fileId }
        _openTabIds.value = newTabs
        if (_activeFileId.value == fileId) {
            _activeFileId.value = newTabs.lastOrNull() ?: currentProject.files.firstOrNull()?.id ?: ""
        }
    }

    fun updateFileContent(fileId: String, newContent: String) {
        _projects.update { list ->
            list.map { proj ->
                if (proj.id == _currentProjectId.value) {
                    val updatedFiles = proj.files.map { file ->
                        if (file.id == fileId) file.copy(content = newContent) else file
                    }
                    proj.copy(files = updatedFiles)
                } else proj
            }
        }
    }

    fun createFile(name: String, content: String = "", isEntrypoint: Boolean = false) {
        val newFileId = "file_" + UUID.randomUUID().toString().take(6)
        val cleanName = if (!name.contains(".") && !name.startsWith(".")) "$name.py" else name
        val newFile = CodeFile(
            id = newFileId,
            projectId = _currentProjectId.value,
            name = cleanName,
            content = content,
            isEntrypoint = isEntrypoint
        )

        _projects.update { list ->
            list.map { proj ->
                if (proj.id == _currentProjectId.value) {
                    proj.copy(files = proj.files + newFile)
                } else proj
            }
        }

        openFile(newFileId)
        closeSheet()
    }

    fun deleteFile(fileId: String) {
        val proj = currentProject
        if (proj.files.size <= 1) return

        _projects.update { list ->
            list.map { p ->
                if (p.id == _currentProjectId.value) {
                    p.copy(files = p.files.filter { it.id != fileId })
                } else p
            }
        }
        closeTab(fileId)
    }

    fun setAsEntrypoint(fileId: String) {
        _projects.update { list ->
            list.map { proj ->
                if (proj.id == _currentProjectId.value) {
                    val updatedFiles = proj.files.map { file ->
                        file.copy(isEntrypoint = file.id == fileId)
                    }
                    proj.copy(files = updatedFiles)
                } else proj
            }
        }
    }

    fun insertSnippet(snippetCode: String, targetFileName: String) {
        val fileToTarget = currentProject.files.find { it.name == targetFileName }
            ?: activeFile

        if (fileToTarget != null) {
            val separator = if (fileToTarget.content.endsWith("\n\n") || fileToTarget.content.isEmpty()) "" else "\n\n"
            val updated = fileToTarget.content + separator + snippetCode
            updateFileContent(fileToTarget.id, updated)
            openFile(fileToTarget.id)
            closeSheet()
            addExecutionLog(LogType.SUCCESS, "✅ Сниппет добавлен в ${fileToTarget.name}")
        }
    }

    // DEDICATED PIP PACKAGE INSTALLATION
    fun installPackage(packageName: String, version: String = "") {
        viewModelScope.launch {
            openSheet(ActiveSheet.PIP_TERMINAL)
            val cleanPkg = packageName.trim()
            val ver = if (version.isNotEmpty()) version else "2.31.0"

            addPipLog(LogType.SYSTEM, "$ pip install $cleanPkg")
            delay(80)
            addPipLog(LogType.SYSTEM, "Collecting $cleanPkg ($ver)")
            delay(120)
            addPipLog(LogType.SYSTEM, "  Downloading $cleanPkg-$ver-py3-none-any.whl (1.4 MB)")
            addPipLog(LogType.SYSTEM, "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 1.4/1.4 MB")
            delay(150)
            addPipLog(LogType.SYSTEM, "Installing collected packages: $cleanPkg")
            delay(100)
            addPipLog(LogType.SUCCESS, "Successfully installed $cleanPkg-$ver")

            // 1. Update project installedPackages list
            _projects.update { list ->
                list.map { proj ->
                    if (proj.id == _currentProjectId.value && !proj.installedPackages.contains(cleanPkg)) {
                        proj.copy(installedPackages = proj.installedPackages + cleanPkg)
                    } else proj
                }
            }

            // 2. Update requirements.txt if present
            val lineToAdd = if (version.isNotEmpty()) "$cleanPkg==$version" else cleanPkg
            val reqFile = currentProject.files.find { it.isRequirements }
            if (reqFile != null) {
                if (!reqFile.content.contains(cleanPkg)) {
                    val updatedContent = if (reqFile.content.isEmpty()) lineToAdd + "\n" else reqFile.content.trimEnd() + "\n" + lineToAdd + "\n"
                    updateFileContent(reqFile.id, updatedContent)
                }
            }
        }
    }

    fun uninstallPackage(packageName: String) {
        viewModelScope.launch {
            openSheet(ActiveSheet.PIP_TERMINAL)
            addPipLog(LogType.SYSTEM, "$ pip uninstall -y $packageName")
            delay(80)
            addPipLog(LogType.SUCCESS, "Successfully uninstalled $packageName")

            _projects.update { list ->
                list.map { proj ->
                    if (proj.id == _currentProjectId.value) {
                        proj.copy(installedPackages = proj.installedPackages.filter { it != packageName })
                    } else proj
                }
            }

            val reqFile = currentProject.files.find { it.isRequirements }
            if (reqFile != null) {
                val lines = reqFile.content.lines().filter { !it.startsWith(packageName) }
                updateFileContent(reqFile.id, lines.joinToString("\n"))
            }
        }
    }

    fun runPipCommand(command: String) {
        val trimmed = command.trim()
        viewModelScope.launch {
            addPipLog(LogType.SYSTEM, "$ $trimmed")
            delay(80)
            when {
                trimmed.startsWith("pip install ") -> {
                    val pkg = trimmed.removePrefix("pip install ").trim()
                    installPackage(pkg)
                }
                trimmed == "pip list" || trimmed == "pip freeze" -> {
                    addPipLog(LogType.STDOUT, "Package            Version")
                    addPipLog(LogType.STDOUT, "------------------ -------")
                    if (currentProject.installedPackages.isEmpty()) {
                        addPipLog(LogType.SYSTEM, "(virtualenv is clean, no packages installed)")
                    } else {
                        currentProject.installedPackages.forEach { pkg ->
                            addPipLog(LogType.STDOUT, String.format("%-18s %s", pkg, "1.0.0"))
                        }
                    }
                }
                trimmed == "pip --version" -> {
                    addPipLog(LogType.STDOUT, "pip 24.0 from /env/lib/python3.11/site-packages/pip (python 3.11)")
                }
                else -> {
                    addPipLog(LogType.SYSTEM, "Running: $trimmed")
                    delay(100)
                    addPipLog(LogType.SUCCESS, "Command completed.")
                }
            }
        }
    }

    fun updateSettings(settings: EditorSettings) {
        _editorSettings.value = settings
    }

    // DEDICATED EXECUTION RUNNER
    fun runProject() {
        if (_isRunning.value) return
        _isRunning.value = true
        _executionLogs.value = emptyList()
        openSheet(ActiveSheet.EXECUTION_TERMINAL)

        viewModelScope.launch {
            executor.runProject(
                project = currentProject,
                onLog = { entry ->
                    _executionLogs.update { it + entry }
                }
            )
            _isRunning.value = false
        }
    }

    fun stopProject() {
        _isRunning.value = false
        addExecutionLog(LogType.SYSTEM, "⏹ Process stopped by user.")
    }

    fun clearExecutionLogs() {
        _executionLogs.value = emptyList()
    }

    fun clearPipLogs() {
        _pipTerminalLogs.value = emptyList()
    }

    fun addGitHubWorkflowToProject() {
        val workflow = GitHubActionsHub.androidApkWorkflow
        createFile(workflow.fileName, workflow.ymlContent)
        addExecutionLog(LogType.SUCCESS, "✅ Файл ${workflow.fileName} добавлен в проект!")
    }

    fun addExecutionLog(type: LogType, text: String) {
        _executionLogs.update { it + ConsoleEntry(type = type, text = text) }
    }

    fun addPipLog(type: LogType, text: String) {
        _pipTerminalLogs.update { it + ConsoleEntry(type = type, text = text) }
    }
}
