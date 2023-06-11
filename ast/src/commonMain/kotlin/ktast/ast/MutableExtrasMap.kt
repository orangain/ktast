package ktast.ast

/**
 * Interface for mapping extras to nodes and moving extras between nodes.
 */
interface MutableExtrasMap : ExtrasMap {
    /**
     * Moves extras from one node to another.
     */
    fun moveExtras(fromNode: Node, toNode: Node)
}