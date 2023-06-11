package ktast.ast

/**
 * Interface for mapping extras to nodes.
 */
interface ExtrasMap {
    /**
     * Returns extras before the given node.
     */
    fun extrasBefore(node: Node): List<Node.Extra>

    /**
     * Returns extras within the given node.
     */
    fun extrasWithin(node: Node): List<Node.Extra>

    /**
     * Returns extras after the given node.
     */
    fun extrasAfter(node: Node): List<Node.Extra>
}