package ktast.ast

interface ExtrasMap {
    fun extrasBefore(v: Node): List<Node.Extra>
    fun extrasWithin(v: Node): List<Node.Extra>
    fun extrasAfter(v: Node): List<Node.Extra>
}