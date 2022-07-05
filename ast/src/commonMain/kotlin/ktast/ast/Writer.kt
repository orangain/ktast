package ktast.ast

open class Writer(
    val app: Appendable = StringBuilder(),
    val extrasMap: ExtrasMap? = null
) : Visitor() {
    protected val extrasSinceLastNonSymbol = mutableListOf<Node.Extra>()
    protected var nextHeuristicWhitespace = ""
    protected var lastAppendedToken = ""

    protected val nonSymbolCategories = setOf(
        CharCategory.UPPERCASE_LETTER,
        CharCategory.LOWERCASE_LETTER,
        CharCategory.TITLECASE_LETTER,
        CharCategory.MODIFIER_LETTER,
        CharCategory.OTHER_LETTER,
        CharCategory.DECIMAL_DIGIT_NUMBER,
    )

    protected fun appendLabel(label: String?) {
        if (label != null) append('@').append(label)
    }

    protected fun append(ch: Char) = append(ch.toString())
    protected fun append(str: String) = also {
        if (str == "") return@also
        if (lastAppendedToken.endsWith(">") && str.startsWith("=")) {
            doAppend(" ") // Insert heuristic space between '>' and '=' not to be confused with '>='
        }
        if (lastAppendedToken != "" && isNonSymbol(lastAppendedToken.last()) && isNonSymbol(str.first())) {
            doAppend(" ") // Insert heuristic space between two non-symbols
        }
        doAppend(str)
        lastAppendedToken = str
    }

    protected fun isNonSymbol(ch: Char) = ch == '_' || nonSymbolCategories.contains(ch.category)

    protected fun doAppend(str: String) {
        app.append(str)
    }

    fun write(v: Node) {
        extrasSinceLastNonSymbol.clear()
        nextHeuristicWhitespace = ""
        lastAppendedToken = ""
        visit(v)
    }

    override fun visit(v: Node, parent: Node?) {
        v.writeExtrasBefore()
        v.writeHeuristicNewline(parent)
        writeHeuristicSpace()
        v.apply {
            when (this) {
                is Node.CommaSeparatedNodeList<*> -> {
                    children(elements, ",", prefix, suffix, trailingComma)
                }
                is Node.NodeList<*> -> {
                    children(elements, prefix = prefix, suffix = suffix)
                }
                is Node.HasSimpleStringRepresentation -> {
                    append(string)
                }
                is Node.KotlinFile -> {
                    children(annotationSets, skipWritingExtrasWithin = true)
                    children(packageDirective)
                    children(importDirectives)
                    children(declarations)
                }
                is Node.PackageDirective -> {
                    children(modifiers)
                    children(packageKeyword)
                    children(names, ".")
                }
                is Node.ImportDirective -> {
                    children(importKeyword)
                    children(names, ".")
                    children(alias)
                }
                is Node.ImportDirective.Alias -> {
                    append("as")
                    children(name)
                }
                is Node.Declaration.Class -> {
                    children(modifiers)
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
                is Node.Declaration.Class.Parent.CallConstructor -> {
                    children(type)
                    children(args)
                }
                is Node.Declaration.Class.Parent.DelegatedType -> {
                    children(type)
                    children(byKeyword)
                    children(expression)
                }
                is Node.Declaration.Class.Parent.Type -> {
                    children(type)
                }
                is Node.Declaration.Class.PrimaryConstructor -> {
                    children(modifiers)
                    children(constructorKeyword)
                    children(params)
                }
                is Node.Declaration.Class.Body -> {
                    append("{")
                    children(enumEntries, skipWritingExtrasWithin = true)
                    children(declarations)
                    append("}")
                }
                is Node.Declaration.Init -> {
                    children(modifiers)
                    append("init")
                    children(block)
                }
                is Node.Declaration.Function -> {
                    children(modifiers)
                    children(funKeyword)
                    children(typeParams)
                    if (receiverTypeRef != null) children(receiverTypeRef).append(".")
                    name?.also { children(it) }
                    children(params)
                    if (typeRef != null) append(":").also { children(typeRef) }
                    children(postModifiers)
                    children(body)
                }
                is Node.Declaration.Function.Param -> {
                    children(modifiers)
                    children(valOrVar)
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                    children(equals)
                    children(defaultValue)
                }
                is Node.Declaration.Function.Body.Block ->
                    children(block)
                is Node.Declaration.Function.Body.Expr ->
                    children(equals, expression)
                is Node.Declaration.Property -> {
                    children(modifiers)
                    children(valOrVar)
                    children(typeParams)
                    if (receiverTypeRef != null) children(receiverTypeRef).append('.')
                    children(variable)
                    children(typeConstraints)
                    children(equals)
                    children(initializer)
                    children(delegate)
                    children(accessors)
                }
                is Node.Declaration.Property.Delegate -> {
                    children(byKeyword)
                    children(expression)
                }
                is Node.Declaration.Property.Variable.Single -> {
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                }
                is Node.Declaration.Property.Variable.Multi -> {
                    children(vars, ",", "(", ")", trailingComma)
                }
                is Node.Declaration.Property.Accessor.Getter -> {
                    children(modifiers)
                    children(getKeyword)
                    if (body != null) {
                        append("()")
                        if (typeRef != null) append(":").also { children(typeRef) }
                        children(postModifiers)
                        children(body)
                    }
                }
                is Node.Declaration.Property.Accessor.Setter -> {
                    children(modifiers)
                    children(setKeyword)
                    if (body != null) {
                        append("(")
                        children(params)
                        append(")")
                        children(postModifiers)
                        children(body)
                    }
                }
                is Node.Declaration.TypeAlias -> {
                    children(modifiers)
                    append("typealias")
                    children(name)
                    children(typeParams).append("=")
                    children(typeRef)
                }
                is Node.Declaration.SecondaryConstructor -> {
                    children(modifiers)
                    children(constructorKeyword)
                    children(params)
                    if (delegationCall != null) append(":").also { children(delegationCall) }
                    children(block)
                }
                is Node.Declaration.SecondaryConstructor.DelegationCall ->
                    append(target.name.lowercase()).also { children(args) }
                is Node.EnumEntry -> {
                    children(modifiers)
                    children(name)
                    children(args)
                    children(body)
                    check(parent is Node.Declaration.Class.Body) // condition should always be true
                    val isLastEntry = parent.enumEntries.last() === this
                    if (!isLastEntry || parent.hasTrailingCommaInEnumEntries) {
                        append(",")
                    }
                    writeExtrasWithin() // Semicolon after trailing comma is avaialbe as extrasWithin
                    if (parent.declarations.isNotEmpty() && isLastEntry && !containsSemicolon(extrasSinceLastNonSymbol)) {
                        append(";") // Insert heuristic semicolon after the last enum entry
                    }
                }
                is Node.TypeParam -> {
                    children(modifiers)
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                }
                is Node.TypeArg -> {
                    children(modifiers)
                    children(typeRef)
                    if (asterisk) {
                        append("*")
                    }
                }
                is Node.TypeRef -> {
                    children(lPar)
                    children(modifiers)
                    children(innerLPar)
                    children(innerMods)
                    children(type)
                    children(innerRPar)
                    children(rPar)
                }
                is Node.Type.Function -> {
                    if (contextReceivers != null) {
                        append("context")
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
                is Node.Type.Function.ContextReceiver -> {
                    children(typeRef)
                }
                is Node.Type.Function.Receiver -> {
                    children(typeRef)
                }
                is Node.Type.Function.Param -> {
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
                    children(modifiers)
                    children(type)
                    children(rPar)
                    append('?')
                }
                is Node.Type.Dynamic ->
                    append("dynamic")
                is Node.ExpressionContainer -> {
                    children(expression)
                }
                is Node.ValueArg -> {
                    if (name != null) children(name).append("=")
                    if (asterisk) append('*')
                    children(expression)
                }
                is Node.Expression.If -> {
                    children(ifKeyword)
                    append("(")
                    children(condition)
                    append(")")
                    children(body)
                    if (elseBody != null) {
                        append("else")
                        children(elseBody)
                    }
                }
                is Node.Expression.Try -> {
                    append("try")
                    children(block)
                    if (catches.isNotEmpty()) children(catches)
                    if (finallyBlock != null) append("finally").also { children(finallyBlock) }
                }
                is Node.Expression.Try.Catch -> {
                    children(catchKeyword)
                    children(params)
                    children(block)
                }
                is Node.Expression.For -> {
                    children(forKeyword)
                    append("(")
                    children(annotationSets)
                    children(loopParam)
                    append("in")
                    children(loopRange)
                    append(")")
                    children(body)
                }
                is Node.Expression.While -> {
                    if (!doWhile) {
                        children(whileKeyword)
                        append("(")
                        children(condition)
                        append(")")
                        children(body)
                    } else {
                        append("do")
                        children(body)
                        children(whileKeyword)
                        append("(")
                        children(condition)
                        append(")")
                    }
                }
                is Node.Expression.Binary -> {
                    children(listOf(lhs, operator, rhs))
                }
                is Node.Expression.Binary.Operator.Infix ->
                    append(str)
                is Node.Expression.Binary.Operator.Token ->
                    if (token == Node.Expression.Binary.Token.IN || token == Node.Expression.Binary.Token.NOT_IN) {
                        // Using appendNonSymbol may cause insertion of unneeded space before !in.
                        // However, we ignore them as it is rare case for now.
                        append(token.str)
                    } else {
                        append(token.str)
                    }
                is Node.Expression.Unary ->
                    if (prefix) children(operator, expression) else children(expression, operator)
                is Node.Expression.Unary.Operator ->
                    append(token.str)
                is Node.Expression.BinaryType ->
                    children(listOf(lhs, operator, rhs), "")
                is Node.Expression.BinaryType.Operator -> {
                    if (token == Node.Expression.BinaryType.Token.COL) {
                        append(token.str)
                    } else {
                        // Using appendNonSymbol may cause insertion of unneeded spaces before or after symbols.
                        // However, we ignore them as it is rare case for now.
                        append(token.str)
                    }
                }
                is Node.Expression.CallableReference -> {
                    if (lhs != null) children(lhs)
                    append("::")
                    children(rhs)
                }
                is Node.Expression.ClassLiteral -> {
                    if (lhs != null) children(lhs)
                    append("::")
                    append("class")
                }
                is Node.Expression.DoubleColon.Receiver.Expression ->
                    children(expression)
                is Node.Expression.DoubleColon.Receiver.Type -> {
                    children(type)
                    children(questionMarks)
                }
                is Node.Expression.Parenthesized ->
                    append('(').also { children(expression) }.append(')')
                is Node.Expression.StringTemplate ->
                    if (raw) append("\"\"\"").also { children(entries) }.append("\"\"\"")
                    else append('"').also { children(entries) }.append('"')
                is Node.Expression.StringTemplate.Entry.Regular ->
                    append(str)
                is Node.Expression.StringTemplate.Entry.ShortTemplate ->
                    append('$').append(str)
                is Node.Expression.StringTemplate.Entry.UnicodeEscape ->
                    append("\\u").append(digits)
                is Node.Expression.StringTemplate.Entry.RegularEscape ->
                    append('\\').append(
                        when (char) {
                            '\b' -> 'b'
                            '\n' -> 'n'
                            '\t' -> 't'
                            '\r' -> 'r'
                            else -> char
                        }
                    )
                is Node.Expression.StringTemplate.Entry.LongTemplate ->
                    append("\${").also { children(expression) }.append('}')
                is Node.Expression.Constant ->
                    append(value)
                is Node.Expression.Lambda -> {
                    append("{")
                    if (params != null) {
                        children(params)
                        append("->")
                    }
                    children(body)
                    append("}")
                }
                is Node.Expression.Lambda.Param.Single -> {
                    children(name)
                    if (typeRef != null) {
                        append(":")
                        children(typeRef)
                    }
                }
                is Node.Expression.Lambda.Param.Multi -> {
                    children(vars)
                    if (destructTypeRef != null) append(":").also { children(destructTypeRef) }
                }
                is Node.Expression.Lambda.Body -> {
                    children(statements)
                }
                is Node.Expression.This -> {
                    append("this")
                    appendLabel(label)
                }
                is Node.Expression.Super -> {
                    append("super")
                    if (typeArg != null) append('<').also { children(typeArg) }.append('>')
                    appendLabel(label)
                }
                is Node.Expression.When -> {
                    append("when")
                    children(lPar, expression, rPar)
                    append("{")
                    children(entries)
                    append("}")
                }
                is Node.Expression.When.Entry.Conditions -> {
                    children(conditions, ",", trailingSeparator = trailingComma)
                    append("->").also { children(body) }
                }
                is Node.Expression.When.Entry.Else -> {
                    children(elseKeyword)
                    append("->").also { children(body) }
                }
                is Node.Expression.When.Condition.Expression ->
                    children(expression)
                is Node.Expression.When.Condition.In -> {
                    if (not) append('!')
                    append("in").also { children(expression) }
                }
                is Node.Expression.When.Condition.Is -> {
                    if (not) append('!')
                    append("is").also { children(typeRef) }
                }
                is Node.Expression.Object -> {
                    children(declaration)
                }
                is Node.Expression.Throw ->
                    append("throw").also { children(expression) }
                is Node.Expression.Return -> {
                    append("return")
                    appendLabel(label)
                    children(expression)
                }
                is Node.Expression.Continue -> {
                    append("continue")
                    appendLabel(label)
                }
                is Node.Expression.Break -> {
                    append("break")
                    appendLabel(label)
                }
                is Node.Expression.CollectionLiteral ->
                    children(expressions, ",", "[", "]", trailingComma)
                is Node.Expression.Name ->
                    append(name)
                is Node.Expression.Labeled ->
                    append(label).append("@").also { children(expression) }
                is Node.Expression.Annotated ->
                    children(annotationSets).also { children(expression) }
                is Node.Expression.Call -> {
                    children(expression)
                    children(typeArgs)
                    children(args)
                    children(lambdaArg)
                }
                is Node.Expression.Call.LambdaArg -> {
                    children(annotationSets)
                    if (label != null) append(label).append("@")
                    children(func)
                }
                is Node.Expression.ArrayAccess -> {
                    children(expression)
                    children(indices, ",", "[", "]", trailingComma)
                }
                is Node.Expression.AnonymousFunction ->
                    children(function)
                is Node.Expression.Property ->
                    children(declaration)
                is Node.Expression.Block -> {
                    append("{").run {
                        children(statements)
                    }
                    append("}")
                }
                is Node.Modifier.AnnotationSet -> {
                    children(atSymbol)
                    if (target != null) append(target.name.lowercase())
                    children(colon)
                    children(lBracket)
                    children(annotations)
                    children(rBracket)
                }
                is Node.Modifier.AnnotationSet.Annotation -> {
                    children(type)
                    children(args)
                    if (parent is Node.Modifier.AnnotationSet && parent.rBracket == null) {
                        nextHeuristicWhitespace = " " // Insert heuristic space after annotation if single form
                    }
                }
                is Node.PostModifier.TypeConstraints -> {
                    children(whereKeyword)
                    children(constraints)
                }
                is Node.PostModifier.TypeConstraints.TypeConstraint -> {
                    children(annotationSets)
                    children(name)
                    append(":")
                    children(typeRef)
                }
                is Node.PostModifier.Contract -> {
                    children(contractKeyword)
                    children(contractEffects)
                }
                is Node.PostModifier.Contract.ContractEffect -> {
                    children(expression)
                }
                else ->
                    error("Unrecognized node type: $this")
            }
        }
        v.writeExtrasAfter()
    }

    protected fun Node.children(vararg v: Node?) = this@Writer.also { v.forEach { visitChildren(it) } }

    protected fun Node.children(
        v: List<Node>,
        sep: String = "",
        prefix: String = "",
        suffix: String = "",
        trailingSeparator: Node? = null,
        skipWritingExtrasWithin: Boolean = false,
    ) =
        this@Writer.also {
            append(prefix)
            v.forEachIndexed { index, t ->
                visit(t, this)
                if (index < v.size - 1) append(sep)
                writeHeuristicExtraAfterChild(t, v.getOrNull(index + 1), this)
            }
            children(trailingSeparator)
            if (!skipWritingExtrasWithin) {
                writeExtrasWithin()
            }
            append(suffix)
        }

    protected open fun Node.writeHeuristicNewline(parent: Node?) {
        if (parent is Node.StatementsContainer && this is Node.Statement) {
            if (parent.statements.first() !== this && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
        if (parent is Node.DeclarationsContainer && this is Node.Declaration) {
            if (parent.declarations.first() !== this && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
        if (parent is Node.Declaration.Property && this is Node.Declaration.Property.Accessor) {
            // Property accessors require newline when the previous element is expression
            if ((parent.accessors.first() === this && (parent.delegate != null || parent.initializer != null)) ||
                (parent.accessors.size == 2 && parent.accessors.last() === this && parent.accessors[0].body is Node.Declaration.Function.Body.Expr)
            ) {
                if (!containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                    append("\n")
                }
            }
        }
        if (parent is Node.Expression.When && this is Node.Expression.When.Entry) {
            if (parent.entries.first() !== this && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
        if (parent is Node.Expression.Annotated && (this is Node.Expression.Binary || this is Node.Expression.BinaryType)) {
            // Annotated expression requires newline between annotation and expression when expression is a binary operation.
            // This is because, without newline, annotated expression of binary expression is ambiguous with binary expression of annotated expression.
            if (!containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
    }

    protected fun writeHeuristicSpace() {
        if (nextHeuristicWhitespace == " " && extrasSinceLastNonSymbol.isEmpty()) {
            append(" ")
        }
        nextHeuristicWhitespace = ""
        extrasSinceLastNonSymbol.clear()
    }

    protected open fun writeHeuristicExtraAfterChild(v: Node, next: Node?, parent: Node?) {
        if (v is Node.Expression.Name && next is Node.Declaration && parent is Node.StatementsContainer) {
            val upperCasedName = v.name.uppercase()
            if (Node.Modifier.KeywordToken.values().any { it.name == upperCasedName } &&
                !containsSemicolon(extrasSinceLastNonSymbol)
            ) {
                append(";")
            }
        }
        if (v is Node.Expression.Call && v.lambdaArg == null && next is Node.Expression.Lambda) {
            if (!containsSemicolon(extrasSinceLastNonSymbol)) {
                append(";")
            }
        }
    }

    protected fun containsNewlineOrSemicolon(extras: List<Node.Extra>): Boolean {
        return containsNewline(extras) || containsSemicolon(extras)
    }

    protected fun containsNewline(extras: List<Node.Extra>): Boolean {
        return extras.any {
            it is Node.Extra.Whitespace && it.text.contains("\n")
        }
    }

    protected fun containsSemicolon(extras: List<Node.Extra>): Boolean {
        return extras.any {
            it is Node.Extra.Semicolon
        }
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

    companion object {
        fun write(v: Node, extrasMap: ExtrasMap? = null) =
            write(v, StringBuilder(), extrasMap).toString()

        fun <T : Appendable> write(v: Node, app: T, extrasMap: ExtrasMap? = null) =
            app.also { Writer(it, extrasMap).write(v) }
    }
}
