package ktast.ast.psi

import ktast.ast.Node
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Parses Kotlin source codes into PSI and converts it to AST.
 */
open class Parser(protected val converter: Converter = Converter()) {
    protected val proj by lazy {
        KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            CompilerConfiguration().apply {
                put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
            },
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).project
    }

    /**
     * Parses Kotlin source code into AST.
     *
     * @param code Kotlin source code
     * @param throwOnError whether to throw an exception if there are errors
     * @return AST node that represents the Kotlin file
     */
    fun parseFile(code: String, throwOnError: Boolean = true): Node.KotlinFile =
        converter.convertKotlinFile(parsePsiFile(code).also { file ->
            if (throwOnError) file.collectDescendantsOfType<PsiErrorElement>().let {
                if (it.isNotEmpty()) throw ParseError(file, it)
            }
        })

    protected fun parsePsiFile(code: String): KtFile =
        PsiManager.getInstance(proj).findFile(LightVirtualFile("temp.kt", KotlinFileType.INSTANCE, code)) as KtFile

    data class ParseError(
        val file: KtFile,
        val errors: List<PsiErrorElement>
    ) : IllegalArgumentException("Failed with ${errors.size} errors, first: ${errors.first().errorDescription}")

    companion object : Parser() {
        init {
            // To hide annoying warning on Windows
            System.setProperty("idea.use.native.fs.for.win", "false")
        }
    }
}