package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultProjects
import com.example.data.PackageCatalog
import com.example.engine.MultiFileExecutor
import com.example.engine.TelegramBotSimulator
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
    CONSOLE,
    BOT_SIMULATOR,
    CREATE_FILE_DIALOG,
    CREATE_PROJECT_DIALOG
}

class CodeStudioViewModel : ViewModel() {

    private val executor = MultiFileExecutor()

    private val _projects = MutableStateFlow<List<Project>>(DefaultProjects.defaultProjects)
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _currentProjectId = MutableStateFlow(DefaultProjects.telegramBotProject.id)
    val currentProjectId: StateFlow<String> = _currentProjectId.asStateFlow()

    // Tabs that are currently open in the top tab row
    private val _openTabIds = MutableStateFlow<List<String>>(
        DefaultProjects.telegramBotProject.files.take(3).map { it.id }
    )
    val openTabIds: StateFlow<List<String>> = _openTabIds.asStateFlow()

    private val _activeFileId = MutableStateFlow(DefaultProjects.telegramBotProject.activeFileId)
    val activeFileId: StateFlow<String> = _activeFileId.asStateFlow()

    private val _editorSettings = MutableStateFlow(EditorSettings())
    val editorSettings: StateFlow<EditorSettings> = _editorSettings.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleEntry>> = _consoleLogs.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _activeSimulator = MutableStateFlow<TelegramBotSimulator?>(null)
    val activeSimulator: StateFlow<TelegramBotSimulator?> = _activeSimulator.asStateFlow()

    private val _activeSheet = MutableStateFlow<ActiveSheet?>(null)
    val activeSheet: StateFlow<ActiveSheet?> = _activeSheet.asStateFlow()

    private val _cursorPosition = MutableStateFlow(0)
    val cursorPosition: StateFlow<Int> = _cursorPosition.asStateFlow()

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
        _openTabIds.value = proj.files.take(3).map { it.id }
        _activeSimulator.value = null
    }

    fun createProject(name: String, description: String, template: String) {
        val newProjId = "proj_" + UUID.randomUUID().toString().take(6)
        val mainFileId = "file_" + UUID.randomUUID().toString().take(6)

        val files = when (template) {
            "telegram" -> {
                DefaultProjects.telegramBotProject.files.map {
                    it.copy(id = "file_" + UUID.randomUUID().toString().take(6), projectId = newProjId)
                }
            }
            "fastapi" -> {
                DefaultProjects.fastApiProject.files.map {
                    it.copy(id = "file_" + UUID.randomUUID().toString().take(6), projectId = newProjId)
                }
            }
            else -> {
                listOf(
                    CodeFile(
                        id = mainFileId,
                        projectId = newProjId,
                        name = "main.py",
                        isEntrypoint = true,
                        content = "# New PocketCode Multi-File Project\nprint('Hello from PocketCode!')\n"
                    ),
                    CodeFile(
                        id = "file_" + UUID.randomUUID().toString().take(6),
                        projectId = newProjId,
                        name = "utils.py",
                        content = "# Helper utilities\ndef get_greeting():\n    return 'Hello, World!'\n"
                    )
                )
            }
        }

        val newProject = Project(
            id = newProjId,
            name = name,
            description = description,
            activeFileId = files.first().id,
            files = files,
            installedPackages = if (template == "telegram") listOf("aiogram", "python-dotenv") else emptyList()
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
        if (proj.files.size <= 1) return // Keep at least one file

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
            addConsoleLog(LogType.SUCCESS, "✅ Сниппет успешно вставлен в файл ${fileToTarget.name}")
        }
    }

    fun installPackage(packageName: String, version: String = "") {
        viewModelScope.launch {
            openSheet(ActiveSheet.CONSOLE)
            addConsoleLog(LogType.SYSTEM, "$ pip install $packageName${if (version.isNotEmpty()) "==$version" else ""}")
            delay(120)
            addConsoleLog(LogType.SYSTEM, "Collecting $packageName...")
            delay(200)
            addConsoleLog(LogType.SYSTEM, "Downloading $packageName (wheels)...")
            delay(250)
            addConsoleLog(LogType.SYSTEM, "Installing collected packages: $packageName")
            delay(150)
            addConsoleLog(LogType.SUCCESS, "Successfully installed $packageName")

            // Update requirements.txt
            val reqFile = currentProject.files.find { it.isRequirements }
            val versionStr = if (version.isNotEmpty()) "==$version" else ">=1.0.0"
            val lineToAdd = "$packageName$versionStr"

            if (reqFile != null) {
                if (!reqFile.content.contains(packageName)) {
                    val updatedContent = if (reqFile.content.isEmpty()) lineToAdd else reqFile.content.trimEnd() + "\n" + lineToAdd + "\n"
                    updateFileContent(reqFile.id, updatedContent)
                }
            } else {
                createFile("requirements.txt", lineToAdd + "\n")
            }

            // Mark as installed in project
            _projects.update { list ->
                list.map { proj ->
                    if (proj.id == _currentProjectId.value && !proj.installedPackages.contains(packageName)) {
                        proj.copy(installedPackages = proj.installedPackages + packageName)
                    } else proj
                }
            }
        }
    }

    fun uninstallPackage(packageName: String) {
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
        addConsoleLog(LogType.SYSTEM, "Uninstalled $packageName from project.")
    }

    fun updateSettings(settings: EditorSettings) {
        _editorSettings.value = settings
    }

    fun runProject() {
        if (_isRunning.value) return
        _isRunning.value = true
        _consoleLogs.value = emptyList()
        openSheet(ActiveSheet.CONSOLE)

        viewModelScope.launch {
            executor.runProject(
                project = currentProject,
                onLog = { entry ->
                    _consoleLogs.update { it + entry }
                },
                onBotSimulatorReady = { sim ->
                    _activeSimulator.value = sim
                }
            )
            _isRunning.value = false
        }
    }

    fun sendBotMessage(text: String) {
        val sim = _activeSimulator.value ?: return
        viewModelScope.launch {
            sim.handleUserMessage(text) { log ->
                _consoleLogs.update { it + log }
            }
        }
    }

    fun sendBotCallback(callbackData: String) {
        val sim = _activeSimulator.value ?: return
        viewModelScope.launch {
            sim.handleCallbackClick(callbackData) { log ->
                _consoleLogs.update { it + log }
            }
        }
    }

    fun clearConsole() {
        _consoleLogs.value = emptyList()
    }

    fun addGitHubWorkflowToProject() {
        val workflow = GitHubActionsHub.androidApkWorkflow
        createFile(workflow.fileName, workflow.ymlContent)
        addConsoleLog(LogType.SUCCESS, "✅ Файл воркфлоу ${workflow.fileName} добавлен в проект!")
    }

    fun addConsoleLog(type: LogType, text: String) {
        _consoleLogs.update { it + ConsoleEntry(type = type, text = text) }
    }
}
