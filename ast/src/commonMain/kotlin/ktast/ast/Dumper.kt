package ktast.ast

class Dumper(
    private val app: Appendable = StringBuilder(),
    private val extrasMap: ExtrasMap? = null,
    private val verbose: Boolean = true,
) : Visitor() {

    companion object {
        fun dump(v: Node, extrasMap: ExtrasMap? = null, verbose: Boolean = true): String {
            val builder = StringBuilder()
            Dumper(builder, extrasMap, verbose).dump(v)
            return builder.toString()
        }
    }

    fun dump(v: Node) {
        visit(v)
    }

    private fun levelOf(path: NodePath): Int {
        return path.ancestors().count()
    }

    override fun visit(path: NodePath) {
        path.writeExtrasBefore()
        path.writeNode()
        super.visit(path)
        path.writeExtrasWithin()
        path.writeExtrasAfter()
    }

    private fun NodePath.writeExtrasBefore() {
        if (extrasMap == null || parent == null) return
        val extraPaths = extrasMap.extrasBefore(node).map { parent.childPath(it) }
        writeExtras(extraPaths, ExtraType.BEFORE)
    }

    private fun NodePath.writeExtrasWithin() {
        if (extrasMap == null) return
        val extraPaths = extrasMap.extrasWithin(node).map { childPath(it) }
        writeExtras(extraPaths, ExtraType.WITHIN)
    }

    private fun NodePath.writeExtrasAfter() {
        if (extrasMap == null || parent == null) return
        val extraPaths = extrasMap.extrasAfter(node).map { parent.childPath(it) }
        writeExtras(extraPaths, ExtraType.AFTER)
    }

    enum class ExtraType {
        BEFORE, WITHIN, AFTER
    }

    private fun writeExtras(extraPaths: List<NodePath>, extraType: ExtraType) {
        extraPaths.forEach { path ->
            path.writeNode("$extraType: ")
        }
    }

    private fun NodePath.writeNode(prefix: String = "") {
        val level = levelOf(this)
        app.append("  ".repeat(level))
        app.append(prefix)
        app.append(node::class.qualifiedName?.substring(10)) // 10 means length of "ktast.ast."
        if (verbose) {
            node.apply {
                when (this) {
                    is Node.Modifier.AnnotationSet -> mapOf("target" to target)
                    is Node.Expression.StringLiteralExpression -> mapOf("raw" to raw)
                    is Node.Expression.StringLiteralExpression.TemplateStringEntry -> mapOf("short" to short)
                    is Node.SimpleTextNode -> mapOf("text" to text)
                    else -> null
                }?.let { m ->
                    app.append("{" + m.map { "${it.key}=\"${toEscapedString(it.value.toString())}\"" }
                        .joinToString(", ") + "}")
                }
            }
        }
        app.appendLine()
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