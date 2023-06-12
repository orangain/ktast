package ktast.ast

/**
 * Visitor for AST nodes that can mutate the nodes.
 *
 * @param extrasMap optional extras map, defaults to null.
 */
open class MutableVisitor(
    protected val extrasMap: MutableExtrasMap? = null
) {
    companion object {
        /**
         * Traverse the given AST node and its descendants depth-first order and calls the given callback function for each node. When the callback function returns a different node, the node is replaced with the returned node.
         *
         * @param rootNode root AST node to traverse.
         * @param extrasMap optional extras map, defaults to null.
         * @param callback callback function to be called for each node.
         * @return the modified root node.
         */
        fun <T : Node> traverse(
            rootNode: T,
            extrasMap: MutableExtrasMap? = null,
            callback: (path: NodePath<*>) -> Node
        ): T =
            object : MutableVisitor(extrasMap) {
                override fun <C : Node> preVisit(path: NodePath<C>): C = callback(path) as C
            }.traverse(rootNode)
    }

    /**
     * Traverse the given AST node and its descendants depth-first order and calls the protected [preVisit] and [postVisit] methods for each node. When the methods return a different node, the node is replaced with the returned node.
     *
     * @param rootNode root AST node to traverse.
     * @return the modified root node.
     */
    fun <T : Node> traverse(rootNode: T) = visit(NodePath.rootPathOf(rootNode))

    /**
     * Method to be called before visiting the descendants of the given node.
     *
     * @param path path of the node.
     * @return the modified node.
     */
    protected open fun <T : Node> preVisit(path: NodePath<T>): T = path.node

    /**
     * Method to be called after visiting the descendants of the given node.
     *
     * @param path path of the node.
     * @return the modified node.
     */
    protected open fun <T : Node> postVisit(path: NodePath<T>): T = path.node

    /**
     * Method to be called for each node.
     *
     * @param path path of the node.
     * @param ch changed flag.
     * @return the modified node.
     */
    protected open fun <T : Node> visit(path: NodePath<T>, ch: ChangedRef = ChangedRef(false)): T = ch.sub { newCh ->
        val origNode = path.node
        path.copy(node = preVisit(path)).run {
            node.run {
                @Suppress("UNCHECKED_CAST") val new: T = when (this) {
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
                        packageKeyword = visitChildren(packageKeyword, newCh),
                        names = visitChildren(names, newCh),
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
                        lAngle = visitChildren(lAngle, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        rAngle = visitChildren(rAngle, newCh),
                        primaryConstructor = visitChildren(primaryConstructor, newCh),
                        classParents = visitChildren(classParents, newCh),
                        typeConstraintSet = visitChildren(typeConstraintSet, newCh),
                        classBody = visitChildren(classBody, newCh)
                    )
                    is Node.Declaration.ClassDeclaration.ConstructorClassParent -> copy(
                        type = visitChildren(type, newCh),
                        lPar = visitChildren(lPar, newCh),
                        args = visitChildren(args, newCh),
                        rPar = visitChildren(rPar, newCh),
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
                        lBrace = visitChildren(lBrace, newCh),
                        enumEntries = visitChildren(enumEntries, newCh),
                        declarations = visitChildren(declarations, newCh),
                        rBrace = visitChildren(rBrace, newCh),
                    )
                    is Node.Declaration.ClassDeclaration.ClassBody.Initializer -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        block = visitChildren(block, newCh),
                    )
                    is Node.Declaration.FunctionDeclaration -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        funKeyword = visitChildren(funKeyword, newCh),
                        lAngle = visitChildren(lAngle, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        rAngle = visitChildren(rAngle, newCh),
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
                        lAngle = visitChildren(lAngle, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        rAngle = visitChildren(rAngle, newCh),
                        receiverType = visitChildren(receiverType, newCh),
                        lPar = visitChildren(lPar, newCh),
                        variables = visitChildren(variables, newCh),
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
                        annotationSets = visitChildren(annotationSets, newCh),
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
                        lAngle = visitChildren(lAngle, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        rAngle = visitChildren(rAngle, newCh),
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
                        lPar = visitChildren(lPar, newCh),
                        args = visitChildren(args, newCh),
                        rPar = visitChildren(rPar, newCh),
                        classBody = visitChildren(classBody, newCh)
                    )
                    is Node.TypeParam -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh)
                    )
                    is Node.TypeArg -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        type = visitChildren(type, newCh),
                    )
                    is Node.Type.FunctionType -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        contextReceiver = visitChildren(contextReceiver, newCh),
                        receiverType = visitChildren(receiverType, newCh),
                        dotSymbol = visitChildren(dotSymbol, newCh),
                        lPar = visitChildren(lPar, newCh),
                        params = visitChildren(params, newCh),
                        rPar = visitChildren(rPar, newCh),
                        returnType = visitChildren(returnType, newCh),
                    )
                    is Node.ContextReceiver -> copy(
                        lPar = visitChildren(lPar, newCh),
                        receiverTypes = visitChildren(receiverTypes, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.Type.FunctionType.FunctionTypeParam -> copy(
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh),
                    )
                    is Node.Type.SimpleType -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        pieces = visitChildren(pieces, newCh),
                    )
                    is Node.Type.SimpleType.SimpleTypePiece -> copy(
                        name = visitChildren(name, newCh),
                        lAngle = visitChildren(lAngle, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        rAngle = visitChildren(rAngle, newCh),
                    )
                    is Node.Type.NullableType -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        innerType = visitChildren(innerType, newCh),
                        questionMark = visitChildren(questionMark, newCh),
                    )
                    is Node.Type.ParenthesizedType -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        lPar = visitChildren(lPar, newCh),
                        innerType = visitChildren(innerType, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.Type.DynamicType -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        dynamicKeyword = visitChildren(dynamicKeyword, newCh),
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
                        innerExpression = visitChildren(innerExpression, newCh)
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
                        lBrace = visitChildren(lBrace, newCh),
                        params = visitChildren(params, newCh),
                        arrow = visitChildren(arrow, newCh),
                        lambdaBody = visitChildren(lambdaBody, newCh),
                        rBrace = visitChildren(rBrace, newCh),
                    )
                    is Node.LambdaParam -> copy(
                        lPar = visitChildren(lPar, newCh),
                        variables = visitChildren(variables, newCh),
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
                        subject = visitChildren(subject, newCh),
                        lBrace = visitChildren(lBrace, newCh),
                        whenBranches = visitChildren(whenBranches, newCh),
                        rBrace = visitChildren(rBrace, newCh),
                    )
                    is Node.Expression.WhenExpression.WhenSubject -> copy(
                        lPar = visitChildren(lPar, newCh),
                        annotationSets = visitChildren(annotationSets, newCh),
                        valKeyword = visitChildren(valKeyword, newCh),
                        variable = visitChildren(variable, newCh),
                        expression = visitChildren(expression, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.Expression.WhenExpression.ConditionalWhenBranch -> copy(
                        whenConditions = visitChildren(whenConditions, newCh),
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
                        lAngle = visitChildren(lAngle, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        rAngle = visitChildren(rAngle, newCh),
                        lPar = visitChildren(lPar, newCh),
                        args = visitChildren(args, newCh),
                        rPar = visitChildren(rPar, newCh),
                        lambdaArg = visitChildren(lambdaArg, newCh),
                    )
                    is Node.Expression.CallExpression.LambdaArg -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        label = visitChildren(label, newCh),
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Expression.IndexedAccessExpression -> copy(
                        expression = visitChildren(expression, newCh),
                        indices = visitChildren(indices, newCh),
                    )
                    is Node.Expression.AnonymousFunctionExpression -> copy(
                        function = visitChildren(function, newCh)
                    )
                    is Node.Expression.BlockExpression -> copy(
                        lBrace = visitChildren(lBrace, newCh),
                        statements = visitChildren(statements, newCh),
                        rBrace = visitChildren(rBrace, newCh),
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
                        lPar = visitChildren(lPar, newCh),
                        args = visitChildren(args, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.PostModifier.TypeConstraintSet -> copy(
                        whereKeyword = visitChildren(whereKeyword, newCh),
                        constraints = visitChildren(constraints, newCh),
                    )
                    is Node.PostModifier.TypeConstraintSet.TypeConstraint -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh)
                    )
                    is Node.PostModifier.Contract -> copy(
                        contractKeyword = visitChildren(contractKeyword, newCh),
                        lBracket = visitChildren(lBracket, newCh),
                        contractEffects = visitChildren(contractEffects, newCh),
                        rBracket = visitChildren(rBracket, newCh),
                    )
                    is Node.Keyword -> this
                    is Node.Extra -> this
                    // Currently, else branch is required even when sealed classes are exhaustive.
                    // See: https://youtrack.jetbrains.com/issue/KT-21908
                    else -> error("Unrecognized node: $this")
                } as T
                new.origOrChanged(this, newCh)
            }
        }.let {
            postVisit(path.copy(node = it))
        }.also { newCh.markIf(origNode, it) }
    }

    protected fun <T : Node?> NodePath<*>.visitChildren(v: T, ch: ChangedRef): T =
        if (v != null) {
            visit(childPathOf(v), ch).also { new ->
                if (ch.changed) {
                    extrasMap?.moveExtras(v, new)
                }
            }
        } else {
            v
        }

    protected fun <T : Node> NodePath<*>.visitChildren(v: List<T>, ch: ChangedRef): List<T> = ch.sub { newCh ->
        val newList = v.map { orig -> visitChildren(orig, newCh).also { newCh.markIf(it, orig) } }
        newList.origOrChanged(v, newCh)
    }

    protected fun <T> T.origOrChanged(orig: T, ref: ChangedRef) = if (ref.changed) this else orig

    protected class ChangedRef(var changed: Boolean) {
        fun <T : Node?> markIf(v1: T, v2: T) {
            if (v1 !== v2) changed = true
        }

        fun <T> sub(fn: (ChangedRef) -> T): T = ChangedRef(false).let { newCh ->
            fn(newCh).also { if (newCh.changed) changed = true }
        }
    }
}