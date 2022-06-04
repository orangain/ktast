package kastree.ast

open class MutableVisitor {

    open fun <T: Node?> preVisit(v: T, parent: Node): T = v
    open fun <T: Node?> postVisit(v: T, parent: Node): T = v

    open fun <T: Node?> visit(v: T, parent: Node, ch: ChangedRef = ChangedRef(false)): T = v.run {
        ch.sub { newCh ->
            preVisit(this, parent)?.run {
                val new: Node = when (this) {
                    is Node.NodeList<*> -> copy(
                        children = visitChildren(children, newCh),
                    )
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
                        packageNameExpr = visitChildren(packageNameExpr, newCh),
                    )
                    is Node.Import -> copy(
                        importKeyword = visitChildren(importKeyword, newCh),
                    )
                    is Node.Decl.Structured -> copy(
                        mods = visitChildren(mods, newCh),
                        declarationKeyword = visitChildren(declarationKeyword, newCh),
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        primaryConstructor = visitChildren(primaryConstructor, newCh),
                        colon = visitChildren(colon, newCh),
                        parentAnns = visitChildren(parentAnns, newCh),
                        parents = visitChildren(parents, newCh),
                        typeConstraints = visitChildren(typeConstraints, newCh),
                        body = visitChildren(body, newCh)
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
                    is Node.Decl.Init -> copy(
                        block = visitChildren(block, newCh)
                    )
                    is Node.Decl.Func -> copy(
                        mods = visitChildren(mods, newCh),
                        funKeyword = visitChildren(funKeyword, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        receiverType = visitChildren(receiverType, newCh),
                        name = visitChildren(name, newCh),
                        paramTypeParams = visitChildren(paramTypeParams, newCh),
                        params = visitChildren(params, newCh),
                        type = visitChildren(type, newCh),
                        typeConstraints = visitChildren(typeConstraints, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Decl.Func.Params -> copy(
                        params = visitChildren(params, newCh),
                    )
                    is Node.Decl.Func.Params.Param -> copy(
                        mods = visitChildren(mods, newCh),
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh),
                        initializer = visitChildren(initializer, newCh)
                    )
                    is Node.Decl.Func.Body.Block -> copy(
                        block = visitChildren(block, newCh)
                    )
                    is Node.Decl.Func.Body.Expr -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Decl.Property -> copy(
                        mods = visitChildren(mods, newCh),
                        valOrVar = visitChildren(valOrVar, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        receiverType = visitChildren(receiverType, newCh),
                        vars = visitChildren(vars, newCh),
                        typeConstraints = visitChildren(typeConstraints, newCh),
                        initializer = visitChildren(initializer, newCh),
                        delegate = visitChildren(delegate, newCh),
                        accessors = visitChildren(accessors, newCh)
                    )
                    is Node.Decl.Property.Delegate -> copy(
                        byKeyword = visitChildren(byKeyword, newCh),
                        expr = visitChildren(expr, newCh),
                    )
                    is Node.Decl.Property.Var -> copy(
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh)
                    )
                    is Node.Decl.Property.Accessors -> copy(
                        first = visitChildren(first, newCh),
                        second = visitChildren(second, newCh)
                    )
                    is Node.Decl.Property.Accessor.Get -> copy(
                        mods = visitChildren(mods, newCh),
                        type = visitChildren(type, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Decl.Property.Accessor.Set -> copy(
                        mods = visitChildren(mods, newCh),
                        paramMods = visitChildren(paramMods, newCh),
                        paramName = visitChildren(paramName, newCh),
                        paramType = visitChildren(paramType, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Decl.TypeAlias -> copy(
                        mods = visitChildren(mods, newCh),
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh),
                        type = visitChildren(type, newCh)
                    )
                    is Node.Decl.Constructor -> copy(
                        mods = visitChildren(mods, newCh),
                        constructorKeyword = visitChildren(constructorKeyword, newCh),
                        params = visitChildren(params, newCh),
                        delegationCall = visitChildren(delegationCall, newCh),
                        block = visitChildren(block, newCh)
                    )
                    is Node.Decl.Constructor.DelegationCall -> copy(
                        args = visitChildren(args, newCh)
                    )
                    is Node.Decl.EnumEntry -> copy(
                        mods = visitChildren(mods, newCh),
                        name = visitChildren(name, newCh),
                        args = visitChildren(args, newCh),
                        members = visitChildren(members, newCh)
                    )
                    is Node.Initializer -> copy(
                        equals = visitChildren(equals, newCh),
                        expr = visitChildren(expr, newCh),
                    )
                    is Node.TypeParams -> copy(
                        params = visitChildren(params, newCh),
                    )
                    is Node.TypeParams.TypeParam -> copy(
                        mods = visitChildren(mods, newCh),
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh)
                    )
                    is Node.TypeConstraint -> copy(
                        anns = visitChildren(anns, newCh),
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh)
                    )
                    is Node.Type.Func -> copy(
                        receiverType = visitChildren(receiverType, newCh),
                        params = visitChildren(params, newCh),
                        type = visitChildren(type, newCh)
                    )
                    is Node.Type.Func.Param -> copy(
                        name = visitChildren(name, newCh),
                        type = visitChildren(type, newCh)
                    )
                    is Node.Type.Simple -> copy(
                        pieces = visitChildren(pieces, newCh)
                    )
                    is Node.Type.Simple.Piece -> copy(
                        name = visitChildren(name, newCh),
                        typeParams = visitChildren(typeParams, newCh)
                    )
                    is Node.Type.Nullable -> copy(
                        lpar = visitChildren(lpar, newCh),
                        mods = visitChildren(mods, newCh),
                        type = visitChildren(type, newCh),
                        rpar = visitChildren(rpar, newCh),
                    )
                    is Node.Type.Dynamic -> this
                    is Node.TypeProjection -> copy(
                        mods = visitChildren(mods, newCh),
                        typeRef = visitChildren(typeRef, newCh),
                    )
                    is Node.TypeRef -> copy(
                        mods = visitChildren(mods, newCh),
                        lpar = visitChildren(lpar, newCh),
                        innerMods = visitChildren(innerMods, newCh),
                        ref = visitChildren(ref, newCh),
                        rpar = visitChildren(rpar, newCh),
                    )
                    is Node.ValueArgs -> copy(
                        args = visitChildren(args, newCh),
                    )
                    is Node.ValueArgs.ValueArg -> copy(
                        name = visitChildren(name, newCh),
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.If -> copy(
                        lpar = visitChildren(lpar, newCh),
                        expr = visitChildren(expr, newCh),
                        rpar = visitChildren(rpar, newCh),
                        body = visitChildren(body, newCh),
                        elseBody = visitChildren(elseBody, newCh)
                    )
                    is Node.Expr.Try -> copy(
                        block = visitChildren(block, newCh),
                        catches = visitChildren(catches, newCh),
                        finallyBlock = visitChildren(finallyBlock, newCh)
                    )
                    is Node.Expr.Try.Catch -> copy(
                        anns = visitChildren(anns, newCh),
                        varType = visitChildren(varType, newCh),
                        block = visitChildren(block, newCh)
                    )
                    is Node.Expr.For -> copy(
                        anns = visitChildren(anns, newCh),
                        vars = visitChildren(vars, newCh),
                        inExpr = visitChildren(inExpr, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Expr.While -> copy(
                        expr = visitChildren(expr, newCh),
                        body = visitChildren(body, newCh)
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
                        type = visitChildren(type, newCh)
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
                    is Node.Expr.Lambda.Param -> copy(
                        vars = visitChildren(vars, newCh),
                        destructType = visitChildren(destructType, newCh)
                    )
                    is Node.Expr.Lambda.Body -> copy(
                        stmts = visitChildren(stmts, newCh)
                    )
                    is Node.Expr.This -> this
                    is Node.Expr.Super -> copy(
                        typeArg = visitChildren(typeArg, newCh)
                    )
                    is Node.Expr.When -> copy(
                        expr = visitChildren(expr, newCh),
                        entries = visitChildren(entries, newCh)
                    )
                    is Node.Expr.When.Entry -> copy(
                        conds = visitChildren(conds, newCh),
                        body = visitChildren(body, newCh)
                    )
                    is Node.Expr.When.Cond.Expr -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.When.Cond.In -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.When.Cond.Is -> copy(
                        type = visitChildren(type, newCh)
                    )
                    is Node.Expr.Object -> copy(
                        parents = visitChildren(parents, newCh),
                        members = visitChildren(members, newCh)
                    )
                    is Node.Expr.Throw -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.Return -> copy(
                        returnKeyword = visitChildren(returnKeyword, newCh),
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.Continue -> this
                    is Node.Expr.Break -> this
                    is Node.Expr.CollLit -> copy(
                        exprs = visitChildren(exprs, newCh)
                    )
                    is Node.Expr.Name -> this
                    is Node.Expr.Labeled -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.Annotated -> copy(
                        anns = visitChildren(anns, newCh),
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Expr.TypeOp -> copy (
                        lhs = visitChildren(lhs, newCh),
                        oper = visitChildren(oper, newCh),
                        rhs = visitChildren(rhs, newCh)
                    )
                    is Node.Expr.TypeOp.Oper -> this
                    is Node.Expr.Call -> copy(
                        expr = visitChildren(expr, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        args = visitChildren(args, newCh),
                        lambda = visitChildren(lambda, newCh)
                    )
                    is Node.Expr.Call.TrailLambda -> copy(
                        anns = visitChildren(anns, newCh),
                        func = visitChildren(func, newCh)
                    )
                    is Node.Expr.ArrayAccess -> copy(
                        expr = visitChildren(expr, newCh),
                        indices = visitChildren(indices, newCh)
                    )
                    is Node.Expr.AnonFunc -> copy(
                        func = visitChildren(func, newCh)
                    )
                    is Node.Expr.Property -> copy(
                        decl = visitChildren(decl, newCh)
                    )
                    is Node.Expr.Block -> copy(
                        stmts = visitChildren(stmts, newCh)
                    )
                    is Node.Stmt.Decl -> copy(
                        decl = visitChildren(decl, newCh)
                    )
                    is Node.Stmt.Expr -> copy(
                        expr = visitChildren(expr, newCh)
                    )
                    is Node.Modifier.AnnotationSet -> copy(
                        atSymbol = visitChildren(atSymbol, newCh),
                        lBracket = visitChildren(lBracket, newCh),
                        anns = visitChildren(anns, newCh),
                        rBracket = visitChildren(rBracket, newCh),
                    )
                    is Node.Modifier.AnnotationSet.Annotation -> copy(
                        nameTypeReference = visitChildren(nameTypeReference, newCh),
                        typeArgs = visitChildren(typeArgs, newCh),
                        args = visitChildren(args, newCh)
                    )
                    is Node.Modifier.Lit -> this
                    is Node.Keyword -> this
                    is Node.Extra -> this
                    else -> error("Unrecognized node: $this")
                }
                new.origOrChanged(this, newCh)
            }.let { postVisit(it, parent) as T }.also { newCh.markIf(this, it) }
        }
    }

    protected inline fun <T: Node?> Node?.visitChildren(v: T, ch: ChangedRef): T = visit(v, this!!, ch)

    protected inline fun <T: Node?> Node?.visitChildren(v: List<T>, ch: ChangedRef): List<T> = ch.sub { newCh ->
        val newList = v.map { orig -> visit(orig, this!!, newCh).also { newCh.markIf(it, orig) } }
        newList.origOrChanged(v, newCh)
    }

    protected inline fun <T> T.origOrChanged(orig: T, ref: ChangedRef) = if (ref.changed) this else orig

    open class ChangedRef(var changed: Boolean) {
        inline fun markIf(v1: Any?, v2: Any?) { if (v1 !== v2) changed = true }

        open fun <T> sub(fn: (ChangedRef) -> T): T = ChangedRef(false).let { newCh ->
            fn(newCh).also { if (newCh.changed) changed = true }
        }
    }

    companion object {
        fun <T: Node> preVisit(v: T, fn: (v: Node?, parent: Node) -> Node?) = object : MutableVisitor() {
            override fun <T : Node?> preVisit(v: T, parent: Node): T = fn(v, parent) as T
        }.visit(v, v)

        fun <T: Node> postVisit(v: T, fn: (v: Node?, parent: Node) -> Node?) = object : MutableVisitor() {
            override fun <T : Node?> postVisit(v: T, parent: Node): T = fn(v, parent) as T
        }.visit(v, v)
    }
}