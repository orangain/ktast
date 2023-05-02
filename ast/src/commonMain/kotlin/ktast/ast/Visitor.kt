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
                visitChildren(alias)
            }
            is Node.ImportDirective.Alias -> {
                visitChildren(name)
            }
            is Node.ClassDeclaration -> {
                visitChildren(modifiers)
                visitChildren(declarationKeyword)
                visitChildren(name)
                visitChildren(typeParams)
                visitChildren(primaryConstructor)
                visitChildren(parents)
                visitChildren(typeConstraints)
                visitChildren(body)
            }
            is Node.ClassDeclaration.Parent.CallConstructor -> {
                visitChildren(type)
                visitChildren(typeArgs)
                visitChildren(args)
                visitChildren(lambda)
            }
            is Node.ClassDeclaration.Parent.DelegatedType -> {
                visitChildren(type)
                visitChildren(byKeyword)
                visitChildren(expression)
            }
            is Node.ClassDeclaration.Parent.Type -> {
                visitChildren(type)
            }
            is Node.ClassDeclaration.PrimaryConstructor -> {
                visitChildren(modifiers)
                visitChildren(constructorKeyword)
                visitChildren(params)
            }
            is Node.ClassDeclaration.Body -> {
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
            is Node.FunctionDeclaration.Param -> {
                visitChildren(modifiers)
                visitChildren(valOrVar)
                visitChildren(name)
                visitChildren(typeRef)
                visitChildren(equals)
                visitChildren(defaultValue)
            }
            is Node.PropertyDeclaration -> {
                visitChildren(modifiers)
                visitChildren(valOrVar)
                visitChildren(typeParams)
                visitChildren(receiverTypeRef)
                visitChildren(lPar)
                visitChildren(variables)
                visitChildren(trailingComma)
                visitChildren(rPar)
                visitChildren(typeConstraints)
                visitChildren(equals)
                visitChildren(initializer)
                visitChildren(delegate)
                visitChildren(accessors)
            }
            is Node.PropertyDeclaration.Delegate -> {
                visitChildren(byKeyword)
                visitChildren(expression)
            }
            is Node.PropertyDeclaration.Variable -> {
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.PropertyDeclaration.Accessor.Getter -> {
                visitChildren(modifiers)
                visitChildren(getKeyword)
                visitChildren(typeRef)
                visitChildren(postModifiers)
                visitChildren(equals)
                visitChildren(body)
            }
            is Node.PropertyDeclaration.Accessor.Setter -> {
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
            is Node.EnumEntry -> {
                visitChildren(modifiers)
                visitChildren(name)
                visitChildren(args)
                visitChildren(body)
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
                visitChildren(receiver)
                visitChildren(params)
                visitChildren(returnTypeRef)
                visitChildren(rPar)
            }
            is Node.FunctionType.ContextReceiver -> {
                visitChildren(typeRef)
            }
            is Node.FunctionType.Receiver -> {
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
                visitChildren(catches)
                visitChildren(finallyBlock)
            }
            is Node.TryExpression.Catch -> {
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
            is Node.StringTemplateExpression -> {
                visitChildren(entries)
            }
            is Node.StringTemplateExpression.Entry.Regular -> {}
            is Node.StringTemplateExpression.Entry.ShortTemplate -> {}
            is Node.StringTemplateExpression.Entry.UnicodeEscape -> {}
            is Node.StringTemplateExpression.Entry.RegularEscape -> {}
            is Node.StringTemplateExpression.Entry.LongTemplate -> {
                visitChildren(expression)
            }
            is Node.ConstantExpression -> {}
            is Node.LambdaExpression -> {
                visitChildren(params)
                visitChildren(body)
            }
            is Node.LambdaExpression.Param -> {
                visitChildren(lPar)
                visitChildren(variables)
                visitChildren(trailingComma)
                visitChildren(rPar)
                visitChildren(colon)
                visitChildren(destructTypeRef)
            }
            is Node.LambdaExpression.Param.Variable -> {
                visitChildren(modifiers)
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.LambdaExpression.Body -> {
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
                visitChildren(branches)
            }
            is Node.WhenExpression.Branch.Conditional -> {
                visitChildren(conditions)
                visitChildren(trailingComma)
                visitChildren(body)
            }
            is Node.WhenExpression.Branch.Else -> {
                visitChildren(elseKeyword)
                visitChildren(body)
            }
            is Node.WhenExpression.Condition.Expression -> {
                visitChildren(expression)
            }
            is Node.WhenExpression.Condition.In -> {
                visitChildren(expression)
            }
            is Node.WhenExpression.Condition.Is -> {
                visitChildren(typeRef)
            }
            is Node.ObjectExpression -> {
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
            is Node.AnnotationSetModifier -> {
                visitChildren(atSymbol)
                visitChildren(target)
                visitChildren(colon)
                visitChildren(lBracket)
                visitChildren(annotations)
                visitChildren(rBracket)
            }
            is Node.AnnotationSetModifier.Annotation -> {
                visitChildren(type)
                visitChildren(args)
            }
            is Node.PostModifier.TypeConstraints -> {
                visitChildren(whereKeyword)
                visitChildren(constraints)
            }
            is Node.PostModifier.TypeConstraints.TypeConstraint -> {
                visitChildren(annotationSets)
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.PostModifier.Contract -> {
                visitChildren(contractKeyword)
                visitChildren(contractEffects)
            }
            is Node.PostModifier.Contract.ContractEffect -> {
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