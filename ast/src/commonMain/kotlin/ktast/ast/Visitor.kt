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
            is Node.HasSimpleStringRepresentation -> {}
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
            is Node.ClassDeclaration.ClassParent.CallConstructor -> {
                visitChildren(type)
                visitChildren(typeArgs)
                visitChildren(args)
                visitChildren(lambda)
            }
            is Node.ClassDeclaration.ClassParent.DelegatedType -> {
                visitChildren(type)
                visitChildren(byKeyword)
                visitChildren(expression)
            }
            is Node.ClassDeclaration.ClassParent.Type -> {
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
            is Node.InitDeclaration -> {
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
            is Node.SecondaryConstructorDeclaration -> {
                visitChildren(modifiers)
                visitChildren(constructorKeyword)
                visitChildren(params)
                visitChildren(delegationCall)
                visitChildren(block)
            }
            is Node.SecondaryConstructorDeclaration.DelegationCall -> {
                visitChildren(target)
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
            is Node.FunctionType.Param -> {
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
            is Node.IfExpression -> {
                visitChildren(ifKeyword)
                visitChildren(condition)
                visitChildren(body)
                visitChildren(elseBody)
            }
            is Node.TryExpression -> {
                visitChildren(block)
                visitChildren(catchClauses)
                visitChildren(finallyBlock)
            }
            is Node.TryExpression.CatchClause -> {
                visitChildren(catchKeyword)
                visitChildren(params)
                visitChildren(block)
            }
            is Node.ForExpression -> {
                visitChildren(forKeyword)
                visitChildren(loopParam)
                visitChildren(loopRange)
                visitChildren(body)
            }
            is Node.WhileExpression -> {
                visitChildren(whileKeyword)
                visitChildren(condition)
                visitChildren(body)
            }
            is Node.BinaryExpression -> {
                visitChildren(lhs)
                visitChildren(operator)
                visitChildren(rhs)
            }
            is Node.BinaryInfixExpression -> {
                visitChildren(lhs)
                visitChildren(operator)
                visitChildren(rhs)
            }
            is Node.UnaryExpression -> {
                visitChildren(expression)
                visitChildren(operator)
            }
            is Node.BinaryTypeExpression -> {
                visitChildren(lhs)
                visitChildren(operator)
                visitChildren(rhs)
            }
            is Node.CallableReferenceExpression -> {
                visitChildren(lhs)
                visitChildren(rhs)
            }
            is Node.ClassLiteralExpression -> {
                visitChildren(lhs)
            }
            is Node.DoubleColonExpression.Receiver.Expression -> {
                visitChildren(expression)
            }
            is Node.DoubleColonExpression.Receiver.Type -> {
                visitChildren(type)
                visitChildren(questionMarks)
            }
            is Node.ParenthesizedExpression -> {
                visitChildren(expression)
            }
            is Node.StringLiteralExpression -> {
                visitChildren(entries)
            }
            is Node.StringLiteralExpression.LiteralStringEntry -> {}
            is Node.StringLiteralExpression.ShortTemplateEntry -> {}
            is Node.StringLiteralExpression.UnicodeEscapeEntry -> {}
            is Node.StringLiteralExpression.RegularEscapeEntry -> {}
            is Node.StringLiteralExpression.LongTemplateEntry -> {
                visitChildren(expression)
            }
            is Node.ConstantLiteralExpression -> {}
            is Node.LambdaExpression -> {
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
            is Node.LambdaParam.Variable -> {
                visitChildren(modifiers)
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.LambdaExpression.LambdaBody -> {
                visitChildren(statements)
            }
            is Node.ThisExpression -> {}
            is Node.SuperExpression -> {
                visitChildren(typeArg)
            }
            is Node.WhenExpression -> {
                visitChildren(whenKeyword)
                visitChildren(lPar)
                visitChildren(expression)
                visitChildren(rPar)
                visitChildren(whenBranches)
            }
            is Node.WhenExpression.WhenBranch.Conditional -> {
                visitChildren(whenConditions)
                visitChildren(trailingComma)
                visitChildren(body)
            }
            is Node.WhenExpression.WhenBranch.Else -> {
                visitChildren(elseKeyword)
                visitChildren(body)
            }
            is Node.WhenExpression.WhenCondition.Expression -> {
                visitChildren(expression)
            }
            is Node.WhenExpression.WhenCondition.In -> {
                visitChildren(expression)
            }
            is Node.WhenExpression.WhenCondition.Is -> {
                visitChildren(typeRef)
            }
            is Node.ObjectLiteralExpression -> {
                visitChildren(declaration)
            }
            is Node.ThrowExpression -> {
                visitChildren(expression)
            }
            is Node.ReturnExpression -> {
                visitChildren(expression)
            }
            is Node.ContinueExpression -> {}
            is Node.BreakExpression -> {}
            is Node.CollectionLiteralExpression -> {
                visitChildren(expressions)
                visitChildren(trailingComma)
            }
            is Node.NameExpression -> {}
            is Node.LabeledExpression -> {
                visitChildren(expression)
            }
            is Node.AnnotatedExpression -> {
                visitChildren(annotationSets)
                visitChildren(expression)
            }
            is Node.CallExpression -> {
                visitChildren(expression)
                visitChildren(typeArgs)
                visitChildren(args)
                visitChildren(lambdaArg)
            }
            is Node.CallExpression.LambdaArg -> {
                visitChildren(annotationSets)
                visitChildren(expression)
            }
            is Node.ArrayAccessExpression -> {
                visitChildren(expression)
                visitChildren(indices)
                visitChildren(trailingComma)
            }
            is Node.AnonymousFunctionExpression -> {
                visitChildren(function)
            }
            is Node.PropertyExpression -> {
                visitChildren(declaration)
            }
            is Node.BlockExpression -> {
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
            is Node.Extra -> {}
            else -> error("Expected to be unreachable here. Missing visitor implementation for $this.")
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