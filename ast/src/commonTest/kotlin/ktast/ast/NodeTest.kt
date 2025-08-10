package ktast.ast

import kotlin.test.Test
import kotlin.test.assertEquals

class StringLiteralExpressionTest {
    @Test
    fun testNormalString() {
        val node = Node.Expression.StringLiteralExpression("\"", listOf())

        assertEquals("\"", node.suffix)
        assertEquals(false, node.raw)
    }

    @Test
    fun testRawString() {
        val node = Node.Expression.StringLiteralExpression("\"\"\"", listOf())

        assertEquals("\"\"\"", node.suffix)
        assertEquals(true, node.raw)
    }

    @Test
    fun testMultiDollarString() {
        val node = Node.Expression.StringLiteralExpression("$$\"", listOf())

        assertEquals("\"", node.suffix)
        assertEquals(false, node.raw)
    }

    @Test
    fun testMultiDollarRawString() {
        val node = Node.Expression.StringLiteralExpression("$$$\"\"\"", listOf())

        assertEquals("\"\"\"", node.suffix)
        assertEquals(true, node.raw)
    }
}
