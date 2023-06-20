package ktast.ast

import ktast.ast.psi.ConverterWithExtras
import ktast.ast.psi.Parser
import org.junit.Test
import kotlin.test.assertEquals

class DumperTest {

    private val code = """
        val x = {
            // x is empty
        }
    """.trimIndent()

    @Test
    fun testWithExtrasAndProperties() {
        assertDumpedAs(
            code,
            """
                Node.KotlinFile
                  Node.Declaration.PropertyDeclaration
                    Node.Keyword.Val{text="val"}
                    Node.Variable
                      BEFORE: Node.Extra.Whitespace{text=" "}
                      Node.Expression.NameExpression{text="x"}
                      AFTER: Node.Extra.Whitespace{text=" "}
                    BEFORE: Node.Extra.Whitespace{text=" "}
                    Node.Expression.LambdaExpression
                      WITHIN: Node.Extra.Whitespace{text="\n    "}
                      WITHIN: Node.Extra.Comment{text="// x is empty"}
                      WITHIN: Node.Extra.Whitespace{text="\n"}
            """.trimIndent(),
            withExtras = true,
            withProperties = true,
        )
    }

    @Test
    fun testWithExtrasButWithoutProperties() {
        assertDumpedAs(
            code,
            """
                Node.KotlinFile
                  Node.Declaration.PropertyDeclaration
                    Node.Keyword.Val
                    Node.Variable
                      BEFORE: Node.Extra.Whitespace
                      Node.Expression.NameExpression
                      AFTER: Node.Extra.Whitespace
                    BEFORE: Node.Extra.Whitespace
                    Node.Expression.LambdaExpression
                      WITHIN: Node.Extra.Whitespace
                      WITHIN: Node.Extra.Comment
                      WITHIN: Node.Extra.Whitespace
            """.trimIndent(),
            withExtras = true,
            withProperties = false,
        )
    }

    @Test
    fun testWithoutExtrasButWithProperties() {
        assertDumpedAs(
            code,
            """
                Node.KotlinFile
                  Node.Declaration.PropertyDeclaration
                    Node.Keyword.Val{text="val"}
                    Node.Variable
                      Node.Expression.NameExpression{text="x"}
                    Node.Expression.LambdaExpression
            """.trimIndent(),
            withExtras = false,
            withProperties = true,
        )
    }

    @Test
    fun testWithoutExtrasOrProperties() {
        assertDumpedAs(
            code,
            """
                Node.KotlinFile
                  Node.Declaration.PropertyDeclaration
                    Node.Keyword.Val
                    Node.Variable
                      Node.Expression.NameExpression
                    Node.Expression.LambdaExpression
            """.trimIndent(),
            withExtras = false,
            withProperties = false,
        )
    }

    private fun assertDumpedAs(code: String, expectedDump: String, withExtras: Boolean, withProperties: Boolean) {
        val converter = ConverterWithExtras()
        val node = Parser(converter).parseFile(code)
        val actualDump = Dumper.dump(node, withExtras = withExtras, withProperties = withProperties)
        assertEquals(expectedDump.trim(), actualDump.trim())
    }

}