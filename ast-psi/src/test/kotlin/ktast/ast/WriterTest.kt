package ktast.ast

import ktast.ast.psi.Parser
import org.junit.Test
import kotlin.test.assertEquals

class WriterTest {

    @Test
    fun writeWithExtras() {
        assertParseAndWrite(
            """
                fun x() {
                    // do nothing
                }
            """.trimIndent(),
            """
                fun x() {
                    // do nothing
                }
            """.trimIndent(),
            withExtras = true
        )
    }

    @Test
    fun writeWithoutExtras() {
        assertParseAndWrite(
            """
                fun x() {
                    // do nothing
                }
            """.trimIndent(),
            """
                fun x(){}
            """.trimIndent(),
            withExtras = false
        )
    }

    private fun assertParseAndWrite(origCode: String, expectedCode: String, withExtras: Boolean) {
        val origFile = Parser.parseFile(origCode)
        println("----ORIG AST----\n${Dumper.dump(origFile)}\n------------")

        val newCode = Writer.write(origFile, withExtras)
        assertEquals(expectedCode.trim(), newCode.trim(), "Parse -> Write for original code, not equal")
    }

}