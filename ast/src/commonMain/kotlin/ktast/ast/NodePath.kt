package ktast.ast

data class NodePath(
    val node: Node,
    val parent: NodePath?,
) {
    companion object {
        fun rootPath(node: Node): NodePath = NodePath(node, null)
    }

    fun ancestors(): Sequence<Node> = generateSequence(parent) { it.parent }.map { it.node }

    fun childPath(child: Node): NodePath = NodePath(child, this)
}