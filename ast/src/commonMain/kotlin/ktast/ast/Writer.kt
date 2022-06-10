package ktast.ast

open class Writer(
    val app: Appendable = StringBuilder(),
    val extrasMap: ExtrasMap? = null
) : Visitor() {
    protected fun append(ch: Char) = also { app.append(ch) }
    protected fun append(str: String) = also { app.append(str) }
    protected fun appendName(name: String) = append(name)
    protected fun appendNames(names: List<String>, sep: String) = also {
        names.forEachIndexed { index, name ->
            if (index > 0) append(sep)
            appendName(name)
        }
    }

    fun write(v: Node) {
        visit(v, v)
    }

    override fun visit(v: Node?, parent: Node) {
        v?.writeExtrasBefore()
        v?.apply {
            when (this) {
                is Node.NodeList<*> -> {
                    append(prefix)

                    // First, do all the enum entries if there are any
                    val enumEntries = children.takeWhile { it is Node.Decl.EnumEntry }
                    if (enumEntries.isNotEmpty()) {
                        children(enumEntries) // Each EnumEntry has contains commas and trailing semicolon.
                    }

                    // Now the rest of the members
                    val restChildren = children.drop(enumEntries.size)
                    children(restChildren, separator)
                    children(trailingSeparator)

                    writeExtrasWithin()
                    append(suffix)
                }
                is Node.File -> {
                    if (anns.isNotEmpty()) childAnns()
                    children(pkg)
                    children(imports)
                    children(decls)
                }
                is Node.Package -> {
                    children(mods).append("package")
                    children(packageNameExpr)
                }
                is Node.Import -> {
                    children(importKeyword)
                    children(names, ".")
                    children(alias)
                }
                is Node.Import.Alias -> {
                    append("as")
                    children(name)
                }
                is Node.Decl.Structured -> {
                    children(mods)
                    children(declarationKeyword)
                    children(name)
                    children(typeParams)
                    children(primaryConstructor)
                    children(colon)
                    children(parentAnns)
                    children(parents, ",")
                    childTypeConstraints(typeConstraints)
                    children(body)
                }
                is Node.Decl.Structured.Parent.CallConstructor -> {
                    children(type)
                    parenChildren(args)
                }
                is Node.Decl.Structured.Parent.DelegatedType -> {
                    children(type)
                    children(byKeyword)
                    children(expr)
                }
                is Node.Decl.Structured.Parent.Type -> {
                    children(type)
                }
                is Node.Decl.Structured.PrimaryConstructor -> {
                    children(mods)
                    children(constructorKeyword)
                    children(params)
                }
                is Node.Decl.Structured.Body -> {
                    children(decls, prefix = "{", suffix = "}")
                }
                is Node.Decl.Init -> {
                    children(mods)
                    append("init")
                    children(block)
                }
                is Node.Decl.Func -> {
                    children(mods)
                    children(funKeyword)
                    children(typeParams)
                    if (receiverTypeRef != null) children(receiverTypeRef).append(".")
                    name?.also { children(it) }
                    children(paramTypeParams)
                    children(params)
                    if (typeRef != null) append(":").also { children(typeRef) }
                    children(postMods)
                    children(body)
                }
                is Node.Decl.Func.Params -> {
                    children(params, ",", "(", ")", trailingComma, this)
                }
                is Node.Decl.Func.Params.Param -> {
                    children(mods)
                    children(valOrVar)
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                    children(initializer)
                }
                is Node.Decl.Func.Body.Block ->
                    children(block)
                is Node.Decl.Func.Body.Expr ->
                    children(equals, expr)
                is Node.Decl.Property -> {
                    children(mods)
                    children(valOrVar)
                    children(typeParams)
                    if (receiverTypeRef != null) children(receiverTypeRef).append('.')
                    childVars(vars, trailingComma)
                    childTypeConstraints(typeConstraints)
                    children(initializer)
                    children(delegate)
                    if (accessors != null) children(accessors)
                }
                is Node.Decl.Property.Delegate -> {
                    children(byKeyword)
                    children(expr)
                }
                is Node.Decl.Property.Var -> {
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                }
                is Node.Decl.Property.Accessors -> {
                    children(first)
                    if (second != null) children(second)
                }
                is Node.Decl.Property.Accessor.Get -> {
                    children(mods).append("get")
                    if (body != null) {
                        append("()")
                        if (typeRef != null) append(":").also { children(typeRef) }
                        children(postMods)
                        children(body)
                    }
                }
                is Node.Decl.Property.Accessor.Set -> {
                    children(mods).append("set")
                    if (body != null) {
                        append("(")
                        children(params)
                        append(")")
                        children(postMods)
                        children(body)
                    }
                }
                is Node.Decl.Property.Accessor.Params -> {
                    children(params, ",")
                    children(trailingComma)
                }
                is Node.Decl.TypeAlias -> {
                    children(mods).append("typealias")
                    children(name)
                    children(typeParams).append("=")
                    children(typeRef)
                }
                is Node.Decl.Constructor -> {
                    children(mods)
                    children(constructorKeyword)
                    children(params)
                    if (delegationCall != null) append(":").also { children(delegationCall) }
                    children(block)
                }
                is Node.Decl.Constructor.DelegationCall ->
                    append(target.name.lowercase()).also { parenChildren(args) }
                is Node.Decl.EnumEntry -> {
                    children(mods)
                    children(name)
                    children(args)
                    children(body)
                    if (hasComma) {
                        append(",")
                    }
                    writeExtrasWithin()
                }
                is Node.Initializer -> {
                    children(equals)
                    children(expr)
                }
                is Node.TypeParams -> {
                    bracketedChildren(params, trailingComma)
                }
                is Node.TypeParams.TypeParam -> {
                    children(mods)
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                }
                is Node.TypeProjection.Asterisk -> {
                    children(asterisk)
                }
                is Node.TypeProjection.Type -> {
                    children(mods)
                    children(typeRef)
                }
                is Node.TypeRef -> {
                    if (contextReceivers != null) {
                        append("context")
                        children(contextReceivers)
                    }
                    children(mods)
                    children(lPar)
                    children(innerMods)
                    children(type)
                    children(rPar)
                }
                is Node.Type.Func -> {
                    if (receiver != null) {
                        children(receiver)
                        if (receiver.typeRef.type != null) {
                            append('.')
                        }
                    }
                    if (params != null) {
                        children(params).append("->")
                    }
                    children(typeRef)
                }
                is Node.Type.Func.Receiver -> {
                    children(typeRef)
                }
                is Node.Type.Func.Param -> {
                    if (name != null) children(name).append(":")
                    children(typeRef)
                }
                is Node.Type.Simple ->
                    children(pieces, ".")
                is Node.Type.Simple.Piece -> {
                    children(name)
                    children(typeParams)
                }
                is Node.Type.Nullable -> {
                    children(lPar)
                    children(mods)
                    children(type)
                    children(rPar)
                    append('?')
                }
                is Node.Type.Dynamic ->
                    append("dynamic")
                is Node.ContextReceiver -> {
                    children(typeRef)
                }
                is Node.ConstructorCallee -> {
                    children(type)
                }
                is Node.Body -> {
                    children(expr)
                }
                is Node.ValueArgs -> {
                    children(args, ",", "(", ")", trailingComma, this)
                }
                is Node.ValueArgs.ValueArg -> {
                    if (name != null) children(name).append("=")
                    if (asterisk) append('*')
                    children(expr)
                }
                is Node.Expr.If -> {
                    append("if")
                    children(lPar, expr, rPar, body)
                    children(elseKeyword, elseBody)
                }
                is Node.Expr.Try -> {
                    append("try")
                    children(block)
                    if (catches.isNotEmpty()) children(catches, "", prefix = "")
                    if (finallyBlock != null) append(" finally ").also { children(finallyBlock) }
                }
                is Node.Expr.Try.Catch -> {
                    children(catchKeyword)
                    append("(")
                    childAnns(sameLine = true)
                    appendName(varName).append(": ")
                    children(varType)
                    children(trailingComma)
                    append(")")
                    children(block)
                }
                is Node.Expr.For -> {
                    append("for (")
                    childAnns(sameLine = true)
                    childVars(vars, null).append("in ").also { children(inExpr) }.append(")")
                    children(body)
                }
                is Node.Expr.While -> {
                    if (!doWhile) append("while (").also { children(expr) }.append(")").also { children(body) }
                    else append("do").also { children(body) }.append("while (").also { children(expr) }.append(')')
                }
                is Node.Expr.BinaryOp -> {
                    children(listOf(lhs, oper, rhs))
                }
                is Node.Expr.BinaryOp.Oper.Infix ->
                    append(str)
                is Node.Expr.BinaryOp.Oper.Token ->
                    append(token.str)
                is Node.Expr.UnaryOp ->
                    if (prefix) children(oper, expr) else children(expr, oper)
                is Node.Expr.UnaryOp.Oper ->
                    append(token.str)
                is Node.Expr.TypeOp ->
                    children(listOf(lhs, oper, rhs), "")
                is Node.Expr.TypeOp.Oper ->
                    append(token.str)
                is Node.Expr.DoubleColonRef.Callable -> {
                    if (recv != null) children(recv)
                    append("::")
                    children(name)
                }
                is Node.Expr.DoubleColonRef.Class -> {
                    if (recv != null) children(recv)
                    append("::class")
                }
                is Node.Expr.DoubleColonRef.Recv.Expr ->
                    children(expr)
                is Node.Expr.DoubleColonRef.Recv.Type -> {
                    children(type)
                    children(questionMarks)
                }
                is Node.Expr.Paren ->
                    append('(').also { children(expr) }.append(')')
                is Node.Expr.StringTmpl ->
                    if (raw) append("\"\"\"").also { children(elems) }.append("\"\"\"")
                    else append('"').also { children(elems) }.append('"')
                is Node.Expr.StringTmpl.Elem.Regular ->
                    append(str)
                is Node.Expr.StringTmpl.Elem.ShortTmpl ->
                    append('$').appendName(str)
                is Node.Expr.StringTmpl.Elem.UnicodeEsc ->
                    append("\\u").append(digits)
                is Node.Expr.StringTmpl.Elem.RegularEsc ->
                    append('\\').append(
                        when (char) {
                            '\b' -> 'b'
                            '\n' -> 'n'
                            '\t' -> 't'
                            '\r' -> 'r'
                            else -> char
                        }
                    )
                is Node.Expr.StringTmpl.Elem.LongTmpl ->
                    append("\${").also { children(expr) }.append('}')
                is Node.Expr.Const ->
                    append(value)
                is Node.Expr.Lambda -> {
                    append("{")
                    if (params != null) {
                        children(params)
                        append("->")
                    }
                    children(body)
                    append("}")
                }
                is Node.Expr.Lambda.Param.Single -> {
                    children(variable)
                }
                is Node.Expr.Lambda.Param.Multi -> {
                    children(vars)
                    if (destructTypeRef != null) append(":").also { children(destructTypeRef) }
                }
                is Node.Expr.Lambda.Body -> {
                    if (stmts.isNotEmpty()) {
                        children(stmts)
                    }
                    writeExtrasWithin()
                }
                is Node.Expr.This -> {
                    append("this")
                    if (label != null) append('@').appendName(label)
                }
                is Node.Expr.Super -> {
                    append("super")
                    if (typeArg != null) append('<').also { children(typeArg) }.append('>')
                    if (label != null) append('@').appendName(label)
                }
                is Node.Expr.When -> {
                    append("when")
                    children(lPar, expr, rPar)
                    append("{")
                    children(entries)
                    append("}")
                }
                is Node.Expr.When.Entry.Conds -> {
                    children(conds, ",", trailingSeparator = trailingComma)
                    append("->").also { children(body) }
                }
                is Node.Expr.When.Entry.Else -> {
                    children(elseKeyword)
                    append("->").also { children(body) }
                }
                is Node.Expr.When.Cond.Expr ->
                    children(expr)
                is Node.Expr.When.Cond.In -> {
                    if (not) append('!')
                    append("in").also { children(expr) }
                }
                is Node.Expr.When.Cond.Is -> {
                    if (not) append('!')
                    append("is").also { children(typeRef) }
                }
                is Node.Expr.Object -> {
                    children(decl)
                }
                is Node.Expr.Throw ->
                    append("throw").also { children(expr) }
                is Node.Expr.Return -> {
                    children(returnKeyword)
                    if (label != null) append('@').appendName(label)
                    children(expr)
                }
                is Node.Expr.Continue -> {
                    append("continue")
                    if (label != null) append('@').appendName(label)
                }
                is Node.Expr.Break -> {
                    append("break")
                    if (label != null) append('@').appendName(label)
                }
                is Node.Expr.CollLit ->
                    children(exprs, ",", "[", "]", trailingComma)
                is Node.Expr.Name ->
                    appendName(name)
                is Node.Expr.Labeled ->
                    appendName(label).append("@").also { children(expr) }
                is Node.Expr.Annotated ->
                    childAnns().also { children(expr) }
                is Node.Expr.Call -> {
                    children(expr)
                    children(typeArgs)
                    children(args)
                    children(lambda)
                }
                is Node.Expr.Call.TrailLambda -> {
                    if (anns.isNotEmpty()) childAnns(sameLine = true)
                    if (label != null) appendName(label).append("@")
                    children(func)
                }
                is Node.Expr.ArrayAccess -> {
                    children(expr)
                    children(indices, ",", "[", "]", trailingComma)
                }
                is Node.Expr.AnonFunc ->
                    children(func)
                is Node.Expr.Property ->
                    children(decl)
                is Node.Expr.Block -> {
                    append("{").run {
                        if (stmts.isNotEmpty()) {
                            children(stmts)
                        }
                        writeExtrasWithin()
                    }
                    append("}")
                }
                is Node.Stmt.Decl -> {
                    children(decl)
                }
                is Node.Stmt.Expr ->
                    children(expr)
                is Node.Modifier.AnnotationSet -> {
                    children(atSymbol)
                    if (target != null) append(target.name.lowercase()).append(':')
                    children(lBracket)
                    children(anns)
                    children(rBracket)
                }
                is Node.Modifier.AnnotationSet.Annotation -> {
                    children(constructorCallee)
                    children(args)
                }
                is Node.Modifier.Lit ->
                    append(keyword.name.lowercase())
                is Node.PostModifier.TypeConstraints -> {
                    children(whereKeyword)
                    children(constraints)
                }
                is Node.PostModifier.TypeConstraints.TypeConstraint -> {
                    childAnns(sameLine = true)
                    children(name)
                    append(":")
                    children(typeRef)
                }
                is Node.PostModifier.Contract -> {
                    children(contractKeyword)
                    children(contractEffects)
                }
                is Node.PostModifier.Contract.ContractEffect -> {
                    children(expr)
                }
                is Node.Keyword -> append(value)
                else ->
                    error("Unrecognized node type: $this")
            }
        }
        v?.writeExtrasAfter()
    }

    protected open fun Node.writeExtrasBefore() {
        if (extrasMap == null) return
        // Write everything before
        writeExtras(extrasMap.extrasBefore(this))
    }

    protected open fun Node.writeExtrasWithin() {
        if (extrasMap == null) return
        // Write everything within
        writeExtras(extrasMap.extrasWithin(this))
    }

    protected open fun Node.writeExtrasAfter() {
        if (extrasMap == null) return
        // Write everything after that doesn't start a line or end a line
        writeExtras(extrasMap.extrasAfter(this))
    }

    protected open fun Node.writeExtras(extras: List<Node.Extra>) {
        extras.forEach {
            append(it.text)
        }
    }

    protected fun Node.childTypeConstraints(v: Node.NodeList<Node.PostModifier.TypeConstraints.TypeConstraint>?) =
        this@Writer.also {
            if (v != null) append("where").also { children(v) }
        }

    protected fun Node.childVars(vars: List<Node.Decl.Property.Var>, trailingComma: Node.Keyword.Comma?) =
        if (vars.size == 1 && trailingComma == null) {
            children(vars)
        } else {
            append('(')
            vars.forEachIndexed { index, v ->
                children(v)
                if (index < vars.size - 1) append(",")
            }
            children(trailingComma)
            append(')')
        }

    // Ends with newline (or space if sameLine) if there are any
    protected fun Node.WithAnnotations.childAnns(sameLine: Boolean = false) = this@Writer.also {
        if (anns.isNotEmpty()) (this@childAnns as Node).apply {
            if (sameLine) children(anns, " ")
            else anns.forEach { ann -> children(ann) }
        }
    }

    // Null list values are asterisks
    protected fun Node.bracketedChildren(v: List<Node>, trailingComma: Node.Keyword.Comma?) = this@Writer.also {
        if (v.isNotEmpty()) {
            append('<')
            v.forEachIndexed { index, node ->
                if (index > 0) append(",")
                children(node)
            }
            children(trailingComma)
            append('>')
        }
    }

    protected fun Node.parenChildren(v: Node.ValueArgs?) = v?.args?.let { children(it, ",", "(", ")") }

    protected fun Node.children(vararg v: Node?) = this@Writer.also { v.forEach { visitChildren(it) } }

    protected fun Node.children(
        v: List<Node?>,
        sep: String = "",
        prefix: String = "",
        suffix: String = "",
        trailingSeparator: Node? = null,
        parent: Node? = null,
    ) =
        this@Writer.also {
            append(prefix)
            v.forEachIndexed { index, t ->
                visit(t, this)
                if (index < v.size - 1) append(sep)
            }
            children(trailingSeparator)
            parent?.writeExtrasWithin()
            append(suffix)
        }

    companion object {
        fun write(v: Node, extrasMap: ExtrasMap? = null) =
            write(v, StringBuilder(), extrasMap).toString()

        fun <T : Appendable> write(v: Node, app: T, extrasMap: ExtrasMap? = null) =
            app.also { Writer(it, extrasMap).write(v) }
    }
}
