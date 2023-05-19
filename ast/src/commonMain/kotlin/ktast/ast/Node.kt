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
        val annotationSets: List<AnnotationSetModifier>
    }

    interface WithModifiers : WithAnnotationSets {
        val modifiers: Modifiers?
        override val annotationSets: List<AnnotationSetModifier>
            get() = modifiers?.elements.orEmpty().mapNotNull { it as? AnnotationSetModifier }
    }

    interface KotlinEntry : WithAnnotationSets {
        val packageDirective: PackageDirective?
        val importDirectives: ImportDirectives?
    }

    interface WithPostModifiers {
        val postModifiers: List<PostModifier>
    }

    interface WithFunctionBody {
        val equals: Keyword.Equal?
        val body: Expression?
    }

    interface StatementsContainer {
        val statements: List<Statement>
    }

    interface DeclarationsContainer {
        val declarations: List<Declaration>
    }

    interface HasSimpleStringRepresentation {
        val string: String
    }

    interface TokenContainer<T : HasSimpleStringRepresentation> : HasSimpleStringRepresentation {
        val token: T
        override val string: String
            get() = token.string
    }

    /**
     * AST node corresponds to KtFile.
     */
    data class KotlinFile(
        override val annotationSets: List<AnnotationSetModifier>,
        override val packageDirective: PackageDirective?,
        override val importDirectives: ImportDirectives?,
        override val declarations: List<Declaration>
    ) : Node(), KotlinEntry, DeclarationsContainer

    data class KotlinScript(
        override val annotationSets: List<AnnotationSetModifier>,
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
        val names: List<NameExpression>,
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
        val names: List<NameExpression>,
        val importAlias: ImportAlias?
    ) : Node() {

        /**
         * AST node corresponds to KtImportAlias.
         */
        data class ImportAlias(
            val name: NameExpression,
        ) : Node()
    }

    /**
     * Base class of [Node.Declaration] and [Node.Expression].
     */
    sealed class Statement : Node()

    sealed class Declaration : Statement()

    /**
     * AST node corresponds to KtClassOrObject.
     */
    data class ClassDeclaration(
        override val modifiers: Modifiers?,
        val declarationKeyword: DeclarationKeyword,
        val name: NameExpression?,
        val typeParams: TypeParams?,
        val primaryConstructor: PrimaryConstructor?,
        val parents: Parents?,
        val typeConstraints: TypeConstraints?,
        val body: Body?,
    ) : Declaration(), WithModifiers {

        val isClass = declarationKeyword.token == DeclarationKeyword.Token.CLASS
        val isObject = declarationKeyword.token == DeclarationKeyword.Token.OBJECT
        val isInterface = declarationKeyword.token == DeclarationKeyword.Token.INTERFACE
        val isCompanion = modifiers?.elements.orEmpty().contains(KeywordModifier(KeywordModifier.Token.COMPANION))
        val isEnum = modifiers?.elements.orEmpty().contains(KeywordModifier(KeywordModifier.Token.ENUM))

        data class DeclarationKeyword(override val token: Token) : Node(),
            TokenContainer<DeclarationKeyword.Token> {
            companion object {
                private val mapStringToToken = Token.values().associateBy { it.string }
                fun of(value: String): DeclarationKeyword =
                    mapStringToToken[value]?.let(::DeclarationKeyword) ?: error("Unknown value: $value")
            }

            enum class Token : HasSimpleStringRepresentation {
                INTERFACE, CLASS, OBJECT;

                override val string: String
                    get() = name.lowercase()
            }
        }

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
                val type: SimpleType,
                val typeArgs: TypeArgs?,
                val args: ValueArgs?,
                val lambda: CallExpression.LambdaArg?
            ) : Parent()

            /**
             * AST node corresponds to KtDelegatedSuperTypeEntry.
             */
            data class DelegatedType(
                val type: SimpleType,
                val byKeyword: Keyword.By,
                val expression: Expression
            ) : Parent()

            /**
             * AST node corresponds to KtSuperTypeEntry.
             */
            data class Type(
                val type: SimpleType,
            ) : Parent()
        }

        /**
         * AST node corresponds to KtPrimaryConstructor.
         */
        data class PrimaryConstructor(
            override val modifiers: Modifiers?,
            val constructorKeyword: Keyword.Constructor?,
            val params: FunctionDeclaration.Params?
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
    data class InitDeclaration(
        override val modifiers: Modifiers?,
        val block: BlockExpression,
    ) : Declaration(), WithModifiers

    /**
     * AST node corresponds to KtNamedFunction.
     */
    data class FunctionDeclaration(
        override val modifiers: Modifiers?,
        val funKeyword: Keyword.Fun,
        val typeParams: TypeParams?,
        val receiverTypeRef: TypeRef?,
        // Name not present on anonymous functions
        val name: NameExpression?,
        val params: Params?,
        val typeRef: TypeRef?,
        override val postModifiers: List<PostModifier>,
        override val equals: Keyword.Equal?,
        override val body: Expression?,
    ) : Declaration(), WithModifiers, WithPostModifiers, WithFunctionBody {
        /**
         * AST node corresponds to KtParameterList under KtNamedFunction.
         */
        data class Params(
            override val elements: List<Param>,
            override val trailingComma: Keyword.Comma?,
        ) : CommaSeparatedNodeList<Param>("(", ")")

        /**
         * AST node corresponds to KtParameter inside KtNamedFunction.
         */
        data class Param(
            override val modifiers: Modifiers?,
            val valOrVar: PropertyDeclaration.ValOrVar?,
            val name: NameExpression,
            // typeRef can be null for anon functions
            val typeRef: TypeRef?,
            val equals: Keyword.Equal?,
            val defaultValue: Expression?,
        ) : Node(), WithModifiers
    }

    /**
     * AST node corresponds to KtProperty or KtDestructuringDeclaration.
     */
    data class PropertyDeclaration(
        override val modifiers: Modifiers?,
        val valOrVar: ValOrVar,
        val typeParams: TypeParams?,
        val receiverTypeRef: TypeRef?,
        val lPar: Keyword.LPar?,
        // Always at least one, more than one is destructuring
        val variables: List<Variable>,
        val trailingComma: Keyword.Comma?,
        val rPar: Keyword.RPar?,
        val typeConstraints: TypeConstraints?,
        val equals: Keyword.Equal?,
        val initializer: Expression?,
        val delegate: Delegate?,
        val accessors: List<Accessor>
    ) : Declaration(), WithModifiers {
        init {
            if (delegate != null) {
                require(equals == null && initializer == null) {
                    "equals and initializer must be null when delegate is not null"
                }
            }
            require((equals == null && initializer == null) || (equals != null && initializer != null)) {
                "equals and initializer must be both null or both non-null"
            }
            if (variables.size >= 2) {
                require(lPar != null && rPar != null) { "lPar and rPar are required when there are multiple variables" }
            }
            if (trailingComma != null) {
                require(lPar != null && rPar != null) { "lPar and rPar are required when trailing comma exists" }
            }
        }

        data class ValOrVar(override val token: Token) : Node(), TokenContainer<ValOrVar.Token> {
            companion object {
                private val mapStringToToken = Token.values().associateBy { it.string }
                fun of(value: String): ValOrVar =
                    mapStringToToken[value]?.let(::ValOrVar) ?: error("Unknown value: $value")
            }

            enum class Token : HasSimpleStringRepresentation {
                VAL, VAR;

                override val string: String
                    get() = name.lowercase()
            }
        }

        /**
         * Virtual AST node corresponds a part of KtProperty or AST node corresponds to KtDestructuringDeclarationEntry.
         */
        data class Variable(
            val name: NameExpression,
            val typeRef: TypeRef?
        ) : Node()

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
        sealed class Accessor : Node(), WithModifiers, WithPostModifiers, WithFunctionBody {

            data class Getter(
                override val modifiers: Modifiers?,
                val getKeyword: Keyword.Get,
                val typeRef: TypeRef?,
                override val postModifiers: List<PostModifier>,
                override val equals: Keyword.Equal?,
                override val body: Expression?,
            ) : Accessor()

            data class Setter(
                override val modifiers: Modifiers?,
                val setKeyword: Keyword.Set,
                val params: LambdaExpression.Params?,
                override val postModifiers: List<PostModifier>,
                override val equals: Keyword.Equal?,
                override val body: Expression?,
            ) : Accessor()
        }
    }

    /**
     * AST node corresponds to KtTypeAlias.
     */
    data class TypeAliasDeclaration(
        override val modifiers: Modifiers?,
        val name: NameExpression,
        val typeParams: TypeParams?,
        val typeRef: TypeRef
    ) : Declaration(), WithModifiers

    /**
     * AST node corresponds to KtSecondaryConstructor.
     */
    data class SecondaryConstructorDeclaration(
        override val modifiers: Modifiers?,
        val constructorKeyword: Keyword.Constructor,
        val params: FunctionDeclaration.Params?,
        val delegationCall: DelegationCall?,
        val block: BlockExpression?
    ) : Declaration(), WithModifiers {
        /**
         * AST node corresponds to KtConstructorDelegationCall.
         */
        data class DelegationCall(
            val target: DelegationTarget,
            val args: ValueArgs?
        ) : Node()

        data class DelegationTarget(override val token: Token) : Node(), TokenContainer<DelegationTarget.Token> {
            companion object {
                private val mapStringToToken = Token.values().associateBy { it.string }
                fun of(value: String): DelegationTarget = mapStringToToken[value]?.let(::DelegationTarget)
                    ?: error("Unknown value: $value")
            }

            enum class Token : HasSimpleStringRepresentation {
                THIS, SUPER;

                override val string: String
                    get() = name.lowercase()
            }
        }
    }

    /**
     * AST node corresponds to KtEnumEntry.
     */
    data class EnumEntry(
        override val modifiers: Modifiers?,
        val name: NameExpression,
        val args: ValueArgs?,
        val body: ClassDeclaration.Body?,
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
        val name: NameExpression,
        val typeRef: TypeRef?
    ) : Node(), WithModifiers

    sealed class Type : Node() {

        interface NameWithTypeArgs {
            val name: NameExpression
            val typeArgs: TypeArgs?
        }

    }

    /**
     * AST node corresponds to KtFunctionType.
     * Note that properties [lPar], [modifiers] and [rPar] correspond to those of parent KtTypeReference.
     */
    data class FunctionType(
        val lPar: Keyword.LPar?,
        override val modifiers: Modifiers?,
        val contextReceivers: ContextReceivers?,
        val receiver: Receiver?,
        val params: Params?,
        val returnTypeRef: TypeRef,
        val rPar: Keyword.RPar?,
    ) : Type(), WithModifiers {
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
         * AST node corresponds to KtParameterList under KtFunctionType.
         */
        data class Params(
            override val elements: List<Param>,
            override val trailingComma: Keyword.Comma?,
        ) : CommaSeparatedNodeList<Param>("(", ")")

        /**
         * AST node corresponds to KtParameter inside KtFunctionType.
         */
        data class Param(
            val name: NameExpression?,
            val typeRef: TypeRef
        ) : Node()
    }

    /**
     * AST node corresponds to KtUserType.
     */
    data class SimpleType(
        val qualifiers: List<Qualifier>,
        override val name: NameExpression,
        override val typeArgs: TypeArgs?,
    ) : Type(), Type.NameWithTypeArgs {
        data class Qualifier(
            override val name: NameExpression,
            override val typeArgs: TypeArgs?,
        ) : Node(), NameWithTypeArgs
    }

    /**
     * AST node corresponds to KtNullableType.
     */
    data class NullableType(
        val lPar: Keyword.LPar?,
        override val modifiers: Modifiers?,
        val type: Type,
        val rPar: Keyword.RPar?,
    ) : Type(), WithModifiers

    /**
     * AST node corresponds to KtDynamicType.
     */
    data class DynamicType(val _unused_: Boolean = false) : Type()

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
    ) : Node(), WithModifiers {
        init {
            if (asterisk) {
                require(modifiers == null && typeRef == null) {
                    "modifiers and typeRef must be null when asterisk is true"
                }
            } else {
                require(typeRef != null) {
                    "typeRef must be not null when asterisk is false"
                }
            }
        }
    }

    /**
     * AST node corresponds to KtTypeReference.
     */
    data class TypeRef(
        val lPar: Keyword.LPar?,
        override val modifiers: Modifiers?,
        val type: Type,
        val rPar: Keyword.RPar?,
    ) : Node(), WithModifiers

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
        val name: NameExpression?,
        val asterisk: Boolean, // Array spread operator
        val expression: Expression
    ) : Node()

    /**
     * AST node corresponds to KtContainerNode.
     */
    data class ExpressionContainer(
        val expression: Expression,
    ) : Node()

    sealed class Expression : Statement()

    /**
     * AST node corresponds to KtIfExpression.
     */
    data class IfExpression(
        val ifKeyword: Keyword.If,
        val condition: Expression,
        val body: ExpressionContainer,
        val elseBody: ExpressionContainer?
    ) : Expression()

    /**
     * AST node corresponds to KtTryExpression.
     */
    data class TryExpression(
        val block: BlockExpression,
        val catches: List<Catch>,
        val finallyBlock: BlockExpression?
    ) : Expression() {
        /**
         * AST node corresponds to KtCatchClause.
         */
        data class Catch(
            val catchKeyword: Keyword.Catch,
            val params: FunctionDeclaration.Params,
            val block: BlockExpression
        ) : Node()
    }

    /**
     * AST node corresponds to KtForExpression.
     */
    data class ForExpression(
        val forKeyword: Keyword.For,
        val loopParam: LambdaExpression.Param,
        val loopRange: ExpressionContainer,
        val body: ExpressionContainer,
    ) : Expression()

    /**
     * AST node corresponds to KtWhileExpressionBase.
     */
    data class WhileExpression(
        val whileKeyword: Keyword.While,
        val condition: ExpressionContainer,
        val body: ExpressionContainer,
        val doWhile: Boolean
    ) : Expression()

    sealed class BaseBinaryExpression : Expression() {
        abstract val lhs: Expression
        abstract val rhs: Expression
    }

    /**
     * AST node corresponds to KtBinaryExpression or KtQualifiedExpression.
     */
    data class BinaryExpression(
        override val lhs: Expression,
        val operator: Operator,
        override val rhs: Expression
    ) : BaseBinaryExpression() {
        data class Operator(override val token: Token) : Node(), TokenContainer<Operator.Token> {
            companion object {
                private val mapStringToToken = Token.values().associateBy { it.string }
                fun of(value: String): Operator = mapStringToToken[value]?.let(::Operator)
                    ?: error("Unknown value: $value")
            }

            enum class Token(override val string: String) : HasSimpleStringRepresentation {
                MUL("*"), DIV("/"), MOD("%"), ADD("+"), SUB("-"),
                IN("in"), NOT_IN("!in"),
                GT(">"), GTE(">="), LT("<"), LTE("<="),
                EQ("=="), NEQ("!="),
                ASSN("="), MUL_ASSN("*="), DIV_ASSN("/="), MOD_ASSN("%="), ADD_ASSN("+="), SUB_ASSN("-="),
                OR("||"), AND("&&"), ELVIS("?:"), RANGE(".."), UNTIL("..<"),
                DOT("."), DOT_SAFE("?."), SAFE("?")
            }
        }
    }

    data class BinaryInfixExpression(
        override val lhs: Expression,
        val operator: NameExpression,
        override val rhs: Expression
    ) : BaseBinaryExpression()

    /**
     * AST node corresponds to KtUnaryExpression.
     */
    data class UnaryExpression(
        val expression: Expression,
        val operator: Operator,
        val prefix: Boolean
    ) : Expression() {
        data class Operator(override val token: Token) : Node(), TokenContainer<Operator.Token> {
            companion object {
                private val mapStringToToken = Token.values().associateBy { it.string }
                fun of(value: String): Operator =
                    mapStringToToken[value]?.let(::Operator) ?: error("Unknown value: $value")
            }

            enum class Token(override val string: String) : HasSimpleStringRepresentation {
                NEG("-"), POS("+"), INC("++"), DEC("--"), NOT("!"), NULL_DEREF("!!")
            }
        }
    }

    /**
     * AST node corresponds to KtBinaryExpressionWithTypeRHS or KtIsExpression.
     */
    data class BinaryTypeExpression(
        val lhs: Expression,
        val operator: Operator,
        val rhs: TypeRef
    ) : Expression() {
        data class Operator(override val token: Token) : Node(), TokenContainer<Operator.Token> {
            companion object {
                private val mapStringToToken = Token.values().associateBy { it.string }
                fun of(value: String): Operator =
                    mapStringToToken[value]?.let(::Operator) ?: error("Unknown value: $value")
            }

            enum class Token(override val string: String) : HasSimpleStringRepresentation {
                AS("as"), AS_SAFE("as?"), COL(":"), IS("is"), NOT_IS("!is")
            }
        }

    }

    /**
     * AST node corresponds to KtDoubleColonExpression.
     */
    sealed class DoubleColonExpression : Expression() {
        abstract val lhs: Receiver?

        sealed class Receiver : Node() {
            data class Expression(val expression: Node.Expression) : Receiver()
            data class Type(
                val type: SimpleType,
                val questionMarks: List<Keyword.Question>,
            ) : Receiver()
        }
    }

    /**
     * AST node corresponds to KtCallableReferenceExpression.
     */
    data class CallableReferenceExpression(
        override val lhs: Receiver?,
        val rhs: NameExpression
    ) : DoubleColonExpression()

    /**
     * AST node corresponds to KtClassLiteralExpression.
     */
    data class ClassLiteralExpression(
        // Class literal expression without lhs is not supported in Kotlin syntax, but Kotlin compiler does parse it.
        override val lhs: Receiver?
    ) : DoubleColonExpression()

    /**
     * AST node corresponds to KtParenthesizedExpression.
     */
    data class ParenthesizedExpression(
        val expression: Expression
    ) : Expression()

    /**
     * AST node corresponds to KtStringTemplateExpression.
     */
    data class StringTemplateExpression(
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
    data class ConstantExpression(
        val value: String,
        val form: Form
    ) : Expression() {
        enum class Form { BOOLEAN, CHAR, INT, FLOAT, NULL }
    }

    /**
     * AST node corresponds to KtLambdaExpression.
     */
    data class LambdaExpression(
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
         * AST node corresponds to KtParameter under KtLambdaExpression.
         */
        data class Param(
            val lPar: Keyword.LPar?,
            val variables: List<Variable>,
            val trailingComma: Keyword.Comma?,
            val rPar: Keyword.RPar?,
            val colon: Keyword.Colon?,
            val destructTypeRef: TypeRef?,
        ) : Node() {
            init {
                if (variables.size >= 2) {
                    require(lPar != null && rPar != null) { "lPar and rPar are required when there are multiple variables" }
                }
                if (trailingComma != null) {
                    require(lPar != null && rPar != null) { "lPar and rPar are required when trailing comma exists" }
                }
            }

            /**
             * AST node corresponds to KtDestructuringDeclarationEntry or virtual AST node corresponds to KtParameter whose child is IDENTIFIER.
             */
            data class Variable(
                override val modifiers: Modifiers?,
                val name: NameExpression,
                val typeRef: TypeRef?,
            ) : Node(), WithModifiers
        }

        /**
         * AST node corresponds to KtBlockExpression in lambda body.
         * In lambda expression, left and right braces are not included in [LambdaExpression.Body], but are included in Lambda.
         * This means:
         *
         * <Lambda> = { <Param>, <Param> -> <Body> }
         */
        data class Body(override val statements: List<Statement>) : Expression(), StatementsContainer
    }

    /**
     * AST node corresponds to KtThisExpression.
     */
    data class ThisExpression(
        val label: String?
    ) : Expression()

    /**
     * AST node corresponds to KtSuperExpression.
     */
    data class SuperExpression(
        val typeArg: TypeRef?,
        val label: String?
    ) : Expression()

    /**
     * AST node corresponds to KtWhenExpression.
     */
    data class WhenExpression(
        val whenKeyword: Keyword.When,
        val lPar: Keyword.LPar?,
        val expression: Expression?,
        val rPar: Keyword.RPar?,
        val branches: List<Branch>
    ) : Expression() {
        /**
         * AST node corresponds to KtWhenEntry.
         */
        sealed class Branch : Node() {
            data class Conditional(
                val conditions: List<Condition>,
                val trailingComma: Keyword.Comma?,
                val body: Expression,
            ) : Branch()

            data class Else(
                val elseKeyword: Keyword.Else,
                val body: Expression,
            ) : Branch()
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
    data class ObjectExpression(
        val declaration: ClassDeclaration,
    ) : Expression()

    /**
     * AST node corresponds to KtThrowExpression.
     */
    data class ThrowExpression(
        val expression: Expression
    ) : Expression()

    /**
     * AST node corresponds to KtReturnExpression.
     */
    data class ReturnExpression(
        val label: String?,
        val expression: Expression?
    ) : Expression()

    /**
     * AST node corresponds to KtContinueExpression.
     */
    data class ContinueExpression(
        val label: String?
    ) : Expression()

    /**
     * AST node corresponds to KtBreakExpression.
     */
    data class BreakExpression(
        val label: String?
    ) : Expression()

    /**
     * AST node corresponds to KtCollectionLiteralExpression.
     */
    data class CollectionLiteralExpression(
        val expressions: List<Expression>,
        val trailingComma: Keyword.Comma?,
    ) : Expression()

    /**
     * AST node corresponds to KtValueArgumentName, KtSimpleNameExpression or PsiElement whose elementType is IDENTIFIER.
     */
    data class NameExpression(
        val name: String
    ) : Expression()

    /**
     * AST node corresponds to KtLabeledExpression.
     */
    data class LabeledExpression(
        val label: String,
        val expression: Expression
    ) : Expression()

    /**
     * AST node corresponds to KtAnnotatedExpression.
     */
    data class AnnotatedExpression(
        override val annotationSets: List<AnnotationSetModifier>,
        val expression: Expression
    ) : Expression(), WithAnnotationSets

    /**
     * AST node corresponds to KtCallExpression.
     */
    data class CallExpression(
        val expression: Expression,
        val typeArgs: TypeArgs?,
        val args: ValueArgs?,
        val lambdaArg: LambdaArg?,
    ) : Expression() {
        /**
         * AST node corresponds to KtLambdaArgument.
         */
        data class LambdaArg(
            override val annotationSets: List<AnnotationSetModifier>,
            val label: String?,
            val expression: LambdaExpression
        ) : Node(), WithAnnotationSets
    }

    /**
     * AST node corresponds to KtArrayAccessExpression.
     */
    data class ArrayAccessExpression(
        val expression: Expression,
        val indices: List<Expression>,
        val trailingComma: Keyword.Comma?,
    ) : Expression()

    /**
     * Virtual AST node corresponds to KtNamedFunction in expression context.
     */
    data class AnonymousFunctionExpression(
        val function: FunctionDeclaration
    ) : Expression()

    /**
     * AST node corresponds to KtProperty or KtDestructuringDeclaration.
     * This is only present for when expressions and labeled expressions.
     */
    data class PropertyExpression(
        val declaration: PropertyDeclaration
    ) : Expression()

    /**
     * AST node corresponds to KtBlockExpression.
     */
    data class BlockExpression(override val statements: List<Statement>) : Expression(), StatementsContainer

    /**
     * AST node corresponds to KtModifierList.
     */
    data class Modifiers(
        override val elements: List<Modifier>,
    ) : NodeList<Modifier>()

    sealed class Modifier : Node()

    /**
     * AST node corresponds to KtAnnotation or KtAnnotationEntry not under KtAnnotation.
     */
    data class AnnotationSetModifier(
        val atSymbol: Node.Keyword.At?,
        val target: Target?,
        val colon: Node.Keyword.Colon?,
        val lBracket: Node.Keyword.LBracket?,
        val annotations: List<Annotation>,
        val rBracket: Node.Keyword.RBracket?,
    ) : Modifier() {

        data class Target(override val token: Token) : Node(), TokenContainer<Target.Token> {
            companion object {
                private val mapStringToToken = Token.values().associateBy { it.string }
                fun of(value: String): Target = mapStringToToken[value]?.let(::Target)
                    ?: error("Unknown value: $value")
            }

            enum class Token : HasSimpleStringRepresentation {
                FIELD, FILE, PROPERTY, GET, SET, RECEIVER, PARAM, SETPARAM, DELEGATE;

                override val string: String
                    get() = name.lowercase()
            }
        }

        /**
         * AST node corresponds to KtAnnotationEntry under KtAnnotation or virtual AST node corresponds to KtAnnotationEntry not under KtAnnotation.
         */
        data class Annotation(
            val type: SimpleType,
            val args: ValueArgs?
        ) : Node()
    }

    data class KeywordModifier(override val token: Token) : Modifier(), TokenContainer<KeywordModifier.Token> {
        companion object {
            private val mapStringToToken = Token.values().associateBy { it.string }
            fun of(value: String): KeywordModifier =
                mapStringToToken[value]?.let(Node::KeywordModifier) ?: error("Unknown value: $value")
        }

        enum class Token : HasSimpleStringRepresentation {
            ABSTRACT, FINAL, OPEN, ANNOTATION, SEALED, DATA, OVERRIDE, LATEINIT, INNER, ENUM, COMPANION, VALUE,
            PRIVATE, PROTECTED, PUBLIC, INTERNAL,
            IN, OUT, NOINLINE, CROSSINLINE, VARARG, REIFIED,
            TAILREC, OPERATOR, INFIX, INLINE, EXTERNAL, SUSPEND, CONST, FUN,
            ACTUAL, EXPECT;

            override val string: String
                get() = name.lowercase()
        }
    }

    sealed class PostModifier : Node()

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
            override val annotationSets: List<AnnotationSetModifier>,
            val name: NameExpression,
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

    sealed class Keyword(override val string: String) : Node(), HasSimpleStringRepresentation {
        override fun toString(): String {
            return javaClass.simpleName
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Keyword

            if (string != other.string) return false

            return true
        }

        override fun hashCode(): Int {
            return string.hashCode()
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
        class When : Keyword("when")
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
        class Colon : Keyword(":")
    }

    sealed class Extra : Node() {
        abstract val text: String
    }

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