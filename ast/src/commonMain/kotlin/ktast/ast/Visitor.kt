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
            is Node.File -> {
                visitChildren(annotationSets)
                visitChildren(packageDirective)
                visitChildren(importDirectives)
                visitChildren(declarations)
            }
            is Node.Script -> {
                visitChildren(annotationSets)
                visitChildren(packageDirective)
                visitChildren(importDirectives)
                visitChildren(exprs)
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
            is Node.Declaration.Structured -> {
                visitChildren(modifiers)
                visitChildren(declarationKeyword)
                visitChildren(name)
                visitChildren(typeParams)
                visitChildren(primaryConstructor)
                visitChildren(parents)
                visitChildren(typeConstraints)
                visitChildren(body)
            }
            is Node.Declaration.Structured.Parents -> {
                visitChildren(items)
            }
            is Node.Declaration.Structured.Parent.CallConstructor -> {
                visitChildren(type)
                visitChildren(typeArgs)
                visitChildren(args)
                visitChildren(lambda)
            }
            is Node.Declaration.Structured.Parent.DelegatedType -> {
                visitChildren(type)
                visitChildren(byKeyword)
                visitChildren(expr)
            }
            is Node.Declaration.Structured.Parent.Type -> {
                visitChildren(type)
            }
            is Node.Declaration.Structured.PrimaryConstructor -> {
                visitChildren(modifiers)
                visitChildren(constructorKeyword)
                visitChildren(params)
            }
            is Node.Declaration.Structured.Body -> {
                visitChildren(enumEntries)
                visitChildren(declarations)
            }
            is Node.Declaration.Init -> {
                visitChildren(modifiers)
                visitChildren(block)
            }
            is Node.Declaration.Func -> {
                visitChildren(modifiers)
                visitChildren(funKeyword)
                visitChildren(typeParams)
                visitChildren(receiverTypeRef)
                visitChildren(name)
                visitChildren(postTypeParams)
                visitChildren(params)
                visitChildren(typeRef)
                visitChildren(postMods)
                visitChildren(body)
            }
            is Node.Declaration.Func.Param -> {
                visitChildren(modifiers)
                visitChildren(valOrVar)
                visitChildren(name)
                visitChildren(typeRef)
                visitChildren(initializer)
            }
            is Node.Declaration.Func.Body.Block -> {
                visitChildren(block)
            }
            is Node.Declaration.Func.Body.Expr -> {
                visitChildren(equals)
                visitChildren(expr)
            }
            is Node.Declaration.Property -> {
                visitChildren(modifiers)
                visitChildren(valOrVar)
                visitChildren(typeParams)
                visitChildren(receiverTypeRef)
                visitChildren(variable)
                visitChildren(typeConstraints)
                visitChildren(initializer)
                visitChildren(delegate)
                visitChildren(accessors)
            }
            is Node.Declaration.Property.Delegate -> {
                visitChildren(byKeyword)
                visitChildren(expr)
            }
            is Node.Declaration.Property.Variable.Single -> {
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.Declaration.Property.Variable.Multi -> {
                visitChildren(vars)
                visitChildren(trailingComma)
            }
            is Node.Declaration.Property.Accessor.Get -> {
                visitChildren(modifiers)
                visitChildren(getKeyword)
                visitChildren(typeRef)
                visitChildren(postMods)
                visitChildren(body)
            }
            is Node.Declaration.Property.Accessor.Set -> {
                visitChildren(modifiers)
                visitChildren(setKeyword)
                visitChildren(params)
                visitChildren(postMods)
                visitChildren(body)
            }
            is Node.Declaration.Property.Accessor.Params -> {
                visitChildren(elements)
                visitChildren(trailingComma)
            }
            is Node.Declaration.TypeAlias -> {
                visitChildren(modifiers)
                visitChildren(name)
                visitChildren(typeParams)
                visitChildren(typeRef)
            }
            is Node.Declaration.SecondaryConstructor -> {
                visitChildren(modifiers)
                visitChildren(constructorKeyword)
                visitChildren(params)
                visitChildren(delegationCall)
                visitChildren(block)
            }
            is Node.Declaration.SecondaryConstructor.DelegationCall -> {
                visitChildren(args)
            }
            is Node.EnumEntry -> {
                visitChildren(modifiers)
                visitChildren(name)
                visitChildren(args)
                visitChildren(body)
            }
            is Node.Initializer -> {
                visitChildren(equals)
                visitChildren(expr)
            }
            is Node.TypeParam -> {
                visitChildren(modifiers)
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.TypeArg.Asterisk -> {
                visitChildren(asterisk)
            }
            is Node.TypeArg.Type -> {
                visitChildren(modifiers)
                visitChildren(typeRef)
            }
            is Node.TypeRef -> {
                visitChildren(lPar)
                visitChildren(modifiers)
                visitChildren(innerLPar)
                visitChildren(innerMods)
                visitChildren(type)
                visitChildren(innerRPar)
                visitChildren(rPar)
            }
            is Node.Type.Func -> {
                visitChildren(contextReceivers)
                visitChildren(receiver)
                visitChildren(params)
                visitChildren(typeRef)
            }
            is Node.Type.Func.ContextReceiver -> {
                visitChildren(typeRef)
            }
            is Node.Type.Func.Receiver -> {
                visitChildren(typeRef)
            }
            is Node.Type.Func.Param -> {
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.Type.Simple -> {
                visitChildren(pieces)
            }
            is Node.Type.Simple.Piece -> {
                visitChildren(name)
                visitChildren(typeArgs)
            }
            is Node.Type.Nullable -> {
                visitChildren(lPar)
                visitChildren(modifiers)
                visitChildren(type)
                visitChildren(rPar)
            }
            is Node.Type.Dynamic -> {}
            is Node.ConstructorCallee -> {
                visitChildren(type)
            }
            is Node.ValueArg -> {
                visitChildren(name)
                visitChildren(expr)
            }
            is Node.Container -> {
                visitChildren(expr)
            }
            is Node.Expr.If -> {
                visitChildren(ifKeyword)
                visitChildren(condition)
                visitChildren(body)
                visitChildren(elseBody)
            }
            is Node.Expr.Try -> {
                visitChildren(block)
                visitChildren(catches)
                visitChildren(finallyBlock)
            }
            is Node.Expr.Try.Catch -> {
                visitChildren(catchKeyword)
                visitChildren(params)
                visitChildren(block)
            }
            is Node.Expr.For -> {
                visitChildren(forKeyword)
                visitChildren(annotationSets)
                visitChildren(loopParam)
                visitChildren(loopRange)
                visitChildren(body)
            }
            is Node.Expr.While -> {
                visitChildren(whileKeyword)
                visitChildren(condition)
                visitChildren(body)
            }
            is Node.Expr.BinaryOp -> {
                visitChildren(lhs)
                visitChildren(oper)
                visitChildren(rhs)
            }
            is Node.Expr.BinaryOp.Oper.Infix -> {}
            is Node.Expr.BinaryOp.Oper.Token -> {}
            is Node.Expr.UnaryOp -> {
                visitChildren(expr)
                visitChildren(oper)
            }
            is Node.Expr.UnaryOp.Oper -> {}
            is Node.Expr.TypeOp -> {
                visitChildren(lhs)
                visitChildren(oper)
                visitChildren(rhs)
            }
            is Node.Expr.TypeOp.Oper -> {}
            is Node.Expr.DoubleColonRef.Callable -> {
                visitChildren(recv)
                visitChildren(name)
            }
            is Node.Expr.DoubleColonRef.Class -> {
                visitChildren(recv)
            }
            is Node.Expr.DoubleColonRef.Recv.Expr -> {
                visitChildren(expr)
            }
            is Node.Expr.DoubleColonRef.Recv.Type -> {
                visitChildren(type)
                visitChildren(questionMarks)
            }
            is Node.Expr.Paren -> {
                visitChildren(expr)
            }
            is Node.Expr.StringTmpl -> {
                visitChildren(elems)
            }
            is Node.Expr.StringTmpl.Elem.Regular -> {}
            is Node.Expr.StringTmpl.Elem.ShortTmpl -> {}
            is Node.Expr.StringTmpl.Elem.UnicodeEsc -> {}
            is Node.Expr.StringTmpl.Elem.RegularEsc -> {}
            is Node.Expr.StringTmpl.Elem.LongTmpl -> {
                visitChildren(expr)
            }
            is Node.Expr.Const -> {}
            is Node.Expr.Lambda -> {
                visitChildren(params)
                visitChildren(body)
            }
            is Node.Expr.Lambda.Param.Single -> {
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.Expr.Lambda.Param.Multi -> {
                visitChildren(vars)
                visitChildren(destructTypeRef)
            }
            is Node.Expr.Lambda.Body -> {
                visitChildren(statements)
            }
            is Node.Expr.This -> {}
            is Node.Expr.Super -> {
                visitChildren(typeArg)
            }
            is Node.Expr.When -> {
                visitChildren(lPar)
                visitChildren(expr)
                visitChildren(rPar)
                visitChildren(entries)
            }
            is Node.Expr.When.Entry.Conds -> {
                visitChildren(conds)
                visitChildren(trailingComma)
                visitChildren(body)
            }
            is Node.Expr.When.Entry.Else -> {
                visitChildren(elseKeyword)
                visitChildren(body)
            }
            is Node.Expr.When.Cond.Expr -> {
                visitChildren(expr)
            }
            is Node.Expr.When.Cond.In -> {
                visitChildren(expr)
            }
            is Node.Expr.When.Cond.Is -> {
                visitChildren(typeRef)
            }
            is Node.Expr.Object -> {
                visitChildren(declaration)
            }
            is Node.Expr.Throw -> {
                visitChildren(expr)
            }
            is Node.Expr.Return -> {
                visitChildren(expr)
            }
            is Node.Expr.Continue -> {}
            is Node.Expr.Break -> {}
            is Node.Expr.CollLit -> {
                visitChildren(exprs)
                visitChildren(trailingComma)
            }
            is Node.Expr.Name -> {}
            is Node.Expr.Labeled -> {
                visitChildren(expr)
            }
            is Node.Expr.Annotated -> {
                visitChildren(annotationSets)
                visitChildren(expr)
            }
            is Node.Expr.Call -> {
                visitChildren(expr)
                visitChildren(typeArgs)
                visitChildren(args)
                visitChildren(lambdaArgs)
            }
            is Node.Expr.Call.LambdaArg -> {
                visitChildren(annotationSets)
                visitChildren(func)
            }
            is Node.Expr.ArrayAccess -> {
                visitChildren(expr)
                visitChildren(indices)
                visitChildren(trailingComma)
            }
            is Node.Expr.AnonFunc -> {
                visitChildren(func)
            }
            is Node.Expr.Property -> {
                visitChildren(declaration)
            }
            is Node.Expr.Block -> {
                visitChildren(statements)
            }
            is Node.Modifier.AnnotationSet -> {
                visitChildren(atSymbol)
                visitChildren(lBracket)
                visitChildren(anns)
                visitChildren(rBracket)
            }
            is Node.Modifier.AnnotationSet.Annotation -> {
                visitChildren(constructorCallee)
                visitChildren(args)
            }
            is Node.Modifier.Lit -> {}
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
                visitChildren(expr)
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