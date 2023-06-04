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

    open fun convertStatement(v: KtExpression): Node.Statement = when (v) {
        is KtForExpression -> convertFor(v)
        is KtWhileExpression -> convertWhile(v)
        is KtDoWhileExpression -> convertDoWhile(v)
        is KtDeclaration -> convertDeclaration(v)
        else -> convertExpression(v) as? Node.Statement ?: error("Unrecognized statement $v")
    }

    open fun convertFor(v: KtForExpression) = Node.Statement.ForStatement(
        forKeyword = convertKeyword(v.forKeyword),
        lPar = convertKeyword(v.leftParenthesis ?: error("No left parenthesis for $v")),
        loopParam = convertLambdaParam(v.loopParameter ?: error("No param on for $v")),
        inKeyword = convertKeyword(v.inKeyword ?: error("No in keyword for $v")),
        loopRange = convertExpression(v.loopRangeContainer.expression),
        rPar = convertKeyword(v.rightParenthesis ?: error("No right parenthesis for $v")),
        body = convertExpression(v.bodyContainer.expression ?: error("No body expression for $v")),
    ).map(v)

    open fun convertWhile(v: KtWhileExpression) = Node.Statement.WhileStatement(
        whileKeyword = convertKeyword(v.whileKeyword),
        lPar = convertKeyword(v.leftParenthesis ?: error("No left parenthesis for $v")),
        condition = convertExpression(v.conditionContainer.expression),
        rPar = convertKeyword(v.rightParenthesis ?: error("No right parenthesis for $v")),
        body = convertExpression(v.bodyContainer.expression ?: error("No body expression for $v")),
    ).map(v)

    open fun convertDoWhile(v: KtDoWhileExpression) = Node.Statement.DoWhileStatement(
        doKeyword = convertKeyword(v.doKeyword),
        body = convertExpression(v.bodyContainer.expression ?: error("No body expression for $v")),
        whileKeyword = convertKeyword(v.whileKeyword ?: error("No while keyword for $v")),
        lPar = convertKeyword(v.leftParenthesis ?: error("No left parenthesis for $v")),
        condition = convertExpression(v.conditionContainer.expression),
        rPar = convertKeyword(v.rightParenthesis ?: error("No right parenthesis for $v")),
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

    open fun convertClass(v: KtClassOrObject) = Node.Declaration.ClassDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        classDeclarationKeyword = v.getDeclarationKeyword()?.let(::convertKeyword)
            ?: error("declarationKeyword not found"),
        name = v.nameIdentifier?.let(::convertName),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        primaryConstructor = v.primaryConstructor?.let(::convertPrimaryConstructor),
        classParents = v.getSuperTypeList()?.let(::convertParents),
        typeConstraintSet = v.typeConstraintList?.let { typeConstraintList ->
            Node.PostModifier.TypeConstraintSet(
                whereKeyword = convertKeyword(v.whereKeyword),
                constraints = convertTypeConstraints(typeConstraintList),
            ).mapNotCorrespondsPsiElement(v)
        },
        classBody = v.body?.let(::convertClassBody),
    ).map(v)

    open fun convertParents(v: KtSuperTypeList) = Node.Declaration.ClassDeclaration.ClassParents(
        elements = v.entries.map(::convertParent),
    ).map(v)

    open fun convertParent(v: KtSuperTypeListEntry) = when (v) {
        is KtSuperTypeCallEntry -> Node.Declaration.ClassDeclaration.ConstructorClassParent(
            type = v.typeReference?.let(::convertType) as? Node.Type.SimpleType
                ?: error("Bad type on super call $v"),
            args = v.valueArgumentList?.let(::convertValueArgs) ?: error("No value arguments for $v"),
        ).map(v)
        is KtDelegatedSuperTypeEntry -> Node.Declaration.ClassDeclaration.DelegationClassParent(
            type = v.typeReference?.let(::convertType)
                ?: error("No type on delegated super type $v"),
            byKeyword = convertKeyword(v.byKeywordNode.psi),
            expression = convertExpression(v.delegateExpression ?: error("Missing delegateExpression for $v")),
        ).map(v)
        is KtSuperTypeEntry -> Node.Declaration.ClassDeclaration.TypeClassParent(
            type = v.typeReference?.let(::convertType)
                ?: error("No type on super type $v"),
        ).map(v)
        else -> error("Unknown super type entry $v")
    }

    open fun convertPrimaryConstructor(v: KtPrimaryConstructor) = Node.Declaration.ClassDeclaration.PrimaryConstructor(
        modifiers = v.modifierList?.let(::convertModifiers),
        constructorKeyword = v.getConstructorKeyword()?.let(::convertKeyword),
        params = v.valueParameterList?.let(::convertFuncParams)
    ).map(v)

    open fun convertInit(v: KtAnonymousInitializer) = Node.Declaration.ClassDeclaration.ClassBody.Initializer(
        modifiers = v.modifierList?.let(::convertModifiers),
        block = convertBlock(v.body as? KtBlockExpression ?: error("No init block for $v")),
    ).map(v)

    open fun convertFunction(v: KtNamedFunction): Node.Declaration.FunctionDeclaration {
        if (v.typeParameterList != null) {
            val hasTypeParameterListBeforeFunctionName = v.allChildren.find {
                it is KtTypeParameterList || it is KtTypeReference || it.node.elementType == KtTokens.IDENTIFIER
            } is KtTypeParameterList
            if (!hasTypeParameterListBeforeFunctionName) {
                // According to the Kotlin syntax, type parameters are not allowed here. However, Kotlin compiler can parse them.
                throw Unsupported("Type parameters after function name is not allowed")
            }
        }

        return Node.Declaration.FunctionDeclaration(
            modifiers = v.modifierList?.let(::convertModifiers),
            funKeyword = v.funKeyword?.let { convertKeyword(it) } ?: error("No fun keyword for $v"),
            typeParams = v.typeParameterList?.let(::convertTypeParams),
            receiverType = v.receiverTypeReference?.let(::convertType),
            name = v.nameIdentifier?.let(::convertName),
            params = v.valueParameterList?.let(::convertFuncParams),
            returnType = v.typeReference?.let(::convertType),
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
        type = v.typeReference?.let(::convertType),
        equals = v.equalsToken?.let(::convertKeyword),
        defaultValue = v.defaultValue?.let(::convertExpression),
    ).map(v)

    open fun convertProperty(v: KtProperty) = Node.Declaration.PropertyDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        valOrVarKeyword = convertKeyword(v.valOrVarKeyword),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        receiverType = v.receiverTypeReference?.let(::convertType),
        lPar = null,
        variables = listOf(
            Node.Variable(
                modifiers = null,
                name = v.nameIdentifier?.let(::convertName) ?: error("No property name on $v"),
                type = v.typeReference?.let(::convertType)
            ).mapNotCorrespondsPsiElement(v)
        ),
        trailingComma = null,
        rPar = null,
        typeConstraintSet = v.typeConstraintList?.let { typeConstraintList ->
            Node.PostModifier.TypeConstraintSet(
                whereKeyword = convertKeyword(v.whereKeyword),
                constraints = convertTypeConstraints(typeConstraintList),
            ).mapNotCorrespondsPsiElement(v)
        },
        equals = v.equalsToken?.let(::convertKeyword),
        initializer = v.initializer?.let(::convertExpression),
        propertyDelegate = v.delegate?.let(::convertPropertyDelegate),
        accessors = v.accessors.map(::convertPropertyAccessor),
    ).map(v)

    open fun convertProperty(v: KtDestructuringDeclaration) = Node.Declaration.PropertyDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        valOrVarKeyword = v.valOrVarKeyword?.let(::convertKeyword) ?: error("Missing valOrVarKeyword"),
        typeParams = null,
        receiverType = null,
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
        type = v.typeReference?.let(::convertType)
    ).map(v)

    open fun convertPropertyDelegate(v: KtPropertyDelegate) = Node.Declaration.PropertyDeclaration.PropertyDelegate(
        byKeyword = convertKeyword(v.byKeyword),
        expression = convertExpression(v.expression ?: error("Missing expression for $v")),
    ).map(v)

    open fun convertPropertyAccessor(v: KtPropertyAccessor) =
        if (v.isGetter) Node.Declaration.PropertyDeclaration.Getter(
            modifiers = v.modifierList?.let(::convertModifiers),
            getKeyword = convertKeyword(v.getKeyword),
            type = v.returnTypeReference?.let(::convertType),
            postModifiers = convertPostModifiers(v),
            equals = v.equalsToken?.let(::convertKeyword),
            body = v.bodyExpression?.let(::convertExpression),
        ).map(v) else Node.Declaration.PropertyDeclaration.Setter(
            modifiers = v.modifierList?.let(::convertModifiers),
            setKeyword = convertKeyword(v.setKeyword),
            params = v.parameterList?.let(::convertLambdaParams),
            postModifiers = convertPostModifiers(v),
            equals = v.equalsToken?.let(::convertKeyword),
            body = v.bodyExpression?.let(::convertExpression),
        ).map(v)

    open fun convertTypeAlias(v: KtTypeAlias) = Node.Declaration.TypeAliasDeclaration(
        modifiers = v.modifierList?.let(::convertModifiers),
        name = v.nameIdentifier?.let(::convertName) ?: error("No type alias name for $v"),
        typeParams = v.typeParameterList?.let(::convertTypeParams),
        equals = convertKeyword(v.equalsToken),
        type = convertType(v.getTypeReference() ?: error("No type alias ref for $v"))
    ).map(v)

    open fun convertSecondaryConstructor(v: KtSecondaryConstructor) =
        Node.Declaration.ClassDeclaration.ClassBody.SecondaryConstructor(
            modifiers = v.modifierList?.let(::convertModifiers),
            constructorKeyword = convertKeyword(v.getConstructorKeyword()),
            params = v.valueParameterList?.let(::convertFuncParams),
            delegationCall = if (v.hasImplicitDelegationCall()) null else convertCall(
                v.getDelegationCall()
            ),
            block = v.bodyExpression?.let(::convertBlock)
        ).map(v)

    open fun convertEnumEntry(v: KtEnumEntry): Node.Declaration.ClassDeclaration.ClassBody.EnumEntry =
        Node.Declaration.ClassDeclaration.ClassBody.EnumEntry(
            modifiers = v.modifierList?.let(::convertModifiers),
            name = v.nameIdentifier?.let(::convertName) ?: error("Unnamed enum"),
            args = v.initializerList?.let(::convertValueArgs),
            classBody = v.body?.let(::convertClassBody),
        ).map(v)

    open fun convertClassBody(v: KtClassBody): Node.Declaration.ClassDeclaration.ClassBody {
        val ktEnumEntries = v.declarations.filterIsInstance<KtEnumEntry>()
        val declarationsExcludingKtEnumEntry = v.declarations.filter { it !is KtEnumEntry }
        return Node.Declaration.ClassDeclaration.ClassBody(
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
        type = v.extendsBound?.let(::convertType)
    ).map(v)

    open fun convertTypeArgs(v: KtTypeArgumentList) = Node.TypeArgs(
        elements = v.arguments.map(::convertTypeArg),
        trailingComma = v.trailingComma?.let(::convertKeyword),
    ).map(v)

    open fun convertTypeArg(v: KtTypeProjection): Node.TypeArg {
        return if (v.projectionKind == KtProjectionKind.STAR) {
            Node.TypeArg.StarProjection(
                asterisk = convertKeyword(v.projectionToken ?: error("Missing projection token for $v")),
            ).map(v)
        } else {
            Node.TypeArg.TypeProjection(
                modifiers = v.modifierList?.let(::convertModifiers),
                type = convertType(v.typeReference ?: error("Missing type ref for $v")),
            ).map(v)
        }
    }

    open fun convertTypeConstraints(v: KtTypeConstraintList) = Node.PostModifier.TypeConstraintSet.TypeConstraints(
        elements = v.constraints.map(::convertTypeConstraint),
    ).map(v)

    open fun convertTypeConstraint(v: KtTypeConstraint) = Node.PostModifier.TypeConstraintSet.TypeConstraint(
        annotationSets = v.children.mapNotNull {
            when (it) {
                is KtAnnotationEntry -> convertAnnotationSet(it)
                is KtAnnotation -> convertAnnotationSet(it)
                else -> null
            }
        },
        name = v.subjectTypeParameterName?.let { convertName(it) } ?: error("No type constraint name for $v"),
        type = convertType(v.boundTypeReference ?: error("No type constraint type for $v"))
    ).map(v)

    open fun convertType(v: KtTypeReference): Node.Type {
        return convertType(v, v.nonExtraChildren(), mapTarget = v)
    }

    // Actually, type of v is KtTypeReference or KtNullableType.
    open fun convertType(v: KtElement, targetChildren: List<PsiElement>, mapTarget: KtElement?): Node.Type {
        val modifierListElements = targetChildren.takeWhile { it is KtModifierList }
        check(modifierListElements.size <= 1) { "Multiple modifier lists in type children: $targetChildren" }
        val modifierList = modifierListElements.firstOrNull() as? KtModifierList
        val questionMarks = targetChildren.takeLastWhile { it.node.elementType == KtTokens.QUEST }
        val restChildren =
            targetChildren.subList(modifierListElements.size, targetChildren.size - questionMarks.size)

        // questionMarks can be ignored here because when v is KtNullableType, it will be handled in caller side.
        if (restChildren.first().node.elementType == KtTokens.LPAR && restChildren.last().node.elementType == KtTokens.RPAR) {
            return Node.Type.ParenthesizedType(
                modifiers = modifierList?.let { convertModifiers(it) },
                lPar = convertKeyword(restChildren.first()),
                type = convertType(v, restChildren.subList(1, restChildren.size - 1), mapTarget = null),
                rPar = convertKeyword(restChildren.last()),
            ).mapIfPossible(mapTarget)
        }

        val modifiers = modifierList?.let(::convertModifiers)
        return when (val typeEl = restChildren.first()) {
            is KtFunctionType -> Node.Type.FunctionType(
                modifiers = modifiers,
                contextReceiver = typeEl.contextReceiverList?.let(::convertContextReceiver),
                receiverType = typeEl.receiver?.typeReference?.let(::convertType),
                dotSymbol = findChildByType(typeEl, KtTokens.DOT)?.let(::convertKeyword),
                params = typeEl.parameterList?.let(::convertTypeFunctionParams),
                returnType = convertType(typeEl.returnTypeReference ?: error("No return type for $typeEl")),
            ).map(mapTarget ?: typeEl)
            is KtUserType -> Node.Type.SimpleType(
                modifiers = modifiers,
                qualifiers = generateSequence(typeEl.qualifier) { it.qualifier }.toList().reversed()
                    .map(::convertTypeSimpleQualifier),
                name = convertName(typeEl.referenceExpression ?: error("No type name for $typeEl")),
                typeArgs = typeEl.typeArgumentList?.let(::convertTypeArgs),
            ).map(mapTarget ?: typeEl)
            is KtNullableType -> Node.Type.NullableType(
                modifiers = modifiers,
                type = convertType(typeEl, typeEl.nonExtraChildren(), null),
                questionMark = convertKeyword(
                    findChildByType(typeEl, KtTokens.QUEST) ?: error("No question mark for $typeEl")
                ),
            ).map(mapTarget ?: typeEl)
            is KtDynamicType -> Node.Type.DynamicType(
                modifiers = modifiers,
            ).map(mapTarget ?: typeEl)
            else -> error("Unrecognized type of $typeEl")
        }
    }

    open fun convertContextReceiver(v: KtContextReceiverList) = Node.Type.FunctionType.ContextReceiver(
        receiverTypes = Node.Type.FunctionType.ContextReceiverTypes(
            elements = v.contextReceivers().map(::convertType),
            trailingComma = null,
        ).mapNotCorrespondsPsiElement(v)
    ).map(v)

    open fun convertType(v: KtContextReceiver): Node.Type {
        val typeRef = v.typeReference() ?: error("No type ref for $v")
        return convertType(
            typeRef,
            typeRef.nonExtraChildren(),
            mapTarget = v,
        )
    }

    open fun convertContractEffects(v: KtContractEffectList) = Node.PostModifier.Contract.ContractEffects(
        elements = v.children.filterIsInstance<KtContractEffect>().map(::convertContractEffect),
        trailingComma = findTrailingSeparator(v, KtTokens.COMMA)?.let(::convertKeyword),
    ).map(v)

    open fun convertContractEffect(v: KtContractEffect) = Node.PostModifier.Contract.ContractEffect(
        expression = convertExpression(v.getExpression()),
    ).map(v)

    open fun convertTypeFunctionParams(v: KtParameterList) = Node.Type.FunctionType.FunctionTypeParams(
        elements = v.parameters.map(::convertTypeFunctionParam),
        trailingComma = v.trailingComma?.let(::convertKeyword)
    ).map(v)

    open fun convertTypeFunctionParam(v: KtParameter) = Node.Type.FunctionType.FunctionTypeParam(
        name = v.nameIdentifier?.let(::convertName),
        type = convertType(v.typeReference ?: error("No param type"))
    ).map(v)

    open fun convertTypeSimpleQualifier(v: KtUserType) = Node.Type.SimpleType.SimpleTypeQualifier(
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
        asterisk = v.getSpreadElement()?.let(::convertKeyword),
        expression = convertExpression(v.getArgumentExpression() ?: error("No expr for value arg"))
    ).map(v)

    open fun convertExpression(v: KtExpression): Node.Expression = when (v) {
        is KtIfExpression -> convertIf(v)
        is KtTryExpression -> convertTry(v)
        is KtBinaryExpression -> convertBinary(v)
        is KtQualifiedExpression -> convertBinary(v)
        is KtPrefixExpression -> convertPrefix(v)
        is KtPostfixExpression -> convertPostfix(v)
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
        is KtConstructorDelegationReferenceExpression -> convertThisOrSuperExpression(v)
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
        lPar = convertKeyword(v.leftParenthesis ?: error("No left parenthesis on if for $v")),
        condition = convertExpression(v.condition ?: error("No cond on if for $v")),
        rPar = convertKeyword(v.rightParenthesis ?: error("No right parenthesis on if for $v")),
        body = convertExpression(v.thenContainer.expression ?: error("No then body on if for $v")),
        elseKeyword = v.elseKeyword?.let(::convertKeyword),
        elseBody = v.elseContainer?.expression?.let(::convertExpression),
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

    open fun convertPrefix(v: KtPrefixExpression) = Node.Expression.PrefixUnaryExpression(
        operator = convertKeyword(v.operationReference),
        expression = convertExpression(v.baseExpression ?: error("No base expression for $v")),
    ).map(v)

    open fun convertPostfix(v: KtPostfixExpression) = Node.Expression.PostfixUnaryExpression(
        expression = convertExpression(v.baseExpression ?: error("No base expression for $v")),
        operator = convertKeyword(v.operationReference),
    ).map(v)

    open fun convertBinaryType(v: KtBinaryExpressionWithTypeRHS) = Node.Expression.BinaryTypeExpression(
        lhs = convertExpression(v.left),
        operator = convertKeyword(v.operationReference),
        rhs = convertType(v.right ?: error("No type op rhs for $v"))
    ).map(v)

    open fun convertBinaryType(v: KtIsExpression) = Node.Expression.BinaryTypeExpression(
        lhs = convertExpression(v.leftHandSide),
        operator = convertKeyword(v.operationReference),
        rhs = convertType(v.typeReference ?: error("No type op rhs for $v"))
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

    open fun convertConst(v: KtConstantExpression): Node.Expression.ConstantLiteralExpression =
        when (v.node.elementType) {
            KtNodeTypes.BOOLEAN_CONSTANT -> Node.Expression.BooleanLiteralExpression(v.text)
            KtNodeTypes.CHARACTER_CONSTANT -> Node.Expression.CharacterLiteralExpression(v.text)
            KtNodeTypes.INTEGER_CONSTANT -> Node.Expression.IntegerLiteralExpression(v.text)
            KtNodeTypes.FLOAT_CONSTANT -> Node.Expression.RealLiteralExpression(v.text)
            KtNodeTypes.NULL -> Node.Expression.NullLiteralExpression()
            else -> error("Unrecognized constant type for $v")
        }.map(v)

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
                destructType = v.typeReference?.let(::convertType),
            ).map(v)
        } else {
            Node.LambdaParam(
                lPar = null,
                variables = listOf(
                    Node.Variable(
                        modifiers = v.modifierList?.let(::convertModifiers),
                        name = v.nameIdentifier?.let(::convertName) ?: error("No lambda param name on $v"),
                        type = v.typeReference?.let(::convertType),
                    ).mapNotCorrespondsPsiElement(v)
                ),
                trailingComma = null,
                rPar = null,
                colon = null,
                destructType = null,
            ).map(v)
        }
    }

    open fun convertLambdaBody(v: KtBlockExpression) = Node.Expression.LambdaExpression.LambdaBody(
        statements = v.statements.map(::convertStatement)
    ).map(v)

    open fun convertThis(v: KtThisExpression) = Node.Expression.ThisExpression(
        label = v.getTargetLabel()?.let(::convertName),
    ).map(v)

    open fun convertSuper(v: KtSuperExpression) = Node.Expression.SuperExpression(
        typeArgType = v.superTypeQualifier?.let(::convertType),
        label = v.getTargetLabel()?.let(::convertName),
    ).map(v)

    open fun convertWhen(v: KtWhenExpression) = Node.Expression.WhenExpression(
        whenKeyword = convertKeyword(v.whenKeyword),
        lPar = v.leftParenthesis?.let(::convertKeyword),
        expression = v.subjectExpression?.let(::convertExpression),
        rPar = v.rightParenthesis?.let(::convertKeyword),
        whenBranches = v.entries.map(::convertWhenEntry),
    ).map(v)

    open fun convertWhenEntry(v: KtWhenEntry): Node.Expression.WhenExpression.WhenBranch {
        val elseKeyword = v.elseKeyword
        val body = convertExpression(v.expression ?: error("No when entry body for $v"))
        return if (elseKeyword == null) {
            Node.Expression.WhenExpression.ConditionalWhenBranch(
                whenConditions = v.conditions.map(::convertWhenCondition),
                trailingComma = v.trailingComma?.let(::convertKeyword),
                body = body,
            ).map(v)
        } else {
            Node.Expression.WhenExpression.ElseWhenBranch(
                elseKeyword = convertKeyword(elseKeyword),
                body = body,
            ).map(v)
        }
    }

    open fun convertWhenCondition(v: KtWhenCondition) = when (v) {
        is KtWhenConditionWithExpression -> Node.Expression.WhenExpression.ExpressionWhenCondition(
            expression = convertExpression(v.expression ?: error("No when cond expr for $v")),
        ).map(v)
        is KtWhenConditionInRange -> Node.Expression.WhenExpression.RangeWhenCondition(
            operator = convertKeyword(v.operationReference),
            expression = convertExpression(v.rangeExpression ?: error("No when in expr for $v")),
        ).map(v)
        is KtWhenConditionIsPattern -> Node.Expression.WhenExpression.TypeWhenCondition(
            operator = convertKeyword(
                findChildByType(v, KtTokens.IS_KEYWORD)
                    ?: findChildByType(v, KtTokens.NOT_IS)
                    ?: error("No when is operator for $v")
            ),
            type = convertType(v.typeReference ?: error("No when is type for $v")),
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
        label = v.getTargetLabel()?.let(::convertName),
        expression = v.returnedExpression?.let(::convertExpression)
    ).map(v)

    open fun convertContinue(v: KtContinueExpression) = Node.Expression.ContinueExpression(
        label = v.getTargetLabel()?.let(::convertName),
    ).map(v)

    open fun convertBreak(v: KtBreakExpression) = Node.Expression.BreakExpression(
        label = v.getTargetLabel()?.let(::convertName),
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

    open fun convertThisOrSuperExpression(v: KtConstructorDelegationReferenceExpression): Node.Expression =
        when (v.text) {
            "this" -> Node.Expression.ThisExpression(
                label = null,
            ).map(v)
            "super" -> Node.Expression.SuperExpression(
                typeArgType = null,
                label = null,
            ).map(v)
            else -> error("Unrecognized this/super expr $v")
        }

    open fun convertLabeled(v: KtLabeledExpression) = Node.Expression.LabeledExpression(
        label = convertName(v.getTargetLabel() ?: error("No label name for $v")),
        statement = convertStatement(v.baseExpression ?: error("No label expr for $v"))
    ).map(v)

    open fun convertAnnotated(v: KtAnnotatedExpression) = Node.Expression.AnnotatedExpression(
        annotationSets = convertAnnotationSets(v),
        statement = convertStatement(v.baseExpression ?: error("No annotated expr for $v"))
    ).map(v)

    open fun convertCall(v: KtCallElement) = Node.Expression.CallExpression(
        calleeExpression = convertExpression(v.calleeExpression ?: error("No call expr for $v")),
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
        var label: Node.Expression.NameExpression? = null
        var annotationSets: List<Node.Modifier.AnnotationSet> = emptyList()
        fun KtExpression.extractLambda(): KtLambdaExpression? = when (this) {
            is KtLambdaExpression -> this
            is KtLabeledExpression -> baseExpression?.extractLambda().also {
                label = convertName(getTargetLabel() ?: error("No label for $this"))
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

    open fun convertArrayAccess(v: KtArrayAccessExpression) = Node.Expression.IndexedAccessExpression(
        expression = convertExpression(v.arrayExpression ?: error("No array expr for $v")),
        indices = v.indexExpressions.map(::convertExpression),
        trailingComma = v.trailingComma?.let(::convertKeyword),
    ).map(v)

    open fun convertAnonymousFunction(v: KtNamedFunction) =
        Node.Expression.AnonymousFunctionExpression(convertFunction(v))

    open fun convertPropertyExpr(v: KtProperty) = Node.Expression.PropertyExpression(
        property = convertProperty(v)
    ).map(v)

    open fun convertPropertyExpr(v: KtDestructuringDeclaration) = Node.Expression.PropertyExpression(
        property = convertProperty(v)
    ).map(v)

    open fun convertBlock(v: KtBlockExpression) = Node.Expression.BlockExpression(
        statements = v.statements.map(::convertStatement)
    ).map(v)

    open fun convertAnnotationSets(v: KtElement): List<Node.Modifier.AnnotationSet> = v.children.flatMap { elem ->
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

    open fun convertAnnotationSet(v: KtAnnotation) = Node.Modifier.AnnotationSet(
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

    open fun convertAnnotationSet(v: KtAnnotationEntry) = Node.Modifier.AnnotationSet(
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

    open fun convertAnnotationWithoutMapping(v: KtAnnotationEntry) = Node.Modifier.AnnotationSet.Annotation(
        type = convertType(
            v.calleeExpression?.typeReference ?: error("No callee expression, type reference or type element for $v")
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
                    else -> convertKeyword<Node.Modifier.KeywordModifier>(psi)
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
                is KtTypeConstraintList -> Node.PostModifier.TypeConstraintSet(
                    whereKeyword = convertKeyword(prevPsi),
                    constraints = convertTypeConstraints(psi),
                ).mapNotCorrespondsPsiElement(v)
                is KtContractEffectList -> Node.PostModifier.Contract(
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
    protected open fun <T : Node> T.mapIfPossible(v: PsiElement?) = also { onNode(it, v) }

    class Unsupported(message: String) : UnsupportedOperationException(message)

    companion object : Converter() {
        internal val KtImportDirective.importKeyword: PsiElement
            get() = findChildByType(this, KtTokens.IMPORT_KEYWORD) ?: error("Missing import keyword for $this")
        internal val KtImportDirective.asterisk: PsiElement?
            get() = findChildByType(this, KtTokens.MUL)

        internal val KtTypeParameterListOwner.whereKeyword: PsiElement
            get() = findChildByType(this, KtTokens.WHERE_KEYWORD) ?: error("No where keyword for $this")

        internal val KtDeclarationWithInitializer.equalsToken: PsiElement
            get() = findChildByType(this, KtTokens.EQ) ?: error("No equals token for $this")
        internal val KtTypeAlias.equalsToken: PsiElement
            get() = findChildByType(this, KtTokens.EQ) ?: error("No equals token for $this")

        internal val KtPropertyDelegate.byKeyword: PsiElement
            get() = byKeywordNode.psi

        internal val KtPropertyAccessor.setKeyword: PsiElement
            get() = findChildByType(this, KtTokens.SET_KEYWORD) ?: error("No set keyword for $this")
        internal val KtPropertyAccessor.getKeyword: PsiElement
            get() = findChildByType(this, KtTokens.GET_KEYWORD) ?: error("No get keyword for $this")

        internal val KtEnumEntry.comma: PsiElement?
            get() = findChildByType(this, KtTokens.COMMA)

        internal val KtContainerNode.expression: KtExpression
            get() = findChildByClass<KtExpression>(this) ?: error("No expression for $this")

        internal val KtIfExpression.thenContainer: KtContainerNode
            get() = findChildByType(this, KtNodeTypes.THEN) as? KtContainerNode
                ?: error("No then container for $this")
        internal val KtIfExpression.elseContainer: KtContainerNode?
            get() = findChildByType(this, KtNodeTypes.ELSE) as? KtContainerNode

        internal val KtCatchClause.catchKeyword: PsiElement
            get() = findChildByType(this, KtTokens.CATCH_KEYWORD) ?: error("No catch keyword for $this")

        internal val KtLoopExpression.bodyContainer: KtContainerNodeForControlStructureBody
            get() = findChildByType(this, KtNodeTypes.BODY)
                    as? KtContainerNodeForControlStructureBody ?: error("No body for $this")

        internal val KtForExpression.loopRangeContainer: KtContainerNode
            get() = findChildByType(this, KtNodeTypes.LOOP_RANGE)
                    as? KtContainerNode ?: error("No in range for $this")

        internal val KtWhileExpressionBase.whileKeyword: PsiElement
            get() = findChildByType(this, KtTokens.WHILE_KEYWORD) ?: error("No while keyword for $this")
        internal val KtWhileExpressionBase.conditionContainer: KtContainerNode
            get() = findChildByType(this, KtNodeTypes.CONDITION)
                    as? KtContainerNode ?: error("No condition for $this")
        internal val KtDoWhileExpression.doKeyword: PsiElement
            get() = findChildByType(this, KtTokens.DO_KEYWORD) ?: error("No do keyword for $this")

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

        internal fun PsiElement.nonExtraChildren() =
            allChildren.filterNot { it is PsiComment || it is PsiWhiteSpace }.toList()
    }
}