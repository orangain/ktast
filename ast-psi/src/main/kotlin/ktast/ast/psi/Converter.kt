package ktast.ast.psi

import ktast.ast.Node
import org.jetbrains.kotlin.KtNodeTypes
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
        imports = v.importList?.let(::convertImports),
        decls = v.declarations.map(::convertDecl)
    ).map(v)

    open fun convertPackage(v: KtPackageDirective) = Node.Package(
        packageKeyword = convertKeyword(v.packageKeyword ?: error("No package keyword $v"), Node.Keyword::Package),
        mods = v.modifierList?.let(::convertModifiers),
        names = v.packageNames.map(::convertName),
    ).map(v)

    open fun convertImports(v: KtImportList): Node.NodeList<Node.Import> = Node.NodeList(
        children = v.imports.map(::convertImport),
    ).map(v)

    open fun convertImport(v: KtImportDirective) = Node.Import(
        importKeyword = convertKeyword(v.importKeyword, Node.Keyword::Import),
        names = convertImportNames(v.importedReference ?: error("No imported reference for $v"))
                + listOfNotNull(v.asterisk?.let(::convertName)),
        alias = v.alias?.let(::convertImportAlias)
    ).map(v)

    open fun convertImportNames(v: KtExpression): List<Node.Expr.Name> = when (v) {
        // Flatten nest of KtDotQualifiedExpression into list.
        is KtDotQualifiedExpression ->
            convertImportNames(v.receiverExpression) + listOf(
                convertName(v.selectorExpression as? KtNameReferenceExpression ?: error("No name reference for $v"))
            )
        is KtReferenceExpression -> listOf(convertName(v))
        else -> error("Unexpected type $v")
    }

    open fun convertImportAlias(v: KtImportAlias) = Node.Import.Alias(
        name = convertName(v.nameIdentifier ?: error("No name identifier for $v")),
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
        is KtSecondaryConstructor -> convertSecondaryConstructor(v)
        else -> error("Unrecognized declaration type for $v")
    }

    open fun convertStructured(v: KtClassOrObject) = Node.Decl.Structured(
        mods = v.modifierList?.let(::convertModifiers),
        declarationKeyword = v.getDeclarationKeyword()?.let(::convertDeclarationKeyword)
            ?: error("declarationKeyword not found"),
        name = v.nameIdentifier?.let(::convertName),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        primaryConstructor = v.primaryConstructor?.let(::convertPrimaryConstructor),
        parents = v.getSuperTypeList()?.let(::convertParents),
        typeConstraints = v.typeConstraintList?.let { typeConstraintList ->
            Node.PostModifier.TypeConstraints(
                whereKeyword = convertKeyword(v.whereKeyword, Node.Keyword::Where),
                constraints = convertTypeConstraints(typeConstraintList),
            ).mapNotCorrespondsPsiElement(v)
        },
        body = v.body?.let { convertDecls(it) },
    ).map(v)

    open fun convertParents(v: KtSuperTypeList) = Node.Decl.Structured.Parents(
        items = v.entries.map(::convertParent),
    ).map(v)

    open fun convertParent(v: KtSuperTypeListEntry) = when (v) {
        is KtSuperTypeCallEntry -> Node.Decl.Structured.Parent.CallConstructor(
            type = v.typeReference?.typeElement?.let(::convertType) as? Node.Type.Simple
                ?: error("Bad type on super call $v"),
            typeArgs = v.typeArgumentList?.let(::convertTypeProjections),
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
        mods = v.modifierList?.let(::convertModifiers),
        constructorKeyword = v.getConstructorKeyword()?.let { convertKeyword(it, Node.Keyword::Constructor) },
        params = v.valueParameterList?.let(::convertFuncParams)
    ).map(v)

    open fun convertInit(v: KtAnonymousInitializer) = Node.Decl.Init(
        mods = v.modifierList?.let(::convertModifiers),
        block = convertBlock(v.body as? KtBlockExpression ?: error("No init block for $v")),
    ).map(v)

    open fun convertFunc(v: KtNamedFunction): Node.Decl.Func {
        val hasTypeParameterListBeforeFunctionName = v.allChildren.find {
            it is KtTypeParameterList || it is KtTypeReference || it.node.elementType == KtTokens.IDENTIFIER
        } is KtTypeParameterList

        return Node.Decl.Func(
            mods = v.modifierList?.let(::convertModifiers),
            funKeyword = v.funKeyword?.let { convertKeyword(it, Node.Keyword::Fun) } ?: error("No fun keyword for $v"),
            typeParams =
            if (hasTypeParameterListBeforeFunctionName) v.typeParameterList?.let(::convertTypeParams) else null,
            receiverTypeRef = v.receiverTypeReference?.let(::convertTypeRef),
            name = v.nameIdentifier?.let(::convertName),
            postTypeParams =
            if (!hasTypeParameterListBeforeFunctionName) v.typeParameterList?.let(::convertTypeParams) else null,
            params = v.valueParameterList?.let(::convertFuncParams),
            typeRef = v.typeReference?.let(::convertTypeRef),
            postMods = convertPostModifiers(v),
            body = convertFuncBody(v),
        ).map(v)
    }

    open fun convertFuncParams(v: KtParameterList) = Node.Decl.Func.Params(
        params = v.parameters.map(::convertFuncParam),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertFuncParam(v: KtParameter) = Node.Decl.Func.Params.Param(
        mods = v.modifierList?.let(::convertModifiers),
        valOrVar = v.valOrVarKeyword?.let(::convertValOrVarKeyword),
        name = v.nameIdentifier?.let(::convertName) ?: error("No param name"),
        typeRef = v.typeReference?.let(::convertTypeRef),
        initializer = v.defaultValue?.let {
            convertInitializer(v.equalsToken ?: error("No equals token for initializer of $v"), it, v)
        },
    ).map(v)

    open fun convertFuncBody(v: KtDeclarationWithBody): Node.Decl.Func.Body? =
        when (val bodyExpression = v.bodyExpression) {
            null -> null
            is KtBlockExpression ->
                Node.Decl.Func.Body.Block(
                    block = convertBlock(bodyExpression),
                ).mapNotCorrespondsPsiElement(v)
            else ->
                Node.Decl.Func.Body.Expr(
                    equals = convertKeyword(v.equalsToken ?: error("No equals token before $v"), Node.Keyword::Equal),
                    expr = convertExpr(bodyExpression),
                ).mapNotCorrespondsPsiElement(v)
        }

    open fun convertProperty(v: KtProperty) = Node.Decl.Property(
        mods = v.modifierList?.let(::convertModifiers),
        valOrVar = convertValOrVarKeyword(v.valOrVarKeyword),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        receiverTypeRef = v.receiverTypeReference?.let(::convertTypeRef),
        variable = Node.Decl.Property.Variable.Single(
            name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
            typeRef = v.typeReference?.let(::convertTypeRef)
        ).mapNotCorrespondsPsiElement(v),
        typeConstraints = v.typeConstraintList?.let { typeConstraintList ->
            Node.PostModifier.TypeConstraints(
                whereKeyword = convertKeyword(v.whereKeyword, Node.Keyword::Where),
                constraints = convertTypeConstraints(typeConstraintList),
            ).mapNotCorrespondsPsiElement(v)
        },
        initializer = v.initializer?.let {
            convertInitializer(v.equalsToken ?: error("No equals token for initializer of $v"), it, v)
        },
        delegate = v.delegate?.let(::convertPropertyDelegate),
        accessors = v.accessors.map(::convertPropertyAccessor),
    ).map(v)

    open fun convertProperty(v: KtDestructuringDeclaration) = Node.Decl.Property(
        mods = v.modifierList?.let(::convertModifiers),
        valOrVar = v.valOrVarKeyword?.let(::convertValOrVarKeyword) ?: error("Missing valOrVarKeyword"),
        typeParams = null,
        receiverTypeRef = null,
        variable = Node.Decl.Property.Variable.Multi(
            vars = v.entries.map(::convertPropertyVariable),
            trailingComma = v.trailingComma?.let(::convertComma),
        ),
        typeConstraints = null,
        initializer = v.initializer?.let { convertInitializer(v.equalsToken, it, v) },
        delegate = null,
        accessors = listOf(),
    ).map(v)

    open fun convertPropertyVariable(v: KtDestructuringDeclarationEntry) = Node.Decl.Property.Variable.Single(
        name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
        typeRef = v.typeReference?.let(::convertTypeRef)
    ).map(v)

    open fun convertPropertyDelegate(v: KtPropertyDelegate) = Node.Decl.Property.Delegate(
        byKeyword = convertKeyword(v.byKeyword, Node.Keyword::By),
        expr = convertExpr(v.expression ?: error("Missing expression for $v")),
    ).map(v)

    open fun convertPropertyAccessor(v: KtPropertyAccessor) =
        if (v.isGetter) Node.Decl.Property.Accessor.Get(
            mods = v.modifierList?.let(::convertModifiers),
            getKeyword = convertKeyword(v.getKeyword, Node.Keyword::Get),
            typeRef = v.returnTypeReference?.let(::convertTypeRef),
            postMods = convertPostModifiers(v),
            body = convertFuncBody(v),
        ).map(v) else Node.Decl.Property.Accessor.Set(
            mods = v.modifierList?.let(::convertModifiers),
            setKeyword = convertKeyword(v.setKeyword, Node.Keyword::Set),
            params = v.parameterList?.let { convertPropertyAccessorParams(it) },
            postMods = convertPostModifiers(v),
            body = convertFuncBody(v),
        ).map(v)

    open fun convertPropertyAccessorParams(v: KtParameterList) = Node.Decl.Property.Accessor.Params(
        params = v.parameters.map(::convertFuncParam),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertTypeAlias(v: KtTypeAlias) = Node.Decl.TypeAlias(
        mods = v.modifierList?.let(::convertModifiers),
        name = v.nameIdentifier?.let(::convertName) ?: error("No type alias name for $v"),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        typeRef = convertTypeRef(v.getTypeReference() ?: error("No type alias ref for $v"))
    ).map(v)

    open fun convertSecondaryConstructor(v: KtSecondaryConstructor) = Node.Decl.SecondaryConstructor(
        mods = v.modifierList?.let(::convertModifiers),
        constructorKeyword = convertKeyword(v.getConstructorKeyword(), Node.Keyword::Constructor),
        params = v.valueParameterList?.let(::convertFuncParams),
        delegationCall = if (v.hasImplicitDelegationCall()) null else v.getDelegationCall().let {
            Node.Decl.SecondaryConstructor.DelegationCall(
                target =
                if (it.isCallToThis) Node.Decl.SecondaryConstructor.DelegationTarget.THIS
                else Node.Decl.SecondaryConstructor.DelegationTarget.SUPER,
                args = it.valueArgumentList?.let(::convertValueArgs)
            ).map(it)
        },
        block = v.bodyExpression?.let(::convertBlock)
    ).map(v)

    open fun convertEnumEntry(v: KtEnumEntry) = Node.Decl.EnumEntry(
        mods = v.modifierList?.let(::convertModifiers),
        name = v.nameIdentifier?.let(::convertName) ?: error("Unnamed enum"),
        args = v.initializerList?.let(::convertValueArgs),
        body = v.body?.let(::convertClassBody),
        hasComma = v.comma != null,
    ).map(v)

    open fun convertClassBody(v: KtClassBody) = Node.Decl.Structured.Body(
        decls = v.declarations.map(::convertDecl),
    ).map(v)

    open fun convertInitializer(equalsToken: PsiElement, expr: KtExpression, parent: PsiElement) = Node.Initializer(
        equals = convertKeyword(equalsToken, Node.Keyword::Equal),
        expr = convertExpr(expr),
    ).mapNotCorrespondsPsiElement(parent)

    open fun convertTypeParams(v: KtTypeParameterList) = Node.TypeParams(
        params = v.parameters.map(::convertTypeParam),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertTypeParam(v: KtTypeParameter) = Node.TypeParams.TypeParam(
        mods = v.modifierList?.let(::convertModifiers),
        name = v.nameIdentifier?.let(::convertName) ?: error("No type param name for $v"),
        typeRef = v.extendsBound?.let(::convertTypeRef)
    ).map(v)

    open fun convertTypeProjections(v: KtTypeArgumentList): Node.NodeList<Node.TypeProjection> = Node.NodeList(
        children = v.arguments.map(::convertTypeProjection),
        separator = ",",
        prefix = "<",
        suffix = ">",
        trailingSeparator = v.trailingComma?.let(::convertComma),
    )

    open fun convertTypeProjection(v: KtTypeProjection): Node.TypeProjection =
        if (v.projectionKind == KtProjectionKind.STAR) {
            Node.TypeProjection.Asterisk(
                asterisk = convertKeyword(
                    v.projectionToken ?: error("No projection token for $v"),
                    Node.Keyword::Asterisk
                ),
            ).map(v)
        } else {
            Node.TypeProjection.Type(
                mods = v.modifierList?.let(::convertModifiers),
                typeRef = convertTypeRef(v.typeReference ?: error("No type reference for $v")),
            ).map(v)
        }

    open fun convertTypeRef(v: KtTypeReference): Node.TypeRef {
        var lPar: PsiElement? = null
        var rPar: PsiElement? = null
        var allChildren = v.allChildren.toList()
        if (v.firstChild.node.elementType == KtTokens.LPAR && v.lastChild.node.elementType == KtTokens.RPAR) {
            lPar = v.firstChild
            rPar = v.lastChild
            allChildren = allChildren.subList(1, allChildren.size - 1)
        }
        var innerLPar: PsiElement? = null
        var innerRPar: PsiElement? = null
        var mods: KtModifierList? = null
        var innerMods: KtModifierList? = null
        allChildren.forEach {
            when (it) {
                is KtModifierList -> {
                    if (innerLPar == null) {
                        mods = it
                    } else {
                        innerMods = it
                    }
                }
                else -> {
                    when (it.node.elementType) {
                        KtTokens.LPAR -> innerLPar = it
                        KtTokens.RPAR -> innerRPar = it
                        else -> {}
                    }
                }
            }
        }

        return Node.TypeRef(
            lPar = lPar?.let { convertKeyword(it, Node.Keyword::LPar) },
            contextReceivers = v.contextReceiverList?.let { convertContextReceivers(it) },
            mods = mods?.let { convertModifiers(it) },
            innerLPar = innerLPar?.let { convertKeyword(it, Node.Keyword::LPar) },
            innerMods = innerMods?.let { convertModifiers(it) },
            type = v.typeElement?.let { convertType(it) }, // v.typeElement is null when the type reference has only context receivers.
            innerRPar = innerRPar?.let { convertKeyword(it, Node.Keyword::RPar) },
            rPar = rPar?.let { convertKeyword(it, Node.Keyword::RPar) },
        ).map(v)
    }

    open fun convertTypeConstraints(v: KtTypeConstraintList): Node.NodeList<Node.PostModifier.TypeConstraints.TypeConstraint> =
        Node.NodeList(
            children = v.constraints.map(::convertTypeConstraint),
            separator = ",",
        ).map(v)

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
            receiver = v.receiver?.let(::convertTypeFuncReceiver),
            params = v.parameterList?.let(::convertTypeFuncParams),
            typeRef = convertTypeRef(v.returnTypeReference ?: error("No return type"))
        ).map(v)
        is KtUserType -> Node.Type.Simple(
            pieces = generateSequence(v) { it.qualifier }.toList().reversed().map { type ->
                Node.Type.Simple.Piece(
                    name = type.referenceExpression?.let(::convertName) ?: error("No type name for $type"),
                    typeParams = type.typeArgumentList?.let(::convertTypeProjections),
                ).mapNotCorrespondsPsiElement(type)
            }
        ).map(v)
        is KtNullableType -> Node.Type.Nullable(
            lPar = v.leftParenthesis?.let { convertKeyword(it, Node.Keyword::LPar) },
            mods = v.modifierList?.let(::convertModifiers),
            type = convertType(v.innerType ?: error("No inner type for nullable")),
            rPar = v.rightParenthesis?.let { convertKeyword(it, Node.Keyword::RPar) },
        ).map(v)
        is KtDynamicType -> Node.Type.Dynamic().map(v)
        else -> error("Unrecognized type of $v")
    }

    open fun convertTypeFuncReceiver(v: KtFunctionTypeReceiver) = Node.Type.Func.Receiver(
        typeRef = convertTypeRef(v.typeReference),
    ).map(v)

    open fun convertTypeFuncParams(v: KtParameterList): Node.NodeList<Node.Type.Func.Param> = Node.NodeList(
        children = v.parameters.map(::convertTypeFuncParam),
        separator = ",",
        prefix = "(",
        suffix = ")",
        trailingSeparator = v.trailingComma?.let(::convertComma)
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
            trailingSeparator = findTrailingSeparator(v, KtTokens.COMMA)?.let(::convertComma),
        ).map(v)

    open fun convertContractEffect(v: KtContractEffect) = Node.PostModifier.Contract.ContractEffect(
        expr = convertExpr(v.getExpression()),
    ).map(v)

    open fun convertConstructorCallee(v: KtConstructorCalleeExpression) = Node.ConstructorCallee(
        type = convertType(
            v.typeReference?.typeElement ?: error("No type reference or type element for $v")
        ) as? Node.Type.Simple ?: error(""),
    ).map(v)

    open fun convertValueArgs(v: KtValueArgumentList) = Node.ValueArgs(
        args = v.arguments.map(::convertValueArg),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertValueArgs(v: KtInitializerList): Node.ValueArgs {
        val valueArgumentList = (v.initializers.firstOrNull() as? KtSuperTypeCallEntry)?.valueArgumentList
        return Node.ValueArgs(
            args = (valueArgumentList?.arguments
                ?: error("No value arguments for $v")).map(::convertValueArg),
            trailingComma = valueArgumentList.trailingComma?.let(::convertComma),
        ).map(v)
    }

    open fun convertValueArg(v: KtValueArgument) = Node.ValueArgs.ValueArg(
        name = v.getArgumentName()?.let(::convertValueArgName),
        asterisk = v.getSpreadElement() != null,
        expr = convertExpr(v.getArgumentExpression() ?: error("No expr for value arg"))
    ).map(v)

    open fun convertContainer(v: KtContainerNode) = Node.Container(
        expr = convertExpr(v.expression),
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
        is KtConstructorCalleeExpression -> error("Supposed to be unreachable here. KtConstructorCalleeExpression is expected to be inside of KtSuperTypeCallEntry or KtAnnotationEntry.")
        is KtArrayAccessExpression -> convertArrayAccess(v)
        is KtNamedFunction -> convertAnonFunc(v)
        is KtProperty -> convertPropertyExpr(v)
        is KtDestructuringDeclaration -> convertPropertyExpr(v)
        // TODO: this is present in a recovery test where an interface decl is on rhs of a gt expr
        is KtClass -> throw Unsupported("Class expressions not supported")
        else -> error("Unrecognized expression type from $v")
    }

    open fun convertIf(v: KtIfExpression) = Node.Expr.If(
        ifKeyword = convertKeyword(v.ifKeyword, Node.Keyword::If),
        expr = convertExpr(v.condition ?: error("No cond on if for $v")),
        body = convertContainer(v.thenContainer),
        elseBody = v.elseContainer?.let(::convertContainer),
    ).map(v)

    open fun convertTry(v: KtTryExpression) = Node.Expr.Try(
        block = convertBlock(v.tryBlock),
        catches = v.catchClauses.map(::convertTryCatch),
        finallyBlock = v.finallyBlock?.finalExpression?.let(::convertBlock)
    ).map(v)

    open fun convertTryCatch(v: KtCatchClause) = Node.Expr.Try.Catch(
        catchKeyword = convertKeyword(v.catchKeyword, Node.Keyword::Catch),
        params = convertFuncParams(v.parameterList ?: error("No catch params for $v")),
        block = convertBlock(v.catchBody as? KtBlockExpression ?: error("No catch block for $v")),
    ).map(v)

    open fun convertFor(v: KtForExpression) = Node.Expr.For(
        forKeyword = convertKeyword(v.forKeyword, Node.Keyword::For),
        anns = v.loopParameter?.annotations?.map(::convertAnnotationSet) ?: emptyList(),
        loopParam = convertLambdaParam(v.loopParameter ?: error("No param on for $v")),
        loopRange = convertContainer(v.loopRangeContainer),
        body = convertContainer(v.bodyContainer),
    ).map(v)

    open fun convertWhile(v: KtWhileExpressionBase) = Node.Expr.While(
        whileKeyword = convertKeyword(v.whileKeyword, Node.Keyword::While),
        condition = convertContainer(v.conditionContainer),
        body = convertContainer(v.bodyContainer),
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
        recv = v.receiverExpression?.let { expr ->
            convertDoubleColonRefRecv(
                expr,
                v.questionMarks.map { convertKeyword(it, Node.Keyword::Question) }
            )
        },
        name = convertName(v.callableReference)
    ).map(v)

    open fun convertDoubleColonRefClass(v: KtClassLiteralExpression) = Node.Expr.DoubleColonRef.Class(
        recv = v.receiverExpression?.let { expr ->
            convertDoubleColonRefRecv(
                expr,
                v.questionMarks.map { convertKeyword(it, Node.Keyword::Question) }
            )
        }
    ).map(v)

    open fun convertDoubleColonRefRecv(
        v: KtExpression,
        questionMarks: List<Node.Keyword.Question>
    ): Node.Expr.DoubleColonRef.Recv = when (v) {
        is KtSimpleNameExpression -> Node.Expr.DoubleColonRef.Recv.Type(
            type = Node.Type.Simple(
                listOf(
                    Node.Type.Simple.Piece(
                        convertName(v.getReferencedNameElement()),
                        Node.NodeList(emptyList(), ",")
                    ).map(v)
                )
            ).mapNotCorrespondsPsiElement(v),
            questionMarks = questionMarks,
        ).map(v)
        is KtCallExpression ->
            if (v.valueArgumentList == null && v.lambdaArguments.isEmpty())
                Node.Expr.DoubleColonRef.Recv.Type(
                    type = Node.Type.Simple(listOf(
                        Node.Type.Simple.Piece(
                            name = v.calleeExpression?.let { (it as? KtSimpleNameExpression)?.let(::convertName) }
                                ?: error("Missing text for call ref type of $v"),
                            typeParams = v.typeArgumentList?.let(::convertTypeProjections)
                        ).mapNotCorrespondsPsiElement(v)
                    )).mapNotCorrespondsPsiElement(v),
                    questionMarks = questionMarks,
                ).map(v)
            else Node.Expr.DoubleColonRef.Recv.Expr(convertExpr(v)).map(v)
        is KtDotQualifiedExpression -> {
            val lhs = convertDoubleColonRefRecv(v.receiverExpression, questionMarks)
            val rhs = v.selectorExpression?.let { convertDoubleColonRefRecv(it, questionMarks) }
            if (lhs is Node.Expr.DoubleColonRef.Recv.Type && rhs is Node.Expr.DoubleColonRef.Recv.Type)
                Node.Expr.DoubleColonRef.Recv.Type(
                    type = Node.Type.Simple(lhs.type.pieces + rhs.type.pieces).map(v),
                    questionMarks = listOf(),
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
        trailingSeparator = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertLambdaParam(v: KtParameter): Node.Expr.Lambda.Param {
        val destructuringDeclaration = v.destructuringDeclaration
        return if (destructuringDeclaration == null) {
            Node.Expr.Lambda.Param.Single(
                name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
                typeRef = v.typeReference?.let(::convertTypeRef),
            ).map(v)
        } else {
            Node.Expr.Lambda.Param.Multi(
                vars = convertLambdaParamVars(destructuringDeclaration),
                destructTypeRef = v.typeReference?.let(::convertTypeRef),
            ).map(v)
        }
    }

    open fun convertLambdaParamVars(v: KtDestructuringDeclaration): Node.NodeList<Node.Expr.Lambda.Param.Single> =
        Node.NodeList(
            children = v.entries.map(::convertLambdaParamVar),
            separator = ",",
            prefix = "(",
            suffix = ")",
        ).map(v)

    open fun convertLambdaParamVar(v: KtDestructuringDeclarationEntry) =
        Node.Expr.Lambda.Param.Single(
            name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
            typeRef = v.typeReference?.let(::convertTypeRef)
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

    open fun convertWhenEntry(v: KtWhenEntry): Node.Expr.When.Entry {
        val elseKeyword = v.elseKeyword
        return if (elseKeyword == null) {
            Node.Expr.When.Entry.Conds(
                conds = v.conditions.map(::convertWhenCond),
                trailingComma = v.trailingComma?.let(::convertComma),
                body = convertExpr(v.expression ?: error("No when entry body for $v"))
            ).map(v)
        } else {
            Node.Expr.When.Entry.Else(
                elseKeyword = convertKeyword(elseKeyword, Node.Keyword::Else),
                body = convertExpr(v.expression ?: error("No when entry body for $v")),
            ).map(v)
        }
    }

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
        exprs = v.getInnerExpressions().map(::convertExpr),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertValueArgName(v: KtValueArgumentName) = Node.Expr.Name(
        name = (v.referenceExpression.getIdentifier() ?: error("No identifier for $v")).text,
    ).map(v)

    open fun convertName(v: KtSimpleNameExpression) = Node.Expr.Name(
        name = (v.getIdentifier() ?: error("No identifier for $v")).text,
    ).map(v)

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
        typeArgs = v.typeArgumentList?.let(::convertTypeProjections),
        args = v.valueArgumentList?.let(::convertValueArgs),
        lambdaArgs = v.lambdaArguments.map(::convertCallLambdaArg)
    ).map(v)

    open fun convertCallLambdaArg(v: KtLambdaArgument): Node.Expr.Call.LambdaArg {
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
        return Node.Expr.Call.LambdaArg(
            anns = anns,
            label = label,
            func = convertLambda(expr)
        ).map(v)
    }

    open fun convertArrayAccess(v: KtArrayAccessExpression) = Node.Expr.ArrayAccess(
        expr = convertExpr(v.arrayExpression ?: error("No array expr for $v")),
        indices = v.indexExpressions.map(::convertExpr),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertAnonFunc(v: KtNamedFunction) = Node.Expr.AnonFunc(convertFunc(v))

    open fun convertPropertyExpr(v: KtProperty) = Node.Expr.Property(
        decl = convertProperty(v)
    ).map(v)

    open fun convertPropertyExpr(v: KtDestructuringDeclaration) = Node.Expr.Property(
        decl = convertProperty(v)
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

    open fun convertAnnotation(v: KtAnnotationEntry) = Node.Modifier.AnnotationSet.Annotation(
        constructorCallee = convertConstructorCallee(v.calleeExpression ?: error("No callee expression for $v")),
        args = v.valueArgumentList?.let(::convertValueArgs),
    ).map(v)

    open fun convertModifiers(v: KtModifierList): Node.NodeList<Node.Modifier> = Node.NodeList(
        children = v.node.children().mapNotNull { node ->
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
        }.toList(),
        separator = "",
        prefix = "",
        suffix = "",
    ).map(v)

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
                    constraints = convertTypeConstraints(psi),
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

    open fun convertComma(v: PsiElement): Node.Keyword.Comma = convertKeyword(v, Node.Keyword::Comma)

    open fun <T : Node.Keyword> convertKeyword(v: PsiElement, factory: () -> T): T =
        factory().also {
            check(v.text == it.value) { "Unexpected keyword: ${v.text}" }
        }.map(v)

    protected open fun <T : Node> T.map(v: PsiElement) = also { onNode(it, v) }
    protected open fun <T : Node> T.mapNotCorrespondsPsiElement(v: PsiElement) = also { onNode(it, null) }

    class Unsupported(message: String) : UnsupportedOperationException(message)

    companion object : Converter() {
        internal val modifiersByText = Node.Modifier.Keyword.values().associateBy { it.name.lowercase() }
        internal val binaryTokensByText = Node.Expr.BinaryOp.Token.values().associateBy { it.str }
        internal val unaryTokensByText = Node.Expr.UnaryOp.Token.values().associateBy { it.str }
        internal val typeTokensByText = Node.Expr.TypeOp.Token.values().associateBy { it.str }

        internal val KtImportDirective.importKeyword: PsiElement
            get() = findChildByType(this, KtTokens.IMPORT_KEYWORD) ?: error("Missing import keyword for $this")
        internal val KtImportDirective.asterisk: PsiElement?
            get() = findChildByType(this, KtTokens.MUL)

        internal val KtTypeParameterListOwner.whereKeyword: PsiElement
            get() = findChildByType(this, KtTokens.WHERE_KEYWORD) ?: error("No where keyword for $this")

        internal val KtDeclarationWithInitializer.equalsToken: PsiElement
            get() = findChildByType(this, KtTokens.EQ) ?: error("No equals token for initializer of $this")

        internal val KtPropertyDelegate.byKeyword: PsiElement
            get() = byKeywordNode.psi

        internal val KtPropertyAccessor.setKeyword: PsiElement
            get() = findChildByType(this, KtTokens.SET_KEYWORD) ?: error("No set keyword for $this")
        internal val KtPropertyAccessor.getKeyword: PsiElement
            get() = findChildByType(this, KtTokens.GET_KEYWORD) ?: error("No get keyword for $this")

        private val KtEnumEntry.comma: PsiElement?
            get() = findChildByType(this, KtTokens.COMMA)

        internal val KtTypeReference.contextReceiverList
            get() = getStubOrPsiChild(KtStubElementTypes.CONTEXT_RECEIVER_LIST)

        internal val KtNullableType.leftParenthesis: PsiElement?
            get() = findChildByType(this, KtTokens.LPAR)
        internal val KtNullableType.rightParenthesis: PsiElement?
            get() = findChildByType(this, KtTokens.RPAR)

        internal val KtIfExpression.thenContainer: KtContainerNode
            get() = findChildByType(this, KtNodeTypes.THEN) as? KtContainerNode ?: error("No then container for $this")
        internal val KtIfExpression.elseContainer: KtContainerNode?
            get() = findChildByType(this, KtNodeTypes.ELSE) as? KtContainerNode

        internal val KtCatchClause.catchKeyword: PsiElement
            get() = findChildByType(this, KtTokens.CATCH_KEYWORD) ?: error("No catch keyword for $this")

        internal val KtForExpression.loopRangeContainer: KtContainerNode
            get() = findChildByType(this, KtNodeTypes.LOOP_RANGE)
                    as? KtContainerNode ?: error("No in range for $this")

        private val KtWhileExpressionBase.whileKeyword: PsiElement
            get() = findChildByType(this, KtTokens.WHILE_KEYWORD) ?: error("No while keyword for $this")

        internal val KtLoopExpression.bodyContainer: KtContainerNodeForControlStructureBody
            get() = findChildByType(this, KtNodeTypes.BODY)
                    as? KtContainerNodeForControlStructureBody ?: error("No body for $this")

        private val KtWhileExpressionBase.conditionContainer: KtContainerNode
            get() = findChildByType(this, KtNodeTypes.CONDITION)
                    as? KtContainerNode ?: error("No condition for $this")

        internal val KtDoubleColonExpression.questionMarks
            get() = allChildren
                .takeWhile { it.node.elementType != KtTokens.COLONCOLON }
                .filter { it.node.elementType == KtTokens.QUEST }
                .toList()

        private fun findChildByType(v: KtElement, type: IElementType): PsiElement? =
            v.node.findChildByType(type)?.psi

        internal val KtContainerNode.expression: KtExpression
            get() = findChildByClass<KtExpression>(this) ?: error("No expression for $this")

        private inline fun <reified T> findChildByClass(v: PsiElement): T? =
            v.children.firstOrNull { it is T } as? T

        internal fun findTrailingSeparator(v: KtElement, elementType: IElementType): PsiElement? =
            // Note that v.children.lastOrNull() is not equal to v.lastChild. children contain only KtElements, but lastChild is a last element of allChildren.
            v.children.lastOrNull()
                ?.siblings(forward = true, withItself = false)
                ?.find { it.node.elementType == elementType }
    }
}