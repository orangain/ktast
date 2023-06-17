package ktast.ast

/**
 * Visitor for AST nodes.
 */
open class Visitor {
    companion object {
        /**
         * Traverses the given AST node and its descendants depth-first order and calls the given callback function for each node.
         *
         * @param rootNode root AST node to traverse.
         * @param callback function to be called for each node.
         */
        fun traverse(rootNode: Node, callback: (path: NodePath<*>) -> Unit) = object : Visitor() {
            override fun visit(path: NodePath<*>) {
                callback(path)
                super.visit(path)
            }
        }.traverse(rootNode)
    }

    /**
     * Traverses the given AST node and its descendants depth-first order and calls the protected [visit] method for each node.
     *
     * @param rootNode root AST node to traverse.
     */
    fun traverse(rootNode: Node) = visit(NodePath.rootPathOf(rootNode))

    /**
     * Method to be called for each node.
     *
     * @param path path of the node.
     */
    protected open fun visit(path: NodePath<*>): Unit = path.run {
        node.run {
            when (this) {
                is Node.KotlinFile -> {
                    visitChildren(annotationSets)
                    visitChildren(packageDirective)
                    visitChildren(importDirectives)
                    visitChildren(declarations)
                }
                is Node.KotlinScript -> {
                    visitChildren(annotationSets)
                    visitChildren(packageDirective)
                    visitChildren(importDirectives)
                    visitChildren(expressions)
                }
                is Node.PackageDirective -> {
                    visitChildren(names)
                }
                is Node.ImportDirective -> {
                    visitChildren(names)
                    visitChildren(aliasName)
                }
                is Node.Statement.ForStatement -> {
                    visitChildren(lPar)
                    visitChildren(loopParam)
                    visitChildren(inKeyword)
                    visitChildren(loopRange)
                    visitChildren(rPar)
                    visitChildren(body)
                }
                is Node.Statement.WhileStatement -> {
                    visitChildren(lPar)
                    visitChildren(condition)
                    visitChildren(rPar)
                    visitChildren(body)
                }
                is Node.Statement.DoWhileStatement -> {
                    visitChildren(body)
                    visitChildren(lPar)
                    visitChildren(condition)
                    visitChildren(rPar)
                }
                is Node.Declaration.ClassDeclaration -> {
                    visitChildren(modifiers)
                    visitChildren(classDeclarationKeyword)
                    visitChildren(name)
                    visitChildren(lAngle)
                    visitChildren(typeParams)
                    visitChildren(rAngle)
                    visitChildren(primaryConstructor)
                    visitChildren(classParents)
                    visitChildren(typeConstraintSet)
                    visitChildren(classBody)
                }
                is Node.Declaration.ClassDeclaration.ConstructorClassParent -> {
                    visitChildren(type)
                    visitChildren(lPar)
                    visitChildren(args)
                    visitChildren(rPar)
                }
                is Node.Declaration.ClassDeclaration.DelegationClassParent -> {
                    visitChildren(type)
                    visitChildren(expression)
                }
                is Node.Declaration.ClassDeclaration.TypeClassParent -> {
                    visitChildren(type)
                }
                is Node.Declaration.ClassDeclaration.PrimaryConstructor -> {
                    visitChildren(modifiers)
                    visitChildren(constructorKeyword)
                    visitChildren(lPar)
                    visitChildren(params)
                    visitChildren(rPar)
                }
                is Node.Declaration.ClassDeclaration.ClassBody -> {
                    visitChildren(enumEntries)
                    visitChildren(declarations)
                }
                is Node.Declaration.ClassDeclaration.ClassBody.Initializer -> {
                    visitChildren(block)
                }
                is Node.Declaration.FunctionDeclaration -> {
                    visitChildren(modifiers)
                    visitChildren(lAngle)
                    visitChildren(typeParams)
                    visitChildren(rAngle)
                    visitChildren(receiverType)
                    visitChildren(name)
                    visitChildren(lPar)
                    visitChildren(params)
                    visitChildren(rPar)
                    visitChildren(returnType)
                    visitChildren(postModifiers)
                    visitChildren(equals)
                    visitChildren(body)
                }
                is Node.FunctionParam -> {
                    visitChildren(modifiers)
                    visitChildren(valOrVarKeyword)
                    visitChildren(name)
                    visitChildren(type)
                    visitChildren(equals)
                    visitChildren(defaultValue)
                }
                is Node.Declaration.PropertyDeclaration -> {
                    visitChildren(modifiers)
                    visitChildren(valOrVarKeyword)
                    visitChildren(lAngle)
                    visitChildren(typeParams)
                    visitChildren(rAngle)
                    visitChildren(receiverType)
                    visitChildren(lPar)
                    visitChildren(variables)
                    visitChildren(rPar)
                    visitChildren(typeConstraintSet)
                    visitChildren(equals)
                    visitChildren(initializer)
                    visitChildren(propertyDelegate)
                    visitChildren(accessors)
                }
                is Node.Declaration.PropertyDeclaration.PropertyDelegate -> {
                    visitChildren(expression)
                }
                is Node.Variable -> {
                    visitChildren(annotationSets)
                    visitChildren(name)
                    visitChildren(type)
                }
                is Node.Declaration.PropertyDeclaration.Getter -> {
                    visitChildren(modifiers)
                    visitChildren(lPar)
                    visitChildren(rPar)
                    visitChildren(type)
                    visitChildren(postModifiers)
                    visitChildren(equals)
                    visitChildren(body)
                }
                is Node.Declaration.PropertyDeclaration.Setter -> {
                    visitChildren(modifiers)
                    visitChildren(lPar)
                    visitChildren(params)
                    visitChildren(rPar)
                    visitChildren(postModifiers)
                    visitChildren(equals)
                    visitChildren(body)
                }
                is Node.Declaration.TypeAliasDeclaration -> {
                    visitChildren(modifiers)
                    visitChildren(name)
                    visitChildren(lAngle)
                    visitChildren(typeParams)
                    visitChildren(rAngle)
                    visitChildren(equals)
                    visitChildren(type)
                }
                is Node.Declaration.ClassDeclaration.ClassBody.SecondaryConstructor -> {
                    visitChildren(modifiers)
                    visitChildren(constructorKeyword)
                    visitChildren(lPar)
                    visitChildren(params)
                    visitChildren(rPar)
                    visitChildren(delegationCall)
                    visitChildren(block)
                }
                is Node.Declaration.ClassDeclaration.ClassBody.EnumEntry -> {
                    visitChildren(modifiers)
                    visitChildren(name)
                    visitChildren(lPar)
                    visitChildren(args)
                    visitChildren(rPar)
                    visitChildren(classBody)
                }
                is Node.TypeParam -> {
                    visitChildren(modifiers)
                    visitChildren(name)
                    visitChildren(type)
                }
                is Node.TypeArg -> {
                    visitChildren(modifiers)
                    visitChildren(type)
                }
                is Node.Type.FunctionType -> {
                    visitChildren(modifiers)
                    visitChildren(contextReceiver)
                    visitChildren(receiverType)
                    visitChildren(dotSymbol)
                    visitChildren(lPar)
                    visitChildren(params)
                    visitChildren(rPar)
                    visitChildren(returnType)
                }
                is Node.ContextReceiver -> {
                    visitChildren(lPar)
                    visitChildren(receiverTypes)
                    visitChildren(rPar)
                }
                is Node.Type.FunctionType.FunctionTypeParam -> {
                    visitChildren(name)
                    visitChildren(type)
                }
                is Node.Type.SimpleType -> {
                    visitChildren(modifiers)
                    visitChildren(pieces)
                }
                is Node.Type.SimpleType.SimpleTypePiece -> {
                    visitChildren(name)
                    visitChildren(lAngle)
                    visitChildren(typeArgs)
                    visitChildren(rAngle)
                }
                is Node.Type.NullableType -> {
                    visitChildren(modifiers)
                    visitChildren(innerType)
                    visitChildren(questionMark)
                }
                is Node.Type.ParenthesizedType -> {
                    visitChildren(modifiers)
                    visitChildren(lPar)
                    visitChildren(innerType)
                    visitChildren(rPar)
                }
                is Node.Type.DynamicType -> {
                    visitChildren(modifiers)
                }
                is Node.ValueArg -> {
                    visitChildren(name)
                    visitChildren(asterisk)
                    visitChildren(expression)
                }
                is Node.Expression.IfExpression -> {
                    visitChildren(lPar)
                    visitChildren(condition)
                    visitChildren(rPar)
                    visitChildren(body)
                    visitChildren(elseBody)
                }
                is Node.Expression.TryExpression -> {
                    visitChildren(block)
                    visitChildren(catchClauses)
                    visitChildren(finallyBlock)
                }
                is Node.Expression.TryExpression.CatchClause -> {
                    visitChildren(lPar)
                    visitChildren(params)
                    visitChildren(rPar)
                    visitChildren(block)
                }
                is Node.Expression.BinaryExpression -> {
                    visitChildren(lhs)
                    visitChildren(operator)
                    visitChildren(rhs)
                }
                is Node.Expression.PrefixUnaryExpression -> {
                    visitChildren(operator)
                    visitChildren(expression)
                }
                is Node.Expression.PostfixUnaryExpression -> {
                    visitChildren(expression)
                    visitChildren(operator)
                }
                is Node.Expression.BinaryTypeExpression -> {
                    visitChildren(lhs)
                    visitChildren(operator)
                    visitChildren(rhs)
                }
                is Node.Expression.CallableReferenceExpression -> {
                    visitChildren(lhs)
                    visitChildren(questionMarks)
                    visitChildren(rhs)
                }
                is Node.Expression.ClassLiteralExpression -> {
                    visitChildren(lhs)
                    visitChildren(questionMarks)
                }
                is Node.Expression.ParenthesizedExpression -> {
                    visitChildren(innerExpression)
                }
                is Node.Expression.StringLiteralExpression -> {
                    visitChildren(entries)
                }
                is Node.Expression.StringLiteralExpression.LiteralStringEntry -> {}
                is Node.Expression.StringLiteralExpression.EscapeStringEntry -> {}
                is Node.Expression.StringLiteralExpression.TemplateStringEntry -> {
                    visitChildren(expression)
                }
                is Node.Expression.ConstantLiteralExpression -> {}
                is Node.Expression.LambdaExpression -> {
                    visitChildren(params)
                    visitChildren(arrow)
                    visitChildren(statements)
                }
                is Node.LambdaParam -> {
                    visitChildren(lPar)
                    visitChildren(variables)
                    visitChildren(rPar)
                    visitChildren(colon)
                    visitChildren(destructType)
                }
                is Node.Expression.ThisExpression -> {
                    visitChildren(label)
                }
                is Node.Expression.SuperExpression -> {
                    visitChildren(typeArgType)
                    visitChildren(label)
                }
                is Node.Expression.WhenExpression -> {
                    visitChildren(whenKeyword)
                    visitChildren(subject)
                    visitChildren(whenBranches)
                }
                is Node.Expression.WhenExpression.WhenSubject -> {
                    visitChildren(lPar)
                    visitChildren(annotationSets)
                    visitChildren(valKeyword)
                    visitChildren(variable)
                    visitChildren(expression)
                    visitChildren(rPar)
                }
                is Node.Expression.WhenExpression.ConditionalWhenBranch -> {
                    visitChildren(whenConditions)
                    visitChildren(arrow)
                    visitChildren(body)
                }
                is Node.Expression.WhenExpression.ElseWhenBranch -> {
                    visitChildren(arrow)
                    visitChildren(body)
                }
                is Node.Expression.WhenExpression.ExpressionWhenCondition -> {
                    visitChildren(expression)
                }
                is Node.Expression.WhenExpression.RangeWhenCondition -> {
                    visitChildren(operator)
                    visitChildren(expression)
                }
                is Node.Expression.WhenExpression.TypeWhenCondition -> {
                    visitChildren(operator)
                    visitChildren(type)
                }
                is Node.Expression.ObjectLiteralExpression -> {
                    visitChildren(declaration)
                }
                is Node.Expression.ThrowExpression -> {
                    visitChildren(expression)
                }
                is Node.Expression.ReturnExpression -> {
                    visitChildren(expression)
                    visitChildren(label)
                }
                is Node.Expression.ContinueExpression -> {
                    visitChildren(label)
                }
                is Node.Expression.BreakExpression -> {
                    visitChildren(label)
                }
                is Node.Expression.CollectionLiteralExpression -> {
                    visitChildren(expressions)
                }
                is Node.Expression.NameExpression -> {}
                is Node.Expression.LabeledExpression -> {
                    visitChildren(label)
                    visitChildren(statement)
                }
                is Node.Expression.AnnotatedExpression -> {
                    visitChildren(annotationSets)
                    visitChildren(statement)
                }
                is Node.Expression.CallExpression -> {
                    visitChildren(calleeExpression)
                    visitChildren(lAngle)
                    visitChildren(typeArgs)
                    visitChildren(rAngle)
                    visitChildren(lPar)
                    visitChildren(args)
                    visitChildren(rPar)
                    visitChildren(lambdaArg)
                }
                is Node.Expression.CallExpression.LambdaArg -> {
                    visitChildren(annotationSets)
                    visitChildren(label)
                    visitChildren(expression)
                }
                is Node.Expression.IndexedAccessExpression -> {
                    visitChildren(expression)
                    visitChildren(indices)
                }
                is Node.Expression.AnonymousFunctionExpression -> {
                    visitChildren(function)
                }
                is Node.Expression.BlockExpression -> {
                    visitChildren(statements)
                }
                is Node.Modifier.AnnotationSet -> {
                    visitChildren(atSymbol)
                    visitChildren(target)
                    visitChildren(colon)
                    visitChildren(lBracket)
                    visitChildren(annotations)
                    visitChildren(rBracket)
                }
                is Node.Modifier.AnnotationSet.Annotation -> {
                    visitChildren(type)
                    visitChildren(lPar)
                    visitChildren(args)
                    visitChildren(rPar)
                }
                is Node.PostModifier.TypeConstraintSet -> {
                    visitChildren(constraints)
                }
                is Node.PostModifier.TypeConstraintSet.TypeConstraint -> {
                    visitChildren(annotationSets)
                    visitChildren(name)
                    visitChildren(type)
                }
                is Node.PostModifier.Contract -> {
                    visitChildren(lBracket)
                    visitChildren(contractEffects)
                    visitChildren(rBracket)
                }
                is Node.Keyword -> {}
                is Node.Extra -> {}
            }
        }
        Unit
    }

    protected fun NodePath<*>.visitChildren(child: Node?) {
        if (child != null) {
            visit(childPathOf(child))
        }
    }

    protected fun NodePath<*>.visitChildren(elements: List<Node>) {
        elements.forEach { child -> visit(childPathOf(child)) }
    }
}