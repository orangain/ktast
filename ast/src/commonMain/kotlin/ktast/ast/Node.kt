package ktast.ast

sealed class Node {
    var tag: Any? = null

    data class NodeList<out T : Node>(
        val children: List<T>,
        val separator: String = "",
        val prefix: String = "",
        val suffix: String = "",
        val trailingSeparator: Keyword? = null,
    ) : Node()

    interface WithAnnotations {
        val anns: List<Modifier.AnnotationSet>
    }

    interface WithModifiers : WithAnnotations {
        val mods: NodeList<Modifier>?
        override val anns: List<Modifier.AnnotationSet>
            get() = mods?.children.orEmpty().mapNotNull { it as? Modifier.AnnotationSet }
    }

    interface Entry : WithAnnotations {
        val pkg: Package?
        val imports: NodeList<Import>?
    }

    interface WithOptionalParentheses {
        val lPar: Keyword.LPar?
        val rPar: Keyword.RPar?
    }

    interface WithPostModifiers {
        val postMods: List<PostModifier>
    }

    data class File(
        override val anns: List<Modifier.AnnotationSet>,
        override val pkg: Package?,
        override val imports: NodeList<Import>?,
        val decls: List<Decl>
    ) : Node(), Entry

    data class Script(
        override val anns: List<Modifier.AnnotationSet>,
        override val pkg: Package?,
        override val imports: NodeList<Import>?,
        val exprs: List<Expr>
    ) : Node(), Entry

    /**
     * AST node corresponds to KtPackageDirective.
     */
    data class Package(
        override val mods: NodeList<Modifier>?,
        val packageNameExpr: Expr,
    ) : Node(), WithModifiers

    /**
     * AST node corresponds to KtImportDirective.
     */
    data class Import(
        val importKeyword: Keyword.Import,
        val names: List<Expr.Name>,
        val alias: Alias?
    ) : Node() {

        /**
         * AST node corresponds to KtImportAlias.
         */
        data class Alias(
            val name: Expr.Name,
        ) : Node()
    }

    sealed class Decl : Node() {
        /**
         * AST node corresponds to KtClassOrObject.
         */
        data class Structured(
            override val mods: NodeList<Modifier>?,
            val declarationKeyword: Keyword.Declaration,
            val name: Expr.Name?,
            val typeParams: TypeParams?,
            val primaryConstructor: PrimaryConstructor?,
            val colon: Keyword.Colon?,
            val parentAnns: List<Modifier.AnnotationSet>,
            val parents: List<Parent>,
            val typeConstraints: PostModifier.TypeConstraints?,
            // TODO: Can include primary constructor
            val body: NodeList<Decl>?,
        ) : Decl(), WithModifiers {

            val isClass = declarationKeyword.token == Keyword.DeclarationToken.CLASS
            val isObject = declarationKeyword.token == Keyword.DeclarationToken.OBJECT
            val isInterface = declarationKeyword.token == Keyword.DeclarationToken.INTERFACE
            val isCompanion = mods?.children.orEmpty().contains(Modifier.Lit(Modifier.Keyword.COMPANION))
            val isEnum = mods?.children.orEmpty().contains(Modifier.Lit(Modifier.Keyword.ENUM))

            sealed class Parent : Node() {
                data class CallConstructor(
                    val type: Node.Type.Simple,
                    val typeArgs: NodeList<TypeProjection>?,
                    val args: ValueArgs?,
                    val lambda: Expr.Call.LambdaArg?
                ) : Parent()

                data class DelegatedType(
                    val type: Node.Type.Simple,
                    val byKeyword: Keyword.By,
                    val expr: Expr
                ) : Parent()

                data class Type(
                    val type: Node.Type.Simple,
                ) : Parent()
            }

            data class PrimaryConstructor(
                override val mods: NodeList<Modifier>?,
                val constructorKeyword: Keyword.Constructor?,
                val params: Func.Params?
            ) : Node(), WithModifiers

            /**
             * AST node corresponds to KtClassBody.
             */
            data class Body(
                val decls: List<Decl>
            ) : Node()
        }

        /**
         * AST node corresponds to KtAnonymousInitializer.
         */
        data class Init(
            override val mods: NodeList<Modifier>?,
            val block: Expr.Block,
        ) : Decl(), WithModifiers

        /**
         * AST node corresponds to KtNamedFunction.
         */
        data class Func(
            override val mods: NodeList<Modifier>?,
            val funKeyword: Keyword.Fun,
            val typeParams: TypeParams?,
            val receiverTypeRef: TypeRef?,
            // Name not present on anonymous functions
            val name: Expr.Name?,
            // According to the Kotlin syntax, type parameters are not allowed here. However, Kotlin compiler can parse them.
            val postTypeParams: TypeParams?,
            val params: Params?,
            val typeRef: TypeRef?,
            override val postMods: List<PostModifier>,
            val body: Body?
        ) : Decl(), WithModifiers, WithPostModifiers {
            /**
             * AST node corresponds to KtParameterList under KtNamedFunction.
             */
            data class Params(
                val params: List<Param>,
                val trailingComma: Keyword.Comma?,
            ) : Node() {
                /**
                 * AST node corresponds to KtParameter.
                 */
                data class Param(
                    override val mods: NodeList<Modifier>?,
                    val valOrVar: Keyword.ValOrVar?,
                    val name: Expr.Name,
                    // Type can be null for anon functions
                    val typeRef: TypeRef?,
                    val initializer: Initializer?
                ) : Node(), WithModifiers
            }

            /**
             * Virtual AST node corresponds to function body.
             */
            sealed class Body : Node() {
                data class Block(val block: Node.Expr.Block) : Func.Body()
                data class Expr(
                    val equals: Keyword.Equal,
                    val expr: Node.Expr,
                ) : Func.Body()
            }
        }

        /**
         * AST node corresponds to KtProperty or KtDestructuringDeclaration.
         */
        data class Property(
            override val mods: NodeList<Modifier>?,
            val valOrVar: Keyword.ValOrVar,
            val typeParams: TypeParams?,
            val receiverTypeRef: TypeRef?,
            // Always at least one, more than one is destructuring
            val variable: Variable,
            val typeConstraints: PostModifier.TypeConstraints?,
            val initializer: Initializer?,
            val delegate: Delegate?,
            val accessors: Accessors?
        ) : Decl(), WithModifiers {
            /**
             * Virtual AST node corresponds a part of KtProperty,
             * virtual AST node corresponds to a list of KtDestructuringDeclarationEntry or
             * AST node corresponds to KtDestructuringDeclarationEntry.
             */
            sealed class Variable : Node() {
                /**
                 * Virtual AST node corresponds a part of KtProperty or AST node corresponds to KtDestructuringDeclarationEntry.
                 */
                data class Single(
                    val name: Expr.Name,
                    val typeRef: TypeRef?
                ) : Variable()

                /**
                 * Virtual AST node corresponds to a list of KtDestructuringDeclarationEntry.
                 */
                data class Multi(
                    val vars: List<Single>,
                    val trailingComma: Keyword.Comma?,
                ) : Variable()
            }

            data class Delegate(
                val byKeyword: Keyword.By,
                val expr: Expr,
            ) : Node()

            data class Accessors(
                val first: Accessor,
                val second: Accessor?
            ) : Node()

            /**
             * AST node corresponds to KtPropertyAccessor.
             */
            sealed class Accessor : Node(), WithModifiers, WithPostModifiers {
                data class Get(
                    override val mods: NodeList<Modifier>?,
                    val getKeyword: Keyword.Get,
                    val typeRef: TypeRef?,
                    override val postMods: List<PostModifier>,
                    val body: Func.Body?
                ) : Accessor()

                data class Set(
                    override val mods: NodeList<Modifier>?,
                    val setKeyword: Keyword.Set,
                    val params: Params?,
                    override val postMods: List<PostModifier>,
                    val body: Func.Body?
                ) : Accessor()

                /**
                 * AST node corresponds to KtParameterList under KtPropertyAccessor.
                 */
                data class Params(
                    val params: List<Func.Params.Param>,
                    val trailingComma: Keyword.Comma?,
                ) : Node()
            }
        }

        data class TypeAlias(
            override val mods: NodeList<Modifier>?,
            val name: Expr.Name,
            val typeParams: TypeParams?,
            val typeRef: TypeRef
        ) : Decl(), WithModifiers

        data class Constructor(
            override val mods: NodeList<Modifier>?,
            val constructorKeyword: Keyword.Constructor,
            val params: Func.Params?,
            val delegationCall: DelegationCall?,
            val block: Expr.Block?
        ) : Decl(), WithModifiers {
            data class DelegationCall(
                val target: DelegationTarget,
                val args: ValueArgs?
            ) : Node()

            enum class DelegationTarget { THIS, SUPER }
        }

        /**
         * AST node corresponds to KtEnumEntry.
         */
        data class EnumEntry(
            override val mods: NodeList<Modifier>?,
            val name: Expr.Name,
            val args: ValueArgs?,
            val body: Structured.Body?,
            /**
             * Whether this enum entry has comma or not. All entries excluding the last one should have value true.
             * The last entry can have both true or false.
             */
            val hasComma: Boolean,
        ) : Decl(), WithModifiers
    }

    data class Initializer(
        val equals: Keyword.Equal,
        val expr: Expr,
    ) : Node()

    /**
     * AST node corresponds to KtTypeParameterList.
     */
    data class TypeParams(
        val params: List<TypeParam>,
        val trailingComma: Keyword.Comma?,
    ) : Node() {
        /**
         * AST node corresponds to KtTypeParameter.
         */
        data class TypeParam(
            override val mods: NodeList<Modifier>?,
            val name: Expr.Name,
            val typeRef: TypeRef?
        ) : Node(), WithModifiers
    }

    sealed class Type : Node() {
        /**
         * AST node corresponds to KtFunctionType.
         */
        data class Func(
            val receiver: Receiver?,
            val params: NodeList<Param>?,
            val typeRef: TypeRef
        ) : Type() {
            /**
             * AST node corresponds KtFunctionTypeReceiver.
             */
            data class Receiver(
                val typeRef: TypeRef,
            ) : Node()

            /**
             * AST node corresponds to KtParameter.
             */
            data class Param(
                val name: Expr.Name?,
                val typeRef: TypeRef
            ) : Node()
        }

        /**
         * AST node corresponds to KtUserType.
         */
        data class Simple(
            val pieces: List<Piece>
        ) : Type() {
            data class Piece(
                val name: Expr.Name,
                val typeParams: NodeList<TypeProjection>?
            ) : Node()
        }

        /**
         * AST node corresponds to KtNullableType.
         */
        data class Nullable(
            override val lPar: Keyword.LPar?,
            override val mods: NodeList<Modifier>?,
            val type: Type,
            override val rPar: Keyword.RPar?,
        ) : Type(), WithModifiers, WithOptionalParentheses

        /**
         * AST node corresponds to KtDynamicType.
         */
        data class Dynamic(val _unused_: Boolean = false) : Type()
    }

    /**
     * AST node corresponds to KtTypeProjection.
     */
    sealed class TypeProjection : Node() {
        data class Asterisk(
            val asterisk: Keyword.Asterisk,
        ) : TypeProjection()

        data class Type(
            override val mods: NodeList<Modifier>?,
            val typeRef: TypeRef,
        ) : TypeProjection(), WithModifiers
    }

    /**
     * AST node corresponds to KtTypeReference.
     */
    data class TypeRef(
        override val lPar: Keyword.LPar?,
        val contextReceivers: NodeList<ContextReceiver>?,
        override val mods: NodeList<Modifier>?,
        val innerLPar: Keyword.LPar?,
        val innerMods: NodeList<Modifier>?,
        val type: Type?,
        val innerRPar: Keyword.RPar?,
        override val rPar: Keyword.RPar?,
    ) : Node(), WithModifiers, WithOptionalParentheses

    data class ContextReceiver(
        val typeRef: TypeRef,
    ) : Node()

    /**
     * AST node corresponds to KtConstructorCalleeExpression.
     */
    data class ConstructorCallee(
        val type: Type.Simple,
    ) : Node()

    /**
     * AST node corresponds to KtValueArgumentList or KtInitializerList.
     */
    data class ValueArgs(
        val args: List<ValueArg>,
        val trailingComma: Keyword.Comma?,
    ) : Node() {
        /**
         * AST node corresponds to KtValueArgument.
         */
        data class ValueArg(
            val name: Expr.Name?,
            val asterisk: Boolean,
            val expr: Expr
        ) : Node()
    }

    /**
     * AST node corresponds to PsiElement having type of BODY.
     */
    data class Body(
        val expr: Expr,
    ) : Node()

    /**
     * AST node corresponds to KtContainerNode.
     */
    data class Container(
        val expr: Expr,
    ) : Node()

    sealed class Expr : Node() {
        /**
         * AST node corresponds to KtIfExpression.
         */
        data class If(
            val lPar: Keyword.LPar,
            val expr: Expr,
            val rPar: Keyword.RPar,
            val body: Expr,
            val elseKeyword: Keyword.Else?,
            val elseBody: Expr?
        ) : Expr()

        /**
         * AST node corresponds to KtTryExpression.
         */
        data class Try(
            val block: Block,
            val catches: List<Catch>,
            val finallyBlock: Block?
        ) : Expr() {
            /**
             * AST node corresponds to KtCatchClause.
             */
            data class Catch(
                val catchKeyword: Keyword.Catch,
                val params: Decl.Func.Params,
                val block: Block
            ) : Node()
        }

        /**
         * AST node corresponds to KtForExpression.
         */
        data class For(
            val forKeyword: Keyword.For,
            override val anns: List<Modifier.AnnotationSet>,
            val loopParam: Lambda.Param,
            val loopRange: LoopRange,
            val body: Body,
        ) : Expr(), WithAnnotations {
            /**
             * AST node corresponds to KtContainerNode under KtForExpression.
             */
            data class LoopRange(
                val expr: Expr,
            ) : Node()
        }

        /**
         * AST node corresponds to KtWhileExpressionBase.
         */
        data class While(
            val whileKeyword: Keyword.While,
            val condition: Container,
            val body: Body,
            val doWhile: Boolean
        ) : Expr()

        data class BinaryOp(
            val lhs: Expr,
            val oper: Oper,
            val rhs: Expr
        ) : Expr() {
            sealed class Oper : Node() {
                data class Infix(val str: String) : Oper()
                data class Token(val token: BinaryOp.Token) : Oper()
            }

            enum class Token(val str: String) {
                MUL("*"), DIV("/"), MOD("%"), ADD("+"), SUB("-"),
                IN("in"), NOT_IN("!in"),
                GT(">"), GTE(">="), LT("<"), LTE("<="),
                EQ("=="), NEQ("!="),
                ASSN("="), MUL_ASSN("*="), DIV_ASSN("/="), MOD_ASSN("%="), ADD_ASSN("+="), SUB_ASSN("-="),
                OR("||"), AND("&&"), ELVIS("?:"), RANGE(".."),
                DOT("."), DOT_SAFE("?."), SAFE("?")
            }
        }

        data class UnaryOp(
            val expr: Expr,
            val oper: Oper,
            val prefix: Boolean
        ) : Expr() {
            data class Oper(val token: Token) : Node()
            enum class Token(val str: String) {
                NEG("-"), POS("+"), INC("++"), DEC("--"), NOT("!"), NULL_DEREF("!!")
            }
        }

        data class TypeOp(
            val lhs: Expr,
            val oper: Oper,
            val rhs: TypeRef
        ) : Expr() {
            data class Oper(val token: Token) : Node()
            enum class Token(val str: String) {
                AS("as"), AS_SAFE("as?"), COL(":"), IS("is"), NOT_IS("!is")
            }
        }

        sealed class DoubleColonRef : Expr() {
            abstract val recv: Recv?

            /**
             * AST node corresponds to KtCallableReferenceExpression.
             */
            data class Callable(
                override val recv: Recv?,
                val name: Name
            ) : DoubleColonRef()

            /**
             * AST node corresponds to KtClassLiteralExpression.
             */
            data class Class(
                override val recv: Recv?
            ) : DoubleColonRef()

            sealed class Recv : Node() {
                data class Expr(val expr: Node.Expr) : Recv()
                data class Type(
                    val type: Node.Type.Simple,
                    val questionMarks: List<Keyword.Question>,
                ) : Recv()
            }
        }

        data class Paren(
            val expr: Expr
        ) : Expr()

        data class StringTmpl(
            val elems: List<Elem>,
            val raw: Boolean
        ) : Expr() {
            sealed class Elem : Node() {
                data class Regular(val str: String) : Elem()
                data class ShortTmpl(val str: String) : Elem()
                data class UnicodeEsc(val digits: String) : Elem()
                data class RegularEsc(val char: Char) : Elem()
                data class LongTmpl(val expr: Expr) : Elem()
            }
        }

        data class Const(
            val value: String,
            val form: Form
        ) : Expr() {
            enum class Form { BOOLEAN, CHAR, INT, FLOAT, NULL }
        }

        /**
         * AST node corresponds to KtLambdaExpression.
         */
        data class Lambda(
            val params: NodeList<Param>?,
            val body: Body?
        ) : Expr() {
            /**
             * AST node corresponds to KtParameter or KtDestructuringDeclarationEntry in lambda arguments or for statement.
             */
            sealed class Param : Node() {
                /**
                 * AST node corresponds to KtParameter whose child is IDENTIFIER or KtDestructuringDeclarationEntry.
                 */
                data class Single(
                    val name: Name,
                    val typeRef: TypeRef?
                ) : Param()

                /**
                 * AST node corresponds to KtParameter whose child is KtDestructuringDeclaration.
                 */
                data class Multi(
                    val vars: NodeList<Single>,
                    val destructTypeRef: TypeRef?
                ) : Param()
            }

            /**
             * AST node corresponds to KtBlockExpression in lambda body.
             * In lambda expression, left and right braces are not included in Lambda.Body, but are included in Lambda.
             * This means:
             *
             * <Lambda> = { <Param>, <Param> -> <Body> }
             */
            data class Body(val stmts: List<Stmt>) : Expr()
        }

        data class This(
            val label: String?
        ) : Expr()

        data class Super(
            val typeArg: TypeRef?,
            val label: String?
        ) : Expr()

        /**
         * AST node corresponds to KtWhenExpression.
         */
        data class When(
            val lPar: Keyword.LPar,
            val expr: Expr?,
            val rPar: Keyword.RPar,
            val entries: List<Entry>
        ) : Expr() {
            /**
             * AST node corresponds to KtWhenEntry.
             */
            sealed class Entry : Node() {
                data class Conds(
                    val conds: List<Cond>,
                    val trailingComma: Keyword.Comma?,
                    val body: Expr,
                ) : When.Entry()

                data class Else(
                    val elseKeyword: Keyword.Else,
                    val body: Expr,
                ) : When.Entry()
            }

            /**
             * AST node corresponds to KtWhenCondition.
             */
            sealed class Cond : Node() {
                /**
                 * AST node corresponds to KtWhenConditionWithExpression.
                 */
                data class Expr(val expr: Node.Expr) : Cond()

                /**
                 * AST node corresponds to KtWhenConditionInRange.
                 */
                data class In(
                    val expr: Node.Expr,
                    val not: Boolean
                ) : Cond()

                /**
                 * AST node corresponds to KtWhenConditionIsPattern.
                 */
                data class Is(
                    val typeRef: TypeRef,
                    val not: Boolean
                ) : Cond()
            }
        }

        /**
         * AST node corresponds to KtObjectLiteralExpression.
         */
        data class Object(
            val decl: Decl.Structured,
        ) : Expr()

        data class Throw(
            val expr: Expr
        ) : Expr()

        data class Return(
            val returnKeyword: Keyword.Return,
            val label: String?,
            val expr: Expr?
        ) : Expr()

        data class Continue(
            val label: String?
        ) : Expr()

        data class Break(
            val label: String?
        ) : Expr()

        /**
         * AST node corresponds to KtCollectionLiteralExpression.
         */
        data class CollLit(
            val exprs: List<Expr>,
            val trailingComma: Keyword.Comma?,
        ) : Expr()

        data class Name(
            val name: String
        ) : Expr()

        data class Labeled(
            val label: String,
            val expr: Expr
        ) : Expr()

        data class Annotated(
            override val anns: List<Modifier.AnnotationSet>,
            val expr: Expr
        ) : Expr(), WithAnnotations

        /**
         * AST node corresponds to KtCallExpression.
         */
        data class Call(
            val expr: Expr,
            val typeArgs: NodeList<TypeProjection>?,
            val args: ValueArgs?,
            // According to the Kotlin syntax, at most one lambda argument is allowed. However, Kotlin compiler can parse multiple lambda arguments.
            val lambdaArgs: List<LambdaArg>
        ) : Expr() {
            /**
             * AST node corresponds to KtLambdaArgument.
             */
            data class LambdaArg(
                override val anns: List<Modifier.AnnotationSet>,
                val label: String?,
                val func: Lambda
            ) : Node(), WithAnnotations
        }

        /**
         * AST node corresponds to KtArrayAccessExpression.
         */
        data class ArrayAccess(
            val expr: Expr,
            val indices: List<Expr>,
            val trailingComma: Keyword.Comma?,
        ) : Expr()

        data class AnonFunc(
            val func: Decl.Func
        ) : Expr()

        /**
         * AST node corresponds to KtProperty or KtDestructuringDeclaration.
         * This is only present for when expressions and labeled expressions.
         */
        data class Property(
            val decl: Decl.Property
        ) : Expr()

        /**
         * AST node corresponds to KtBlockExpression.
         */
        data class Block(val stmts: List<Stmt>) : Expr()
    }

    sealed class Stmt : Node() {
        data class Decl(val decl: Node.Decl) : Stmt()
        data class Expr(val expr: Node.Expr) : Stmt()
    }

    sealed class Modifier : Node() {
        /**
         * AST node corresponds to KtAnnotation or single KtAnnotationEntry.
         */
        data class AnnotationSet(
            val atSymbol: Node.Keyword.At?,
            val target: Target?,
            val lBracket: Node.Keyword.LBracket?,
            val anns: List<Annotation>,
            val rBracket: Node.Keyword.RBracket?,
        ) : Modifier() {
            enum class Target {
                FIELD, FILE, PROPERTY, GET, SET, RECEIVER, PARAM, SETPARAM, DELEGATE
            }

            /**
             * AST node corresponds to KtAnnotationEntry.
             */
            data class Annotation(
                val constructorCallee: ConstructorCallee,
                val args: ValueArgs?
            ) : Node()
        }

        data class Lit(val keyword: Keyword) : Modifier()
        enum class Keyword {
            ABSTRACT, FINAL, OPEN, ANNOTATION, SEALED, DATA, OVERRIDE, LATEINIT, INNER, ENUM, COMPANION,
            PRIVATE, PROTECTED, PUBLIC, INTERNAL,
            IN, OUT, NOINLINE, CROSSINLINE, VARARG, REIFIED,
            TAILREC, OPERATOR, INFIX, INLINE, EXTERNAL, SUSPEND, CONST, FUN,
            ACTUAL, EXPECT
        }
    }

    sealed class PostModifier : Node() {
        /**
         * Virtual AST node corresponds to a pair of "where" keyword and KtTypeConstraintList.
         */
        data class TypeConstraints(
            val whereKeyword: Keyword.Where,
            val constraints: NodeList<TypeConstraint>,
        ) : PostModifier() {
            /**
             * AST node corresponds to KtTypeConstraint.
             */
            data class TypeConstraint(
                override val anns: List<Modifier.AnnotationSet>,
                val name: Expr.Name,
                val typeRef: TypeRef
            ) : Node(), WithAnnotations
        }

        /**
         * Virtual AST node corresponds to a pair of "contract" keyword and KtContractEffectList.
         */
        data class Contract(
            val contractKeyword: Keyword.Contract,
            val contractEffects: NodeList<ContractEffect>,
        ) : PostModifier() {
            /**
             * AST node corresponds to KtContractEffect.
             */
            data class ContractEffect(
                val expr: Expr,
            ) : Node()
        }
    }

    sealed class Keyword(val value: String) : Node() {
        override fun toString(): String {
            return javaClass.simpleName
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Keyword

            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }

        data class ValOrVar(val token: ValOrVarToken) : Keyword(token.name.lowercase()) {
            companion object {
                private val valOrVarValues = ValOrVarToken.values().associateBy { it.name.lowercase() }

                fun of(value: String) = valOrVarValues[value]?.let { ValOrVar(it) }
                    ?: error("Unknown value: $value")
            }
        }

        enum class ValOrVarToken {
            VAL, VAR,
        }

        data class Declaration(val token: DeclarationToken) : Keyword(token.name.lowercase()) {
            companion object {
                private val declarationValues = DeclarationToken.values().associateBy { it.name.lowercase() }

                fun of(value: String) = declarationValues[value]?.let { Declaration(it) }
                    ?: error("Unknown value: $value")
            }
        }

        enum class DeclarationToken {
            INTERFACE, CLASS, OBJECT,
        }

        class Import : Keyword("import")
        class Fun : Keyword("fun")
        class Constructor : Keyword("constructor")
        class Return : Keyword("return")
        class For : Keyword("for")
        class While : Keyword("while")
        class Else : Keyword("else")
        class Catch : Keyword("catch")
        class By : Keyword("by")
        class Contract : Keyword("contract")
        class Where : Keyword("where")
        class Get : Keyword("get")
        class Set : Keyword("set")
        class Equal : Keyword("=")
        class Colon : Keyword(":")
        class Comma : Keyword(",")
        class Question : Keyword("?")
        class Asterisk : Keyword("*")
        class LPar : Keyword("(")
        class RPar : Keyword(")")
        class LBracket : Keyword("[")
        class RBracket : Keyword("]")
        class At : Keyword("@")
    }

    sealed class Extra : Node() {
        abstract val text: String

        data class Whitespace(
            override val text: String,
        ) : Extra()

        data class Comment(
            override val text: String,
        ) : Extra()

        data class Semicolon(
            override val text: String
        ) : Extra()
    }
}