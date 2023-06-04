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
                    is Node.Statement.ForStatement -> copy(
                        forKeyword = visitChildren(forKeyword, newCh),
                        lPar = visitChildren(lPar, newCh),
                        loopParam = visitChildren(loopParam, newCh),
                        inKeyword = visitChildren(inKeyword, newCh),
                        loopRange = visitChildren(loopRange, newCh),
                        rPar = visitChildren(rPar, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Statement.WhileStatement -> copy(
                        whileKeyword = visitChildren(whileKeyword, newCh),
                        lPar = visitChildren(lPar, newCh),
                        condition = visitChildren(condition, newCh),
                        rPar = visitChildren(rPar, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Statement.DoWhileStatement -> copy(
                        doKeyword = visitChildren(doKeyword, newCh),
                        body = visitChildren(body, newCh),
                        whileKeyword = visitChildren(whileKeyword, newCh),
                        lPar = visitChildren(lPar, newCh),
                        condition = visitChildren(condition, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.Declaration.ClassDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        classDeclarationKeyword = visitChildren(classDeclarationKeyword, newCh),
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        primaryConstructor = visitChildren(primaryConstructor, newCh),
                        classParents = visitChildren(classParents, newCh),
                        typeConstraintSet = visitChildren(typeConstraintSet, newCh),
                        classBody = visitChildren(classBody, newCh)
                    )
                    is Node.Declaration.ClassDeclaration.ClassParents -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.Declaration.ClassDeclaration.ConstructorClassParent -> copy(
                        type = visitChildren(type, newCh),
                        args = visitChildren(args, newCh),
                    )
                    is Node.Declaration.ClassDeclaration.DelegationClassParent -> copy(
                        type = visitChildren(type, newCh),
                        byKeyword = visitChildren(byKeyword, newCh),
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Declaration.ClassDeclaration.TypeClassParent -> copy(
                        type = visitChildren(type, newCh),
                    )
                    is Node.Declaration.ClassDeclaration.PrimaryConstructor -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        constructorKeyword = visitChildren(constructorKeyword, newCh),
                        lPar = visitChildren(lPar, newCh),
                        params = visitChildren(params, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.Declaration.ClassDeclaration.ClassBody -> copy(
                        enumEntries = visitChildren(enumEntries, newCh),
                        declarations = visitChildren(declarations, newCh),
                    )
                    is Node.Declaration.ClassDeclaration.ClassBody.Initializer -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        block = visitChildren(block, newCh),
                    )
                    is Node.Declaration.FunctionDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        funKeyword = visitChildren(funKeyword, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        receiverType = visitChildren(receiverType, newCh),
                        name = visitChildren(name, newCh),
                        lPar = visitChildren(lPar, newCh),
                        params = visitChildren(params, newCh),
                        rPar = visitChildren(rPar, newCh),
                        returnType = visitChildren(returnType, newCh),
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
                        valOrVarKeyword = visitChildren(valOrVarKeyword, newCh),
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh),
                        equals = visitChildren(equals, newCh),
                        defaultValue = visitChildren(defaultValue, newCh),
                    )
                    is Node.Declaration.PropertyDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        valOrVarKeyword = visitChildren(valOrVarKeyword, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        receiverType = visitChildren(receiverType, newCh),
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
                    is Node.Declaration.PropertyDeclaration.PropertyDelegate -> copy(
                        byKeyword = visitChildren(byKeyword, newCh),
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Variable -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh),
                    )
                    is Node.Declaration.PropertyDeclaration.Getter -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        getKeyword = visitChildren(getKeyword, newCh),
                        type = visitChildren(type, newCh),
                        postModifiers = visitChildren(postModifiers, newCh),
                        equals = visitChildren(equals, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Declaration.PropertyDeclaration.Setter -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        setKeyword = visitChildren(setKeyword, newCh),
                        params = visitChildren(params, newCh),
                        postModifiers = visitChildren(postModifiers, newCh),
                        equals = visitChildren(equals, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Declaration.TypeAliasDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        equals = visitChildren(equals, newCh),
                        type = visitChildren(type, newCh)
                    )
                    is Node.Declaration.ClassDeclaration.ClassBody.SecondaryConstructor -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        constructorKeyword = visitChildren(constructorKeyword, newCh),
                        lPar = visitChildren(lPar, newCh),
                        params = visitChildren(params, newCh),
                        rPar = visitChildren(rPar, newCh), delegationCall = visitChildren(delegationCall, newCh),
                        block = visitChildren(block, newCh)
                    )
                    is Node.Declaration.ClassDeclaration.ClassBody.EnumEntry -> copy(
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
                        type = visitChildren(type, newCh)
                    )
                    is Node.TypeArgs -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.TypeArg.TypeProjection -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        type = visitChildren(type, newCh),
                    )
                    is Node.TypeArg.StarProjection -> copy(
                        asterisk = visitChildren(asterisk, newCh),
                    )
                    is Node.Type.FunctionType -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        contextReceiver = visitChildren(contextReceiver, newCh),
                        receiverType = visitChildren(receiverType, newCh),
                        dotSymbol = visitChildren(dotSymbol, newCh),
                        params = visitChildren(params, newCh),
                        returnType = visitChildren(returnType, newCh),
                    )
                    is Node.ContextReceiver -> copy(
                        receiverTypes = visitChildren(receiverTypes, newCh),
                    )
                    is Node.ContextReceiverTypes -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Type.FunctionType.FunctionTypeParams -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Type.FunctionType.FunctionTypeParam -> copy(
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh),
                    )
                    is Node.Type.SimpleType -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        qualifiers = visitChildren(qualifiers, newCh),
                        name = visitChildren(name, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                    )
                    is Node.Type.SimpleType.SimpleTypeQualifier -> copy(
                        name = visitChildren(name, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                    )
                    is Node.Type.NullableType -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        type = visitChildren(type, newCh),
                        questionMark = visitChildren(questionMark, newCh),
                    )
                    is Node.Type.ParenthesizedType -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        lPar = visitChildren(lPar, newCh),
                        type = visitChildren(type, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.Type.DynamicType -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                    )
                    is Node.ValueArgs -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.ValueArg -> copy(
                        name = visitChildren(name, newCh),
                        asterisk = visitChildren(asterisk, newCh),
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.IfExpression -> copy(
                        ifKeyword = visitChildren(ifKeyword, newCh),
                        lPar = visitChildren(lPar, newCh),
                        condition = visitChildren(condition, newCh),
                        rPar = visitChildren(rPar, newCh),
                        body = visitChildren(body, newCh),
                        elseKeyword = visitChildren(elseKeyword, newCh),
                        elseBody = visitChildren(elseBody, newCh)
                    )
                    is Node.Expression.TryExpression -> copy(
                        block = visitChildren(block, newCh),
                        catchClauses = visitChildren(catchClauses, newCh),
                        finallyBlock = visitChildren(finallyBlock, newCh)
                    )
                    is Node.Expression.TryExpression.CatchClause -> copy(
                        catchKeyword = visitChildren(catchKeyword, newCh),
                        lPar = visitChildren(lPar, newCh),
                        params = visitChildren(params, newCh),
                        rPar = visitChildren(rPar, newCh),
                        block = visitChildren(block, newCh),
                    )
                    is Node.Expression.BinaryExpression -> copy(
                        lhs = visitChildren(lhs, newCh),
                        operator = visitChildren(operator, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.Expression.PrefixUnaryExpression -> copy(
                        operator = visitChildren(operator, newCh),
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.PostfixUnaryExpression -> copy(
                        expression = visitChildren(expression, newCh),
                        operator = visitChildren(operator, newCh)
                    )
                    is Node.Expression.BinaryTypeExpression -> copy(
                        lhs = visitChildren(lhs, newCh),
                        operator = visitChildren(operator, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.Expression.CallableReferenceExpression -> copy(
                        lhs = visitChildren(lhs, newCh),
                        questionMarks = visitChildren(questionMarks, newCh),
                        rhs = visitChildren(rhs, newCh),
                    )
                    is Node.Expression.ClassLiteralExpression -> copy(
                        lhs = visitChildren(lhs, newCh),
                        questionMarks = visitChildren(questionMarks, newCh),
                    )
                    is Node.Expression.ParenthesizedExpression -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.StringLiteralExpression -> copy(
                        entries = visitChildren(entries, newCh)
                    )
                    is Node.Expression.StringLiteralExpression.LiteralStringEntry -> this
                    is Node.Expression.StringLiteralExpression.EscapeStringEntry -> this
                    is Node.Expression.StringLiteralExpression.TemplateStringEntry -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.ConstantLiteralExpression -> this
                    is Node.Expression.LambdaExpression -> copy(
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
                        destructType = visitChildren(destructType, newCh),
                    )
                    is Node.Expression.LambdaExpression.LambdaBody -> copy(
                        statements = visitChildren(statements, newCh)
                    )
                    is Node.Expression.ThisExpression -> copy(
                        label = visitChildren(label, newCh),
                    )
                    is Node.Expression.SuperExpression -> copy(
                        typeArgType = visitChildren(typeArgType, newCh),
                        label = visitChildren(label, newCh),
                    )
                    is Node.Expression.WhenExpression -> copy(
                        whenKeyword = visitChildren(whenKeyword, newCh),
                        lPar = visitChildren(lPar, newCh),
                        expression = visitChildren(expression, newCh),
                        rPar = visitChildren(rPar, newCh),
                        whenBranches = visitChildren(whenBranches, newCh),
                    )
                    is Node.Expression.WhenExpression.ConditionalWhenBranch -> copy(
                        whenConditions = visitChildren(whenConditions, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Expression.WhenExpression.ElseWhenBranch -> copy(
                        elseKeyword = visitChildren(elseKeyword, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Expression.WhenExpression.ExpressionWhenCondition -> copy(
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Expression.WhenExpression.RangeWhenCondition -> copy(
                        operator = visitChildren(operator, newCh),
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Expression.WhenExpression.TypeWhenCondition -> copy(
                        operator = visitChildren(operator, newCh),
                        type = visitChildren(type, newCh),
                    )
                    is Node.Expression.ObjectLiteralExpression -> copy(
                        declaration = visitChildren(declaration, newCh),
                    )
                    is Node.Expression.ThrowExpression -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.ReturnExpression -> copy(
                        label = visitChildren(label, newCh),
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Expression.ContinueExpression -> copy(
                        label = visitChildren(label, newCh),
                    )
                    is Node.Expression.BreakExpression -> copy(
                        label = visitChildren(label, newCh),
                    )
                    is Node.Expression.CollectionLiteralExpression -> copy(
                        expressions = visitChildren(expressions, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Expression.NameExpression -> this
                    is Node.Expression.LabeledExpression -> copy(
                        label = visitChildren(label, newCh),
                        statement = visitChildren(statement, newCh),
                    )
                    is Node.Expression.AnnotatedExpression -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        statement = visitChildren(statement, newCh)
                    )
                    is Node.Expression.CallExpression -> copy(
                        calleeExpression = visitChildren(calleeExpression, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        args = visitChildren(args, newCh),
                        lambdaArg = visitChildren(lambdaArg, newCh)
                    )
                    is Node.Expression.CallExpression.LambdaArg -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        label = visitChildren(label, newCh),
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Expression.IndexedAccessExpression -> copy(
                        expression = visitChildren(expression, newCh),
                        indices = visitChildren(indices, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Expression.AnonymousFunctionExpression -> copy(
                        function = visitChildren(function, newCh)
                    )
                    is Node.Expression.PropertyExpression -> copy(
                        property = visitChildren(property, newCh)
                    )
                    is Node.Expression.BlockExpression -> copy(
                        statements = visitChildren(statements, newCh)
                    )
                    is Node.Modifiers -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.Modifier.AnnotationSet -> copy(
                        atSymbol = visitChildren(atSymbol, newCh),
                        target = visitChildren(target, newCh),
                        colon = visitChildren(colon, newCh),
                        lBracket = visitChildren(lBracket, newCh),
                        annotations = visitChildren(annotations, newCh),
                        rBracket = visitChildren(rBracket, newCh),
                    )
                    is Node.Modifier.AnnotationSet.Annotation -> copy(
                        type = visitChildren(type, newCh),
                        args = visitChildren(args, newCh),
                    )
                    is Node.PostModifier.TypeConstraintSet -> copy(
                        whereKeyword = visitChildren(whereKeyword, newCh),
                        constraints = visitChildren(constraints, newCh),
                    )
                    is Node.PostModifier.TypeConstraintSet.TypeConstraints -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.PostModifier.TypeConstraintSet.TypeConstraint -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh)
                    )
                    is Node.PostModifier.Contract -> copy(
                        contractKeyword = visitChildren(contractKeyword, newCh),
                        contractEffects = visitChildren(contractEffects, newCh),
                    )
                    is Node.PostModifier.Contract.ContractEffects -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Keyword -> this
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