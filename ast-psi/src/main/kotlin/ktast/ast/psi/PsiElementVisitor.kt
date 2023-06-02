package ktast.ast.psi

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.psiUtil.allChildren

open class PsiElementVisitor {
    fun visit(element: PsiElement) {
        if (element is LeafPsiElement) {
            onLeafElement(element)
        } else {
            onBeginElement(element)
            element.allChildren.forEach { visit(it) }
            onEndElement(element)
        }
    }

    protected open fun onBeginElement(element: PsiElement) {}

    protected open fun onEndElement(element: PsiElement) {}

    protected open fun onLeafElement(element: LeafPsiElement) {}
}