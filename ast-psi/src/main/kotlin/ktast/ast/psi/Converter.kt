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
        importDirectives = v.importList?.imports?.map(::convertImportDirective) ?: listOf(),
        declarations = v.declarations.map(::convertDeclaration)
    ).map(v)

    open fun convertPackageDirective(v: KtPackageDirective) = Node.PackageDirective(
        packageKeyword = convertKeyword(v.packageKeyword ?: error("No package keyword $v")),
        modifiers = convertModifiers(v.modifierList),
        names = v.packageNames.map(::convertNameExpression),
    ).map(v)

    open fun convertImportDirective(v: KtImportDirective) = Node.ImportDirective(
        importKeyword = convertKeyword(v.importKeyword),
        names = convertImportNames(v.importedReference ?: error("No imported reference for $v"))
                + listOfNotNull(v.asterisk?.let(::convertNameExpression)),
        importAlias = v.alias?.let(::convertImportAlias)
    ).map(v)

    protected fun convertImportNames(v: KtExpression): List<Node.Expression.NameExpression> = when (v) {
        // Flatten nest of KtDotQualifiedExpression into list.
        is KtDotQualifiedExpression ->
            convertImportNames(v.receiverExpression) + listOf(
                convertNameExpression(
                    v.selectorExpression as? KtNameReferenceExpression ?: error("No name reference for $v")
                )
            )
        is KtReferenceExpression -> listOf(convertNameExpression(v))
        else -> error("Unexpected type $v")
    }

    open fun convertImportAlias(v: KtImportAlias) = Node.ImportDirective.ImportAlias(
        name = convertNameExpression(v.nameIdentifier ?: error("No name identifier for $v")),
    ).map(v)

    open fun convertStatement(v: KtExpression): Node.Statement = when (v) {
        is KtForExpression -> convertForStatement(v)
        is KtWhileExpression -> convertWhileStatement(v)
        is KtDoWhileExpression -> convertDoWhileStatement(v)
        is KtDeclaration -> convertDeclaration(v)
        else -> convertExpression(v) as? Node.Statement ?: error("Unrecognized statement $v")
    }

    open fun convertForStatement(v: KtForExpression) = Node.Statement.ForStatement(
        forKeyword = convertKeyword(v.forKeyword),
        lPar = convertKeyword(v.leftParenthesis ?: error("No left parenthesis for $v")),
        loopParam = convertLambdaParam(v.loopParameter ?: error("No param on for $v")),
        inKeyword = convertKeyword(v.inKeyword ?: error("No in keyword for $v")),
        loopRange = convertExpression(v.loopRangeContainer.expression),
        rPar = convertKeyword(v.rightParenthesis ?: error("No right parenthesis for $v")),
        body = convertExpression(v.bodyContainer.expression ?: error("No body expression for $v")),
    ).map(v)

    open fun convertWhileStatement(v: KtWhileExpression) = Node.Statement.WhileStatement(
        whileKeyword = convertKeyword(v.whileKeyword),
        lPar = convertKeyword(v.leftParenthesis ?: error("No left parenthesis for $v")),
        condition = convertExpression(v.conditionContainer.expression),
        rPar = convertKeyword(v.rightParenthesis ?: error("No right parenthesis for $v")),
        body = convertExpression(v.bodyContainer.expression ?: error("No body expression for $v")),
    ).map(v)

    open fun convertDoWhileStatement(v: KtDoWhileExpression) = Node.Statement.DoWhileStatement(
        doKeyword = convertKeyword(v.doKeyword),
        body = convertExpression(v.bodyContainer.expression ?: error("No body expression for $v")),
        whileKeyword = convertKeyword(v.whileKeyword ?: error("No while keyword for $v")),
        lPar = convertKeyword(v.leftParenthesis ?: error("No left parenthesis for $v")),
        condition = convertExpression(v.conditionContainer.expression),
        rPar = convertKeyword(v.rightParenthesis ?: error("No right parenthesis for $v")),
    ).map(v)

    open fun convertDeclaration(v: KtDeclaration): Node.Declaration = when (v) {
        is KtEnumEntry -> error("KtEnumEntry is handled in convertEnumEntry")
        is KtClassOrObject -> convertClassDeclaration(v)
        is KtAnonymousInitializer -> convertInitializer(v)
        is KtNamedFunction -> convertFunctionDeclaration(v)
        is KtDestructuringDeclaration -> convertPropertyDeclaration(v)
        is KtProperty -> convertPropertyDeclaration(v)
        is KtTypeAlias -> convertTypeAliasDeclaration(v)
        is KtSecondaryConstructor -> convertSecondaryConstructor(v)
        else -> error("Unrecognized declaration type for $v")
    }

    open fun convertClassDeclaration(v: KtClassOrObject) = Node.Declaration.ClassDeclaration(
        modifiers = convertModifiers(v.modifierList),
        classDeclarationKeyword = v.getDeclarationKeyword()?.let(::convertKeyword)
            ?: error("declarationKeyword not found"),
        name = v.nameIdentifier?.let(::convertNameExpression),
        lAngle = v.typeParameterList?.leftAngle?.let(::convertKeyword),
        typeParams = convertTypeParams(v.typeParameterList),
        rAngle = v.typeParameterList?.rightAngle?.let(::convertKeyword),
        primaryConstructor = v.primaryConstructor?.let(::convertPrimaryConstructor),
        classParents = convertClassParents(v.getSuperTypeList()),
        typeConstraintSet = v.typeConstraintList?.let { typeConstraintList ->
            Node.PostModifier.TypeConstraintSet(
                whereKeyword = convertKeyword(v.whereKeyword),
                constraints = convertTypeConstraints(typeConstraintList),
            ).mapNotCorrespondsPsiElement(v)
        },
        classBody = v.body?.let(::convertClassBody),
    ).map(v)

    open fun convertClassParents(v: KtSuperTypeList?): List<Node.Declaration.ClassDeclaration.ClassParent> =
        v?.entries.orEmpty().map(::convertClassParent)

    open fun convertClassParent(v: KtSuperTypeListEntry) = when (v) {
        is KtSuperTypeCallEntry -> convertConstructorClassParent(v)
        is KtDelegatedSuperTypeEntry -> convertDelegationClassParent(v)
        is KtSuperTypeEntry -> convertTypeClassParent(v)
        else -> error("Unknown super type entry $v")
    }

    open fun convertConstructorClassParent(v: KtSuperTypeCallEntry) =
        Node.Declaration.ClassDeclaration.ConstructorClassParent(
            type = v.typeReference?.let(::convertType) as? Node.Type.SimpleType
                ?: error("Bad type on super call $v"),
            lPar = v.valueArgumentList?.leftParenthesis?.let(::convertKeyword)
                ?: error("No left parenthesis for $v"),
            args = convertValueArgs(v.valueArgumentList) ?: error("No value arguments for $v"),
            rPar = v.valueArgumentList?.rightParenthesis?.let(::convertKeyword)
                ?: error("No right parenthesis for $v"),
        ).map(v)

    open fun convertDelegationClassParent(v: KtDelegatedSuperTypeEntry) =
        Node.Declaration.ClassDeclaration.DelegationClassParent(
            type = v.typeReference?.let(::convertType)
                ?: error("No type on delegated super type $v"),
            byKeyword = convertKeyword(v.byKeywordNode.psi),
            expression = convertExpression(v.delegateExpression ?: error("Missing delegateExpression for $v")),
        ).map(v)

    open fun convertTypeClassParent(v: KtSuperTypeEntry) = Node.Declaration.ClassDeclaration.TypeClassParent(
        type = v.typeReference?.let(::convertType)
            ?: error("No type on super type $v"),
    ).map(v)

    open fun convertPrimaryConstructor(v: KtPrimaryConstructor) = Node.Declaration.ClassDeclaration.PrimaryConstructor(
        modifiers = convertModifiers(v.modifierList),
        constructorKeyword = v.getConstructorKeyword()?.let(::convertKeyword),
        lPar = v.valueParameterList?.leftParenthesis?.let(::convertKeyword),
        params = convertFuncParams(v.valueParameterList),
        rPar = v.valueParameterList?.rightParenthesis?.let(::convertKeyword),
    ).map(v)

    open fun convertClassBody(v: KtClassBody): Node.Declaration.ClassDeclaration.ClassBody {
        val ktEnumEntries = v.declarations.filterIsInstance<KtEnumEntry>()
        val declarationsExcludingKtEnumEntry = v.declarations.filter { it !is KtEnumEntry }
        return Node.Declaration.ClassDeclaration.ClassBody(
            lBrace = convertKeyword(v.lBrace ?: error("Missing lBrace for $v")),
            enumEntries = ktEnumEntries.map(::convertEnumEntry),
            declarations = declarationsExcludingKtEnumEntry.map(::convertDeclaration),
            rBrace = convertKeyword(v.rBrace ?: error("Missing rBrace for $v")),
        ).map(v)
    }

    open fun convertEnumEntry(v: KtEnumEntry): Node.Declaration.ClassDeclaration.ClassBody.EnumEntry =
        Node.Declaration.ClassDeclaration.ClassBody.EnumEntry(
            modifiers = convertModifiers(v.modifierList),
            name = v.nameIdentifier?.let(::convertNameExpression) ?: error("Unnamed enum"),
            lPar = v.initializerList?.valueArgumentList?.leftParenthesis?.let(::convertKeyword),
            args = convertValueArgs(v.initializerList?.valueArgumentList),
            rPar = v.initializerList?.valueArgumentList?.rightParenthesis?.let(::convertKeyword),
            classBody = v.body?.let(::convertClassBody),
        ).map(v)

    open fun convertInitializer(v: KtAnonymousInitializer) = Node.Declaration.ClassDeclaration.ClassBody.Initializer(
        modifiers = convertModifiers(v.modifierList),
        block = convertBlockExpression(v.body as? KtBlockExpression ?: error("No init block for $v")),
    ).map(v)

    open fun convertSecondaryConstructor(v: KtSecondaryConstructor) =
        Node.Declaration.ClassDeclaration.ClassBody.SecondaryConstructor(
            modifiers = convertModifiers(v.modifierList),
            constructorKeyword = convertKeyword(v.getConstructorKeyword()),
            lPar = v.valueParameterList?.leftParenthesis?.let(::convertKeyword),
            params = convertFuncParams(v.valueParameterList),
            rPar = v.valueParameterList?.rightParenthesis?.let(::convertKeyword),
            delegationCall = if (v.hasImplicitDelegationCall()) null else convertCallExpression(
                v.getDelegationCall()
            ),
            block = v.bodyExpression?.let(::convertBlockExpression)
        ).map(v)

    open fun convertFunctionDeclaration(v: KtNamedFunction): Node.Declaration.FunctionDeclaration {
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
            modifiers = convertModifiers(v.modifierList),
            funKeyword = v.funKeyword?.let { convertKeyword(it) } ?: error("No fun keyword for $v"),
            lAngle = v.typeParameterList?.leftAngle?.let(::convertKeyword),
            typeParams = convertTypeParams(v.typeParameterList),
            rAngle = v.typeParameterList?.rightAngle?.let(::convertKeyword),
            receiverType = v.receiverTypeReference?.let(::convertType),
            name = v.nameIdentifier?.let(::convertNameExpression),
            lPar = v.valueParameterList?.leftParenthesis?.let(::convertKeyword),
            params = convertFuncParams(v.valueParameterList),
            rPar = v.valueParameterList?.rightParenthesis?.let(::convertKeyword),
            returnType = v.typeReference?.let(::convertType),
            postModifiers = convertPostModifiers(v),
            equals = v.equalsToken?.let(::convertKeyword),
            body = v.bodyExpression?.let { convertExpression(it) },
        ).map(v)
    }

    open fun convertPropertyDeclaration(v: KtProperty) = Node.Declaration.PropertyDeclaration(
        modifiers = convertModifiers(v.modifierList),
        valOrVarKeyword = convertKeyword(v.valOrVarKeyword),
        lAngle = v.typeParameterList?.leftAngle?.let(::convertKeyword),
        typeParams = convertTypeParams(v.typeParameterList),
        rAngle = v.typeParameterList?.rightAngle?.let(::convertKeyword),
        receiverType = v.receiverTypeReference?.let(::convertType),
        lPar = null,
        variables = listOf(
            Node.Variable(
                modifiers = listOf(),
                name = v.nameIdentifier?.let(::convertNameExpression) ?: error("No property name on $v"),
                type = v.typeReference?.let(::convertType)
            ).mapNotCorrespondsPsiElement(v)
        ),
        rPar = null,
        typeConstraintSet = v.typeConstraintList?.let { typeConstraintList ->
            Node.PostModifier.TypeConstraintSet(
                whereKeyword = convertKeyword(v.whereKeyword),
                constraints = convertTypeConstraints(typeConstraintList),
            ).mapNotCorrespondsPsiElement(v)
        },
        equals = v.equalsToken?.let(::convertKeyword),
        initializer = v.initializer?.let(this::convertExpression),
        propertyDelegate = v.delegate?.let(::convertPropertyDelegate),
        accessors = v.accessors.map(::convertPropertyAccessor),
    ).map(v)

    open fun convertPropertyDeclaration(v: KtDestructuringDeclaration) = Node.Declaration.PropertyDeclaration(
        modifiers = convertModifiers(v.modifierList),
        valOrVarKeyword = v.valOrVarKeyword?.let(::convertKeyword) ?: error("Missing valOrVarKeyword"),
        lAngle = null,
        typeParams = listOf(),
        rAngle = null,
        receiverType = null,
        lPar = v.lPar?.let(::convertKeyword),
        variables = v.entries.map(::convertVariable),
        rPar = v.rPar?.let(::convertKeyword),
        typeConstraintSet = null,
        equals = convertKeyword(v.equalsToken),
        initializer = v.initializer?.let(this::convertExpression),
        propertyDelegate = null,
        accessors = listOf(),
    ).map(v)

    open fun convertPropertyDelegate(v: KtPropertyDelegate) = Node.Declaration.PropertyDeclaration.PropertyDelegate(
        byKeyword = convertKeyword(v.byKeyword),
        expression = convertExpression(v.expression ?: error("Missing expression for $v")),
    ).map(v)

    open fun convertPropertyAccessor(v: KtPropertyAccessor): Node.Declaration.PropertyDeclaration.Accessor =
        when (v.isGetter) {
            true -> convertGetter(v)
            false -> convertSetter(v)
        }

    open fun convertGetter(v: KtPropertyAccessor) = Node.Declaration.PropertyDeclaration.Getter(
        modifiers = convertModifiers(v.modifierList),
        getKeyword = convertKeyword(v.getKeyword),
        type = v.returnTypeReference?.let(::convertType),
        postModifiers = convertPostModifiers(v),
        equals = v.equalsToken?.let(::convertKeyword),
        body = v.bodyExpression?.let(::convertExpression),
    ).map(v)

    open fun convertSetter(v: KtPropertyAccessor) = Node.Declaration.PropertyDeclaration.Setter(
        modifiers = convertModifiers(v.modifierList),
        setKeyword = convertKeyword(v.setKeyword),
        params = convertLambdaParams(v.parameterList),
        postModifiers = convertPostModifiers(v),
        equals = v.equalsToken?.let(::convertKeyword),
        body = v.bodyExpression?.let(::convertExpression),
    ).map(v)

    open fun convertTypeAliasDeclaration(v: KtTypeAlias) = Node.Declaration.TypeAliasDeclaration(
        modifiers = convertModifiers(v.modifierList),
        name = v.nameIdentifier?.let(::convertNameExpression) ?: error("No type alias name for $v"),
        lAngle = v.typeParameterList?.leftAngle?.let(::convertKeyword),
        typeParams = convertTypeParams(v.typeParameterList),
        rAngle = v.typeParameterList?.rightAngle?.let(::convertKeyword),
        equals = convertKeyword(v.equalsToken),
        type = convertType(v.getTypeReference() ?: error("No type alias ref for $v"))
    ).map(v)

    open fun convertFuncParams(v: KtParameterList?): List<Node.FunctionParam> =
        v?.parameters.orEmpty().map(::convertFuncParam)

    open fun convertFuncParam(v: KtParameter) = Node.FunctionParam(
        modifiers = convertModifiers(v.modifierList),
        valOrVarKeyword = v.valOrVarKeyword?.let(::convertKeyword),
        name = v.nameIdentifier?.let(::convertNameExpression) ?: error("No param name"),
        type = v.typeReference?.let(::convertType),
        equals = v.equalsToken?.let(::convertKeyword),
        defaultValue = v.defaultValue?.let(this::convertExpression),
    ).map(v)

    open fun convertVariable(v: KtDestructuringDeclarationEntry) = Node.Variable(
        modifiers = convertModifiers(v.modifierList),
        name = v.nameIdentifier?.let(::convertNameExpression) ?: error("No property name on $v"),
        type = v.typeReference?.let(::convertType)
    ).map(v)

    open fun convertTypeParams(v: KtTypeParameterList?): List<Node.TypeParam> =
        v?.parameters.orEmpty().map(::convertTypeParam)

    open fun convertTypeParam(v: KtTypeParameter) = Node.TypeParam(
        modifiers = convertModifiers(v.modifierList),
        name = v.nameIdentifier?.let(::convertNameExpression) ?: error("No type param name for $v"),
        type = v.extendsBound?.let(::convertType)
    ).map(v)

    open fun convertType(v: KtTypeReference): Node.Type {
        return convertType(v, v.nonExtraChildren())
    }

    protected fun convertType(v: KtElement, targetChildren: List<PsiElement>): Node.Type {
        require(v is KtTypeReference || v is KtNullableType) { "Unexpected type: $v" }

        val modifierListElements = targetChildren.takeWhile { it is KtModifierList }
        check(modifierListElements.size <= 1) { "Multiple modifier lists in type children: $targetChildren" }
        val modifierList = modifierListElements.firstOrNull() as? KtModifierList
        val questionMarks = targetChildren.takeLastWhile { it.node.elementType == KtTokens.QUEST }
        val restChildren =
            targetChildren.subList(modifierListElements.size, targetChildren.size - questionMarks.size)

        // questionMarks can be ignored here because when v is KtNullableType, it will be handled in caller side.
        if (restChildren.first().node.elementType == KtTokens.LPAR && restChildren.last().node.elementType == KtTokens.RPAR) {
            return Node.Type.ParenthesizedType(
                modifiers = convertModifiers(modifierList),
                lPar = convertKeyword(restChildren.first()),
                innerType = convertType(v, restChildren.subList(1, restChildren.size - 1)),
                rPar = convertKeyword(restChildren.last()),
            ).mapNotCorrespondsPsiElement(v)
        }

        return when (val typeEl = restChildren.first()) {
            is KtFunctionType -> convertFunctionType(modifierList, typeEl)
            is KtUserType -> convertSimpleType(modifierList, typeEl)
            is KtNullableType -> convertNullableType(modifierList, typeEl)
            is KtDynamicType -> convertDynamicType(modifierList, typeEl)
            else -> error("Unrecognized type of $typeEl")
        }
    }

    protected fun convertNullableType(modifierList: KtModifierList?, v: KtNullableType) = Node.Type.NullableType(
        modifiers = convertModifiers(modifierList),
        innerType = convertType(v, v.nonExtraChildren()),
        questionMark = convertKeyword(
            findChildByType(v, KtTokens.QUEST) ?: error("No question mark for $v")
        ),
    ).mapNotCorrespondsPsiElement(v)

    protected fun convertSimpleType(modifierList: KtModifierList?, v: KtUserType) = Node.Type.SimpleType(
        modifiers = convertModifiers(modifierList),
        qualifiers = generateSequence(v.qualifier) { it.qualifier }.toList().reversed()
            .map(::convertTypeSimpleQualifier),
        name = convertNameExpression(v.referenceExpression ?: error("No type name for $v")),
        lAngle = v.typeArgumentList?.leftAngle?.let(::convertKeyword),
        typeArgs = convertTypeArgs(v.typeArgumentList),
        rAngle = v.typeArgumentList?.rightAngle?.let(::convertKeyword),
    ).mapNotCorrespondsPsiElement(v)

    open fun convertTypeSimpleQualifier(v: KtUserType) = Node.Type.SimpleType.SimpleTypeQualifier(
        name = convertNameExpression(v.referenceExpression ?: error("No type name for $v")),
        lAngle = v.typeArgumentList?.leftAngle?.let(::convertKeyword),
        typeArgs = convertTypeArgs(v.typeArgumentList),
        rAngle = v.typeArgumentList?.rightAngle?.let(::convertKeyword),
    ).mapNotCorrespondsPsiElement(v) // Don't map v because v necessarily corresponds to a single name expression.


    protected fun convertDynamicType(modifierList: KtModifierList?, v: KtDynamicType) = Node.Type.DynamicType(
        modifiers = convertModifiers(modifierList),
        dynamicKeyword = convertKeyword(v.dynamicKeyword),
    ).mapNotCorrespondsPsiElement(v)

    protected fun convertFunctionType(modifierList: KtModifierList?, v: KtFunctionType) = Node.Type.FunctionType(
        modifiers = convertModifiers(modifierList),
        contextReceiver = v.contextReceiverList?.let(::convertContextReceiver),
        receiverType = v.receiver?.typeReference?.let(::convertType),
        dotSymbol = findChildByType(v, KtTokens.DOT)?.let(::convertKeyword),
        lPar = v.parameterList?.leftParenthesis?.let(::convertKeyword),
        params = convertTypeFunctionParams(v.parameterList),
        rPar = v.parameterList?.rightParenthesis?.let(::convertKeyword),
        returnType = convertType(v.returnTypeReference ?: error("No return type for $v")),
    ).mapNotCorrespondsPsiElement(v)

    open fun convertTypeFunctionParams(v: KtParameterList?): List<Node.Type.FunctionType.FunctionTypeParam> =
        v?.parameters.orEmpty().map(::convertTypeFunctionParam)

    open fun convertTypeFunctionParam(v: KtParameter) = Node.Type.FunctionType.FunctionTypeParam(
        name = v.nameIdentifier?.let(::convertNameExpression),
        type = convertType(v.typeReference ?: error("No param type"))
    ).map(v)

    open fun convertTypeArgs(v: KtTypeArgumentList?): List<Node.TypeArg> = v?.arguments.orEmpty().map(::convertTypeArg)

    open fun convertTypeArg(v: KtTypeProjection): Node.TypeArg = when (v.projectionKind) {
        KtProjectionKind.STAR -> convertStarProjection(v)
        else -> convertTypeProjection(v)
    }

    open fun convertStarProjection(v: KtTypeProjection) = Node.TypeArg.StarProjection(
        asterisk = convertKeyword(v.projectionToken ?: error("Missing projection token for $v")),
    ).map(v)

    open fun convertTypeProjection(v: KtTypeProjection) = Node.TypeArg.TypeProjection(
        modifiers = convertModifiers(v.modifierList),
        type = convertType(v.typeReference ?: error("Missing type ref for $v")),
    ).map(v)

    open fun convertValueArgs(v: KtValueArgumentList?): List<Node.ValueArg> =
        v?.arguments.orEmpty().map(::convertValueArg)

    open fun convertValueArg(v: KtValueArgument) = Node.ValueArg(
        name = v.getArgumentName()?.let(::convertValueArgName),
        asterisk = v.getSpreadElement()?.let(::convertKeyword),
        expression = convertExpression(v.getArgumentExpression() ?: error("No expr for value arg"))
    ).map(v)

    open fun convertExpression(v: KtExpression): Node.Expression = when (v) {
        is KtIfExpression -> convertIfExpression(v)
        is KtTryExpression -> convertTryExpression(v)
        is KtBinaryExpression -> convertBinaryExpression(v)
        is KtQualifiedExpression -> convertBinaryExpression(v)
        is KtPrefixExpression -> convertPrefixUnaryExpression(v)
        is KtPostfixExpression -> convertPostfixUnaryExpression(v)
        is KtBinaryExpressionWithTypeRHS -> convertBinaryTypeExpression(v)
        is KtIsExpression -> convertBinaryTypeExpression(v)
        is KtCallableReferenceExpression -> convertCallableReferenceExpression(v)
        is KtClassLiteralExpression -> convertClassLiteralExpression(v)
        is KtParenthesizedExpression -> convertParenthesizedExpression(v)
        is KtStringTemplateExpression -> convertStringLiteralExpression(v)
        is KtConstantExpression -> convertConstantLiteralExpression(v)
        is KtBlockExpression -> convertBlockExpression(v)
        is KtFunctionLiteral -> error("Supposed to be unreachable here. KtFunctionLiteral is expected to be inside of KtLambdaExpression.")
        is KtLambdaExpression -> convertLambdaExpression(v)
        is KtThisExpression -> convertThisExpression(v)
        is KtSuperExpression -> convertSuperExpression(v)
        is KtWhenExpression -> convertWhenExpression(v)
        is KtObjectLiteralExpression -> convertObjectLiteralExpression(v)
        is KtThrowExpression -> convertThrowExpression(v)
        is KtReturnExpression -> convertReturnExpression(v)
        is KtContinueExpression -> convertContinueExpression(v)
        is KtBreakExpression -> convertBreakExpression(v)
        is KtCollectionLiteralExpression -> convertCollectionLiteralExpression(v)
        is KtSimpleNameExpression -> convertNameExpression(v)
        is KtConstructorDelegationReferenceExpression -> convertThisOrSuperExpression(v)
        is KtLabeledExpression -> convertLabeledExpression(v)
        is KtAnnotatedExpression -> convertAnnotatedExpression(v)
        is KtCallExpression -> convertCallExpression(v)
        is KtConstructorCalleeExpression -> error("Supposed to be unreachable here. KtConstructorCalleeExpression is expected to be inside of KtSuperTypeCallEntry or KtAnnotationEntry.")
        is KtArrayAccessExpression -> convertIndexedAccessExpression(v)
        is KtNamedFunction -> convertAnonymousFunctionExpression(v)
        is KtProperty -> convertPropertyExpression(v)
        is KtDestructuringDeclaration -> convertPropertyExpression(v)
        // TODO: this is present in a recovery test where an interface decl is on rhs of a gt expr
        is KtClass -> throw Unsupported("Class expressions not supported")
        else -> error("Unrecognized expression type from $v")
    }

    open fun convertIfExpression(v: KtIfExpression) = Node.Expression.IfExpression(
        ifKeyword = convertKeyword(v.ifKeyword),
        lPar = convertKeyword(v.leftParenthesis ?: error("No left parenthesis on if for $v")),
        condition = convertExpression(v.condition ?: error("No cond on if for $v")),
        rPar = convertKeyword(v.rightParenthesis ?: error("No right parenthesis on if for $v")),
        body = convertExpression(v.thenContainer.expression ?: error("No then body on if for $v")),
        elseKeyword = v.elseKeyword?.let(::convertKeyword),
        elseBody = v.elseContainer?.expression?.let(this::convertExpression),
    ).map(v)

    open fun convertTryExpression(v: KtTryExpression) = Node.Expression.TryExpression(
        block = convertBlockExpression(v.tryBlock),
        catchClauses = v.catchClauses.map(::convertCatchClause),
        finallyBlock = v.finallyBlock?.finalExpression?.let(::convertBlockExpression)
    ).map(v)

    open fun convertCatchClause(v: KtCatchClause) = Node.Expression.TryExpression.CatchClause(
        catchKeyword = convertKeyword(v.catchKeyword),
        lPar = convertKeyword(v.parameterList?.leftParenthesis ?: error("No catch lpar for $v")),
        params = convertFuncParams(v.parameterList ?: error("No catch params for $v")),
        rPar = convertKeyword(v.parameterList?.rightParenthesis ?: error("No catch rpar for $v")),
        block = convertBlockExpression(v.catchBody as? KtBlockExpression ?: error("No catch block for $v")),
    ).map(v)

    open fun convertWhenExpression(v: KtWhenExpression) = Node.Expression.WhenExpression(
        whenKeyword = convertKeyword(v.whenKeyword),
        lPar = v.leftParenthesis?.let(::convertKeyword),
        expression = v.subjectExpression?.let(this::convertExpression),
        rPar = v.rightParenthesis?.let(::convertKeyword),
        whenBranches = v.entries.map(::convertWhenBranch),
    ).map(v)

    open fun convertWhenBranch(v: KtWhenEntry): Node.Expression.WhenExpression.WhenBranch {
        val elseKeyword = v.elseKeyword
        val body = convertExpression(v.expression ?: error("No when entry body for $v"))
        return if (elseKeyword == null) {
            Node.Expression.WhenExpression.ConditionalWhenBranch(
                whenConditions = v.conditions.map(::convertWhenCondition),
                body = body,
            ).map(v)
        } else {
            Node.Expression.WhenExpression.ElseWhenBranch(
                elseKeyword = convertKeyword(elseKeyword),
                body = body,
            ).map(v)
        }
    }

    open fun convertWhenCondition(v: KtWhenCondition): Node.Expression.WhenExpression.WhenCondition = when (v) {
        is KtWhenConditionWithExpression -> convertExpressionWhenCondition(v)
        is KtWhenConditionInRange -> convertRangeWhenCondition(v)
        is KtWhenConditionIsPattern -> convertTypeWhenCondition(v)
        else -> error("Unrecognized when cond of $v")
    }

    open fun convertExpressionWhenCondition(v: KtWhenConditionWithExpression) =
        Node.Expression.WhenExpression.ExpressionWhenCondition(
            expression = convertExpression(v.expression ?: error("No when cond expr for $v")),
        ).map(v)

    open fun convertRangeWhenCondition(v: KtWhenConditionInRange) = Node.Expression.WhenExpression.RangeWhenCondition(
        operator = convertKeyword(v.operationReference),
        expression = convertExpression(v.rangeExpression ?: error("No when in expr for $v")),
    ).map(v)

    open fun convertTypeWhenCondition(v: KtWhenConditionIsPattern) = Node.Expression.WhenExpression.TypeWhenCondition(
        operator = convertKeyword(
            findChildByType(v, KtTokens.IS_KEYWORD)
                ?: findChildByType(v, KtTokens.NOT_IS)
                ?: error("No when is operator for $v")
        ),
        type = convertType(v.typeReference ?: error("No when is type for $v")),
    ).map(v)

    open fun convertBinaryExpression(v: KtBinaryExpression) = Node.Expression.BinaryExpression(
        lhs = convertExpression(v.left ?: error("No binary lhs for $v")),
        operator = if (v.operationReference.isConventionOperator()) {
            convertKeyword(v.operationReference)
        } else {
            convertNameExpression(v.operationReference.firstChild)
        },
        rhs = convertExpression(v.right ?: error("No binary rhs for $v"))
    ).map(v)

    open fun convertBinaryExpression(v: KtQualifiedExpression) = Node.Expression.BinaryExpression(
        lhs = convertExpression(v.receiverExpression),
        operator = convertKeyword(v.operationTokenNode.psi),
        rhs = convertExpression(v.selectorExpression ?: error("No qualified rhs for $v"))
    ).map(v)

    open fun convertPrefixUnaryExpression(v: KtPrefixExpression) = Node.Expression.PrefixUnaryExpression(
        operator = convertKeyword(v.operationReference),
        expression = convertExpression(v.baseExpression ?: error("No base expression for $v")),
    ).map(v)

    open fun convertPostfixUnaryExpression(v: KtPostfixExpression) = Node.Expression.PostfixUnaryExpression(
        expression = convertExpression(v.baseExpression ?: error("No base expression for $v")),
        operator = convertKeyword(v.operationReference),
    ).map(v)

    open fun convertBinaryTypeExpression(v: KtBinaryExpressionWithTypeRHS) = Node.Expression.BinaryTypeExpression(
        lhs = convertExpression(v.left),
        operator = convertKeyword(v.operationReference),
        rhs = convertType(v.right ?: error("No type op rhs for $v"))
    ).map(v)

    open fun convertBinaryTypeExpression(v: KtIsExpression) = Node.Expression.BinaryTypeExpression(
        lhs = convertExpression(v.leftHandSide),
        operator = convertKeyword(v.operationReference),
        rhs = convertType(v.typeReference ?: error("No type op rhs for $v"))
    ).map(v)

    open fun convertCallableReferenceExpression(v: KtCallableReferenceExpression) =
        Node.Expression.CallableReferenceExpression(
            lhs = v.receiverExpression?.let(this::convertExpression),
            questionMarks = v.questionMarks.map(::convertKeyword),
            rhs = convertNameExpression(v.callableReference)
        ).map(v)

    open fun convertClassLiteralExpression(v: KtClassLiteralExpression) = Node.Expression.ClassLiteralExpression(
        lhs = v.receiverExpression?.let(this::convertExpression),
        questionMarks = v.questionMarks.map(::convertKeyword),
    ).map(v)

    open fun convertParenthesizedExpression(v: KtParenthesizedExpression) = Node.Expression.ParenthesizedExpression(
        innerExpression = convertExpression(v.expression ?: error("No expression for $v"))
    ).map(v)

    open fun convertStringLiteralExpression(v: KtStringTemplateExpression) = Node.Expression.StringLiteralExpression(
        entries = v.entries.map(::convertStringEntry),
        raw = v.text.startsWith("\"\"\"")
    ).map(v)

    open fun convertStringEntry(v: KtStringTemplateEntry): Node.Expression.StringLiteralExpression.StringEntry =
        when (v) {
            is KtLiteralStringTemplateEntry -> convertLiteralStringEntry(v)
            is KtEscapeStringTemplateEntry -> convertEscapeStringEntry(v)
            is KtStringTemplateEntryWithExpression -> convertTemplateStringEntry(v)
            else -> error("Unrecognized string template type for $v")
        }

    open fun convertLiteralStringEntry(v: KtLiteralStringTemplateEntry) =
        Node.Expression.StringLiteralExpression.LiteralStringEntry(
            text = v.text,
        ).map(v)

    open fun convertEscapeStringEntry(v: KtEscapeStringTemplateEntry) =
        Node.Expression.StringLiteralExpression.EscapeStringEntry(
            text = v.text,
        ).map(v)

    open fun convertTemplateStringEntry(v: KtStringTemplateEntryWithExpression) =
        Node.Expression.StringLiteralExpression.TemplateStringEntry(
            expression = convertExpression(v.expression ?: error("No expr tmpl")),
            short = v is KtSimpleNameStringTemplateEntry,
        ).map(v)

    open fun convertConstantLiteralExpression(v: KtConstantExpression): Node.Expression.ConstantLiteralExpression =
        when (v.node.elementType) {
            KtNodeTypes.BOOLEAN_CONSTANT -> Node.Expression.BooleanLiteralExpression(v.text)
            KtNodeTypes.CHARACTER_CONSTANT -> Node.Expression.CharacterLiteralExpression(v.text)
            KtNodeTypes.INTEGER_CONSTANT -> Node.Expression.IntegerLiteralExpression(v.text)
            KtNodeTypes.FLOAT_CONSTANT -> Node.Expression.RealLiteralExpression(v.text)
            KtNodeTypes.NULL -> Node.Expression.NullLiteralExpression()
            else -> error("Unrecognized constant type for $v")
        }.map(v)

    open fun convertLambdaExpression(v: KtLambdaExpression) = Node.Expression.LambdaExpression(
        lBrace = convertKeyword(v.lBrace),
        params = convertLambdaParams(v.functionLiteral.valueParameterList),
        arrow = v.functionLiteral.arrow?.let(::convertKeyword),
        lambdaBody = v.bodyExpression?.let(::convertLambdaBody),
        rBrace = convertKeyword(v.rBrace),
    ).map(v)

    open fun convertLambdaParams(v: KtParameterList?): List<Node.LambdaParam> =
        v?.parameters.orEmpty().map(::convertLambdaParam)

    open fun convertLambdaParam(v: KtParameter): Node.LambdaParam {
        val destructuringDeclaration = v.destructuringDeclaration
        return if (destructuringDeclaration != null) {
            Node.LambdaParam(
                lPar = destructuringDeclaration.lPar?.let(::convertKeyword),
                variables = destructuringDeclaration.entries.map(::convertVariable),
                rPar = destructuringDeclaration.rPar?.let(::convertKeyword),
                colon = v.colon?.let(::convertKeyword),
                destructType = v.typeReference?.let(::convertType),
            ).map(v)
        } else {
            Node.LambdaParam(
                lPar = null,
                variables = listOf(
                    Node.Variable(
                        modifiers = convertModifiers(v.modifierList),
                        name = v.nameIdentifier?.let(::convertNameExpression) ?: error("No lambda param name on $v"),
                        type = v.typeReference?.let(::convertType),
                    ).mapNotCorrespondsPsiElement(v)
                ),
                rPar = null,
                colon = null,
                destructType = null,
            ).map(v)
        }
    }

    open fun convertLambdaBody(v: KtBlockExpression) = Node.Expression.LambdaExpression.LambdaBody(
        statements = v.statements.map(::convertStatement),
    ).map(v)

    open fun convertThisExpression(v: KtThisExpression) = Node.Expression.ThisExpression(
        label = v.getTargetLabel()?.let(::convertNameExpression),
    ).map(v)

    open fun convertSuperExpression(v: KtSuperExpression) = Node.Expression.SuperExpression(
        typeArgType = v.superTypeQualifier?.let(::convertType),
        label = v.getTargetLabel()?.let(::convertNameExpression),
    ).map(v)

    open fun convertObjectLiteralExpression(v: KtObjectLiteralExpression) = Node.Expression.ObjectLiteralExpression(
        declaration = convertClassDeclaration(v.objectDeclaration),
    ).map(v)

    open fun convertThrowExpression(v: KtThrowExpression) = Node.Expression.ThrowExpression(
        expression = convertExpression(v.thrownExpression ?: error("No throw expr for $v"))
    ).map(v)

    open fun convertReturnExpression(v: KtReturnExpression) = Node.Expression.ReturnExpression(
        label = v.getTargetLabel()?.let(::convertNameExpression),
        expression = v.returnedExpression?.let(this::convertExpression)
    ).map(v)

    open fun convertContinueExpression(v: KtContinueExpression) = Node.Expression.ContinueExpression(
        label = v.getTargetLabel()?.let(::convertNameExpression),
    ).map(v)

    open fun convertBreakExpression(v: KtBreakExpression) = Node.Expression.BreakExpression(
        label = v.getTargetLabel()?.let(::convertNameExpression),
    ).map(v)

    open fun convertCollectionLiteralExpression(v: KtCollectionLiteralExpression) =
        Node.Expression.CollectionLiteralExpression(
            expressions = v.getInnerExpressions().map(this::convertExpression),
        ).map(v)

    open fun convertValueArgName(v: KtValueArgumentName) = Node.Expression.NameExpression(
        text = (v.referenceExpression.getIdentifier() ?: error("No identifier for $v")).text,
    ).map(v)

    open fun convertNameExpression(v: KtSimpleNameExpression) = Node.Expression.NameExpression(
        text = (v.getIdentifier() ?: error("No identifier for $v")).text,
    ).map(v)

    open fun convertNameExpression(v: PsiElement) = Node.Expression.NameExpression(
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

    open fun convertLabeledExpression(v: KtLabeledExpression) = Node.Expression.LabeledExpression(
        label = convertNameExpression(v.getTargetLabel() ?: error("No label name for $v")),
        statement = convertStatement(v.baseExpression ?: error("No label expr for $v"))
    ).map(v)

    open fun convertAnnotatedExpression(v: KtAnnotatedExpression) = Node.Expression.AnnotatedExpression(
        annotationSets = convertAnnotationSets(v),
        statement = convertStatement(v.baseExpression ?: error("No annotated expr for $v"))
    ).map(v)

    open fun convertCallExpression(v: KtCallElement) = Node.Expression.CallExpression(
        calleeExpression = convertExpression(v.calleeExpression ?: error("No call expr for $v")),
        lAngle = v.typeArgumentList?.leftAngle?.let(::convertKeyword),
        typeArgs = convertTypeArgs(v.typeArgumentList),
        rAngle = v.typeArgumentList?.rightAngle?.let(::convertKeyword),
        lPar = v.valueArgumentList?.leftParenthesis?.let(::convertKeyword),
        args = convertValueArgs(v.valueArgumentList),
        rPar = v.valueArgumentList?.rightParenthesis?.let(::convertKeyword),
        lambdaArg = v.lambdaArguments.also {
            if (it.size >= 2) {
                // According to the Kotlin syntax, at most one lambda argument is allowed.
                // However, Kotlin compiler can parse multiple lambda arguments.
                throw Unsupported("At most one lambda argument is allowed")
            }
        }.firstOrNull()?.let(::convertLambdaArg)
    ).map(v)

    open fun convertLambdaArg(v: KtLambdaArgument): Node.Expression.CallExpression.LambdaArg {
        var label: Node.Expression.NameExpression? = null
        var annotationSets: List<Node.Modifier.AnnotationSet> = emptyList()
        fun KtExpression.extractLambda(): KtLambdaExpression? = when (this) {
            is KtLambdaExpression -> this
            is KtLabeledExpression -> baseExpression?.extractLambda().also {
                label = convertNameExpression(getTargetLabel() ?: error("No label for $this"))
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
            expression = convertLambdaExpression(expr)
        ).map(v)
    }

    open fun convertIndexedAccessExpression(v: KtArrayAccessExpression) = Node.Expression.IndexedAccessExpression(
        expression = convertExpression(v.arrayExpression ?: error("No array expr for $v")),
        indices = v.indexExpressions.map(this::convertExpression),
    ).map(v)

    open fun convertAnonymousFunctionExpression(v: KtNamedFunction) = Node.Expression.AnonymousFunctionExpression(
        function = convertFunctionDeclaration(v),
    ).mapNotCorrespondsPsiElement(v)

    open fun convertPropertyExpression(v: KtProperty) = Node.Expression.PropertyExpression(
        property = convertPropertyDeclaration(v)
    ).map(v)

    open fun convertPropertyExpression(v: KtDestructuringDeclaration) = Node.Expression.PropertyExpression(
        property = convertPropertyDeclaration(v)
    ).map(v)

    open fun convertBlockExpression(v: KtBlockExpression) = Node.Expression.BlockExpression(
        lBrace = convertKeyword(v.lBrace ?: error("No left brace for $v")),
        statements = v.statements.map(::convertStatement),
        rBrace = convertKeyword(v.rBrace ?: error("No right brace for $v")),
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
        lPar = v.valueArgumentList?.leftParenthesis?.let(::convertKeyword),
        args = convertValueArgs(v.valueArgumentList),
        rPar = v.valueArgumentList?.rightParenthesis?.let(::convertKeyword),
    )

    open fun convertModifiers(v: KtModifierList?): List<Node.Modifier> {
        if (v == null) {
            return listOf()
        }
        val nonExtraChildren = v.allChildren.filterNot { it is PsiComment || it is PsiWhiteSpace }.toList()
        return nonExtraChildren.mapNotNull { psi ->
            // We go over the node children because we want to preserve order
            when (psi) {
                is KtAnnotationEntry -> convertAnnotationSet(psi)
                is KtAnnotation -> convertAnnotationSet(psi)
                is PsiWhiteSpace -> null
                else -> convertKeyword<Node.Modifier.KeywordModifier>(psi)
            }
        }.toList()
    }

    open fun convertContextReceiver(v: KtContextReceiverList) = Node.ContextReceiver(
        lPar = convertKeyword(v.leftParenthesis),
        receiverTypes = v.contextReceivers().map { convertType(it.typeReference() ?: error("No type ref for $it")) },
        rPar = convertKeyword(v.rightParenthesis),
    ).mapNotCorrespondsPsiElement(v)

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
                    lBracket = convertKeyword(psi.leftBracket),
                    contractEffects = convertContractEffects(psi),
                    rBracket = convertKeyword(psi.rightBracket),
                ).mapNotCorrespondsPsiElement(v)
                else -> null
            }.also { prevPsi = psi }
        }
    }

    open fun convertTypeConstraints(v: KtTypeConstraintList): List<Node.PostModifier.TypeConstraintSet.TypeConstraint> =
        v.constraints.map(::convertTypeConstraint)

    open fun convertTypeConstraint(v: KtTypeConstraint) = Node.PostModifier.TypeConstraintSet.TypeConstraint(
        annotationSets = v.children.mapNotNull {
            when (it) {
                is KtAnnotationEntry -> convertAnnotationSet(it)
                is KtAnnotation -> convertAnnotationSet(it)
                else -> null
            }
        },
        name = v.subjectTypeParameterName?.let { convertNameExpression(it) } ?: error("No type constraint name for $v"),
        type = convertType(v.boundTypeReference ?: error("No type constraint type for $v"))
    ).map(v)

    open fun convertContractEffects(v: KtContractEffectList): List<Node.Expression> =
        v.children.filterIsInstance<KtContractEffect>().map { convertExpression(it.getExpression()) }

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

        internal val KtTypeParameterList.leftAngle: PsiElement?
            get() = findChildByType(this, KtTokens.LT)
        internal val KtTypeParameterList.rightAngle: PsiElement?
            get() = findChildByType(this, KtTokens.GT)
        internal val KtTypeParameterListOwner.whereKeyword: PsiElement
            get() = findChildByType(this, KtTokens.WHERE_KEYWORD) ?: error("No where keyword for $this")

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

        internal val KtLambdaExpression.lBrace: PsiElement
            get() = leftCurlyBrace.psi ?: error("No lBrace for $this")
        internal val KtLambdaExpression.rBrace: PsiElement
            get() = rightCurlyBrace?.psi
                ?: error("No rBrace for $this") // It seems funny, but lBrace is non-null, while rBrace is nullable.

        internal val KtDoubleColonExpression.questionMarks
            get() = allChildren
                .takeWhile { it.node.elementType != KtTokens.COLONCOLON }
                .filter { it.node.elementType == KtTokens.QUEST }
                .toList()

        internal val KtDynamicType.dynamicKeyword: PsiElement
            get() = findChildByType(this, KtTokens.DYNAMIC_KEYWORD) ?: error("No dynamic keyword for $this")

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