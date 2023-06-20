package ktast.ast

/**
 * Supplemental data for a node.
 *
 * @property extrasBefore list of extra nodes before the node.
 * @property extrasWithin list of extra nodes within the node.
 * @property extrasAfter list of extra nodes after the node.
 * @property tag can be used to store per-node state if desired.
 */
data class NodeSupplement(
    val extrasBefore: List<Node.Extra> = listOf(),
    val extrasWithin: List<Node.Extra> = listOf(),
    val extrasAfter: List<Node.Extra> = listOf(),
    var tag: Any? = null
)