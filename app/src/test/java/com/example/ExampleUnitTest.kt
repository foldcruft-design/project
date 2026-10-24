package com.example

import com.example.data.DefaultProjects
import com.example.data.PackageCatalog
import com.example.engine.TelegramBotSimulator
import com.example.github.GitHubActionsHub
import com.example.model.SyntaxThemes
import com.example.snippets.SnippetLibrary
import com.example.ui.components.FileTypeHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `blank project starts with completely empty files`() {
        val blank = DefaultProjects.blankProject
        assertEquals("Чистый проект", blank.name)
        assertTrue(blank.files.isNotEmpty())
        assertTrue("main.py in blank project should be empty", blank.files.first().content.isEmpty())
    }

    @Test
    fun `default telegram bot project has required multi-file architecture`() {
        val botProject = DefaultProjects.telegramBotProject
        assertEquals("Telegram Bot Pro", botProject.name)
        assertTrue(botProject.files.size >= 5)

        val fileNames = botProject.files.map { it.name }
        assertTrue("Project must contain main.py", fileNames.contains("main.py"))
        assertTrue("Project must contain handlers.py", fileNames.contains("handlers.py"))
        assertTrue("Project must contain keyboards.py", fileNames.contains("keyboards.py"))
        assertTrue("Project must contain config.py", fileNames.contains("config.py"))
        assertTrue("Project must contain requirements.txt", fileNames.contains("requirements.txt"))
        assertTrue("Project must contain .env", fileNames.contains(".env"))
        assertTrue("Project must contain android.yml", fileNames.contains("android.yml"))
    }

    @Test
    fun `file type helper provides badges for python java cpp and others`() {
        assertEquals("Py", FileTypeHelper.getLanguageBadge("main.py").label)
        assertEquals("Java", FileTypeHelper.getLanguageBadge("App.java").label)
        assertEquals("C++", FileTypeHelper.getLanguageBadge("main.cpp").label)
        assertEquals("JS", FileTypeHelper.getLanguageBadge("index.js").label)
        assertEquals("TS", FileTypeHelper.getLanguageBadge("app.ts").label)
        assertEquals("YAML", FileTypeHelper.getLanguageBadge("android.yml").label)
    }

    @Test
    fun `github dark syntax theme exists and is default`() {
        val githubTheme = SyntaxThemes.getById("github_dark")
        assertEquals("GitHub Dark (Default)", githubTheme.name)
        assertNotNull(githubTheme.keywordColor)
        assertNotNull(githubTheme.stringColor)
    }

    @Test
    fun `package catalog contains essential bot and web libraries`() {
        val packages = PackageCatalog.curatedPackages
        val names = packages.map { it.name }
        assertTrue(names.contains("aiogram"))
        assertTrue(names.contains("python-dotenv"))
        assertTrue(names.contains("fastapi"))
        assertTrue(names.contains("requests"))
    }

    @Test
    fun `bot simulator handles start command and callback queries`() = runBlocking {
        val simulator = TelegramBotSimulator(DefaultProjects.telegramBotProject)
        val reply = simulator.handleUserMessage("/start") {}
        assertTrue(reply.text.contains("Привет"))
        assertTrue(reply.inlineButtons.isNotEmpty())

        val callbackResult = simulator.handleCallbackClick("action_stats") {}
        assertTrue(callbackResult.contains("Статистика"))
    }

    @Test
    fun `github actions hub contains android yml apk compilation workflow`() {
        val apkWorkflow = GitHubActionsHub.androidApkWorkflow
        assertTrue(apkWorkflow.ymlContent.contains("assembleDebug"))
        assertTrue(apkWorkflow.ymlContent.contains("upload-artifact"))
        assertTrue(apkWorkflow.fileName.contains("android.yml"))
    }
}
