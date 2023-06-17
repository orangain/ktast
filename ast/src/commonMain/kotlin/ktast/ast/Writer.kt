package ktast.ast

/**
 * Writer that converts AST nodes back to source code. When the [extrasMap] is provided, it will preserve the original whitespaces, comments, semicolons and trailing commas. Even when the [extrasMap] is not provided, it will still insert whitespaces and semicolons automatically to avoid compilation errors or loss of meaning.
 *
 * Example usage:
 * ```
 * // Without extras map
 * val source = Writer.write(node)
 * // With extras map
 * val sourceWithExtras = Writer.write(node, extrasMap)
 * ```
 */
open class Writer(
    protected val appendable: Appendable = StringBuilder(),
    protected val extrasMap: ExtrasMap? = null
) : Visitor() {
    companion object {
        /**
         * Converts the given AST node back to source code.
         *
         * @param rootNode root AST node to convert.
         * @param extrasMap optional extras map, defaults to null.
         * @return source code.
         */
        fun write(rootNode: Node, extrasMap: ExtrasMap? = null): String =
            write(rootNode, StringBuilder(), extrasMap).toString()

        /**
         * Converts the given AST node back to source code.
         *
         * @param rootNode root AST node to convert.
         * @param appendable appendable to write to.
         * @param extrasMap optional extras map, defaults to null.
         * @return appendable.
         */
        fun <T : Appendable> write(rootNode: Node, appendable: T, extrasMap: ExtrasMap? = null): T =
            appendable.also { Writer(it, extrasMap).write(rootNode) }
    }

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

    /**
     * Converts the given AST node back to source code.
     *
     * @param rootNode root AST node to convert.
     */
    fun write(rootNode: Node) {
        extrasSinceLastNonSymbol.clear()
        nextHeuristicWhitespace = ""
        lastAppendedToken = ""
        traverse(rootNode)
    }

    protected fun NodePath<*>.appendLabel(label: Node.Expression.NameExpression?) {
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
        appendable.append(str)
        lastAppendedToken = str
    }

    override fun visit(path: NodePath<*>): Unit = path.run {
        writeExtrasBefore()
        writeHeuristicNewline()
        writeHeuristicSpace()

        node.run {
            when (this) {
                is Node.KotlinFile -> {
                    children(annotationSets)
                    children(packageDirective)
                    children(importDirectives)
                    children(declarations)
                    writeExtrasWithin()
                }
                is Node.KotlinScript -> {
                    children(annotationSets)
                    children(packageDirective)
                    children(importDirectives)
                    children(expressions)
                    writeExtrasWithin()
                }
                is Node.PackageDirective -> {
                    append("package")
                    children(names, ".")
                }
                is Node.ImportDirective -> {
                    append("import")
                    children(names, ".")
                    if (aliasName != null) {
                        append("as")
                        children(aliasName)
                    }
                }
                is Node.Statement.ForStatement -> {
                    append("for")
                    children(lPar, loopParam, inKeyword, loopRange, rPar, body)
                }
                is Node.Statement.WhileStatement -> {
                    append("while")
                    children(lPar, condition, rPar, body)
                }
                is Node.Statement.DoWhileStatement -> {
                    append("do")
                    children(body)
                    append("while")
                    children(lPar, condition, rPar)
                }
                is Node.Declaration.ClassDeclaration -> {
                    children(modifiers)
                    children(classDeclarationKeyword)
                    children(name)
                    commaSeparatedChildren(lAngle, typeParams, rAngle)
                    children(primaryConstructor)
                    if (classParents.isNotEmpty()) {
                        append(":")
                    }
                    commaSeparatedChildren(classParents)
                    children(typeConstraintSet)
                    children(classBody)
                }
                is Node.Declaration.ClassDeclaration.ConstructorClassParent -> {
                    children(type)
                    commaSeparatedChildren(lPar, args, rPar)
                }
                is Node.Declaration.ClassDeclaration.DelegationClassParent -> {
                    children(type)
                    append("by")
                    children(expression)
                }
                is Node.Declaration.ClassDeclaration.TypeClassParent -> {
                    children(type)
                }
                is Node.Declaration.ClassDeclaration.PrimaryConstructor -> {
                    children(modifiers)
                    children(constructorKeyword)
                    commaSeparatedChildren(lPar, params, rPar)
                }
                is Node.Declaration.ClassDeclaration.ClassBody -> {
                    writeBlock {
                        children(enumEntries, ",")
                        if (enumEntries.isNotEmpty() && declarations.isNotEmpty() && !containsSemicolon(
                                extrasSinceLastNonSymbol
                            )
                        ) {
                            append(";") // Insert heuristic semicolon after the last enum entry
                        }
                        children(declarations)
                    }
                }
                is Node.Declaration.ClassDeclaration.ClassBody.Initializer -> {
                    append("init")
                    children(block)
                }
                is Node.Declaration.FunctionDeclaration -> {
                    children(modifiers)
                    append("fun")
                    commaSeparatedChildren(lAngle, typeParams, rAngle)
                    if (receiverType != null) children(receiverType).append(".")
                    name?.also { children(it) }
                    commaSeparatedChildren(lPar, params, rPar)
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
                    commaSeparatedChildren(lAngle, typeParams, rAngle)
                    if (receiverType != null) children(receiverType).append('.')
                    children(lPar)
                    children(variables, ",")
                    children(rPar)
                    children(typeConstraintSet)
                    children(equals)
                    children(initializer)
                    children(propertyDelegate)
                    children(accessors)
                }
                is Node.Declaration.PropertyDeclaration.PropertyDelegate -> {
                    append("by")
                    children(expression)
                }
                is Node.Variable -> {
                    children(annotationSets)
                    children(name)
                    if (type != null) append(":").also { children(type) }
                }
                is Node.Declaration.PropertyDeclaration.Getter -> {
                    children(modifiers)
                    append("get")
                    children(lPar, rPar)
                    if (type != null) append(":").also { children(type) }
                    children(postModifiers)
                    children(equals)
                    children(body)
                }
                is Node.Declaration.PropertyDeclaration.Setter -> {
                    children(modifiers)
                    append("set")
                    commaSeparatedChildren(lPar, params, rPar)
                    children(postModifiers)
                    children(equals)
                    children(body)
                }
                is Node.Declaration.TypeAliasDeclaration -> {
                    children(modifiers)
                    append("typealias")
                    children(name)
                    commaSeparatedChildren(lAngle, typeParams, rAngle)
                    children(equals)
                    children(type)
                }
                is Node.Declaration.ClassDeclaration.ClassBody.SecondaryConstructor -> {
                    children(modifiers)
                    children(constructorKeyword)
                    commaSeparatedChildren(lPar, params, rPar)
                    if (delegationCall != null) append(":").also { children(delegationCall) }
                    children(block)
                }
                is Node.Declaration.ClassDeclaration.ClassBody.EnumEntry -> {
                    children(modifiers)
                    children(name)
                    commaSeparatedChildren(lPar, args, rPar)
                    children(classBody)
                }
                is Node.TypeParam -> {
                    children(modifiers)
                    children(name)
                    if (type != null) append(":").also { children(type) }
                }
                is Node.TypeArg -> {
                    children(modifiers)
                    children(type)
                }
                is Node.Type.FunctionType -> {
                    children(modifiers)
                    children(contextReceiver)
                    children(receiverType)
                    children(dotSymbol)
                    commaSeparatedChildren(lPar, params, rPar)
                    append("->")
                    children(returnType)
                }
                is Node.ContextReceiver -> {
                    append("context")
                    commaSeparatedChildren(lPar, receiverTypes, rPar)
                }
                is Node.Type.FunctionType.FunctionTypeParam -> {
                    if (name != null) children(name).append(":")
                    children(type)
                }
                is Node.Type.SimpleType -> {
                    children(modifiers)
                    children(pieces, ".")
                }
                is Node.Type.SimpleType.SimpleTypePiece -> {
                    children(name)
                    commaSeparatedChildren(lAngle, typeArgs, rAngle)
                }
                is Node.Type.NullableType -> {
                    children(modifiers)
                    children(innerType)
                    children(questionMark)
                }
                is Node.Type.ParenthesizedType -> {
                    children(modifiers)
                    children(lPar)
                    children(innerType)
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
                    append("if")
                    children(lPar, condition, rPar, body)
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
                    append("catch")
                    commaSeparatedChildren(lPar, params, rPar)
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
                    append('(').also { children(innerExpression) }.append(')')
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
                    writeBlock {
                        commaSeparatedChildren(params)
                        children(arrow)
                        children(statements)
                    }
                }
                is Node.LambdaParam -> {
                    children(lPar)
                    children(variables, ",")
                    children(rPar)
                    children(colon)
                    children(destructType)
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
                    children(whenKeyword, subject)
                    writeBlock {
                        children(whenBranches)
                    }
                }
                is Node.Expression.WhenExpression.WhenSubject -> {
                    children(lPar)
                    children(annotationSets)
                    children(valKeyword, variable)
                    if (variable != null) {
                        append("=")
                    }
                    children(expression, rPar)
                }
                is Node.Expression.WhenExpression.ConditionalWhenBranch -> {
                    children(whenConditions, ",")
                    children(arrow, body)
                }
                is Node.Expression.WhenExpression.ElseWhenBranch -> {
                    append("else")
                    children(arrow, body)
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
                    children(expressions, ",", "[", "]")
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
                    commaSeparatedChildren(lAngle, typeArgs, rAngle)
                    commaSeparatedChildren(lPar, args, rPar)
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
                    children(indices, ",", "[", "]")
                }
                is Node.Expression.AnonymousFunctionExpression ->
                    children(function)
                is Node.Expression.BlockExpression -> {
                    writeBlock {
                        children(statements)
                    }
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
                    commaSeparatedChildren(lPar, args, rPar)
                    val parentNode = path.parent?.node
                    if (parentNode is Node.Modifier.AnnotationSet && parentNode.rBracket == null) {
                        nextHeuristicWhitespace = " " // Insert heuristic space after annotation if single form
                    }
                }
                is Node.PostModifier.TypeConstraintSet -> {
                    append("where")
                    commaSeparatedChildren(constraints)
                }
                is Node.PostModifier.TypeConstraintSet.TypeConstraint -> {
                    children(annotationSets)
                    children(name)
                    append(":")
                    children(type)
                }
                is Node.PostModifier.Contract -> {
                    append("contract")
                    commaSeparatedChildren(lBracket, contractEffects, rBracket)
                }
                is Node.Keyword -> {
                    append(text)
                }
                is Node.Extra -> error("Extra nodes must not be visited. node: $this")
            }
            Unit
        }
        writeExtrasAfter()
    }

    protected fun NodePath<*>.children(vararg v: Node?) = this@Writer.also { v.forEach { visitChildren(it) } }

    protected fun NodePath<*>.commaSeparatedChildren(elements: List<Node>) =
        commaSeparatedChildren(null, elements, null)

    protected fun NodePath<*>.commaSeparatedChildren(prefix: Node?, elements: List<Node>, suffix: Node?) =
        this@Writer.also {
            visitChildren(prefix)
            children(elements, ",")
            visitChildren(suffix)
        }

    protected fun NodePath<*>.children(
        v: List<Node>,
        sep: String = "",
        prefix: String = "",
        suffix: String = "",
    ) =
        this@Writer.also {
            append(prefix)
            v.forEachIndexed { index, child ->
                val childPath = childPathOf(child)
                visit(childPath)
                if (index < v.size - 1) append(sep)
                childPath.writeHeuristicExtraAfterChild(v.getOrNull(index + 1))
            }
            append(suffix)
        }

    private fun NodePath<*>.writeBlock(block: () -> Unit) {
        append("{")
        block()
        writeExtrasWithin()
        append("}")
    }

    protected open fun NodePath<*>.writeHeuristicNewline() {
        val parentNode = parent?.node
        if (parentNode is Node.WithStatements && node is Node.Statement) {
            if (parentNode.statements.first() !== node && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
        if (parentNode is Node.WithDeclarations && node is Node.Declaration) {
            if (parentNode.declarations.first() !== node && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
        if (parentNode is Node.Declaration.PropertyDeclaration && node is Node.Declaration.PropertyDeclaration.Accessor) {
            // Property accessors require newline when the previous element is expression
            if ((parentNode.accessors.first() === node && (parentNode.propertyDelegate != null || parentNode.initializer != null)) ||
                (parentNode.accessors.size == 2 && parentNode.accessors.last() === node && parentNode.accessors[0].equals != null)
            ) {
                if (!containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                    append("\n")
                }
            }
        }
        if (parentNode is Node.Expression.WhenExpression && node is Node.Expression.WhenExpression.WhenBranch) {
            if (parentNode.whenBranches.first() !== node && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
        if (parentNode is Node.Expression.AnnotatedExpression && (node is Node.Expression.BinaryExpression || node is Node.Expression.BinaryTypeExpression)) {
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

    protected open fun NodePath<*>.writeHeuristicExtraAfterChild(next: Node?) {
        if (node is Node.Expression.NameExpression && modifierKeywords.contains(node.text) && next is Node.Declaration && parent?.node is Node.WithStatements) {
            // Insert heuristic semicolon after name expression whose name is the same as the modifier keyword and next
            // is declaration to avoid ambiguity with keyword modifier.
            if (!containsSemicolon(extrasSinceLastNonSymbol)) {
                append(";")
            }
        }
        if (node is Node.Expression.CallExpression && node.lambdaArg == null && next is Node.Expression.LambdaExpression) {
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

    protected open fun NodePath<*>.writeExtrasBefore() {
        if (extrasMap == null) return
        writeExtras(extrasMap.extrasBefore(node))
    }

    protected open fun NodePath<*>.writeExtrasWithin() {
        if (extrasMap == null) return
        writeExtras(extrasMap.extrasWithin(node))
    }

    protected open fun NodePath<*>.writeExtrasAfter() {
        if (extrasMap == null) return
        writeExtras(extrasMap.extrasAfter(node))
    }

    protected open fun NodePath<*>.writeExtras(extras: List<Node.Extra>) {
        extras.forEach {
            append(it.text)
        }
        extrasSinceLastNonSymbol.addAll(extras)
    }
}
