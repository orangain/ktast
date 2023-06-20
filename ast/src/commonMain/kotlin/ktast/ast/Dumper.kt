package ktast.ast

/**
 * Utility class for dumping AST.
 *
 * Example usage:
 * ```
 * Dumper.dump(node)
 * ```
 *
 * Sample output for the code `val x = ""`:
 * ```
 * Node.KotlinFile
 *   Node.Declaration.PropertyDeclaration
 *     Node.Keyword.Val{text="val"}
 *     Node.Variable
 *       Node.Expression.NameExpression{text="x"}
 *     Node.Expression.StringLiteralExpression{raw=false}
 * ```
 */
class Dumper(
    private val appendable: Appendable = StringBuilder(),
    private val verbose: Boolean = true,
) : Visitor() {

    companion object {
        /**
         * Dumps the given AST node.
         *
         * @param node root AST node to dump.
         * @param verbose whether to dump extra node attributes, defaults to true.
         */
        fun dump(node: Node, verbose: Boolean = true): String {
            val builder = StringBuilder()
            Dumper(builder, verbose).dump(node)
            return builder.toString()
        }
    }

    /**
     * Dumps the given AST node.
     *
     * @param node root AST node to dump.
     */
    fun dump(node: Node) {
        traverse(node)
    }

    private fun levelOf(path: NodePath<*>): Int {
        return path.ancestors().count()
    }

    override fun visit(path: NodePath<*>) {
        path.writeExtrasBefore()
        path.writeNode()
        super.visit(path)
        path.writeExtrasWithin()
        path.writeExtrasAfter()
    }

    private fun NodePath<*>.writeExtrasBefore() {
        if (parent == null) return
        val extraPaths = node.supplement.extrasBefore.map { parent.childPathOf(it) }
        writeExtras(extraPaths, ExtraType.BEFORE)
    }

    private fun NodePath<*>.writeExtrasWithin() {
        val extraPaths = node.supplement.extrasWithin.map { childPathOf(it) }
        writeExtras(extraPaths, ExtraType.WITHIN)
    }

    private fun NodePath<*>.writeExtrasAfter() {
        if (parent == null) return
        val extraPaths = node.supplement.extrasAfter.map { parent.childPathOf(it) }
        writeExtras(extraPaths, ExtraType.AFTER)
    }

    private enum class ExtraType {
        BEFORE, WITHIN, AFTER
    }

    private fun writeExtras(extraPaths: List<NodePath<*>>, extraType: ExtraType) {
        extraPaths.forEach { path ->
            path.writeNode("$extraType: ")
        }
    }

    private fun NodePath<*>.writeNode(prefix: String = "") {
        val level = levelOf(this)
        appendable.append("  ".repeat(level))
        appendable.append(prefix)
        appendable.append(node::class.qualifiedName?.substring(10)) // 10 means length of "ktast.ast."
        if (verbose) {
            node.apply {
                when (this) {
                    is Node.Expression.StringLiteralExpression -> mapOf("raw" to raw)
                    is Node.Expression.StringLiteralExpression.TemplateStringEntry -> mapOf("short" to short)
                    is Node.SimpleTextNode -> mapOf("text" to text)
                    else -> null
                }?.let { m ->
                    appendable.append("{" + m.map { "${it.key}=${valueOf(it.value)}" }
                        .joinToString(", ") + "}")
                }
            }
        }
        appendable.appendLine()
    }
}

private fun valueOf(v: Any): String {
    return when (v) {
        is String -> "\"${toEscapedString(v)}\""
        else -> v.toString()
    }
}

private fun toEscapedString(s: String): String {
    return s.map { it.toEscapedString() }.joinToString("")
}

private fun Char.toEscapedString(): String {
    return when (this) {
        '\b' -> "\\b"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\t' -> "\\t"
        '"' -> "\\\""
        else -> this.toString()
    }
}