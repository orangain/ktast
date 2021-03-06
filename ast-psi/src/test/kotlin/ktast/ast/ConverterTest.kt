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
                  Node.Declaration.Property
                    Node.Declaration.Property.ValOrVar
                    AFTER: Node.Extra.Whitespace
                    Node.Declaration.Property.Variable
                      Node.Expression.Name
                      AFTER: Node.Extra.Whitespace
                    Node.Keyword.Equal
                    AFTER: Node.Extra.Whitespace
                    Node.Expression.StringTemplate
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
                  Node.Declaration.Property
                    Node.Declaration.Property.ValOrVar
                    AFTER: Node.Extra.Whitespace
                    Node.Declaration.Property.Variable
                      Node.Expression.Name
                      AFTER: Node.Extra.Whitespace
                    Node.Keyword.Equal
                    AFTER: Node.Extra.Whitespace
                    Node.Expression.StringTemplate
                    AFTER: Node.Extra.Whitespace
                    AFTER: Node.Extra.Comment
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