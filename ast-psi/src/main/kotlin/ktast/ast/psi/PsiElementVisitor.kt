package ktast.ast.psi

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.psi.api.KDocElement
import org.jetbrains.kotlin.psi.psiUtil.allChildren

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