package ktast.ast.psi

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

object Corpus {
    private val overrideErrors = mapOf(
        Paths.get("kdoc", "Simple.kt") to listOf("Unclosed comment")
    )

    val default by lazy { localTestData + kotlinRepoTestData }

    private val kotlinRepoTestData by lazy {
        // Recursive from $KOTLIN_REPO/compiler/testData/psi/**/*.kt
        loadTestDataFromDir(Paths.get(
            System.getenv("KOTLIN_REPO") ?: error("No KOTLIN_REPO env var"),
            "compiler/testData/psi"
        ).also { require(Files.isDirectory(it)) { "Dir not found at $it" } })
    }

    private val localTestData by lazy {
        loadTestDataFromDir(File(javaClass.getResource("/localTestData").toURI()).toPath())
    }

    private fun loadTestDataFromDir(root: Path) = Files.walk(root)
        .filter { it.toString().endsWith(".kt") }
        .toList<Path>()
        .map { ktPath ->
            val relativePath = root.relativize(ktPath)
            Unit.FromFile(
                relativePath = relativePath,
                fullPath = ktPath,
                // Text files (same name w/ ext changed from kt to txt) have <whitespace>PsiElement:<error>
                errorMessages = overrideErrors[relativePath]
                    ?: Paths.get(ktPath.toString().replace(".kt", ".txt")).let { txtPath ->
                        if (!Files.isRegularFile(txtPath)) {
                            emptyList()
                        } else {
                            txtPath.toFile().readLines().mapNotNull { line ->
                                line.substringAfterLast("PsiErrorElement:", "").takeIf { it.isNotEmpty() }
                            }
                        }
                    }
            )
        }

    sealed class Unit {
        abstract val name: String
        abstract val errorMessages: List<String>
        abstract fun read(): String
        final override fun toString() = name

        data class FromFile(
            val relativePath: Path,
            val fullPath: Path,
            override val errorMessages: List<String>
        ) : Unit() {
            override val name: String get() = relativePath.toString()
            override fun read() = fullPath.toFile().readText()
        }

        data class FromString(
            override val name: String,
            val contents: String,
            override val errorMessages: List<String> = emptyList()
        ) : Unit() {
            override fun read() = contents
        }
    }
}