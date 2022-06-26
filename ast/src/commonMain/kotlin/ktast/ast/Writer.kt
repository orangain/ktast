package ktast.ast

open class Writer(
    val app: Appendable = StringBuilder(),
    val extrasMap: ExtrasMap? = null
) : Visitor() {
    protected var lastSymbol: String? = null
    protected var lastNonSymbol: String? = null
    protected var extrasSinceLastNonSymbol = mutableListOf<Node.Extra>()
    protected var nextHeuristicWhitespace = ""

    protected fun doAppend(str: String) {
        if (str == "") return
        if (nextHeuristicWhitespace == " " && (!str.contains(" ") && !str.contains("\n"))) {
            app.append(" ")
        }
        app.append(str)
        nextHeuristicWhitespace = ""
    }

    protected fun append(ch: Char) = append(ch.toString())
    protected fun append(str: String) = also {
        if (str == "") return@also
        if (lastSymbol != null && lastSymbol!!.endsWith(">") && str.startsWith("=")) {
            doAppend(" ") // Insert heuristic space between '>' and '=' not to be confused with '>='
        }
        doAppend(str)
        lastSymbol = str
        lastNonSymbol = null
    }


    protected fun appendKeyword(keyword: String) = appendNonSymbol(keyword)
    protected fun appendName(name: String) = appendNonSymbol(name)
    protected fun appendLabel(label: String?) {
        if (label != null) append('@').appendNonSymbol(label)
    }

    protected fun appendNonSymbol(str: String) = also {
        if (lastNonSymbol != null) {
            append(" ") // Insert heuristic space between two non-symbols
        }
        doAppend(str)
        lastNonSymbol = str
        extrasSinceLastNonSymbol.clear()
    }

    fun write(v: Node) {
        visit(v, v)
    }

    protected fun containsNewlineOrSemicolon(extras: List<Node.Extra>): Boolean {
        return extras.any {
            it is Node.Extra.Semicolon || (it is Node.Extra.Whitespace && it.text.contains("\n"))
        }
    }

    protected open fun Node.writeHeuristicNewline(parent: Node) {
        if (parent is Node.StatementsContainer && this is Node.Statement) {
            if (parent.statements.first() !== this && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
        if (parent is Node.DeclsContainer && this is Node.Decl && this !is Node.Decl.EnumEntry) {
            if (parent.decls.first() !== this && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
        if (parent is Node.Decl.Property && this is Node.Decl.Property.Accessor) {
            // Property accessors require newline when the previous element is expression
            if ((parent.accessors.first() === this && (parent.delegate != null || parent.initializer != null)) ||
                (parent.accessors.size == 2 && parent.accessors.last() === this && parent.accessors[0].body is Node.Decl.Func.Body.Expr)
            ) {
                if (!containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                    append("\n")
                }
            }
        }
        if (parent is Node.Expr.When && this is Node.Expr.When.Entry) {
            if (parent.entries.first() !== this && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
    }

    override fun visit(v: Node?, parent: Node) {
        if (v == null) return
        v.writeExtrasBefore()
        v.writeHeuristicNewline(parent)
        v.apply {
            when (this) {
                is Node.CommaSeparatedNodeList<*> -> {
                    children(elements, ",", prefix, suffix, trailingComma, parent = this)
                }
                is Node.NodeList<*> -> {
                    children(elements, prefix = prefix, suffix = suffix, parent = this)
                }
                is Node.File -> {
                    children(anns)
                    children(pkg)
                    children(imports)
                    children(decls)
                }
                is Node.Package -> {
                    children(mods)
                    children(packageKeyword)
                    children(names, ".")
                    writeExtrasWithin()
                }
                is Node.Import -> {
                    children(importKeyword)
                    children(names, ".")
                    children(alias)
                }
                is Node.Import.Alias -> {
                    children(Node.Keyword.As())
                    children(name)
                }
                is Node.Decl.Structured -> {
                    children(mods)
                    children(declarationKeyword)
                    children(name)
                    children(typeParams)
                    children(primaryConstructor)
                    if (parents != null) {
                        append(":")
                        children(parents)
                    }
                    children(typeConstraints)
                    children(body)
                }
                is Node.Decl.Structured.Parents -> {
                    children(items, ",")
                }
                is Node.Decl.Structured.Parent.CallConstructor -> {
                    children(type)
                    children(args)
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
                    append("{")
                    children(enumEntries)
                    children(decls, parent = this)
                    append("}")
                }
                is Node.Decl.Init -> {
                    children(mods)
                    children(Node.Keyword.Init())
                    children(block)
                }
                is Node.Decl.Func -> {
                    children(mods)
                    children(funKeyword)
                    children(typeParams)
                    if (receiverTypeRef != null) children(receiverTypeRef).append(".")
                    name?.also { children(it) }
                    children(postTypeParams)
                    children(params)
                    if (typeRef != null) append(":").also { children(typeRef) }
                    children(postMods)
                    children(body)
                }
                is Node.Decl.Func.Param -> {
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
                    children(variable)
                    children(typeConstraints)
                    children(initializer)
                    children(delegate)
                    children(accessors)
                }
                is Node.Decl.Property.Delegate -> {
                    children(byKeyword)
                    children(expr)
                }
                is Node.Decl.Property.Variable.Single -> {
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                }
                is Node.Decl.Property.Variable.Multi -> {
                    children(vars, ",", "(", ")", trailingComma)
                }
                is Node.Decl.Property.Accessor.Get -> {
                    children(mods)
                    children(getKeyword)
                    if (body != null) {
                        append("()")
                        if (typeRef != null) append(":").also { children(typeRef) }
                        children(postMods)
                        children(body)
                    }
                }
                is Node.Decl.Property.Accessor.Set -> {
                    children(mods)
                    children(setKeyword)
                    if (body != null) {
                        append("(")
                        children(params)
                        append(")")
                        children(postMods)
                        children(body)
                    }
                }
                is Node.Decl.TypeAlias -> {
                    children(mods)
                    children(Node.Keyword.Typealias())
                    children(name)
                    children(typeParams).append("=")
                    children(typeRef)
                }
                is Node.Decl.SecondaryConstructor -> {
                    children(mods)
                    children(constructorKeyword)
                    children(params)
                    if (delegationCall != null) append(":").also { children(delegationCall) }
                    children(block)
                }
                is Node.Decl.SecondaryConstructor.DelegationCall ->
                    appendKeyword(target.name.lowercase()).also { children(args) }
                is Node.Decl.EnumEntry -> {
                    children(mods)
                    children(name)
                    children(args)
                    children(body)
                    if (parent is Node.Decl.Structured.Body) { // should always be true
                        if (parent.enumEntries.lastOrNull() !== this || (parent.enumEntries.lastOrNull() === this && parent.hasTrailingCommaInEnumEntries)) {
                            append(",")
                        }
                    }
                    if (parent is Node.Decl.Structured.Body &&
                        parent.decls.isNotEmpty() &&
                        parent.enumEntries.last() === this &&
                        !extrasMap?.extrasAfter(body ?: args ?: name).orEmpty().any { it is Node.Extra.Semicolon }
                    ) {
                        append(";") // Insert heuristic semicolon after the last enum entry
                    }
                    writeExtrasWithin()
                }
                is Node.Initializer -> {
                    children(equals)
                    children(expr)
                }
                is Node.TypeParam -> {
                    children(mods)
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                }
                is Node.TypeArg.Asterisk -> {
                    children(asterisk)
                }
                is Node.TypeArg.Type -> {
                    children(mods)
                    children(typeRef)
                }
                is Node.TypeRef -> {
                    children(lPar)
                    children(mods)
                    children(innerLPar)
                    children(innerMods)
                    children(type)
                    children(innerRPar)
                    children(rPar)
                }
                is Node.Type.Func -> {
                    if (contextReceivers != null) {
                        children(Node.Keyword.Context())
                        children(contextReceivers)
                    }
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
                is Node.Type.Func.ContextReceiver -> {
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
                    children(typeArgs)
                }
                is Node.Type.Nullable -> {
                    children(lPar)
                    children(mods)
                    children(type)
                    children(rPar)
                    append('?')
                }
                is Node.Type.Dynamic ->
                    children(Node.Keyword.Dynamic())
                is Node.ConstructorCallee -> {
                    children(type)
                }
                is Node.Container -> {
                    children(expr)
                }
                is Node.ValueArg -> {
                    if (name != null) children(name).append("=")
                    if (asterisk) append('*')
                    children(expr)
                }
                is Node.Expr.If -> {
                    children(ifKeyword)
                    append("(")
                    children(condition)
                    append(")")
                    children(body)
                    if (elseBody != null) {
                        children(Node.Keyword.Else())
                        children(elseBody)
                    }
                }
                is Node.Expr.Try -> {
                    children(Node.Keyword.Try())
                    children(block)
                    if (catches.isNotEmpty()) children(catches)
                    if (finallyBlock != null) children(Node.Keyword.Finally()).also { children(finallyBlock) }
                }
                is Node.Expr.Try.Catch -> {
                    children(catchKeyword)
                    children(params)
                    children(block)
                }
                is Node.Expr.For -> {
                    children(forKeyword)
                    append("(")
                    children(anns)
                    children(loopParam)
                    children(Node.Keyword.In())
                    children(loopRange)
                    append(")")
                    children(body)
                }
                is Node.Expr.While -> {
                    if (!doWhile) {
                        children(whileKeyword)
                        append("(")
                        children(condition)
                        append(")")
                        children(body)
                    } else {
                        children(Node.Keyword.Do())
                        children(body)
                        children(whileKeyword)
                        append("(")
                        children(condition)
                        append(")")
                    }
                }
                is Node.Expr.BinaryOp -> {
                    children(listOf(lhs, oper, rhs))
                }
                is Node.Expr.BinaryOp.Oper.Infix ->
                    appendKeyword(str)
                is Node.Expr.BinaryOp.Oper.Token ->
                    append(token.str)
                is Node.Expr.UnaryOp ->
                    if (prefix) children(oper, expr) else children(expr, oper)
                is Node.Expr.UnaryOp.Oper ->
                    append(token.str)
                is Node.Expr.TypeOp ->
                    children(listOf(lhs, oper, rhs), "")
                is Node.Expr.TypeOp.Oper -> {
                    // Using appendNonSymbol may cause insertion of unneeded spaces before or after symbols.
                    // However, we ignore them as it is rare case for now.
                    appendNonSymbol(token.str)
                }
                is Node.Expr.DoubleColonRef.Callable -> {
                    if (recv != null) children(recv)
                    append("::")
                    children(name)
                }
                is Node.Expr.DoubleColonRef.Class -> {
                    if (recv != null) children(recv)
                    append("::")
                    children(Node.Keyword.Declaration.of("class"))
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
                    appendNonSymbol(value)
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
                    children(name)
                    if (typeRef != null) {
                        append(":")
                        children(typeRef)
                    }
                }
                is Node.Expr.Lambda.Param.Multi -> {
                    children(vars)
                    if (destructTypeRef != null) append(":").also { children(destructTypeRef) }
                }
                is Node.Expr.Lambda.Body -> {
                    children(statements)
                    writeExtrasWithin()
                }
                is Node.Expr.This -> {
                    children(Node.Keyword.This())
                    appendLabel(label)
                }
                is Node.Expr.Super -> {
                    children(Node.Keyword.Super())
                    if (typeArg != null) append('<').also { children(typeArg) }.append('>')
                    appendLabel(label)
                }
                is Node.Expr.When -> {
                    children(Node.Keyword.When())
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
                    children(Node.Keyword.In()).also { children(expr) }
                }
                is Node.Expr.When.Cond.Is -> {
                    if (not) append('!')
                    children(Node.Keyword.Is()).also { children(typeRef) }
                }
                is Node.Expr.Object -> {
                    children(decl)
                }
                is Node.Expr.Throw ->
                    children(Node.Keyword.Throw()).also { children(expr) }
                is Node.Expr.Return -> {
                    children(Node.Keyword.Return())
                    appendLabel(label)
                    children(expr)
                }
                is Node.Expr.Continue -> {
                    children(Node.Keyword.Continue())
                    appendLabel(label)
                }
                is Node.Expr.Break -> {
                    children(Node.Keyword.Break())
                    appendLabel(label)
                }
                is Node.Expr.CollLit ->
                    children(exprs, ",", "[", "]", trailingComma)
                is Node.Expr.Name ->
                    appendName(name)
                is Node.Expr.Labeled ->
                    appendName(label).append("@").also { children(expr) }
                is Node.Expr.Annotated ->
                    children(anns).also { children(expr) }
                is Node.Expr.Call -> {
                    children(expr)
                    children(typeArgs)
                    children(args)
                    children(lambdaArgs)
                }
                is Node.Expr.Call.LambdaArg -> {
                    children(anns)
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
                        children(statements)
                        writeExtrasWithin()
                    }
                    append("}")
                }
                is Node.Modifier.AnnotationSet -> {
                    children(atSymbol)
                    if (target != null) appendKeyword(target.name.lowercase()).append(':')
                    children(lBracket)
                    children(anns)
                    if (rBracket != null) {
                        // Disable insertion of heuristic space after annotation if rBracket exists
                        nextHeuristicWhitespace = ""
                    }
                    children(rBracket)
                }
                is Node.Modifier.AnnotationSet.Annotation -> {
                    children(constructorCallee)
                    children(args)
                    nextHeuristicWhitespace = " " // Insert heuristic space after annotation
                }
                is Node.Modifier.Lit ->
                    appendKeyword(keyword.name.lowercase())
                is Node.PostModifier.TypeConstraints -> {
                    children(whereKeyword)
                    children(constraints)
                }
                is Node.PostModifier.TypeConstraints.TypeConstraint -> {
                    children(anns)
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
                is Node.Keyword -> appendKeyword(value)
                is Node.Symbol -> append(value)
                else ->
                    error("Unrecognized node type: $this")
            }
        }
        v.writeExtrasAfter()
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
        extrasSinceLastNonSymbol.addAll(extras)
    }

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
