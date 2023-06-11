package ktast.ast

/**
 * Interface for mapping extras to nodes.
 */
interface ExtrasMap {
    /**
     * Returns extras before the given node.
     */
    fun extrasBefore(v: Node): List<Node.Extra>

    /**
     * Returns extras within the given node.
     */
    fun extrasWithin(v: Node): List<Node.Extra>

    /**
     * Returns extras after the given node.
     */
    fun extrasAfter(v: Node): List<Node.Extra>
}