package ktast.ast

open class MutableVisitor(
    protected val extrasMap: MutableExtrasMap? = null
) {

    open fun <T : Node> preVisit(v: T, parent: Node?): T = v
    open fun <T : Node> postVisit(v: T, parent: Node?): T = v

    fun <T : Node> visit(v: T) = visit(v, null)
    open fun <T : Node> visit(v: T, parent: Node?, ch: ChangedRef = ChangedRef(false)): T = v.run {
        ch.sub { newCh ->
            preVisit(this, parent).run {
                val new: Node = when (this) {
                    is Node.HasSimpleStringRepresentation -> this
                    is Node.KotlinFile -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        packageDirective = visitChildren(packageDirective, newCh),
                        importDirectives = visitChildren(importDirectives, newCh),
                        declarations = visitChildren(declarations, newCh)
                    )
                    is Node.KotlinScript -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        packageDirective = visitChildren(packageDirective, newCh),
                        importDirectives = visitChildren(importDirectives, newCh),
                        expressions = visitChildren(expressions, newCh)
                    )
                    is Node.PackageDirective -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        packageKeyword = visitChildren(packageKeyword, newCh),
                        names = visitChildren(names, newCh),
                    )
                    is Node.ImportDirectives -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.ImportDirective -> copy(
                        importKeyword = visitChildren(importKeyword, newCh),
                        names = visitChildren(names, newCh),
                        importAlias = visitChildren(importAlias, newCh),
                    )
                    is Node.ImportDirective.ImportAlias -> copy(
                        name = visitChildren(name, newCh),
                    )
                    is Node.ClassDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        declarationKeyword = visitChildren(declarationKeyword, newCh),
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        primaryConstructor = visitChildren(primaryConstructor, newCh),
                        classParents = visitChildren(classParents, newCh),
                        typeConstraintSet = visitChildren(typeConstraintSet, newCh),
                        classBody = visitChildren(classBody, newCh)
                    )
                    is Node.ClassDeclaration.ClassParents -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.ClassDeclaration.ClassParent.CallConstructor -> copy(
                        type = visitChildren(type, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        args = visitChildren(args, newCh),
                        lambda = visitChildren(lambda, newCh)
                    )
                    is Node.ClassDeclaration.ClassParent.DelegatedType -> copy(
                        type = visitChildren(type, newCh),
                        byKeyword = visitChildren(byKeyword, newCh),
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.ClassDeclaration.ClassParent.Type -> copy(
                        type = visitChildren(type, newCh),
                    )
                    is Node.ClassDeclaration.PrimaryConstructor -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        constructorKeyword = visitChildren(constructorKeyword, newCh),
                        params = visitChildren(params, newCh)
                    )
                    is Node.ClassDeclaration.ClassBody -> copy(
                        enumEntries = visitChildren(enumEntries, newCh),
                        declarations = visitChildren(declarations, newCh),
                    )
                    is Node.InitDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        block = visitChildren(block, newCh),
                    )
                    is Node.FunctionDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        funKeyword = visitChildren(funKeyword, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        receiverTypeRef = visitChildren(receiverTypeRef, newCh),
                        name = visitChildren(name, newCh),
                        params = visitChildren(params, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                        postModifiers = visitChildren(postModifiers, newCh),
                        equals = visitChildren(equals, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.FunctionParams -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.FunctionParam -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        valOrVar = visitChildren(valOrVar, newCh),
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                        equals = visitChildren(equals, newCh),
                        defaultValue = visitChildren(defaultValue, newCh),
                    )
                    is Node.PropertyDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        valOrVar = visitChildren(valOrVar, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        receiverTypeRef = visitChildren(receiverTypeRef, newCh),
                        lPar = visitChildren(lPar, newCh),
                        variables = visitChildren(variables, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                        rPar = visitChildren(rPar, newCh),
                        typeConstraintSet = visitChildren(typeConstraintSet, newCh),
                        equals = visitChildren(equals, newCh),
                        initializer = visitChildren(initializer, newCh),
                        propertyDelegate = visitChildren(propertyDelegate, newCh),
                        accessors = visitChildren(accessors, newCh)
                    )
                    is Node.PropertyDeclaration.PropertyDelegate -> copy(
                        byKeyword = visitChildren(byKeyword, newCh),
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Variable -> copy(
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.PropertyDeclaration.Getter -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        getKeyword = visitChildren(getKeyword, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                        postModifiers = visitChildren(postModifiers, newCh),
                        equals = visitChildren(equals, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.PropertyDeclaration.Setter -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        setKeyword = visitChildren(setKeyword, newCh),
                        params = visitChildren(params, newCh),
                        postModifiers = visitChildren(postModifiers, newCh),
                        equals = visitChildren(equals, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.TypeAliasDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.SecondaryConstructorDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        constructorKeyword = visitChildren(constructorKeyword, newCh),
                        params = visitChildren(params, newCh),
                        delegationCall = visitChildren(delegationCall, newCh),
                        block = visitChildren(block, newCh)
                    )
                    is Node.SecondaryConstructorDeclaration.DelegationCall -> copy(
                        target = visitChildren(target, newCh),
                        args = visitChildren(args, newCh),
                    )
                    is Node.ClassDeclaration.ClassBody.EnumEntry -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        name = visitChildren(name, newCh),
                        args = visitChildren(args, newCh),
                        classBody = visitChildren(classBody, newCh)
                    )
                    is Node.TypeParams -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.TypeParam -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.TypeArgs -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.TypeArg -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.TypeRef -> copy(
                        lPar = visitChildren(lPar, newCh),
                        modifiers = visitChildren(modifiers, newCh),
                        type = visitChildren(type, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.FunctionType -> copy(
                        lPar = visitChildren(lPar, newCh),
                        modifiers = visitChildren(modifiers, newCh),
                        contextReceivers = visitChildren(contextReceivers, newCh),
                        functionTypeReceiver = visitChildren(functionTypeReceiver, newCh),
                        params = visitChildren(params, newCh),
                        returnTypeRef = visitChildren(returnTypeRef, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.FunctionType.ContextReceivers -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.FunctionType.ContextReceiver -> copy(
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.FunctionType.FunctionTypeReceiver -> copy(
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.FunctionType.Params -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.FunctionType.Param -> copy(
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.SimpleType -> copy(
                        qualifiers = visitChildren(qualifiers, newCh),
                        name = visitChildren(name, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                    )
                    is Node.SimpleType.Qualifier -> copy(
                        name = visitChildren(name, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                    )
                    is Node.NullableType -> copy(
                        lPar = visitChildren(lPar, newCh),
                        modifiers = visitChildren(modifiers, newCh),
                        type = visitChildren(type, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.DynamicType -> this
                    is Node.ValueArgs -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.ValueArg -> copy(
                        name = visitChildren(name, newCh),
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.ExpressionContainer -> copy(
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.IfExpression -> copy(
                        ifKeyword = visitChildren(ifKeyword, newCh),
                        condition = visitChildren(condition, newCh),
                        body = visitChildren(body, newCh),
                        elseBody = visitChildren(elseBody, newCh)
                    )
                    is Node.TryExpression -> copy(
                        block = visitChildren(block, newCh),
                        catchClauses = visitChildren(catchClauses, newCh),
                        finallyBlock = visitChildren(finallyBlock, newCh)
                    )
                    is Node.TryExpression.CatchClause -> copy(
                        catchKeyword = visitChildren(catchKeyword, newCh),
                        params = visitChildren(params, newCh),
                        block = visitChildren(block, newCh),
                    )
                    is Node.ForExpression -> copy(
                        forKeyword = visitChildren(forKeyword, newCh),
                        loopParam = visitChildren(loopParam, newCh),
                        loopRange = visitChildren(loopRange, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.WhileExpression -> copy(
                        whileKeyword = visitChildren(whileKeyword, newCh),
                        condition = visitChildren(condition, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.BinaryExpression -> copy(
                        lhs = visitChildren(lhs, newCh),
                        operator = visitChildren(operator, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.BinaryInfixExpression -> copy(
                        lhs = visitChildren(lhs, newCh),
                        operator = visitChildren(operator, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.UnaryExpression -> copy(
                        expression = visitChildren(expression, newCh),
                        operator = visitChildren(operator, newCh)
                    )
                    is Node.BinaryTypeExpression -> copy(
                        lhs = visitChildren(lhs, newCh),
                        operator = visitChildren(operator, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.CallableReferenceExpression -> copy(
                        lhs = visitChildren(lhs, newCh),
                        rhs = visitChildren(rhs, newCh),
                    )
                    is Node.ClassLiteralExpression -> copy(
                        lhs = visitChildren(lhs, newCh)
                    )
                    is Node.DoubleColonExpression.Receiver.Expression -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.DoubleColonExpression.Receiver.Type -> copy(
                        type = visitChildren(type, newCh),
                        questionMarks = visitChildren(questionMarks, newCh),
                    )
                    is Node.ParenthesizedExpression -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.StringLiteralExpression -> copy(
                        entries = visitChildren(entries, newCh)
                    )
                    is Node.StringLiteralExpression.LiteralStringEntry -> this
                    is Node.StringLiteralExpression.ShortTemplateEntry -> this
                    is Node.StringLiteralExpression.UnicodeEscapeEntry -> this
                    is Node.StringLiteralExpression.RegularEscapeEntry -> this
                    is Node.StringLiteralExpression.LongTemplateEntry -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.ConstantLiteralExpression -> this
                    is Node.LambdaExpression -> copy(
                        params = visitChildren(params, newCh),
                        lambdaBody = visitChildren(lambdaBody, newCh)
                    )
                    is Node.LambdaParams -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.LambdaParam -> copy(
                        lPar = visitChildren(lPar, newCh),
                        variables = visitChildren(variables, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                        rPar = visitChildren(rPar, newCh),
                        colon = visitChildren(colon, newCh),
                        destructTypeRef = visitChildren(destructTypeRef, newCh),
                    )
                    is Node.LambdaParam.Variable -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.LambdaExpression.LambdaBody -> copy(
                        statements = visitChildren(statements, newCh)
                    )
                    is Node.ThisExpression -> this
                    is Node.SuperExpression -> copy(
                        typeArg = visitChildren(typeArg, newCh)
                    )
                    is Node.WhenExpression -> copy(
                        whenKeyword = visitChildren(whenKeyword, newCh),
                        lPar = visitChildren(lPar, newCh),
                        expression = visitChildren(expression, newCh),
                        rPar = visitChildren(rPar, newCh),
                        whenBranches = visitChildren(whenBranches, newCh),
                    )
                    is Node.WhenExpression.WhenBranch.Conditional -> copy(
                        whenConditions = visitChildren(whenConditions, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.WhenExpression.WhenBranch.Else -> copy(
                        elseKeyword = visitChildren(elseKeyword, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.WhenExpression.WhenCondition.Expression -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.WhenExpression.WhenCondition.In -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.WhenExpression.WhenCondition.Is -> copy(
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.ObjectLiteralExpression -> copy(
                        declaration = visitChildren(declaration, newCh),
                    )
                    is Node.ThrowExpression -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.ReturnExpression -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.ContinueExpression -> this
                    is Node.BreakExpression -> this
                    is Node.CollectionLiteralExpression -> copy(
                        expressions = visitChildren(expressions, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.NameExpression -> this
                    is Node.LabeledExpression -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.AnnotatedExpression -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.CallExpression -> copy(
                        expression = visitChildren(expression, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        args = visitChildren(args, newCh),
                        lambdaArg = visitChildren(lambdaArg, newCh)
                    )
                    is Node.CallExpression.LambdaArg -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.ArrayAccessExpression -> copy(
                        expression = visitChildren(expression, newCh),
                        indices = visitChildren(indices, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.AnonymousFunctionExpression -> copy(
                        function = visitChildren(function, newCh)
                    )
                    is Node.PropertyExpression -> copy(
                        declaration = visitChildren(declaration, newCh)
                    )
                    is Node.BlockExpression -> copy(
                        statements = visitChildren(statements, newCh)
                    )
                    is Node.Modifiers -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.AnnotationSet -> copy(
                        atSymbol = visitChildren(atSymbol, newCh),
                        target = visitChildren(target, newCh),
                        colon = visitChildren(colon, newCh),
                        lBracket = visitChildren(lBracket, newCh),
                        annotations = visitChildren(annotations, newCh),
                        rBracket = visitChildren(rBracket, newCh),
                    )
                    is Node.AnnotationSet.Annotation -> copy(
                        type = visitChildren(type, newCh),
                        args = visitChildren(args, newCh),
                    )
                    is Node.TypeConstraintSet -> copy(
                        whereKeyword = visitChildren(whereKeyword, newCh),
                        constraints = visitChildren(constraints, newCh),
                    )
                    is Node.TypeConstraintSet.TypeConstraints -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.TypeConstraintSet.TypeConstraint -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.Contract -> copy(
                        contractKeyword = visitChildren(contractKeyword, newCh),
                        contractEffects = visitChildren(contractEffects, newCh),
                    )
                    is Node.Contract.ContractEffects -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Contract.ContractEffect -> copy(
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Extra -> this
                    // Currently, else branch is required even when sealed classes are exhaustive.
                    // See: https://youtrack.jetbrains.com/issue/KT-21908
                    else -> error("Unrecognized node: $this")
                }
                new.origOrChanged(this, newCh)
            }.let { postVisit(it, parent) as T }.also { newCh.markIf(this, it) }
        }
    }

    protected fun <T : Node?> Node.visitChildren(v: T, ch: ChangedRef): T =
        if (v != null) {
            visit(v, this, ch).also { new ->
                if (ch.changed) {
                    extrasMap?.moveExtras(v, new)
                }
            }
        } else {
            v
        }

    protected fun <T : Node> Node.visitChildren(v: List<T>, ch: ChangedRef): List<T> = ch.sub { newCh ->
        val newList = v.map { orig -> visitChildren(orig, newCh).also { newCh.markIf(it, orig) } }
        newList.origOrChanged(v, newCh)
    }

    protected fun <T> T.origOrChanged(orig: T, ref: ChangedRef) = if (ref.changed) this else orig

    open class ChangedRef(var changed: Boolean) {
        fun markIf(v1: Any?, v2: Any?) {
            if (v1 !== v2) changed = true
        }

        open fun <T> sub(fn: (ChangedRef) -> T): T = ChangedRef(false).let { newCh ->
            fn(newCh).also { if (newCh.changed) changed = true }
        }
    }

    companion object {
        fun <T : Node> preVisit(v: T, extrasMap: MutableExtrasMap? = null, fn: (v: Node, parent: Node?) -> Node?) =
            object : MutableVisitor(extrasMap) {
                override fun <T : Node> preVisit(v: T, parent: Node?): T = fn(v, parent) as T
            }.visit(v)

        fun <T : Node> postVisit(v: T, extrasMap: MutableExtrasMap? = null, fn: (v: Node, parent: Node?) -> Node?) =
            object : MutableVisitor(extrasMap) {
                override fun <T : Node> postVisit(v: T, parent: Node?): T = fn(v, parent) as T
            }.visit(v)
    }
}