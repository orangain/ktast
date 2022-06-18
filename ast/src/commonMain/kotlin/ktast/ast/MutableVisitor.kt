package ktast.ast

open class MutableVisitor {

    open fun <T : Node?> preVisit(v: T, parent: Node): T = v
    open fun <T : Node?> postVisit(v: T, parent: Node): T = v

    open fun <T : Node?> visit(v: T, parent: Node, ch: ChangedRef = ChangedRef(false)): T = v.run {
        ch.sub { newCh ->
            preVisit(this, parent)?.run {
                val new: Node = when (this) {
                    is Node.File -> copy(
                        anns = visitChildren(anns, newCh),
                        pkg = visitChildren(pkg, newCh),
                        imports = visitChildren(imports, newCh),
                        decls = visitChildren(decls, newCh)
                    )
                    is Node.Script -> copy(
                        anns = visitChildren(anns, newCh),
                        pkg = visitChildren(pkg, newCh),
                        imports = visitChildren(imports, newCh),
                        exprs = visitChildren(exprs, newCh)
                    )
                    is Node.Package -> copy(
                        mods = visitChildren(mods, newCh),
                        packageKeyword = visitChildren(packageKeyword, newCh),
                        names = visitChildren(names, newCh),
                    )
                    is Node.Imports -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.Import -> copy(
                        importKeyword = visitChildren(importKeyword, newCh),
                        names = visitChildren(names, newCh),
                        alias = visitChildren(alias, newCh),
                    )
                    is Node.Import.Alias -> copy(
                        name = visitChildren(name, newCh),
                    )
                    is Node.Decls -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.Decl.Structured -> copy(
                        mods = visitChildren(mods, newCh),
                        declarationKeyword = visitChildren(declarationKeyword, newCh),
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        primaryConstructor = visitChildren(primaryConstructor, newCh),
                        parents = visitChildren(parents, newCh),
                        typeConstraints = visitChildren(typeConstraints, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Decl.Structured.Parents -> copy(
                        items = visitChildren(items, newCh),
                    )
                    is Node.Decl.Structured.Parent.CallConstructor -> copy(
                        type = visitChildren(type, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        args = visitChildren(args, newCh),
                        lambda = visitChildren(lambda, newCh)
                    )
                    is Node.Decl.Structured.Parent.DelegatedType -> copy(
                        type = visitChildren(type, newCh),
                        byKeyword = visitChildren(byKeyword, newCh),
                        expr = visitChildren(expr, newCh),
                    )
                    is Node.Decl.Structured.Parent.Type -> copy(
                        type = visitChildren(type, newCh),
                    )
                    is Node.Decl.Structured.PrimaryConstructor -> copy(
                        mods = visitChildren(mods, newCh),
                        constructorKeyword = visitChildren(constructorKeyword, newCh),
                        params = visitChildren(params, newCh)
                    )
                    is Node.Decl.Structured.Body -> copy(
                        decls = visitChildren(decls, newCh),
                    )
                    is Node.Decl.Init -> copy(
                        mods = visitChildren(mods, newCh),
                        block = visitChildren(block, newCh),
                    )
                    is Node.Decl.Func -> copy(
                        mods = visitChildren(mods, newCh),
                        funKeyword = visitChildren(funKeyword, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        receiverTypeRef = visitChildren(receiverTypeRef, newCh),
                        name = visitChildren(name, newCh),
                        postTypeParams = visitChildren(postTypeParams, newCh),
                        params = visitChildren(params, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                        postMods = visitChildren(postMods, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Decl.Func.Params -> copy(
                        params = visitChildren(params, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Decl.Func.Params.Param -> copy(
                        mods = visitChildren(mods, newCh),
                        valOrVar = visitChildren(valOrVar, newCh),
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                        initializer = visitChildren(initializer, newCh)
                    )
                    is Node.Decl.Func.Body.Block -> copy(
                        block = visitChildren(block, newCh)
                    )
                    is Node.Decl.Func.Body.Expr -> copy(
                        equals = visitChildren(equals, newCh),
                        expr = visitChildren(expr, newCh),
                    )
                    is Node.Decl.Property -> copy(
                        mods = visitChildren(mods, newCh),
                        valOrVar = visitChildren(valOrVar, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        receiverTypeRef = visitChildren(receiverTypeRef, newCh),
                        variable = visitChildren(variable, newCh),
                        typeConstraints = visitChildren(typeConstraints, newCh),
                        initializer = visitChildren(initializer, newCh),
                        delegate = visitChildren(delegate, newCh),
                        accessors = visitChildren(accessors, newCh)
                    )
                    is Node.Decl.Property.Delegate -> copy(
                        byKeyword = visitChildren(byKeyword, newCh),
                        expr = visitChildren(expr, newCh),
                    )
                    is Node.Decl.Property.Variable.Single -> copy(
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.Decl.Property.Variable.Multi -> copy(
                        vars = visitChildren(vars, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Decl.Property.Accessor.Get -> copy(
                        mods = visitChildren(mods, newCh),
                        getKeyword = visitChildren(getKeyword, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                        postMods = visitChildren(postMods, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Decl.Property.Accessor.Set -> copy(
                        mods = visitChildren(mods, newCh),
                        setKeyword = visitChildren(setKeyword, newCh),
                        params = visitChildren(params, newCh),
                        postMods = visitChildren(postMods, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Decl.Property.Accessor.Params -> copy(
                        params = visitChildren(params, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Decl.TypeAlias -> copy(
                        mods = visitChildren(mods, newCh),
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.Decl.SecondaryConstructor -> copy(
                        mods = visitChildren(mods, newCh),
                        constructorKeyword = visitChildren(constructorKeyword, newCh),
                        params = visitChildren(params, newCh),
                        delegationCall = visitChildren(delegationCall, newCh),
                        block = visitChildren(block, newCh)
                    )
                    is Node.Decl.SecondaryConstructor.DelegationCall -> copy(
                        args = visitChildren(args, newCh)
                    )
                    is Node.Decl.EnumEntry -> copy(
                        mods = visitChildren(mods, newCh),
                        name = visitChildren(name, newCh),
                        args = visitChildren(args, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Initializer -> copy(
                        equals = visitChildren(equals, newCh),
                        expr = visitChildren(expr, newCh),
                    )
                    is Node.TypeParams -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.TypeParam -> copy(
                        mods = visitChildren(mods, newCh),
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.TypeArgs -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.TypeArg.Asterisk -> copy(
                        asterisk = visitChildren(asterisk, newCh),
                    )
                    is Node.TypeArg.Type -> copy(
                        mods = visitChildren(mods, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.TypeRef -> copy(
                        lPar = visitChildren(lPar, newCh),
                        contextReceivers = visitChildren(contextReceivers, newCh),
                        mods = visitChildren(mods, newCh),
                        innerLPar = visitChildren(innerLPar, newCh),
                        innerMods = visitChildren(innerMods, newCh),
                        type = visitChildren(type, newCh),
                        innerRPar = visitChildren(innerRPar, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.Type.Func -> copy(
                        receiver = visitChildren(receiver, newCh),
                        params = visitChildren(params, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.Type.Func.Receiver -> copy(
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.Type.Func.Params -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Type.Func.Param -> copy(
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
                        mods = visitChildren(mods, newCh),
                        type = visitChildren(type, newCh),
                        rPar = visitChildren(rPar, newCh),
                    )
                    is Node.Type.Dynamic -> this
                    is Node.ContextReceivers -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.ContextReceiver -> copy(
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.ConstructorCallee -> copy(
                        type = visitChildren(type, newCh),
                    )
                    is Node.ValueArgs -> copy(
                        args = visitChildren(args, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.ValueArgs.ValueArg -> copy(
                        name = visitChildren(name, newCh),
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Container -> copy(
                        expr = visitChildren(expr, newCh),
                    )
                    is Node.Expr.If -> copy(
                        ifKeyword = visitChildren(ifKeyword, newCh),
                        condition = visitChildren(condition, newCh),
                        body = visitChildren(body, newCh),
                        elseBody = visitChildren(elseBody, newCh)
                    )
                    is Node.Expr.Try -> copy(
                        block = visitChildren(block, newCh),
                        catches = visitChildren(catches, newCh),
                        finallyBlock = visitChildren(finallyBlock, newCh)
                    )
                    is Node.Expr.Try.Catch -> copy(
                        catchKeyword = visitChildren(catchKeyword, newCh),
                        params = visitChildren(params, newCh),
                        block = visitChildren(block, newCh),
                    )
                    is Node.Expr.For -> copy(
                        forKeyword = visitChildren(forKeyword, newCh),
                        anns = visitChildren(anns, newCh),
                        loopParam = visitChildren(loopParam, newCh),
                        loopRange = visitChildren(loopRange, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Expr.While -> copy(
                        whileKeyword = visitChildren(whileKeyword, newCh),
                        condition = visitChildren(condition, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Expr.BinaryOp -> copy(
                        lhs = visitChildren(lhs, newCh),
                        oper = visitChildren(oper, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.Expr.BinaryOp.Oper.Infix -> this
                    is Node.Expr.BinaryOp.Oper.Token -> this
                    is Node.Expr.UnaryOp -> copy(
                        expr = visitChildren(expr, newCh),
                        oper = visitChildren(oper, newCh)
                    )
                    is Node.Expr.UnaryOp.Oper -> this
                    is Node.Expr.TypeOp -> copy(
                        lhs = visitChildren(lhs, newCh),
                        oper = visitChildren(oper, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.Expr.TypeOp.Oper -> this
                    is Node.Expr.DoubleColonRef.Callable -> copy(
                        recv = visitChildren(recv, newCh),
                        name = visitChildren(name, newCh),
                    )
                    is Node.Expr.DoubleColonRef.Class -> copy(
                        recv = visitChildren(recv, newCh)
                    )
                    is Node.Expr.DoubleColonRef.Recv.Expr -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.DoubleColonRef.Recv.Type -> copy(
                        type = visitChildren(type, newCh),
                        questionMarks = visitChildren(questionMarks, newCh),
                    )
                    is Node.Expr.Paren -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.StringTmpl -> copy(
                        elems = visitChildren(elems, newCh)
                    )
                    is Node.Expr.StringTmpl.Elem.Regular -> this
                    is Node.Expr.StringTmpl.Elem.ShortTmpl -> this
                    is Node.Expr.StringTmpl.Elem.UnicodeEsc -> this
                    is Node.Expr.StringTmpl.Elem.RegularEsc -> this
                    is Node.Expr.StringTmpl.Elem.LongTmpl -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.Const -> this
                    is Node.Expr.Lambda -> copy(
                        params = visitChildren(params, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Expr.Lambda.Params -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Expr.Lambda.Param.Single -> copy(
                        name = visitChildren(name, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.Expr.Lambda.Param.Multi -> copy(
                        vars = visitChildren(vars, newCh),
                        destructTypeRef = visitChildren(destructTypeRef, newCh)
                    )
                    is Node.Expr.Lambda.Param.Multi.Variables -> copy(
                        elements = visitChildren(elements, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Expr.Lambda.Body -> copy(
                        statements = visitChildren(statements, newCh)
                    )
                    is Node.Expr.This -> this
                    is Node.Expr.Super -> copy(
                        typeArg = visitChildren(typeArg, newCh)
                    )
                    is Node.Expr.When -> copy(
                        lPar = visitChildren(lPar, newCh),
                        expr = visitChildren(expr, newCh),
                        rPar = visitChildren(rPar, newCh),
                        entries = visitChildren(entries, newCh),
                    )
                    is Node.Expr.When.Entry.Conds -> copy(
                        conds = visitChildren(conds, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Expr.When.Entry.Else -> copy(
                        elseKeyword = visitChildren(elseKeyword, newCh),
                        body = visitChildren(body, newCh),
                    )
                    is Node.Expr.When.Cond.Expr -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.When.Cond.In -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.When.Cond.Is -> copy(
                        typeRef = visitChildren(typeRef, newCh)
                    )
                    is Node.Expr.Object -> copy(
                        decl = visitChildren(decl, newCh),
                    )
                    is Node.Expr.Throw -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.Return -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.Continue -> this
                    is Node.Expr.Break -> this
                    is Node.Expr.CollLit -> copy(
                        exprs = visitChildren(exprs, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Expr.Name -> this
                    is Node.Expr.Labeled -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.Annotated -> copy(
                        anns = visitChildren(anns, newCh),
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.Call -> copy(
                        expr = visitChildren(expr, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        args = visitChildren(args, newCh),
                        lambdaArgs = visitChildren(lambdaArgs, newCh)
                    )
                    is Node.Expr.Call.LambdaArg -> copy(
                        anns = visitChildren(anns, newCh),
                        func = visitChildren(func, newCh)
                    )
                    is Node.Expr.ArrayAccess -> copy(
                        expr = visitChildren(expr, newCh),
                        indices = visitChildren(indices, newCh),
                        trailingComma = visitChildren(trailingComma, newCh),
                    )
                    is Node.Expr.AnonFunc -> copy(
                        func = visitChildren(func, newCh)
                    )
                    is Node.Expr.Property -> copy(
                        decl = visitChildren(decl, newCh)
                    )
                    is Node.Expr.Block -> copy(
                        statements = visitChildren(statements, newCh)
                    )
                    is Node.Modifiers -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.Modifier.AnnotationSet -> copy(
                        atSymbol = visitChildren(atSymbol, newCh),
                        lBracket = visitChildren(lBracket, newCh),
                        anns = visitChildren(anns, newCh),
                        rBracket = visitChildren(rBracket, newCh),
                    )
                    is Node.Modifier.AnnotationSet.Annotation -> copy(
                        constructorCallee = visitChildren(constructorCallee, newCh),
                        args = visitChildren(args, newCh),
                    )
                    is Node.Modifier.Lit -> this
                    is Node.PostModifier.TypeConstraints -> copy(
                        whereKeyword = visitChildren(whereKeyword, newCh),
                        constraints = visitChildren(constraints, newCh),
                    )
                    is Node.PostModifier.TypeConstraints.TypeConstraintList -> copy(
                        elements = visitChildren(elements, newCh),
                    )
                    is Node.PostModifier.TypeConstraints.TypeConstraint -> copy(
                        anns = visitChildren(anns, newCh),
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
                        expr = visitChildren(expr, newCh),
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

    protected fun <T : Node?> Node?.visitChildren(v: T, ch: ChangedRef): T = visit(v, this!!, ch)

    protected fun <T : Node?> Node?.visitChildren(v: List<T>, ch: ChangedRef): List<T> = ch.sub { newCh ->
        val newList = v.map { orig -> visit(orig, this!!, newCh).also { newCh.markIf(it, orig) } }
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
        fun <T : Node> preVisit(v: T, fn: (v: Node?, parent: Node) -> Node?) = object : MutableVisitor() {
            override fun <T : Node?> preVisit(v: T, parent: Node): T = fn(v, parent) as T
        }.visit(v, v)

        fun <T : Node> postVisit(v: T, fn: (v: Node?, parent: Node) -> Node?) = object : MutableVisitor() {
            override fun <T : Node?> postVisit(v: T, parent: Node): T = fn(v, parent) as T
        }.visit(v, v)
    }
}