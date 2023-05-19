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
import org.jetbrains.kotlin.psi.psiUtil.siblings

open class Converter {
    protected open fun onNode(node: Node, elem: PsiElement?) {}

    open fun convertKotlinFile(v: KtFile) = Node.KotlinFile(
        annotationSets = convertAnnotationSets(v),
        packageDirective = v.packageDirective?.takeIf { it.packageNames.isNotEmpty() }?.let(::convertPackageDirective),
        importDirectives = v.importList?.let(::convertImportDirectives),
        declarations = v.declarations.map(::convertDeclaration)
    ).map(v)

    open fun convertPackageDirective(v: KtPackageDirective) = Node.PackageDirective(
        packageKeyword = convertKeyword(v.packageKeyword ?: error("No package keyword $v"), Node.Keyword::Package),
        modifiers = v.modifierList?.let(::convertModifiers),
        names = v.packageNames.map(::convertName),
    ).map(v)

    open fun convertImportDirectives(v: KtImportList): Node.ImportDirectives? = if (v.imports.isEmpty())
        null // Explicitly returns null here. This is because, unlike other PsiElements, KtImportList does exist even when there is no import statement.
    else
        Node.ImportDirectives(
            elements = v.imports.map(::convertImportDirective),
        ).map(v)

    open fun convertImportDirective(v: KtImportDirective) = Node.ImportDirective(
        importKeyword = convertKeyword(v.importKeyword, Node.Keyword::Import),
        names = convertImportNames(v.importedReference ?: error("No imported reference for $v"))
                + listOfNotNull(v.asterisk?.let(::convertName)),
        importAlias = v.alias?.let(::convertImportAlias)
    ).map(v)

    open fun convertImportNames(v: KtExpression): List<Node.NameExpression> = when (v) {
        // Flatten nest of KtDotQualifiedExpression into list.
        is KtDotQualifiedExpression ->
            convertImportNames(v.receiverExpression) + listOf(
                convertName(v.selectorExpression as? KtNameReferenceExpression ?: error("No name reference for $v"))
            )
        is KtReferenceExpression -> listOf(convertName(v))
        else -> error("Unexpected type $v")
    }

    open fun convertImportAlias(v: KtImportAlias) = Node.ImportDirective.ImportAlias(
        name = convertName(v.nameIdentifier ?: error("No name identifier for $v")),
    ).map(v)

    open fun convertDeclaration(v: KtDeclaration): Node.Declaration = when (v) {
        is KtEnumEntry -> error("KtEnumEntry is handled in convertEnumEntry")
        is KtClassOrObject -> convertClass(v)
        is KtAnonymousInitializer -> convertInit(v)
        is KtNamedFunction -> convertFunction(v)
        is KtDestructuringDeclaration -> convertProperty(v)
        is KtProperty -> convertProperty(v)
        is KtTypeAlias -> convertTypeAlias(v)
        is KtSecondaryConstructor -> convertSecondaryConstructor(v)
        else -> error("Unrecognized declaration type for $v")
    }

    open fun convertClass(v: KtClassOrObject) = Node.ClassDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        declarationKeyword = v.getDeclarationKeyword()?.let(::convertDeclarationKeyword)
            ?: error("declarationKeyword not found"),
        name = v.nameIdentifier?.let(::convertName),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        primaryConstructor = v.primaryConstructor?.let(::convertPrimaryConstructor),
        classParents = v.getSuperTypeList()?.let(::convertParents),
        typeConstraints = v.typeConstraintList?.let { typeConstraintList ->
            Node.TypeConstraints(
                whereKeyword = convertKeyword(v.whereKeyword, Node.Keyword::Where),
                constraints = convertTypeConstraints(typeConstraintList),
            ).mapNotCorrespondsPsiElement(v)
        },
        classBody = v.body?.let(::convertClassBody),
    ).map(v)

    open fun convertDeclarationKeyword(v: PsiElement) = Node.ClassDeclaration.DeclarationKeyword.of(v.text)
        .map(v)

    open fun convertParents(v: KtSuperTypeList) = Node.ClassDeclaration.ClassParents(
        elements = v.entries.map(::convertParent),
    ).map(v)

    open fun convertParent(v: KtSuperTypeListEntry) = when (v) {
        is KtSuperTypeCallEntry -> Node.ClassDeclaration.ClassParent.CallConstructor(
            type = v.typeReference?.typeElement?.let(::convertType) as? Node.SimpleType
                ?: error("Bad type on super call $v"),
            typeArgs = v.typeArgumentList?.let(::convertTypeArgs),
            args = v.valueArgumentList?.let(::convertValueArgs),
            // TODO
            lambda = null
        ).map(v)
        is KtDelegatedSuperTypeEntry -> Node.ClassDeclaration.ClassParent.DelegatedType(
            type = v.typeReference?.typeElement?.let(::convertType) as? Node.SimpleType
                ?: error("Bad type on super call $v"),
            byKeyword = convertKeyword(v.byKeywordNode.psi, Node.Keyword::By),
            expression = convertExpression(v.delegateExpression ?: error("Missing delegateExpression for $v")),
        ).map(v)
        is KtSuperTypeEntry -> Node.ClassDeclaration.ClassParent.Type(
            type = v.typeReference?.typeElement?.let(::convertType) as? Node.SimpleType
                ?: error("Bad type on super call $v"),
        ).map(v)
        else -> error("Unknown super type entry $v")
    }

    open fun convertPrimaryConstructor(v: KtPrimaryConstructor) = Node.ClassDeclaration.PrimaryConstructor(
        modifiers = v.modifierList?.let(::convertModifiers),
        constructorKeyword = v.getConstructorKeyword()?.let { convertKeyword(it, Node.Keyword::Constructor) },
        params = v.valueParameterList?.let(::convertFuncParams)
    ).map(v)

    open fun convertInit(v: KtAnonymousInitializer) = Node.InitDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        block = convertBlock(v.body as? KtBlockExpression ?: error("No init block for $v")),
    ).map(v)

    open fun convertFunction(v: KtNamedFunction): Node.FunctionDeclaration {
        if (v.typeParameterList != null) {
            val hasTypeParameterListBeforeFunctionName = v.allChildren.find {
                it is KtTypeParameterList || it is KtTypeReference || it.node.elementType == KtTokens.IDENTIFIER
            } is KtTypeParameterList
            if (!hasTypeParameterListBeforeFunctionName) {
                // According to the Kotlin syntax, type parameters are not allowed here. However, Kotlin compiler can parse them.
                throw Unsupported("Type parameters after function name is not allowed")
            }
        }

        return Node.FunctionDeclaration(
            modifiers = v.modifierList?.let(::convertModifiers),
            funKeyword = v.funKeyword?.let { convertKeyword(it, Node.Keyword::Fun) } ?: error("No fun keyword for $v"),
            typeParams = v.typeParameterList?.let(::convertTypeParams),
            receiverTypeRef = v.receiverTypeReference?.let(::convertTypeRef),
            name = v.nameIdentifier?.let(::convertName),
            params = v.valueParameterList?.let(::convertFuncParams),
            typeRef = v.typeReference?.let(::convertTypeRef),
            postModifiers = convertPostModifiers(v),
            equals = v.equalsToken?.let { convertKeyword(it, Node.Keyword::Equal) },
            body = v.bodyExpression?.let { convertExpression(it) },
        ).map(v)
    }

    open fun convertFuncParams(v: KtParameterList) = Node.FunctionParams(
        elements = v.parameters.map(::convertFuncParam),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertFuncParam(v: KtParameter) = Node.FunctionParam(
        modifiers = v.modifierList?.let(::convertModifiers),
        valOrVar = v.valOrVarKeyword?.let(::convertValOrVar),
        name = v.nameIdentifier?.let(::convertName) ?: error("No param name"),
        typeRef = v.typeReference?.let(::convertTypeRef),
        equals = v.equalsToken?.let { convertKeyword(it, Node.Keyword::Equal) },
        defaultValue = v.defaultValue?.let(::convertExpression),
    ).map(v)

    open fun convertProperty(v: KtProperty) = Node.PropertyDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        valOrVar = convertValOrVar(v.valOrVarKeyword),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        receiverTypeRef = v.receiverTypeReference?.let(::convertTypeRef),
        lPar = null,
        variables = listOf(
            Node.Variable(
                name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
                typeRef = v.typeReference?.let(::convertTypeRef)
            ).mapNotCorrespondsPsiElement(v)
        ),
        trailingComma = null,
        rPar = null,
        typeConstraints = v.typeConstraintList?.let { typeConstraintList ->
            Node.TypeConstraints(
                whereKeyword = convertKeyword(v.whereKeyword, Node.Keyword::Where),
                constraints = convertTypeConstraints(typeConstraintList),
            ).mapNotCorrespondsPsiElement(v)
        },
        equals = v.equalsToken?.let { convertKeyword(it, Node.Keyword::Equal) },
        initializer = v.initializer?.let(::convertExpression),
        propertyDelegate = v.delegate?.let(::convertPropertyDelegate),
        accessors = v.accessors.map(::convertPropertyAccessor),
    ).map(v)

    open fun convertProperty(v: KtDestructuringDeclaration) = Node.PropertyDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        valOrVar = v.valOrVarKeyword?.let(::convertValOrVar) ?: error("Missing valOrVarKeyword"),
        typeParams = null,
        receiverTypeRef = null,
        lPar = v.lPar?.let { convertKeyword(it, Node.Keyword::LPar) },
        variables = v.entries.map(::convertPropertyVariable),
        trailingComma = v.trailingComma?.let(::convertComma),
        rPar = v.rPar?.let { convertKeyword(it, Node.Keyword::RPar) },
        typeConstraints = null,
        equals = convertKeyword(v.equalsToken, Node.Keyword::Equal),
        initializer = v.initializer?.let(::convertExpression),
        propertyDelegate = null,
        accessors = listOf(),
    ).map(v)

    open fun convertValOrVar(v: PsiElement) = Node.PropertyDeclaration.ValOrVar.of(v.text)
        .map(v)

    open fun convertPropertyVariable(v: KtDestructuringDeclarationEntry) = Node.Variable(
        name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
        typeRef = v.typeReference?.let(::convertTypeRef)
    ).map(v)

    open fun convertPropertyDelegate(v: KtPropertyDelegate) = Node.PropertyDeclaration.PropertyDelegate(
        byKeyword = convertKeyword(v.byKeyword, Node.Keyword::By),
        expression = convertExpression(v.expression ?: error("Missing expression for $v")),
    ).map(v)

    open fun convertPropertyAccessor(v: KtPropertyAccessor) =
        if (v.isGetter) Node.PropertyDeclaration.Getter(
            modifiers = v.modifierList?.let(::convertModifiers),
            getKeyword = convertKeyword(v.getKeyword, Node.Keyword::Get),
            typeRef = v.returnTypeReference?.let(::convertTypeRef),
            postModifiers = convertPostModifiers(v),
            equals = v.equalsToken?.let { convertKeyword(it, Node.Keyword::Equal) },
            body = v.bodyExpression?.let(::convertExpression),
        ).map(v) else Node.PropertyDeclaration.Setter(
            modifiers = v.modifierList?.let(::convertModifiers),
            setKeyword = convertKeyword(v.setKeyword, Node.Keyword::Set),
            params = v.parameterList?.let(::convertLambdaParams),
            postModifiers = convertPostModifiers(v),
            equals = v.equalsToken?.let { convertKeyword(it, Node.Keyword::Equal) },
            body = v.bodyExpression?.let(::convertExpression),
        ).map(v)

    open fun convertTypeAlias(v: KtTypeAlias) = Node.TypeAliasDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        name = v.nameIdentifier?.let(::convertName) ?: error("No type alias name for $v"),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        typeRef = convertTypeRef(v.getTypeReference() ?: error("No type alias ref for $v"))
    ).map(v)

    open fun convertSecondaryConstructor(v: KtSecondaryConstructor) = Node.SecondaryConstructorDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        constructorKeyword = convertKeyword(v.getConstructorKeyword(), Node.Keyword::Constructor),
        params = v.valueParameterList?.let(::convertFuncParams),
        delegationCall = if (v.hasImplicitDelegationCall()) null else convertSecondaryConstructorDelegationCall(v.getDelegationCall()),
        block = v.bodyExpression?.let(::convertBlock)
    ).map(v)

    open fun convertSecondaryConstructorDelegationCall(v: KtConstructorDelegationCall) =
        Node.SecondaryConstructorDeclaration.DelegationCall(
            target = Node.SecondaryConstructorDeclaration.DelegationTarget(
                if (v.isCallToThis) {
                    Node.SecondaryConstructorDeclaration.DelegationTarget.Token.THIS
                } else {
                    Node.SecondaryConstructorDeclaration.DelegationTarget.Token.SUPER
                }
            ),
            args = v.valueArgumentList?.let(::convertValueArgs)
        ).map(v)

    open fun convertEnumEntry(v: KtEnumEntry): Node.ClassDeclaration.ClassBody.EnumEntry =
        Node.ClassDeclaration.ClassBody.EnumEntry(
            modifiers = v.modifierList?.let(::convertModifiers),
            name = v.nameIdentifier?.let(::convertName) ?: error("Unnamed enum"),
            args = v.initializerList?.let(::convertValueArgs),
            classBody = v.body?.let(::convertClassBody),
        ).map(v)

    open fun convertClassBody(v: KtClassBody): Node.ClassDeclaration.ClassBody {
        val ktEnumEntries = v.declarations.filterIsInstance<KtEnumEntry>()
        val declarationsExcludingKtEnumEntry = v.declarations.filter { it !is KtEnumEntry }
        return Node.ClassDeclaration.ClassBody(
            enumEntries = ktEnumEntries.map(::convertEnumEntry),
            hasTrailingCommaInEnumEntries = ktEnumEntries.lastOrNull()?.comma != null,
            declarations = declarationsExcludingKtEnumEntry.map(::convertDeclaration),
        ).map(v)
    }

    open fun convertTypeParams(v: KtTypeParameterList) = Node.TypeParams(
        elements = v.parameters.map(::convertTypeParam),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertTypeParam(v: KtTypeParameter) = Node.TypeParam(
        modifiers = v.modifierList?.let(::convertModifiers),
        name = v.nameIdentifier?.let(::convertName) ?: error("No type param name for $v"),
        typeRef = v.extendsBound?.let(::convertTypeRef)
    ).map(v)

    open fun convertTypeArgs(v: KtTypeArgumentList) = Node.TypeArgs(
        elements = v.arguments.map(::convertTypeArg),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertTypeArg(v: KtTypeProjection): Node.TypeArg = Node.TypeArg(
        modifiers = v.modifierList?.let(::convertModifiers),
        typeRef = v.typeReference?.let(::convertTypeRef),
        asterisk = v.projectionKind == KtProjectionKind.STAR,
    ).map(v)

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
        var modifierList: KtModifierList? = null
        var innerModifierList: KtModifierList? = null
        allChildren.forEach {
            when (it) {
                is KtModifierList -> {
                    if (innerLPar == null) {
                        modifierList = it
                    } else {
                        innerModifierList = it
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
            modifiers = modifierList?.let { convertModifiers(it) },
            type = convertType(
                v.typeElement ?: error("No type element for $v"),
                innerLPar,
                innerModifierList,
                innerRPar
            ),
            rPar = rPar?.let { convertKeyword(it, Node.Keyword::RPar) },
        ).map(v)
    }

    open fun convertTypeConstraints(v: KtTypeConstraintList) = Node.TypeConstraints.TypeConstraintList(
        elements = v.constraints.map(::convertTypeConstraint),
    ).map(v)

    open fun convertTypeConstraint(v: KtTypeConstraint) = Node.TypeConstraints.TypeConstraint(
        annotationSets = v.children.mapNotNull {
            when (it) {
                is KtAnnotationEntry -> convertAnnotationSet(it)
                is KtAnnotation -> convertAnnotationSet(it)
                else -> null
            }
        },
        name = v.subjectTypeParameterName?.let { convertName(it) } ?: error("No type constraint name for $v"),
        typeRef = convertTypeRef(v.boundTypeReference ?: error("No type constraint type for $v"))
    ).map(v)

    open fun convertType(
        v: KtTypeElement,
        lPar: PsiElement? = null,
        modifierList: KtModifierList? = null,
        rPar: PsiElement? = null,
    ): Node.Type = when (v) {
        is KtFunctionType -> Node.FunctionType(
            lPar = lPar?.let { convertKeyword(it, Node.Keyword::LPar) },
            modifiers = modifierList?.let(::convertModifiers),
            contextReceivers = v.contextReceiverList?.let { convertContextReceivers(it) },
            functionTypeReceiver = v.receiver?.let(::convertTypeFunctionReceiver),
            params = v.parameterList?.let(::convertTypeFunctionParams),
            returnTypeRef = convertTypeRef(v.returnTypeReference ?: error("No return type")),
            rPar = rPar?.let { convertKeyword(it, Node.Keyword::RPar) },
        ).map(v)
        is KtUserType -> Node.SimpleType(
            qualifiers = generateSequence(v.qualifier) { it.qualifier }.toList().reversed()
                .map(::convertTypeSimpleQualifier),
            name = convertName(v.referenceExpression ?: error("No type name for $v")),
            typeArgs = v.typeArgumentList?.let(::convertTypeArgs),
        ).map(v)
        is KtNullableType -> Node.NullableType(
            lPar = v.leftParenthesis?.let { convertKeyword(it, Node.Keyword::LPar) },
            modifiers = v.modifierList?.let(::convertModifiers),
            type = convertType(v.innerType ?: error("No inner type for nullable")),
            rPar = v.rightParenthesis?.let { convertKeyword(it, Node.Keyword::RPar) },
        ).map(v)
        is KtDynamicType -> Node.DynamicType().map(v)
        else -> error("Unrecognized type of $v")
    }

    open fun convertContextReceivers(v: KtContextReceiverList) = Node.FunctionType.ContextReceivers(
        elements = v.contextReceivers().map(::convertContextReceiver),
        trailingComma = null,
    ).map(v)

    open fun convertContextReceiver(v: KtContextReceiver) = Node.FunctionType.ContextReceiver(
        typeRef = convertTypeRef(v.typeReference() ?: error("Missing type reference for $v")),
    ).map(v)

    open fun convertContractEffects(v: KtContractEffectList) = Node.Contract.ContractEffects(
        elements = v.children.filterIsInstance<KtContractEffect>().map(::convertContractEffect),
        trailingComma = findTrailingSeparator(v, KtTokens.COMMA)?.let(::convertComma),
    ).map(v)

    open fun convertContractEffect(v: KtContractEffect) = Node.Contract.ContractEffect(
        expression = convertExpression(v.getExpression()),
    ).map(v)

    open fun convertTypeFunctionReceiver(v: KtFunctionTypeReceiver) = Node.FunctionType.FunctionTypeReceiver(
        typeRef = convertTypeRef(v.typeReference),
    ).map(v)

    open fun convertTypeFunctionParams(v: KtParameterList) = Node.FunctionType.Params(
        elements = v.parameters.map(::convertTypeFunctionParam),
        trailingComma = v.trailingComma?.let(::convertComma)
    ).map(v)

    open fun convertTypeFunctionParam(v: KtParameter) = Node.FunctionType.Param(
        name = v.nameIdentifier?.let(::convertName),
        typeRef = convertTypeRef(v.typeReference ?: error("No param type"))
    ).map(v)

    open fun convertTypeSimpleQualifier(v: KtUserType) = Node.SimpleType.Qualifier(
        name = convertName(v.referenceExpression ?: error("No type name for $v")),
        typeArgs = v.typeArgumentList?.let(::convertTypeArgs),
    ).map(v)

    open fun convertValueArgs(v: KtValueArgumentList) = Node.ValueArgs(
        elements = v.arguments.map(::convertValueArg),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertValueArgs(v: KtInitializerList): Node.ValueArgs {
        val valueArgumentList = (v.initializers.firstOrNull() as? KtSuperTypeCallEntry)?.valueArgumentList
            ?: error("No value arguments for $v")
        return Node.ValueArgs(
            elements = (valueArgumentList.arguments).map(::convertValueArg),
            trailingComma = valueArgumentList.trailingComma?.let(::convertComma),
        ).map(v)
    }

    open fun convertValueArg(v: KtValueArgument) = Node.ValueArg(
        name = v.getArgumentName()?.let(::convertValueArgName),
        asterisk = v.getSpreadElement() != null,
        expression = convertExpression(v.getArgumentExpression() ?: error("No expr for value arg"))
    ).map(v)

    open fun convertExpressionContainer(v: KtContainerNode) = Node.ExpressionContainer(
        expression = convertExpression(v.expression),
    ).map(v)

    open fun convertExpression(v: KtExpression): Node.Expression = when (v) {
        is KtIfExpression -> convertIf(v)
        is KtTryExpression -> convertTry(v)
        is KtForExpression -> convertFor(v)
        is KtWhileExpressionBase -> convertWhile(v)
        is KtBinaryExpression -> convertBinary(v)
        is KtQualifiedExpression -> convertBinary(v)
        is KtUnaryExpression -> convertUnary(v)
        is KtBinaryExpressionWithTypeRHS -> convertBinaryType(v)
        is KtIsExpression -> convertBinaryType(v)
        is KtCallableReferenceExpression -> convertCallableReference(v)
        is KtClassLiteralExpression -> convertClassLiteral(v)
        is KtParenthesizedExpression -> convertParenthesized(v)
        is KtStringTemplateExpression -> convertStringTemplate(v)
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
        is KtNamedFunction -> convertAnonymousFunction(v)
        is KtProperty -> convertPropertyExpr(v)
        is KtDestructuringDeclaration -> convertPropertyExpr(v)
        // TODO: this is present in a recovery test where an interface decl is on rhs of a gt expr
        is KtClass -> throw Unsupported("Class expressions not supported")
        else -> error("Unrecognized expression type from $v")
    }

    open fun convertIf(v: KtIfExpression) = Node.IfExpression(
        ifKeyword = convertKeyword(v.ifKeyword, Node.Keyword::If),
        condition = convertExpression(v.condition ?: error("No cond on if for $v")),
        body = convertExpressionContainer(v.thenContainer),
        elseBody = v.elseContainer?.let(::convertExpressionContainer),
    ).map(v)

    open fun convertTry(v: KtTryExpression) = Node.TryExpression(
        block = convertBlock(v.tryBlock),
        catches = v.catchClauses.map(::convertTryCatch),
        finallyBlock = v.finallyBlock?.finalExpression?.let(::convertBlock)
    ).map(v)

    open fun convertTryCatch(v: KtCatchClause) = Node.TryExpression.Catch(
        catchKeyword = convertKeyword(v.catchKeyword, Node.Keyword::Catch),
        params = convertFuncParams(v.parameterList ?: error("No catch params for $v")),
        block = convertBlock(v.catchBody as? KtBlockExpression ?: error("No catch block for $v")),
    ).map(v)

    open fun convertFor(v: KtForExpression) = Node.ForExpression(
        forKeyword = convertKeyword(v.forKeyword, Node.Keyword::For),
        loopParam = convertLambdaParam(v.loopParameter ?: error("No param on for $v")),
        loopRange = convertExpressionContainer(v.loopRangeContainer),
        body = convertExpressionContainer(v.bodyContainer),
    ).map(v)

    open fun convertWhile(v: KtWhileExpressionBase) = Node.WhileExpression(
        whileKeyword = convertKeyword(v.whileKeyword, Node.Keyword::While),
        condition = convertExpressionContainer(v.conditionContainer),
        body = convertExpressionContainer(v.bodyContainer),
        doWhile = v is KtDoWhileExpression
    ).map(v)

    open fun convertBinary(v: KtBinaryExpression): Node.BaseBinaryExpression =
        if (v.operationReference.isConventionOperator()) {
            Node.BinaryExpression(
                lhs = convertExpression(v.left ?: error("No binary lhs for $v")),
                operator = convertBinaryOperator(v.operationReference),
                rhs = convertExpression(v.right ?: error("No binary rhs for $v"))
            ).map(v)
        } else {
            Node.BinaryInfixExpression(
                lhs = convertExpression(v.left ?: error("No binary lhs for $v")),
                operator = convertName(v.operationReference.firstChild),
                rhs = convertExpression(v.right ?: error("No binary rhs for $v"))
            ).map(v)
        }

    open fun convertBinary(v: KtQualifiedExpression) = Node.BinaryExpression(
        lhs = convertExpression(v.receiverExpression),
        operator = Node.BinaryExpression.Operator(
            if (v is KtDotQualifiedExpression) {
                Node.BinaryExpression.Operator.Token.DOT
            } else {
                Node.BinaryExpression.Operator.Token.DOT_SAFE
            }
        ),
        rhs = convertExpression(v.selectorExpression ?: error("No qualified rhs for $v"))
    ).map(v)

    open fun convertBinaryOperator(v: PsiElement) = Node.BinaryExpression.Operator.of(v.text)
        .map(v)

    open fun convertUnary(v: KtUnaryExpression) = Node.UnaryExpression(
        expression = convertExpression(v.baseExpression ?: error("No unary expr for $v")),
        operator = convertUnaryOperator(v.operationReference),
        prefix = v is KtPrefixExpression
    ).map(v)

    open fun convertUnaryOperator(v: PsiElement) = Node.UnaryExpression.Operator.of(v.text)
        .map(v)

    open fun convertBinaryType(v: KtBinaryExpressionWithTypeRHS) = Node.BinaryTypeExpression(
        lhs = convertExpression(v.left),
        operator = convertBinaryTypeOperator(v.operationReference),
        rhs = convertTypeRef(v.right ?: error("No type op rhs for $v"))
    ).map(v)

    open fun convertBinaryType(v: KtIsExpression) = Node.BinaryTypeExpression(
        lhs = convertExpression(v.leftHandSide),
        operator = convertBinaryTypeOperator(v.operationReference),
        rhs = convertTypeRef(v.typeReference ?: error("No type op rhs for $v"))
    ).map(v)

    open fun convertBinaryTypeOperator(v: PsiElement) = Node.BinaryTypeExpression.Operator.of(v.text)
        .map(v)

    open fun convertCallableReference(v: KtCallableReferenceExpression) = Node.CallableReferenceExpression(
        lhs = v.receiverExpression?.let { expr ->
            convertDoubleColonReceiver(
                expr,
                v.questionMarks.map { convertKeyword(it, Node.Keyword::Question) }
            )
        },
        rhs = convertName(v.callableReference)
    ).map(v)

    open fun convertClassLiteral(v: KtClassLiteralExpression) = Node.ClassLiteralExpression(
        lhs = v.receiverExpression?.let { expr ->
            convertDoubleColonReceiver(
                expr,
                v.questionMarks.map { convertKeyword(it, Node.Keyword::Question) }
            )
        }
    ).map(v)

    open fun convertDoubleColonReceiver(
        v: KtExpression,
        questionMarks: List<Node.Keyword.Question>
    ): Node.DoubleColonExpression.Receiver = when (v) {
        is KtSimpleNameExpression -> Node.DoubleColonExpression.Receiver.Type(
            type = Node.SimpleType(
                qualifiers = listOf(),
                name = convertName(v.getReferencedNameElement()),
                typeArgs = null,
            ).mapNotCorrespondsPsiElement(v),
            questionMarks = questionMarks,
        ).map(v)
        is KtCallExpression ->
            if (v.valueArgumentList == null && v.lambdaArguments.isEmpty())
                Node.DoubleColonExpression.Receiver.Type(
                    type = Node.SimpleType(
                        qualifiers = listOf(),
                        name = convertName(
                            v.calleeExpression as? KtSimpleNameExpression
                                ?: error("Missing text for call ref type of $v")
                        ),
                        typeArgs = v.typeArgumentList?.let(::convertTypeArgs)
                    ).mapNotCorrespondsPsiElement(v),
                    questionMarks = questionMarks,
                ).map(v)
            else Node.DoubleColonExpression.Receiver.Expression(convertExpression(v)).map(v)
        is KtDotQualifiedExpression -> {
            val lhs = convertDoubleColonReceiver(v.receiverExpression, questionMarks)
            val rhs = v.selectorExpression?.let { convertDoubleColonReceiver(it, questionMarks) }
            if (lhs is Node.DoubleColonExpression.Receiver.Type && rhs is Node.DoubleColonExpression.Receiver.Type)
                Node.DoubleColonExpression.Receiver.Type(
                    type = Node.SimpleType(
                        qualifiers = lhs.type.qualifiers + Node.SimpleType.Qualifier(lhs.type.name, lhs.type.typeArgs),
                        name = rhs.type.name,
                        typeArgs = rhs.type.typeArgs,
                    ).mapNotCorrespondsPsiElement(v),
                    questionMarks = listOf(),
                ).map(v)
            else Node.DoubleColonExpression.Receiver.Expression(convertExpression(v)).map(v)
        }
        else -> Node.DoubleColonExpression.Receiver.Expression(convertExpression(v)).map(v)
    }

    open fun convertParenthesized(v: KtParenthesizedExpression) = Node.ParenthesizedExpression(
        expression = convertExpression(v.expression ?: error("No expression for $v"))
    ).map(v)

    open fun convertStringTemplate(v: KtStringTemplateExpression) = Node.StringTemplateExpression(
        entries = v.entries.map(::convertStringTemplateEntry),
        raw = v.text.startsWith("\"\"\"")
    ).map(v)

    open fun convertStringTemplateEntry(v: KtStringTemplateEntry) = when (v) {
        is KtLiteralStringTemplateEntry ->
            Node.StringTemplateExpression.Entry.Regular(v.text).map(v)
        is KtSimpleNameStringTemplateEntry ->
            Node.StringTemplateExpression.Entry.ShortTemplate(v.expression?.text ?: error("No short tmpl text")).map(v)
        is KtBlockStringTemplateEntry ->
            Node.StringTemplateExpression.Entry.LongTemplate(convertExpression(v.expression ?: error("No expr tmpl")))
                .map(v)
        is KtEscapeStringTemplateEntry ->
            if (v.text.startsWith("\\u"))
                Node.StringTemplateExpression.Entry.UnicodeEscape(v.text.substring(2)).map(v)
            else
                Node.StringTemplateExpression.Entry.RegularEscape(v.unescapedValue.first()).map(v)
        else ->
            error("Unrecognized string template type for $v")
    }

    open fun convertConst(v: KtConstantExpression) = Node.ConstantExpression(
        value = v.text,
        form = when (v.node.elementType) {
            KtNodeTypes.BOOLEAN_CONSTANT -> Node.ConstantExpression.Form.BOOLEAN
            KtNodeTypes.CHARACTER_CONSTANT -> Node.ConstantExpression.Form.CHAR
            KtNodeTypes.INTEGER_CONSTANT -> Node.ConstantExpression.Form.INT
            KtNodeTypes.FLOAT_CONSTANT -> Node.ConstantExpression.Form.FLOAT
            KtNodeTypes.NULL -> Node.ConstantExpression.Form.NULL
            else -> error("Unrecognized const type for $v")
        }
    ).map(v)

    open fun convertLambda(v: KtLambdaExpression) = Node.LambdaExpression(
        params = v.functionLiteral.valueParameterList?.let(::convertLambdaParams),
        body = v.bodyExpression?.let(::convertLambdaBody)
    ).map(v)

    open fun convertLambdaParams(v: KtParameterList) = Node.LambdaExpression.Params(
        elements = v.parameters.map(::convertLambdaParam),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertLambdaParam(v: KtParameter): Node.LambdaExpression.Param {
        val destructuringDeclaration = v.destructuringDeclaration
        return if (destructuringDeclaration != null) {
            Node.LambdaExpression.Param(
                lPar = destructuringDeclaration.lPar?.let { convertKeyword(it, Node.Keyword::LPar) },
                variables = destructuringDeclaration.entries.map(::convertLambdaParamVariable),
                trailingComma = destructuringDeclaration.trailingComma?.let(::convertComma),
                rPar = destructuringDeclaration.rPar?.let { convertKeyword(it, Node.Keyword::RPar) },
                colon = v.colon?.let { convertKeyword(it, Node.Keyword::Colon) },
                destructTypeRef = v.typeReference?.let(::convertTypeRef),
            ).map(v)
        } else {
            Node.LambdaExpression.Param(
                lPar = null,
                variables = listOf(
                    Node.LambdaExpression.Param.Variable(
                        modifiers = v.modifierList?.let(::convertModifiers),
                        name = v.nameIdentifier?.let(::convertName) ?: error("No lambda param name on $v"),
                        typeRef = v.typeReference?.let(::convertTypeRef),
                    ).mapNotCorrespondsPsiElement(v)
                ),
                trailingComma = null,
                rPar = null,
                colon = null,
                destructTypeRef = null,
            ).map(v)
        }
    }

    open fun convertLambdaParamVariable(v: KtDestructuringDeclarationEntry) = Node.LambdaExpression.Param.Variable(
        modifiers = v.modifierList?.let(::convertModifiers),
        name = v.nameIdentifier?.let(::convertName) ?: error("No lambda param name on $v"),
        typeRef = v.typeReference?.let(::convertTypeRef),
    ).map(v)

    open fun convertLambdaBody(v: KtBlockExpression) = Node.LambdaExpression.Body(
        statements = v.statements.map(::convertStatement)
    ).map(v)

    open fun convertThis(v: KtThisExpression) = Node.ThisExpression(
        label = v.getLabelName()
    ).map(v)

    open fun convertSuper(v: KtSuperExpression) = Node.SuperExpression(
        typeArg = v.superTypeQualifier?.let(::convertTypeRef),
        label = v.getLabelName()
    ).map(v)

    open fun convertWhen(v: KtWhenExpression) = Node.WhenExpression(
        whenKeyword = convertKeyword(v.whenKeyword, Node.Keyword::When),
        lPar = v.leftParenthesis?.let { convertKeyword(it, Node.Keyword::LPar) },
        expression = v.subjectExpression?.let(::convertExpression),
        rPar = v.rightParenthesis?.let { convertKeyword(it, Node.Keyword::RPar) },
        branches = v.entries.map(::convertWhenEntry),
    ).map(v)

    open fun convertWhenEntry(v: KtWhenEntry): Node.WhenExpression.Branch {
        val elseKeyword = v.elseKeyword
        return if (elseKeyword == null) {
            Node.WhenExpression.Branch.Conditional(
                conditions = v.conditions.map(::convertWhenCondition),
                trailingComma = v.trailingComma?.let(::convertComma),
                body = convertExpression(v.expression ?: error("No when entry body for $v"))
            ).map(v)
        } else {
            Node.WhenExpression.Branch.Else(
                elseKeyword = convertKeyword(elseKeyword, Node.Keyword::Else),
                body = convertExpression(v.expression ?: error("No when entry body for $v")),
            ).map(v)
        }
    }

    open fun convertWhenCondition(v: KtWhenCondition) = when (v) {
        is KtWhenConditionWithExpression -> Node.WhenExpression.Condition.Expression(
            expression = convertExpression(v.expression ?: error("No when cond expr for $v"))
        ).map(v)
        is KtWhenConditionInRange -> Node.WhenExpression.Condition.In(
            expression = convertExpression(v.rangeExpression ?: error("No when in expr for $v")),
            not = v.isNegated
        ).map(v)
        is KtWhenConditionIsPattern -> Node.WhenExpression.Condition.Is(
            typeRef = convertTypeRef(v.typeReference ?: error("No when is type for $v")),
            not = v.isNegated
        ).map(v)
        else -> error("Unrecognized when cond of $v")
    }

    open fun convertObject(v: KtObjectLiteralExpression) = Node.ObjectExpression(
        declaration = convertClass(v.objectDeclaration),
    ).map(v)

    open fun convertThrow(v: KtThrowExpression) = Node.ThrowExpression(
        expression = convertExpression(v.thrownExpression ?: error("No throw expr for $v"))
    ).map(v)

    open fun convertReturn(v: KtReturnExpression) = Node.ReturnExpression(
        label = v.getLabelName(),
        expression = v.returnedExpression?.let(::convertExpression)
    ).map(v)

    open fun convertContinue(v: KtContinueExpression) = Node.ContinueExpression(
        label = v.getLabelName()
    ).map(v)

    open fun convertBreak(v: KtBreakExpression) = Node.BreakExpression(
        label = v.getLabelName()
    ).map(v)

    open fun convertCollLit(v: KtCollectionLiteralExpression) = Node.CollectionLiteralExpression(
        expressions = v.getInnerExpressions().map(::convertExpression),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertValueArgName(v: KtValueArgumentName) = Node.NameExpression(
        name = (v.referenceExpression.getIdentifier() ?: error("No identifier for $v")).text,
    ).map(v)

    open fun convertName(v: KtSimpleNameExpression) = Node.NameExpression(
        name = (v.getIdentifier() ?: error("No identifier for $v")).text,
    ).map(v)

    open fun convertName(v: PsiElement) = Node.NameExpression(
        name = v.text
    ).map(v)

    open fun convertLabeled(v: KtLabeledExpression) = Node.LabeledExpression(
        label = v.getLabelName() ?: error("No label name for $v"),
        expression = convertExpression(v.baseExpression ?: error("No label expr for $v"))
    ).map(v)

    open fun convertAnnotated(v: KtAnnotatedExpression) = Node.AnnotatedExpression(
        annotationSets = convertAnnotationSets(v),
        expression = convertExpression(v.baseExpression ?: error("No annotated expr for $v"))
    ).map(v)

    open fun convertCall(v: KtCallExpression) = Node.CallExpression(
        expression = convertExpression(v.calleeExpression ?: error("No call expr for $v")),
        typeArgs = v.typeArgumentList?.let(::convertTypeArgs),
        args = v.valueArgumentList?.let(::convertValueArgs),
        lambdaArg = v.lambdaArguments.also {
            if (it.size >= 2) {
                // According to the Kotlin syntax, at most one lambda argument is allowed.
                // However, Kotlin compiler can parse multiple lambda arguments.
                throw Unsupported("At most one lambda argument is allowed")
            }
        }.firstOrNull()?.let(::convertCallLambdaArg)
    ).map(v)

    open fun convertCallLambdaArg(v: KtLambdaArgument): Node.CallExpression.LambdaArg {
        var label: String? = null
        var annotationSets: List<Node.AnnotationSetModifier> = emptyList()
        fun KtExpression.extractLambda(): KtLambdaExpression? = when (this) {
            is KtLambdaExpression -> this
            is KtLabeledExpression -> baseExpression?.extractLambda().also {
                label = getLabelName()
            }
            is KtAnnotatedExpression -> baseExpression?.extractLambda().also {
                annotationSets = convertAnnotationSets(this)
            }
            else -> null
        }

        val expr = v.getArgumentExpression()?.extractLambda() ?: error("No lambda for $v")
        return Node.CallExpression.LambdaArg(
            annotationSets = annotationSets,
            label = label,
            expression = convertLambda(expr)
        ).map(v)
    }

    open fun convertArrayAccess(v: KtArrayAccessExpression) = Node.ArrayAccessExpression(
        expression = convertExpression(v.arrayExpression ?: error("No array expr for $v")),
        indices = v.indexExpressions.map(::convertExpression),
        trailingComma = v.trailingComma?.let(::convertComma),
    ).map(v)

    open fun convertAnonymousFunction(v: KtNamedFunction) = Node.AnonymousFunctionExpression(convertFunction(v))

    open fun convertPropertyExpr(v: KtProperty) = Node.PropertyExpression(
        declaration = convertProperty(v)
    ).map(v)

    open fun convertPropertyExpr(v: KtDestructuringDeclaration) = Node.PropertyExpression(
        declaration = convertProperty(v)
    ).map(v)

    open fun convertBlock(v: KtBlockExpression) = Node.BlockExpression(
        statements = v.statements.map(::convertStatement)
    ).map(v)

    open fun convertStatement(v: KtExpression): Node.Statement =
        if (v is KtDeclaration)
            convertDeclaration(v)
        else
            convertExpression(v)

    open fun convertAnnotationSets(v: KtElement): List<Node.AnnotationSetModifier> = v.children.flatMap { elem ->
        // We go over the node children because we want to preserve order
        when (elem) {
            is KtAnnotationEntry ->
                listOf(convertAnnotationSet(elem))
            is KtAnnotation ->
                listOf(convertAnnotationSet(elem))
            is KtAnnotationsContainer ->
                convertAnnotationSets(elem)
            else ->
                emptyList()
        }
    }

    open fun convertAnnotationSet(v: KtAnnotation) = Node.AnnotationSetModifier(
        atSymbol = v.atSymbol?.let { convertKeyword(it, Node.Keyword::At) },
        target = v.useSiteTarget?.let(::convertAnnotationSetTarget),
        colon = v.colon?.let { convertKeyword(it, Node.Keyword::Colon) },
        lBracket = v.lBracket?.let { convertKeyword(it, Node.Keyword::LBracket) },
        annotations = v.entries.map {
            convertAnnotationWithoutMapping(it)
                .map(it)
        },
        rBracket = v.rBracket?.let { convertKeyword(it, Node.Keyword::RBracket) },
    ).map(v)

    open fun convertAnnotationSet(v: KtAnnotationEntry) = Node.AnnotationSetModifier(
        atSymbol = v.atSymbol?.let { convertKeyword(it, Node.Keyword::At) },
        target = v.useSiteTarget?.let(::convertAnnotationSetTarget),
        colon = v.colon?.let { convertKeyword(it, Node.Keyword::Colon) },
        lBracket = null,
        annotations = listOf(
            convertAnnotationWithoutMapping(v)
                .mapNotCorrespondsPsiElement(v),
        ),
        rBracket = null,
    ).map(v)

    open fun convertAnnotationSetTarget(v: KtAnnotationUseSiteTarget) = Node.AnnotationSetModifier.Target(
        when (v.getAnnotationUseSiteTarget()) {
            AnnotationUseSiteTarget.FIELD -> Node.AnnotationSetModifier.Target.Token.FIELD
            AnnotationUseSiteTarget.FILE -> Node.AnnotationSetModifier.Target.Token.FILE
            AnnotationUseSiteTarget.PROPERTY -> Node.AnnotationSetModifier.Target.Token.PROPERTY
            AnnotationUseSiteTarget.PROPERTY_GETTER -> Node.AnnotationSetModifier.Target.Token.GET
            AnnotationUseSiteTarget.PROPERTY_SETTER -> Node.AnnotationSetModifier.Target.Token.SET
            AnnotationUseSiteTarget.RECEIVER -> Node.AnnotationSetModifier.Target.Token.RECEIVER
            AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> Node.AnnotationSetModifier.Target.Token.PARAM
            AnnotationUseSiteTarget.SETTER_PARAMETER -> Node.AnnotationSetModifier.Target.Token.SETPARAM
            AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> Node.AnnotationSetModifier.Target.Token.DELEGATE
        }
    )

    open fun convertAnnotationWithoutMapping(v: KtAnnotationEntry) = Node.AnnotationSetModifier.Annotation(
        type = convertType(
            v.calleeExpression?.typeReference?.typeElement
                ?: error("No callee expression, type reference or type element for $v")
        ) as? Node.SimpleType ?: error("calleeExpression is not simple type"),
        args = v.valueArgumentList?.let(::convertValueArgs),
    )

    open fun convertModifiers(v: KtModifierList): Node.Modifiers {
        val nonExtraChildren = v.allChildren.filterNot { it is PsiComment || it is PsiWhiteSpace }.toList()
        return Node.Modifiers(
            elements = nonExtraChildren.mapNotNull { psi ->
                // We go over the node children because we want to preserve order
                when (psi) {
                    is KtAnnotationEntry -> convertAnnotationSet(psi)
                    is KtAnnotation -> convertAnnotationSet(psi)
                    is PsiWhiteSpace -> null
                    else -> convertKeywordModifier(psi)
                }
            }.toList(),
        ).map(v)
    }

    open fun convertKeywordModifier(v: PsiElement) = Node.KeywordModifier.of(v.text)
        .map(v)

    open fun convertPostModifiers(v: KtElement): List<Node.PostModifier> {
        val nonExtraChildren = v.allChildren.filterNot { it is PsiComment || it is PsiWhiteSpace }.toList()

        if (nonExtraChildren.isEmpty()) {
            return listOf()
        }

        var prevPsi = nonExtraChildren[0]
        return nonExtraChildren.drop(1).mapNotNull { psi ->
            when (psi) {
                is KtTypeConstraintList -> Node.TypeConstraints(
                    whereKeyword = convertKeyword(prevPsi, Node.Keyword::Where),
                    constraints = convertTypeConstraints(psi),
                ).mapNotCorrespondsPsiElement(v)
                is KtContractEffectList -> Node.Contract(
                    contractKeyword = convertKeyword(prevPsi, Node.Keyword::Contract),
                    contractEffects = convertContractEffects(psi),
                ).mapNotCorrespondsPsiElement(v)
                else -> null
            }.also { prevPsi = psi }
        }
    }

    open fun convertComma(v: PsiElement): Node.Keyword.Comma = convertKeyword(v, Node.Keyword::Comma)

    open fun <T : Node.Keyword> convertKeyword(v: PsiElement, factory: () -> T): T =
        factory().also {
            check(v.text == it.string) { "Unexpected keyword: ${v.text}" }
        }.map(v)

    protected open fun <T : Node> T.map(v: PsiElement) = also { onNode(it, v) }
    protected open fun <T : Node> T.mapNotCorrespondsPsiElement(v: PsiElement) = also { onNode(it, null) }

    class Unsupported(message: String) : UnsupportedOperationException(message)

    companion object : Converter() {
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

        internal val KtEnumEntry.comma: PsiElement?
            get() = findChildByType(this, KtTokens.COMMA)

        internal val KtNullableType.leftParenthesis: PsiElement?
            get() = findChildByType(this, KtTokens.LPAR)
        internal val KtNullableType.rightParenthesis: PsiElement?
            get() = findChildByType(this, KtTokens.RPAR)

        internal val KtContainerNode.expression: KtExpression
            get() = findChildByClass<KtExpression>(this) ?: error("No expression for $this")

        internal val KtIfExpression.thenContainer: KtContainerNode
            get() = findChildByType(this, KtNodeTypes.THEN) as? KtContainerNode ?: error("No then container for $this")
        internal val KtIfExpression.elseContainer: KtContainerNode?
            get() = findChildByType(this, KtNodeTypes.ELSE) as? KtContainerNode

        internal val KtCatchClause.catchKeyword: PsiElement
            get() = findChildByType(this, KtTokens.CATCH_KEYWORD) ?: error("No catch keyword for $this")

        internal val KtForExpression.loopRangeContainer: KtContainerNode
            get() = findChildByType(this, KtNodeTypes.LOOP_RANGE)
                    as? KtContainerNode ?: error("No in range for $this")

        internal val KtWhileExpressionBase.whileKeyword: PsiElement
            get() = findChildByType(this, KtTokens.WHILE_KEYWORD) ?: error("No while keyword for $this")

        internal val KtLoopExpression.bodyContainer: KtContainerNodeForControlStructureBody
            get() = findChildByType(this, KtNodeTypes.BODY)
                    as? KtContainerNodeForControlStructureBody ?: error("No body for $this")

        internal val KtWhileExpressionBase.conditionContainer: KtContainerNode
            get() = findChildByType(this, KtNodeTypes.CONDITION)
                    as? KtContainerNode ?: error("No condition for $this")

        internal val KtDoubleColonExpression.questionMarks
            get() = allChildren
                .takeWhile { it.node.elementType != KtTokens.COLONCOLON }
                .filter { it.node.elementType == KtTokens.QUEST }
                .toList()

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

        private fun findChildByType(v: KtElement, type: IElementType): PsiElement? =
            v.node.findChildByType(type)?.psi

        private inline fun <reified T> findChildByClass(v: PsiElement): T? =
            v.children.firstOrNull { it is T } as? T

        internal fun findTrailingSeparator(v: KtElement, elementType: IElementType): PsiElement? =
            // Note that v.children.lastOrNull() is not equal to v.lastChild. children contain only KtElements, but lastChild is a last element of allChildren.
            v.children.lastOrNull()
                ?.siblings(forward = true, withItself = false)
                ?.find { it.node.elementType == elementType }
    }
}