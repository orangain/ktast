package ktast.ast.psi

import ktast.ast.MutableVisitor
import ktast.ast.Node
import ktast.ast.Visitor
import ktast.ast.Writer
import org.junit.Test
import kotlin.test.assertEquals

class ReadmeExampleTest {
    @Test
    fun readme() {
        //// Parser
        val code = """
            package foo
        
            fun bar() {
                // Print hello
                println("Hello, World!")
            }
        
            fun baz() = println("Hello, again!")
        """.trimIndent()
        // Call the parser with the code
        val file = Parser.parseFile(code)

        //// Writer
        assertEquals(
            """
                package foo
            
                fun bar() {
                    // Print hello
                    println("Hello, World!")
                }
            
                fun baz() = println("Hello, again!")
            """.trimIndent(),
            Writer.write(file)
        )

        //// Visitor
        val strings = mutableListOf<String>()
        Visitor.traverse(file) { path ->
            val node = path.node
            if (node is Node.Expression.StringLiteralExpression.LiteralStringEntry) {
                strings.add(node.text)
            }
        }
        // Prints [Hello, World!, Hello, again!]
        assertEquals(listOf("Hello, World!", "Hello, again!"), strings)

        //// MutableVisitor
        val newFile = MutableVisitor.traverse(file) { path ->
            val node = path.node
            if (node !is Node.Expression.StringLiteralExpression.LiteralStringEntry) node
            else node.copy(text = node.text.replace("Hello", "Howdy"))
        }

        assertEquals(
            """
                package foo
    
                fun bar() {
                    // Print hello
                    println("Howdy, World!")
                }
    
                fun baz() = println("Howdy, again!")
            """.trimIndent(),
            Writer.write(newFile)
        )
    }

    @Test
    fun readmeWithoutExtras() {
        val code = """
            package foo
        
            fun bar() {
                // Print hello
                println("Hello, World!")
            }
        
            fun baz() = println("Hello, again!")
        """.trimIndent()

        val fileWithoutExtras = Parser(Converter()).parseFile(code)

        assertEquals(
            """
                package foo fun bar(){println("Hello, World!")}
                fun baz()=println("Hello, again!")
            """.trimIndent(),
            Writer.write(fileWithoutExtras)
        )
    }
}