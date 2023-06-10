package ktast.ast

data class NodePath(
    val node: Node,
    val parent: NodePath?,
) {
    fun ancestors(): Sequence<NodePath> = generateSequence(parent) { it.parent }

    fun childPath(child: Node): NodePath = NodePath(child, this)
}