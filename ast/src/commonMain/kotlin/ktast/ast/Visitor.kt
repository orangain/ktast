package ktast.ast

open class Visitor {
    fun visit(v: Node) = visit(v, v)

    protected open fun visit(v: Node?, parent: Node) = v.run {
        when (this) {
            is Node.NodeList<*> -> {
                visitChildren(children)
                visitChildren(trailingSeparator)
            }
            is Node.File -> {
                visitChildren(anns)
                visitChildren(pkg)
                visitChildren(imports)
                visitChildren(decls)
            }
            is Node.Script -> {
                visitChildren(anns)
                visitChildren(pkg)
                visitChildren(imports)
                visitChildren(exprs)
            }
            is Node.Package -> {
                visitChildren(mods)
                visitChildren(packageKeyword)
                visitChildren(names)
            }
            is Node.Import -> {
                visitChildren(importKeyword)
                visitChildren(names)
                visitChildren(alias)
            }
            is Node.Import.Alias -> {
                visitChildren(name)
            }
            is Node.Decl.Structured -> {
                visitChildren(mods)
                visitChildren(declarationKeyword)
                visitChildren(name)
                visitChildren(typeParams)
                visitChildren(primaryConstructor)
                visitChildren(parents)
                visitChildren(typeConstraints)
                visitChildren(body)
            }
            is Node.Decl.Structured.Parents -> {
                visitChildren(items)
            }
            is Node.Decl.Structured.Parent.CallConstructor -> {
                visitChildren(type)
                visitChildren(typeArgs)
                visitChildren(args)
                visitChildren(lambda)
            }
            is Node.Decl.Structured.Parent.DelegatedType -> {
                visitChildren(type)
                visitChildren(byKeyword)
                visitChildren(expr)
            }
            is Node.Decl.Structured.Parent.Type -> {
                visitChildren(type)
            }
            is Node.Decl.Structured.PrimaryConstructor -> {
                visitChildren(mods)
                visitChildren(constructorKeyword)
                visitChildren(params)
            }
            is Node.Decl.Structured.Body -> {
                visitChildren(decls)
            }
            is Node.Decl.Init -> {
                visitChildren(mods)
                visitChildren(block)
            }
            is Node.Decl.Func -> {
                visitChildren(mods)
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
            is Node.Decl.Func.Params -> {
                visitChildren(params)
                visitChildren(trailingComma)
            }
            is Node.Decl.Func.Params.Param -> {
                visitChildren(mods)
                visitChildren(valOrVar)
                visitChildren(name)
                visitChildren(typeRef)
                visitChildren(initializer)
            }
            is Node.Decl.Func.Body.Block -> {
                visitChildren(block)
            }
            is Node.Decl.Func.Body.Expr -> {
                visitChildren(equals)
                visitChildren(expr)
            }
            is Node.Decl.Property -> {
                visitChildren(mods)
                visitChildren(valOrVar)
                visitChildren(typeParams)
                visitChildren(receiverTypeRef)
                visitChildren(variable)
                visitChildren(typeConstraints)
                visitChildren(initializer)
                visitChildren(delegate)
                visitChildren(accessors)
            }
            is Node.Decl.Property.Delegate -> {
                visitChildren(byKeyword)
                visitChildren(expr)
            }
            is Node.Decl.Property.Variable.Single -> {
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.Decl.Property.Variable.Multi -> {
                visitChildren(vars)
                visitChildren(trailingComma)
            }
            is Node.Decl.Property.Accessor.Get -> {
                visitChildren(mods)
                visitChildren(getKeyword)
                visitChildren(typeRef)
                visitChildren(postMods)
                visitChildren(body)
            }
            is Node.Decl.Property.Accessor.Set -> {
                visitChildren(mods)
                visitChildren(setKeyword)
                visitChildren(params)
                visitChildren(postMods)
                visitChildren(body)
            }
            is Node.Decl.Property.Accessor.Params -> {
                visitChildren(params)
                visitChildren(trailingComma)
            }
            is Node.Decl.TypeAlias -> {
                visitChildren(mods)
                visitChildren(name)
                visitChildren(typeParams)
                visitChildren(typeRef)
            }
            is Node.Decl.SecondaryConstructor -> {
                visitChildren(mods)
                visitChildren(constructorKeyword)
                visitChildren(params)
                visitChildren(delegationCall)
                visitChildren(block)
            }
            is Node.Decl.SecondaryConstructor.DelegationCall -> {
                visitChildren(args)
            }
            is Node.Decl.EnumEntry -> {
                visitChildren(mods)
                visitChildren(name)
                visitChildren(args)
                visitChildren(body)
            }
            is Node.Initializer -> {
                visitChildren(equals)
                visitChildren(expr)
            }
            is Node.TypeParams -> {
                visitChildren(params)
                visitChildren(trailingComma)
            }
            is Node.TypeParam -> {
                visitChildren(mods)
                visitChildren(name)
                visitChildren(typeRef)
            }
            is Node.TypeProjection.Asterisk -> {
                visitChildren(asterisk)
            }
            is Node.TypeProjection.Type -> {
                visitChildren(mods)
                visitChildren(typeRef)
            }
            is Node.TypeRef -> {
                visitChildren(lPar)
                visitChildren(contextReceivers)
                visitChildren(mods)
                visitChildren(innerLPar)
                visitChildren(innerMods)
                visitChildren(type)
                visitChildren(innerRPar)
                visitChildren(rPar)
            }
            is Node.Type.Func -> {
                visitChildren(receiver)
                visitChildren(params)
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
                visitChildren(typeParams)
            }
            is Node.Type.Nullable -> {
                visitChildren(lPar)
                visitChildren(mods)
                visitChildren(type)
                visitChildren(rPar)
            }
            is Node.Type.Dynamic -> {}
            is Node.ContextReceiver -> {
                visitChildren(typeRef)
            }
            is Node.ConstructorCallee -> {
                visitChildren(type)
            }
            is Node.ValueArgs -> {
                visitChildren(args)
                visitChildren(trailingComma)
            }
            is Node.ValueArgs.ValueArg -> {
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
                visitChildren(anns)
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
                visitChildren(stmts)
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
                visitChildren(decl)
            }
            is Node.Expr.Throw -> {
                visitChildren(expr)
            }
            is Node.Expr.Return -> {
                visitChildren(returnKeyword)
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
                visitChildren(anns)
                visitChildren(expr)
            }
            is Node.Expr.Call -> {
                visitChildren(expr)
                visitChildren(typeArgs)
                visitChildren(args)
                visitChildren(lambdaArgs)
            }
            is Node.Expr.Call.LambdaArg -> {
                visitChildren(anns)
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
                visitChildren(decl)
            }
            is Node.Expr.Block -> {
                visitChildren(stmts)
            }
            is Node.Stmt.Decl -> {
                visitChildren(decl)
            }
            is Node.Stmt.Expr -> {
                visitChildren(expr)
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
                visitChildren(anns)
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
            null -> {}
        }
    }

    protected fun <T : Node?> Node?.visitChildren(v: T) {
        visit(v, this!!)
    }

    protected fun <T : Node?> Node?.visitChildren(v: List<T>) {
        v.forEach { orig -> visit(orig, this!!) }
    }

    companion object {
        fun visit(v: Node, fn: (v: Node?, parent: Node) -> Unit) = object : Visitor() {
            override fun visit(v: Node?, parent: Node) {
                fn(v, parent)
                super.visit(v, parent)
            }
        }.visit(v)
    }
}