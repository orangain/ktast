package ktast.ast.psi

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren

internal fun PsiElement.nonExtraChildren() =
    allChildren.filterNot { it is PsiComment || it is PsiWhiteSpace }.toList()

internal val KtImportDirective.asterisk: PsiElement?
    get() = findChildByType(this, KtTokens.MUL)

internal val KtTypeParameterList.leftAngle: PsiElement?
    get() = findChildByType(this, KtTokens.LT)
internal val KtTypeParameterList.rightAngle: PsiElement?
    get() = findChildByType(this, KtTokens.GT)

internal val KtTypeArgumentList.leftAngle: PsiElement?
    get() = findChildByType(this, KtTokens.LT)
internal val KtTypeArgumentList.rightAngle: PsiElement?
    get() = findChildByType(this, KtTokens.GT)

internal val KtDeclarationWithInitializer.equalsToken: PsiElement
    get() = findChildByType(this, KtTokens.EQ) ?: error("No equals token for $this")
internal val KtInitializerList.valueArgumentList: KtValueArgumentList
    get() = (initializers.firstOrNull() as? KtSuperTypeCallEntry)?.valueArgumentList
        ?: error("No value arguments for $this")
internal val KtTypeAlias.equalsToken: PsiElement
    get() = findChildByType(this, KtTokens.EQ) ?: error("No equals token for $this")

internal val KtPropertyAccessor.setKeyword: PsiElement
    get() = findChildByType(this, KtTokens.SET_KEYWORD) ?: error("No set keyword for $this")
internal val KtPropertyAccessor.getKeyword: PsiElement
    get() = findChildByType(this, KtTokens.GET_KEYWORD) ?: error("No get keyword for $this")

internal val KtQualifiedExpression.operator: PsiElement
    get() = operationTokenNode.psi

internal val KtDoubleColonExpression.questionMarks
    get() = allChildren
        .takeWhile { it.node.elementType != KtTokens.COLONCOLON }
        .filter { it.node.elementType == KtTokens.QUEST }
        .toList()

internal val KtNullableType.questionMark: PsiElement
    get() = questionMarkNode.psi
internal val KtDynamicType.dynamicKeyword: PsiElement
    get() = findChildByType(this, KtTokens.DYNAMIC_KEYWORD) ?: error("No dynamic keyword for $this")
internal val KtFunctionType.dotSymbol: PsiElement?
    get() = findChildByType(this, KtTokens.DOT)

internal val KtAnnotation.atSymbol: PsiElement?
    get() = findChildByType(this, KtTokens.AT)
internal val KtAnnotation.colon: PsiElement?
    get() = findChildByType(this, KtTokens.COLON)
internal val KtAnnotation.lBracket: PsiElement?
    get() = findChildByType(this, KtTokens.LBRACKET)
internal val KtAnnotation.rBracket: PsiElement?
    get() = findChildByType(this, KtTokens.RBRACKET)
internal val KtAnnotationEntry.colon: PsiElement?
    get() = findChildByType(this, KtTokens.COLON)

internal val KtContextReceiverList.leftParenthesis: PsiElement
    get() = findChildByType(this, KtTokens.LPAR) ?: error("No left parenthesis for $this")
internal val KtContextReceiverList.rightParenthesis: PsiElement
    get() = findChildByType(this, KtTokens.RPAR) ?: error("No right parenthesis for $this")
internal val KtContractEffectList.leftBracket: PsiElement
    get() = findChildByType(this, KtTokens.LBRACKET) ?: error("No left bracket for $this")
internal val KtContractEffectList.rightBracket: PsiElement
    get() = findChildByType(this, KtTokens.RBRACKET) ?: error("No right bracket for $this")

private fun findChildByType(v: KtElement, type: IElementType): PsiElement? =
    v.node.findChildByType(type)?.psi