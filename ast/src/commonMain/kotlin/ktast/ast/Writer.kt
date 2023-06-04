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

    protected fun Node.appendLabel(label: Node.Expression.NameExpression?) {
        if (label != null) {
            append('@')
            children(label)
        }
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
                is Node.Statement.ForStatement -> {
                    children(forKeyword, lPar, loopParam, inKeyword, loopRange, rPar, body)
                }
                is Node.Statement.WhileStatement -> {
                    children(whileKeyword, lPar, condition, rPar, body)
                }
                is Node.Statement.DoWhileStatement -> {
                    children(doKeyword, body, whileKeyword, lPar, condition, rPar)
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
                    if (receiverType != null) children(receiverType).append(".")
                    name?.also { children(it) }
                    children(params)
                    if (returnType != null) append(":").also { children(returnType) }
                    children(postModifiers)
                    children(equals)
                    children(body)
                }
                is Node.FunctionParam -> {
                    children(modifiers)
                    children(valOrVarKeyword)
                    children(name)
                    if (type != null) append(":").also { children(type) }
                    children(equals)
                    children(defaultValue)
                }
                is Node.Declaration.PropertyDeclaration -> {
                    children(modifiers)
                    children(valOrVarKeyword)
                    children(typeParams)
                    if (receiverType != null) children(receiverType).append('.')
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
                    if (type != null) append(":").also { children(type) }
                }
                is Node.Declaration.PropertyDeclaration.Getter -> {
                    children(modifiers)
                    children(getKeyword)
                    if (body != null) {
                        append("()")
                        if (type != null) append(":").also { children(type) }
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
                    children(typeParams)
                    children(equals)
                    children(type)
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
                    if (type != null) append(":").also { children(type) }
                }
                is Node.TypeArg.TypeProjection -> {
                    children(modifiers)
                    children(type)
                }
                is Node.TypeArg.StarProjection -> {
                    children(asterisk)
                }
                is Node.Type.FunctionType -> {
                    children(modifiers)
                    children(contextReceiver)
                    children(receiverType)
                    children(dotSymbol)
                    if (params != null) {
                        children(params).append("->")
                    }
                    children(returnType)
                }
                is Node.ContextReceiver -> {
                    append("context")
                    children(receiverTypes)
                }
                is Node.Type.FunctionType.FunctionTypeParam -> {
                    if (name != null) children(name).append(":")
                    children(type)
                }
                is Node.Type.SimpleType -> {
                    children(modifiers)
                    if (qualifiers.isNotEmpty()) {
                        children(qualifiers, ".")
                        append(".")
                    }
                    children(name)
                    children(typeArgs)
                }
                is Node.Type.SimpleType.SimpleTypeQualifier -> {
                    children(name)
                    children(typeArgs)
                }
                is Node.Type.NullableType -> {
                    children(modifiers)
                    children(type)
                    children(questionMark)
                }
                is Node.Type.ParenthesizedType -> {
                    children(modifiers)
                    children(lPar)
                    children(type)
                    children(rPar)
                }
                is Node.Type.DynamicType -> {
                    children(modifiers)
                    append("dynamic")
                }
                is Node.ValueArg -> {
                    if (name != null) children(name).append("=")
                    children(asterisk)
                    children(expression)
                }
                is Node.Expression.IfExpression -> {
                    children(ifKeyword, lPar, condition, rPar, body, elseKeyword, elseBody)
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
                is Node.Expression.BinaryExpression -> {
                    children(lhs, operator, rhs)
                }
                is Node.Expression.PrefixUnaryExpression ->
                    children(operator, expression)
                is Node.Expression.PostfixUnaryExpression ->
                    children(expression, operator)
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
                    doAppend(text)
                is Node.Expression.StringLiteralExpression.EscapeStringEntry -> {
                    doAppend(text)
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
                    append(text)
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
                    children(destructType)
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
                    if (typeArgType != null) append('<').also { children(typeArgType) }.append('>')
                    appendLabel(label)
                }
                is Node.Expression.WhenExpression -> {
                    children(whenKeyword, lPar, expression, rPar)
                    append("{")
                    children(whenBranches)
                    append("}")
                }
                is Node.Expression.WhenExpression.ConditionalWhenBranch -> {
                    children(whenConditions, ",", trailingSeparator = trailingComma)
                    append("->").also { children(body) }
                }
                is Node.Expression.WhenExpression.ElseWhenBranch -> {
                    children(elseKeyword)
                    append("->").also { children(body) }
                }
                is Node.Expression.WhenExpression.ExpressionWhenCondition -> {
                    children(expression)
                }
                is Node.Expression.WhenExpression.RangeWhenCondition -> {
                    children(operator)
                    children(expression)
                }
                is Node.Expression.WhenExpression.TypeWhenCondition -> {
                    children(operator)
                    children(type)
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
                is Node.Expression.LabeledExpression -> {
                    children(label)
                    append("@")
                    children(statement)
                }
                is Node.Expression.AnnotatedExpression ->
                    children(annotationSets).also { children(statement) }
                is Node.Expression.CallExpression -> {
                    children(calleeExpression)
                    children(typeArgs)
                    children(args)
                    children(lambdaArg)
                }
                is Node.Expression.CallExpression.LambdaArg -> {
                    children(annotationSets)
                    if (label != null) {
                        children(label)
                        append("@")
                    }
                    children(expression)
                }
                is Node.Expression.IndexedAccessExpression -> {
                    children(expression)
                    children(indices, ",", "[", "]", trailingComma)
                }
                is Node.Expression.AnonymousFunctionExpression ->
                    children(function)
                is Node.Expression.PropertyExpression ->
                    children(property)
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
                    children(type)
                }
                is Node.PostModifier.Contract -> {
                    children(contractKeyword)
                    children(contractEffects)
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
