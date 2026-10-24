package com.example

import com.example.data.DefaultProjects
import com.example.data.PackageCatalog
import com.example.engine.TelegramBotSimulator
import com.example.github.GitHubActionsHub
import com.example.model.SyntaxThemes
import com.example.snippets.SnippetLibrary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `default telegram bot project has required multi-file architecture`() {
        val botProject = DefaultProjects.telegramBotProject
        assertEquals("Telegram Bot Pro", botProject.name)
        assertTrue(botProject.files.size >= 4)

        val fileNames = botProject.files.map { it.name }
        assertTrue("Project must contain main.py", fileNames.contains("main.py"))
        assertTrue("Project must contain handlers.py", fileNames.contains("handlers.py"))
        assertTrue("Project must contain keyboards.py", fileNames.contains("keyboards.py"))
        assertTrue("Project must contain config.py", fileNames.contains("config.py"))
        assertTrue("Project must contain requirements.txt", fileNames.contains("requirements.txt"))
        assertTrue("Project must contain .env", fileNames.contains(".env"))
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
    fun `syntax themes exist and have contrasting colors`() {
        val themes = SyntaxThemes.allThemes
        assertTrue(themes.size >= 5)
        val oneDark = SyntaxThemes.getById("one_dark")
        assertEquals("One Dark Pro", oneDark.name)
        assertNotNull(oneDark.keywordColor)
        assertNotNull(oneDark.stringColor)
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
    fun `snippet library contains cards for bot and keyboards`() {
        val cards = SnippetLibrary.cards
        assertTrue(cards.isNotEmpty())
        assertTrue(cards.any { it.targetFileSuggestion == "handlers.py" })
        assertTrue(cards.any { it.targetFileSuggestion == "keyboards.py" })
    }

    @Test
    fun `github actions hub contains android apk compilation workflow`() {
        val apkWorkflow = GitHubActionsHub.androidApkWorkflow
        assertTrue(apkWorkflow.ymlContent.contains("assembleDebug"))
        assertTrue(apkWorkflow.ymlContent.contains("upload-artifact"))
        assertTrue(apkWorkflow.fileName.contains("build-apk.yml"))
    }
}
