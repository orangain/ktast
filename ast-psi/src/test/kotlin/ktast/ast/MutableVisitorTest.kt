package ktast.ast

import ktast.ast.psi.ConverterWithMutableExtras
import ktast.ast.psi.Parser
import org.junit.Test

class MutableVisitorTest {
    @Test
    fun testIdentityVisitor() {
        assertMutateAndWriteExact(
            """
                val x = 1
                val y = 2
            """.trimIndent(),
            { path -> path.node },
            """
                val x = 1
                val y = 2
            """.trimIndent(),
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
}


private fun assertMutateAndWriteExact(
    origCode: String,
    fn: (path: NodePath<*>) -> Node,
    expectedCode: String
) {
    val origExtrasConv = ConverterWithMutableExtras()
    val origFile = Parser(origExtrasConv).parseFile(origCode)
    println("----ORIG AST----\n${Dumper.dump(origFile, origExtrasConv)}\n------------")

    val newFile = MutableVisitor.preVisit(origFile, origExtrasConv, fn)
    val newCode = Writer.write(newFile, origExtrasConv)
    kotlin.test.assertEquals(
        expectedCode.trim(),
        newCode.trim(),
        "Parse -> Mutate -> Write for original code, not equal"
    )
}