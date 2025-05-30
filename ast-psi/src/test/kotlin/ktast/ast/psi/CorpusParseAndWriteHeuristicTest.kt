package ktast.ast.psi

import ktast.ast.Dumper
import ktast.ast.Writer
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtilRt
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.io.path.name
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class CorpusParseAndWriteHeuristicTest(private val unit: Corpus.Unit) {

    /**
     * Test case to verify that the AST -> source code -> AST conversion matches the original AST, even if the Writer does not have Extra (e.g., whitespace) information.
     */
    @Test
    fun testParseAndWriteHeuristic() {
        // In order to test, we parse the test code (failing and validating errors if present),
        // convert to our AST, write out our AST, re-parse what we wrote, re-convert, and compare
        try {
            val path = if (unit is Corpus.Unit.FromFile) {
                println("Loading ${unit.fullPath}")
                unit.fullPath.name
            } else {
                "test.kt"
            }
            val origCode = StringUtilRt.convertLineSeparators(unit.read())
            println("----ORIG CODE----\n$origCode\n------------")
            val origFile = Parser(Converter()).parseFile(origCode, path)
            val origDump = Dumper.dump(origFile)
            println("----ORIG AST----\n$origDump\n------------")

            val newCode = Writer.write(origFile)
            println("----NEW CODE----\n$newCode\n-----------")
            val newFile = Parser(Converter()).parseFile(newCode, path)
            val newDump = Dumper.dump(newFile)

//            assertEquals(origCode, newCode)
            assertEquals(origDump, newDump)
            assertEquals(origFile, newFile)
        } catch (e: Converter.Unsupported) {
            if (unit.canSkip) {
                Assume.assumeNoException(e.message, e)
            } else {
                throw e
            }
        } catch (e: Parser.ParseError) {
            if (!unit.canSkip || unit.errorMessages.isEmpty()) throw e
            assertEquals(unit.errorMessages.toSet(), e.errors.map { it.errorDescription }.toSet())
            Assume.assumeTrue("Partial parsing not supported (expected parse errors: ${unit.errorMessages})", false)
        }
    }

    companion object {
        const val debug = false

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = Corpus.default
        // Uncomment to test a specific file
        //.filter { it.relativePath.toString().endsWith("list\\basic.kt") }

        // Good for quick testing
//        @JvmStatic @Parameterized.Parameters(name = "{0}")
//        fun data() = listOf(Corpus.Unit.FromString("temp", """
//            val lambdaType: (@A() (() -> C))
//        """))
    }
}