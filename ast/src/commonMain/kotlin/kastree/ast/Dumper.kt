package kastree.ast

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

    fun dump(v: Node) { visit(v, v) }

    private val levelMap = mutableMapOf<Int, Int>()

    private fun levelOf(v: Node): Int {
        return (levelMap[System.identityHashCode(v)] ?: -1)
    }

    private fun setLevel(v: Node, parent: Node) {
        levelMap[System.identityHashCode(v)] = levelOf(parent) + 1
    }

    override fun visit(v: Node?, parent: Node) {
        if (v == null) {
            return
        }

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
        app.append(this::class.qualifiedName?.substring(12))
        if (verbose) {
            when (this) {
                is Node.Modifier.Lit -> mapOf("keyword" to keyword)
                is Node.Decl.Func -> mapOf("name" to name)
                is Node.Decl.Property.Var -> mapOf("name" to name)
                is Node.Expr.Name -> mapOf("name" to name)
                is Node.Expr.Const -> mapOf("value" to value)
                is Node.Extra.Comment -> mapOf("text" to text)
                else -> null
            }?.let {
                app.append(it.toString())
            }
        }
        app.appendLine()
    }
}
