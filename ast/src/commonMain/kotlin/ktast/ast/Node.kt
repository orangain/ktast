package ktast.ast

/**
 * Base class of all AST nodes.
 */
sealed class Node {
    var tag: Any? = null

    abstract class NodeList<out E : Node>(
        val prefix: String = "",
        val suffix: String = "",
    ) : Node() {
        abstract val elements: List<E>
    }

    abstract class CommaSeparatedNodeList<out E : Node>(
        prefix: String,
        suffix: String,
    ) : NodeList<E>(prefix, suffix) {
        abstract val trailingComma: Keyword.Comma?
    }

    interface WithAnnotationSets {
        val annotationSets: List<Modifier.AnnotationSet>
    }

    interface WithModifiers : WithAnnotationSets {
        val modifiers: Modifiers?
        override val annotationSets: List<Modifier.AnnotationSet>
            get() = modifiers?.elements.orEmpty().mapNotNull { it as? Modifier.AnnotationSet }
    }

    interface KotlinEntry : WithAnnotationSets {
        val packageDirective: PackageDirective?
        val importDirectives: ImportDirectives?
    }

    interface WithPostModifiers {
        val postModifiers: List<PostModifier>
    }

    interface StatementsContainer {
        val statements: List<Statement>
    }

    interface DeclarationsContainer {
        val declarations: List<Declaration>
    }

    /**
     * AST node corresponds to KtFile.
     */
    data class KotlinFile(
        override val annotationSets: List<Modifier.AnnotationSet>,
        override val packageDirective: PackageDirective?,
        override val importDirectives: ImportDirectives?,
        override val declarations: List<Declaration>
    ) : Node(), KotlinEntry, DeclarationsContainer

    data class KotlinScript(
        override val annotationSets: List<Modifier.AnnotationSet>,
        override val packageDirective: PackageDirective?,
        override val importDirectives: ImportDirectives?,
        val expressions: List<Expression>
    ) : Node(), KotlinEntry

    /**
     * AST node corresponds to KtPackageDirective.
     */
    data class PackageDirective(
        override val modifiers: Modifiers?,
        val packageKeyword: Keyword.Package,
        val names: List<Expression.Name>,
    ) : Node(), WithModifiers

    /**
     * AST node corresponds to KtImportList.
     */
    data class ImportDirectives(
        override val elements: List<ImportDirective>,
    ) : NodeList<ImportDirective>()

    /**
     * AST node corresponds to KtImportDirective.
     */
    data class ImportDirective(
        val importKeyword: Keyword.Import,
        val names: List<Expression.Name>,
        val alias: Alias?
    ) : Node() {

        /**
         * AST node corresponds to KtImportAlias.
         */
        data class Alias(
            val name: Expression.Name,
        ) : Node()
    }

    /**
     * Base class of [Node.Declaration] and [Node.Expression].
     */
    sealed class Statement : Node()

    sealed class Declaration : Statement() {
        /**
         * AST node corresponds to KtClassOrObject.
         */
        data class Class(
            override val modifiers: Modifiers?,
            val declarationKeyword: Keyword.Declaration,
            val name: Expression.Name?,
            val typeParams: TypeParams?,
            val primaryConstructor: PrimaryConstructor?,
            val parents: Parents?,
            val typeConstraints: PostModifier.TypeConstraints?,
            val body: Body?,
        ) : Declaration(), WithModifiers {

            val isClass = declarationKeyword.token == Keyword.DeclarationToken.CLASS
            val isObject = declarationKeyword.token == Keyword.DeclarationToken.OBJECT
            val isInterface = declarationKeyword.token == Keyword.DeclarationToken.INTERFACE
            val isCompanion = modifiers?.elements.orEmpty().contains(Modifier.Literal(Modifier.Keyword.COMPANION))
            val isEnum = modifiers?.elements.orEmpty().contains(Modifier.Literal(Modifier.Keyword.ENUM))

            /**
             * AST node corresponds to KtSuperTypeList.
             */
            data class Parents(
                override val elements: List<Parent>,
            ) : CommaSeparatedNodeList<Parent>("", "") {
                override val trailingComma: Keyword.Comma? = null
            }

            /**
             * AST node corresponds to KtSuperTypeListEntry.
             */
            sealed class Parent : Node() {
                /**
                 * AST node corresponds to KtSuperTypeCallEntry.
                 */
                data class CallConstructor(
                    val type: Node.Type.Simple,
                    val typeArgs: TypeArgs?,
                    val args: ValueArgs?,
                    val lambda: Expression.Call.LambdaArg?
                ) : Parent()

                /**
                 * AST node corresponds to KtDelegatedSuperTypeEntry.
                 */
                data class DelegatedType(
                    val type: Node.Type.Simple,
                    val byKeyword: Keyword.By,
                    val expression: Expression
                ) : Parent()

                /**
                 * AST node corresponds to KtSuperTypeEntry.
                 */
                data class Type(
                    val type: Node.Type.Simple,
                ) : Parent()
            }

            /**
             * AST node corresponds to KtPrimaryConstructor.
             */
            data class PrimaryConstructor(
                override val modifiers: Modifiers?,
                val constructorKeyword: Keyword.Constructor?,
                val params: Declaration.Function.Params?
            ) : Node(), WithModifiers

            /**
             * AST node corresponds to KtClassBody.
             */
            data class Body(
                val enumEntries: List<EnumEntry>,
                val hasTrailingCommaInEnumEntries: Boolean,
                override val declarations: List<Declaration>,
            ) : Node(), DeclarationsContainer
        }

        /**
         * AST node corresponds to KtAnonymousInitializer.
         */
        data class Init(
            override val modifiers: Modifiers?,
            val block: Expression.Block,
        ) : Declaration(), WithModifiers

        /**
         * AST node corresponds to KtNamedFunction.
         */
        data class Function(
            override val modifiers: Modifiers?,
            val funKeyword: Keyword.Fun,
            val typeParams: TypeParams?,
            val receiverTypeRef: TypeRef?,
            // Name not present on anonymous functions
            val name: Expression.Name?,
            // According to the Kotlin syntax, type parameters are not allowed here. However, Kotlin compiler can parse them.
            val postTypeParams: TypeParams?,
            val params: Params?,
            val typeRef: TypeRef?,
            override val postModifiers: List<PostModifier>,
            val body: Body?
        ) : Declaration(), WithModifiers, WithPostModifiers {
            /**
             * AST node corresponds to KtParameterList under KtNamedFunction.
             */
            data class Params(
                override val elements: List<Param>,
                override val trailingComma: Keyword.Comma?,
            ) : CommaSeparatedNodeList<Param>("(", ")")

            /**
             * AST node corresponds to KtParameter.
             */
            data class Param(
                override val modifiers: Modifiers?,
                val valOrVar: Keyword.ValOrVar?,
                val name: Expression.Name,
                // Type can be null for anon functions
                val typeRef: TypeRef?,
                val equals: Keyword.Equal?,
                val defaultValue: Expression?,
            ) : Node(), WithModifiers

            /**
             * Virtual AST node corresponds to function body.
             */
            sealed class Body : Node() {
                data class Block(val block: Expression.Block) : Declaration.Function.Body()
                data class Expr(
                    val equals: Keyword.Equal,
                    val expression: Expression,
                ) : Declaration.Function.Body()
            }
        }

        /**
         * AST node corresponds to KtProperty or KtDestructuringDeclaration.
         */
        data class Property(
            override val modifiers: Modifiers?,
            val valOrVar: Keyword.ValOrVar,
            val typeParams: TypeParams?,
            val receiverTypeRef: TypeRef?,
            // Always at least one, more than one is destructuring
            val variable: Variable,
            val typeConstraints: PostModifier.TypeConstraints?,
            val equals: Keyword.Equal?,
            val initializer: Expression?,
            val delegate: Delegate?,
            val accessors: List<Accessor>
        ) : Declaration(), WithModifiers {
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
                    val name: Expression.Name,
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

            /**
             * AST node corresponds to KtPropertyDelegate.
             */
            data class Delegate(
                val byKeyword: Keyword.By,
                val expression: Expression,
            ) : Node()

            /**
             * AST node corresponds to KtPropertyAccessor.
             */
            sealed class Accessor : Node(), WithModifiers, WithPostModifiers {
                abstract val body: Function.Body?

                data class Getter(
                    override val modifiers: Modifiers?,
                    val getKeyword: Keyword.Get,
                    val typeRef: TypeRef?,
                    override val postModifiers: List<PostModifier>,
                    override val body: Function.Body?
                ) : Accessor()

                data class Setter(
                    override val modifiers: Modifiers?,
                    val setKeyword: Keyword.Set,
                    val params: Params?,
                    override val postModifiers: List<PostModifier>,
                    override val body: Function.Body?
                ) : Accessor()

                /**
                 * AST node corresponds to KtParameterList under KtPropertyAccessor.
                 * Unlike [Function.Params], it does not contain parentheses.
                 */
                data class Params(
                    override val elements: List<Function.Param>,
                    override val trailingComma: Keyword.Comma?,
                ) : CommaSeparatedNodeList<Function.Param>("", "")
            }
        }

        /**
         * AST node corresponds to KtTypeAlias.
         */
        data class TypeAlias(
            override val modifiers: Modifiers?,
            val name: Expression.Name,
            val typeParams: TypeParams?,
            val typeRef: TypeRef
        ) : Declaration(), WithModifiers

        /**
         * AST node corresponds to KtSecondaryConstructor.
         */
        data class SecondaryConstructor(
            override val modifiers: Modifiers?,
            val constructorKeyword: Keyword.Constructor,
            val params: Declaration.Function.Params?,
            val delegationCall: DelegationCall?,
            val block: Expression.Block?
        ) : Declaration(), WithModifiers {
            /**
             * AST node corresponds to KtConstructorDelegationCall.
             */
            data class DelegationCall(
                val target: DelegationTarget,
                val args: ValueArgs?
            ) : Node()

            enum class DelegationTarget { THIS, SUPER }
        }

    }

    /**
     * AST node corresponds to KtEnumEntry.
     */
    data class EnumEntry(
        override val modifiers: Modifiers?,
        val name: Expression.Name,
        val args: ValueArgs?,
        val body: Declaration.Class.Body?,
    ) : Node(), WithModifiers

    /**
     * AST node corresponds to KtTypeParameterList.
     */
    data class TypeParams(
        override val elements: List<TypeParam>,
        override val trailingComma: Keyword.Comma?,
    ) : CommaSeparatedNodeList<TypeParam>("<", ">")

    /**
     * AST node corresponds to KtTypeParameter.
     */
    data class TypeParam(
        override val modifiers: Modifiers?,
        val name: Expression.Name,
        val typeRef: TypeRef?
    ) : Node(), WithModifiers

    sealed class Type : Node() {
        /**
         * AST node corresponds to KtFunctionType.
         */
        data class Function(
            val contextReceivers: ContextReceivers?,
            val receiver: Receiver?,
            val params: Params?,
            val typeRef: TypeRef
        ) : Type() {
            /**
             * AST node corresponds to KtContextReceiverList.
             */
            data class ContextReceivers(
                override val elements: List<ContextReceiver>,
                override val trailingComma: Keyword.Comma?,
            ) : CommaSeparatedNodeList<ContextReceiver>("(", ")")

            /**
             * AST node corresponds to KtContextReceiver.
             */
            data class ContextReceiver(
                val typeRef: TypeRef,
            ) : Node()

            /**
             * AST node corresponds KtFunctionTypeReceiver.
             */
            data class Receiver(
                val typeRef: TypeRef,
            ) : Node()

            /**
             * AST node corresponds to KtParameterList.
             */
            data class Params(
                override val elements: List<Param>,
                override val trailingComma: Keyword.Comma?,
            ) : CommaSeparatedNodeList<Param>("(", ")")

            /**
             * AST node corresponds to KtParameter.
             */
            data class Param(
                val name: Expression.Name?,
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
                val name: Expression.Name,
                val typeArgs: TypeArgs?
            ) : Node()
        }

        /**
         * AST node corresponds to KtNullableType.
         */
        data class Nullable(
            val lPar: Keyword.LPar?,
            override val modifiers: Modifiers?,
            val type: Type,
            val rPar: Keyword.RPar?,
        ) : Type(), WithModifiers

        /**
         * AST node corresponds to KtDynamicType.
         */
        data class Dynamic(val _unused_: Boolean = false) : Type()
    }

    /**
     * AST node corresponds to KtTypeArgumentList.
     */
    data class TypeArgs(
        override val elements: List<TypeArg>,
        override val trailingComma: Keyword.Comma?,
    ) : CommaSeparatedNodeList<TypeArg>("<", ">")

    /**
     * AST node corresponds to KtTypeProjection.
     */
    data class TypeArg(
        override val modifiers: Modifiers?,
        val typeRef: TypeRef?,
        val asterisk: Boolean,
    ) : Node(), WithModifiers

    /**
     * AST node corresponds to KtTypeReference.
     */
    data class TypeRef(
        val lPar: Keyword.LPar?,
        override val modifiers: Modifiers?,
        val innerLPar: Keyword.LPar?,
        val innerMods: Modifiers?,
        val type: Type?,
        val innerRPar: Keyword.RPar?,
        val rPar: Keyword.RPar?,
    ) : Node(), WithModifiers

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
        override val elements: List<ValueArg>,
        override val trailingComma: Keyword.Comma?,
    ) : CommaSeparatedNodeList<ValueArg>("(", ")")

    /**
     * AST node corresponds to KtValueArgument.
     */
    data class ValueArg(
        val name: Expression.Name?,
        val asterisk: Boolean, // Array spread operator
        val expression: Expression
    ) : Node()

    /**
     * AST node corresponds to KtContainerNode.
     */
    data class ExpressionContainer(
        val expression: Expression,
    ) : Node()

    sealed class Expression : Statement() {
        /**
         * AST node corresponds to KtIfExpression.
         */
        data class If(
            val ifKeyword: Keyword.If,
            val condition: Expression,
            val body: ExpressionContainer,
            val elseBody: ExpressionContainer?
        ) : Expression()

        /**
         * AST node corresponds to KtTryExpression.
         */
        data class Try(
            val block: Block,
            val catches: List<Catch>,
            val finallyBlock: Block?
        ) : Expression() {
            /**
             * AST node corresponds to KtCatchClause.
             */
            data class Catch(
                val catchKeyword: Keyword.Catch,
                val params: Declaration.Function.Params,
                val block: Block
            ) : Node()
        }

        /**
         * AST node corresponds to KtForExpression.
         */
        data class For(
            val forKeyword: Keyword.For,
            override val annotationSets: List<Modifier.AnnotationSet>,
            val loopParam: Lambda.Param,
            val loopRange: ExpressionContainer,
            val body: ExpressionContainer,
        ) : Expression(), WithAnnotationSets

        /**
         * AST node corresponds to KtWhileExpressionBase.
         */
        data class While(
            val whileKeyword: Keyword.While,
            val condition: ExpressionContainer,
            val body: ExpressionContainer,
            val doWhile: Boolean
        ) : Expression()

        /**
         * AST node corresponds to KtBinaryExpression or KtQualifiedExpression.
         */
        data class Binary(
            val lhs: Expression,
            val operator: Operator,
            val rhs: Expression
        ) : Expression() {
            sealed class Operator : Node() {
                data class Infix(val str: String) : Operator()
                data class Token(val token: Binary.Token) : Operator()
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

        /**
         * AST node corresponds to KtUnaryExpression.
         */
        data class Unary(
            val expression: Expression,
            val operator: Operator,
            val prefix: Boolean
        ) : Expression() {
            data class Operator(val token: Token) : Node()
            enum class Token(val str: String) {
                NEG("-"), POS("+"), INC("++"), DEC("--"), NOT("!"), NULL_DEREF("!!")
            }
        }

        /**
         * AST node corresponds to KtBinaryExpressionWithTypeRHS or KtIsExpression.
         */
        data class BinaryType(
            val lhs: Expression,
            val operator: Operator,
            val rhs: TypeRef
        ) : Expression() {
            data class Operator(val token: Token) : Node()
            enum class Token(val str: String) {
                AS("as"), AS_SAFE("as?"), COL(":"), IS("is"), NOT_IS("!is")
            }
        }

        /**
         * AST node corresponds to KtDoubleColonExpression.
         */
        sealed class DoubleColon : Expression() {
            abstract val lhs: Receiver?

            sealed class Receiver : Node() {
                data class Expression(val expression: Node.Expression) : Receiver()
                data class Type(
                    val type: Node.Type.Simple,
                    val questionMarks: List<Keyword.Question>,
                ) : Receiver()
            }
        }

        /**
         * AST node corresponds to KtCallableReferenceExpression.
         */
        data class CallableReference(
            override val lhs: Receiver?,
            val rhs: Name
        ) : DoubleColon()

        /**
         * AST node corresponds to KtClassLiteralExpression.
         */
        data class ClassLiteral(
            // Class literal expression without lhs is not supported, but Kotlin compiler does parse it.
            override val lhs: Receiver?
        ) : DoubleColon()

        /**
         * AST node corresponds to KtParenthesizedExpression.
         */
        data class Parenthesized(
            val expression: Expression
        ) : Expression()

        /**
         * AST node corresponds to KtStringTemplateExpression.
         */
        data class StringTemplate(
            val entries: List<Entry>,
            val raw: Boolean
        ) : Expression() {
            /**
             * AST node corresponds to KtStringTemplateEntry.
             */
            sealed class Entry : Node() {
                data class Regular(val str: String) : Entry()
                data class ShortTemplate(val str: String) : Entry()
                data class UnicodeEscape(val digits: String) : Entry()
                data class RegularEscape(val char: Char) : Entry()
                data class LongTemplate(val expression: Expression) : Entry()
            }
        }

        /**
         * AST node corresponds to KtConstantExpression.
         */
        data class Constant(
            val value: String,
            val form: Form
        ) : Expression() {
            enum class Form { BOOLEAN, CHAR, INT, FLOAT, NULL }
        }

        /**
         * AST node corresponds to KtLambdaExpression.
         */
        data class Lambda(
            val params: Params?,
            val body: Body?
        ) : Expression() {
            /**
             * AST node corresponds to KtParameterList under KtLambdaExpression.
             */
            data class Params(
                override val elements: List<Param>,
                override val trailingComma: Keyword.Comma?,
            ) : CommaSeparatedNodeList<Param>("", "")

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
                    val vars: Variables,
                    val destructTypeRef: TypeRef?
                ) : Param() {
                    /**
                     * AST node corresponds to KtDestructuringDeclaration in lambda arguments.
                     */
                    data class Variables(
                        override val elements: List<Single>,
                        override val trailingComma: Keyword.Comma?,
                    ) : CommaSeparatedNodeList<Single>("(", ")")
                }
            }

            /**
             * AST node corresponds to KtBlockExpression in lambda body.
             * In lambda expression, left and right braces are not included in Lambda.Body, but are included in Lambda.
             * This means:
             *
             * <Lambda> = { <Param>, <Param> -> <Body> }
             */
            data class Body(override val statements: List<Statement>) : Expression(), StatementsContainer
        }

        /**
         * AST node corresponds to KtThisExpression.
         */
        data class This(
            val label: String?
        ) : Expression()

        /**
         * AST node corresponds to KtSuperExpression.
         */
        data class Super(
            val typeArg: TypeRef?,
            val label: String?
        ) : Expression()

        /**
         * AST node corresponds to KtWhenExpression.
         */
        data class When(
            val lPar: Keyword.LPar,
            val expression: Expression?,
            val rPar: Keyword.RPar,
            val entries: List<Entry>
        ) : Expression() {
            /**
             * AST node corresponds to KtWhenEntry.
             */
            sealed class Entry : Node() {
                data class Conditions(
                    val conditions: List<Condition>,
                    val trailingComma: Keyword.Comma?,
                    val body: Expression,
                ) : Entry()

                data class Else(
                    val elseKeyword: Keyword.Else,
                    val body: Expression,
                ) : Entry()
            }

            /**
             * AST node corresponds to KtWhenCondition.
             */
            sealed class Condition : Node() {
                /**
                 * AST node corresponds to KtWhenConditionWithExpression.
                 */
                data class Expression(val expression: Node.Expression) : Condition()

                /**
                 * AST node corresponds to KtWhenConditionInRange.
                 */
                data class In(
                    val expression: Node.Expression,
                    val not: Boolean
                ) : Condition()

                /**
                 * AST node corresponds to KtWhenConditionIsPattern.
                 */
                data class Is(
                    val typeRef: TypeRef,
                    val not: Boolean
                ) : Condition()
            }
        }

        /**
         * AST node corresponds to KtObjectLiteralExpression.
         */
        data class Object(
            val declaration: Declaration.Class,
        ) : Expression()

        /**
         * AST node corresponds to KtThrowExpression.
         */
        data class Throw(
            val expression: Expression
        ) : Expression()

        /**
         * AST node corresponds to KtReturnExpression.
         */
        data class Return(
            val label: String?,
            val expression: Expression?
        ) : Expression()

        /**
         * AST node corresponds to KtContinueExpression.
         */
        data class Continue(
            val label: String?
        ) : Expression()

        /**
         * AST node corresponds to KtBreakExpression.
         */
        data class Break(
            val label: String?
        ) : Expression()

        /**
         * AST node corresponds to KtCollectionLiteralExpression.
         */
        data class CollectionLiteral(
            val expressions: List<Expression>,
            val trailingComma: Keyword.Comma?,
        ) : Expression()

        /**
         * AST node corresponds to KtValueArgumentName, KtSimpleNameExpression or PsiElement whose elementType is IDENTIFIER.
         */
        data class Name(
            val name: String
        ) : Expression()

        /**
         * AST node corresponds to KtLabeledExpression.
         */
        data class Labeled(
            val label: String,
            val expression: Expression
        ) : Expression()

        /**
         * AST node corresponds to KtAnnotatedExpression.
         */
        data class Annotated(
            override val annotationSets: List<Modifier.AnnotationSet>,
            val expression: Expression
        ) : Expression(), WithAnnotationSets

        /**
         * AST node corresponds to KtCallExpression.
         */
        data class Call(
            val expression: Expression,
            val typeArgs: TypeArgs?,
            val args: ValueArgs?,
            // According to the Kotlin syntax, at most one lambda argument is allowed. However, Kotlin compiler can parse multiple lambda arguments.
            val lambdaArgs: List<LambdaArg>
        ) : Expression() {
            /**
             * AST node corresponds to KtLambdaArgument.
             */
            data class LambdaArg(
                override val annotationSets: List<Modifier.AnnotationSet>,
                val label: String?,
                val func: Lambda
            ) : Node(), WithAnnotationSets
        }

        /**
         * AST node corresponds to KtArrayAccessExpression.
         */
        data class ArrayAccess(
            val expression: Expression,
            val indices: List<Expression>,
            val trailingComma: Keyword.Comma?,
        ) : Expression()

        /**
         * Virtual AST node corresponds to KtNamedFunction in expression context.
         */
        data class AnonymousFunction(
            val function: Declaration.Function
        ) : Expression()

        /**
         * AST node corresponds to KtProperty or KtDestructuringDeclaration.
         * This is only present for when expressions and labeled expressions.
         */
        data class Property(
            val declaration: Declaration.Property
        ) : Expression()

        /**
         * AST node corresponds to KtBlockExpression.
         */
        data class Block(override val statements: List<Statement>) : Expression(), StatementsContainer
    }

    /**
     * AST node corresponds to KtModifierList.
     */
    data class Modifiers(
        override val elements: List<Modifier>,
    ) : NodeList<Modifier>()

    sealed class Modifier : Node() {
        /**
         * AST node corresponds to KtAnnotation or single KtAnnotationEntry.
         */
        data class AnnotationSet(
            val atSymbol: Node.Keyword.At?,
            val target: Target?,
            val lBracket: Node.Keyword.LBracket?,
            val annotations: List<Annotation>,
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

        data class Literal(val keyword: Keyword) : Modifier()
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
            val constraints: TypeConstraintList,
        ) : PostModifier() {
            /**
             * AST node corresponds to KtTypeConstraintList.
             */
            data class TypeConstraintList(
                override val elements: List<TypeConstraint>,
            ) : CommaSeparatedNodeList<TypeConstraint>("", "") {
                override val trailingComma: Keyword.Comma? = null // Trailing comma is not allowed.
            }

            /**
             * AST node corresponds to KtTypeConstraint.
             */
            data class TypeConstraint(
                override val annotationSets: List<Modifier.AnnotationSet>,
                val name: Expression.Name,
                val typeRef: TypeRef
            ) : Node(), WithAnnotationSets
        }

        /**
         * Virtual AST node corresponds to a pair of "contract" keyword and KtContractEffectList.
         */
        data class Contract(
            val contractKeyword: Keyword.Contract,
            val contractEffects: ContractEffects,
        ) : PostModifier() {
            /**
             * AST node corresponds to KtContractEffectList.
             */
            data class ContractEffects(
                override val elements: List<ContractEffect>,
                override val trailingComma: Keyword.Comma?,
            ) : CommaSeparatedNodeList<ContractEffect>("[", "]")

            /**
             * AST node corresponds to KtContractEffect.
             */
            data class ContractEffect(
                val expression: Expression,
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

        class Package : Keyword("package")
        class Import : Keyword("import")
        class Fun : Keyword("fun")
        class Constructor : Keyword("constructor")
        class For : Keyword("for")
        class While : Keyword("while")
        class If : Keyword("if")
        class Else : Keyword("else")
        class Catch : Keyword("catch")
        class By : Keyword("by")
        class Contract : Keyword("contract")
        class Where : Keyword("where")
        class Get : Keyword("get")
        class Set : Keyword("set")
        class Equal : Keyword("=")
        class Comma : Keyword(",")
        class Question : Keyword("?")
        class LPar : Keyword("(")
        class RPar : Keyword(")")
        class LBracket : Keyword("[")
        class RBracket : Keyword("]")
        class At : Keyword("@")
    }

    sealed class Extra : Node() {
        abstract val text: String

        /**
         * AST node corresponds to PsiWhiteSpace.
         */
        data class Whitespace(
            override val text: String,
        ) : Extra()

        /**
         * AST node corresponds to PsiComment.
         */
        data class Comment(
            override val text: String,
        ) : Extra()

        /**
         * AST node corresponds to PsiElement whose elementType is SEMICOLON.
         */
        data class Semicolon(
            override val text: String
        ) : Extra()
    }
}