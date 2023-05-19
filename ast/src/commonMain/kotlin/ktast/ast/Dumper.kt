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

    private val levelMap = mutableMapOf<Int, Int>()

    private fun levelOf(v: Node?): Int {
        return if (v == null) -1 else levelMap[System.identityHashCode(v)] ?: error("$v is not found in levelMap")
    }

    private fun setLevel(v: Node, parent: Node?) {
        levelMap[System.identityHashCode(v)] = levelOf(parent) + 1
    }

    override fun visit(v: Node, parent: Node?) {
        setLevel(v, parent)

        v.writeExtrasBefore()
        v.printNode()
        super.visit(v, parent)
        v.writeExtrasWithin()
        v.writeExtrasAfter()
    }

    private fun Node.writeExtrasBefore() {
        if (extrasMap == null) return
        writeExtras(extrasMap.extrasBefore(this), levelOf(this), ExtraType.BEFORE)
    }

    private fun Node.writeExtrasWithin() {
        if (extrasMap == null) return
        writeExtras(extrasMap.extrasWithin(this), levelOf(this) + 1, ExtraType.WITHIN)
    }

    private fun Node.writeExtrasAfter() {
        if (extrasMap == null) return
        writeExtras(extrasMap.extrasAfter(this), levelOf(this), ExtraType.AFTER)
    }

    enum class ExtraType {
        BEFORE, WITHIN, AFTER
    }

    private fun writeExtras(extras: List<Node.Extra>, level: Int, extraType: ExtraType) {
        extras.forEach {
            it.printNode(level, "$extraType: ")
        }
    }

    private fun Node.printNode(level: Int = levelOf(this), prefix: String = "") {
        app.append("  ".repeat(level))
        app.append(prefix)
        app.append(this::class.qualifiedName?.substring(10)) // 10 means length of "ktast.ast."
        if (verbose) {
            when (this) {
                is Node.HasSimpleStringRepresentation -> mapOf("str" to string)
                is Node.SecondaryConstructorDeclaration.DelegationCall -> mapOf("target" to target)
                is Node.UnaryExpression -> mapOf("prefix" to prefix)
                is Node.AnnotationSetModifier -> mapOf("target" to target)
                is Node.NameExpression -> mapOf("name" to name)
                is Node.ConstantLiteralExpression -> mapOf("value" to value, "form" to form)
                is Node.Comment -> mapOf("text" to text)
                is Node.StringLiteralExpression.LiteralStringEntry -> mapOf("str" to str)
                is Node.StringLiteralExpression.ShortTemplateEntry -> mapOf("str" to str)
                is Node.StringLiteralExpression.UnicodeEscapeEntry -> mapOf("digits" to digits)
                is Node.StringLiteralExpression.RegularEscapeEntry -> mapOf("char" to char.toEscapedString())
                else -> null
            }?.let {
                app.append(it.toString())
            }
        }
        app.appendLine()
    }
}

private fun Char.toEscapedString(): String {
    return when (this) {
        '\b' -> "\\b"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\t' -> "\\t"
        else -> this.toString()
    }
}