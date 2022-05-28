package kastree.ast

import kastree.ast.psi.ConverterWithExtras
import kastree.ast.psi.Parser
import org.junit.Test
import kotlin.test.assertEquals

class ParserTest {

    @Test
    fun testDeclaration() {
        assertParsedAs("""
            val x = ""
        """.trimIndent(), """
            Node.File
              Node.Decl.Property
                Node.Decl.Property.Var
                Node.Expr.StringTmpl
        """.trimIndent())
    }

    @Test
    fun testInlineComment() {
        assertParsedAs("""
            val x = "" // x is empty
        """.trimIndent(), """
            Node.File
              Node.Decl.Property
                Node.Decl.Property.Var
                Node.Expr.StringTmpl
              AFTER: Node.Extra.Comment
        """.trimIndent())
    }

    @Test
    fun testLineComment() {
        assertParsedAs("""
            // x is empty
            val x = ""
        """.trimIndent(), """
            Node.File
              BEFORE: Node.Extra.Comment
              Node.Decl.Property
                Node.Decl.Property.Var
                Node.Expr.StringTmpl
        """.trimIndent())
    }

    @Test
    fun testFunctionBlock() {
        assertParsedAs("""
            fun setup() {
                // do something
                val x = ""
            }
        """.trimIndent(), """
            Node.File
              Node.Decl.Func
                Node.Decl.Func.Body.Block
                  Node.Expr.Block
                    BEFORE: Node.Extra.Comment
                    Node.Stmt.Decl
                      Node.Decl.Property
                        Node.Decl.Property.Var
                        Node.Expr.StringTmpl
        """.trimIndent())
    }

    @Test
    fun testFunctionExpression() {
        assertParsedAs("""
            fun setup() = {
                // do something
                val x = ""
            }
        """.trimIndent(), """
            Node.File
              Node.Decl.Func
                Node.Decl.Func.Body.Expr
                  Node.Expr.Lambda
                    Node.Expr.Lambda.Body
                      BEFORE: Node.Extra.Comment
                      Node.Stmt.Decl
                        Node.Decl.Property
                          Node.Decl.Property.Var
                          Node.Expr.StringTmpl
        """.trimIndent())
    }

    private fun assertParsedAs(code: String, expectedDump: String) {
        val converter = ConverterWithExtras()
        val node = Parser(converter).parseFile(code)
        val actualDump = Dumper.dump(node, converter, verbose = false)
        assertEquals(expectedDump.trim(), actualDump.trim())
    }

}