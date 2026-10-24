package com.example.data

import com.example.model.CodeFile
import com.example.model.Project

object DefaultProjects {

    val starterProject: Project by lazy {
        val projId = "proj_starter"
        val mainFileId = "file_starter_main"

        val mainPy = CodeFile(
            id = mainFileId,
            projectId = projId,
            name = "main.py",
            isEntrypoint = true,
            content = """# ==========================================
# PocketCode - Python Starter Kit
# ==========================================

def calculate_stats(numbers):
    total = sum(numbers)
    count = len(numbers)
    average = total / count if count > 0 else 0
    return {
        "count": count,
        "sum": total,
        "avg": average,
        "max": max(numbers),
        "min": min(numbers)
    }

print("🚀 Python Starter Kit Ready!")
data = [12, 45, 78, 23, 56, 89, 90]
print(f"📊 Input data: {data}")

stats = calculate_stats(data)
print(f"📈 Sum: {stats['sum']}, Average: {stats['avg']}")
print(f"🔝 Max: {stats['max']}, Min: {stats['min']}")

# Loop demonstration
print("\n⚡ Processing elements:")
for i in range(len(data)):
    val = data[i]
    status = "HIGH" if val > 50 else "LOW"
    print(f"  [{i + 1}] Value {val} -> Status: {status}")

print("\n✨ Program finished successfully with exit code 0.")
"""
        )

        Project(
            id = projId,
            name = "Python Project",
            description = "Чистый стартовый набор для программиста",
            language = "python",
            activeFileId = mainFileId,
            files = listOf(mainPy),
            installedPackages = emptyList()
        )
    }

    val defaultProjects = listOf(starterProject)
}
