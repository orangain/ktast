package ktast.ast.psi

import ktast.ast.Node
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

open class Converter {
    protected open fun onNode(node: Node, elem: PsiElement?) {}

    open fun convertFile(v: KtFile) = Node.File(
        anns = convertAnnotationSets(v),
        pkg = v.packageDirective?.takeIf { it.packageNames.isNotEmpty() }?.let(::convertPackage),
        imports = v.importDirectives.map(::convertImport),
        decls = v.declarations.map(::convertDecl)
    ).map(v)

    open fun convertPackage(v: KtPackageDirective) = Node.Package(
        mods = convertModifiers(v),
        packageNameExpr = v.packageNameExpression?.let(::convertExpr) ?: error("No package name expression for $v"),
    ).map(v)

    open fun convertImport(v: KtImportDirective) = Node.Import(
        importKeyword = convertKeyword(
            findChildByType(v, KtTokens.IMPORT_KEYWORD) ?: error("Missing import keyword"),
            Node.Keyword::Import
        ),
        names = v.importedFqName?.pathSegments()?.map { it.asString() } ?: error("Missing import path"),
        wildcard = v.isAllUnder,
        alias = v.aliasName
    ).map(v)

    open fun convertDecls(v: KtClassBody): Node.NodeList<Node.Decl> = Node.NodeList(
        children = v.declarations.map(::convertDecl),
        separator = "",
        prefix = "{",
        suffix = "}",
    ).map(v)

    open fun convertDecl(v: KtDeclaration): Node.Decl = when (v) {
        is KtEnumEntry -> convertEnumEntry(v)
        is KtClassOrObject -> convertStructured(v)
        is KtAnonymousInitializer -> convertInit(v)
        is KtNamedFunction -> convertFunc(v)
        is KtDestructuringDeclaration -> convertProperty(v)
        is KtProperty -> convertProperty(v)
        is KtTypeAlias -> convertTypeAlias(v)
        is KtSecondaryConstructor -> convertConstructor(v)
        else -> error("Unrecognized declaration type for $v")
    }

    open fun convertStructured(v: KtClassOrObject) = Node.Decl.Structured(
        mods = convertModifiers(v),
        declarationKeyword = v.getDeclarationKeyword()?.let(::convertDeclarationKeyword)
            ?: error("declarationKeyword not found"),
        name = v.nameIdentifier?.let(::convertName),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        primaryConstructor = v.primaryConstructor?.let(::convertPrimaryConstructor),
        colon = v.getColon()?.let { convertKeyword(it, Node.Keyword::Colon) },
        // TODO: this
        parentAnns = emptyList(),
        parents = v.superTypeListEntries.map(::convertParent),
        typeConstraints = v.typeConstraints.map(::convertTypeConstraint),
        body = v.body?.let { convertDecls(it) },
    ).map(v)

    open fun convertParent(v: KtSuperTypeListEntry) = when (v) {
        is KtSuperTypeCallEntry -> Node.Decl.Structured.Parent.CallConstructor(
            type = v.typeReference?.typeElement?.let(::convertType) as? Node.Type.Simple
                ?: error("Bad type on super call $v"),
            typeArgs = v.typeArguments.map(::convertTypeProjection),
            args = v.valueArgumentList?.let(::convertValueArgs),
            // TODO
            lambda = null
        ).map(v)
        is KtDelegatedSuperTypeEntry -> Node.Decl.Structured.Parent.DelegatedType(
            type = v.typeReference?.typeElement?.let(::convertType) as? Node.Type.Simple
                ?: error("Bad type on super call $v"),
            byKeyword = convertKeyword(v.byKeywordNode.psi, Node.Keyword::By),
            expr = convertExpr(v.delegateExpression ?: error("Missing delegateExpression for $v")),
        ).map(v)
        is KtSuperTypeEntry -> Node.Decl.Structured.Parent.Type(
            type = v.typeReference?.typeElement?.let(::convertType) as? Node.Type.Simple
                ?: error("Bad type on super call $v"),
        ).map(v)
        else -> error("Unknown super type entry $v")
    }

    open fun convertPrimaryConstructor(v: KtPrimaryConstructor) = Node.Decl.Structured.PrimaryConstructor(
        mods = convertModifiers(v),
        constructorKeyword = v.getConstructorKeyword()?.let { convertKeyword(it, Node.Keyword::Constructor) },
        params = v.valueParameterList?.let(::convertFuncParams)
    ).map(v)

    open fun convertInit(v: KtAnonymousInitializer) = Node.Decl.Init(
        block = convertBlock(v.body as? KtBlockExpression ?: error("No init block for $v"))
    ).map(v)

    open fun convertFunc(v: KtNamedFunction) = Node.Decl.Func(
        mods = convertModifiers(v),
        funKeyword = v.funKeyword?.let { convertKeyword(it, Node.Keyword::Fun) } ?: error("No fun keyword for $v"),
        typeParams =
        if (v.hasTypeParameterListBeforeFunctionName()) v.typeParameterList?.let(::convertTypeParams) else null,
        receiverTypeRef = v.receiverTypeReference?.let(::convertTypeRef),
        name = v.nameIdentifier?.let(::convertName),
        paramTypeParams =
        if (!v.hasTypeParameterListBeforeFunctionName()) v.typeParameterList?.let(::convertTypeParams) else null,
        params = v.valueParameterList?.let(::convertFuncParams),
        typeRef = v.typeReference?.let(::convertTypeRef),
        postMods = convertPostModifiers(v),
        body = v.bodyExpression?.let(::convertFuncBody)
    ).map(v)

    open fun convertFuncParams(v: KtParameterList) = Node.Decl.Func.Params(
        params = v.parameters.map(::convertFuncParam),
    ).map(v)

    open fun convertFuncParam(v: KtParameter) = Node.Decl.Func.Params.Param(
        mods = convertModifiers(v),
        readOnly = if (v.hasValOrVar()) !v.isMutable else null,
        name = v.nameIdentifier?.let(::convertName) ?: error("No param name"),
        typeRef = v.typeReference?.let(::convertTypeRef),
        initializer = v.defaultValue?.let {
            convertInitializer(v.equalsToken ?: error("No equals token for initializer of $v"), it, v)
        },
    ).map(v)

    open fun convertFuncBody(v: KtExpression) =
        if (v is KtBlockExpression) Node.Decl.Func.Body.Block(convertBlock(v)).map(v)
        else Node.Decl.Func.Body.Expr(convertExpr(v)).map(v)

    open fun convertProperty(v: KtProperty) = Node.Decl.Property(
        mods = convertModifiers(v),
        valOrVar = convertValOrVarKeyword(v.valOrVarKeyword),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        receiverTypeRef = v.receiverTypeReference?.let(::convertTypeRef),
        vars = listOf(
            Node.Decl.Property.Var(
                name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
                typeRef = v.typeReference?.let(::convertTypeRef)
            ).mapNotCorrespondsPsiElement(v)
        ),
        typeConstraints = v.typeConstraints.map(::convertTypeConstraint),
        initializer = v.initializer?.let {
            convertInitializer(v.equalsToken ?: error("No equals token for initializer of $v"), it, v)
        },
        delegate = v.delegate?.let(::convertPropertyDelegate),
        accessors = v.accessors.map(::convertPropertyAccessor).let {
            if (it.isEmpty()) null else Node.Decl.Property.Accessors(
                first = it.first(),
                second = it.getOrNull(1)
            )
        }
    ).map(v)

    open fun convertProperty(v: KtDestructuringDeclaration) = Node.Decl.Property(
        mods = convertModifiers(v),
        valOrVar = v.valOrVarKeyword?.let(::convertValOrVarKeyword) ?: error("Missing valOrVarKeyword"),
        typeParams = null,
        receiverTypeRef = null,
        vars = v.entries.map(::convertPropertyVar),
        typeConstraints = emptyList(),
        initializer = v.initializer?.let {
            convertInitializer(findChildByType(v, KtTokens.EQ) ?: error("No equals token for initializer of $v"), it, v)
        },
        delegate = null,
        accessors = null
    ).map(v)

    open fun convertPropertyDelegate(v: KtPropertyDelegate) = Node.Decl.Property.Delegate(
        byKeyword = convertKeyword(v.byKeywordNode.psi, Node.Keyword::By),
        expr = convertExpr(v.expression ?: error("Missing expression for $v")),
    ).map(v)

    open fun convertPropertyVars(v: KtParameter) =
        v.destructuringDeclaration?.entries?.map(::convertPropertyVar) ?: listOf(
            if (v.name == "_") null else Node.Decl.Property.Var(
                name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
                typeRef = v.typeReference?.let(::convertTypeRef)
            ).map(v)
        )

    open fun convertPropertyVar(v: KtDestructuringDeclarationEntry) =
        if (v.name == "_") null else Node.Decl.Property.Var(
            name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
            typeRef = v.typeReference?.let(::convertTypeRef)
        ).map(v)

    open fun convertPropertyAccessor(v: KtPropertyAccessor) =
        if (v.isGetter) Node.Decl.Property.Accessor.Get(
            mods = convertModifiers(v),
            typeRef = v.returnTypeReference?.let(::convertTypeRef),
            contractKeyword = findChildByType(v, KtTokens.CONTRACT_KEYWORD)?.let {
                convertKeyword(it, Node.Keyword::Contract)
            },
            contractEffects = v.contractDescription?.let(::convertContractEffects),
            body = v.bodyExpression?.let(::convertFuncBody)
        ).map(v) else Node.Decl.Property.Accessor.Set(
            mods = convertModifiers(v),
            paramMods = v.parameter?.let(::convertModifiers) ?: emptyList(),
            paramName = v.parameter?.nameIdentifier?.let(::convertName),
            paramTypeRef = v.parameter?.typeReference?.let(::convertTypeRef),
            contractKeyword = findChildByType(v, KtTokens.CONTRACT_KEYWORD)?.let {
                convertKeyword(it, Node.Keyword::Contract)
            },
            contractEffects = v.contractDescription?.let(::convertContractEffects),
            body = v.bodyExpression?.let(::convertFuncBody)
        ).map(v)

    open fun convertTypeAlias(v: KtTypeAlias) = Node.Decl.TypeAlias(
        mods = convertModifiers(v),
        name = v.nameIdentifier?.let(::convertName) ?: error("No type alias name for $v"),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        typeRef = convertTypeRef(v.getTypeReference() ?: error("No type alias ref for $v"))
    ).map(v)

    open fun convertConstructor(v: KtSecondaryConstructor) = Node.Decl.Constructor(
        mods = convertModifiers(v),
        constructorKeyword = convertKeyword(v.getConstructorKeyword(), Node.Keyword::Constructor),
        params = v.valueParameterList?.let(::convertFuncParams),
        delegationCall = if (v.hasImplicitDelegationCall()) null else v.getDelegationCall().let {
            Node.Decl.Constructor.DelegationCall(
                target =
                if (it.isCallToThis) Node.Decl.Constructor.DelegationTarget.THIS
                else Node.Decl.Constructor.DelegationTarget.SUPER,
                args = it.valueArgumentList?.let(::convertValueArgs)
            ).map(it)
        },
        block = v.bodyExpression?.let(::convertBlock)
    ).map(v)

    open fun convertEnumEntry(v: KtEnumEntry) = Node.Decl.EnumEntry(
        mods = convertModifiers(v),
        name = v.nameIdentifier?.let(::convertName) ?: error("Unnamed enum"),
        args = v.initializerList?.let(::convertValueArgs),
        members = v.declarations.map(::convertDecl),
        hasComma = findChildByType(v, KtTokens.COMMA) != null,
    ).map(v)

    open fun convertInitializer(equalsToken: PsiElement, expr: KtExpression, parent: PsiElement) = Node.Initializer(
        equals = convertKeyword(equalsToken, Node.Keyword::Equal),
        expr = convertExpr(expr),
    ).mapNotCorrespondsPsiElement(parent)

    open fun convertTypeParams(v: KtTypeParameterList) = Node.TypeParams(
        params = v.parameters.map(::convertTypeParam),
    ).map(v)

    open fun convertTypeParam(v: KtTypeParameter) = Node.TypeParams.TypeParam(
        mods = convertModifiers(v),
        name = v.nameIdentifier?.let(::convertName) ?: error("No type param name for $v"),
        typeRef = v.extendsBound?.let(::convertTypeRef)
    ).map(v)

    open fun convertTypeProjections(v: KtTypeArgumentList?) = v?.arguments?.map {
        if (it.projectionKind == KtProjectionKind.STAR) null
        else convertTypeProjection(it)
    } ?: emptyList()

    open fun convertTypeProjection(v: KtTypeProjection): Node.TypeProjection? =
        v.typeReference?.let {
            Node.TypeProjection(
                mods = convertModifiers(v.modifierList),
                typeRef = convertTypeRef(it),
            ).map(v)
        }

    open fun convertTypeRef(v: KtTypeReference): Node.TypeRef {
        var lPar: PsiElement? = null
        var rPar: PsiElement? = null
        var mods: KtModifierList? = null
        var innerMods: KtModifierList? = null
        v.allChildren.forEach {
            when (it) {
                is KtModifierList -> {
                    if (lPar == null) {
                        mods = it
                    } else {
                        innerMods = it
                    }
                }
                else -> {
                    when (it.node.elementType) {
                        KtTokens.LPAR -> lPar = it
                        KtTokens.RPAR -> rPar = it
                        else -> {}
                    }
                }
            }
        }

        return Node.TypeRef(
            contextReceivers = v.contextReceiverList?.let { convertContextReceivers(it) },
            mods = convertModifiers(mods),
            lPar = lPar?.let { convertKeyword(it, Node.Keyword::LPar) },
            innerMods = convertModifiers(innerMods),
            type = v.typeElement?.let { convertType(it) }, // v.typeElement is null when the type reference has only context receivers.
            rPar = rPar?.let { convertKeyword(it, Node.Keyword::RPar) },
        ).map(v)
    }

    open fun convertTypeConstraint(v: KtTypeConstraint) = Node.PostModifier.TypeConstraints.TypeConstraint(
        anns = v.children.mapNotNull {
            when (it) {
                is KtAnnotationEntry -> convertAnnotationSet(it)
                is KtAnnotation -> convertAnnotationSet(it)
                else -> null
            }
        },
        name = v.subjectTypeParameterName?.let { convertName(it) } ?: error("No type constraint name for $v"),
        typeRef = convertTypeRef(v.boundTypeReference ?: error("No type constraint type for $v"))
    ).map(v)

    open fun convertType(v: KtTypeElement): Node.Type = when (v) {
        is KtFunctionType -> Node.Type.Func(
            receiverTypeRef = v.receiverTypeReference?.let(::convertTypeRef),
            params = v.parameterList?.let { convertTypeFuncParams(it) },
            typeRef = convertTypeRef(v.returnTypeReference ?: error("No return type"))
        ).map(v)
        is KtUserType -> Node.Type.Simple(
            pieces = generateSequence(v) { it.qualifier }.toList().reversed().map {
                Node.Type.Simple.Piece(
                    name = it.referenceExpression?.let(::convertName) ?: error("No type name for $it"),
                    typeParams = convertTypeProjections(it.typeArgumentList)
                ).mapNotCorrespondsPsiElement(it)
            }
        ).map(v)
        is KtNullableType -> Node.Type.Nullable(
            lPar = findChildAndConvertLPar(v),
            mods = convertModifiers(v.modifierList),
            type = convertType(v.innerType ?: error("No inner type for nullable")),
            rPar = findChildAndConvertRPar(v),
        ).map(v)
        is KtDynamicType -> Node.Type.Dynamic().map(v)
        else -> error("Unrecognized type of $v")
    }

    open fun convertTypeFuncParams(v: KtParameterList): Node.NodeList<Node.Type.Func.Param> = Node.NodeList(
        children = v.parameters.map(::convertTypeFuncParam),
        separator = ",",
        prefix = "(",
        suffix = ")",
        trailingSeparator = findTrailingSeparator(v, KtTokens.COMMA)?.let { convertKeyword(it, Node.Keyword::Comma) }
    ).map(v)

    open fun convertTypeFuncParam(v: KtParameter) = Node.Type.Func.Param(
        name = v.nameIdentifier?.let(::convertName),
        typeRef = convertTypeRef(v.typeReference ?: error("No param type"))
    ).map(v)

    open fun convertContextReceivers(v: KtContextReceiverList): Node.NodeList<Node.ContextReceiver> = Node.NodeList(
        children = v.contextReceivers().map(::convertContextReceiver),
        separator = ",",
        prefix = "(",
        suffix = ")",
    ).map(v)

    open fun convertContextReceiver(v: KtContextReceiver) = Node.ContextReceiver(
        typeRef = convertTypeRef(v.typeReference() ?: error("Missing type reference for $v")),
    ).map(v)

    open fun convertContractEffects(v: KtContractEffectList): Node.NodeList<Node.PostModifier.Contract.ContractEffect> =
        Node.NodeList(
            children = v.children.filterIsInstance<KtContractEffect>().map(::convertContractEffect),
            separator = ",",
            prefix = "[",
            suffix = "]",
            trailingSeparator = findTrailingSeparator(v, KtTokens.COMMA)
                ?.let { convertKeyword(it, Node.Keyword::Comma) },
        ).map(v)

    open fun convertContractEffect(v: KtContractEffect) = Node.PostModifier.Contract.ContractEffect(
        expr = convertExpr(v.getExpression()),
    ).map(v)

    open fun convertValueArgs(v: KtValueArgumentList) = Node.ValueArgs(
        args = v.arguments.map(::convertValueArg)
    ).map(v)

    open fun convertValueArgs(v: KtInitializerList) = Node.ValueArgs(
        args = ((v.initializers.firstOrNull() as? KtSuperTypeCallEntry)?.valueArgumentList?.arguments
            ?: error("No value arguments for $v")).map(::convertValueArg)
    ).map(v)

    open fun convertValueArg(v: KtValueArgument) = Node.ValueArgs.ValueArg(
        name = v.getArgumentName()?.referenceExpression?.let(::convertName),
        asterisk = v.getSpreadElement() != null,
        expr = convertExpr(v.getArgumentExpression() ?: error("No expr for value arg"))
    ).map(v)

    open fun convertExpr(v: KtExpression): Node.Expr = when (v) {
        is KtIfExpression -> convertIf(v)
        is KtTryExpression -> convertTry(v)
        is KtForExpression -> convertFor(v)
        is KtWhileExpressionBase -> convertWhile(v)
        is KtBinaryExpression -> convertBinaryOp(v)
        is KtQualifiedExpression -> convertBinaryOp(v)
        is KtUnaryExpression -> convertUnaryOp(v)
        is KtBinaryExpressionWithTypeRHS -> convertTypeOp(v)
        is KtIsExpression -> convertTypeOp(v)
        is KtCallableReferenceExpression -> convertDoubleColonRefCallable(v)
        is KtClassLiteralExpression -> convertDoubleColonRefClass(v)
        is KtParenthesizedExpression -> convertParen(v)
        is KtStringTemplateExpression -> convertStringTmpl(v)
        is KtConstantExpression -> convertConst(v)
        is KtBlockExpression -> convertBlock(v)
        is KtFunctionLiteral -> error("Supposed to be unreachable here. KtFunctionLiteral is expected to be inside of KtLambdaExpression.")
        is KtLambdaExpression -> convertLambda(v)
        is KtThisExpression -> convertThis(v)
        is KtSuperExpression -> convertSuper(v)
        is KtWhenExpression -> convertWhen(v)
        is KtObjectLiteralExpression -> convertObject(v)
        is KtThrowExpression -> convertThrow(v)
        is KtReturnExpression -> convertReturn(v)
        is KtContinueExpression -> convertContinue(v)
        is KtBreakExpression -> convertBreak(v)
        is KtCollectionLiteralExpression -> convertCollLit(v)
        is KtSimpleNameExpression -> convertName(v)
        is KtLabeledExpression -> convertLabeled(v)
        is KtAnnotatedExpression -> convertAnnotated(v)
        is KtCallExpression -> convertCall(v)
        is KtArrayAccessExpression -> convertArrayAccess(v)
        is KtNamedFunction -> convertAnonFunc(v)
        is KtProperty -> convertPropertyExpr(v)
        is KtDestructuringDeclaration -> convertPropertyExpr(v)
        // TODO: this is present in a recovery test where an interface decl is on rhs of a gt expr
        is KtClass -> throw Unsupported("Class expressions not supported")
        else -> error("Unrecognized expression type from $v")
    }

    open fun convertIf(v: KtIfExpression) = Node.Expr.If(
        lPar = convertKeyword(v.leftParenthesis ?: error("No leftParenthesis on if for $v"), Node.Keyword::LPar),
        expr = convertExpr(v.condition ?: error("No cond on if for $v")),
        rPar = convertKeyword(v.rightParenthesis ?: error("No rightParenthesis on if for $v"), Node.Keyword::RPar),
        body = convertExpr(v.then ?: error("No then on if for $v")),
        elseBody = v.`else`?.let(::convertExpr)
    ).map(v)

    open fun convertTry(v: KtTryExpression) = Node.Expr.Try(
        block = convertBlock(v.tryBlock),
        catches = v.catchClauses.map(::convertTryCatch),
        finallyBlock = v.finallyBlock?.finalExpression?.let(::convertBlock)
    ).map(v)

    open fun convertTryCatch(v: KtCatchClause) = Node.Expr.Try.Catch(
        anns = v.catchParameter?.annotations?.map(::convertAnnotationSet) ?: emptyList(),
        varName = v.catchParameter?.name ?: error("No catch param name for $v"),
        varType = v.catchParameter?.typeReference?.typeElement?.let(::convertType) as? Node.Type.Simple
            ?: error("Invalid catch param type for $v"),
        block = convertBlock(v.catchBody as? KtBlockExpression ?: error("No catch block for $v"))
    ).map(v)

    open fun convertFor(v: KtForExpression) = Node.Expr.For(
        anns = v.loopParameter?.annotations?.map(::convertAnnotationSet) ?: emptyList(),
        vars = convertPropertyVars(v.loopParameter ?: error("No param on for $v")),
        inExpr = convertExpr(v.loopRange ?: error("No in range for $v")),
        body = convertExpr(v.body ?: error("No body for $v"))
    ).map(v)

    open fun convertWhile(v: KtWhileExpressionBase) = Node.Expr.While(
        expr = convertExpr(v.condition ?: error("No while cond for $v")),
        body = convertExpr(v.body ?: error("No while body for $v")),
        doWhile = v is KtDoWhileExpression
    ).map(v)

    open fun convertBinaryOp(v: KtBinaryExpression) = Node.Expr.BinaryOp(
        lhs = convertExpr(v.left ?: error("No binary lhs for $v")),
        oper = binaryTokensByText[v.operationReference.text].let {
            if (it != null) Node.Expr.BinaryOp.Oper.Token(it).map(v.operationReference)
            else Node.Expr.BinaryOp.Oper.Infix(v.operationReference.text).map(v.operationReference)
        },
        rhs = convertExpr(v.right ?: error("No binary rhs for $v"))
    ).map(v)

    open fun convertBinaryOp(v: KtQualifiedExpression) = Node.Expr.BinaryOp(
        lhs = convertExpr(v.receiverExpression),
        oper = Node.Expr.BinaryOp.Oper.Token(
            if (v is KtDotQualifiedExpression) Node.Expr.BinaryOp.Token.DOT else Node.Expr.BinaryOp.Token.DOT_SAFE
        ),
        rhs = convertExpr(v.selectorExpression ?: error("No qualified rhs for $v"))
    ).map(v)

    open fun convertUnaryOp(v: KtUnaryExpression) = Node.Expr.UnaryOp(
        expr = convertExpr(v.baseExpression ?: error("No unary expr for $v")),
        oper = v.operationReference.let {
            Node.Expr.UnaryOp.Oper(unaryTokensByText[it.text] ?: error("Unable to find op ref $it")).map(it)
        },
        prefix = v is KtPrefixExpression
    ).map(v)

    open fun convertTypeOp(v: KtBinaryExpressionWithTypeRHS) = Node.Expr.TypeOp(
        lhs = convertExpr(v.left),
        oper = v.operationReference.let {
            Node.Expr.TypeOp.Oper(typeTokensByText[it.text] ?: error("Unable to find op ref $it")).map(it)
        },
        rhs = convertTypeRef(v.right ?: error("No type op rhs for $v"))
    ).map(v)

    open fun convertTypeOp(v: KtIsExpression) = Node.Expr.TypeOp(
        lhs = convertExpr(v.leftHandSide),
        oper = v.operationReference.let {
            Node.Expr.TypeOp.Oper(typeTokensByText[it.text] ?: error("Unable to find op ref $it")).map(it)
        },
        rhs = convertTypeRef(v.typeReference ?: error("No type op rhs for $v"))
    )

    open fun convertDoubleColonRefCallable(v: KtCallableReferenceExpression) = Node.Expr.DoubleColonRef.Callable(
        recv = v.receiverExpression?.let { convertDoubleColonRefRecv(it, v.questionMarks) },
        name = convertName(v.callableReference)
    ).map(v)

    open fun convertDoubleColonRefClass(v: KtClassLiteralExpression) = Node.Expr.DoubleColonRef.Class(
        recv = v.receiverExpression?.let { convertDoubleColonRefRecv(it, v.questionMarks) }
    ).map(v)

    open fun convertDoubleColonRefRecv(v: KtExpression, questionMarks: Int): Node.Expr.DoubleColonRef.Recv = when (v) {
        is KtSimpleNameExpression -> Node.Expr.DoubleColonRef.Recv.Type(
            type = Node.Type.Simple(
                listOf(Node.Type.Simple.Piece(convertName(v.getReferencedNameElement()), emptyList()).map(v))
            ).mapNotCorrespondsPsiElement(v),
            questionMarks = questionMarks
        ).map(v)
        is KtCallExpression ->
            if (v.valueArgumentList == null && v.lambdaArguments.isEmpty())
                Node.Expr.DoubleColonRef.Recv.Type(
                    type = Node.Type.Simple(listOf(
                        Node.Type.Simple.Piece(
                            name = v.calleeExpression?.let { (it as? KtSimpleNameExpression)?.let(::convertName) }
                                ?: error("Missing text for call ref type of $v"),
                            typeParams = convertTypeProjections(v.typeArgumentList)
                        ).mapNotCorrespondsPsiElement(v)
                    )).mapNotCorrespondsPsiElement(v),
                    questionMarks = questionMarks
                ).map(v)
            else Node.Expr.DoubleColonRef.Recv.Expr(convertExpr(v)).map(v)
        is KtDotQualifiedExpression -> {
            val lhs = convertDoubleColonRefRecv(v.receiverExpression, questionMarks)
            val rhs = v.selectorExpression?.let { convertDoubleColonRefRecv(it, questionMarks) }
            if (lhs is Node.Expr.DoubleColonRef.Recv.Type && rhs is Node.Expr.DoubleColonRef.Recv.Type)
                Node.Expr.DoubleColonRef.Recv.Type(
                    type = Node.Type.Simple(lhs.type.pieces + rhs.type.pieces).map(v),
                    questionMarks = 0
                ).map(v)
            else Node.Expr.DoubleColonRef.Recv.Expr(convertExpr(v)).map(v)
        }
        else -> Node.Expr.DoubleColonRef.Recv.Expr(convertExpr(v)).map(v)
    }

    open fun convertParen(v: KtParenthesizedExpression) = Node.Expr.Paren(
        expr = convertExpr(v.expression ?: error("No paren expr for $v"))
    )

    open fun convertStringTmpl(v: KtStringTemplateExpression) = Node.Expr.StringTmpl(
        elems = v.entries.map(::convertStringTmplElem),
        raw = v.text.startsWith("\"\"\"")
    ).map(v)

    open fun convertStringTmplElem(v: KtStringTemplateEntry) = when (v) {
        is KtLiteralStringTemplateEntry ->
            Node.Expr.StringTmpl.Elem.Regular(v.text).map(v)
        is KtSimpleNameStringTemplateEntry ->
            Node.Expr.StringTmpl.Elem.ShortTmpl(v.expression?.text ?: error("No short tmpl text")).map(v)
        is KtBlockStringTemplateEntry ->
            Node.Expr.StringTmpl.Elem.LongTmpl(convertExpr(v.expression ?: error("No expr tmpl"))).map(v)
        is KtEscapeStringTemplateEntry ->
            if (v.text.startsWith("\\u"))
                Node.Expr.StringTmpl.Elem.UnicodeEsc(v.text.substring(2)).map(v)
            else
                Node.Expr.StringTmpl.Elem.RegularEsc(v.unescapedValue.first()).map(v)
        else ->
            error("Unrecognized string template type for $v")
    }

    open fun convertConst(v: KtConstantExpression) = Node.Expr.Const(
        value = v.text,
        form = when (v.node.elementType) {
            KtNodeTypes.BOOLEAN_CONSTANT -> Node.Expr.Const.Form.BOOLEAN
            KtNodeTypes.CHARACTER_CONSTANT -> Node.Expr.Const.Form.CHAR
            KtNodeTypes.INTEGER_CONSTANT -> Node.Expr.Const.Form.INT
            KtNodeTypes.FLOAT_CONSTANT -> Node.Expr.Const.Form.FLOAT
            KtNodeTypes.NULL -> Node.Expr.Const.Form.NULL
            else -> error("Unrecognized const type for $v")
        }
    ).map(v)

    open fun convertLambda(v: KtLambdaExpression) = Node.Expr.Lambda(
        params = v.functionLiteral.valueParameterList?.let(::convertLambdaParams),
        body = v.bodyExpression?.let(::convertLambdaBody)
    ).map(v)

    open fun convertLambdaParams(v: KtParameterList): Node.NodeList<Node.Expr.Lambda.Param> = Node.NodeList(
        children = v.parameters.map(::convertLambdaParam),
        separator = ",",
        prefix = "",
        suffix = "",
    ).map(v)

    open fun convertLambdaParam(v: KtParameter) = Node.Expr.Lambda.Param(
        vars = convertPropertyVars(v),
        destructTypeRef = if (v.destructuringDeclaration != null) v.typeReference?.let(::convertTypeRef) else null
    ).map(v)

    open fun convertLambdaBody(v: KtBlockExpression) = Node.Expr.Lambda.Body(
        stmts = v.statements.map(::convertStmtNo)
    ).map(v)

    open fun convertThis(v: KtThisExpression) = Node.Expr.This(
        label = v.getLabelName()
    ).map(v)

    open fun convertSuper(v: KtSuperExpression) = Node.Expr.Super(
        typeArg = v.superTypeQualifier?.let(::convertTypeRef),
        label = v.getLabelName()
    ).map(v)

    open fun convertWhen(v: KtWhenExpression) = Node.Expr.When(
        lPar = convertKeyword(v.leftParenthesis ?: error("No left parenthesis for $v"), Node.Keyword::LPar),
        expr = v.subjectExpression?.let(::convertExpr),
        rPar = convertKeyword(v.rightParenthesis ?: error("No right parenthesis for $v"), Node.Keyword::RPar),
        entries = v.entries.map(::convertWhenEntry),
    ).map(v)

    open fun convertWhenEntry(v: KtWhenEntry) = Node.Expr.When.Entry(
        conds = v.conditions.map(::convertWhenCond),
        body = convertExpr(v.expression ?: error("No when entry body for $v"))
    ).map(v)

    open fun convertWhenCond(v: KtWhenCondition) = when (v) {
        is KtWhenConditionWithExpression -> Node.Expr.When.Cond.Expr(
            expr = convertExpr(v.expression ?: error("No when cond expr for $v"))
        ).map(v)
        is KtWhenConditionInRange -> Node.Expr.When.Cond.In(
            expr = convertExpr(v.rangeExpression ?: error("No when in expr for $v")),
            not = v.isNegated
        ).map(v)
        is KtWhenConditionIsPattern -> Node.Expr.When.Cond.Is(
            typeRef = convertTypeRef(v.typeReference ?: error("No when is type for $v")),
            not = v.isNegated
        ).map(v)
        else -> error("Unrecognized when cond of $v")
    }

    open fun convertObject(v: KtObjectLiteralExpression) = Node.Expr.Object(
        decl = convertStructured(v.objectDeclaration),
    ).map(v)

    open fun convertThrow(v: KtThrowExpression) = Node.Expr.Throw(
        expr = convertExpr(v.thrownExpression ?: error("No throw expr for $v"))
    ).map(v)

    open fun convertReturn(v: KtReturnExpression) = Node.Expr.Return(
        returnKeyword = convertKeyword(v.returnKeyword, Node.Keyword::Return),
        label = v.getLabelName(),
        expr = v.returnedExpression?.let(::convertExpr)
    ).map(v)

    open fun convertContinue(v: KtContinueExpression) = Node.Expr.Continue(
        label = v.getLabelName()
    ).map(v)

    open fun convertBreak(v: KtBreakExpression) = Node.Expr.Break(
        label = v.getLabelName()
    ).map(v)

    open fun convertCollLit(v: KtCollectionLiteralExpression) = Node.Expr.CollLit(
        exprs = v.getInnerExpressions().map(::convertExpr)
    ).map(v)

    open fun convertName(v: KtSimpleNameExpression) =
        convertName(v.getIdentifier() ?: error("Name identifier not found for $v"))
            .map(v)

    open fun convertName(v: PsiElement) = Node.Expr.Name(
        name = v.text
    ).map(v)

    open fun convertLabeled(v: KtLabeledExpression) = Node.Expr.Labeled(
        label = v.getLabelName() ?: error("No label name for $v"),
        expr = convertExpr(v.baseExpression ?: error("No label expr for $v"))
    ).map(v)

    open fun convertAnnotated(v: KtAnnotatedExpression) = Node.Expr.Annotated(
        anns = convertAnnotationSets(v),
        expr = convertExpr(v.baseExpression ?: error("No annotated expr for $v"))
    ).map(v)

    open fun convertCall(v: KtCallExpression) = Node.Expr.Call(
        expr = convertExpr(v.calleeExpression ?: error("No call expr for $v")),
        typeArgs = v.typeArguments.map(::convertTypeProjection),
        args = v.valueArgumentList?.let(::convertValueArgs),
        lambda = v.lambdaArguments.firstOrNull()?.let(::convertCallTrailLambda)
    ).map(v)

    open fun convertCallTrailLambda(v: KtLambdaArgument): Node.Expr.Call.TrailLambda {
        var label: String? = null
        var anns: List<Node.Modifier.AnnotationSet> = emptyList()
        fun KtExpression.extractLambda(allowParens: Boolean = false): KtLambdaExpression? = when (this) {
            is KtLambdaExpression -> this
            is KtLabeledExpression -> baseExpression?.extractLambda(allowParens).also {
                label = getLabelName()
            }
            is KtAnnotatedExpression -> baseExpression?.extractLambda(allowParens).also {
                anns = convertAnnotationSets(this)
            }
            is KtParenthesizedExpression -> if (allowParens) expression?.extractLambda(allowParens) else null
            else -> null
        }

        val expr = v.getArgumentExpression()?.extractLambda() ?: error("No lambda for $v")
        return Node.Expr.Call.TrailLambda(
            anns = anns,
            label = label,
            func = convertLambda(expr)
        ).map(v)
    }

    open fun convertArrayAccess(v: KtArrayAccessExpression) = Node.Expr.ArrayAccess(
        expr = convertExpr(v.arrayExpression ?: error("No array expr for $v")),
        indices = v.indexExpressions.map(::convertExpr)
    ).map(v)

    open fun convertAnonFunc(v: KtNamedFunction) = Node.Expr.AnonFunc(convertFunc(v))

    open fun convertPropertyExpr(v: KtProperty) = Node.Expr.Property(
        decl = convertProperty(v)
    ).map(v)

    open fun convertPropertyExpr(v: KtDestructuringDeclaration) = Node.Expr.Property(
        decl = convertProperty(v)
    ).map(v)

    open fun convertAnnotation(v: KtAnnotationEntry) = Node.Modifier.AnnotationSet.Annotation(
        nameType = convertType(v.typeReference?.typeElement ?: error("Missing annotation name")),
        typeArgs = v.typeArguments.map { convertTypeProjection(it) ?: error("No ann typ arg for $v") },
        args = v.valueArgumentList?.let(::convertValueArgs)
    ).map(v)

    open fun convertBlock(v: KtBlockExpression) = Node.Expr.Block(
        stmts = v.statements.map(::convertStmtNo)
    ).map(v)

    open fun convertStmtNo(v: KtExpression) =
        if (v is KtDeclaration) Node.Stmt.Decl(convertDecl(v)).map(v) else Node.Stmt.Expr(convertExpr(v)).map(v)

    open fun convertAnnotationSets(v: KtElement): List<Node.Modifier.AnnotationSet> = v.children.flatMap { elem ->
        // We go over the node children because we want to preserve order
        when (elem) {
            is KtAnnotationEntry ->
                listOf(convertAnnotationSet(elem))
            is KtAnnotation ->
                listOf(convertAnnotationSet(elem))
            is KtFileAnnotationList ->
                convertAnnotationSets(elem)
            else ->
                emptyList()
        }
    }

    open fun convertAnnotationSet(v: KtAnnotation) = Node.Modifier.AnnotationSet(
        atSymbol = v.node.findChildByType(KtTokens.AT)?.let { convertKeyword(it.psi, Node.Keyword::At) },
        target = v.useSiteTarget?.let(::convertAnnotationSetTarget),
        lBracket = v.node.findChildByType(KtTokens.LBRACKET)?.let { convertKeyword(it.psi, Node.Keyword::LBracket) },
        anns = v.entries.map(::convertAnnotation),
        rBracket = v.node.findChildByType(KtTokens.RBRACKET)?.let { convertKeyword(it.psi, Node.Keyword::RBracket) },
    ).map(v)

    open fun convertAnnotationSet(v: KtAnnotationEntry) = Node.Modifier.AnnotationSet(
        atSymbol = v.atSymbol?.let { convertKeyword(it, Node.Keyword::At) },
        target = v.useSiteTarget?.let(::convertAnnotationSetTarget),
        lBracket = null,
        anns = listOf(convertAnnotation(v)),
        rBracket = null,
    ).map(v)

    open fun convertAnnotationSetTarget(v: KtAnnotationUseSiteTarget) = when (v.getAnnotationUseSiteTarget()) {
        AnnotationUseSiteTarget.FIELD -> Node.Modifier.AnnotationSet.Target.FIELD
        AnnotationUseSiteTarget.FILE -> Node.Modifier.AnnotationSet.Target.FILE
        AnnotationUseSiteTarget.PROPERTY -> Node.Modifier.AnnotationSet.Target.PROPERTY
        AnnotationUseSiteTarget.PROPERTY_GETTER -> Node.Modifier.AnnotationSet.Target.GET
        AnnotationUseSiteTarget.PROPERTY_SETTER -> Node.Modifier.AnnotationSet.Target.SET
        AnnotationUseSiteTarget.RECEIVER -> Node.Modifier.AnnotationSet.Target.RECEIVER
        AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> Node.Modifier.AnnotationSet.Target.PARAM
        AnnotationUseSiteTarget.SETTER_PARAMETER -> Node.Modifier.AnnotationSet.Target.SETPARAM
        AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> Node.Modifier.AnnotationSet.Target.DELEGATE
    }

    open fun convertModifiers(v: KtModifierListOwner) = convertModifiers(v.modifierList)

    open fun convertModifiers(v: KtModifierList?): List<Node.Modifier> =
        v?.node?.children().orEmpty().mapNotNull { node ->
            // We go over the node children because we want to preserve order
            node.psi.let { psi ->
                when (psi) {
                    is KtAnnotationEntry -> convertAnnotationSet(psi)
                    is KtAnnotation -> convertAnnotationSet(psi)
                    is PsiWhiteSpace -> null
                    else -> (
                            modifiersByText[node.text]?.let { keyword ->
                                Node.Modifier.Lit(keyword)
                            } ?: error("Unrecognized modifier: ${node.text}")
                            ).map(psi)
                }
            }
        }.toList()

    open fun convertPostModifiers(v: KtElement): List<Node.PostModifier> {
        val nonExtraChildren = v.allChildren.filterNot { it is PsiComment || it is PsiWhiteSpace }.toList()

        if (nonExtraChildren.isEmpty()) {
            return listOf()
        }

        var prevPsi = nonExtraChildren[0]
        return nonExtraChildren.drop(1).mapNotNull { psi ->
            when (psi) {
                is KtTypeConstraintList -> Node.PostModifier.TypeConstraints(
                    whereKeyword = convertKeyword(prevPsi, Node.Keyword::Where),
                    constraints = psi.constraints.map(::convertTypeConstraint),
                ).mapNotCorrespondsPsiElement(v)
                is KtContractEffectList -> Node.PostModifier.Contract(
                    contractKeyword = convertKeyword(prevPsi, Node.Keyword::Contract),
                    contractEffects = convertContractEffects(psi),
                ).mapNotCorrespondsPsiElement(v)
                else -> null
            }.also { prevPsi = psi }
        }
    }

    open fun convertValOrVarKeyword(v: PsiElement) = Node.Keyword.ValOrVar.of(v.text)
        .map(v)

    open fun convertDeclarationKeyword(v: PsiElement) = Node.Keyword.Declaration.of(v.text)
        .map(v)

    open fun <T : Node.Keyword> convertKeyword(v: PsiElement, factory: () -> T): T =
        factory().also {
            check(v.text == it.value) { "Unexpected keyword: ${v.text}" }
        }.map(v)

    open fun findChildAndConvertLPar(v: KtElement) =
        findChildByType(v, KtTokens.LPAR)?.let { convertKeyword(it, Node.Keyword::LPar) }

    open fun findChildAndConvertRPar(v: KtElement) =
        findChildByType(v, KtTokens.RPAR)?.let { convertKeyword(it, Node.Keyword::RPar) }

    protected open fun <T : Node> T.map(v: PsiElement) = also { onNode(it, v) }
    protected open fun <T : Node> T.mapNotCorrespondsPsiElement(v: PsiElement) = also { onNode(it, null) }

    class Unsupported(message: String) : UnsupportedOperationException(message)

    companion object : Converter() {
        internal val modifiersByText = Node.Modifier.Keyword.values().associateBy { it.name.lowercase() }
        internal val binaryTokensByText = Node.Expr.BinaryOp.Token.values().associateBy { it.str }
        internal val unaryTokensByText = Node.Expr.UnaryOp.Token.values().associateBy { it.str }
        internal val typeTokensByText = Node.Expr.TypeOp.Token.values().associateBy { it.str }

        internal val KtTypeReference.contextReceiverList get() = getStubOrPsiChild(KtStubElementTypes.CONTEXT_RECEIVER_LIST)
        internal val KtDoubleColonExpression.questionMarks
            get() =
                generateSequence(
                    node.firstChildNode,
                    ASTNode::getTreeNext
                ).takeWhile { it.elementType != KtTokens.COLONCOLON }.count { it.elementType == KtTokens.QUEST }

        internal fun findChildByType(element: KtElement, type: IElementType): PsiElement? =
            element.node.findChildByType(type)?.psi

        internal fun findTrailingSeparator(v: KtElement, elementType: IElementType): PsiElement? =
            // Note that v.children.lastOrNull() is not equal to v.lastChild. children contain only KtElements, but lastChild is a last element of allChildren.
            v.children.lastOrNull()
                ?.siblings(forward = true, withItself = false)
                ?.find { it.node.elementType == elementType }
    }
}