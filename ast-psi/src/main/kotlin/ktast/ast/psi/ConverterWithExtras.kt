package ktast.ast.psi

import ktast.ast.ExtrasMap
import ktast.ast.Node
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import java.util.*

open class ConverterWithExtras : Converter(), ExtrasMap {
    // Sometimes many nodes are created from the same element, but we only want the last node we're given. We
    // remove the previous nodes we've found for the same identity when we see a new one. So we don't have to
    // keep PSI elems around, we hold a map to the element's identity hash code. Then we use that number to tie
    // to the extras to keep duplicates out. Usually using identity hash codes would be problematic due to
    // potential reuse, we know the PSI objects are all around at the same time so it's good enough.
    protected val psiIdentitiesToNodes = mutableMapOf<Int, Node>()
    protected val extrasBefore = IdentityHashMap<Node, List<Node.Extra>>()
    protected val extrasWithin = IdentityHashMap<Node, List<Node.Extra>>()
    protected val extrasAfter = IdentityHashMap<Node, List<Node.Extra>>()

    // This keeps track of ws nodes we've seen before so we don't duplicate them
    private val seenExtraPsiIdentities = mutableSetOf<Int>()

    override fun extrasBefore(v: Node) = extrasBefore[v] ?: emptyList()
    override fun extrasWithin(v: Node) = extrasWithin[v] ?: emptyList()
    override fun extrasAfter(v: Node) = extrasAfter[v] ?: emptyList()

    override fun onNode(node: Node, elem: PsiElement?) {
        // We ignore whitespace and comments here to prevent recursion
        if (elem is PsiWhiteSpace || elem is PsiComment || elem == null) return
        // If we've done this elem before, just set this node as the curr and move on
        val elemId = System.identityHashCode(elem)
        if (psiIdentitiesToNodes.contains(elemId)) {
            return
        }
        psiIdentitiesToNodes[elemId] = node

        if (node is Node.KotlinEntry) {
            fillWholeExtras(node, elem)
        }
    }

    protected open fun fillWholeExtras(rootNode: Node.KotlinEntry, rootElement: PsiElement) {
        val extraElementsSinceLastNode = mutableListOf<PsiElement>()

        val visitor = object : PsiElementVisitor() {
            private var lastNode: Node? = null

            override fun onBeginElement(element: PsiElement) {
                fillExtrasFor(element)
            }

            override fun onEndElement(element: PsiElement) {
                val node = psiIdentitiesToNodes[System.identityHashCode(element)] ?: return
                if (lastNode != null) {
                    fillExtrasAfter(lastNode!!)
                } else {
                    fillExtrasWithin(node)
                }
                lastNode = node
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
                        fillExtrasAfter(lastNode!!)
                    }
                } else {
                    fillExtrasBefore(node)
                }
                lastNode = node
            }

            private fun fillExtrasBefore(node: Node) {
                convertExtras(extraElementsSinceLastNode).also {
                    if (it.isNotEmpty()) extrasBefore[node] = (extrasBefore[node] ?: listOf()) + it
                }
                extraElementsSinceLastNode.clear()
            }

            private fun fillExtrasAfter(node: Node) {
                convertExtras(extraElementsSinceLastNode).also {
                    if (it.isNotEmpty()) extrasAfter[node] = (extrasAfter[node] ?: listOf()) + it
                }
                extraElementsSinceLastNode.clear()
            }

            private fun fillExtrasWithin(node: Node) {
                convertExtras(extraElementsSinceLastNode).also {
                    if (it.isNotEmpty()) extrasWithin[node] = (extrasWithin[node] ?: listOf()) + it
                }
                extraElementsSinceLastNode.clear()
            }
        }
        visitor.visit(rootElement)
    }

    protected open fun isExtra(e: PsiElement) =
        e is PsiWhiteSpace || e is PsiComment || isSemicolon(e)

    protected fun isSemicolon(e: PsiElement) = e.node.elementType == KtTokens.SEMICOLON

    protected open fun convertExtras(elems: List<PsiElement>): List<Node.Extra> = elems.mapNotNull { elem ->
        // Ignore elems we've done before
        val elemId = System.identityHashCode(elem)
        if (!seenExtraPsiIdentities.add(elemId)) null else when {
            elem is PsiWhiteSpace -> Node.Extra.Whitespace(elem.text)
            elem is PsiComment -> Node.Extra.Comment(elem.text)
            elem.node.elementType == KtTokens.SEMICOLON -> Node.Extra.Semicolon()
            else -> error("elems must contain only PsiWhiteSpace or PsiComment or SEMICOLON elements.")
        }
    }
}