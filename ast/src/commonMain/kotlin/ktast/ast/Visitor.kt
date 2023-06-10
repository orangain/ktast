package ktast.ast

open class Visitor {
    fun visit(node: Node) = visit(NodePath.rootPath(node))

    protected open fun visit(path: NodePath): Unit = path.run {
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
                    visitChildren(modifiers)
                    visitChildren(packageKeyword)
                    visitChildren(names)
                }
                is Node.ImportDirective -> {
                    visitChildren(importKeyword)
                    visitChildren(names)
                    visitChildren(importAlias)
                }
                is Node.ImportDirective.ImportAlias -> {
                    visitChildren(name)
                }
                is Node.Statement.ForStatement -> {
                    visitChildren(forKeyword)
                    visitChildren(lPar)
                    visitChildren(loopParam)
                    visitChildren(inKeyword)
                    visitChildren(loopRange)
                    visitChildren(rPar)
                    visitChildren(body)
                }
                is Node.Statement.WhileStatement -> {
                    visitChildren(whileKeyword)
                    visitChildren(lPar)
                    visitChildren(condition)
                    visitChildren(rPar)
                    visitChildren(body)
                }
                is Node.Statement.DoWhileStatement -> {
                    visitChildren(doKeyword)
                    visitChildren(body)
                    visitChildren(whileKeyword)
                    visitChildren(lPar)
                    visitChildren(condition)
                    visitChildren(rPar)
                }
                is Node.Declaration.ClassDeclaration -> {
                    visitChildren(modifiers)
                    visitChildren(classDeclarationKeyword)
                    visitChildren(name)
                    visitChildren(typeParams)
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
                    visitChildren(byKeyword)
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
                    visitChildren(lBrace)
                    visitChildren(enumEntries)
                    visitChildren(declarations)
                    visitChildren(rBrace)
                }
                is Node.Declaration.ClassDeclaration.ClassBody.Initializer -> {
                    visitChildren(modifiers)
                    visitChildren(block)
                }
                is Node.Declaration.FunctionDeclaration -> {
                    visitChildren(modifiers)
                    visitChildren(funKeyword)
                    visitChildren(typeParams)
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
                    visitChildren(typeParams)
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
                    visitChildren(byKeyword)
                    visitChildren(expression)
                }
                is Node.Variable -> {
                    visitChildren(modifiers)
                    visitChildren(name)
                    visitChildren(type)
                }
                is Node.Declaration.PropertyDeclaration.Getter -> {
                    visitChildren(modifiers)
                    visitChildren(getKeyword)
                    visitChildren(type)
                    visitChildren(postModifiers)
                    visitChildren(equals)
                    visitChildren(body)
                }
                is Node.Declaration.PropertyDeclaration.Setter -> {
                    visitChildren(modifiers)
                    visitChildren(setKeyword)
                    visitChildren(params)
                    visitChildren(postModifiers)
                    visitChildren(equals)
                    visitChildren(body)
                }
                is Node.Declaration.TypeAliasDeclaration -> {
                    visitChildren(modifiers)
                    visitChildren(name)
                    visitChildren(typeParams)
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
                    visitChildren(qualifiers)
                    visitChildren(name)
                    visitChildren(lAngle)
                    visitChildren(typeArgs)
                    visitChildren(rAngle)
                }
                is Node.Type.SimpleType.SimpleTypeQualifier -> {
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
                    visitChildren(dynamicKeyword)
                }
                is Node.ValueArg -> {
                    visitChildren(name)
                    visitChildren(asterisk)
                    visitChildren(expression)
                }
                is Node.Expression.IfExpression -> {
                    visitChildren(ifKeyword)
                    visitChildren(lPar)
                    visitChildren(condition)
                    visitChildren(rPar)
                    visitChildren(body)
                    visitChildren(elseKeyword)
                    visitChildren(elseBody)
                }
                is Node.Expression.TryExpression -> {
                    visitChildren(block)
                    visitChildren(catchClauses)
                    visitChildren(finallyBlock)
                }
                is Node.Expression.TryExpression.CatchClause -> {
                    visitChildren(catchKeyword)
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
                    visitChildren(lBrace)
                    visitChildren(params)
                    visitChildren(arrow)
                    visitChildren(lambdaBody)
                    visitChildren(rBrace)
                }
                is Node.LambdaParam -> {
                    visitChildren(lPar)
                    visitChildren(variables)
                    visitChildren(rPar)
                    visitChildren(colon)
                    visitChildren(destructType)
                }
                is Node.Expression.LambdaExpression.LambdaBody -> {
                    visitChildren(statements)
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
                    visitChildren(lPar)
                    visitChildren(expression)
                    visitChildren(rPar)
                    visitChildren(whenBranches)
                }
                is Node.Expression.WhenExpression.ConditionalWhenBranch -> {
                    visitChildren(whenConditions)
                    visitChildren(body)
                }
                is Node.Expression.WhenExpression.ElseWhenBranch -> {
                    visitChildren(elseKeyword)
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
                is Node.Expression.PropertyExpression -> {
                    visitChildren(property)
                }
                is Node.Expression.BlockExpression -> {
                    visitChildren(lBrace)
                    visitChildren(statements)
                    visitChildren(rBrace)
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
                    visitChildren(whereKeyword)
                    visitChildren(constraints)
                }
                is Node.PostModifier.TypeConstraintSet.TypeConstraint -> {
                    visitChildren(annotationSets)
                    visitChildren(name)
                    visitChildren(type)
                }
                is Node.PostModifier.Contract -> {
                    visitChildren(contractKeyword)
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

    protected fun NodePath.visitChildren(child: Node?) {
        if (child != null) {
            visit(childPath(child))
        }
    }

    protected fun NodePath.visitChildren(elements: List<Node>) {
        elements.forEach { child -> visit(childPath(child)) }
    }

    companion object {
        fun visit(v: Node, fn: (path: NodePath) -> Unit) = object : Visitor() {
            override fun visit(path: NodePath) {
                fn(path)
                super.visit(path)
            }
        }.visit(v)
    }
}