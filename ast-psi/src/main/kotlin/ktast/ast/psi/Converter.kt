package ktast.ast.psi

import ktast.ast.Node
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.siblings
import kotlin.reflect.full.createInstance

open class Converter {
    protected open fun onNode(node: Node, elem: PsiElement?) {}

    open fun convertKotlinFile(v: KtFile) = Node.KotlinFile(
        annotationSets = convertAnnotationSets(v),
        packageDirective = v.packageDirective?.takeIf { it.packageNames.isNotEmpty() }?.let(::convertPackageDirective),
        importDirectives = v.importList?.let(::convertImportDirectives),
        declarations = v.declarations.map(::convertDeclaration)
    ).map(v)

    open fun convertPackageDirective(v: KtPackageDirective) = Node.PackageDirective(
        packageKeyword = convertKeyword(v.packageKeyword ?: error("No package keyword $v")),
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
        importKeyword = convertKeyword(v.importKeyword),
        names = convertImportNames(v.importedReference ?: error("No imported reference for $v"))
                + listOfNotNull(v.asterisk?.let(::convertName)),
        importAlias = v.alias?.let(::convertImportAlias)
    ).map(v)

    open fun convertImportNames(v: KtExpression): List<Node.Expression.NameExpression> = when (v) {
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
        classDeclarationKeyword = v.getDeclarationKeyword()?.let(::convertKeyword)
            ?: error("declarationKeyword not found"),
        name = v.nameIdentifier?.let(::convertName),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        primaryConstructor = v.primaryConstructor?.let(::convertPrimaryConstructor),
        classParents = v.getSuperTypeList()?.let(::convertParents),
        typeConstraintSet = v.typeConstraintList?.let { typeConstraintList ->
            Node.TypeConstraintSet(
                whereKeyword = convertKeyword(v.whereKeyword),
                constraints = convertTypeConstraints(typeConstraintList),
            ).mapNotCorrespondsPsiElement(v)
        },
        classBody = v.body?.let(::convertClassBody),
    ).map(v)

    open fun convertParents(v: KtSuperTypeList) = Node.ClassDeclaration.ClassParents(
        elements = v.entries.map(::convertParent),
    ).map(v)

    open fun convertParent(v: KtSuperTypeListEntry) = when (v) {
        is KtSuperTypeCallEntry -> Node.ClassDeclaration.ConstructorClassParent(
            type = v.typeReference?.typeElement?.let(::convertType) as? Node.Type.SimpleType
                ?: error("Bad type on super call $v"),
            args = v.valueArgumentList?.let(::convertValueArgs),
        ).map(v)
        is KtDelegatedSuperTypeEntry -> Node.ClassDeclaration.DelegationClassParent(
            type = v.typeReference?.typeElement?.let(::convertType)
                ?: error("No type on delegated super type $v"),
            byKeyword = convertKeyword(v.byKeywordNode.psi),
            expression = convertExpression(v.delegateExpression ?: error("Missing delegateExpression for $v")),
        ).map(v)
        is KtSuperTypeEntry -> Node.ClassDeclaration.TypeClassParent(
            type = v.typeReference?.typeElement?.let(::convertType)
                ?: error("No type on super type $v"),
        ).map(v)
        else -> error("Unknown super type entry $v")
    }

    open fun convertPrimaryConstructor(v: KtPrimaryConstructor) = Node.ClassDeclaration.PrimaryConstructor(
        modifiers = v.modifierList?.let(::convertModifiers),
        constructorKeyword = v.getConstructorKeyword()?.let(::convertKeyword),
        params = v.valueParameterList?.let(::convertFuncParams)
    ).map(v)

    open fun convertInit(v: KtAnonymousInitializer) = Node.ClassDeclaration.ClassBody.Initializer(
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
            funKeyword = v.funKeyword?.let { convertKeyword(it) } ?: error("No fun keyword for $v"),
            typeParams = v.typeParameterList?.let(::convertTypeParams),
            receiverTypeRef = v.receiverTypeReference?.let(::convertTypeRef),
            name = v.nameIdentifier?.let(::convertName),
            params = v.valueParameterList?.let(::convertFuncParams),
            typeRef = v.typeReference?.let(::convertTypeRef),
            postModifiers = convertPostModifiers(v),
            equals = v.equalsToken?.let(::convertKeyword),
            body = v.bodyExpression?.let { convertExpression(it) },
        ).map(v)
    }

    open fun convertFuncParams(v: KtParameterList) = Node.FunctionParams(
        elements = v.parameters.map(::convertFuncParam),
        trailingComma = v.trailingComma?.let(::convertKeyword),
    ).map(v)

    open fun convertFuncParam(v: KtParameter) = Node.FunctionParam(
        modifiers = v.modifierList?.let(::convertModifiers),
        valOrVarKeyword = v.valOrVarKeyword?.let(::convertKeyword),
        name = v.nameIdentifier?.let(::convertName) ?: error("No param name"),
        typeRef = v.typeReference?.let(::convertTypeRef),
        equals = v.equalsToken?.let(::convertKeyword),
        defaultValue = v.defaultValue?.let(::convertExpression),
    ).map(v)

    open fun convertProperty(v: KtProperty) = Node.PropertyDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        valOrVarKeyword = convertKeyword(v.valOrVarKeyword),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        receiverTypeRef = v.receiverTypeReference?.let(::convertTypeRef),
        lPar = null,
        variables = listOf(
            Node.Variable(
                modifiers = null,
                name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
                typeRef = v.typeReference?.let(::convertTypeRef)
            ).mapNotCorrespondsPsiElement(v)
        ),
        trailingComma = null,
        rPar = null,
        typeConstraintSet = v.typeConstraintList?.let { typeConstraintList ->
            Node.TypeConstraintSet(
                whereKeyword = convertKeyword(v.whereKeyword),
                constraints = convertTypeConstraints(typeConstraintList),
            ).mapNotCorrespondsPsiElement(v)
        },
        equals = v.equalsToken?.let(::convertKeyword),
        initializer = v.initializer?.let(::convertExpression),
        propertyDelegate = v.delegate?.let(::convertPropertyDelegate),
        accessors = v.accessors.map(::convertPropertyAccessor),
    ).map(v)

    open fun convertProperty(v: KtDestructuringDeclaration) = Node.PropertyDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        valOrVarKeyword = v.valOrVarKeyword?.let(::convertKeyword) ?: error("Missing valOrVarKeyword"),
        typeParams = null,
        receiverTypeRef = null,
        lPar = v.lPar?.let(::convertKeyword),
        variables = v.entries.map(::convertVariable),
        trailingComma = v.trailingComma?.let(::convertKeyword),
        rPar = v.rPar?.let(::convertKeyword),
        typeConstraintSet = null,
        equals = convertKeyword(v.equalsToken),
        initializer = v.initializer?.let(::convertExpression),
        propertyDelegate = null,
        accessors = listOf(),
    ).map(v)

    open fun convertVariable(v: KtDestructuringDeclarationEntry) = Node.Variable(
        modifiers = v.modifierList?.let(::convertModifiers),
        name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
        typeRef = v.typeReference?.let(::convertTypeRef)
    ).map(v)

    open fun convertPropertyDelegate(v: KtPropertyDelegate) = Node.PropertyDeclaration.PropertyDelegate(
        byKeyword = convertKeyword(v.byKeyword),
        expression = convertExpression(v.expression ?: error("Missing expression for $v")),
    ).map(v)

    open fun convertPropertyAccessor(v: KtPropertyAccessor) =
        if (v.isGetter) Node.PropertyDeclaration.Getter(
            modifiers = v.modifierList?.let(::convertModifiers),
            getKeyword = convertKeyword(v.getKeyword),
            typeRef = v.returnTypeReference?.let(::convertTypeRef),
            postModifiers = convertPostModifiers(v),
            equals = v.equalsToken?.let(::convertKeyword),
            body = v.bodyExpression?.let(::convertExpression),
        ).map(v) else Node.PropertyDeclaration.Setter(
            modifiers = v.modifierList?.let(::convertModifiers),
            setKeyword = convertKeyword(v.setKeyword),
            params = v.parameterList?.let(::convertLambdaParams),
            postModifiers = convertPostModifiers(v),
            equals = v.equalsToken?.let(::convertKeyword),
            body = v.bodyExpression?.let(::convertExpression),
        ).map(v)

    open fun convertTypeAlias(v: KtTypeAlias) = Node.TypeAliasDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        name = v.nameIdentifier?.let(::convertName) ?: error("No type alias name for $v"),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        typeRef = convertTypeRef(v.getTypeReference() ?: error("No type alias ref for $v"))
    ).map(v)

    open fun convertSecondaryConstructor(v: KtSecondaryConstructor) =
        Node.ClassDeclaration.ClassBody.SecondaryConstructor(
            modifiers = v.modifierList?.let(::convertModifiers),
            constructorKeyword = convertKeyword(v.getConstructorKeyword()),
            params = v.valueParameterList?.let(::convertFuncParams),
            constructorDelegationCall = if (v.hasImplicitDelegationCall()) null else convertSecondaryConstructorDelegationCall(
                v.getDelegationCall()
            ),
            block = v.bodyExpression?.let(::convertBlock)
        ).map(v)

    open fun convertSecondaryConstructorDelegationCall(v: KtConstructorDelegationCall) =
        Node.ClassDeclaration.ClassBody.SecondaryConstructor.ConstructorDelegationCall(
            targetKeyword = convertKeyword(v.calleeExpression?.firstChild ?: error("No delegation target for $v")),
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
        trailingComma = v.trailingComma?.let(::convertKeyword),
    ).map(v)

    open fun convertTypeParam(v: KtTypeParameter) = Node.TypeParam(
        modifiers = v.modifierList?.let(::convertModifiers),
        name = v.nameIdentifier?.let(::convertName) ?: error("No type param name for $v"),
        typeRef = v.extendsBound?.let(::convertTypeRef)
    ).map(v)

    open fun convertTypeArgs(v: KtTypeArgumentList) = Node.TypeArgs(
        elements = v.arguments.map(::convertTypeArg),
        trailingComma = v.trailingComma?.let(::convertKeyword),
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
            lPar = lPar?.let(::convertKeyword),
            modifiers = modifierList?.let { convertModifiers(it) },
            type = convertType(
                v.typeElement ?: error("No type element for $v"),
                innerLPar,
                innerModifierList,
                innerRPar
            ),
            rPar = rPar?.let(::convertKeyword),
        ).map(v)
    }

    open fun convertTypeConstraints(v: KtTypeConstraintList) = Node.TypeConstraintSet.TypeConstraints(
        elements = v.constraints.map(::convertTypeConstraint),
    ).map(v)

    open fun convertTypeConstraint(v: KtTypeConstraint) = Node.TypeConstraintSet.TypeConstraint(
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
        is KtFunctionType -> Node.Type.FunctionType(
            lPar = lPar?.let(::convertKeyword),
            modifiers = modifierList?.let(::convertModifiers),
            contextReceivers = v.contextReceiverList?.let { convertContextReceivers(it) },
            functionTypeReceiver = v.receiver?.let(::convertTypeFunctionReceiver),
            params = v.parameterList?.let(::convertTypeFunctionParams),
            returnTypeRef = convertTypeRef(v.returnTypeReference ?: error("No return type")),
            rPar = rPar?.let(::convertKeyword),
        ).map(v)
        is KtUserType -> Node.Type.SimpleType(
            qualifiers = generateSequence(v.qualifier) { it.qualifier }.toList().reversed()
                .map(::convertTypeSimpleQualifier),
            name = convertName(v.referenceExpression ?: error("No type name for $v")),
            typeArgs = v.typeArgumentList?.let(::convertTypeArgs),
        ).map(v)
        is KtNullableType -> Node.Type.NullableType(
            lPar = v.leftParenthesis?.let(::convertKeyword),
            modifiers = v.modifierList?.let(::convertModifiers),
            type = convertType(v.innerType ?: error("No inner type for nullable")),
            rPar = v.rightParenthesis?.let(::convertKeyword),
        ).map(v)
        is KtDynamicType -> Node.Type.DynamicType().map(v)
        else -> error("Unrecognized type of $v")
    }

    open fun convertContextReceivers(v: KtContextReceiverList) = Node.Type.FunctionType.ContextReceivers(
        elements = v.contextReceivers().map(::convertContextReceiver),
        trailingComma = null,
    ).map(v)

    open fun convertContextReceiver(v: KtContextReceiver) = Node.Type.FunctionType.ContextReceiver(
        typeRef = convertTypeRef(v.typeReference() ?: error("Missing type reference for $v")),
    ).map(v)

    open fun convertContractEffects(v: KtContractEffectList) = Node.Contract.ContractEffects(
        elements = v.children.filterIsInstance<KtContractEffect>().map(::convertContractEffect),
        trailingComma = findTrailingSeparator(v, KtTokens.COMMA)?.let(::convertKeyword),
    ).map(v)

    open fun convertContractEffect(v: KtContractEffect) = Node.Contract.ContractEffect(
        expression = convertExpression(v.getExpression()),
    ).map(v)

    open fun convertTypeFunctionReceiver(v: KtFunctionTypeReceiver) = Node.Type.FunctionType.FunctionTypeReceiver(
        typeRef = convertTypeRef(v.typeReference),
    ).map(v)

    open fun convertTypeFunctionParams(v: KtParameterList) = Node.Type.FunctionType.FunctionTypeParams(
        elements = v.parameters.map(::convertTypeFunctionParam),
        trailingComma = v.trailingComma?.let(::convertKeyword)
    ).map(v)

    open fun convertTypeFunctionParam(v: KtParameter) = Node.Type.FunctionType.FunctionTypeParam(
        name = v.nameIdentifier?.let(::convertName),
        typeRef = convertTypeRef(v.typeReference ?: error("No param type"))
    ).map(v)

    open fun convertTypeSimpleQualifier(v: KtUserType) = Node.Type.SimpleType.Qualifier(
        name = convertName(v.referenceExpression ?: error("No type name for $v")),
        typeArgs = v.typeArgumentList?.let(::convertTypeArgs),
    ).map(v)

    open fun convertValueArgs(v: KtValueArgumentList) = Node.ValueArgs(
        elements = v.arguments.map(::convertValueArg),
        trailingComma = v.trailingComma?.let(::convertKeyword),
    ).map(v)

    open fun convertValueArgs(v: KtInitializerList): Node.ValueArgs {
        val valueArgumentList = (v.initializers.firstOrNull() as? KtSuperTypeCallEntry)?.valueArgumentList
            ?: error("No value arguments for $v")
        return Node.ValueArgs(
            elements = (valueArgumentList.arguments).map(::convertValueArg),
            trailingComma = valueArgumentList.trailingComma?.let(::convertKeyword),
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

    open fun convertIf(v: KtIfExpression) = Node.Expression.IfExpression(
        ifKeyword = convertKeyword(v.ifKeyword),
        condition = convertExpression(v.condition ?: error("No cond on if for $v")),
        body = convertExpressionContainer(v.thenContainer),
        elseBody = v.elseContainer?.let(::convertExpressionContainer),
    ).map(v)

    open fun convertTry(v: KtTryExpression) = Node.Expression.TryExpression(
        block = convertBlock(v.tryBlock),
        catchClauses = v.catchClauses.map(::convertTryCatch),
        finallyBlock = v.finallyBlock?.finalExpression?.let(::convertBlock)
    ).map(v)

    open fun convertTryCatch(v: KtCatchClause) = Node.Expression.TryExpression.CatchClause(
        catchKeyword = convertKeyword(v.catchKeyword),
        params = convertFuncParams(v.parameterList ?: error("No catch params for $v")),
        block = convertBlock(v.catchBody as? KtBlockExpression ?: error("No catch block for $v")),
    ).map(v)

    open fun convertFor(v: KtForExpression) = Node.Expression.ForExpression(
        forKeyword = convertKeyword(v.forKeyword),
        loopParam = convertLambdaParam(v.loopParameter ?: error("No param on for $v")),
        loopRange = convertExpressionContainer(v.loopRangeContainer),
        body = convertExpressionContainer(v.bodyContainer),
    ).map(v)

    open fun convertWhile(v: KtWhileExpressionBase) = Node.Expression.WhileExpression(
        whileKeyword = convertKeyword(v.whileKeyword),
        condition = convertExpressionContainer(v.conditionContainer),
        body = convertExpressionContainer(v.bodyContainer),
        doWhile = v is KtDoWhileExpression
    ).map(v)

    open fun convertBinary(v: KtBinaryExpression) = Node.Expression.BinaryExpression(
        lhs = convertExpression(v.left ?: error("No binary lhs for $v")),
        operator = if (v.operationReference.isConventionOperator()) {
            convertKeyword(v.operationReference)
        } else {
            convertName(v.operationReference.firstChild)
        },
        rhs = convertExpression(v.right ?: error("No binary rhs for $v"))
    ).map(v)

    open fun convertBinary(v: KtQualifiedExpression) = Node.Expression.BinaryExpression(
        lhs = convertExpression(v.receiverExpression),
        operator = convertKeyword(v.operationTokenNode.psi),
        rhs = convertExpression(v.selectorExpression ?: error("No qualified rhs for $v"))
    ).map(v)

    open fun convertUnary(v: KtUnaryExpression) = Node.Expression.UnaryExpression(
        expression = convertExpression(v.baseExpression ?: error("No unary expr for $v")),
        operator = convertKeyword(v.operationReference),
        prefix = v is KtPrefixExpression
    ).map(v)

    open fun convertBinaryType(v: KtBinaryExpressionWithTypeRHS) = Node.Expression.BinaryTypeExpression(
        lhs = convertExpression(v.left),
        operator = convertKeyword(v.operationReference),
        rhs = convertTypeRef(v.right ?: error("No type op rhs for $v"))
    ).map(v)

    open fun convertBinaryType(v: KtIsExpression) = Node.Expression.BinaryTypeExpression(
        lhs = convertExpression(v.leftHandSide),
        operator = convertKeyword(v.operationReference),
        rhs = convertTypeRef(v.typeReference ?: error("No type op rhs for $v"))
    ).map(v)

    open fun convertCallableReference(v: KtCallableReferenceExpression) = Node.Expression.CallableReferenceExpression(
        lhs = v.receiverExpression?.let(::convertExpression),
        questionMarks = v.questionMarks.map(::convertKeyword),
        rhs = convertName(v.callableReference)
    ).map(v)

    open fun convertClassLiteral(v: KtClassLiteralExpression) = Node.Expression.ClassLiteralExpression(
        lhs = v.receiverExpression?.let(::convertExpression),
        questionMarks = v.questionMarks.map(::convertKeyword),
    ).map(v)

    open fun convertParenthesized(v: KtParenthesizedExpression) = Node.Expression.ParenthesizedExpression(
        expression = convertExpression(v.expression ?: error("No expression for $v"))
    ).map(v)

    open fun convertStringTemplate(v: KtStringTemplateExpression) = Node.Expression.StringLiteralExpression(
        entries = v.entries.map(::convertStringTemplateEntry),
        raw = v.text.startsWith("\"\"\"")
    ).map(v)

    open fun convertStringTemplateEntry(v: KtStringTemplateEntry) = when (v) {
        is KtLiteralStringTemplateEntry -> Node.Expression.StringLiteralExpression.LiteralStringEntry(v.text)
            .map(v)
        is KtEscapeStringTemplateEntry -> Node.Expression.StringLiteralExpression.EscapeStringEntry(v.text)
            .map(v)
        is KtStringTemplateEntryWithExpression ->
            Node.Expression.StringLiteralExpression.TemplateStringEntry(
                expression = convertExpression(v.expression ?: error("No expr tmpl")),
                short = v is KtSimpleNameStringTemplateEntry,
            ).map(v)
        else ->
            error("Unrecognized string template type for $v")
    }

    open fun convertConst(v: KtConstantExpression) = Node.Expression.ConstantLiteralExpression(
        value = v.text,
        form = when (v.node.elementType) {
            KtNodeTypes.BOOLEAN_CONSTANT -> Node.Expression.ConstantLiteralExpression.Form.BOOLEAN
            KtNodeTypes.CHARACTER_CONSTANT -> Node.Expression.ConstantLiteralExpression.Form.CHAR
            KtNodeTypes.INTEGER_CONSTANT -> Node.Expression.ConstantLiteralExpression.Form.INT
            KtNodeTypes.FLOAT_CONSTANT -> Node.Expression.ConstantLiteralExpression.Form.FLOAT
            KtNodeTypes.NULL -> Node.Expression.ConstantLiteralExpression.Form.NULL
            else -> error("Unrecognized const type for $v")
        }
    ).map(v)

    open fun convertLambda(v: KtLambdaExpression) = Node.Expression.LambdaExpression(
        params = v.functionLiteral.valueParameterList?.let(::convertLambdaParams),
        lambdaBody = v.bodyExpression?.let(::convertLambdaBody)
    ).map(v)

    open fun convertLambdaParams(v: KtParameterList) = Node.LambdaParams(
        elements = v.parameters.map(::convertLambdaParam),
        trailingComma = v.trailingComma?.let(::convertKeyword),
    ).map(v)

    open fun convertLambdaParam(v: KtParameter): Node.LambdaParam {
        val destructuringDeclaration = v.destructuringDeclaration
        return if (destructuringDeclaration != null) {
            Node.LambdaParam(
                lPar = destructuringDeclaration.lPar?.let(::convertKeyword),
                variables = destructuringDeclaration.entries.map(::convertVariable),
                trailingComma = destructuringDeclaration.trailingComma?.let(::convertKeyword),
                rPar = destructuringDeclaration.rPar?.let(::convertKeyword),
                colon = v.colon?.let(::convertKeyword),
                destructTypeRef = v.typeReference?.let(::convertTypeRef),
            ).map(v)
        } else {
            Node.LambdaParam(
                lPar = null,
                variables = listOf(
                    Node.Variable(
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

    open fun convertLambdaBody(v: KtBlockExpression) = Node.Expression.LambdaExpression.LambdaBody(
        statements = v.statements.map(::convertStatement)
    ).map(v)

    open fun convertThis(v: KtThisExpression) = Node.Expression.ThisExpression(
        label = v.getLabelName()
    ).map(v)

    open fun convertSuper(v: KtSuperExpression) = Node.Expression.SuperExpression(
        typeArg = v.superTypeQualifier?.let(::convertTypeRef),
        label = v.getLabelName()
    ).map(v)

    open fun convertWhen(v: KtWhenExpression) = Node.Expression.WhenExpression(
        whenKeyword = convertKeyword(v.whenKeyword),
        lPar = v.leftParenthesis?.let(::convertKeyword),
        expression = v.subjectExpression?.let(::convertExpression),
        rPar = v.rightParenthesis?.let(::convertKeyword),
        whenBranches = v.entries.map(::convertWhenEntry),
    ).map(v)

    open fun convertWhenEntry(v: KtWhenEntry) = Node.Expression.WhenExpression.WhenBranch(
        whenConditions = v.conditions.map(::convertWhenCondition),
        trailingComma = v.trailingComma?.let(::convertKeyword),
        elseKeyword = v.elseKeyword?.let(::convertKeyword),
        body = convertExpression(v.expression ?: error("No when entry body for $v"))
    ).map(v)

    open fun convertWhenCondition(v: KtWhenCondition) = when (v) {
        is KtWhenConditionWithExpression -> Node.Expression.WhenExpression.WhenCondition(
            operator = null,
            expression = convertExpression(v.expression ?: error("No when cond expr for $v")),
            typeRef = null,
        ).map(v)
        is KtWhenConditionInRange -> Node.Expression.WhenExpression.WhenCondition(
            operator = convertKeyword(v.operationReference),
            expression = convertExpression(v.rangeExpression ?: error("No when in expr for $v")),
            typeRef = null,
        ).map(v)
        is KtWhenConditionIsPattern -> Node.Expression.WhenExpression.WhenCondition(
            operator = convertKeyword(
                findChildByType(v, KtTokens.IS_KEYWORD)
                    ?: findChildByType(v, KtTokens.NOT_IS)
                    ?: error("No when is operator for $v")
            ),
            expression = null,
            typeRef = convertTypeRef(v.typeReference ?: error("No when is type for $v")),
        ).map(v)
        else -> error("Unrecognized when cond of $v")
    }

    open fun convertObject(v: KtObjectLiteralExpression) = Node.Expression.ObjectLiteralExpression(
        declaration = convertClass(v.objectDeclaration),
    ).map(v)

    open fun convertThrow(v: KtThrowExpression) = Node.Expression.ThrowExpression(
        expression = convertExpression(v.thrownExpression ?: error("No throw expr for $v"))
    ).map(v)

    open fun convertReturn(v: KtReturnExpression) = Node.Expression.ReturnExpression(
        label = v.getLabelName(),
        expression = v.returnedExpression?.let(::convertExpression)
    ).map(v)

    open fun convertContinue(v: KtContinueExpression) = Node.Expression.ContinueExpression(
        label = v.getLabelName()
    ).map(v)

    open fun convertBreak(v: KtBreakExpression) = Node.Expression.BreakExpression(
        label = v.getLabelName()
    ).map(v)

    open fun convertCollLit(v: KtCollectionLiteralExpression) = Node.Expression.CollectionLiteralExpression(
        expressions = v.getInnerExpressions().map(::convertExpression),
        trailingComma = v.trailingComma?.let(::convertKeyword),
    ).map(v)

    open fun convertValueArgName(v: KtValueArgumentName) = Node.Expression.NameExpression(
        text = (v.referenceExpression.getIdentifier() ?: error("No identifier for $v")).text,
    ).map(v)

    open fun convertName(v: KtSimpleNameExpression) = Node.Expression.NameExpression(
        text = (v.getIdentifier() ?: error("No identifier for $v")).text,
    ).map(v)

    open fun convertName(v: PsiElement) = Node.Expression.NameExpression(
        text = v.text
    ).map(v)

    open fun convertLabeled(v: KtLabeledExpression) = Node.Expression.LabeledExpression(
        label = v.getLabelName() ?: error("No label name for $v"),
        expression = convertExpression(v.baseExpression ?: error("No label expr for $v"))
    ).map(v)

    open fun convertAnnotated(v: KtAnnotatedExpression) = Node.Expression.AnnotatedExpression(
        annotationSets = convertAnnotationSets(v),
        expression = convertExpression(v.baseExpression ?: error("No annotated expr for $v"))
    ).map(v)

    open fun convertCall(v: KtCallExpression) = Node.Expression.CallExpression(
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

    open fun convertCallLambdaArg(v: KtLambdaArgument): Node.Expression.CallExpression.LambdaArg {
        var label: String? = null
        var annotationSets: List<Node.AnnotationSet> = emptyList()
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
        return Node.Expression.CallExpression.LambdaArg(
            annotationSets = annotationSets,
            label = label,
            expression = convertLambda(expr)
        ).map(v)
    }

    open fun convertArrayAccess(v: KtArrayAccessExpression) = Node.Expression.ArrayAccessExpression(
        expression = convertExpression(v.arrayExpression ?: error("No array expr for $v")),
        indices = v.indexExpressions.map(::convertExpression),
        trailingComma = v.trailingComma?.let(::convertKeyword),
    ).map(v)

    open fun convertAnonymousFunction(v: KtNamedFunction) =
        Node.Expression.AnonymousFunctionExpression(convertFunction(v))

    open fun convertPropertyExpr(v: KtProperty) = Node.Expression.PropertyExpression(
        declaration = convertProperty(v)
    ).map(v)

    open fun convertPropertyExpr(v: KtDestructuringDeclaration) = Node.Expression.PropertyExpression(
        declaration = convertProperty(v)
    ).map(v)

    open fun convertBlock(v: KtBlockExpression) = Node.Expression.BlockExpression(
        statements = v.statements.map(::convertStatement)
    ).map(v)

    open fun convertStatement(v: KtExpression): Node.Statement =
        if (v is KtDeclaration)
            convertDeclaration(v)
        else
            convertExpression(v)

    open fun convertAnnotationSets(v: KtElement): List<Node.AnnotationSet> = v.children.flatMap { elem ->
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

    open fun convertAnnotationSet(v: KtAnnotation) = Node.AnnotationSet(
        atSymbol = v.atSymbol?.let(::convertKeyword),
        target = v.useSiteTarget?.let(::convertKeyword),
        colon = v.colon?.let(::convertKeyword),
        lBracket = v.lBracket?.let(::convertKeyword),
        annotations = v.entries.map {
            convertAnnotationWithoutMapping(it)
                .map(it)
        },
        rBracket = v.rBracket?.let(::convertKeyword),
    ).map(v)

    open fun convertAnnotationSet(v: KtAnnotationEntry) = Node.AnnotationSet(
        atSymbol = v.atSymbol?.let(::convertKeyword),
        target = v.useSiteTarget?.let(::convertKeyword),
        colon = v.colon?.let(::convertKeyword),
        lBracket = null,
        annotations = listOf(
            convertAnnotationWithoutMapping(v)
                .mapNotCorrespondsPsiElement(v),
        ),
        rBracket = null,
    ).map(v)

    open fun convertAnnotationWithoutMapping(v: KtAnnotationEntry) = Node.AnnotationSet.Annotation(
        type = convertType(
            v.calleeExpression?.typeReference?.typeElement
                ?: error("No callee expression, type reference or type element for $v")
        ) as? Node.Type.SimpleType ?: error("calleeExpression is not simple type"),
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
                    else -> convertKeyword<Node.KeywordModifier>(psi)
                }
            }.toList(),
        ).map(v)
    }

    open fun convertPostModifiers(v: KtElement): List<Node.PostModifier> {
        val nonExtraChildren = v.allChildren.filterNot { it is PsiComment || it is PsiWhiteSpace }.toList()

        if (nonExtraChildren.isEmpty()) {
            return listOf()
        }

        var prevPsi = nonExtraChildren[0]
        return nonExtraChildren.drop(1).mapNotNull { psi ->
            when (psi) {
                is KtTypeConstraintList -> Node.TypeConstraintSet(
                    whereKeyword = convertKeyword(prevPsi),
                    constraints = convertTypeConstraints(psi),
                ).mapNotCorrespondsPsiElement(v)
                is KtContractEffectList -> Node.Contract(
                    contractKeyword = convertKeyword(prevPsi),
                    contractEffects = convertContractEffects(psi),
                ).mapNotCorrespondsPsiElement(v)
                else -> null
            }.also { prevPsi = psi }
        }
    }

    protected val mapTextToKeywordKClass =
        Node.Keyword::class.sealedSubclasses.filter { it.isData }.associateBy { it.createInstance().text }

    protected inline fun <reified T : Node.Keyword> convertKeyword(v: PsiElement): T =
        ((mapTextToKeywordKClass[v.text]?.createInstance() as? T) ?: error("Unexpected keyword: ${v.text}"))
            .map(v)

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