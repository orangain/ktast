package ktast.ast

/**
 * Writer that converts AST nodes back to source code. When the [withExtras] is true, which is the default, it will preserve the original whitespaces, comments, semicolons and trailing commas. Even when the [withExtras] is false, it will still insert minimal whitespaces and semicolons automatically to avoid compilation errors or loss of meaning.
 *
 * Example usage:
 * ```
 * // Write with extras
 * val source = Writer.write(node)
 * // Write without extras
 * val sourceWithoutExtras = Writer.write(node, withExtras = false)
 * ```
 */
open class Writer(
    protected val appendable: Appendable = StringBuilder(),
    protected val withExtras: Boolean = true,
) : Visitor() {
    companion object {
        /**
         * Converts the given AST node back to source code.
         *
         * @param rootNode root AST node to convert.
         * @param withExtras whether to write extra nodes such as comments and whitespaces, defaults to true.
         * @return source code.
         */
        fun write(rootNode: Node, withExtras: Boolean = true): String =
            write(rootNode, StringBuilder(), withExtras).toString()

        /**
         * Converts the given AST node back to source code.
         *
         * @param rootNode root AST node to convert.
         * @param appendable appendable to write to.
         * @param withExtras whether to write extra nodes such as comments and whitespaces, defaults to true.
         * @return appendable.
         */
        fun <T : Appendable> write(rootNode: Node, appendable: T, withExtras: Boolean = true): T =
            appendable.also { Writer(it, withExtras).write(rootNode) }
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
                    children(lPar, loopParameter, inKeyword, loopRange, rPar, body)
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
                is Node.Declaration.ClassOrObject.ConstructorClassParent -> {
                    children(type)
                    commaSeparatedChildren(lPar, arguments, rPar)
                }
                is Node.Declaration.ClassOrObject.DelegationClassParent -> {
                    children(type)
                    append("by")
                    children(expression)
                }
                is Node.Declaration.ClassOrObject.TypeClassParent -> {
                    children(type)
                }
                is Node.Declaration.ClassOrObject.ClassBody -> {
                    writeBlock {
                        children(enumEntries) // Comma between enum entries is represented as a trailing comma of each enum entry
                        if (enumEntries.isNotEmpty() && declarations.isNotEmpty() && !containsSemicolon(
                                extrasSinceLastNonSymbol
                            )
                        ) {
                            append(";") // Insert heuristic semicolon after the last enum entry
                        }
                        children(declarations)
                    }
                }
                is Node.Declaration.ClassOrObject.ClassBody.EnumEntry -> {
                    children(modifiers)
                    children(name)
                    commaSeparatedChildren(lPar, arguments, rPar)
                    children(classBody)
                }
                is Node.Declaration.ClassOrObject.ClassBody.Initializer -> {
                    append("init")
                    children(block)
                }
                is Node.Declaration.ClassOrObject.ClassBody.SecondaryConstructor -> {
                    children(modifiers)
                    children(constructorKeyword)
                    commaSeparatedChildren(lPar, parameters, rPar)
                    if (delegationCall != null) append(":").also { children(delegationCall) }
                    children(block)
                }
                is Node.Declaration.ClassDeclaration -> {
                    children(modifiers)
                    children(declarationKeyword)
                    children(name)
                    commaSeparatedChildren(lAngle, typeParameters, rAngle)
                    children(primaryConstructor)
                    if (parents.isNotEmpty()) {
                        append(":")
                    }
                    commaSeparatedChildren(parents)
                    children(typeConstraintSet)
                    children(body)
                }
                is Node.Declaration.ClassDeclaration.PrimaryConstructor -> {
                    children(modifiers)
                    children(constructorKeyword)
                    commaSeparatedChildren(lPar, parameters, rPar)
                }
                is Node.Declaration.ObjectDeclaration -> {
                    children(modifiers)
                    children(declarationKeyword)
                    children(name)
                    if (parents.isNotEmpty()) {
                        append(":")
                    }
                    commaSeparatedChildren(parents)
                    children(body)
                }
                is Node.Declaration.FunctionDeclaration -> {
                    children(modifiers)
                    append("fun")
                    commaSeparatedChildren(lAngle, typeParameters, rAngle)
                    if (receiverType != null) children(receiverType).append(".")
                    name?.also { children(it) }
                    commaSeparatedChildren(lPar, parameters, rPar)
                    if (returnType != null) append(":").also { children(returnType) }
                    children(postModifiers)
                    writeFunctionBody(body)
                }
                is Node.Declaration.PropertyDeclaration -> {
                    children(modifiers)
                    children(valOrVarKeyword)
                    commaSeparatedChildren(lAngle, typeParameters, rAngle)
                    if (receiverType != null) children(receiverType).append('.')
                    children(lPar)
                    children(variables, ",")
                    children(rPar)
                    children(typeConstraintSet)
                    writeFunctionBody(initializerExpression)
                    if (delegateExpression != null) {
                        append("by")
                        children(delegateExpression)
                    }
                    children(accessors)
                }
                is Node.Declaration.PropertyDeclaration.Getter -> {
                    children(modifiers)
                    append("get")
                    children(lPar, rPar)
                    if (type != null) append(":").also { children(type) }
                    children(postModifiers)
                    writeFunctionBody(body)
                }
                is Node.Declaration.PropertyDeclaration.Setter -> {
                    children(modifiers)
                    append("set")
                    children(lPar, parameter, rPar)
                    children(postModifiers)
                    writeFunctionBody(body)
                }
                is Node.Declaration.TypeAliasDeclaration -> {
                    children(modifiers)
                    append("typealias")
                    children(name)
                    commaSeparatedChildren(lAngle, typeParameters, rAngle)
                    append("=")
                    children(type)
                }
                is Node.Declaration.ScriptBody -> {
                    children(declarations)
                    writeExtrasWithin()
                }
                is Node.Declaration.ScriptInitializer -> {
                    children(body)
                }
                is Node.Type.ParenthesizedType -> {
                    children(modifiers)
                    children(lPar)
                    children(innerType)
                    children(rPar)
                }
                is Node.Type.NullableType -> {
                    children(modifiers)
                    children(innerType)
                    children(questionMark)
                }
                is Node.Type.SimpleType -> {
                    children(modifiers)
                    if (qualifiers.isNotEmpty()) {
                        children(qualifiers, ".")
                        append(".")
                    }
                    children(name)
                    commaSeparatedChildren(lAngle, typeArguments, rAngle)
                }
                is Node.Type.SimpleType.SimpleTypeQualifier -> {
                    children(name)
                    commaSeparatedChildren(lAngle, typeArguments, rAngle)
                }
                is Node.Type.DynamicType -> {
                    children(modifiers)
                    append("dynamic")
                }
                is Node.Type.FunctionType -> {
                    children(modifiers)
                    children(contextReceiver)
                    children(receiverType)
                    if (receiverType != null) append(".")
                    commaSeparatedChildren(lPar, parameters, rPar)
                    append("->")
                    children(returnType)
                }
                is Node.Type.FunctionType.FunctionTypeParameter -> {
                    if (name != null) children(name).append(":")
                    children(type)
                }
                is Node.Type.IntersectionType -> {
                    children(modifiers)
                    children(leftType)
                    append("&")
                    children(rightType)
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
                    commaSeparatedChildren(lPar, parameters, rPar)
                    children(block)
                }
                is Node.Expression.WhenExpression -> {
                    children(whenKeyword, subject)
                    writeBlock {
                        children(branches)
                    }
                }
                is Node.Expression.WhenExpression.WhenSubject -> {
                    children(lPar)
                    children(annotationSets)
                    if (variable != null) {
                        children(variable)
                        append("=")
                    }
                    children(expression, rPar)
                }
                is Node.Expression.WhenExpression.ConditionalWhenBranch -> {
                    children(conditions, ",")
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
                is Node.Expression.BlockExpression -> {
                    writeBlock {
                        children(statements)
                    }
                }
                is Node.Expression.CallExpression -> {
                    children(calleeExpression)
                    commaSeparatedChildren(lAngle, typeArguments, rAngle)
                    commaSeparatedChildren(lPar, arguments, rPar)
                    children(lambdaArgument)
                }
                is Node.Expression.LambdaExpression -> {
                    writeBlock {
                        commaSeparatedChildren(parameters)
                        children(arrow)
                        children(statements)
                    }
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
                    children(lhs)
                    children(questionMarks)
                    append("::")
                    append("class")
                }
                is Node.Expression.ParenthesizedExpression ->
                    append('(').also { children(innerExpression) }.append(')')
                is Node.Expression.StringLiteralExpression -> {
                    if (interpolationPrefix != null) {
                        append(interpolationPrefix)
                    }
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
                        Pair(prefix, "")
                    } else {
                        Pair("$prefix{", "}")
                    }
                    doAppend(prefix)
                    children(expression)
                    doAppend(suffix)
                }
                is Node.Expression.ConstantLiteralExpression ->
                    append(text)
                is Node.Expression.ObjectLiteralExpression -> {
                    children(declaration)
                }
                is Node.Expression.CollectionLiteralExpression ->
                    children(expressions, ",", "[", "]")
                is Node.Expression.ThisExpression -> {
                    append("this")
                    appendLabel(label)
                }
                is Node.Expression.SuperExpression -> {
                    append("super")
                    if (typeArgument != null) append('<').also { children(typeArgument) }.append('>')
                    appendLabel(label)
                }
                is Node.Expression.NameExpression ->
                    append(text)
                is Node.Expression.LabeledExpression -> {
                    children(label)
                    append("@")
                    children(statement)
                }
                is Node.Expression.AnnotatedExpression ->
                    children(annotationSets).also { children(statement) }
                is Node.Expression.IndexedAccessExpression -> {
                    children(expression)
                    children(indices, ",", "[", "]")
                }
                is Node.Expression.AnonymousFunctionExpression ->
                    children(function)
                is Node.TypeParameter -> {
                    children(modifiers)
                    children(name)
                    if (type != null) append(":").also { children(type) }
                }
                is Node.FunctionParameter -> {
                    children(modifiers)
                    children(valOrVarKeyword)
                    children(name)
                    if (type != null) append(":").also { children(type) }
                    writeFunctionBody(defaultValue)
                }
                is Node.LambdaParameter -> {
                    children(lPar)
                    children(variables, ",")
                    children(rPar)
                    if (destructuringType != null) {
                        append(":")
                        children(destructuringType)
                    }
                }
                is Node.Variable -> {
                    children(annotationSets)
                    if (parent?.node is Node.Expression.WhenExpression.WhenSubject) {
                        // Output "val" here rather than in WhenSubject because "val" token exists inside KtProperty.
                        append("val")
                    }
                    children(name)
                    if (type != null) append(":").also { children(type) }
                }
                is Node.TypeArgument -> {
                    children(modifiers)
                    children(type)
                }
                is Node.ValueArgument -> {
                    if (name != null) children(name).append("=")
                    children(spreadOperator)
                    children(expression)
                }
                is Node.Modifier.ContextReceiver -> {
                    append("context")
                    commaSeparatedChildren(lPar, receiverTypes, rPar)
                }
                is Node.Modifier.AnnotationSet -> {
                    append("@")
                    if (target != null) {
                        children(target)
                        append(":")
                    }
                    children(lBracket)
                    children(annotations)
                    children(rBracket)
                }
                is Node.Modifier.AnnotationSet.Annotation -> {
                    children(type)
                    commaSeparatedChildren(lPar, arguments, rPar)
                    val parentNode = path.parent?.node
                    if (parentNode is Node.Modifier.AnnotationSet && parentNode.rBracket == null) {
                        nextHeuristicWhitespace = " " // Insert heuristic space after annotation if single form
                    }
                }
                is Node.Modifier.ContextParameter -> {
                    append("context")
                    children(lPar)
                    commaSeparatedChildren(parameters)
                    children(rPar)
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

    private fun NodePath<*>.writeFunctionBody(body: Node.Expression?) {
        if (body != null && body !is Node.Expression.BlockExpression) {
            append("=")
        }
        children(body)
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
            if ((parentNode.accessors.first() === node && (parentNode.delegateExpression != null || parentNode.initializerExpression != null)) ||
                (parentNode.accessors.size == 2 && parentNode.accessors.last() === node && parentNode.accessors[0].body != null && parentNode.accessors[0].body !is Node.Expression.BlockExpression)
            ) {
                if (!containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                    append("\n")
                }
            }
        }
        if (parentNode is Node.Expression.WhenExpression && node is Node.Expression.WhenExpression.WhenBranch) {
            if (parentNode.branches.first() !== node && !containsNewlineOrSemicolon(extrasSinceLastNonSymbol)) {
                append("\n")
            }
        }
        if (parentNode is Node.Expression.AnnotatedExpression && ((node is Node.Expression.BinaryExpression && node.operator !is Node.Keyword.Dot) || node is Node.Expression.BinaryTypeExpression)) {
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
        if (node is Node.Expression.CallExpression && node.lambdaArgument == null && next is Node.Expression.LambdaExpression) {
            if (!containsSemicolon(extrasSinceLastNonSymbol)) {
                append(";")
            }
        }
        if (node is Node.Declaration.PropertyDeclaration && next is Node.Declaration.ScriptInitializer) {
            // if it's method that looks like a getter or setter and comes after a property declaration, we need to properly separate them
            // see kotlin/compiler/testData/psi/script/topLevelPropertiesWithGetSet.kts
            if (!containsSemicolon(extrasSinceLastNonSymbol)) {
                append(";")
            }
        }
        if (node is Node.Declaration.ClassOrObject.ClassBody.EnumEntry && next != null) {
            if (!containsTrailingComma(extrasSinceLastNonSymbol)) {
                append(",")
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

    protected fun containsTrailingComma(extras: List<Node.Extra>): Boolean {
        return extras.any {
            it is Node.Extra.TrailingComma
        }
    }

    protected open fun NodePath<*>.writeExtrasBefore() {
        writeExtras(node.supplement.extrasBefore)
    }

    protected open fun NodePath<*>.writeExtrasWithin() {
        writeExtras(node.supplement.extrasWithin)
    }

    protected open fun NodePath<*>.writeExtrasAfter() {
        writeExtras(node.supplement.extrasAfter)
    }

    protected open fun NodePath<*>.writeExtras(extras: List<Node.Extra>) {
        if (!withExtras) return
        extras.forEach {
            append(it.text)
        }
        extrasSinceLastNonSymbol.addAll(extras)
    }
}
