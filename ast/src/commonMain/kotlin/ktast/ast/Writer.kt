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
        if (heuristicSpaceInsertionCriteria.any { it(lastAppendedToken.lastOrNull(), str.firstOrNull()) }) {
            doAppend(" ")
        }
        doAppend(str)
    }

    protected val heuristicSpaceInsertionCriteria: List<(Char?, Char?) -> Boolean> = listOf(
        // Insert heuristic space between '>' and '=' not to be confused with '>='
        { last, next -> last == '>' && next == '=' },
        // Insert heuristic space between two non-symbols
        { last, next -> isNonSymbol(last) && isNonSymbol(next) },
    )

    protected fun isNonSymbol(ch: Char?) =
        ch != null && (ch == '_' || nonSymbolCategories.contains(ch.category))

    protected fun doAppend(str: String) {
        if (str == "") return
        app.append(str)
        lastAppendedToken = str
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
                    children(importAlias)
                }
                is Node.ImportDirective.ImportAlias -> {
                    append("as")
                    children(name)
                }
                is Node.Declaration.ClassDeclaration -> {
                    children(modifiers)
                    children(classDeclarationKeyword)
                    children(name)
                    children(typeParams)
                    children(primaryConstructor)
                    if (classParents != null) {
                        append(":")
                        children(classParents)
                    }
                    children(typeConstraintSet)
                    children(classBody)
                }
                is Node.Declaration.ClassDeclaration.ConstructorClassParent -> {
                    children(type)
                    children(args)
                }
                is Node.Declaration.ClassDeclaration.DelegationClassParent -> {
                    children(type)
                    children(byKeyword)
                    children(expression)
                }
                is Node.Declaration.ClassDeclaration.TypeClassParent -> {
                    children(type)
                }
                is Node.Declaration.ClassDeclaration.PrimaryConstructor -> {
                    children(modifiers)
                    children(constructorKeyword)
                    children(params)
                }
                is Node.Declaration.ClassDeclaration.ClassBody -> {
                    append("{")
                    children(enumEntries, skipWritingExtrasWithin = true)
                    children(declarations)
                    append("}")
                }
                is Node.Declaration.ClassDeclaration.ClassBody.Initializer -> {
                    children(modifiers)
                    append("init")
                    children(block)
                }
                is Node.Declaration.FunctionDeclaration -> {
                    children(modifiers)
                    children(funKeyword)
                    children(typeParams)
                    if (receiverTypeRef != null) children(receiverTypeRef).append(".")
                    name?.also { children(it) }
                    children(params)
                    if (typeRef != null) append(":").also { children(typeRef) }
                    children(postModifiers)
                    children(equals)
                    children(body)
                }
                is Node.FunctionParam -> {
                    children(modifiers)
                    children(valOrVarKeyword)
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                    children(equals)
                    children(defaultValue)
                }
                is Node.Declaration.PropertyDeclaration -> {
                    children(modifiers)
                    children(valOrVarKeyword)
                    children(typeParams)
                    if (receiverTypeRef != null) children(receiverTypeRef).append('.')
                    children(lPar)
                    children(variables, ",")
                    children(trailingComma)
                    children(rPar)
                    children(typeConstraintSet)
                    children(equals)
                    children(initializer)
                    children(propertyDelegate)
                    children(accessors)
                }
                is Node.Declaration.PropertyDeclaration.PropertyDelegate -> {
                    children(byKeyword)
                    children(expression)
                }
                is Node.Variable -> {
                    children(modifiers)
                    children(name)
                    if (typeRef != null) append(":").also { children(typeRef) }
                }
                is Node.Declaration.PropertyDeclaration.Getter -> {
                    children(modifiers)
                    children(getKeyword)
                    if (body != null) {
                        append("()")
                        if (typeRef != null) append(":").also { children(typeRef) }
                        children(postModifiers)
                        children(equals)
                        children(body)
                    }
                }
                is Node.Declaration.PropertyDeclaration.Setter -> {
                    children(modifiers)
                    children(setKeyword)
                    if (body != null) {
                        append("(")
                        children(params)
                        append(")")
                        children(postModifiers)
                        children(equals)
                        children(body)
                    }
                }
                is Node.Declaration.TypeAliasDeclaration -> {
                    children(modifiers)
                    append("typealias")
                    children(name)
                    children(typeParams).append("=")
                    children(typeRef)
                }
                is Node.Declaration.ClassDeclaration.ClassBody.SecondaryConstructor -> {
                    children(modifiers)
                    children(constructorKeyword)
                    children(params)
                    if (delegationCall != null) append(":").also { children(delegationCall) }
                    children(block)
                }
                is Node.Declaration.ClassDeclaration.ClassBody.EnumEntry -> {
                    children(modifiers)
                    children(name)
                    children(args)
                    children(classBody)
                    check(parent is Node.Declaration.ClassDeclaration.ClassBody) // condition should always be true
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
                    children(type)
                    children(rPar)
                }
                is Node.Type.FunctionType -> {
                    children(lPar)
                    children(modifiers)
                    if (contextReceivers != null) {
                        append("context")
                        children(contextReceivers)
                    }
                    if (functionTypeReceiver != null) {
                        children(functionTypeReceiver)
                        append('.')
                    }
                    if (params != null) {
                        children(params).append("->")
                    }
                    children(returnTypeRef)
                    children(rPar)
                }
                is Node.Type.FunctionType.ContextReceiver -> {
                    children(typeRef)
                }
                is Node.Type.FunctionType.FunctionTypeReceiver -> {
                    children(typeRef)
                }
                is Node.Type.FunctionType.FunctionTypeParam -> {
                    if (name != null) children(name).append(":")
                    children(typeRef)
                }
                is Node.Type.SimpleType -> {
                    if (qualifiers.isNotEmpty()) {
                        children(qualifiers, ".")
                        append(".")
                    }
                    children(name)
                    children(typeArgs)
                }
                is Node.Type.SimpleType.Qualifier -> {
                    children(name)
                    children(typeArgs)
                }
                is Node.Type.NullableType -> {
                    children(lPar)
                    children(modifiers)
                    children(type)
                    children(rPar)
                    append('?')
                }
                is Node.Type.DynamicType ->
                    append("dynamic")
                is Node.ExpressionContainer -> {
                    children(expression)
                }
                is Node.ValueArg -> {
                    if (name != null) children(name).append("=")
                    if (asterisk) append('*')
                    children(expression)
                }
                is Node.Expression.IfExpression -> {
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
                is Node.Expression.TryExpression -> {
                    append("try")
                    children(block)
                    if (catchClauses.isNotEmpty()) children(catchClauses)
                    if (finallyBlock != null) append("finally").also { children(finallyBlock) }
                }
                is Node.Expression.TryExpression.CatchClause -> {
                    children(catchKeyword)
                    children(params)
                    children(block)
                }
                is Node.Expression.ForExpression -> {
                    children(forKeyword)
                    append("(")
                    children(loopParam)
                    append("in")
                    children(loopRange)
                    append(")")
                    children(body)
                }
                is Node.Expression.WhileExpression -> {
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
                is Node.Expression.BinaryExpression -> {
                    children(lhs, operator, rhs)
                }
                is Node.Expression.UnaryExpression ->
                    if (prefix) children(operator, expression) else children(expression, operator)
                is Node.Expression.BinaryTypeExpression ->
                    children(listOf(lhs, operator, rhs), "")
                is Node.Expression.CallableReferenceExpression -> {
                    if (lhs != null) children(lhs)
                    children(questionMarks)
                    append("::")
                    children(rhs)
                }
                is Node.Expression.ClassLiteralExpression -> {
                    if (lhs != null) children(lhs)
                    children(questionMarks)
                    append("::")
                    append("class")
                }
                is Node.Expression.ParenthesizedExpression ->
                    append('(').also { children(expression) }.append(')')
                is Node.Expression.StringLiteralExpression -> {
                    if (raw) {
                        append("\"\"\"")
                        children(entries)
                        append("\"\"\"")
                    } else {
                        append('"')
                        children(entries)
                        append('"')
                    }
                }
                is Node.Expression.StringLiteralExpression.LiteralStringEntry ->
                    doAppend(str)
                is Node.Expression.StringLiteralExpression.EscapeStringEntry -> {
                    doAppend(str)
                }
                is Node.Expression.StringLiteralExpression.TemplateStringEntry -> {
                    val (prefix, suffix) = if (short) {
                        Pair("$", "")
                    } else {
                        Pair("\${", "}")
                    }
                    doAppend(prefix)
                    children(expression)
                    doAppend(suffix)
                }
                is Node.Expression.ConstantLiteralExpression ->
                    append(value)
                is Node.Expression.LambdaExpression -> {
                    append("{")
                    if (params != null) {
                        children(params)
                        append("->")
                    }
                    children(lambdaBody)
                    append("}")
                }
                is Node.LambdaParam -> {
                    children(lPar)
                    children(variables, ",")
                    children(trailingComma)
                    children(rPar)
                    children(colon)
                    children(destructTypeRef)
                }
                is Node.Expression.LambdaExpression.LambdaBody -> {
                    children(statements)
                }
                is Node.Expression.ThisExpression -> {
                    append("this")
                    appendLabel(label)
                }
                is Node.Expression.SuperExpression -> {
                    append("super")
                    if (typeArg != null) append('<').also { children(typeArg) }.append('>')
                    appendLabel(label)
                }
                is Node.Expression.WhenExpression -> {
                    children(whenKeyword, lPar, expression, rPar)
                    append("{")
                    children(whenBranches)
                    append("}")
                }
                is Node.Expression.WhenExpression.WhenBranch -> {
                    children(whenConditions, ",", trailingSeparator = trailingComma)
                    children(elseKeyword)
                    append("->").also { children(body) }
                }
                is Node.Expression.WhenExpression.WhenCondition -> {
                    children(operator)
                    children(expression)
                    children(typeRef)
                }
                is Node.Expression.ObjectLiteralExpression -> {
                    children(declaration)
                }
                is Node.Expression.ThrowExpression ->
                    append("throw").also { children(expression) }
                is Node.Expression.ReturnExpression -> {
                    append("return")
                    appendLabel(label)
                    children(expression)
                }
                is Node.Expression.ContinueExpression -> {
                    append("continue")
                    appendLabel(label)
                }
                is Node.Expression.BreakExpression -> {
                    append("break")
                    appendLabel(label)
                }
                is Node.Expression.CollectionLiteralExpression ->
                    children(expressions, ",", "[", "]", trailingComma)
                is Node.Expression.NameExpression ->
                    append(text)
                is Node.Expression.LabeledExpression ->
                    append(label).append("@").also { children(expression) }
                is Node.Expression.AnnotatedExpression ->
                    children(annotationSets).also { children(expression) }
                is Node.Expression.CallExpression -> {
                    children(expression)
                    children(typeArgs)
                    children(args)
                    children(lambdaArg)
                }
                is Node.Expression.CallExpression.LambdaArg -> {
                    children(annotationSets)
                    if (label != null) append(label).append("@")
                    children(expression)
                }
                is Node.Expression.ArrayAccessExpression -> {
                    children(expression)
                    children(indices, ",", "[", "]", trailingComma)
                }
                is Node.Expression.AnonymousFunctionExpression ->
                    children(function)
                is Node.Expression.PropertyExpression ->
                    children(declaration)
                is Node.Expression.BlockExpression -> {
                    append("{").run {
                        children(statements)
                    }
                    append("}")
                }
                is Node.Modifier.AnnotationSet -> {
                    children(atSymbol)
                    children(target)
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
                is Node.PostModifier.TypeConstraintSet -> {
                    children(whereKeyword)
                    children(constraints)
                }
                is Node.PostModifier.TypeConstraintSet.TypeConstraint -> {
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
                is Node.Keyword -> {
                    append(text)
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
        if (parent is Node.Declaration.PropertyDeclaration && this is Node.Declaration.PropertyDeclaration.Accessor) {
            // Property accessors require newline when the previous element is expression
            if ((parent.accessors.first() === this && (parent.propertyDelegate != null || parent.initializer != null)) ||
                (parent.accessors.size == 2 && parent.accessors.last() === this && parent.accessors[0].equals != null)
            ) {
                if (!containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                    append("\n")
                }
            }
        }
        if (parent is Node.Expression.WhenExpression && this is Node.Expression.WhenExpression.WhenBranch) {
            if (parent.whenBranches.first() !== this && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
        if (parent is Node.Expression.AnnotatedExpression && (this is Node.Expression.BinaryExpression || this is Node.Expression.BinaryTypeExpression)) {
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

    // See: https://kotlinlang.org/docs/keyword-reference.html#modifier-keywords
    private val modifierKeywords = setOf(
        "abstract",
        "actual",
        "annotation",
        "companion",
        "const",
        "crossinline",
        "data",
        "enum",
        "expect",
        "external",
        "final",
        "infix",
        "inline",
        "inner",
        "internal",
        "lateinit",
        "noinline",
        "open",
        "operator",
        "out",
        "override",
        "private",
        "protected",
        "public",
        "reified",
        "sealed",
        "suspend",
        "tailrec",
        "vararg",
    )

    protected open fun writeHeuristicExtraAfterChild(v: Node, next: Node?, parent: Node?) {
        if (v is Node.Expression.NameExpression && modifierKeywords.contains(v.text) && next is Node.Declaration && parent is Node.StatementsContainer) {
            // Insert heuristic semicolon after name expression whose name is the same as the modifier keyword and next
            // is declaration to avoid ambiguity with keyword modifier.
            if (!containsSemicolon(extrasSinceLastNonSymbol)) {
                append(";")
            }
        }
        if (v is Node.Expression.CallExpression && v.lambdaArg == null && next is Node.Expression.LambdaExpression) {
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
