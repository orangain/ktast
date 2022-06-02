package kastree.ast

import kastree.ast.psi.ConverterWithExtras
import kastree.ast.psi.Parser
import org.junit.Test
import kotlin.test.assertEquals

class WriterTest {
    @Test
    fun testIdentifierUnderscoreEscape() {
        assertParseAndWriteExact("const val c = FOO_BAR")
        assertParseAndWriteExact("const val c = _FOOBAR")
        assertParseAndWriteExact("const val c = FOOBAR_")
        assertParseAndWriteExact("const val c = `___`")
    }

    @Test
    fun testTypeParameterModifiers() {
        assertParseAndWriteExact("""
            fun delete(p: Array<out String>?) {}
        """.trimIndent())
    }

    @Test
    fun testSimpleCharacterEscaping() {
        assertParseAndWriteExact("""val x = "input\b\n\t\r\'\"\\\${'$'}"""")
    }

    @Test
    fun testSuperclassPrimaryConstructor() {
        assertParseAndWriteExact("private object SubnetSorter : DefaultSorter<Subnet>()")
    }

    @Test
    fun testOpType() {
        assertParseAndWriteExact("""val x = "" as String""")
    }

    @Test
    fun testComments() {
        assertParseAndWriteExact("""val x = "" // x is empty""")
    }

    @Test
    fun testEmptyLines() {
        assertParseAndWriteExact("""
            val x = ""
            
            val y = 0
        """.trimIndent())
    }

    @Test
    fun testFunctionBlock() {
        assertParseAndWriteExact("""
            fun setup() {
                // do something
                val x = ""
                val y = 3
                // last
            }
        """.trimIndent())
    }

    @Test
    fun testFunctionBlockHavingOnlyComment() {
        assertParseAndWriteExact("""
            fun setup() {
                // do something
            }
        """.trimIndent())
    }

    @Test
    fun testLambdaExpression() {
        assertParseAndWriteExact("""
            fun setup() {
                run {
                    // do something
                    val x = ""
                }
            }
        """.trimIndent())
    }

    @Test
    fun testLambdaExpressionHavingOnlyComment() {
        assertParseAndWriteExact("""
            fun setup() {
                run {
                    // do something
                }
            }
        """.trimIndent())
    }

    @Test
    fun testLongPackageName() {
        assertParseAndWriteExact("package foo.bar.baz.buzz")
    }

    @Test
    fun testFunctionModifier() {
        assertParseAndWriteExact("""
            private fun setup() {}
        """.trimIndent())
    }

    @Test
    fun testSemicolonAfterIf() {
        assertParseAndWriteExact("""
            fun foo(a: Int): Int { var x = a; var y = x++; if (y+1 != x) return -1; return x; }
        """.trimIndent())
    }

    @Test
    fun testQuotedIdentifiers() {
        assertParseAndWriteExact("""
            @`return` fun `package`() {
              `class`()
            }
        """.trimIndent())
    }

    @Test
    fun testConstructorModifiers() {
        assertParseAndWriteExact("""
            object Foo @[foo] private @[bar()] ()
        """.trimIndent())
    }

    private fun assertParseAndWriteExact(origCode: String) {

        val origExtrasConv = ConverterWithExtras()
        val origFile = Parser(origExtrasConv).parseFile(origCode)
        println("----ORIG AST----\n${Dumper.dump(origFile, origExtrasConv)}\n------------")

        val newCode = Writer.write(origFile, origExtrasConv)
        assertEquals(origCode.trim(), newCode.trim(), "Parse -> Write for original code, not equal")

        val identityNode = MutableVisitor.preVisit(origFile) { v, _ -> v }
        val identityCode = Writer.write(identityNode, origExtrasConv)

        assertEquals(origCode.trim(), identityCode.trim(), "Parse -> Identity Transform -> Write for original code, not equal")
    }

}