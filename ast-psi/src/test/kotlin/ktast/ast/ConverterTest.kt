package ktast.ast

import ktast.ast.psi.ConverterWithExtras
import ktast.ast.psi.Parser
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
                  Node.PropertyDeclaration
                    Node.Keyword.Val
                    AFTER: Node.Whitespace
                    Node.Variable
                      Node.NameExpression
                      AFTER: Node.Whitespace
                    Node.Keyword.Equal
                    AFTER: Node.Whitespace
                    Node.StringLiteralExpression
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
                  Node.PropertyDeclaration
                    Node.Keyword.Val
                    AFTER: Node.Whitespace
                    Node.Variable
                      Node.NameExpression
                      AFTER: Node.Whitespace
                    Node.Keyword.Equal
                    AFTER: Node.Whitespace
                    Node.StringLiteralExpression
                    AFTER: Node.Whitespace
                    AFTER: Node.Comment
            """.trimIndent()
        )
    }

    private fun assertParsedAs(code: String, expectedDump: String) {
        val converter = ConverterWithExtras()
        val node = Parser(converter).parseFile(code)
        val actualDump = Dumper.dump(node, converter, verbose = false)
        assertEquals(expectedDump.trim(), actualDump.trim())
    }

}