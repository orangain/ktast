package ktast.ast.psi

import ktast.ast.Dumper
import ktast.ast.Node
import org.junit.Test
import kotlin.test.assertEquals

class ConverterWithExtrasTest {

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

    @Test
    fun testIfExpression() {
        assertParsedAs(
            """
                fun main() {
                    if ( a == 1 ) {
                        x
                    }
                }
            """.trimIndent(),
            ::getFunctionBody,
            """
                Node.Expression.BlockExpression
                  BEFORE: Node.Extra.Whitespace
                  Node.Expression.IfExpression
                    BEFORE: Node.Extra.Whitespace
                    Node.Keyword.LPar
                    AFTER: Node.Extra.Whitespace
                    Node.Expression.BinaryExpression
                      Node.Expression.NameExpression
                      BEFORE: Node.Extra.Whitespace
                      Node.Keyword.EqualEqual
                      BEFORE: Node.Extra.Whitespace
                      Node.Expression.IntegerLiteralExpression
                    BEFORE: Node.Extra.Whitespace
                    Node.Keyword.RPar
                    AFTER: Node.Extra.Whitespace
                    Node.Expression.BlockExpression
                      BEFORE: Node.Extra.Whitespace
                      Node.Expression.NameExpression
                      AFTER: Node.Extra.Whitespace
                  AFTER: Node.Extra.Whitespace
            """.trimIndent()
        )
    }
}

private fun assertParsedAs(code: String, expectedDump: String) {
    assertParsedAs(code, { it }, expectedDump)
}

private fun assertParsedAs(code: String, transform: (Node.KotlinFile) -> Node, expectedDump: String) {
    val converter = ConverterWithExtras()
    val node = transform(Parser(converter).parseFile(code))
    val actualDump = Dumper.dump(node, withProperties = false)
    assertEquals(expectedDump.trim(), actualDump.trim())
}

private fun getFunctionBody(file: Node.KotlinFile): Node {
    val function = (file.declarations[0] as Node.Declaration.FunctionDeclaration)
    return function.body!!
}