package ktast.ast

internal fun toType(e: Node.Expression): Node.Type? {
    when (e) {
        is Node.Expression.NameExpression -> {
            return Node.Type.SimpleType(
                modifiers = listOf(),
                qualifiers = listOf(),
                name = e,
                lAngle = null,
                typeArgs = listOf(),
                rAngle = null,
            )
        }
        is Node.Expression.CallExpression -> {
            if (e.args.isEmpty() && e.lPar == null && e.rPar == null && e.lambdaArg == null) {
                return Node.Type.SimpleType(
                    modifiers = listOf(),
                    qualifiers = listOf(),
                    name = e.calleeExpression as Node.Expression.NameExpression,
                    lAngle = e.lAngle,
                    typeArgs = e.typeArgs,
                    rAngle = e.rAngle,
                )
            }
        }
        is Node.Expression.BinaryExpression -> {
            if (e.operator is Node.Keyword.Dot) {
                val lhs = toType(e.lhs)
                val rhs = toType(e.rhs)
                if (lhs is Node.Type.SimpleType && rhs is Node.Type.SimpleType) {
                    return Node.Type.SimpleType(
                        modifiers = listOf(),
                        qualifiers = lhs.qualifiers + Node.Type.SimpleType.SimpleTypeQualifier(
                            name = lhs.name,
                            lAngle = lhs.lAngle,
                            typeArgs = lhs.typeArgs,
                            rAngle = lhs.rAngle,
                        ),
                        name = rhs.name,
                        lAngle = rhs.lAngle,
                        typeArgs = rhs.typeArgs,
                        rAngle = rhs.rAngle,
                    )
                }
            }
        }
        else -> {}
    }
    return null
}

internal fun wrapWithNullableType(type: Node.Type, questionMarks: List<Node.Keyword.Question>): Node.Type {
    if (questionMarks.isEmpty()) {
        return type
    }
    return Node.Type.NullableType(
        modifiers = listOf(),
        innerType = wrapWithNullableType(type, questionMarks.dropLast(1)),
        questionMark = questionMarks.last(),
    )
}

internal fun getLambdaExpression(node: Node.Statement): Node.Expression.LambdaExpression? {
    return when (node) {
        is Node.Expression.LambdaExpression -> node
        is Node.Expression.AnnotatedExpression -> getLambdaExpression(node.statement)
        is Node.Expression.LabeledExpression -> getLambdaExpression(node.statement)
        else -> null
    }
}