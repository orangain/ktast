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
                      BEFORE: Node.Extra.Whitespace
                      Node.Expression.NameExpression
                      AFTER: Node.Extra.Whitespace
                    BEFORE: Node.Extra.Whitespace
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
                      BEFORE: Node.Extra.Whitespace
                      Node.Expression.NameExpression
                      AFTER: Node.Extra.Whitespace
                    BEFORE: Node.Extra.Whitespace
                    Node.Expression.StringLiteralExpression
                    AFTER: Node.Extra.Whitespace
                    AFTER: Node.Extra.Comment
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
                  WITHIN: Node.Extra.Comment
            """.trimIndent()
        )
    }

    private fun assertParsedAs(code: String, expectedDump: String) {
        val converter = ConverterWithExtras()
        val node = Parser(converter).parseFile(code)
        val actualDump = Dumper.dump(node, withProperties = false)
        assertEquals(expectedDump.trim(), actualDump.trim())
    }

}