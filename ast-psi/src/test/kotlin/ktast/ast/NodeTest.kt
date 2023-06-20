package ktast.ast

import ktast.ast.psi.Converter
import ktast.ast.psi.Parser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(Parameterized::class)
class DoubleColonExpressionTypeTest(private val code: String) {
    @Test
    fun testType() {
        val node = Parser(Converter()).parseFile(code)
        val properties = node.declarations.filterIsInstance<Node.Declaration.PropertyDeclaration>()
        assertEquals(properties.size, 1)
        properties.forEach { property ->
            val type = property.variables[0].type!!
            val classLiteralExpression = property.initializerExpression!! as Node.Expression.ClassLiteralExpression
            assertEquals(type, classLiteralExpression.lhsAsType())
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            // Simple types
            "val x: A = A::class",
            "val x: A.B = A.B::class",
            "val x: A.B.C = A.B.C::class",
            // Generic types
            "val x: A<B> = A<B>::class",
            "val x: A<B>.C = A<B>.C::class",
            "val x: A<B>.C<D> = A<B>.C<D>::class",
            "val x: A<B>.C<D>.E = A<B>.C<D>.E::class",
            "val x: A<B>.C<D>.E<F> = A<B>.C<D>.E<F>::class",
            "val x: A<B,C> = A<B,C>::class",
            "val x: A<B, C>.D = A<B, C>.D::class",
            "val x: A<B,C>.D<E,F> = A<B,C>.D<E,F>::class",
            // Nullable types
            "val x: A? = A?::class",
            "val x: A?? = A??::class",
            "val x: A.B? = A.B?::class",
            "val x: A<B>? = A<B>?::class",
        )
    }
}

@RunWith(Parameterized::class)
class DoubleColonExpressionExpressionTest(private val code: String) {
    @Test
    fun testExpression() {
        val node = Parser(Converter()).parseFile(code)
        val properties = node.declarations.filterIsInstance<Node.Declaration.PropertyDeclaration>()
        assertEquals(properties.size, 1)
        properties.forEach { property ->
            val classLiteralExpression = property.initializerExpression!! as Node.Expression.ClassLiteralExpression
            assertNull(classLiteralExpression.lhsAsType())
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            "val y = this@x::class",
            "val y = super<a>@b::class",
            "val y = (a+b)::class",
            "val y = x()::class",
            "val y = x().y().z()::class",
            "val y = a::b.c::class",
            "val y = A::a::class",
            "val y = object {}::class",
        )
    }
}

@RunWith(Parameterized::class)
class LambdaArgLambdaExpressionTest(private val code: String) {
    @Test
    fun testLambdaExpression() {
        val node = Parser(Converter()).parseFile(code)
        val functionDeclaration = node.declarations.filterIsInstance<Node.Declaration.FunctionDeclaration>().first()
        val callExpressions =
            (functionDeclaration.body as Node.Expression.BlockExpression).statements.filterIsInstance<Node.Expression.CallExpression>()
        assertEquals(1, callExpressions.size)
        callExpressions.forEach { callExpression ->
            assertEquals("x", callExpression.lambdaExpression()!!.parameters[0].variables[0].name.text)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            "fun a() { b { x -> 1 } }",
            "fun a() { b { x, y -> 1 } }",
            "fun a() { b(1) { x -> 1 } }",
            "fun a() { b(1, 2) { x -> 1 } }",
            "fun a() { b @Foo { x -> 1 } }",
            "fun a() { b label@ { x -> 1 } }",
            "fun a() { b @Foo label@ { x -> 1 } }",
        )
    }
}