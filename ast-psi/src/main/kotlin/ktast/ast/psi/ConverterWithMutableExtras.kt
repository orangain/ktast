package ktast.ast.psi

import ktast.ast.MutableExtrasMap
import ktast.ast.Node

/**
 * Converts PSI elements to AST nodes and keeps track of extras. It also supports moving extras from one node to another.
 */
class ConverterWithMutableExtras : ConverterWithExtras(), MutableExtrasMap {
    override fun moveExtras(fromNode: Node, toNode: Node) {
        // Compare two nodes by identity using triple equals.
        if (fromNode === toNode) {
            return // do nothing
        }

        extrasBefore[toNode] = extrasBefore[fromNode]
        extrasWithin[toNode] = extrasWithin[fromNode]
        extrasAfter[toNode] = extrasAfter[fromNode]

        extrasBefore.remove(fromNode)
        extrasWithin.remove(fromNode)
        extrasAfter.remove(fromNode)
    }
}