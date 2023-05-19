package ktast.ast.psi

import ktast.ast.ExtrasMap
import ktast.ast.Node
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.siblings
import java.util.*

open class ConverterWithExtras : Converter(), ExtrasMap {
    // Sometimes many nodes are created from the same element, but we only want the last node we're given. We
    // remove the previous nodes we've found for the same identity when we see a new one. So we don't have to
    // keep PSI elems around, we hold a map to the element's identity hash code. Then we use that number to tie
    // to the extras to keep duplicates out. Usually using identity hash codes would be problematic due to
    // potential reuse, we know the PSI objects are all around at the same time so it's good enough.
    protected val nodesToPsiIdentities = IdentityHashMap<Node, Int>()
    protected val psiIdentitiesToNodes = mutableMapOf<Int, Node>()
    protected val extrasBefore = IdentityHashMap<Node, List<Node.Extra>>()
    protected val extrasWithin = IdentityHashMap<Node, List<Node.Extra>>()
    protected val extrasAfter = IdentityHashMap<Node, List<Node.Extra>>()

    // This keeps track of ws nodes we've seen before so we don't duplicate them
    protected val seenExtraPsiIdentities = mutableSetOf<Int>()

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
        nodesToPsiIdentities[node] = elemId
        psiIdentitiesToNodes.put(elemId, node)
        // Since we've never done this element before, grab its extras and persist
        val (beforeElems, withinElems, afterElems) = nodeExtraElems(elem)
        convertExtras(beforeElems).also { if (it.isNotEmpty()) extrasBefore[node] = it }
        convertExtras(withinElems).also { if (it.isNotEmpty()) extrasWithin[node] = it }
        convertExtras(afterElems).also { if (it.isNotEmpty()) extrasAfter[node] = it }
    }

    protected open fun nodeExtraElems(elem: PsiElement): Triple<List<PsiElement>, List<PsiElement>, List<PsiElement>> {
        // Before starts with all directly above ws/comments (reversed to be top-down)
        val before = elem.siblings(forward = false, withItself = false)
            .filterNot(::shouldSkip)
            .takeWhile(::isExtra)
            .toList().reversed()

        // Go over every child...
        val within = elem.allChildren
            .filterNot(::shouldSkip)
            .filter(::isExtra)
            .toList()

        val after = elem.siblings(forward = true, withItself = false)
            .filterNot(::shouldSkip)
            .takeWhile(::isExtra)
            .toList()

        return Triple(before, within, after)
    }

    protected open fun shouldSkip(e: PsiElement) =
        e is KtImportList && e.imports.isEmpty()

    protected open fun isExtra(e: PsiElement) =
        e is PsiWhiteSpace || e is PsiComment || e.node.elementType == KtTokens.SEMICOLON

    protected open fun convertExtras(elems: List<PsiElement>): List<Node.Extra> = elems.mapNotNull { elem ->
        // Ignore elems we've done before
        val elemId = System.identityHashCode(elem)
        if (!seenExtraPsiIdentities.add(elemId)) null else when {
            elem is PsiWhiteSpace -> Node.Whitespace(elem.text)
            elem is PsiComment -> Node.Comment(elem.text)
            elem.node.elementType == KtTokens.SEMICOLON -> Node.Semicolon(elem.text)
            else -> error("elems must contain only PsiWhiteSpace or PsiComment or SEMICOLON elements.")
        }
    }
}