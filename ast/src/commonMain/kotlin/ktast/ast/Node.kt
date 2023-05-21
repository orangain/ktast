package ktast.ast

/**
 * Common interface of all AST nodes.
 */
sealed interface Node {
    var tag: Any?

    abstract class NodeList<out E : Node>(
        val prefix: String = "",
        val suffix: String = "",
    ) : Node {
        abstract val elements: List<E>
    }

    abstract class CommaSeparatedNodeList<out E : Node>(
        prefix: String,
        suffix: String,
    ) : NodeList<E>(prefix, suffix) {
        abstract val trailingComma: Keyword.Comma?
    }

    interface WithAnnotationSets {
        val annotationSets: List<AnnotationSet>
    }

    interface WithModifiers : WithAnnotationSets {
        val modifiers: Modifiers?
        override val annotationSets: List<AnnotationSet>
            get() = modifiers?.elements.orEmpty().mapNotNull { it as? AnnotationSet }
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

    /**
     * AST node corresponds to KtFile.
     */
    data class KotlinFile(
        override val annotationSets: List<AnnotationSet>,
        override val packageDirective: PackageDirective?,
        override val importDirectives: ImportDirectives?,
        override val declarations: List<Declaration>,
        override var tag: Any? = null,
    ) : Node, KotlinEntry, DeclarationsContainer

    data class KotlinScript(
        override val annotationSets: List<AnnotationSet>,
        override val packageDirective: PackageDirective?,
        override val importDirectives: ImportDirectives?,
        val expressions: List<Expression>,
        override var tag: Any? = null,
    ) : Node, KotlinEntry

    /**
     * AST node corresponds to KtPackageDirective.
     */
    data class PackageDirective(
        override val modifiers: Modifiers?,
        val packageKeyword: Keyword.Package,
        val names: List<NameExpression>,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * AST node corresponds to KtImportList.
     */
    data class ImportDirectives(
        override val elements: List<ImportDirective>,
        override var tag: Any? = null,
    ) : NodeList<ImportDirective>()

    /**
     * AST node corresponds to KtImportDirective.
     */
    data class ImportDirective(
        val importKeyword: Keyword.Import,
        val names: List<NameExpression>,
        val importAlias: ImportAlias?,
        override var tag: Any? = null,
    ) : Node {

        /**
         * AST node corresponds to KtImportAlias.
         */
        data class ImportAlias(
            val name: NameExpression,
            override var tag: Any? = null,
        ) : Node
    }

    /**
     * Base class of [Declaration] and [Expression].
     */
    sealed class Statement : Node

    sealed class Declaration : Statement()

    /**
     * AST node corresponds to KtClassOrObject.
     */
    data class ClassDeclaration(
        override val modifiers: Modifiers?,
        val classDeclarationKeyword: ClassDeclarationKeyword,
        val name: NameExpression?,
        val typeParams: TypeParams?,
        val primaryConstructor: PrimaryConstructor?,
        val classParents: ClassParents?,
        val typeConstraintSet: TypeConstraintSet?,
        val classBody: ClassBody?,
        override var tag: Any? = null,
    ) : Declaration(), WithModifiers {
        val isClass = classDeclarationKeyword is Keyword.Class
        val isObject = classDeclarationKeyword is Keyword.Object
        val isInterface = classDeclarationKeyword is Keyword.Interface
        val isCompanion = modifiers?.elements.orEmpty().any { it is Keyword.Companion }
        val isEnum = modifiers?.elements.orEmpty().any { it is Keyword.Enum }

        sealed interface ClassDeclarationKeyword : Keyword

        /**
         * AST node corresponds to KtSuperTypeList.
         */
        data class ClassParents(
            override val elements: List<ClassParent>,
            override var tag: Any? = null,
        ) : CommaSeparatedNodeList<ClassParent>("", "") {
            override val trailingComma: Keyword.Comma? = null
        }

        /**
         * AST node corresponds to KtSuperTypeListEntry.
         */
        sealed class ClassParent : Node {
            /**
             * AST node corresponds to KtSuperTypeCallEntry.
             */
            data class CallConstructor(
                val type: SimpleType,
                val typeArgs: TypeArgs?,
                val args: ValueArgs?,
                val lambda: CallExpression.LambdaArg?,
                override var tag: Any? = null,
            ) : ClassParent()

            /**
             * AST node corresponds to KtDelegatedSuperTypeEntry.
             */
            data class DelegatedType(
                val type: SimpleType,
                val byKeyword: Keyword.By,
                val expression: Expression,
                override var tag: Any? = null,
            ) : ClassParent()

            /**
             * AST node corresponds to KtSuperTypeEntry.
             */
            data class Type(
                val type: SimpleType,
                override var tag: Any? = null,
            ) : ClassParent()
        }

        /**
         * AST node corresponds to KtPrimaryConstructor.
         */
        data class PrimaryConstructor(
            override val modifiers: Modifiers?,
            val constructorKeyword: Keyword.Constructor?,
            val params: FunctionParams?,
            override var tag: Any? = null,
        ) : Node, WithModifiers

        /**
         * AST node corresponds to KtClassBody.
         */
        data class ClassBody(
            val enumEntries: List<EnumEntry>,
            val hasTrailingCommaInEnumEntries: Boolean,
            override val declarations: List<Declaration>,
            override var tag: Any? = null,
        ) : Node, DeclarationsContainer {

            /**
             * AST node corresponds to KtEnumEntry.
             */
            data class EnumEntry(
                override val modifiers: Modifiers?,
                val name: NameExpression,
                val args: ValueArgs?,
                val classBody: ClassBody?,
                override var tag: Any? = null,
            ) : Node, WithModifiers
        }
    }

    /**
     * AST node corresponds to KtAnonymousInitializer.
     */
    data class InitDeclaration(
        override val modifiers: Modifiers?,
        val block: BlockExpression,
        override var tag: Any? = null,
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
        val params: FunctionParams?,
        val typeRef: TypeRef?,
        override val postModifiers: List<PostModifier>,
        override val equals: Keyword.Equal?,
        override val body: Expression?,
        override var tag: Any? = null,
    ) : Declaration(), WithModifiers, WithPostModifiers, WithFunctionBody {

    }

    /**
     * AST node corresponds to KtParameterList under KtNamedFunction.
     */
    data class FunctionParams(
        override val elements: List<FunctionParam>,
        override val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : CommaSeparatedNodeList<FunctionParam>("(", ")")

    /**
     * AST node corresponds to KtParameter inside KtNamedFunction.
     */
    data class FunctionParam(
        override val modifiers: Modifiers?,
        val valOrVarKeyword: ValOrVarKeyword?,
        val name: NameExpression,
        // typeRef can be null for anon functions
        val typeRef: TypeRef?,
        val equals: Keyword.Equal?,
        val defaultValue: Expression?,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * AST node corresponds to KtProperty or KtDestructuringDeclaration.
     */
    data class PropertyDeclaration(
        override val modifiers: Modifiers?,
        val valOrVarKeyword: ValOrVarKeyword,
        val typeParams: TypeParams?,
        val receiverTypeRef: TypeRef?,
        val lPar: Keyword.LPar?,
        // Always at least one, more than one is destructuring
        val variables: List<Variable>,
        val trailingComma: Keyword.Comma?,
        val rPar: Keyword.RPar?,
        val typeConstraintSet: TypeConstraintSet?,
        val equals: Keyword.Equal?,
        val initializer: Expression?,
        val propertyDelegate: PropertyDelegate?,
        val accessors: List<Accessor>,
        override var tag: Any? = null,
    ) : Declaration(), WithModifiers {
        init {
            if (propertyDelegate != null) {
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

        /**
         * AST node corresponds to KtPropertyDelegate.
         */
        data class PropertyDelegate(
            val byKeyword: Keyword.By,
            val expression: Expression,
            override var tag: Any? = null,
        ) : Node

        /**
         * AST node corresponds to KtPropertyAccessor.
         */
        sealed class Accessor : Node, WithModifiers, WithPostModifiers, WithFunctionBody

        data class Getter(
            override val modifiers: Modifiers?,
            val getKeyword: Keyword.Get,
            val typeRef: TypeRef?,
            override val postModifiers: List<PostModifier>,
            override val equals: Keyword.Equal?,
            override val body: Expression?,
            override var tag: Any? = null,
        ) : Accessor()

        data class Setter(
            override val modifiers: Modifiers?,
            val setKeyword: Keyword.Set,
            val params: LambdaParams?,
            override val postModifiers: List<PostModifier>,
            override val equals: Keyword.Equal?,
            override val body: Expression?,
            override var tag: Any? = null,
        ) : Accessor()
    }

    /**
     * Virtual AST node corresponds a part of KtProperty or AST node corresponds to KtDestructuringDeclarationEntry.
     */
    data class Variable(
        val name: NameExpression,
        val typeRef: TypeRef?,
        override var tag: Any? = null,
    ) : Node

    /**
     * AST node corresponds to KtTypeAlias.
     */
    data class TypeAliasDeclaration(
        override val modifiers: Modifiers?,
        val name: NameExpression,
        val typeParams: TypeParams?,
        val typeRef: TypeRef,
        override var tag: Any? = null,
    ) : Declaration(), WithModifiers

    /**
     * AST node corresponds to KtSecondaryConstructor.
     */
    data class SecondaryConstructorDeclaration(
        override val modifiers: Modifiers?,
        val constructorKeyword: Keyword.Constructor,
        val params: FunctionParams?,
        val delegationCall: DelegationCall?,
        val block: BlockExpression?,
        override var tag: Any? = null,
    ) : Declaration(), WithModifiers {
        /**
         * AST node corresponds to KtConstructorDelegationCall.
         */
        data class DelegationCall(
            val target: DelegationTarget,
            val args: ValueArgs?,
            override var tag: Any? = null,
        ) : Node

        sealed interface DelegationTarget : Keyword
    }

    /**
     * AST node corresponds to KtTypeParameterList.
     */
    data class TypeParams(
        override val elements: List<TypeParam>,
        override val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : CommaSeparatedNodeList<TypeParam>("<", ">")

    /**
     * AST node corresponds to KtTypeParameter.
     */
    data class TypeParam(
        override val modifiers: Modifiers?,
        val name: NameExpression,
        val typeRef: TypeRef?,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    sealed class Type : Node {

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
        val functionTypeReceiver: FunctionTypeReceiver?,
        val params: FunctionTypeParams?,
        val returnTypeRef: TypeRef,
        val rPar: Keyword.RPar?,
        override var tag: Any? = null,
    ) : Type(), WithModifiers {
        /**
         * AST node corresponds to KtContextReceiverList.
         */
        data class ContextReceivers(
            override val elements: List<ContextReceiver>,
            override val trailingComma: Keyword.Comma?,
            override var tag: Any? = null,
        ) : CommaSeparatedNodeList<ContextReceiver>("(", ")")

        /**
         * AST node corresponds to KtContextReceiver.
         */
        data class ContextReceiver(
            val typeRef: TypeRef,
            override var tag: Any? = null,
        ) : Node

        /**
         * AST node corresponds KtFunctionTypeReceiver.
         */
        data class FunctionTypeReceiver(
            val typeRef: TypeRef,
            override var tag: Any? = null,
        ) : Node

        /**
         * AST node corresponds to KtParameterList under KtFunctionType.
         */
        data class FunctionTypeParams(
            override val elements: List<Param>,
            override val trailingComma: Keyword.Comma?,
            override var tag: Any? = null,
        ) : CommaSeparatedNodeList<Param>("(", ")")

        /**
         * AST node corresponds to KtParameter inside KtFunctionType.
         */
        data class Param(
            val name: NameExpression?,
            val typeRef: TypeRef,
            override var tag: Any? = null,
        ) : Node
    }

    /**
     * AST node corresponds to KtUserType.
     */
    data class SimpleType(
        val qualifiers: List<Qualifier>,
        override val name: NameExpression,
        override val typeArgs: TypeArgs?,
        override var tag: Any? = null,
    ) : Type(), Type.NameWithTypeArgs {
        data class Qualifier(
            override val name: NameExpression,
            override val typeArgs: TypeArgs?,
            override var tag: Any? = null,
        ) : Node, NameWithTypeArgs
    }

    /**
     * AST node corresponds to KtNullableType.
     */
    data class NullableType(
        val lPar: Keyword.LPar?,
        override val modifiers: Modifiers?,
        val type: Type,
        val rPar: Keyword.RPar?,
        override var tag: Any? = null,
    ) : Type(), WithModifiers

    /**
     * AST node corresponds to KtDynamicType.
     */
    data class DynamicType(
        override var tag: Any? = null,
    ) : Type()

    /**
     * AST node corresponds to KtTypeArgumentList.
     */
    data class TypeArgs(
        override val elements: List<TypeArg>,
        override val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : CommaSeparatedNodeList<TypeArg>("<", ">")

    /**
     * AST node corresponds to KtTypeProjection.
     */
    data class TypeArg(
        override val modifiers: Modifiers?,
        val typeRef: TypeRef?,
        val asterisk: Boolean,
        override var tag: Any? = null,
    ) : Node, WithModifiers {
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
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * AST node corresponds to KtValueArgumentList or KtInitializerList.
     */
    data class ValueArgs(
        override val elements: List<ValueArg>,
        override val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : CommaSeparatedNodeList<ValueArg>("(", ")")

    /**
     * AST node corresponds to KtValueArgument.
     */
    data class ValueArg(
        val name: NameExpression?,
        val asterisk: Boolean, // Array spread operator
        val expression: Expression,
        override var tag: Any? = null,
    ) : Node

    /**
     * AST node corresponds to KtContainerNode.
     */
    data class ExpressionContainer(
        val expression: Expression,
        override var tag: Any? = null,
    ) : Node

    sealed class Expression : Statement()

    /**
     * AST node corresponds to KtIfExpression.
     */
    data class IfExpression(
        val ifKeyword: Keyword.If,
        val condition: Expression,
        val body: ExpressionContainer,
        val elseBody: ExpressionContainer?,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtTryExpression.
     */
    data class TryExpression(
        val block: BlockExpression,
        val catchClauses: List<CatchClause>,
        val finallyBlock: BlockExpression?,
        override var tag: Any? = null,
    ) : Expression() {
        /**
         * AST node corresponds to KtCatchClause.
         */
        data class CatchClause(
            val catchKeyword: Keyword.Catch,
            val params: FunctionParams,
            val block: BlockExpression,
            override var tag: Any? = null,
        ) : Node
    }

    /**
     * AST node corresponds to KtForExpression.
     */
    data class ForExpression(
        val forKeyword: Keyword.For,
        val loopParam: LambdaParam,
        val loopRange: ExpressionContainer,
        val body: ExpressionContainer,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtWhileExpressionBase.
     */
    data class WhileExpression(
        val whileKeyword: Keyword.While,
        val condition: ExpressionContainer,
        val body: ExpressionContainer,
        val doWhile: Boolean,
        override var tag: Any? = null,
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
        val operator: BinaryOperator,
        override val rhs: Expression,
        override var tag: Any? = null,
    ) : BaseBinaryExpression() {
        sealed interface BinaryOperator : Keyword
    }

    data class BinaryInfixExpression(
        override val lhs: Expression,
        val operator: NameExpression,
        override val rhs: Expression,
        override var tag: Any? = null,
    ) : BaseBinaryExpression()

    /**
     * AST node corresponds to KtUnaryExpression.
     */
    data class UnaryExpression(
        val expression: Expression,
        val operator: UnaryOperator,
        val prefix: Boolean,
        override var tag: Any? = null,
    ) : Expression() {
        sealed interface UnaryOperator : Keyword
    }

    /**
     * AST node corresponds to KtBinaryExpressionWithTypeRHS or KtIsExpression.
     */
    data class BinaryTypeExpression(
        val lhs: Expression,
        val operator: BinaryTypeOperator,
        val rhs: TypeRef,
        override var tag: Any? = null,
    ) : Expression() {
        sealed interface BinaryTypeOperator : Keyword
    }

    /**
     * AST node corresponds to KtDoubleColonExpression.
     */
    sealed class DoubleColonExpression : Expression() {
        abstract val lhs: Receiver?

        sealed class Receiver : Node {
            data class Expression(
                val expression: Node.Expression,
                override var tag: Any? = null,
            ) : Receiver()

            data class Type(
                val type: SimpleType,
                val questionMarks: List<Keyword.Question>,
                override var tag: Any? = null,
            ) : Receiver()
        }
    }

    /**
     * AST node corresponds to KtCallableReferenceExpression.
     */
    data class CallableReferenceExpression(
        override val lhs: Receiver?,
        val rhs: NameExpression,
        override var tag: Any? = null,
    ) : DoubleColonExpression()

    /**
     * AST node corresponds to KtClassLiteralExpression.
     */
    data class ClassLiteralExpression(
        // Class literal expression without lhs is not supported in Kotlin syntax, but Kotlin compiler does parse it.
        override val lhs: Receiver?,
        override var tag: Any? = null,
    ) : DoubleColonExpression()

    /**
     * AST node corresponds to KtParenthesizedExpression.
     */
    data class ParenthesizedExpression(
        val expression: Expression,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtStringTemplateExpression.
     */
    data class StringLiteralExpression(
        val entries: List<StringEntry>,
        val raw: Boolean,
        override var tag: Any? = null,
    ) : Expression() {
        /**
         * AST node corresponds to KtStringTemplateEntry.
         */
        sealed class StringEntry : Node

        /**
         * AST node corresponds to KtLiteralStringTemplateEntry.
         */
        data class LiteralStringEntry(
            val str: String,
            override var tag: Any? = null,
        ) : StringEntry()

        /**
         * AST node corresponds to KtEscapeStringTemplateEntry.
         */
        data class EscapeStringEntry(
            val str: String,
            override var tag: Any? = null,
        ) : StringEntry() {
            init {
                require(str.startsWith('\\')) {
                    "Escape string template entry must start with backslash."
                }
            }
        }

        /**
         * AST node corresponds to KtStringTemplateEntryWithExpression.
         */
        data class TemplateStringEntry(
            val expression: Expression,
            val short: Boolean,
            override var tag: Any? = null,
        ) : StringEntry() {
            init {
                require(!short || expression is NameExpression) {
                    "Short template string entry must be a name expression."
                }
            }
        }
    }

    /**
     * AST node corresponds to KtConstantExpression.
     */
    data class ConstantLiteralExpression(
        val value: String,
        val form: Form,
        override var tag: Any? = null,
    ) : Expression() {
        enum class Form { BOOLEAN, CHAR, INT, FLOAT, NULL }
    }

    /**
     * AST node corresponds to KtLambdaExpression.
     */
    data class LambdaExpression(
        val params: LambdaParams?,
        val lambdaBody: LambdaBody?,
        override var tag: Any? = null,
    ) : Expression() {

        /**
         * AST node corresponds to KtBlockExpression in lambda body.
         * In lambda expression, left and right braces are not included in [LambdaExpression.LambdaBody], but are included in Lambda.
         * This means:
         *
         * <Lambda> = { <Param>, <Param> -> <Body> }
         */
        data class LambdaBody(
            override val statements: List<Statement>,
            override var tag: Any? = null,
        ) : Expression(), StatementsContainer
    }

    /**
     * AST node corresponds to KtParameterList under KtLambdaExpression.
     */
    data class LambdaParams(
        override val elements: List<LambdaParam>,
        override val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : CommaSeparatedNodeList<LambdaParam>("", "")

    /**
     * AST node corresponds to KtParameter under KtLambdaExpression.
     */
    data class LambdaParam(
        val lPar: Keyword.LPar?,
        val variables: List<Variable>,
        val trailingComma: Keyword.Comma?,
        val rPar: Keyword.RPar?,
        val colon: Keyword.Colon?,
        val destructTypeRef: TypeRef?,
        override var tag: Any? = null,
    ) : Node {
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
            override var tag: Any? = null,
        ) : Node, WithModifiers
    }

    /**
     * AST node corresponds to KtThisExpression.
     */
    data class ThisExpression(
        val label: String?,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtSuperExpression.
     */
    data class SuperExpression(
        val typeArg: TypeRef?,
        val label: String?,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtWhenExpression.
     */
    data class WhenExpression(
        val whenKeyword: Keyword.When,
        val lPar: Keyword.LPar?,
        val expression: Expression?,
        val rPar: Keyword.RPar?,
        val whenBranches: List<WhenBranch>,
        override var tag: Any? = null,
    ) : Expression() {
        /**
         * AST node corresponds to KtWhenEntry.
         */
        data class WhenBranch(
            val whenConditions: List<WhenCondition>,
            val trailingComma: Keyword.Comma?,
            val elseKeyword: Keyword.Else?,
            val body: Expression,
            override var tag: Any? = null,
        ) : Node {
            init {
                when {
                    whenConditions.isNotEmpty() -> require(elseKeyword == null) { "elseKeyword must be null when whenConditions is not empty" }
                    else -> require(trailingComma == null && elseKeyword != null) { "trailingComma must be null and elseKeyword must not be null when whenConditions is empty" }
                }
            }
        }

        sealed interface WhenConditionOperator : Keyword
        sealed interface WhenConditionTypeOperator : WhenConditionOperator
        sealed interface WhenConditionRangeOperator : WhenConditionOperator

        /**
         * AST node corresponds to KtWhenCondition.
         */
        data class WhenCondition(
            val operator: WhenConditionOperator?,
            val expression: Expression?,
            val typeRef: TypeRef?,
            override var tag: Any? = null,
        ) : Node {
            init {
                when (operator) {
                    null -> require(expression != null || typeRef == null) { "expression must not be null and typeRef must be null when operator is null" }
                    is WhenConditionTypeOperator -> require(expression == null && typeRef != null) { "expression must be null and typeRef must not be null when operator is type operator" }
                    is WhenConditionRangeOperator -> require(expression != null && typeRef == null) { "expression must not be null and typeRef must be null when operator is range operator" }
                }
            }
        }
    }

    /**
     * AST node corresponds to KtObjectLiteralExpression.
     */
    data class ObjectLiteralExpression(
        val declaration: ClassDeclaration,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtThrowExpression.
     */
    data class ThrowExpression(
        val expression: Expression,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtReturnExpression.
     */
    data class ReturnExpression(
        val label: String?,
        val expression: Expression?,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtContinueExpression.
     */
    data class ContinueExpression(
        val label: String?,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtBreakExpression.
     */
    data class BreakExpression(
        val label: String?,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtCollectionLiteralExpression.
     */
    data class CollectionLiteralExpression(
        val expressions: List<Expression>,
        val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtValueArgumentName, KtSimpleNameExpression or PsiElement whose elementType is IDENTIFIER.
     */
    data class NameExpression(
        val name: String,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtLabeledExpression.
     */
    data class LabeledExpression(
        val label: String,
        val expression: Expression,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtAnnotatedExpression.
     */
    data class AnnotatedExpression(
        override val annotationSets: List<AnnotationSet>,
        val expression: Expression,
        override var tag: Any? = null,
    ) : Expression(), WithAnnotationSets

    /**
     * AST node corresponds to KtCallExpression.
     */
    data class CallExpression(
        val expression: Expression,
        val typeArgs: TypeArgs?,
        val args: ValueArgs?,
        val lambdaArg: LambdaArg?,
        override var tag: Any? = null,
    ) : Expression() {
        /**
         * AST node corresponds to KtLambdaArgument.
         */
        data class LambdaArg(
            override val annotationSets: List<AnnotationSet>,
            val label: String?,
            val expression: LambdaExpression,
            override var tag: Any? = null,
        ) : Node, WithAnnotationSets
    }

    /**
     * AST node corresponds to KtArrayAccessExpression.
     */
    data class ArrayAccessExpression(
        val expression: Expression,
        val indices: List<Expression>,
        val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * Virtual AST node corresponds to KtNamedFunction in expression context.
     */
    data class AnonymousFunctionExpression(
        val function: FunctionDeclaration,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtProperty or KtDestructuringDeclaration.
     * This is only present for when expressions and labeled expressions.
     */
    data class PropertyExpression(
        val declaration: PropertyDeclaration,
        override var tag: Any? = null,
    ) : Expression()

    /**
     * AST node corresponds to KtBlockExpression.
     */
    data class BlockExpression(
        override val statements: List<Statement>,
        override var tag: Any? = null,
    ) : Expression(), StatementsContainer

    /**
     * AST node corresponds to KtModifierList.
     */
    data class Modifiers(
        override val elements: List<Modifier>,
        override var tag: Any? = null,
    ) : NodeList<Modifier>()

    sealed interface Modifier : Node

    /**
     * AST node corresponds to KtAnnotation or KtAnnotationEntry not under KtAnnotation.
     */
    data class AnnotationSet(
        val atSymbol: Keyword.At?,
        val target: AnnotationTarget?,
        val colon: Keyword.Colon?,
        val lBracket: Keyword.LBracket?,
        val annotations: List<Annotation>,
        val rBracket: Keyword.RBracket?,
        override var tag: Any? = null,
    ) : Modifier {
        sealed interface AnnotationTarget : Keyword

        /**
         * AST node corresponds to KtAnnotationEntry under KtAnnotation or virtual AST node corresponds to KtAnnotationEntry not under KtAnnotation.
         */
        data class Annotation(
            val type: SimpleType,
            val args: ValueArgs?,
            override var tag: Any? = null,
        ) : Node
    }

    sealed interface KeywordModifier : Modifier, Keyword

    sealed class PostModifier : Node

    /**
     * Virtual AST node corresponds to a pair of "where" keyword and KtTypeConstraintList.
     */
    data class TypeConstraintSet(
        val whereKeyword: Keyword.Where,
        val constraints: TypeConstraints,
        override var tag: Any? = null,
    ) : PostModifier() {
        /**
         * AST node corresponds to KtTypeConstraintList.
         */
        data class TypeConstraints(
            override val elements: List<TypeConstraint>,
            override var tag: Any? = null,
        ) : CommaSeparatedNodeList<TypeConstraint>("", "") {
            override val trailingComma: Keyword.Comma? = null // Trailing comma is not allowed.
        }

        /**
         * AST node corresponds to KtTypeConstraint.
         */
        data class TypeConstraint(
            override val annotationSets: List<AnnotationSet>,
            val name: NameExpression,
            val typeRef: TypeRef,
            override var tag: Any? = null,
        ) : Node, WithAnnotationSets
    }

    /**
     * Virtual AST node corresponds to a pair of "contract" keyword and KtContractEffectList.
     */
    data class Contract(
        val contractKeyword: Keyword.Contract,
        val contractEffects: ContractEffects,
        override var tag: Any? = null,
    ) : PostModifier() {
        /**
         * AST node corresponds to KtContractEffectList.
         */
        data class ContractEffects(
            override val elements: List<ContractEffect>,
            override val trailingComma: Keyword.Comma?,
            override var tag: Any? = null,
        ) : CommaSeparatedNodeList<ContractEffect>("[", "]")

        /**
         * AST node corresponds to KtContractEffect.
         */
        data class ContractEffect(
            val expression: Expression,
            override var tag: Any? = null,
        ) : Node
    }

    sealed interface ValOrVarKeyword : Keyword

    sealed interface Keyword : Node {
        val string: String

        data class Package(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "package"
        }

        data class Import(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "import"
        }

        data class Class(override var tag: Any? = null) : Keyword, ClassDeclaration.ClassDeclarationKeyword {
            override val string: String; get() = "class"
        }

        data class Object(override var tag: Any? = null) : Keyword, ClassDeclaration.ClassDeclarationKeyword {
            override val string: String; get() = "object"
        }

        data class Interface(override var tag: Any? = null) : Keyword, ClassDeclaration.ClassDeclarationKeyword {
            override val string: String; get() = "interface"
        }

        data class Constructor(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "constructor"
        }

        data class Val(override var tag: Any? = null) : Keyword, ValOrVarKeyword {
            override val string: String; get() = "val"
        }

        data class Var(override var tag: Any? = null) : Keyword, ValOrVarKeyword {
            override val string: String; get() = "var"
        }

        data class This(override var tag: Any? = null) : Keyword, SecondaryConstructorDeclaration.DelegationTarget {
            override val string: String; get() = "this"
        }

        data class Super(override var tag: Any? = null) : Keyword, SecondaryConstructorDeclaration.DelegationTarget {
            override val string: String; get() = "super"
        }

        data class For(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "for"
        }

        data class While(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "while"
        }

        data class If(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "if"
        }

        data class Else(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "else"
        }

        data class Catch(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "catch"
        }

        data class When(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "when"
        }

        data class By(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "by"
        }

        data class Contract(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "contract"
        }

        data class Where(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "where"
        }

        data class Field(override var tag: Any? = null) : Keyword, AnnotationSet.AnnotationTarget {
            override val string: String; get() = "field"
        }

        data class File(override var tag: Any? = null) : Keyword, AnnotationSet.AnnotationTarget {
            override val string: String; get() = "file"
        }

        data class Property(override var tag: Any? = null) : Keyword, AnnotationSet.AnnotationTarget {
            override val string: String; get() = "property"
        }

        data class Get(override var tag: Any? = null) : Keyword, AnnotationSet.AnnotationTarget {
            override val string: String; get() = "get"
        }

        data class Set(override var tag: Any? = null) : Keyword, AnnotationSet.AnnotationTarget {
            override val string: String; get() = "set"
        }

        data class Receiver(override var tag: Any? = null) : Keyword, AnnotationSet.AnnotationTarget {
            override val string: String; get() = "receiver"
        }

        data class Param(override var tag: Any? = null) : Keyword, AnnotationSet.AnnotationTarget {
            override val string: String; get() = "param"
        }

        data class SetParam(override var tag: Any? = null) : Keyword, AnnotationSet.AnnotationTarget {
            override val string: String; get() = "setparam"
        }

        data class Delegate(override var tag: Any? = null) : Keyword, AnnotationSet.AnnotationTarget {
            override val string: String; get() = "delegate"
        }

        data class Equal(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "="
        }

        data class Comma(override var tag: Any? = null) : Keyword {
            override val string: String; get() = ","
        }

        data class LPar(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "("
        }

        data class RPar(override var tag: Any? = null) : Keyword {
            override val string: String; get() = ")"
        }

        data class LBracket(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "["
        }

        data class RBracket(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "]"
        }

        data class At(override var tag: Any? = null) : Keyword {
            override val string: String; get() = "@"
        }

        data class Asterisk(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "*"
        }

        data class Slash(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "/"
        }

        data class Percent(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "%"
        }

        data class Plus(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator,
            UnaryExpression.UnaryOperator {
            override val string: String; get() = "+"
        }

        data class Minus(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator,
            UnaryExpression.UnaryOperator {
            override val string: String; get() = "-"
        }

        data class In(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator, KeywordModifier,
            WhenExpression.WhenConditionRangeOperator {
            override val string: String; get() = "in"
        }

        data class NotIn(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator,
            WhenExpression.WhenConditionRangeOperator {
            override val string: String; get() = "!in"
        }

        data class Greater(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = ">"
        }

        data class GreaterEqual(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = ">="
        }

        data class Less(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "<"
        }

        data class LessEqual(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "<="
        }

        data class EqualEqual(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "=="
        }

        data class NotEqual(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "!="
        }

        data class AsteriskEqual(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "*="
        }

        data class SlashEqual(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "/="
        }

        data class PercentEqual(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "%="
        }

        data class PlusEqual(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "+="
        }

        data class MinusEqual(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "-="
        }

        data class OrOr(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "||"
        }

        data class AndAnd(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "&&"
        }

        data class QuestionColon(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "?:"
        }

        data class DotDot(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = ".."
        }

        data class DotDotLess(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "..<"
        }

        data class Dot(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "."
        }

        data class QuestionDot(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "?."
        }

        data class Question(override var tag: Any? = null) : Keyword, BinaryExpression.BinaryOperator {
            override val string: String; get() = "?"
        }

        data class PlusPlus(override var tag: Any? = null) : Keyword, UnaryExpression.UnaryOperator {
            override val string: String; get() = "++"
        }

        data class MinusMinus(override var tag: Any? = null) : Keyword, UnaryExpression.UnaryOperator {
            override val string: String; get() = "--"
        }

        data class Not(override var tag: Any? = null) : Keyword, UnaryExpression.UnaryOperator {
            override val string: String; get() = "!"
        }

        data class NotNot(override var tag: Any? = null) : Keyword, UnaryExpression.UnaryOperator {
            override val string: String; get() = "!!"
        }

        data class As(override var tag: Any? = null) : Keyword, BinaryTypeExpression.BinaryTypeOperator {
            override val string: String; get() = "as"
        }

        data class AsQuestion(override var tag: Any? = null) : Keyword, BinaryTypeExpression.BinaryTypeOperator {
            override val string: String; get() = "as?"
        }

        data class Colon(override var tag: Any? = null) : Keyword, BinaryTypeExpression.BinaryTypeOperator {
            override val string: String; get() = ":"
        }

        data class Is(override var tag: Any? = null) : Keyword, BinaryTypeExpression.BinaryTypeOperator,
            WhenExpression.WhenConditionTypeOperator {
            override val string: String; get() = "is"
        }

        data class NotIs(override var tag: Any? = null) : Keyword, BinaryTypeExpression.BinaryTypeOperator,
            WhenExpression.WhenConditionTypeOperator {
            override val string: String; get() = "!is"
        }

        data class Abstract(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "abstract"
        }

        data class Final(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "final"
        }

        data class Open(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "open"
        }

        data class Annotation(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "annotation"
        }

        data class Sealed(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "sealed"
        }

        data class Data(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "data"
        }

        data class Override(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "override"
        }

        data class LateInit(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "lateinit"
        }

        data class Inner(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "inner"
        }

        data class Enum(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "enum"
        }

        data class Companion(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "companion"
        }

        data class Value(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "value"
        }

        data class Private(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "private"
        }

        data class Protected(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "protected"
        }

        data class Public(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "public"
        }

        data class Internal(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "internal"
        }

        data class Out(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "out"
        }

        data class Noinline(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "noinline"
        }

        data class CrossInline(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "crossinline"
        }

        data class Vararg(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "vararg"
        }

        data class Reified(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "reified"
        }

        data class TailRec(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "tailrec"
        }

        data class Operator(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "operator"
        }

        data class Infix(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "infix"
        }

        data class Inline(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "inline"
        }

        data class External(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "external"
        }

        data class Suspend(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "suspend"
        }

        data class Const(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "const"
        }

        data class Fun(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "fun"
        }

        data class Actual(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "actual"
        }

        data class Expect(override var tag: Any? = null) : Keyword, KeywordModifier {
            override val string: String; get() = "expect"
        }
    }

    sealed class Extra : Node {
        abstract val text: String
    }

    /**
     * AST node corresponds to PsiWhiteSpace.
     */
    data class Whitespace(
        override val text: String,
        override var tag: Any? = null,
    ) : Extra()

    /**
     * AST node corresponds to PsiComment.
     */
    data class Comment(
        override val text: String,
        override var tag: Any? = null,
    ) : Extra()

    /**
     * AST node corresponds to PsiElement whose elementType is SEMICOLON.
     */
    data class Semicolon(
        override val text: String,
        override var tag: Any? = null,
    ) : Extra()
}