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
                Node.Keyword.ValOrVar
                AFTER: Node.Extra.Whitespace
                Node.Decl.Property.Var
                  Node.Expr.Name
                  AFTER: Node.Extra.Whitespace
                BEFORE: Node.Extra.Whitespace
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
                Node.Keyword.ValOrVar
                AFTER: Node.Extra.Whitespace
                Node.Decl.Property.Var
                  Node.Expr.Name
                  AFTER: Node.Extra.Whitespace
                BEFORE: Node.Extra.Whitespace
                Node.Expr.StringTmpl
                AFTER: Node.Extra.Whitespace
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
              Node.Decl.Property
                BEFORE: Node.Extra.Comment
                BEFORE: Node.Extra.Whitespace
                Node.Keyword.ValOrVar
                AFTER: Node.Extra.Whitespace
                Node.Decl.Property.Var
                  Node.Expr.Name
                  AFTER: Node.Extra.Whitespace
                BEFORE: Node.Extra.Whitespace
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
                BEFORE: Node.Extra.Whitespace
                Node.Expr.Name
                Node.Decl.Func.Body.Block
                  BEFORE: Node.Extra.Whitespace
                  Node.Expr.Block
                    Node.Stmt.Decl
                      BEFORE: Node.Extra.Whitespace
                      BEFORE: Node.Extra.Comment
                      BEFORE: Node.Extra.Whitespace
                      Node.Decl.Property
                        Node.Keyword.ValOrVar
                        AFTER: Node.Extra.Whitespace
                        Node.Decl.Property.Var
                          Node.Expr.Name
                          AFTER: Node.Extra.Whitespace
                        BEFORE: Node.Extra.Whitespace
                        Node.Expr.StringTmpl
                      AFTER: Node.Extra.Whitespace
        """.trimIndent())
    }

    @Test
    fun testFunctionBlockHavingOnlyComment() {
        assertParsedAs("""
            fun setup() {
                // do something
            }
        """.trimIndent(), """
            Node.File
              Node.Decl.Func
                BEFORE: Node.Extra.Whitespace
                Node.Expr.Name
                Node.Decl.Func.Body.Block
                  BEFORE: Node.Extra.Whitespace
                  Node.Expr.Block
                    WITHIN: Node.Extra.Whitespace
                    WITHIN: Node.Extra.Comment
                    WITHIN: Node.Extra.Whitespace
        """.trimIndent())
    }

    @Test
    fun testFunctionExpression() {
        assertParsedAs("""
            fun calc() = 1 + 2
        """.trimIndent(), """
            Node.File
              Node.Decl.Func
                WITHIN: Node.Extra.Whitespace
                BEFORE: Node.Extra.Whitespace
                Node.Expr.Name
                Node.Decl.Func.Body.Expr
                  BEFORE: Node.Extra.Whitespace
                  Node.Expr.BinaryOp
                    Node.Expr.Const
                    BEFORE: Node.Extra.Whitespace
                    Node.Expr.BinaryOp.Oper.Token
                    AFTER: Node.Extra.Whitespace
                    Node.Expr.Const
        """.trimIndent()
        )
    }

    @Test
    fun testLambdaExpression() {
        assertParsedAs("""
            fun setup() {
                run {
                    // do something
                    val x = ""
                }
            }
        """.trimIndent(), """
            Node.File
              Node.Decl.Func
                BEFORE: Node.Extra.Whitespace
                Node.Expr.Name
                Node.Decl.Func.Body.Block
                  BEFORE: Node.Extra.Whitespace
                  Node.Expr.Block
                    Node.Stmt.Expr
                      BEFORE: Node.Extra.Whitespace
                      Node.Expr.Call
                        Node.Expr.Name
                        AFTER: Node.Extra.Whitespace
                        Node.Expr.Call.TrailLambda
                          Node.Expr.Lambda
                            BEFORE: Node.Extra.Whitespace
                            Node.Expr.Lambda.Body
                              Node.Stmt.Decl
                                BEFORE: Node.Extra.Comment
                                BEFORE: Node.Extra.Whitespace
                                Node.Decl.Property
                                  Node.Keyword.ValOrVar
                                  AFTER: Node.Extra.Whitespace
                                  Node.Decl.Property.Var
                                    Node.Expr.Name
                                    AFTER: Node.Extra.Whitespace
                                  BEFORE: Node.Extra.Whitespace
                                  Node.Expr.StringTmpl
                            AFTER: Node.Extra.Whitespace
                      AFTER: Node.Extra.Whitespace
        """.trimIndent())
    }

    @Test
    fun testLambdaExpressionHavingOnlyComment() {
        assertParsedAs("""
            fun setup() {
                run {
                    // do something
                }
            }
        """.trimIndent(), """
            Node.File
              Node.Decl.Func
                BEFORE: Node.Extra.Whitespace
                Node.Expr.Name
                Node.Decl.Func.Body.Block
                  BEFORE: Node.Extra.Whitespace
                  Node.Expr.Block
                    Node.Stmt.Expr
                      BEFORE: Node.Extra.Whitespace
                      Node.Expr.Call
                        Node.Expr.Name
                        AFTER: Node.Extra.Whitespace
                        Node.Expr.Call.TrailLambda
                          Node.Expr.Lambda
                            BEFORE: Node.Extra.Whitespace
                            Node.Expr.Lambda.Body
                              WITHIN: Node.Extra.Comment
                            AFTER: Node.Extra.Whitespace
                      AFTER: Node.Extra.Whitespace
        """.trimIndent())
    }

    private fun assertParsedAs(code: String, expectedDump: String) {
        val converter = ConverterWithExtras()
        val node = Parser(converter).parseFile(code)
        val actualDump = Dumper.dump(node, converter, verbose = false)
        assertEquals(expectedDump.trim(), actualDump.trim())
    }

}