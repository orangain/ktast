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

    private val fileExtensions = Regex("\\.kts?$")

    val default: List<Unit> by lazy { localTestData + kotlinRepoTestData + stringTestData }

    private val localTestData by lazy {
        loadTestDataFromDir(File(javaClass.getResource("/localTestData").toURI()).toPath(), canSkip = false)
    }

    private val kotlinRepoTestData by lazy {
        // Recursive from $KOTLIN_REPO/compiler/testData/psi/**/*.kt and *.kts
        val dir = Paths.get(
            System.getenv("KOTLIN_REPO") ?: error("No KOTLIN_REPO env var"),
            "compiler/testData/psi"
        ).also { require(Files.isDirectory(it)) { "Dir not found at $it" } }

        loadTestDataFromDir(dir, canSkip = true)
    }

    private val stringTestData by lazy {
        listOf(
            Unit.FromString("identifier including underscore", "const val c = FOO_BAR"),
            Unit.FromString("identifier including underscore", "const val c = _FOOBAR"),
            Unit.FromString("identifier including underscore", "const val c = FOOBAR_"),
            Unit.FromString("identifier including underscore", "const val c = `___`"),
            Unit.FromString("type parameter modifiers", "fun delete(p: Array<out String>?) {}"),
            Unit.FromString("simple character escaping", """val x = "input\b\nTest\t\r\u3000\'\"\\\${'$'}""""),
            Unit.FromString("superclass primary constructor", "private object SubnetSorter : DefaultSorter<Subnet>()"),
            Unit.FromString("type operator", """val x = "" as String"""),
            Unit.FromString("comments", """val x = "" // x is empty"""),
            Unit.FromString(
                "empty lines",
                """
                    val x = ""
                    
                    val y = 0
                """.trimIndent()
            ),
            Unit.FromString(
                "function block",

                """
                    fun setup() {
                        // do something
                        val x = ""
                        val y = 3
                        // last
                    }
                """.trimIndent()
            ),
            Unit.FromString(
                "function block having only comment",
                """
                    fun setup() {
                        // do something
                    }
                """.trimIndent()
            ),
            Unit.FromString(
                "lambda expression",
                """
                    fun setup() {
                        run {
                            // do something
                            val x = ""
                        }
                    }
                """.trimIndent()
            ),
            Unit.FromString(
                "lambda expression having only comment",
                """
                    fun setup() {
                        run {
                            // do something
                        }
                    }
                """.trimIndent()
            ),
            Unit.FromString("long package name", "package foo.bar.baz.buzz"),
            Unit.FromString("function modifier", "private fun setup() {}"),
            Unit.FromString(
                "semicolon after if",
                "fun foo(a: Int): Int { var x = a; var y = x++; if (y+1 != x) return -1; return x; }"
            ),
            Unit.FromString(
                "quoted identifiers",
                """
                    @`return` fun `package`() {
                      `class`()
                    }
                """.trimIndent()
            ),
            Unit.FromString("constructor modifiers", "class Foo @[foo] private @[bar()] ()"),
            Unit.FromString(
                "secondary constructor",
                """
                    class Foo {
                        @annot protected constructor(x: Int, y: Int) : this(1,2) {}
                    }
                """.trimIndent()
            ),
            Unit.FromString(
                "function receiver",

                """
                    fun (@[a] T<T>.(A<B>) -> Unit).foo()
                    fun @[a] (@[a] T<T>.(A<B>) -> R).foo() {}
                """.trimIndent()
            ),
            Unit.FromString(
                "type modifiers",
                """
                    val p1:suspend a
                    val p2: suspend (a) -> a
                    val p5: (suspend a).() -> a
                    val p6: a<in suspend a>
                    val p15: suspend (suspend (() -> Unit)) -> Unit
                    @a fun @a a.f1() {}
                """.trimIndent()
            ),
            Unit.FromString(
                "by",
                """
                    class Runnable<a,a>(a : doo = 0) : foo(d=0), bar by x, bar {
                    }
                """.trimIndent()
            ),
            Unit.FromString("context receivers", "typealias f = context(T, X) (a: @[a] a) -> b"),
            Unit.FromString(
                "object literal expression",
                """
                val foo = object : Bar, Baz {
                    fun foo() {}
                }
            """.trimIndent()
            ),
            Unit.FromString("function type with parentheses", "val lambdaType: (@A() (() -> C))"),
            Unit.FromString(
                "nullable type parentheses",
                """
                    val temp3: (suspend (String) -> Int)? = { 5 }
                    val temp4: ((((String) -> Int)?) -> Int)? = { 5 }
                """.trimIndent()
            )
        )
    }

    private fun loadTestDataFromDir(root: Path, canSkip: Boolean): List<Unit.FromFile> = Files.walk(root)
        .filter { fileExtensions.containsMatchIn(it.fileName.toString()) }
        .toList<Path>()
        .map { ktPath ->
            val relativePath = root.relativize(ktPath)
            Unit.FromFile(
                relativePath = relativePath,
                fullPath = ktPath,
                // Text files (same name w/ ext changed from kt to txt) have <whitespace>PsiElement:<error>
                errorMessages = overrideErrors[relativePath]
                    ?: Paths.get(ktPath.toString().replace(fileExtensions, ".txt")).let { txtPath ->
                        if (!Files.isRegularFile(txtPath)) {
                            emptyList()
                        } else {
                            txtPath.toFile().readLines().mapNotNull { line ->
                                line.substringAfterLast("PsiErrorElement:", "").takeIf { it.isNotEmpty() }
                            }
                        }
                    },
                canSkip = canSkip,
            )
        }

    sealed class Unit {
        abstract val name: String
        abstract val errorMessages: List<String>
        abstract val canSkip: Boolean
        abstract fun read(): String
        final override fun toString() = name

        data class FromFile(
            val relativePath: Path,
            val fullPath: Path,
            override val errorMessages: List<String>,
            override val canSkip: Boolean = false,
        ) : Unit() {
            override val name: String get() = relativePath.toString()
            override fun read() = fullPath.toFile().readText()
        }

        data class FromString(
            override val name: String,
            val contents: String,
            override val errorMessages: List<String> = emptyList(),
            override val canSkip: Boolean = false,
        ) : Unit() {
            override fun read() = contents
        }
    }
}