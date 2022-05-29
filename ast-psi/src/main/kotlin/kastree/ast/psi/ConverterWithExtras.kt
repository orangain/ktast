package kastree.ast.psi

import kastree.ast.ExtrasMap
import kastree.ast.Node
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
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
    protected val extrasBefore = mutableMapOf<Int, List<Node.Extra>>()
    protected val extrasWithin = mutableMapOf<Int, List<Node.Extra>>()
    protected val extrasAfter = mutableMapOf<Int, List<Node.Extra>>()
    // This keeps track of ws nodes we've seen before so we don't duplicate them
    protected val seenExtraPsiIdentities = mutableSetOf<Int>()

    override fun extrasBefore(v: Node) = nodesToPsiIdentities[v]?.let { extrasBefore[it] } ?: emptyList()
    override fun extrasWithin(v: Node) = nodesToPsiIdentities[v]?.let { extrasWithin[it] } ?: emptyList()
    override fun extrasAfter(v: Node) = nodesToPsiIdentities[v]?.let { extrasAfter[it] } ?: emptyList()

    internal val allExtrasBefore get() = extrasBefore
    internal val allExtrasAfter get() = extrasAfter

    override fun onNode(node: Node, elem: PsiElement?) {
        // We ignore whitespace and comments here to prevent recursion
        if (elem is PsiWhiteSpace || elem is PsiComment || elem == null) return
        // If we've done this elem before, just set this node as the curr and move on
        val elemId = System.identityHashCode(elem)
        nodesToPsiIdentities[node] = elemId
        psiIdentitiesToNodes.put(elemId, node)?.also { prevNode ->
            nodesToPsiIdentities.remove(prevNode)
            return
        }
        // Since we've never done this element before, grab its extras and persist
        val (beforeElems, withinElems, afterElems) = nodeExtraElems(elem)
        convertExtras(beforeElems).map {
            // As a special case, we make sure all non-block comments start a line when "before"
            if (it is Node.Extra.Comment && !it.startsLine && it.text.startsWith("//")) it.copy(startsLine = true)
            else it
        }.also { if (it.isNotEmpty()) extrasBefore[elemId] = it }
        convertExtras(withinElems).also { if (it.isNotEmpty()) extrasWithin[elemId] = it }
        convertExtras(afterElems).also { if (it.isNotEmpty()) extrasAfter[elemId] = it }
    }

    open fun nodeExtraElems(elem: PsiElement): Triple<List<PsiElement>, List<PsiElement>, List<PsiElement>> {
        val before = mutableListOf<PsiElement>()
        var within = mutableListOf<PsiElement>()
        var after = mutableListOf<PsiElement>()

        // Before starts with all directly above ws/comments (reversed to be top-down)
        before += elem.siblings(forward = false, withItself = false).takeWhile {
            it is PsiWhiteSpace || it is PsiComment
        }.toList().reversed()

        // Go over every child...
        var seenInvalid = false
        elem.allChildren.forEach { child ->
            if (child is PsiWhiteSpace || child is PsiComment) {
                // If it's a ws/comment before anything else, it's a before
                if (!seenInvalid) before += child else {
                    // Otherwise it's within or after
                    within.add(child)
                    after.add(child)
                }
            } else {
                seenInvalid = true
                // Clear after since we've seen a non-ws node
                after.clear()
            }
        }
        // Within needs to have the after vals trimmed
        within = within.subList(0, within.size - after.size)

        // After includes all siblings before the first newline or all if there are only ws/comment siblings
        var indexOfFirstNewline = -1
        var seenNonWs = false
        elem.siblings(forward = true, withItself = false).forEach {
            if (it !is PsiWhiteSpace && it !is PsiComment) seenNonWs = true
            else if (!seenNonWs) {
                if (indexOfFirstNewline == -1 && it is PsiWhiteSpace && it.textContains('\n'))
                    indexOfFirstNewline = after.size
                after.add(it)
            }
        }
        if (seenNonWs && indexOfFirstNewline != -1) after = after.subList(0, indexOfFirstNewline)

        return Triple(before, within, after)
    }

    open fun convertExtras(elems: List<PsiElement>): List<Node.Extra> = elems.mapNotNull { elem ->
        // Ignore elems we've done before
        val elemId = System.identityHashCode(elem)
        if (!seenExtraPsiIdentities.add(elemId)) null else when (elem) {
            is PsiWhiteSpace -> elem.text.count { it == '\n' }.let { newlineCount ->
                if (newlineCount > 1) Node.Extra.BlankLines(newlineCount - 1).map(elem) else null
            }
            is PsiComment -> Node.Extra.Comment(
                text = elem.text,
                startsLine = ((elem.prevSibling ?: elem.prevLeaf()) as? PsiWhiteSpace)?.textContains('\n') == true,
                endsLine = elem.tokenType == KtTokens.EOL_COMMENT ||
                        ((elem.nextSibling ?: elem.nextLeaf()) as? PsiWhiteSpace)?.textContains('\n') == true
            ).map(elem)
            else -> null
        }
    }
}