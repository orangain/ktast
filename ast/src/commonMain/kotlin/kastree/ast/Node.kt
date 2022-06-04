package kastree.ast

sealed class Node {
    var tag: Any? = null

    data class NodeList<out T: Node>(
        val children: List<T>,
        val separator: String,
        val prefix: String,
        val suffix: String,
    ) : Node()

    interface WithAnnotations {
        val anns: List<Modifier.AnnotationSet>
    }

    interface WithModifiers : WithAnnotations {
        val mods: List<Modifier>
        override val anns: List<Modifier.AnnotationSet> get() = mods.mapNotNull { it as? Modifier.AnnotationSet }
    }

    interface Entry : WithAnnotations {
        val pkg: Package?
        val imports: List<Import>
    }

    interface WithOptionalParentheses {
        val lpar: Keyword.Lpar?
        val rpar: Keyword.Rpar?
    }

    data class File(
        override val anns: List<Modifier.AnnotationSet>,
        override val pkg: Package?,
        override val imports: List<Import>,
        val decls: List<Decl>
    ) : Node(), Entry

    data class Script(
        override val anns: List<Modifier.AnnotationSet>,
        override val pkg: Package?,
        override val imports: List<Import>,
        val exprs: List<Expr>
    ) : Node(), Entry

    data class Package(
        override val mods: List<Modifier>,
        val packageNameExpr: Expr,
    ) : Node(), WithModifiers

    data class Import(
        val importKeyword: Keyword.Import,
        val names: List<String>,
        val wildcard: Boolean,
        val alias: String?
    ) : Node()

    sealed class Decl : Node() {
        data class Structured(
            override val mods: List<Modifier>,
            val declarationKeyword: Keyword.Declaration,
            val name: Expr.Name?,
            val typeParams: TypeParams?,
            val primaryConstructor: PrimaryConstructor?,
            val colon: Keyword.Colon?,
            val parentAnns: List<Modifier.AnnotationSet>,
            val parents: List<Parent>,
            val typeConstraints: List<TypeConstraint>,
            // TODO: Can include primary constructor
            val body: NodeList<Decl>?,
        ) : Decl(), WithModifiers {
            enum class Form {
                CLASS, ENUM_CLASS, INTERFACE, OBJECT, COMPANION_OBJECT
            }

            val isClass = declarationKeyword.token == Keyword.DeclarationToken.CLASS
            val isObject = declarationKeyword.token == Keyword.DeclarationToken.OBJECT
            val isInterface = declarationKeyword.token == Keyword.DeclarationToken.INTERFACE
            val isCompanion = mods.contains(Node.Modifier.Lit(Modifier.Keyword.COMPANION))
            val isEnum = mods.contains(Node.Modifier.Lit(Modifier.Keyword.ENUM))

            sealed class Parent : Node() {
                data class CallConstructor(
                    val type: Node.Type.Simple,
                    val typeArgs: List<TypeProjection?>,
                    val args: ValueArgs?,
                    val lambda: Expr.Call.TrailLambda?
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
                override val mods: List<Modifier>,
                val constructorKeyword: Keyword.Constructor?,
                val params: Func.Params?
            ) : Node(), WithModifiers
        }
        data class Init(val block: Expr.Block) : Decl()
        data class Func(
            override val mods: List<Modifier>,
            val funKeyword: Keyword.Fun,
            val typeParams: TypeParams?,
            val receiverType: TypeRef?,
            // Name not present on anonymous functions
            val name: Expr.Name?,
            val paramTypeParams: TypeParams?,
            val params: Params?,
            val type: TypeRef?,
            val typeConstraints: List<TypeConstraint>,
            val body: Body?
        ) : Decl(), WithModifiers {
            data class Params(
                val params: List<Param>
            ) : Node() {
                data class Param(
                    override val mods: List<Modifier>,
                    val readOnly: Boolean?,
                    val name: Expr.Name,
                    // Type can be null for anon functions
                    val type: TypeRef?,
                    val initializer: Initializer?
                ) : Node(), WithModifiers
            }
            sealed class Body : Node() {
                data class Block(val block: Node.Expr.Block) : Body()
                data class Expr(val expr: Node.Expr) : Body()
            }
        }
        data class Property(
            override val mods: List<Modifier>,
            val valOrVar: Keyword.ValOrVar,
            val typeParams: TypeParams?,
            val receiverType: TypeRef?,
            // Always at least one, more than one is destructuring, null is underscore in destructure
            val vars: List<Var?>,
            val typeConstraints: List<TypeConstraint>,
            val initializer: Initializer?,
            val delegate: Delegate?,
            val accessors: Accessors?
        ) : Decl(), WithModifiers {
            data class Var(
                val name: Expr.Name,
                val type: TypeRef?
            ) : Node()
            data class Delegate(
                val byKeyword: Keyword.By,
                val expr: Expr,
            ) : Node()
            data class Accessors(
                val first: Accessor,
                val second: Accessor?
            ) : Node()
            sealed class Accessor : Node(), WithModifiers {
                data class Get(
                    override val mods: List<Modifier>,
                    val type: TypeRef?,
                    val body: Func.Body?
                ) : Accessor()
                data class Set(
                    override val mods: List<Modifier>,
                    val paramMods: List<Modifier>,
                    val paramName: Expr.Name?,
                    val paramType: TypeRef?,
                    val body: Func.Body?
                ) : Accessor()
            }
        }
        data class TypeAlias(
            override val mods: List<Modifier>,
            val name: Expr.Name,
            val typeParams: TypeParams?,
            val type: TypeRef
        ) : Decl(), WithModifiers
        data class Constructor(
            override val mods: List<Modifier>,
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
        data class EnumEntry(
            override val mods: List<Modifier>,
            val name: Expr.Name,
            val args: ValueArgs?,
            val members: List<Decl>
        ) : Decl(), WithModifiers
    }

    data class Initializer(
        val equals: Keyword.Equal,
        val expr: Expr,
    ) : Node()

    data class TypeParams(
        val params: List<TypeParam>,
    ) : Node() {
        data class TypeParam(
            override val mods: List<Modifier>,
            val name: Expr.Name,
            val type: Type?
        ) : Node(), WithModifiers
    }

    data class TypeConstraint(
        override val anns: List<Modifier.AnnotationSet>,
        val name: Expr.Name,
        val type: TypeRef
    ) : Node(), WithAnnotations

    sealed class Type : Node() {
        data class Func(
            val receiverType: TypeRef?,
            val params: NodeList<Param>?,
            val type: TypeRef
        ) : Type() {
            data class Param(
                val name: Expr.Name?,
                val type: TypeRef
            ) : Node()
        }
        data class Simple(
            val pieces: List<Piece>
        ) : Type() {
            data class Piece(
                val name: Expr.Name,
                // Null means any
                val typeParams: List<TypeProjection?>
            ) : Node()
        }
        data class Nullable(
            override val lpar: Keyword.Lpar?,
            override val mods: List<Modifier>,
            val type: Type,
            override val rpar: Keyword.Rpar?,
        ) : Type(), WithModifiers, WithOptionalParentheses
        data class Dynamic(val _unused_: Boolean = false) : Type()
    }

    data class TypeProjection(
        override val mods: List<Modifier>,
        val typeRef: TypeRef,
    ) : Node(), WithModifiers

    data class TypeRef(
        override val mods: List<Modifier>,
        override val lpar: Keyword.Lpar?,
        val innerMods: List<Modifier>,
        val ref: Type,
        override val rpar: Keyword.Rpar?,
    ) : Node(), WithModifiers, WithOptionalParentheses

    data class ValueArgs(
        val args: List<ValueArg>
    ) : Node() {
        data class ValueArg(
            val name: Expr.Name?,
            val asterisk: Boolean,
            val expr: Expr
        ) : Node()
    }

    sealed class Expr : Node() {
        data class If(
            val lpar: Keyword.Lpar,
            val expr: Expr,
            val rpar: Keyword.Rpar,
            val body: Expr,
            val elseBody: Expr?
        ) : Expr()
        data class Try(
            val block: Block,
            val catches: List<Catch>,
            val finallyBlock: Block?
        ) : Expr() {
            data class Catch(
                override val anns: List<Modifier.AnnotationSet>,
                val varName: String,
                val varType: Type.Simple,
                val block: Block
            ) : Node(), WithAnnotations
        }
        data class For(
            override val anns: List<Modifier.AnnotationSet>,
            // More than one means destructure, null means underscore
            val vars: List<Decl.Property.Var?>,
            val inExpr: Expr,
            val body: Expr
        ) : Expr(), WithAnnotations
        data class While(
            val expr: Expr,
            val body: Expr,
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
            data class Callable(
                override val recv: Recv?,
                val name: Name
            ) : DoubleColonRef()
            data class Class(
                override val recv: Recv?
            ) : DoubleColonRef()
            sealed class Recv : Node() {
                data class Expr(val expr: Node.Expr) : Recv()
                data class Type(
                    val type: Node.Type.Simple,
                    val questionMarks: Int
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
        data class Lambda(
            val params: List<Param>,
            val body: Body?
        ) : Expr() {
            data class Param(
                // Multiple means destructure, null means underscore
                val vars: List<Decl.Property.Var?>,
                val destructType: TypeRef?
            ) : Expr()

            data class Body(val stmts: List<Stmt>) : Expr()
        }
        data class This(
            val label: String?
        ) : Expr()
        data class Super(
            val typeArg: TypeRef?,
            val label: String?
        ) : Expr()
        data class When(
            val expr: Expr?,
            val entries: List<Entry>
        ) : Expr() {
            data class Entry(
                val conds: List<Cond>,
                val body: Expr
            ) : Node()
            sealed class Cond : Node() {
                data class Expr(val expr: Node.Expr) : Cond()
                data class In(
                    val expr: Node.Expr,
                    val not: Boolean
                ) : Cond()
                data class Is(
                    val type: TypeRef,
                    val not: Boolean
                ) : Cond()
            }
        }
        data class Object(
            val parents: List<Decl.Structured.Parent>,
            val members: List<Decl>
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
        data class CollLit(
            val exprs: List<Expr>
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
        data class Call(
            val expr: Expr,
            val typeArgs: List<TypeProjection?>,
            val args: ValueArgs?,
            val lambda: TrailLambda?
        ) : Expr() {
            data class TrailLambda(
                override val anns: List<Modifier.AnnotationSet>,
                val label: String?,
                val func: Lambda
            ) : Node(), WithAnnotations
        }
        data class ArrayAccess(
            val expr: Expr,
            val indices: List<Expr>
        ) : Expr()
        data class AnonFunc(
            val func: Decl.Func
        ) : Expr()
        // This is only present for when expressions and labeled expressions
        data class Property(
            val decl: Decl.Property
        ) : Expr()

        data class Block(val stmts: List<Stmt>) : Expr()
    }

    sealed class Stmt : Node() {
        data class Decl(val decl: Node.Decl) : Stmt()
        data class Expr(val expr: Node.Expr) : Stmt()
    }

    sealed class Modifier : Node() {
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
            data class Annotation(
                val nameTypeReference: Type,
                val typeArgs: List<TypeProjection>,
                val args: ValueArgs?
            ) : Node()
        }
        data class Lit(val keyword: Keyword) : Modifier()
        enum class Keyword {
            ABSTRACT, FINAL, OPEN, ANNOTATION, SEALED, DATA, OVERRIDE, LATEINIT, INNER, ENUM, COMPANION,
            PRIVATE, PROTECTED, PUBLIC, INTERNAL,
            IN, OUT, NOINLINE, CROSSINLINE, VARARG, REIFIED,
            TAILREC, OPERATOR, INFIX, INLINE, EXTERNAL, SUSPEND, CONST,
            ACTUAL, EXPECT
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
        class By : Keyword("by")
        class Equal : Keyword("=")
        class Colon : Keyword(":")
        class Lpar : Keyword("(")
        class Rpar : Keyword(")")
        class LBracket : Keyword("[")
        class RBracket : Keyword("]")
        class At : Keyword("@")
    }

    sealed class Extra : Node() {
        data class Whitespace(
            val text: String,
        ) : Extra()
        data class Comment(
            val text: String,
        ) : Extra()
        data class Semicolon(
            val text: String
        ) : Extra()
    }
}