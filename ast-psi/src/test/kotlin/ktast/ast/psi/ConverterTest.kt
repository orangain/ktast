package ktast.ast.psi

import ktast.ast.Dumper
import org.junit.Test
import kotlin.test.assertEquals

class ConverterTest {

    @Test
    fun testDeclaration() {
        assertParsedAs(
            """
                val x = ""
            """.trimIndent(),
            """
                Node.KotlinFile
                  Node.Declaration.PropertyDeclaration
                    Node.Keyword.Val
                    Node.Variable
                      Node.Expression.NameExpression
                    Node.Expression.StringLiteralExpression
            """.trimIndent()
        )
    }

    @Test
    fun testInlineComment() {
        assertParsedAs(
            """
                val x = "" // x is empty
            """.trimIndent(),
            """
                Node.KotlinFile
                  Node.Declaration.PropertyDeclaration
                    Node.Keyword.Val
                    Node.Variable
                      Node.Expression.NameExpression
                    Node.Expression.StringLiteralExpression
            """.trimIndent()
        )
    }

    @Test
    fun testCommentOnly() {
        assertParsedAs(
            """
                // file is empty
            """.trimIndent(),
            """
                Node.KotlinFile
            """.trimIndent()
        )
    }

    private fun assertParsedAs(code: String, expectedDump: String) {
        val converter = Converter()
        val node = Parser(converter).parseFile(code)
        val actualDump = Dumper.dump(node, withProperties = false)
        assertEquals(expectedDump.trim(), actualDump.trim())
    }

}