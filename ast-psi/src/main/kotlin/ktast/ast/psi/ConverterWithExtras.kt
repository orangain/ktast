package ktast.ast.psi

import ktast.ast.Node
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.siblings

/**
 * Converts PSI elements to AST nodes and keeps track of extras.
 */
open class ConverterWithExtras : Converter() {
    // Sometimes many nodes are created from the same element, but we only want the last node, i.e. the most ancestor node.
    // We remove the previous nodes we've found for the same identity when we see a new one. So we don't have to
    // keep PSI elements around, we hold a map to the element's identity hash code. Then we use that number to tie
    // to the extras to keep duplicates out. Usually using identity hash codes would be problematic due to
    // potential reuse, we know the PSI objects are all around at the same time, so it's good enough.
    protected val psiIdentitiesToNodes = mutableMapOf<Int, Node>()

    // This keeps track of ws nodes we've seen before so we don't duplicate them
    private val seenExtraPsiIdentities = mutableSetOf<Int>()

    override fun onNode(node: Node, element: PsiElement) {
        // We ignore whitespace and comments here to prevent recursion
        if (element is PsiWhiteSpace || element is PsiComment) return
        // We only want the last node, i.e. the most ancestor node.
        val elemId = System.identityHashCode(element)
        psiIdentitiesToNodes[elemId] = node
    }

    override fun convert(v: KtFile): Node.KotlinFile {
        psiIdentitiesToNodes.clear()
        seenExtraPsiIdentities.clear()
        return super.convert(v).also {
            fillWholeExtras(it, v)
        }
    }

    protected open fun fillWholeExtras(rootNode: Node.KotlinEntry, rootElement: PsiElement) {
        val extraElementsSinceLastNode = mutableListOf<PsiElement>()

        val visitor = object : PsiElementVisitor() {
            private var lastNode: Node? = null
            private val ancestors = ArrayDeque<Node>()

            override fun onBeginElement(element: PsiElement) {
                fillExtrasFor(element)
                val node = psiIdentitiesToNodes[System.identityHashCode(element)]
                if (node != null) {
                    ancestors.add(node)
                }
            }

            override fun onEndElement(element: PsiElement) {
                val node = psiIdentitiesToNodes[System.identityHashCode(element)] ?: return
                if (lastNode != null && lastNode !== node) {
                    fillExtrasAfter(lastNode!!)
                } else {
                    fillExtrasWithin(node)
                }
                lastNode = node
                ancestors.removeLast()
            }

            override fun onLeafElement(element: PsiElement) {
                fillExtrasFor(element)
            }

            private fun fillExtrasFor(element: PsiElement) {
                if (isExtra(element)) {
                    extraElementsSinceLastNode.add(element)
                    if (isSemicolon(element) && lastNode != null) {
                        fillExtrasAfter(lastNode!!)
                    }
                    return
                }
                val node = psiIdentitiesToNodes[System.identityHashCode(element)]

                if (node == null) {
                    if (lastNode != null) {
                        if (ancestors.contains(lastNode)) {
                            return // Don't update lastNode if lastNode is an ancestor of this node
                        }
                        fillExtrasAfter(lastNode!!)
                    }
                } else {
                    // Add first extra children of this element as extrasBefore.
                    extraElementsSinceLastNode.addAll(element.allChildren.takeWhile(::isExtra))
                    fillExtrasBefore(node)
                }
                lastNode = node
            }

            private fun fillExtrasBefore(node: Node) {
                convertExtras(extraElementsSinceLastNode).also {
                    if (it.isNotEmpty()) {
                        node.supplement.extrasBefore += it
                    }
                }
                extraElementsSinceLastNode.clear()
            }

            private fun fillExtrasAfter(node: Node) {
                convertExtras(extraElementsSinceLastNode).also {
                    if (it.isNotEmpty()) {
                        node.supplement.extrasAfter += it
                    }
                }
                extraElementsSinceLastNode.clear()
            }

            private fun fillExtrasWithin(node: Node) {
                convertExtras(extraElementsSinceLastNode).also {
                    if (it.isNotEmpty()) {
                        node.supplement.extrasWithin += it
                    }
                }
                extraElementsSinceLastNode.clear()
            }
        }
        visitor.visit(rootElement)
    }

    protected open fun isExtra(e: PsiElement) =
        e is PsiWhiteSpace || e is PsiComment || isSemicolon(e) || isTrailingComma(e)

    protected fun isSemicolon(e: PsiElement) = e.node.elementType == KtTokens.SEMICOLON

    private val suffixTokens = setOf(
        KtTokens.RPAR, // End of ")"
        KtTokens.RBRACE, // End of "}"
        KtTokens.RBRACKET, // End of "]"
        KtTokens.GT, // End of ">"
        KtTokens.ARROW, // For when conditions, e.g. "1, 2, -> null"
    )

    protected fun isTrailingComma(e: PsiElement): Boolean {
        if (e.node.elementType != KtTokens.COMMA) return false
        // EnumEntry contains comma for each entry, and we treat them all as trailing commas.
        if (e.parent is KtEnumEntry) return true

        val nextNonExtraElement = e.node.siblings(forward = true)
            .filterNot { it is PsiWhiteSpace || it is PsiComment }.firstOrNull()?.psi
        return nextNonExtraElement == null || suffixTokens.contains(nextNonExtraElement.node.elementType)
    }

    protected open fun convertExtras(elements: List<PsiElement>): List<Node.Extra> = elements.mapNotNull { elem ->
        // Ignore elements we've done before
        val elemId = System.identityHashCode(elem)
        if (!seenExtraPsiIdentities.add(elemId)) null else when {
            elem is PsiWhiteSpace -> Node.Extra.Whitespace(elem.text)
            elem is PsiComment -> Node.Extra.Comment(elem.text)
            elem.node.elementType == KtTokens.SEMICOLON -> Node.Extra.Semicolon()
            elem.node.elementType == KtTokens.COMMA -> Node.Extra.TrailingComma()
            else -> error("elements must contain only PsiWhiteSpace or PsiComment or SEMICOLON elements.")
        }
    }
}