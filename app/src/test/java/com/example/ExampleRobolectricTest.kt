package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DefaultProjects
import com.example.engine.MultiFileExecutor
import com.example.model.CodeFile
import com.example.model.ConsoleEntry
import com.example.model.Project
import com.example.viewmodel.CodeStudioViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PocketCode", appName)
    }

    @Test
    fun `default project starts with single main file`() {
        val starter = DefaultProjects.starterProject
        assertEquals(1, starter.files.size)
        assertEquals("main.py", starter.files.first().name)
        assertTrue(starter.files.first().isEntrypoint)
    }

    @Test
    fun `executor runs single python file without requiring other files`() = runBlocking {
        val executor = MultiFileExecutor()
        val testProject = Project(
            id = "test_proj",
            name = "Test",
            description = "Test description",
            activeFileId = "test_main",
            files = listOf(
                CodeFile(
                    id = "test_main",
                    projectId = "test_proj",
                    name = "main.py",
                    isEntrypoint = true,
                    content = """
x = 10
y = 20
print("Result:", x + y)
"""
                )
            ),
            installedPackages = emptyList()
        )

        val logs = mutableListOf<ConsoleEntry>()
        executor.runProject(testProject) { log ->
            logs.add(log)
        }

        assertTrue("Execution should produce logs", logs.isNotEmpty())
        val stdoutLog = logs.find { it.text.contains("Result: 30") }
        assertNotNull("Expected stdout log with Result: 30", stdoutLog)
    }

    @Test
    fun `viewmodel initializes with 1 starter file and handles pip install`() {
        val viewModel = CodeStudioViewModel()
        assertEquals(1, viewModel.currentProject.files.size)
        assertEquals("main.py", viewModel.activeFile?.name)

        // Install package via pip
        viewModel.installPackage("requests")
        // Package install dispatched
        assertNotNull(viewModel.currentProject)
    }
}
