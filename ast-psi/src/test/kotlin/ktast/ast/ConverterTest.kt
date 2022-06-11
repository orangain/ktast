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
                Node.File
                  Node.NodeList
                  Node.Decl.Property
                    Node.Keyword.ValOrVar
                    AFTER: Node.Extra.Whitespace
                    Node.Decl.Property.Variable.Single
                      Node.Expr.Name
                      AFTER: Node.Extra.Whitespace
                    Node.Initializer
                      Node.Keyword.Equal
                      AFTER: Node.Extra.Whitespace
                      Node.Expr.StringTmpl
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
                Node.File
                  Node.NodeList
                  Node.Decl.Property
                    Node.Keyword.ValOrVar
                    AFTER: Node.Extra.Whitespace
                    Node.Decl.Property.Variable.Single
                      Node.Expr.Name
                      AFTER: Node.Extra.Whitespace
                    Node.Initializer
                      Node.Keyword.Equal
                      AFTER: Node.Extra.Whitespace
                      Node.Expr.StringTmpl
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