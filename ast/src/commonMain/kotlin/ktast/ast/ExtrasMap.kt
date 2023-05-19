package ktast.ast

interface ExtrasMap {
    fun extrasBefore(v: Node): List<Node.Extra>
    fun extrasWithin(v: Node): List<Node.Extra>
    fun extrasAfter(v: Node): List<Node.Extra>

    fun docComment(v: Node): Node.Comment? {
        for (extra in extrasBefore(v)) if (extra is Node.Comment && extra.text.startsWith("/**")) return extra
        return null
    }
}