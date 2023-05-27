package ktast.ast

/**
 * Common interface for all the AST nodes.
 */
sealed interface Node {
    /**
     * Property to store any extra data by the user.
     */
    var tag: Any?

    /**
     * Base class of all nodes that represent a list of nodes.
     *
     * @param E type of elements in the list
     * @property prefix prefix of the list when converted to source code, e.g. `(`.
     * @property suffix suffix of the list when converted to source code, e.g. `)`.
     * @property elements list of elements in the list.
     */
    abstract class NodeList<out E : Node>(
        val prefix: String = "",
        val suffix: String = "",
    ) : Node {
        abstract val elements: List<E>
    }

    /**
     * Specialization of [NodeList] for comma-separated lists.
     *
     * @property trailingComma trailing comma node of the list if exists.
     */
    abstract class CommaSeparatedNodeList<out E : Node>(
        prefix: String,
        suffix: String,
    ) : NodeList<E>(prefix, suffix) {
        abstract val trailingComma: Keyword.Comma?
    }

    /**
     * Common interface for AST nodes that have a simple text representation.
     *
     * @property text text representation of the node.
     */
    sealed interface SimpleTextNode : Node {
        val text: String
    }

    /**
     * Common interface for AST nodes that have annotations.
     *
     * @property annotationSets list of annotation sets.
     */
    interface WithAnnotationSets {
        val annotationSets: List<Modifier.AnnotationSet>
    }

    /**
     * Common interface for AST nodes that have modifiers.
     *
     * @property modifiers list of modifiers.
     */
    interface WithModifiers : WithAnnotationSets {
        val modifiers: Modifiers?
        override val annotationSets: List<Modifier.AnnotationSet>
            get() = modifiers?.elements.orEmpty().mapNotNull { it as? Modifier.AnnotationSet }
    }

    /**
     * Common interface for AST nodes that have package directives and import directives.
     *
     * @property packageDirective package directive if exists, otherwise `null`.
     * @property importDirectives import directives if exist, otherwise `null`.
     */
    interface KotlinEntry : WithAnnotationSets {
        val packageDirective: PackageDirective?
        val importDirectives: ImportDirectives?
    }

    /**
     * Common interface for AST nodes that have post-modifiers.
     *
     * @property postModifiers list of post-modifiers.
     */
    interface WithPostModifiers {
        val postModifiers: List<PostModifier>
    }

    /**
     * Common interface for AST nodes that have a function body.
     *
     * @property equals equal sign if exists, otherwise `null`.
     * @property body function body if exists, otherwise `null`.
     */
    interface WithFunctionBody {
        val equals: Keyword.Equal?
        val body: Expression?
    }

    /**
     * Common interface for AST nodes that have statements.
     *
     * @property statements list of statements.
     */
    interface StatementsContainer {
        val statements: List<Statement>
    }

    /**
     * Common interface for AST nodes that have declarations.
     *
     * @property declarations list of declarations.
     */
    interface DeclarationsContainer {
        val declarations: List<Declaration>
    }

    /**
     * AST node corresponds to KtFile.
     *
     * @property annotationSets list of annotation sets.
     * @property packageDirective package directive if exists, otherwise `null`.
     * @property importDirectives import directives if exist, otherwise `null`.
     * @property declarations list of declarations.
     */
    data class KotlinFile(
        override val annotationSets: List<Modifier.AnnotationSet>,
        override val packageDirective: PackageDirective?,
        override val importDirectives: ImportDirectives?,
        override val declarations: List<Declaration>,
        override var tag: Any? = null,
    ) : Node, KotlinEntry, DeclarationsContainer

    /**
     * @property annotationSets list of annotation sets.
     * @property packageDirective package directive if exists, otherwise `null`.
     * @property importDirectives import directives if exist, otherwise `null`.
     * @property expressions list of expressions.
     */
    data class KotlinScript(
        override val annotationSets: List<Modifier.AnnotationSet>,
        override val packageDirective: PackageDirective?,
        override val importDirectives: ImportDirectives?,
        val expressions: List<Expression>,
        override var tag: Any? = null,
    ) : Node, KotlinEntry

    /**
     * AST node corresponds to KtPackageDirective.
     *
     * @property modifiers modifiers if exists, otherwise `null`.
     * @property packageKeyword package keyword.
     * @property names list of names separated by dots.
     */
    data class PackageDirective(
        override val modifiers: Modifiers?,
        val packageKeyword: Keyword.Package,
        val names: List<Expression.NameExpression>,
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
     *
     * @property importKeyword import keyword.
     * @property names list of names separated by dots.
     * @property importAlias import alias if exists, otherwise `null`.
     */
    data class ImportDirective(
        val importKeyword: Keyword.Import,
        val names: List<Expression.NameExpression>,
        val importAlias: ImportAlias?,
        override var tag: Any? = null,
    ) : Node {

        /**
         * AST node corresponds to KtImportAlias.
         *
         * @property name name of the alias.
         */
        data class ImportAlias(
            val name: Expression.NameExpression,
            override var tag: Any? = null,
        ) : Node
    }

    /**
     * Common interface for [Declaration] and [Expression].
     */
    sealed interface Statement : Node

    /**
     * Common interface for AST nodes that are main contents of a Kotlin file or a class body.
     */
    sealed interface Declaration : Statement {
        /**
         * AST node that represents a class, object or interface. The node corresponds to KtClassOrObject.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property classDeclarationKeyword class declaration keyword.
         * @property name name of the class. If the object is anonymous, the name is `null`.
         * @property typeParams type parameters if exist, otherwise `null`.
         * @property primaryConstructor primary constructor if exists, otherwise `null`.
         * @property classParents class parents if exist, otherwise `null`.
         * @property typeConstraintSet type constraint set if exists, otherwise `null`.
         * @property classBody class body if exists, otherwise `null`.
         */
        data class ClassDeclaration(
            override val modifiers: Modifiers?,
            val classDeclarationKeyword: ClassDeclarationKeyword,
            val name: Expression.NameExpression?,
            val typeParams: TypeParams?,
            val primaryConstructor: PrimaryConstructor?,
            val classParents: ClassParents?,
            val typeConstraintSet: PostModifier.TypeConstraintSet?,
            val classBody: ClassBody?,
            override var tag: Any? = null,
        ) : Declaration, WithModifiers {
            /**
             * Returns `true` if the node is a class, `false` otherwise.
             */
            val isClass = classDeclarationKeyword is Keyword.Class

            /**
             * Returns `true` if the node is an object, `false` otherwise.
             */
            val isObject = classDeclarationKeyword is Keyword.Object

            /**
             * Returns `true` if the node is an interface, `false` otherwise.
             */
            val isInterface = classDeclarationKeyword is Keyword.Interface

            /**
             * Returns `true` if the node has a companion modifier, `false` otherwise.
             */
            val isCompanion = modifiers?.elements.orEmpty().any { it is Keyword.Companion }

            /**
             * Returns `true` if the node has an enum modifier, `false` otherwise.
             */
            val isEnum = modifiers?.elements.orEmpty().any { it is Keyword.Enum }

            /**
             * Common interface for keyword nodes that are used to declare a class.
             */
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
             * AST node that represents a parent of the class. The node corresponds to KtSuperTypeListEntry.
             */
            sealed interface ClassParent : Node

            /**
             * ClassParent node that represents constructor invocation. The node corresponds to KtSuperTypeCallEntry.
             *
             * @property type type of the parent.
             * @property args value arguments of the parent call if exists, otherwise `null`.
             */
            data class ConstructorClassParent(
                val type: Type.SimpleType,
                val args: ValueArgs?,
                override var tag: Any? = null,
            ) : ClassParent

            /**
             * ClassParent node that represents explicit delegation. The node corresponds to KtDelegatedSuperTypeEntry.
             *
             * @property type type of the interface delegated to.
             * @property byKeyword `by` keyword.
             * @property expression expression of the delegation.
             */
            data class DelegationClassParent(
                val type: Type,
                val byKeyword: Keyword.By,
                val expression: Expression,
                override var tag: Any? = null,
            ) : ClassParent

            /**
             * ClassParent node that represents just a type. The node corresponds to KtSuperTypeEntry.
             *
             * @property type type of the parent.
             */
            data class TypeClassParent(
                val type: Type,
                override var tag: Any? = null,
            ) : ClassParent

            /**
             * AST node corresponds to KtPrimaryConstructor.
             *
             * @property modifiers modifiers if exists, otherwise `null`.
             * @property constructorKeyword `constructor` keyword if exists, otherwise `null`.
             * @property params parameters of the constructor if exists, otherwise `null`.
             */
            data class PrimaryConstructor(
                override val modifiers: Modifiers?,
                val constructorKeyword: Keyword.Constructor?,
                val params: FunctionParams?,
                override var tag: Any? = null,
            ) : Node, WithModifiers

            /**
             * AST node corresponds to KtClassBody.
             *
             * @property enumEntries list of enum entries.
             * @property hasTrailingCommaInEnumEntries `true` if the last enum entry has a trailing comma, `false` otherwise.
             * @property declarations list of declarations.
             */
            data class ClassBody(
                val enumEntries: List<EnumEntry>,
                val hasTrailingCommaInEnumEntries: Boolean,
                override val declarations: List<Declaration>,
                override var tag: Any? = null,
            ) : Node, DeclarationsContainer {

                /**
                 * AST node corresponds to KtEnumEntry.
                 *
                 * @property modifiers modifiers if exists, otherwise `null`.
                 * @property name name of the enum entry.
                 * @property args value arguments of the enum entry if exists, otherwise `null`.
                 * @property classBody class body of the enum entry if exists, otherwise `null`.
                 */
                data class EnumEntry(
                    override val modifiers: Modifiers?,
                    val name: Expression.NameExpression,
                    val args: ValueArgs?,
                    val classBody: ClassBody?,
                    override var tag: Any? = null,
                ) : Node, WithModifiers

                /**
                 * AST node that represents an init block, a.k.a. initializer. The node corresponds to KtAnonymousInitializer.
                 *
                 * @property modifiers modifiers if exists, otherwise `null`.
                 * @property block block of the initializer.
                 */
                data class Initializer(
                    override val modifiers: Modifiers?,
                    val block: Expression.BlockExpression,
                    override var tag: Any? = null,
                ) : Declaration, WithModifiers

                /**
                 * AST node corresponds to KtSecondaryConstructor.
                 *
                 * @property modifiers modifiers if exists, otherwise `null`.
                 * @property constructorKeyword `constructor` keyword.
                 * @property params parameters of the secondary constructor if exists, otherwise `null`.
                 * @property delegationCall delegation call expression of the secondary constructor if exists, otherwise `null`.
                 * @property block block of the constructor if exists, otherwise `null`.
                 */
                data class SecondaryConstructor(
                    override val modifiers: Modifiers?,
                    val constructorKeyword: Keyword.Constructor,
                    val params: FunctionParams?,
                    val delegationCall: Expression.CallExpression?,
                    val block: Expression.BlockExpression?,
                    override var tag: Any? = null,
                ) : Declaration, WithModifiers
            }
        }

        /**
         * AST node that represents function declaration. The node corresponds to KtNamedFunction.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property funKeyword `fun` keyword.
         * @property typeParams type parameters of the function if exists, otherwise `null`.
         * @property receiverTypeRef receiver type reference of the function if exists, otherwise `null`.
         * @property name name of the function. If the function is anonymous, the name is `null`.
         * @property params parameters of the function if exists, otherwise `null`.
         * @property returnTypeRef return type reference of the function if exists, otherwise `null`.
         * @property postModifiers post-modifiers of the function.
         * @property equals `=` keyword if exists, otherwise `null`.
         * @property body body of the function if exists, otherwise `null`.
         */
        data class FunctionDeclaration(
            override val modifiers: Modifiers?,
            val funKeyword: Keyword.Fun,
            val typeParams: TypeParams?,
            val receiverTypeRef: TypeRef?,
            val name: Expression.NameExpression?,
            val params: FunctionParams?,
            val returnTypeRef: TypeRef?,
            override val postModifiers: List<PostModifier>,
            override val equals: Keyword.Equal?,
            override val body: Expression?,
            override var tag: Any? = null,
        ) : Declaration, WithModifiers, WithPostModifiers, WithFunctionBody

        /**
         * AST node corresponds to KtProperty or KtDestructuringDeclaration.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property valOrVarKeyword `val` or `var` keyword.
         * @property typeParams type parameters of the property if exists, otherwise `null`.
         * @property receiverTypeRef receiver type reference of the property if exists, otherwise `null`.
         * @property lPar `(` keyword if exists, otherwise `null`. When there are two or more variables, the keyword must exist.
         * @property variables variables of the property. Always at least one, more than one means destructuring.
         * @property trailingComma trailing comma of the variables if exists, otherwise `null`.
         * @property rPar `)` keyword if exists, otherwise `null`. When there are two or more variables, the keyword must exist.
         * @property typeConstraintSet type constraint set of the property if exists, otherwise `null`.
         * @property equals `=` keyword if exists, otherwise `null`. When the property has an initializer, the keyword must exist.
         * @property initializer initializer expression of the property if exists, otherwise `null`. When the property has a delegate, the initializer must be `null`.
         * @property propertyDelegate property delegate of the property if exists, otherwise `null`. When the property has an initializer, the delegate must be `null`.
         * @property accessors accessors of the property.
         */
        data class PropertyDeclaration(
            override val modifiers: Modifiers?,
            val valOrVarKeyword: ValOrVarKeyword,
            val typeParams: TypeParams?,
            val receiverTypeRef: TypeRef?,
            val lPar: Keyword.LPar?,
            val variables: List<Variable>,
            val trailingComma: Keyword.Comma?,
            val rPar: Keyword.RPar?,
            val typeConstraintSet: PostModifier.TypeConstraintSet?,
            val equals: Keyword.Equal?,
            val initializer: Expression?,
            val propertyDelegate: PropertyDelegate?,
            val accessors: List<Accessor>,
            override var tag: Any? = null,
        ) : Declaration, WithModifiers {
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
             *
             * @property byKeyword `by` keyword.
             * @property expression expression of the delegate.
             */
            data class PropertyDelegate(
                val byKeyword: Keyword.By,
                val expression: Expression,
                override var tag: Any? = null,
            ) : Node

            /**
             * AST node corresponds to KtPropertyAccessor.
             */
            sealed interface Accessor : Node, WithModifiers, WithPostModifiers, WithFunctionBody

            /**
             * AST node that represents a property getter.
             *
             * @property modifiers modifiers if exists, otherwise `null`.
             * @property getKeyword `get` keyword.
             * @property typeRef return type reference of the getter if exists, otherwise `null`.
             * @property postModifiers post-modifiers of the getter.
             * @property equals `=` keyword if exists, otherwise `null`.
             * @property body body of the getter if exists, otherwise `null`.
             */
            data class Getter(
                override val modifiers: Modifiers?,
                val getKeyword: Keyword.Get,
                val typeRef: TypeRef?,
                override val postModifiers: List<PostModifier>,
                override val equals: Keyword.Equal?,
                override val body: Expression?,
                override var tag: Any? = null,
            ) : Accessor

            /**
             * AST node that represents a property setter.
             *
             * @property modifiers modifiers if exists, otherwise `null`.
             * @property setKeyword `set` keyword.
             * @property params parameters of the setter if exists, otherwise `null`.
             * @property postModifiers post-modifiers of the setter.
             * @property equals `=` keyword if exists, otherwise `null`.
             * @property body body of the setter if exists, otherwise `null`.
             */
            data class Setter(
                override val modifiers: Modifiers?,
                val setKeyword: Keyword.Set,
                val params: LambdaParams?,
                override val postModifiers: List<PostModifier>,
                override val equals: Keyword.Equal?,
                override val body: Expression?,
                override var tag: Any? = null,
            ) : Accessor
        }

        /**
         * AST node corresponds to KtTypeAlias.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property name name of the type alias.
         * @property typeParams type parameters of the type alias if exists, otherwise `null`.
         * @property typeRef type reference of the type alias.
         */
        data class TypeAliasDeclaration(
            override val modifiers: Modifiers?,
            val name: Expression.NameExpression,
            val typeParams: TypeParams?,
            val typeRef: TypeRef,
            override var tag: Any? = null,
        ) : Declaration, WithModifiers
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
     *
     * @property modifiers modifiers if exists, otherwise `null`.
     * @property valOrVarKeyword `val` or `var` keyword if exists, otherwise `null`.
     * @property name name of the parameter.
     * @property typeRef type reference of the parameter. Can be `null` for anonymous function parameters.
     * @property equals `=` keyword if exists, otherwise `null`.
     * @property defaultValue default value of the parameter if exists, otherwise `null`.
     */
    data class FunctionParam(
        override val modifiers: Modifiers?,
        val valOrVarKeyword: ValOrVarKeyword?,
        val name: Expression.NameExpression,
        val typeRef: TypeRef?,
        val equals: Keyword.Equal?,
        val defaultValue: Expression?,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * AST node corresponds to KtDestructuringDeclarationEntry, virtual AST node corresponds a part of KtProperty, or virtual AST node corresponds to KtParameter whose child is IDENTIFIER.
     *
     * @property modifiers modifiers if exists, otherwise `null`.
     * @property name name of the variable.
     * @property typeRef type reference of the variable if exists, otherwise `null`.
     */
    data class Variable(
        override val modifiers: Modifiers?,
        val name: Expression.NameExpression,
        val typeRef: TypeRef?,
        override var tag: Any? = null,
    ) : Node, WithModifiers

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
     *
     * @property modifiers modifiers if exists, otherwise `null`.
     * @property name name of the type parameter.
     * @property typeRef type reference of the type parameter if exists, otherwise `null`.
     */
    data class TypeParam(
        override val modifiers: Modifiers?,
        val name: Expression.NameExpression,
        val typeRef: TypeRef?,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * Common interface for AST nodes that represent types.
     */
    sealed interface Type : Node {

        private interface NameWithTypeArgs {
            val name: Expression.NameExpression
            val typeArgs: TypeArgs?
        }

        /**
         * AST node corresponds to KtFunctionType.
         * Note that properties [lPar], [modifiers] and [rPar] correspond to those of parent KtTypeReference.
         *
         * @property lPar `(` if exists, otherwise `null`.
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property contextReceivers context receivers if exists, otherwise `null`.
         * @property receiverTypeRef receiver type reference if exists, otherwise `null`.
         * @property dotSymbol `.` if exists, otherwise `null`.
         * @property params parameters of the function type if exists, otherwise `null`.
         * @property returnTypeRef return type reference of the function type.
         * @property rPar `)` if exists, otherwise `null`.
         */
        data class FunctionType(
            val lPar: Keyword.LPar?,
            override val modifiers: Modifiers?,
            val contextReceivers: ContextReceivers?,
            val receiverTypeRef: TypeRef?,
            val dotSymbol: Keyword.Dot?,
            val params: FunctionTypeParams?,
            val returnTypeRef: TypeRef,
            val rPar: Keyword.RPar?,
            override var tag: Any? = null,
        ) : Type, WithModifiers {
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
             *
             * @property typeRef type reference of the context receiver.
             */
            data class ContextReceiver(
                val typeRef: TypeRef,
                override var tag: Any? = null,
            ) : Node

            /**
             * AST node corresponds to KtParameterList under KtFunctionType.
             */
            data class FunctionTypeParams(
                override val elements: List<FunctionTypeParam>,
                override val trailingComma: Keyword.Comma?,
                override var tag: Any? = null,
            ) : CommaSeparatedNodeList<FunctionTypeParam>("(", ")")

            /**
             * AST node corresponds to KtParameter inside KtFunctionType.
             *
             * @property name name of the parameter if exists, otherwise `null`.
             * @property typeRef type reference of the parameter.
             */
            data class FunctionTypeParam(
                val name: Expression.NameExpression?,
                val typeRef: TypeRef,
                override var tag: Any? = null,
            ) : Node
        }

        /**
         * AST node corresponds to KtUserType.
         *
         * @property qualifiers list of qualifiers.
         * @property name name of the type.
         * @property typeArgs type arguments if exists, otherwise `null`.
         */
        data class SimpleType(
            val qualifiers: List<SimpleTypeQualifier>,
            override val name: Expression.NameExpression,
            override val typeArgs: TypeArgs?,
            override var tag: Any? = null,
        ) : Type, NameWithTypeArgs {
            /**
             * AST node corresponds to KtUserType used as a qualifier.
             *
             * @property name name of the qualifier.
             * @property typeArgs type arguments if exists, otherwise `null`.
             */
            data class SimpleTypeQualifier(
                override val name: Expression.NameExpression,
                override val typeArgs: TypeArgs?,
                override var tag: Any? = null,
            ) : Node, NameWithTypeArgs
        }

        /**
         * AST node corresponds to KtNullableType.
         *
         * @property lPar `(` if exists, otherwise `null`.
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property type type.
         * @property rPar `)` if exists, otherwise `null`.
         */
        data class NullableType(
            val lPar: Keyword.LPar?,
            override val modifiers: Modifiers?,
            val type: Type,
            val rPar: Keyword.RPar?,
            override var tag: Any? = null,
        ) : Type, WithModifiers

        /**
         * AST node corresponds to KtDynamicType.
         */
        data class DynamicType(
            override var tag: Any? = null,
        ) : Type

    }

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
     *
     * @property modifiers modifiers if exists, otherwise `null`.
     * @property typeRef type reference if exists, otherwise `null`.
     * @property asterisk `true` if this type projection is `*`, otherwise `false`. When `true`, [modifiers] and [typeRef] must be `null`. When `false`, [typeRef] must not be `null`.
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
                    "typeRef must not be null when asterisk is false"
                }
            }
        }
    }

    /**
     * AST node corresponds to KtTypeReference.
     *
     * @property lPar `(` if exists, otherwise `null`.
     * @property modifiers modifiers if exists, otherwise `null`.
     * @property type type.
     * @property rPar `)` if exists, otherwise `null`.
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
     *
     * @property name name of the argument if exists, otherwise `null`.
     * @property asterisk `true` if this argument is array spread operator, otherwise `false`.
     * @property expression expression of the argument.
     */
    data class ValueArg(
        val name: Expression.NameExpression?,
        val asterisk: Boolean,
        val expression: Expression,
        override var tag: Any? = null,
    ) : Node

    /**
     * AST node corresponds to KtContainerNode.
     *
     * @property expression expression in the container.
     */
    data class ExpressionContainer(
        val expression: Expression,
        override var tag: Any? = null,
    ) : Node

    /**
     * Common interface for AST nodes that represent expressions.
     */
    sealed interface Expression : Statement {
        /**
         * AST node corresponds to KtIfExpression.
         *
         * @property ifKeyword `if` keyword.
         * @property condition condition expression.
         * @property body body expression.
         * @property elseBody else body expression if exists, otherwise `null`.
         */
        data class IfExpression(
            val ifKeyword: Keyword.If,
            val condition: Expression,
            val body: ExpressionContainer,
            val elseBody: ExpressionContainer?,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtTryExpression.
         *
         * @property block block expression.
         * @property catchClauses list of catch clauses.
         * @property finallyBlock finally block expression if exists, otherwise `null`.
         */
        data class TryExpression(
            val block: BlockExpression,
            val catchClauses: List<CatchClause>,
            val finallyBlock: BlockExpression?,
            override var tag: Any? = null,
        ) : Expression {
            /**
             * AST node corresponds to KtCatchClause.
             *
             * @property catchKeyword `catch` keyword.
             * @property params parameters of the catch clause.
             * @property block block expression.
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
         *
         * ```
         * for ([loopParam] in [loopRange]) [body]
         * ```
         *
         * @property forKeyword `for` keyword.
         * @property loopParam loop parameter before `in` keyword.
         * @property loopRange loop range expression after `in` keyword.
         * @property body body expression.
         */
        data class ForExpression(
            val forKeyword: Keyword.For,
            val loopParam: LambdaParam,
            val loopRange: ExpressionContainer,
            val body: ExpressionContainer,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtWhileExpressionBase.
         *
         * @property whileKeyword `while` keyword.
         * @property condition condition expression.
         * @property body body expression.
         * @property doWhile `true` if this is do-while expression, `false` if this is while expression.
         */
        data class WhileExpression(
            val whileKeyword: Keyword.While,
            val condition: ExpressionContainer,
            val body: ExpressionContainer,
            val doWhile: Boolean,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtBinaryExpression or KtQualifiedExpression.
         *
         * @property lhs left-hand side expression.
         * @property operator binary operator.
         * @property rhs right-hand side expression.
         */
        data class BinaryExpression(
            val lhs: Expression,
            val operator: BinaryOperator,
            val rhs: Expression,
            override var tag: Any? = null,
        ) : Expression {
            /**
             * Common interface for AST nodes that represent binary operators.
             * Note that [NameExpression] implements [BinaryOperator] because it can be used as a infix operator.
             */
            sealed interface BinaryOperator : SimpleTextNode
        }

        /**
         * AST node corresponds to KtUnaryExpression.
         *
         * @property expression operand expression.
         * @property operator unary operator.
         * @property prefix `true` if this is prefix expression, `false` if this is postfix expression.
         */
        data class UnaryExpression(
            val expression: Expression,
            val operator: UnaryOperator,
            val prefix: Boolean,
            override var tag: Any? = null,
        ) : Expression {
            /**
             * Common interface for AST nodes that represent unary operators.
             */
            sealed interface UnaryOperator : Keyword
        }

        /**
         * AST node corresponds to KtBinaryExpressionWithTypeRHS or KtIsExpression.
         *
         * @property lhs left-hand side expression.
         * @property operator binary type operator.
         * @property rhs right-hand side type.
         */
        data class BinaryTypeExpression(
            val lhs: Expression,
            val operator: BinaryTypeOperator,
            val rhs: TypeRef,
            override var tag: Any? = null,
        ) : Expression {
            /**
             * Common interface for AST nodes that represent binary type operators.
             */
            sealed interface BinaryTypeOperator : Keyword
        }

        /**
         * AST node corresponds to KtDoubleColonExpression.
         *
         * @property lhs left-hand side expression if exists, otherwise `null`.
         * @property questionMarks list of question marks after [lhs].
         */
        sealed interface DoubleColonExpression : Expression {
            val lhs: Expression?
            val questionMarks: List<Keyword.Question>
        }

        /**
         * AST node corresponds to KtCallableReferenceExpression.
         *
         * @property lhs left-hand side expression if exists, otherwise `null`.
         * @property questionMarks list of question marks after [lhs].
         * @property rhs right-hand side name expression.
         *
         */
        data class CallableReferenceExpression(
            override val lhs: Expression?,
            override val questionMarks: List<Keyword.Question>,
            val rhs: NameExpression,
            override var tag: Any? = null,
        ) : DoubleColonExpression

        /**
         * AST node corresponds to KtClassLiteralExpression.
         *
         * @property lhs left-hand side expression if exists, otherwise `null`. Note that class literal expression without lhs is not supported in Kotlin syntax, but the Kotlin compiler does parse it.
         * @property questionMarks list of question marks after [lhs].
         */
        data class ClassLiteralExpression(
            override val lhs: Expression?,
            override val questionMarks: List<Keyword.Question>,
            override var tag: Any? = null,
        ) : DoubleColonExpression

        /**
         * AST node corresponds to KtParenthesizedExpression.
         *
         * @property expression expression inside parentheses.
         */
        data class ParenthesizedExpression(
            val expression: Expression,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtStringTemplateExpression.
         *
         * @property entries list of string entries.
         * @property raw `true` if this is raw string surrounded by `"""`, `false` if this is regular string surrounded by `"`.
         */
        data class StringLiteralExpression(
            val entries: List<StringEntry>,
            val raw: Boolean,
            override var tag: Any? = null,
        ) : Expression {
            /**
             * AST node corresponds to KtStringTemplateEntry.
             */
            sealed interface StringEntry : Node

            /**
             * AST node corresponds to KtLiteralStringTemplateEntry.
             *
             * @property str string of this entry.
             */
            data class LiteralStringEntry(
                val str: String,
                override var tag: Any? = null,
            ) : StringEntry

            /**
             * AST node corresponds to KtEscapeStringTemplateEntry.
             *
             * @property str string of this entry starting with backslash.
             */
            data class EscapeStringEntry(
                val str: String,
                override var tag: Any? = null,
            ) : StringEntry {
                init {
                    require(str.startsWith('\\')) {
                        "Escape string template entry must start with backslash."
                    }
                }
            }

            /**
             * AST node corresponds to KtStringTemplateEntryWithExpression.
             *
             * @property expression template expression of this entry.
             * @property short `true` if this is short template string entry, e.g. `$x`, `false` if this is long template string entry, e.g. `${x}`. When this is `true`, [expression] must be [NameExpression].
             */
            data class TemplateStringEntry(
                val expression: Expression,
                val short: Boolean,
                override var tag: Any? = null,
            ) : StringEntry {
                init {
                    require(!short || expression is NameExpression) {
                        "Short template string entry must be a name expression."
                    }
                }
            }
        }

        /**
         * AST node corresponds to KtConstantExpression.
         *
         * @property text string representation of this constant.
         */
        sealed interface ConstantLiteralExpression : Expression {
            val text: String
        }

        /**
         * AST node that represents boolean literal. The node corresponds to KtConstantExpression whose expressionType is KtNodeTypes.BOOLEAN_CONSTANT.
         *
         * @property text string representation of this constant, which is either "true" or "false".
         */
        data class BooleanLiteralExpression(
            override val text: String,
            override var tag: Any? = null,
        ) : ConstantLiteralExpression {
            init {
                require(text == "true" || text == "false") {
                    """text must be either "true" or "false"."""
                }
            }
        }

        /**
         * AST node that represents character literal. The node corresponds to KtConstantExpression whose expressionType is KtNodeTypes.CHARACTER_CONSTANT.
         *
         * @property text string representation of this constant, which is surrounded by single quotes.
         */
        data class CharacterLiteralExpression(
            override val text: String,
            override var tag: Any? = null,
        ) : ConstantLiteralExpression {
            init {
                require(text.startsWith('\'') && text.endsWith('\'')) {
                    "text must be surrounded by single quotes."
                }
            }
        }

        /**
         * AST node that represents integer literal. The node corresponds to KtConstantExpression whose expressionType is KtNodeTypes.INTEGER_CONSTANT.
         *
         * @property text string representation of this constant.
         */
        data class IntegerLiteralExpression(
            override val text: String,
            override var tag: Any? = null,
        ) : ConstantLiteralExpression

        /**
         * AST node that represents real number literal. The node corresponds to KtConstantExpression whose expressionType is KtNodeTypes.FLOAT_CONSTANT.
         *
         * @property text string representation of this constant.
         */

        data class RealLiteralExpression(
            override val text: String,
            override var tag: Any? = null,
        ) : ConstantLiteralExpression

        /**
         * AST node that represents null literal. The node corresponds to KtConstantExpression whose expressionType is KtNodeTypes.NULL.
         */
        data class NullLiteralExpression(
            override var tag: Any? = null,
        ) : ConstantLiteralExpression {
            override val text: String
                get() = "null"
        }

        /**
         * AST node corresponds to KtLambdaExpression.
         *
         * @property params parameters of the lambda expression.
         * @property lambdaBody body of the lambda expression.
         */
        data class LambdaExpression(
            val params: LambdaParams?,
            val lambdaBody: LambdaBody?,
            override var tag: Any? = null,
        ) : Expression {

            /**
             * AST node corresponds to KtBlockExpression in lambda body.
             * In lambda expression, left and right braces are not included in [LambdaExpression.LambdaBody], but are included in Lambda.
             * This means:
             *
             * <Lambda> = { <Param>, <Param> -> <Body> }
             *
             * @property statements list of statements in the block.
             */
            data class LambdaBody(
                override val statements: List<Statement>,
                override var tag: Any? = null,
            ) : Expression, StatementsContainer
        }

        /**
         * AST node corresponds to KtThisExpression or KtConstructorDelegationReferenceExpression whose text is "this".
         *
         * @property label label of this expression if exists, otherwise `null`.
         */
        data class ThisExpression(
            val label: String?,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtSuperExpression or KtConstructorDelegationReferenceExpression whose text is "super".
         *
         * @property typeArg type argument if exists, otherwise `null`.
         * @property label label of this expression if exists, otherwise `null`.
         */
        data class SuperExpression(
            val typeArg: TypeRef?,
            val label: String?,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtWhenExpression.
         *
         * @property whenKeyword keyword of when expression.
         * @property lPar left parenthesis of when expression if exists, otherwise `null`.
         * @property expression subject expression of when expression if exists, otherwise `null`.
         * @property rPar right parenthesis of when expression if exists, otherwise `null`.
         * @property whenBranches list of when branches.
         */
        data class WhenExpression(
            val whenKeyword: Keyword.When,
            val lPar: Keyword.LPar?,
            val expression: Expression?,
            val rPar: Keyword.RPar?,
            val whenBranches: List<WhenBranch>,
            override var tag: Any? = null,
        ) : Expression {
            /**
             * AST node corresponds to KtWhenEntry.
             *
             * @property whenConditions list of conditions. When this is empty, [trailingComma] must be `null` and [elseKeyword] must not be `null`.
             * @property trailingComma trailing comma of conditions if exists, otherwise `null`.
             * @property elseKeyword else keyword if exists, otherwise `null`.
             * @property body body expression of this branch.
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

            /**
             * Common interface for when condition operators.
             */
            sealed interface WhenConditionOperator : Keyword

            /**
             * Common interface for when condition type operators.
             */
            sealed interface WhenConditionTypeOperator : WhenConditionOperator

            /**
             * Common interface for when condition range operators.
             */
            sealed interface WhenConditionRangeOperator : WhenConditionOperator

            /**
             * AST node corresponds to KtWhenCondition.
             *
             * @property operator operator of this condition if exists, otherwise `null`.
             * @property expression operand of [operator] if it is [WhenConditionRangeOperator] or condition expression if [operator] is `null`, otherwise `null`.
             * @property typeRef operand of [operator] if it is [WhenConditionTypeOperator], otherwise `null`.
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
         *
         * @property declaration class declaration of this object literal expression.
         */
        data class ObjectLiteralExpression(
            val declaration: Declaration.ClassDeclaration,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtThrowExpression.
         *
         * @property expression expression to be thrown.
         */
        data class ThrowExpression(
            val expression: Expression,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtReturnExpression.
         *
         * @property label label of this return expression if exists, otherwise `null`.
         * @property expression expression to be returned if exists, otherwise `null`.
         */
        data class ReturnExpression(
            val label: String?,
            val expression: Expression?,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtContinueExpression.
         *
         * @property label label of this continue expression if exists, otherwise `null`.
         */
        data class ContinueExpression(
            val label: String?,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtBreakExpression.
         *
         * @property label label of this break expression if exists, otherwise `null`.
         */
        data class BreakExpression(
            val label: String?,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtCollectionLiteralExpression.
         *
         * @property expressions list of element expressions.
         * @property trailingComma trailing comma of [expressions] if exists, otherwise `null`.
         */
        data class CollectionLiteralExpression(
            val expressions: List<Expression>,
            val trailingComma: Keyword.Comma?,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtValueArgumentName, KtSimpleNameExpression or PsiElement whose elementType is IDENTIFIER.
         *
         * @property text string representation of the name expression.
         */
        data class NameExpression(
            override val text: String,
            override var tag: Any? = null,
        ) : Expression, BinaryExpression.BinaryOperator

        /**
         * AST node corresponds to KtLabeledExpression.
         *
         * @property label label before `@` symbol.
         * @property expression expression after `@` symbol.
         */
        data class LabeledExpression(
            val label: String,
            val expression: Expression,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtAnnotatedExpression.
         *
         * @property annotationSets list of annotation sets.
         * @property expression expression of this annotated expression.
         */
        data class AnnotatedExpression(
            override val annotationSets: List<Modifier.AnnotationSet>,
            val expression: Expression,
            override var tag: Any? = null,
        ) : Expression, WithAnnotationSets

        /**
         * AST node corresponds to KtCallElement.
         *
         * @property calleeExpression callee expression.
         * @property typeArgs type arguments if exists, otherwise `null`.
         * @property args value arguments if exists, otherwise `null`.
         * @property lambdaArg lambda argument if exists, otherwise `null`.
         */
        data class CallExpression(
            val calleeExpression: Expression,
            val typeArgs: TypeArgs?,
            val args: ValueArgs?,
            val lambdaArg: LambdaArg?,
            override var tag: Any? = null,
        ) : Expression {
            /**
             * AST node corresponds to KtLambdaArgument.
             *
             * @property annotationSets list of annotation sets.
             * @property label label of this lambda argument if exists, otherwise `null`.
             * @property expression lambda expression.
             */
            data class LambdaArg(
                override val annotationSets: List<Modifier.AnnotationSet>,
                val label: String?,
                val expression: LambdaExpression,
                override var tag: Any? = null,
            ) : Node, WithAnnotationSets
        }

        /**
         * AST node corresponds to KtArrayAccessExpression.
         *
         * @property expression collection expression.
         * @property indices list of index expressions.
         * @property trailingComma trailing comma of [indices] if exists, otherwise `null`.
         */
        data class IndexedAccessExpression(
            val expression: Expression,
            val indices: List<Expression>,
            val trailingComma: Keyword.Comma?,
            override var tag: Any? = null,
        ) : Expression

        /**
         * Virtual AST node corresponds to KtNamedFunction in expression context.
         *
         * @property function function declaration.
         */
        data class AnonymousFunctionExpression(
            val function: Declaration.FunctionDeclaration,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtProperty or KtDestructuringDeclaration.
         * This is only present for when expressions and labeled expressions.
         *
         * @property property property declaration.
         */
        data class PropertyExpression(
            val property: Declaration.PropertyDeclaration,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtBlockExpression.
         *
         * @property statements list of statements.
         */
        data class BlockExpression(
            override val statements: List<Statement>,
            override var tag: Any? = null,
        ) : Expression, StatementsContainer
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
     *
     * @property lPar left parenthesis of this parameter if exists, otherwise `null`.
     * @property variables list of variables.
     * @property trailingComma trailing comma of [variables] if exists, otherwise `null`.
     * @property rPar right parenthesis of this parameter if exists, otherwise `null`.
     * @property colon colon symbol if exists, otherwise `null`.
     * @property destructTypeRef type reference of destructuring if exists, otherwise `null`.
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
    }

    /**
     * AST node corresponds to KtModifierList.
     */
    data class Modifiers(
        override val elements: List<Modifier>,
        override var tag: Any? = null,
    ) : NodeList<Modifier>()

    /**
     * Common interface for modifiers.
     */
    sealed interface Modifier : Node {
        /**
         * AST node corresponds to KtAnnotation or KtAnnotationEntry not under KtAnnotation.
         *
         * @property atSymbol `@` symbol if exists, otherwise `null`.
         * @property target target keyword if exists, otherwise `null`.
         * @property colon colon symbol if exists, otherwise `null`.
         * @property lBracket left bracket symbol if exists, otherwise `null`.
         * @property annotations list of annotations.
         * @property rBracket right bracket symbol if exists, otherwise `null`.
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
            /**
             * Common interface for annotation target keywords.
             */
            sealed interface AnnotationTarget : Keyword

            /**
             * AST node corresponds to KtAnnotationEntry under KtAnnotation or virtual AST node corresponds to KtAnnotationEntry not under KtAnnotation.
             *
             * @property type type of this annotation.
             * @property args value arguments if exists, otherwise `null`.
             */
            data class Annotation(
                val type: Type.SimpleType,
                val args: ValueArgs?,
                override var tag: Any? = null,
            ) : Node
        }

        /**
         * Common interface for keyword modifiers.
         */
        sealed interface KeywordModifier : Modifier, Keyword
    }

    /**
     * Common interface for post-modifiers.
     */
    sealed interface PostModifier : Node {
        /**
         * Virtual AST node corresponds to a pair of "where" keyword and KtTypeConstraintList.
         *
         * @property whereKeyword "where" keyword.
         * @property constraints type constraints.
         */
        data class TypeConstraintSet(
            val whereKeyword: Keyword.Where,
            val constraints: TypeConstraints,
            override var tag: Any? = null,
        ) : PostModifier {
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
             *
             * @property annotationSets list of annotation sets.
             * @property name name of this type constraint.
             * @property typeRef type reference of this type constraint.
             */
            data class TypeConstraint(
                override val annotationSets: List<Modifier.AnnotationSet>,
                val name: Expression.NameExpression,
                val typeRef: TypeRef,
                override var tag: Any? = null,
            ) : Node, WithAnnotationSets
        }

        /**
         * Virtual AST node corresponds to a pair of "contract" keyword and KtContractEffectList.
         *
         * @property contractKeyword "contract" keyword.
         * @property contractEffects contract effects.
         */
        data class Contract(
            val contractKeyword: Keyword.Contract,
            val contractEffects: ContractEffects,
            override var tag: Any? = null,
        ) : PostModifier {
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
             *
             * @property expression expression of this contract effect.
             */
            data class ContractEffect(
                val expression: Expression,
                override var tag: Any? = null,
            ) : Node
        }
    }

    /**
     * Common interface for val or var keywords.
     */
    sealed interface ValOrVarKeyword : Keyword

    /**
     * Common interface for keywords.
     */
    sealed interface Keyword : SimpleTextNode {
        data class Package(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "package"
        }

        data class Import(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "import"
        }

        data class Class(override var tag: Any? = null) : Keyword,
            Declaration.ClassDeclaration.ClassDeclarationKeyword {
            override val text: String; get() = "class"
        }

        data class Object(override var tag: Any? = null) : Keyword,
            Declaration.ClassDeclaration.ClassDeclarationKeyword {
            override val text: String; get() = "object"
        }

        data class Interface(override var tag: Any? = null) : Keyword,
            Declaration.ClassDeclaration.ClassDeclarationKeyword {
            override val text: String; get() = "interface"
        }

        data class Constructor(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "constructor"
        }

        data class Val(override var tag: Any? = null) : Keyword, ValOrVarKeyword {
            override val text: String; get() = "val"
        }

        data class Var(override var tag: Any? = null) : Keyword, ValOrVarKeyword {
            override val text: String; get() = "var"
        }

        data class For(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "for"
        }

        data class While(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "while"
        }

        data class If(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "if"
        }

        data class Else(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "else"
        }

        data class Catch(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "catch"
        }

        data class When(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "when"
        }

        data class By(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "by"
        }

        data class Contract(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "contract"
        }

        data class Where(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "where"
        }

        data class Field(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text: String; get() = "field"
        }

        data class File(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text: String; get() = "file"
        }

        data class Property(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text: String; get() = "property"
        }

        data class Get(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text: String; get() = "get"
        }

        data class Set(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text: String; get() = "set"
        }

        data class Receiver(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text: String; get() = "receiver"
        }

        data class Param(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text: String; get() = "param"
        }

        data class SetParam(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text: String; get() = "setparam"
        }

        data class Delegate(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text: String; get() = "delegate"
        }

        data class Equal(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "="
        }

        data class Comma(override var tag: Any? = null) : Keyword {
            override val text: String; get() = ","
        }

        data class LPar(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "("
        }

        data class RPar(override var tag: Any? = null) : Keyword {
            override val text: String; get() = ")"
        }

        data class LBracket(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "["
        }

        data class RBracket(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "]"
        }

        data class At(override var tag: Any? = null) : Keyword {
            override val text: String; get() = "@"
        }

        data class Asterisk(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "*"
        }

        data class Slash(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "/"
        }

        data class Percent(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "%"
        }

        data class Plus(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator,
            Expression.UnaryExpression.UnaryOperator {
            override val text: String; get() = "+"
        }

        data class Minus(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator,
            Expression.UnaryExpression.UnaryOperator {
            override val text: String; get() = "-"
        }

        data class In(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator,
            Modifier.KeywordModifier,
            Expression.WhenExpression.WhenConditionRangeOperator {
            override val text: String; get() = "in"
        }

        data class NotIn(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator,
            Expression.WhenExpression.WhenConditionRangeOperator {
            override val text: String; get() = "!in"
        }

        data class Greater(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = ">"
        }

        data class GreaterEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = ">="
        }

        data class Less(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "<"
        }

        data class LessEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "<="
        }

        data class EqualEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "=="
        }

        data class NotEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "!="
        }

        data class AsteriskEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "*="
        }

        data class SlashEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "/="
        }

        data class PercentEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "%="
        }

        data class PlusEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "+="
        }

        data class MinusEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "-="
        }

        data class OrOr(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "||"
        }

        data class AndAnd(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "&&"
        }

        data class QuestionColon(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "?:"
        }

        data class DotDot(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = ".."
        }

        data class DotDotLess(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "..<"
        }

        data class Dot(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "."
        }

        data class QuestionDot(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "?."
        }

        data class Question(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text: String; get() = "?"
        }

        data class PlusPlus(override var tag: Any? = null) : Keyword, Expression.UnaryExpression.UnaryOperator {
            override val text: String; get() = "++"
        }

        data class MinusMinus(override var tag: Any? = null) : Keyword, Expression.UnaryExpression.UnaryOperator {
            override val text: String; get() = "--"
        }

        data class Not(override var tag: Any? = null) : Keyword, Expression.UnaryExpression.UnaryOperator {
            override val text: String; get() = "!"
        }

        data class NotNot(override var tag: Any? = null) : Keyword, Expression.UnaryExpression.UnaryOperator {
            override val text: String; get() = "!!"
        }

        data class As(override var tag: Any? = null) : Keyword, Expression.BinaryTypeExpression.BinaryTypeOperator {
            override val text: String; get() = "as"
        }

        data class AsQuestion(override var tag: Any? = null) : Keyword,
            Expression.BinaryTypeExpression.BinaryTypeOperator {
            override val text: String; get() = "as?"
        }

        data class Colon(override var tag: Any? = null) : Keyword, Expression.BinaryTypeExpression.BinaryTypeOperator {
            override val text: String; get() = ":"
        }

        data class Is(override var tag: Any? = null) : Keyword, Expression.BinaryTypeExpression.BinaryTypeOperator,
            Expression.WhenExpression.WhenConditionTypeOperator {
            override val text: String; get() = "is"
        }

        data class NotIs(override var tag: Any? = null) : Keyword, Expression.BinaryTypeExpression.BinaryTypeOperator,
            Expression.WhenExpression.WhenConditionTypeOperator {
            override val text: String; get() = "!is"
        }

        data class Abstract(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "abstract"
        }

        data class Final(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "final"
        }

        data class Open(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "open"
        }

        data class Annotation(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "annotation"
        }

        data class Sealed(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "sealed"
        }

        data class Data(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "data"
        }

        data class Override(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "override"
        }

        data class LateInit(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "lateinit"
        }

        data class Inner(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "inner"
        }

        data class Enum(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "enum"
        }

        data class Companion(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "companion"
        }

        data class Value(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "value"
        }

        data class Private(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "private"
        }

        data class Protected(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "protected"
        }

        data class Public(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "public"
        }

        data class Internal(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "internal"
        }

        data class Out(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "out"
        }

        data class Noinline(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "noinline"
        }

        data class CrossInline(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "crossinline"
        }

        data class Vararg(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "vararg"
        }

        data class Reified(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "reified"
        }

        data class TailRec(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "tailrec"
        }

        data class Operator(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "operator"
        }

        data class Infix(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "infix"
        }

        data class Inline(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "inline"
        }

        data class External(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "external"
        }

        data class Suspend(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "suspend"
        }

        data class Const(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "const"
        }

        data class Fun(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "fun"
        }

        data class Actual(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "actual"
        }

        data class Expect(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text: String; get() = "expect"
        }
    }

    /**
     * Common interface for extra nodes.
     *
     * @property text string representation of the node.
     */
    sealed interface Extra : Node {
        val text: String

        /**
         * AST node corresponds to PsiWhiteSpace.
         *
         * @property text string representation of the node.
         */
        data class Whitespace(
            override val text: String,
            override var tag: Any? = null,
        ) : Extra

        /**
         * AST node corresponds to PsiComment.
         *
         * @property text string representation of the node. It contains comment markers, e.g. "//".
         */
        data class Comment(
            override val text: String,
            override var tag: Any? = null,
        ) : Extra

        /**
         * AST node corresponds to PsiElement whose elementType is SEMICOLON.
         *
         * @property text string representation of the node.
         */
        data class Semicolon(
            override val text: String,
            override var tag: Any? = null,
        ) : Extra
    }

}