package com.example

import com.example.data.DefaultProjects
import com.example.data.PackageCatalog
import com.example.engine.MultiFileExecutor
import com.example.github.GitHubActionsHub
import com.example.model.CodeFile
import com.example.model.ConsoleEntry
import com.example.model.Project
import com.example.model.SyntaxThemes
import com.example.ui.components.FileTypeHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `starter project has 1 clean main py file`() {
        val starter = DefaultProjects.starterProject
        assertEquals("Python Project", starter.name)
        assertEquals(1, starter.files.size)
        assertEquals("main.py", starter.files.first().name)
        assertTrue("main.py must be entrypoint", starter.files.first().isEntrypoint)
        assertTrue("Starter code should not be empty", starter.files.first().content.isNotEmpty())
    }

    @Test
    fun `file type helper provides badges for python java cpp and others`() {
        assertEquals("Py", FileTypeHelper.getLanguageBadge("main.py").label)
        assertEquals("Java", FileTypeHelper.getLanguageBadge("App.java").label)
        assertEquals("C++", FileTypeHelper.getLanguageBadge("main.cpp").label)
        assertEquals("C", FileTypeHelper.getLanguageBadge("main.c").label)
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
    fun `package catalog contains essential programming and web libraries`() {
        val packages = PackageCatalog.curatedPackages
        val names = packages.map { it.name }
        assertTrue(names.contains("requests"))
        assertTrue(names.contains("colorama"))
        assertTrue(names.contains("fastapi"))
    }

    @Test
    fun `executor runs python code and calculates statistics`() = runBlocking {
        val executor = MultiFileExecutor()
        val testProject = Project(
            id = "test_run",
            name = "Test Run",
            description = "Run test",
            activeFileId = "f1",
            files = listOf(
                CodeFile(
                    id = "f1",
                    projectId = "test_run",
                    name = "main.py",
                    isEntrypoint = true,
                    content = """
def add(a, b):
    return a + b

print("Sum is:", add(15, 25))
"""
                )
            ),
            installedPackages = emptyList()
        )

        val logs = mutableListOf<ConsoleEntry>()
        executor.runProject(testProject) { log ->
            logs.add(log)
        }

        assertTrue("Logs should be emitted", logs.isNotEmpty())
        val sumLog = logs.find { it.text.contains("Sum is: 40") }
        assertNotNull("Calculated sum should be printed", sumLog)
    }

    @Test
    fun `github actions hub contains android yml apk compilation workflow`() {
        val apkWorkflow = GitHubActionsHub.androidApkWorkflow
        assertTrue(apkWorkflow.ymlContent.contains("assembleDebug"))
        assertTrue(apkWorkflow.ymlContent.contains("upload-artifact"))
        assertTrue(apkWorkflow.fileName.contains("android.yml"))
    }
}
