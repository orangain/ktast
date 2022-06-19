package ktast.ast

interface MutableExtrasMap : ExtrasMap {
    fun moveExtras(fromNode: Node, toNode: Node)
}