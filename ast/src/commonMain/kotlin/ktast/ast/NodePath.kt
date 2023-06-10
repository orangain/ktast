package ktast.ast

/**
 * A path to a node in the AST.
 *
 * @property node the node at this path
 * @property parent the parent path, or null if this is the root path
 */
data class NodePath<T : Node>(
    val node: T,
    val parent: NodePath<*>?,
) {
    companion object {
        /**
         * Returns the root path for the given node.
         *
         * @param node the node
         * @return the root path
         */
        fun <T : Node> rootPathOf(node: T): NodePath<T> = NodePath(node, null)
    }

    /**
     * Returns the ancestor nodes of this path, starting with the parent and ending with the root.
     */
    fun ancestors(): Sequence<Node> = generateSequence(parent) { it.parent }.map { it.node }

    /**
     * Returns the child path for the given child node.
     *
     * @param child the child node
     * @return the child path
     */
    fun <C : Node> childPathOf(child: C): NodePath<C> = NodePath(child, this)
}