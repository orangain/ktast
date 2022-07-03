package ktast.ast

open class MutableVisitor(
    protected val extrasMap: MutableExtrasMap? = null
) {

    open fun <T : Node> preVisit(v: T, parent: Node?): T = v
    open fun <T : Node> postVisit(v: T, parent: Node?): T = v

    fun visit(v: Node) = visit(v, null)
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
                        alias = visitChildren(alias, newCh),
                    )
                    is Node.ImportDirective.Alias -> copy(
                        name = visitChildren(name, newCh),
                    )
                    is Node.Declaration.Class -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        declarationKeyword = visitChildren(declarationKeyword, newCh),
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        primaryConstructor = visitChildren(primaryConstructor, newCh),
                        parents = visitChildren(parents, newCh),
                        typeConstraints = visitChildren(typeConstraints, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Declaration.Class.Parents -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.Declaration.Class.Parent.CallConstructor -> copy(
                        type = visitChildren(type, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        args = visitChildren(args, newCh),
                        lambda = visitChildren(lambda, newCh)
                    )
                    is Node.Declaration.Class.Parent.DelegatedType -> copy(
                        type = visitChildren(type, newCh),
                        byKeyword = visitChildren(byKeyword, newCh),
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Declaration.Class.Parent.Type -> copy(
                        type = visitChildren(type, newCh),
                    )
                    is Node.Declaration.Class.PrimaryConstructor -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        constructorKeyword = visitChildren(constructorKeyword, newCh),
                        params = visitChildren(params, newCh)
                    )
                    is Node.Declaration.Class.Body -> copy(
                        enumEntries = visitChildren(enumEntries, newCh),
                        declarations = visitChildren(declarations, newCh),
                    )
                    is Node.Declaration.Init -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        block = visitChildren(block, newCh),
                    )
                    is Node.Declaration.Function -> copy(
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
                    is Node.Declaration.Function.Params -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Declaration.Function.Param -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        valOrVar = visitChildren(valOrVar, newCh),
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                        equals = visitChildren(equals, newCh),
                        defaultValue = visitChildren(defaultValue, newCh),
                    )
                    is Node.Declaration.Property -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        valOrVar = visitChildren(valOrVar, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        receiverTypeRef = visitChildren(receiverTypeRef, newCh),
                        variable = visitChildren(variable, newCh),
                        typeConstraints = visitChildren(typeConstraints, newCh),
                        equals = visitChildren(equals, newCh),
                        initializer = visitChildren(initializer, newCh),
                        delegate = visitChildren(delegate, newCh),
                        accessors = visitChildren(accessors, newCh)
                    )
                    is Node.Declaration.Property.Delegate -> copy(
                        byKeyword = visitChildren(byKeyword, newCh),
                        expression = visitChildren(expression, newCh),
                    )
                    is Node.Declaration.Property.Variable.Single -> copy(
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.Declaration.Property.Variable.Multi -> copy(
                        vars = visitChildren(vars, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Declaration.Property.Accessor.Getter -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        getKeyword = visitChildren(getKeyword, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                        postModifiers = visitChildren(postModifiers, newCh),
                        equals = visitChildren(equals, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Declaration.Property.Accessor.Setter -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        setKeyword = visitChildren(setKeyword, newCh),
                        params = visitChildren(params, newCh),
                        postModifiers = visitChildren(postModifiers, newCh),
                        equals = visitChildren(equals, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Declaration.TypeAlias -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.Declaration.SecondaryConstructor -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        constructorKeyword = visitChildren(constructorKeyword, newCh),
                        params = visitChildren(params, newCh),
                        delegationCall = visitChildren(delegationCall, newCh),
                        block = visitChildren(block, newCh)
                    )
                    is Node.Declaration.SecondaryConstructor.DelegationCall -> copy(
                        target = visitChildren(target, newCh),
                        args = visitChildren(args, newCh),
                    )
                    is Node.EnumEntry -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        name = visitChildren(name, newCh),
                        args = visitChildren(args, newCh),
                        body = visitChildren(body, newCh)
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
                        innerLPar = visitChildren(innerLPar, newCh),
                        innerMods = visitChildren(innerMods, newCh),
                        type = visitChildren(type, newCh),
                        innerRPar = visitChildren(innerRPar, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.Type.Function -> copy(
                        contextReceivers = visitChildren(contextReceivers, newCh),
                        receiver = visitChildren(receiver, newCh),
                        params = visitChildren(params, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.Type.Function.ContextReceivers -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Type.Function.ContextReceiver -> copy(
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.Type.Function.Receiver -> copy(
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.Type.Function.Params -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Type.Function.Param -> copy(
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.Type.Simple -> copy(
                        pieces = visitChildren(pieces, newCh)
                    )
                    is Node.Type.Simple.Piece -> copy(
                        name = visitChildren(name, newCh),
                        typeArgs = visitChildren(typeArgs, newCh)
                    )
                    is Node.Type.Nullable -> copy(
                        lPar = visitChildren(lPar, newCh),
                        modifiers = visitChildren(modifiers, newCh),
                        type = visitChildren(type, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.Type.Dynamic -> this
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
                    is Node.Expression.If -> copy(
                        ifKeyword = visitChildren(ifKeyword, newCh),
                        condition = visitChildren(condition, newCh),
                        body = visitChildren(body, newCh),
                        elseBody = visitChildren(elseBody, newCh)
                    )
                    is Node.Expression.Try -> copy(
                        block = visitChildren(block, newCh),
                        catches = visitChildren(catches, newCh),
                        finallyBlock = visitChildren(finallyBlock, newCh)
                    )
                    is Node.Expression.Try.Catch -> copy(
                        catchKeyword = visitChildren(catchKeyword, newCh),
                        params = visitChildren(params, newCh),
                        block = visitChildren(block, newCh),
                    )
                    is Node.Expression.For -> copy(
                        forKeyword = visitChildren(forKeyword, newCh),
                        loopParam = visitChildren(loopParam, newCh),
                        loopRange = visitChildren(loopRange, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Expression.While -> copy(
                        whileKeyword = visitChildren(whileKeyword, newCh),
                        condition = visitChildren(condition, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Expression.Binary -> copy(
                        lhs = visitChildren(lhs, newCh),
                        operator = visitChildren(operator, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.Expression.BinaryInfix -> copy(
                        lhs = visitChildren(lhs, newCh),
                        operator = visitChildren(operator, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.Expression.Unary -> copy(
                        expression = visitChildren(expression, newCh),
                        operator = visitChildren(operator, newCh)
                    )
                    is Node.Expression.BinaryType -> copy(
                        lhs = visitChildren(lhs, newCh),
                        operator = visitChildren(operator, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.Expression.CallableReference -> copy(
                        lhs = visitChildren(lhs, newCh),
                        rhs = visitChildren(rhs, newCh),
                    )
                    is Node.Expression.ClassLiteral -> copy(
                        lhs = visitChildren(lhs, newCh)
                    )
                    is Node.Expression.DoubleColon.Receiver.Expression -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.DoubleColon.Receiver.Type -> copy(
                        type = visitChildren(type, newCh),
                        questionMarks = visitChildren(questionMarks, newCh),
                    )
                    is Node.Expression.Parenthesized -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.StringTemplate -> copy(
                        entries = visitChildren(entries, newCh)
                    )
                    is Node.Expression.StringTemplate.Entry.Regular -> this
                    is Node.Expression.StringTemplate.Entry.ShortTemplate -> this
                    is Node.Expression.StringTemplate.Entry.UnicodeEscape -> this
                    is Node.Expression.StringTemplate.Entry.RegularEscape -> this
                    is Node.Expression.StringTemplate.Entry.LongTemplate -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.Constant -> this
                    is Node.Expression.Lambda -> copy(
                        params = visitChildren(params, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Expression.Lambda.Params -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Expression.Lambda.Param -> copy(
                        lPar = visitChildren(lPar, newCh),
                        variables = visitChildren(variables, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                        rPar = visitChildren(rPar, newCh),
                        colon = visitChildren(colon, newCh),
                        destructTypeRef = visitChildren(destructTypeRef, newCh),
                    )
                    is Node.Expression.Lambda.Param.Variable -> copy(
                        modifiers = visitChildren(modifiers, newCh),
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.Expression.Lambda.Body -> copy(
                        statements = visitChildren(statements, newCh)
                    )
                    is Node.Expression.This -> this
                    is Node.Expression.Super -> copy(
                        typeArg = visitChildren(typeArg, newCh)
                    )
                    is Node.Expression.When -> copy(
                        lPar = visitChildren(lPar, newCh),
                        expression = visitChildren(expression, newCh),
                        rPar = visitChildren(rPar, newCh),
                        branches = visitChildren(branches, newCh),
                    )
                    is Node.Expression.When.Branch.Conditional -> copy(
                        conditions = visitChildren(conditions, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Expression.When.Branch.Else -> copy(
                        elseKeyword = visitChildren(elseKeyword, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Expression.When.Condition.Expression -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.When.Condition.In -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.When.Condition.Is -> copy(
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.Expression.Object -> copy(
                        declaration = visitChildren(declaration, newCh),
                    )
                    is Node.Expression.Throw -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.Return -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.Continue -> this
                    is Node.Expression.Break -> this
                    is Node.Expression.CollectionLiteral -> copy(
                        expressions = visitChildren(expressions, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Expression.Name -> this
                    is Node.Expression.Labeled -> copy(
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.Annotated -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        expression = visitChildren(expression, newCh)
                    )
                    is Node.Expression.Call -> copy(
                        expression = visitChildren(expression, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        args = visitChildren(args, newCh),
                        lambdaArg = visitChildren(lambdaArg, newCh)
                    )
                    is Node.Expression.Call.LambdaArg -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        func = visitChildren(func, newCh)
                    )
                    is Node.Expression.ArrayAccess -> copy(
                        expression = visitChildren(expression, newCh),
                        indices = visitChildren(indices, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Expression.AnonymousFunction -> copy(
                        function = visitChildren(function, newCh)
                    )
                    is Node.Expression.Property -> copy(
                        declaration = visitChildren(declaration, newCh)
                    )
                    is Node.Expression.Block -> copy(
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
                    is Node.PostModifier.TypeConstraints -> copy(
                        whereKeyword = visitChildren(whereKeyword, newCh),
                        constraints = visitChildren(constraints, newCh),
                    )
                    is Node.PostModifier.TypeConstraints.TypeConstraintList -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.PostModifier.TypeConstraints.TypeConstraint -> copy(
                        annotationSets = visitChildren(annotationSets, newCh),
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.PostModifier.Contract -> copy(
                        contractKeyword = visitChildren(contractKeyword, newCh),
                        contractEffects = visitChildren(contractEffects, newCh),
                    )
                    is Node.PostModifier.Contract.ContractEffects -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.PostModifier.Contract.ContractEffect -> copy(
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