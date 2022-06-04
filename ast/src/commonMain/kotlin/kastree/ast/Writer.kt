package kastree.ast

open class Writer(
    val app: Appendable = StringBuilder(),
    val extrasMap: ExtrasMap? = null,
    val includeExtraBlankLines: Boolean = extrasMap == null
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

    fun write(v: Node) { visit(v, v) }

    override fun visit(v: Node?, parent: Node) {
        v?.writeExtrasBefore()
        v?.apply {
            when (this) {
                is Node.NodeList<*> -> {
                    append(prefix)

                    // First, do all the enum entries if there are any
                    val enumEntries = children.takeWhile { it is Node.Decl.EnumEntry }
                    if (enumEntries.isNotEmpty()) {
                        children(enumEntries, ",", postfix = ";")
                    }

                    // Now the rest of the members
                    val restChildren = children.drop(enumEntries.size)
                    children(restChildren, separator)

                    writeExtrasWithin()
                    append(suffix)
                }
                is Node.File -> {
                    if (anns.isNotEmpty()) childAnns()
                    childrenLines(pkg, extraEndLines = 1)
                    childrenLines(imports, extraEndLines = 1)
                    childrenLines(decls, extraMidLines = 1)
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
                    childMods(newlines = false)
                    children(constructorKeyword)
                    children(params)
                }
                is Node.Decl.Init ->
                    append("init").also { children(block) }
                is Node.Decl.Func -> {
                    childMods()
                    children(funKeyword)
                    children(typeParams)
                    if (receiverType != null) children(receiverType).append(".")
                    name?.also { children(it) }
                    children(paramTypeParams)
                    children(params)
                    if (type != null) append(":").also { children(type) }
                    childTypeConstraints(typeConstraints)
                    if (body != null)  { children(body) }
                }
                is Node.Decl.Func.Params -> {
                    parenChildren(params)
                }
                is Node.Decl.Func.Params.Param -> {
                    if (mods.isNotEmpty()) childMods(newlines = false)
                    if (readOnly == true) append("val") else if (readOnly == false) append("var")
                    children(name)
                    if (type != null) append(":").also { children(type) }
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
                    if (receiverType != null) children(receiverType).append('.')
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
                    if (type != null) append(":").also { children(type) }
                }
                is Node.Decl.Property.Accessors -> {
                    childrenLines(first)
                    if (second != null) childrenLines(second)
                }
                is Node.Decl.Property.Accessor.Get -> {
                    childMods().append("get")
                    if (body != null) {
                        append("()")
                        if (type != null) append(":").also { children(type) }
                        append(' ').also { children(body) }
                    }
                }
                is Node.Decl.Property.Accessor.Set -> {
                    childMods().append("set")
                    if (body != null) {
                        append('(')
                        childMods(paramMods, newlines = false)
                        appendName(paramName ?: error("Missing setter param name when body present"))
                        if (paramType != null) append(":").also { children(paramType) }
                        append(")")
                        children(body)
                    }
                }
                is Node.Decl.TypeAlias -> {
                    childMods().append("typealias")
                    children(name)
                    children(typeParams).append("=")
                    children(type)
                }
                is Node.Decl.Constructor -> {
                    childMods()
                    children(constructorKeyword)
                    children(params)
                    if (delegationCall != null) append(":").also { children(delegationCall) }
                    children(block)
                }
                is Node.Decl.Constructor.DelegationCall ->
                    append(target.name.toLowerCase()).also { parenChildren(args) }
                is Node.Decl.EnumEntry -> {
                    childMods()
                    children(name)
                    if (args != null) parenChildren(args)
                    if (members.isNotEmpty()) append("{").run {
                        childrenLines(members, extraMidLines = 1)
                    }.append("}")
                }
                is Node.Initializer -> {
                    children(equals)
                    children(expr)
                }
                is Node.TypeParams -> {
                    bracketedChildren(params)
                }
                is Node.TypeParams.TypeParam -> {
                    childMods(newlines = false)
                    children(name)
                    if (type != null) append(":").also { children(type) }
                }
                is Node.TypeConstraint -> {
                    childAnns(sameLine = true)
                    children(name)
                    append(":")
                    children(type)
                }
                is Node.Type.Func -> {
                    if (receiverType != null) children(receiverType).append('.')
                    if (params != null) { children(params).append("->") }
                    children(type)
                }
                is Node.Type.Func.Param -> {
                    if (name != null) children(name).append(":")
                    children(type)
                }
                is Node.Type.Simple ->
                    children(pieces, ".")
                is Node.Type.Simple.Piece ->
                    children(name).also { bracketedChildren(typeParams) }
                is Node.Type.Nullable -> {
                    children(lpar)
                    children(mods)
                    children(type)
                    children(rpar)
                    append('?')
                }
                is Node.Type.Dynamic ->
                    append("dynamic")
                is Node.TypeProjection -> {
                    children(mods)
                    children(typeRef)
                }
                is Node.TypeRef -> {
                    children(lpar)
                    childModsBeforeType(ref).also { children(ref) }
                    children(rpar)
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
                    children(lpar, expr, rpar, body)
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
                    childVars(vars).append("in").also { children(inExpr) }.append(") ")
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
                    append('\\').append(when (char) {
                        '\b' -> 'b'
                        '\n' -> 'n'
                        '\t' -> 't'
                        '\r' -> 'r'
                        else -> char
                    })
                is Node.Expr.StringTmpl.Elem.LongTmpl ->
                    append("\${").also { children(expr) }.append('}')
                is Node.Expr.Const ->
                    append(value)
                is Node.Expr.Lambda -> {
                    append("{")
                    if (params.isNotEmpty()) append(' ').also { children(params, ", ", "", " ->") }
                    children(body)
                    append("}")
                }
                is Node.Expr.Lambda.Param -> {
                    childVars(vars)
                    if (destructType != null) append(": ").also { children(destructType) }
                }
                is Node.Expr.Lambda.Body -> {
                    if (stmts.isNotEmpty()) { childrenLines(stmts) }
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
                    if (expr != null) append('(').also { children(expr) }.append(')')
                    append("{")
                    childrenLines(entries)
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
                    append("is").also { children(type) }
                }
                is Node.Expr.Object -> {
                    append("object")
                    if (parents.isNotEmpty()) append(" : ").also { children(parents, ", ") }
                    if (members.isEmpty()) append("{}") else append("{").run {
                        childrenLines(members, extraMidLines = 1)
                    }.append("}")
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
                    childAnnsBeforeExpr(expr).also { children(expr) }
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
                            childrenLines(stmts)
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
                    if (target != null) append(target.name.toLowerCase()).append(':')
                    children(lBracket)
                    children(anns)
                    children(rBracket)
                }
                is Node.Modifier.AnnotationSet.Annotation -> {
                    children(nameTypeReference)
                    bracketedChildren(typeArgs)
                    if (args != null) parenChildren(args)
                }
                is Node.Modifier.Lit ->
                    append(keyword.name.toLowerCase())
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
        writeExtras(extrasMap.extrasBefore(this), continueIndent = true)
    }

    protected open fun Node.writeExtrasWithin() {
        if (extrasMap == null) return
        // Write everything within
        writeExtras(extrasMap.extrasWithin(this), continueIndent = false)
    }

    protected open fun Node.writeExtrasAfter() {
        if (extrasMap == null) return
        // Write everything after that doesn't start a line or end a line
        writeExtras(extrasMap.extrasAfter(this), continueIndent = false)
    }

    protected open fun Node.writeExtras(extras: List<Node.Extra>, continueIndent: Boolean) {
        extras.forEach {
            when (it) {
                is Node.Extra.Whitespace -> {
                    append(it.text)
                }
                is Node.Extra.Comment -> {
                    append(it.text)
                }
                is Node.Extra.Semicolon -> {
                    append(it.text)
                }
            }
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
                if (index < vars.size - 1) append(", ")
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

    protected fun Node.WithAnnotations.childAnnsBeforeExpr(expr: Node.Expr) = this@Writer.also {
        if (anns.isNotEmpty()) {
            // As a special case, if there is a trailing annotation with no args and expr is paren,
            // then we need to add an empty set of parens ourselves
            val lastAnn = anns.lastOrNull()?.anns?.singleOrNull()?.takeIf { it.args == null }
            val shouldAddParens = lastAnn != null && expr is Node.Expr.Paren
            (this as Node).children(anns, " ")
            if (shouldAddParens) append("()")
            append(' ')
        }
    }

    // Ends with newline if last is ann or space is last is mod or nothing if empty
    protected fun Node.WithModifiers.childMods(newlines: Boolean = true) =
        (this@childMods as Node).childMods(mods, newlines)

    protected fun Node.childMods(mods: List<Node.Modifier>, newlines: Boolean = true) =
        this@Writer.also {
            if (mods.isNotEmpty()) this@childMods.apply {
                mods.forEachIndexed { index, mod ->
                    children(mod)
                }
            }
        }

    protected fun Node.WithModifiers.childModsBeforeType(ref: Node.Type) = this@Writer.also {
        if (mods.isNotEmpty()) {
            // As a special case, if there is a trailing annotation with no args and the ref has a paren which is a paren
            // type or a non-receiver fn type, then we need to add an empty set of parens ourselves
            val lastAnn = (mods.lastOrNull() as? Node.Modifier.AnnotationSet)?.anns?.
                singleOrNull()?.takeIf { it.args == null }
            val shouldAddParens = lastAnn != null && ref is Node.Type.Func && ref.receiverType == null
//            val shouldAddParens = lastAnn != null &&
//                (ref is Node.Type.Paren || (ref is Node.Type.Func && (
//                    ref.receiverType == null || ref.receiverType.ref is Node.Type.Paren)))
            (this as Node).children(mods, "")
            if (shouldAddParens) append("()")
        }
    }

    protected inline fun Node.children(vararg v: Node?) = this@Writer.also { v.forEach { visitChildren(it) } }

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
    protected fun Node.parenChildren(v: Node.NodeList<Node.Type.Func.Param>) = parenChildren(v.children)

    protected fun Node.childrenLines(v: Node?, extraMidLines: Int = 0, extraEndLines: Int = 0) =
        this@Writer.also { if (v != null) childrenLines(listOf(v), extraMidLines, extraEndLines) }

    protected fun Node.childrenLines(v: List<Node?>, extraMidLines: Int = 0, extraEndLines: Int = 0) =
        this@Writer.also {
            v.forEachIndexed { index, node ->
                children(node)
                if (stmtRequiresEmptyBraceSetBeforeLineEnd(node, v.getOrNull(index + 1))) append("{}")
                if (stmtRequiresSemicolonSetBeforeLineEnd(node, v.getOrNull(index + 1))) append(';')
            }
        }

    protected fun stmtRequiresEmptyBraceSetBeforeLineEnd(v: Node?, next: Node?): Boolean {
        // As a special case, if this is a local memberless class decl stmt and the next line is a paren
        // or ann+paren, we have to explicitly provide an empty brace set
        // See: https://youtrack.jetbrains.com/issue/KT-25578
        // TODO: is there a better place to do this?
        if (v !is Node.Stmt.Decl || v.decl !is Node.Decl.Structured || (v.decl.body?.children?.isNotEmpty() == true) ||
            !v.decl.isClass) return false
        if (next !is Node.Stmt.Expr || (next.expr !is Node.Expr.Paren &&
            (next.expr !is Node.Expr.Annotated || next.expr.expr !is Node.Expr.Paren))) return false
        return true
    }

    protected fun stmtRequiresSemicolonSetBeforeLineEnd(v: Node?, next: Node?) =
        stmtHasModifierLocalVarDeclAmbiguity(v, next) || stmtHasTrailingLambdaAmbiguity(v, next)

    protected fun stmtHasModifierLocalVarDeclAmbiguity(v: Node?, next: Node?): Boolean {
        // As a special case, if there is just a name stmt, and it is a modifier, and the next stmt is
        // a decl, we need a semicolon
        // See: https://youtrack.jetbrains.com/issue/KT-25579
        // TODO: is there a better place to do this
        if (v !is Node.Stmt.Expr || v.expr !is Node.Expr.Name || next !is Node.Stmt.Decl) return false
        val name = v.expr.name.toUpperCase()
        return Node.Modifier.Keyword.values().any { it.name == name }
    }

    protected fun stmtHasTrailingLambdaAmbiguity(v: Node?, next: Node?): Boolean {
        // As a special case, if there is a function call stmt w/ no trailing lambda followed by a brace
        // stmt, the call needs a semicolon
        if (v !is Node.Stmt.Expr || v.expr !is Node.Expr.Call || v.expr.lambda != null) return false
        return next is Node.Stmt.Expr && next.expr is Node.Expr.Lambda
    }

    protected fun Node.children(v: List<Node?>, sep: String = "", prefix: String = "", postfix: String = "") =
        this@Writer.also {
            append(prefix)
            v.forEachIndexed { index, t ->
                visit(t, this)
                if (index < v.size - 1) append(sep)
            }
            append(postfix)
        }

    // We accept lots of false positives to be simple and not have to bring in JVM dep to do accurate check
    protected val String.shouldEscapeIdent get() =
        KEYWORDS.contains(this) ||
        all { it == '_' } ||
        first() in '0'..'9' ||
        any { it !in 'a'..'z' && it !in 'A'..'Z' && it !in '0'..'9' && it != '_' }

    companion object {
        protected val KEYWORDS = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface",
            "is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
            "typeof", "val", "var", "when", "while"
        )

        fun write(v: Node, extrasMap: ExtrasMap? = null) =
            write(v, StringBuilder(), extrasMap).toString()
        fun <T: Appendable> write(v: Node, app: T, extrasMap: ExtrasMap? = null) =
            app.also { Writer(it, extrasMap).write(v) }
    }
}
