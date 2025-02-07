package ktast.ast

import ktast.ast.psi.Parser
import org.junit.Test

class MutableVisitorTest {
    @Test
    fun testIdentityVisitor() {
        val source = """
            val x = 1
            val y = 2
        """.trimIndent()
        assertMutateAndWriteExact(
            source,
            { path -> path.node },
            source,
        )
    }

    @Test
    fun testMutableVisitor() {
        assertMutateAndWriteExact(
            """
                val x = 1
                val y = 2
            """.trimIndent(),
            { path ->
                when (val node = path.node) {
                    is Node.Expression.NameExpression -> {
                        when (node.text) {
                            "x" -> node.copy(text = "a")
                            "y" -> node.copy(text = "b")
                            else -> node
                        }
                    }
                    else -> node
                }
            },
            """
                val a = 1
                val b = 2
            """.trimIndent(),
        )
    }

    @Test
    fun testScript() {
        val script = """
            println("this is a script")
            
            val x = 1
            val y = 2
            
            fun Project.call(v: Int){
                println("this is a function with " + (v + y))
            }
            call(x)
        """.trimIndent()
        assertMutateAndWriteExact(
            script,
            { path -> path.node },
            script,
            script = true,
        )
    }
}


private fun assertMutateAndWriteExact(
    origCode: String,
    fn: (path: NodePath<*>) -> Node,
    expectedCode: String,
    script: Boolean = false,
) {
    val origFile = Parser.parseFile(origCode, path = if (script) "temp.kts" else "temp.kt")
    println("----ORIG AST----\n${Dumper.dump(origFile)}\n------------")

    val newFile = MutableVisitor.traverse(origFile, fn)
    val newCode = Writer.write(newFile)
    kotlin.test.assertEquals(
        expectedCode.trim(),
        newCode.trim(),
        "Parse -> Mutate -> Write for original code, not equal"
    )
}