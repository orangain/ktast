package ktast.ast

open class Visitor {
    fun visit(v: Node) = visit(v, null)

    protected open fun visit(v: Node, parent: Node?) = v.run {
        when (this) {
            is Node.CommaSeparatedNodeList<*> -> {
                visitChildren(elements)
                visitChildren(trailingComma)
            }
            is Node.NodeList<*> -> {
                visitChildren(elements)
            }
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
            is Node.ClassDeclaration -> {
                visitChildren(modifiers)
                visitChildren(classDeclarationKeyword)
                visitChildren(name)
                visitChildren(typeParams)
                visitChildren(primaryConstructor)
                visitChildren(classParents)
                visitChildren(typeConstraintSet)
                visitChildren(classBody)
            }
            is Node.ClassDeclaration.ConstructorClassParent -> {
                visitChildren(type)
                visitChildren(args)
            }
            is Node.ClassDeclaration.DelegationClassParent -> {
                visitChildren(type)
                visitChildren(byKeyword)
                visitChildren(expression)
            }
            is Node.ClassDeclaration.TypeClassParent -> {
                visitChildren(type)
            }
            is Node.ClassDeclaration.PrimaryConstructor -> {
                visitChildren(modifiers)
                visitChildren(constructorKeyword)
                visitChildren(params)
            }
            is Node.ClassDeclaration.ClassBody -> {
                visitChildren(enumEntries)
                visitChildren(declarations)
            }
            is Node.ClassDeclaration.ClassBody.Initializer -> {
                visitChildren(modifiers)
                visitChildren(block)
            }
            is Node.FunctionDeclaration -> {
                visitChildren(modifiers)
                visitChildren(funKeyword)
                visitChildren(typeParams)
                visitChildren(receiverTypeRef)
                visitChildren(name)
                visitChildren(params)
                visitChildren(typeRef)
                visitChildren(postModifiers)
                visitChildren(equals)
                visitChildren(body)
            }
            is Node.FunctionParam -> {
                visitChildren(modifiers)
                visitChildren(valOrVarKeyword)
                visitChildren(name)
                visitChildren(typeRef)
                visitChildren(equals)
                visitChildren(defaultValue)
            }
            is Node.PropertyDeclaration -> {
                visitChildren(modifiers)
                visitChildren(valOrVarKeyword)
                visitChildren(typeParams)
                visitChildren(receiverTypeRef)
                visitChildren(lPar)
                visitChildren(variables)
                visitChildren(trailingComma)
                visitChildren(rPar)
                visitChildren(typeConstraintSet)
                visitChildren(equals)
                visitChildren(initializer)
                visitChildren(propertyDelegate)
                visitChildren(accessors)
            }
            is Node.PropertyDeclaration.PropertyDelegate -> {
                visitChildren(byKeyword)
                visitChildren(expression)
            }
            is Node.Variable -> {
                visitChildren(modifiers)
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.PropertyDeclaration.Getter -> {
                visitChildren(modifiers)
                visitChildren(getKeyword)
                visitChildren(typeRef)
                visitChildren(postModifiers)
                visitChildren(equals)
                visitChildren(body)
            }
            is Node.PropertyDeclaration.Setter -> {
                visitChildren(modifiers)
                visitChildren(setKeyword)
                visitChildren(params)
                visitChildren(postModifiers)
                visitChildren(equals)
                visitChildren(body)
            }
            is Node.TypeAliasDeclaration -> {
                visitChildren(modifiers)
                visitChildren(name)
                visitChildren(typeParams)
                visitChildren(typeRef)
            }
            is Node.ClassDeclaration.ClassBody.SecondaryConstructor -> {
                visitChildren(modifiers)
                visitChildren(constructorKeyword)
                visitChildren(params)
                visitChildren(constructorDelegationCall)
                visitChildren(block)
            }
            is Node.ClassDeclaration.ClassBody.SecondaryConstructor.ConstructorDelegationCall -> {
                visitChildren(targetKeyword)
                visitChildren(args)
            }
            is Node.ClassDeclaration.ClassBody.EnumEntry -> {
                visitChildren(modifiers)
                visitChildren(name)
                visitChildren(args)
                visitChildren(classBody)
            }
            is Node.TypeParam -> {
                visitChildren(modifiers)
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.TypeArg -> {
                visitChildren(modifiers)
                visitChildren(typeRef)
            }
            is Node.TypeRef -> {
                visitChildren(lPar)
                visitChildren(modifiers)
                visitChildren(type)
                visitChildren(rPar)
            }
            is Node.FunctionType -> {
                visitChildren(lPar)
                visitChildren(modifiers)
                visitChildren(contextReceivers)
                visitChildren(functionTypeReceiver)
                visitChildren(params)
                visitChildren(returnTypeRef)
                visitChildren(rPar)
            }
            is Node.FunctionType.ContextReceiver -> {
                visitChildren(typeRef)
            }
            is Node.FunctionType.FunctionTypeReceiver -> {
                visitChildren(typeRef)
            }
            is Node.FunctionType.FunctionTypeParam -> {
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.SimpleType -> {
                visitChildren(qualifiers)
                visitChildren(name)
                visitChildren(typeArgs)
            }
            is Node.SimpleType.Qualifier -> {
                visitChildren(name)
                visitChildren(typeArgs)
            }
            is Node.NullableType -> {
                visitChildren(lPar)
                visitChildren(modifiers)
                visitChildren(type)
                visitChildren(rPar)
            }
            is Node.DynamicType -> {}
            is Node.ValueArg -> {
                visitChildren(name)
                visitChildren(expression)
            }
            is Node.ExpressionContainer -> {
                visitChildren(expression)
            }
            is Node.Expression.IfExpression -> {
                visitChildren(ifKeyword)
                visitChildren(condition)
                visitChildren(body)
                visitChildren(elseBody)
            }
            is Node.Expression.TryExpression -> {
                visitChildren(block)
                visitChildren(catchClauses)
                visitChildren(finallyBlock)
            }
            is Node.Expression.TryExpression.CatchClause -> {
                visitChildren(catchKeyword)
                visitChildren(params)
                visitChildren(block)
            }
            is Node.Expression.ForExpression -> {
                visitChildren(forKeyword)
                visitChildren(loopParam)
                visitChildren(loopRange)
                visitChildren(body)
            }
            is Node.Expression.WhileExpression -> {
                visitChildren(whileKeyword)
                visitChildren(condition)
                visitChildren(body)
            }
            is Node.Expression.BinaryExpression -> {
                visitChildren(lhs)
                visitChildren(operator)
                visitChildren(rhs)
            }
            is Node.Expression.UnaryExpression -> {
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
                visitChildren(expression)
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
                visitChildren(lambdaBody)
            }
            is Node.LambdaParam -> {
                visitChildren(lPar)
                visitChildren(variables)
                visitChildren(trailingComma)
                visitChildren(rPar)
                visitChildren(colon)
                visitChildren(destructTypeRef)
            }
            is Node.Expression.LambdaExpression.LambdaBody -> {
                visitChildren(statements)
            }
            is Node.Expression.ThisExpression -> {}
            is Node.Expression.SuperExpression -> {
                visitChildren(typeArg)
            }
            is Node.Expression.WhenExpression -> {
                visitChildren(whenKeyword)
                visitChildren(lPar)
                visitChildren(expression)
                visitChildren(rPar)
                visitChildren(whenBranches)
            }
            is Node.Expression.WhenExpression.WhenBranch -> {
                visitChildren(whenConditions)
                visitChildren(trailingComma)
                visitChildren(elseKeyword)
                visitChildren(body)
            }
            is Node.Expression.WhenExpression.WhenCondition -> {
                visitChildren(operator)
                visitChildren(expression)
                visitChildren(typeRef)
            }
            is Node.Expression.ObjectLiteralExpression -> {
                visitChildren(declaration)
            }
            is Node.Expression.ThrowExpression -> {
                visitChildren(expression)
            }
            is Node.Expression.ReturnExpression -> {
                visitChildren(expression)
            }
            is Node.Expression.ContinueExpression -> {}
            is Node.Expression.BreakExpression -> {}
            is Node.Expression.CollectionLiteralExpression -> {
                visitChildren(expressions)
                visitChildren(trailingComma)
            }
            is Node.Expression.NameExpression -> {}
            is Node.Expression.LabeledExpression -> {
                visitChildren(expression)
            }
            is Node.Expression.AnnotatedExpression -> {
                visitChildren(annotationSets)
                visitChildren(expression)
            }
            is Node.Expression.CallExpression -> {
                visitChildren(expression)
                visitChildren(typeArgs)
                visitChildren(args)
                visitChildren(lambdaArg)
            }
            is Node.Expression.CallExpression.LambdaArg -> {
                visitChildren(annotationSets)
                visitChildren(expression)
            }
            is Node.Expression.ArrayAccessExpression -> {
                visitChildren(expression)
                visitChildren(indices)
                visitChildren(trailingComma)
            }
            is Node.Expression.AnonymousFunctionExpression -> {
                visitChildren(function)
            }
            is Node.Expression.PropertyExpression -> {
                visitChildren(declaration)
            }
            is Node.Expression.BlockExpression -> {
                visitChildren(statements)
            }
            is Node.AnnotationSet -> {
                visitChildren(atSymbol)
                visitChildren(target)
                visitChildren(colon)
                visitChildren(lBracket)
                visitChildren(annotations)
                visitChildren(rBracket)
            }
            is Node.AnnotationSet.Annotation -> {
                visitChildren(type)
                visitChildren(args)
            }
            is Node.TypeConstraintSet -> {
                visitChildren(whereKeyword)
                visitChildren(constraints)
            }
            is Node.TypeConstraintSet.TypeConstraint -> {
                visitChildren(annotationSets)
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.Contract -> {
                visitChildren(contractKeyword)
                visitChildren(contractEffects)
            }
            is Node.Contract.ContractEffect -> {
                visitChildren(expression)
            }
            is Node.Keyword -> {}
            is Node.Extra -> {}
        }
    }

    protected fun <T : Node> Node.visitChildren(v: T?) {
        if (v != null) {
            visit(v, this)
        }
    }

    protected fun <T : Node> Node.visitChildren(v: List<T>) {
        v.forEach { orig -> visit(orig, this) }
    }

    companion object {
        fun visit(v: Node, fn: (v: Node, parent: Node?) -> Unit) = object : Visitor() {
            override fun visit(v: Node, parent: Node?) {
                fn(v, parent)
                super.visit(v, parent)
            }
        }.visit(v)
    }
}