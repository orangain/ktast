package ktast.ast.psi

import ktast.ast.Node
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.psi.api.KDocElement
import org.jetbrains.kotlin.psi.psiUtil.allChildren

/**
 * A visitor that visits all elements in a PSI tree except for descendants of [KDocElement].
 */
internal open class PsiElementVisitor {
    fun visit(element: PsiElement) {
        if (element is LeafPsiElement || element is KDocElement) {
            onLeafElement(element)
        } else {
            onBeginElement(element)
            element.allChildren.forEach { visit(it) }
            onEndElement(element)
        }
    }

    protected open fun onBeginElement(element: PsiElement) {}

    protected open fun onEndElement(element: PsiElement) {}

    protected open fun onLeafElement(element: PsiElement) {}
}

internal open class PsiElementAndNodeVisitor(
    private val getNode: (PsiElement) -> Node?
) {
    protected val ancestors = ArrayDeque<Node>()

    private val visitor = object : PsiElementVisitor() {
        override fun onBeginElement(element: PsiElement) {
            val node = getNode(element)
            onBeginElement(element, node)
            if (node != null) {
                ancestors.add(node)
            }
        }

        override fun onEndElement(element: PsiElement) {
            val node = getNode(element)
            onEndElement(element, node)
            if (node != null) {
                ancestors.removeLast()
            }
        }

        override fun onLeafElement(element: PsiElement) {
            val node = getNode(element)
            onLeafElement(element, node)
        }
    }

    internal fun visit(element: PsiElement) {
        visitor.visit(element)
    }

    protected open fun onBeginElement(element: PsiElement, node: Node?) {}

    protected open fun onEndElement(element: PsiElement, node: Node?) {}

    protected open fun onLeafElement(element: PsiElement, node: Node?) {}
}