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
                    childMods().append("package")
                    children(packageNameExpr)
                }
                is Node.Import -> {
                    children(importKeyword)
                    appendNames(names, ".")
                    if (wildcard) append(".*") else if (alias != null) append(" as ").appendName(alias)
                }
                is Node.Decl.Structured -> childMods().also {
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
                    childMods()
                    children(constructorKeyword)
                    children(params)
                }
                is Node.Decl.Init ->
                    append("init").also { children(block) }
                is Node.Decl.Func -> {
                    childMods()
                    children(funKeyword)
                    children(typeParams)
                    if (receiverTypeRef != null) children(receiverTypeRef).append(".")
                    name?.also { children(it) }
                    children(paramTypeParams)
                    children(params)
                    if (typeRef != null) append(":").also { children(typeRef) }
                    childTypeConstraints(typeConstraints)
                    if (body != null) {
                        children(body)
                    }
                }
                is Node.Decl.Func.Params -> {
                    parenChildren(params)
                }
                is Node.Decl.Func.Params.Param -> {
                    if (mods.isNotEmpty()) childMods()
                    if (readOnly == true) append("val") else if (readOnly == false) append("var")
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                    children(initializer)
                }
                is Node.Decl.Func.Body.Block ->
                    children(block)
                is Node.Decl.Func.Body.Expr ->
                    append("=").also { children(expr) }
                is Node.Decl.Property -> {
                    childMods()
                    children(valOrVar)
                    children(typeParams)
                    if (receiverTypeRef != null) children(receiverTypeRef).append('.')
                    childVars(vars)
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
                    childMods().append("get")
                    if (body != null) {
                        append("()")
                        if (typeRef != null) append(":").also { children(typeRef) }
                        children(body)
                    }
                }
                is Node.Decl.Property.Accessor.Set -> {
                    childMods().append("set")
                    if (body != null) {
                        append('(')
                        children(paramMods)
                        children(paramName ?: error("Missing setter param name when body present"))
                        if (paramTypeRef != null) append(":").also { children(paramTypeRef) }
                        append(")")
                        children(body)
                    }
                }
                is Node.Decl.TypeAlias -> {
                    childMods().append("typealias")
                    children(name)
                    children(typeParams).append("=")
                    children(typeRef)
                }
                is Node.Decl.Constructor -> {
                    childMods()
                    children(constructorKeyword)
                    children(params)
                    if (delegationCall != null) append(":").also { children(delegationCall) }
                    children(block)
                }
                is Node.Decl.Constructor.DelegationCall ->
                    append(target.name.lowercase()).also { parenChildren(args) }
                is Node.Decl.EnumEntry -> {
                    childMods()
                    children(name)
                    if (args != null) parenChildren(args)
                    if (members.isNotEmpty()) append("{").run {
                        children(members)
                    }.append("}")
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
                    bracketedChildren(params)
                }
                is Node.TypeParams.TypeParam -> {
                    childMods()
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                }
                is Node.TypeProjection -> {
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
                is Node.TypeConstraint -> {
                    childAnns(sameLine = true)
                    children(name)
                    append(":")
                    children(typeRef)
                }
                is Node.Type.Func -> {
                    if (receiverTypeRef != null) {
                        children(receiverTypeRef)
                        if (receiverTypeRef.type != null) {
                            append('.')
                        }
                    }
                    if (params != null) {
                        children(params).append("->")
                    }
                    children(typeRef)
                }
                is Node.Type.Func.Param -> {
                    if (name != null) children(name).append(":")
                    children(typeRef)
                }
                is Node.Type.Simple ->
                    children(pieces, ".")
                is Node.Type.Simple.Piece ->
                    children(name).also { bracketedChildren(typeParams) }
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
                is Node.ValueArgs -> {
                    children(args)
                }
                is Node.ValueArgs.ValueArg -> {
                    if (name != null) children(name).append("=")
                    if (asterisk) append('*')
                    children(expr)
                }
                is Node.Expr.If -> {
                    append("if")
                    children(lPar, expr, rPar, body)
                    if (elseBody != null) append(" else ").also { children(elseBody) }
                }
                is Node.Expr.Try -> {
                    append("try")
                    children(block)
                    if (catches.isNotEmpty()) children(catches, "", prefix = "")
                    if (finallyBlock != null) append(" finally ").also { children(finallyBlock) }
                }
                is Node.Expr.Try.Catch -> {
                    append("catch (")
                    childAnns(sameLine = true)
                    appendName(varName).append(": ").also { children(varType) }.append(")")
                    children(block)
                }
                is Node.Expr.For -> {
                    append("for (")
                    childAnns(sameLine = true)
                    childVars(vars).append("in ").also { children(inExpr) }.append(") ")
                    children(body)
                }
                is Node.Expr.While -> {
                    if (!doWhile) append("while (").also { children(expr) }.append(") ").also { children(body) }
                    else append("do ").also { children(body) }.append(" while (").also { children(expr) }.append(')')
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
                is Node.Expr.DoubleColonRef.Recv.Type ->
                    children(type).append("?".repeat(questionMarks))
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
                is Node.Expr.Lambda.Param -> {
                    childVars(vars)
                    if (destructTypeRef != null) append(": ").also { children(destructTypeRef) }
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
                is Node.Expr.When.Entry -> {
                    if (conds.isEmpty()) append("else")
                    else children(conds, ",")
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
                    children(exprs, ",", "[", "]")
                is Node.Expr.Name ->
                    appendName(name)
                is Node.Expr.Labeled ->
                    appendName(label).append("@").also { children(expr) }
                is Node.Expr.Annotated ->
                    childAnns().also { children(expr) }
                is Node.Expr.Call -> {
                    children(expr)
                    bracketedChildren(typeArgs)
                    if (args != null || lambda == null) parenChildren(args)
                    if (lambda != null) {
                        children(lambda)
                    }
                }
                is Node.Expr.Call.TrailLambda -> {
                    if (anns.isNotEmpty()) childAnns(sameLine = true)
                    if (label != null) appendName(label).append("@")
                    children(func)
                }
                is Node.Expr.ArrayAccess -> {
                    children(expr)
                    children(indices, ",", "[", "]")
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
                    children(nameType)
                    bracketedChildren(typeArgs)
                    if (args != null) parenChildren(args)
                }
                is Node.Modifier.Lit ->
                    append(keyword.name.lowercase())
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

    protected fun Node.childTypeConstraints(v: List<Node.TypeConstraint>) = this@Writer.also {
        if (v.isNotEmpty()) append(" where ").also { children(v, ", ") }
    }

    protected fun Node.childVars(vars: List<Node.Decl.Property.Var?>) =
        if (vars.size == 1) {
            if (vars.single() == null) append('_') else children(vars)
        } else {
            append('(')
            vars.forEachIndexed { index, v ->
                if (v == null) append('_') else children(v)
                if (index < vars.size - 1) append(",")
            }
            append(')')
        }

    // Ends with newline (or space if sameLine) if there are any
    protected fun Node.WithAnnotations.childAnns(sameLine: Boolean = false) = this@Writer.also {
        if (anns.isNotEmpty()) (this@childAnns as Node).apply {
            if (sameLine) children(anns, " ", "", " ")
            else anns.forEach { ann -> children(ann) }
        }
    }

    protected fun Node.WithModifiers.childMods() = (this@childMods as Node).children(mods)

    // Null list values are asterisks
    protected fun Node.bracketedChildren(v: List<Node?>, appendIfNotEmpty: String = "") = this@Writer.also {
        if (v.isNotEmpty()) {
            append('<')
            v.forEachIndexed { index, node ->
                if (index > 0) append(",")
                if (node == null) append('*') else children(node)
            }
            append('>').append(appendIfNotEmpty)
        }
    }

    protected fun Node.parenChildren(v: List<Node?>) = children(v, ",", "(", ")")
    protected fun Node.parenChildren(v: Node.ValueArgs?) = v?.args?.let { children(it, ",", "(", ")") }

    protected fun Node.children(vararg v: Node?) = this@Writer.also { v.forEach { visitChildren(it) } }

    protected fun Node.children(v: List<Node?>, sep: String = "", prefix: String = "", suffix: String = "") =
        this@Writer.also {
            append(prefix)
            v.forEachIndexed { index, t ->
                visit(t, this)
                if (index < v.size - 1) append(sep)
            }
            append(suffix)
        }

    companion object {
        fun write(v: Node, extrasMap: ExtrasMap? = null) =
            write(v, StringBuilder(), extrasMap).toString()

        fun <T : Appendable> write(v: Node, app: T, extrasMap: ExtrasMap? = null) =
            app.also { Writer(it, extrasMap).write(v) }
    }
}
