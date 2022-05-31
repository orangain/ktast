package kastree.ast.psi

import kastree.ast.Dumper
import kastree.ast.Node
import kastree.ast.Writer
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtilRt
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class CorpusTest(private val unit: Corpus.Unit) {

    @Test
    fun testParseAndConvert() {
        // In order to test, we parse the test code (failing and validating errors if present),
        // convert to our AST, write out our AST, re-parse what we wrote, re-convert, and compare
        try {
            val origExtrasConv = ConverterWithExtras()
            val origCode = StringUtilRt.convertLineSeparators(unit.read())
            val origFile = Parser(origExtrasConv).parseFile(origCode)
            println("----ORIG CODE----\n$origCode\n------------")
            println("----ORIG AST----\n${Dumper.dump(origFile, origExtrasConv)}\n------------")

            val newExtrasConv = ConverterWithExtras()
            val newCode = Writer.write(origFile, origExtrasConv)
            println("----NEW CODE----\n$newCode\n-----------")
            val newFile = Parser(newExtrasConv).parseFile(newCode)
            println("----NEW AST----\n${Dumper.dump(newFile, newExtrasConv)}\n------------")

            assertEquals(origFile, newFile)
        } catch (e: Converter.Unsupported) {
            Assume.assumeNoException(e.message, e)
        } catch (e: Parser.ParseError) {
            if (unit.errorMessages.isEmpty()) throw e
            assertEquals(unit.errorMessages.toSet(), e.errors.map { it.errorDescription }.toSet())
            Assume.assumeTrue("Partial parsing not supported (expected parse errors: ${unit.errorMessages})", false)
        }
    }

    companion object {
        const val debug = false

        @JvmStatic @Parameterized.Parameters(name = "{0}")
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