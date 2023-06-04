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
     * @property elements list of elements in the list.
     */
    abstract class NodeList<out E : Node> : Node {
        abstract val elements: List<E>
    }

    /**
     * Specialization of [NodeList] for comma-separated lists.
     *
     * @property trailingComma trailing comma node of the list if exists.
     */
    abstract class CommaSeparatedNodeList<out E : Node> : NodeList<E>() {
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
     * @property importDirectives list of import directives.
     */
    interface KotlinEntry : WithAnnotationSets {
        val packageDirective: PackageDirective?
        val importDirectives: List<ImportDirective>
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

    interface WithTypeParams {
        val lAngle: Keyword.Less?
        val typeParams: TypeParams?
        val rAngle: Keyword.Greater?
    }

    interface WithFunctionParams {
        val lPar: Keyword.LPar?
        val params: FunctionParams?
        val rPar: Keyword.RPar?
    }

    interface WithTypeArgs {
        val lAngle: Keyword.Less?
        val typeArgs: TypeArgs?
        val rAngle: Keyword.Greater?
    }

    interface WithValueArgs {
        val lPar: Keyword.LPar?
        val args: ValueArgs?
        val rPar: Keyword.RPar?
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
     * @property importDirectives list of import directives.
     * @property declarations list of declarations.
     */
    data class KotlinFile(
        override val annotationSets: List<Modifier.AnnotationSet>,
        override val packageDirective: PackageDirective?,
        override val importDirectives: List<ImportDirective>,
        override val declarations: List<Declaration>,
        override var tag: Any? = null,
    ) : Node, KotlinEntry, DeclarationsContainer

    /**
     * @property annotationSets list of annotation sets.
     * @property packageDirective package directive if exists, otherwise `null`.
     * @property importDirectives list of import directives.
     * @property expressions list of expressions.
     */
    data class KotlinScript(
        override val annotationSets: List<Modifier.AnnotationSet>,
        override val packageDirective: PackageDirective?,
        override val importDirectives: List<ImportDirective>,
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
     * Common interface for [Declaration], [Expression] and loop statements.
     */
    sealed interface Statement : Node {

        /**
         * AST node corresponds to KtForExpression.
         *
         * ```
         * for ([loopParam] in [loopRange]) [body]
         * ```
         *
         * @property forKeyword `for` keyword.
         * @property lPar left parenthesis.
         * @property loopParam loop parameter before `in` keyword.
         * @property inKeyword `in` keyword.
         * @property loopRange loop range expression after `in` keyword.
         * @property rPar right parenthesis.
         * @property body body expression.
         */
        data class ForStatement(
            val forKeyword: Keyword.For,
            val lPar: Keyword.LPar,
            val loopParam: LambdaParam,
            val inKeyword: Keyword.In,
            val loopRange: Expression,
            val rPar: Keyword.RPar,
            val body: Expression,
            override var tag: Any? = null,
        ) : Statement

        /**
         * Common interface for [WhileStatement] and [DoWhileStatement].
         */
        sealed interface WhileStatementBase : Statement {
            val whileKeyword: Keyword.While
            val lPar: Keyword.LPar
            val condition: Expression
            val rPar: Keyword.RPar
            val body: Expression
        }

        /**
         * AST node corresponds to KtWhileExpression.
         *
         * @property whileKeyword `while` keyword.
         * @property condition condition expression.
         * @property body body expression.
         */
        data class WhileStatement(
            override val whileKeyword: Keyword.While,
            override val lPar: Keyword.LPar,
            override val condition: Expression,
            override val rPar: Keyword.RPar,
            override val body: Expression,
            override var tag: Any? = null,
        ) : WhileStatementBase

        /**
         * AST node corresponds to KtDoWhileExpression.
         *
         * @property body body expression.
         * @property whileKeyword `while` keyword.
         * @property condition condition expression.
         */
        data class DoWhileStatement(
            val doKeyword: Keyword.Do,
            override val body: Expression,
            override val whileKeyword: Keyword.While,
            override val lPar: Keyword.LPar,
            override val condition: Expression,
            override val rPar: Keyword.RPar,
            override var tag: Any? = null,
        ) : WhileStatementBase
    }

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
            override val lAngle: Keyword.Less?,
            override val typeParams: TypeParams?,
            override val rAngle: Keyword.Greater?,
            val primaryConstructor: PrimaryConstructor?,
            val classParents: ClassParents?,
            val typeConstraintSet: PostModifier.TypeConstraintSet?,
            val classBody: ClassBody?,
            override var tag: Any? = null,
        ) : Declaration, WithModifiers, WithTypeParams {
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
            ) : CommaSeparatedNodeList<ClassParent>() {
                override val trailingComma: Keyword.Comma? = null
            }

            /**
             * AST node that represents a parent of the class. The node corresponds to KtSuperTypeListEntry.
             *
             * @property type type of the parent.
             * @property args value arguments of the parent call if exists, otherwise `null`.
             * @property byKeyword `by` keyword if exists, otherwise `null`.
             * @property expression expression of the delegation if exists, otherwise `null`.
             */
            sealed interface ClassParent : Node, WithValueArgs {
                val type: Type
                val byKeyword: Keyword.By?
                val expression: Expression?
            }

            /**
             * ClassParent node that represents constructor invocation. The node corresponds to KtSuperTypeCallEntry.
             *
             * @property type type of the parent.
             * @property args value arguments of the parent call.
             * @property byKeyword always `null`.
             * @property expression always `null`.
             */
            data class ConstructorClassParent(
                override val type: Type.SimpleType,
                override val lPar: Keyword.LPar,
                override val args: ValueArgs,
                override val rPar: Keyword.RPar,
                override var tag: Any? = null,
            ) : ClassParent {
                override val byKeyword: Keyword.By? = null
                override val expression: Expression? = null
            }

            /**
             * ClassParent node that represents explicit delegation. The node corresponds to KtDelegatedSuperTypeEntry.
             *
             * @property type type of the interface delegated to.
             * @property args always `null`.
             * @property byKeyword `by` keyword.
             * @property expression expression of the delegation.
             */
            data class DelegationClassParent(
                override val type: Type,
                override val byKeyword: Keyword.By,
                override val expression: Expression,
                override var tag: Any? = null,
            ) : ClassParent {
                override val lPar: Keyword.LPar? = null
                override val args: ValueArgs? = null
                override val rPar: Keyword.RPar? = null
            }

            /**
             * ClassParent node that represents just a type. The node corresponds to KtSuperTypeEntry.
             *
             * @property type type of the parent.
             * @property args always `null`.
             * @property byKeyword always `null`.
             * @property expression always `null`.
             */
            data class TypeClassParent(
                override val type: Type,
                override var tag: Any? = null,
            ) : ClassParent {
                override val lPar: Keyword.LPar? = null
                override val args: ValueArgs? = null
                override val rPar: Keyword.RPar? = null
                override val byKeyword: Keyword.By? = null
                override val expression: Expression? = null
            }

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
                override val lPar: Keyword.LPar?,
                override val params: FunctionParams?,
                override val rPar: Keyword.RPar?,
                override var tag: Any? = null,
            ) : Node, WithModifiers, WithFunctionParams

            /**
             * AST node corresponds to KtClassBody.
             *
             * @property enumEntries list of enum entries.
             * @property hasTrailingCommaInEnumEntries `true` if the last enum entry has a trailing comma, `false` otherwise.
             * @property declarations list of declarations.
             */
            data class ClassBody(
                val lBrace: Keyword.LBrace,
                val enumEntries: List<EnumEntry>,
                val hasTrailingCommaInEnumEntries: Boolean,
                override val declarations: List<Declaration>,
                val rBrace: Keyword.RBrace,
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
                    override val lPar: Keyword.LPar?,
                    override val args: ValueArgs?,
                    override val rPar: Keyword.RPar?,
                    val classBody: ClassBody?,
                    override var tag: Any? = null,
                ) : Node, WithModifiers, WithValueArgs

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
                    override val lPar: Keyword.LPar?,
                    override val params: FunctionParams?,
                    override val rPar: Keyword.RPar?,
                    val delegationCall: Expression.CallExpression?,
                    val block: Expression.BlockExpression?,
                    override var tag: Any? = null,
                ) : Declaration, WithModifiers, WithFunctionParams
            }
        }

        /**
         * AST node that represents function declaration. The node corresponds to KtNamedFunction.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property funKeyword `fun` keyword.
         * @property typeParams type parameters of the function if exists, otherwise `null`.
         * @property receiverType receiver type of the function if exists, otherwise `null`.
         * @property name name of the function. If the function is anonymous, the name is `null`.
         * @property params parameters of the function if exists, otherwise `null`.
         * @property returnType return type of the function if exists, otherwise `null`.
         * @property postModifiers post-modifiers of the function.
         * @property equals `=` keyword if exists, otherwise `null`.
         * @property body body of the function if exists, otherwise `null`.
         */
        data class FunctionDeclaration(
            override val modifiers: Modifiers?,
            val funKeyword: Keyword.Fun,
            override val lAngle: Keyword.Less?,
            override val typeParams: TypeParams?,
            override val rAngle: Keyword.Greater?,
            val receiverType: Type?,
            val name: Expression.NameExpression?,
            override val lPar: Keyword.LPar?,
            override val params: FunctionParams?,
            override val rPar: Keyword.RPar?,
            val returnType: Type?,
            override val postModifiers: List<PostModifier>,
            override val equals: Keyword.Equal?,
            override val body: Expression?,
            override var tag: Any? = null,
        ) : Declaration, WithModifiers, WithTypeParams, WithFunctionParams, WithPostModifiers, WithFunctionBody

        /**
         * AST node corresponds to KtProperty or KtDestructuringDeclaration.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property valOrVarKeyword `val` or `var` keyword.
         * @property typeParams type parameters of the property if exists, otherwise `null`.
         * @property receiverType receiver type of the property if exists, otherwise `null`.
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
            val valOrVarKeyword: Keyword.ValOrVarKeyword,
            override val lAngle: Keyword.Less?,
            override val typeParams: TypeParams?,
            override val rAngle: Keyword.Greater?,
            val receiverType: Type?,
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
        ) : Declaration, WithModifiers, WithTypeParams {
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
             * @property type return type of the getter if exists, otherwise `null`.
             * @property postModifiers post-modifiers of the getter.
             * @property equals `=` keyword if exists, otherwise `null`.
             * @property body body of the getter if exists, otherwise `null`.
             */
            data class Getter(
                override val modifiers: Modifiers?,
                val getKeyword: Keyword.Get,
                val type: Type?,
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
            ) : Accessor {
                init {
                    if (params == null) {
                        require(equals == null && body == null) { "equals and body must be null when params is null" }
                    } else {
                        require(body != null) { "body must be non-null when params is non-null" }
                    }
                }
            }
        }

        /**
         * AST node corresponds to KtTypeAlias.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property name name of the type alias.
         * @property typeParams type parameters of the type alias if exists, otherwise `null`.
         * @property type existing type of the type alias.
         */
        data class TypeAliasDeclaration(
            override val modifiers: Modifiers?,
            val name: Expression.NameExpression,
            override val lAngle: Keyword.Less?,
            override val typeParams: TypeParams?,
            override val rAngle: Keyword.Greater?,
            val equals: Keyword.Equal,
            val type: Type,
            override var tag: Any? = null,
        ) : Declaration, WithModifiers, WithTypeParams
    }

    /**
     * AST node corresponds to KtParameterList under KtNamedFunction.
     */
    data class FunctionParams(
        override val elements: List<FunctionParam>,
        override val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : CommaSeparatedNodeList<FunctionParam>()

    /**
     * AST node that represents a formal function parameter of a function declaration. For example, `x: Int` in `fun f(x: Int)` is a function parameter. The node corresponds to KtParameter inside KtNamedFunction.
     *
     * @property modifiers modifiers if exists, otherwise `null`.
     * @property valOrVarKeyword `val` or `var` keyword if exists, otherwise `null`.
     * @property name name of the parameter.
     * @property type type of the parameter. Can be `null` for anonymous function parameters.
     * @property equals `=` keyword if exists, otherwise `null`.
     * @property defaultValue default value of the parameter if exists, otherwise `null`.
     */
    data class FunctionParam(
        override val modifiers: Modifiers?,
        val valOrVarKeyword: Keyword.ValOrVarKeyword?,
        val name: Expression.NameExpression,
        val type: Type?,
        val equals: Keyword.Equal?,
        val defaultValue: Expression?,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * AST node corresponds to KtDestructuringDeclarationEntry, virtual AST node corresponds a part of KtProperty, or virtual AST node corresponds to KtParameter whose child is IDENTIFIER.
     *
     * @property modifiers modifiers if exists, otherwise `null`.
     * @property name name of the variable.
     * @property type type of the variable if exists, otherwise `null`.
     */
    data class Variable(
        override val modifiers: Modifiers?,
        val name: Expression.NameExpression,
        val type: Type?,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * AST node corresponds to KtTypeParameterList.
     */
    data class TypeParams(
        override val elements: List<TypeParam>,
        override val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : CommaSeparatedNodeList<TypeParam>()

    /**
     * AST node that represents a formal type parameter of a function or a class. For example, `T` in `fun <T> f()` is a type parameter. The node corresponds to KtTypeParameter.
     *
     * @property modifiers modifiers if exists, otherwise `null`.
     * @property name name of the type parameter.
     * @property type type of the type parameter if exists, otherwise `null`.
     */
    data class TypeParam(
        override val modifiers: Modifiers?,
        val name: Expression.NameExpression,
        val type: Type?,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * Common interface for AST nodes that represent types.
     */
    sealed interface Type : Node, WithModifiers {

        /**
         * Virtual AST node corresponds to KtTypeReference or KtNullableType having `(` and `)` as children.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property lPar `(` symbol.
         * @property type inner type.
         * @property rPar `)` symbol.
         */
        data class ParenthesizedType(
            override val modifiers: Modifiers?,
            val lPar: Keyword.LPar,
            val type: Type,
            val rPar: Keyword.RPar,
            override var tag: Any? = null,
        ) : Type

        /**
         * AST node corresponds to KtNullableType.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property type type.
         * @property questionMark `?` symbol.
         */
        data class NullableType(
            override val modifiers: Modifiers?,
            val type: Type,
            val questionMark: Keyword.Question,
            override var tag: Any? = null,
        ) : Type

        private interface NameWithTypeArgs : WithTypeArgs {
            val name: Expression.NameExpression
        }

        /**
         * AST node corresponds to KtUserType.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property qualifiers list of qualifiers.
         * @property name name of the type.
         * @property typeArgs type arguments if exists, otherwise `null`.
         */
        data class SimpleType(
            override val modifiers: Modifiers?,
            val qualifiers: List<SimpleTypeQualifier>,
            override val name: Expression.NameExpression,
            override val lAngle: Keyword.Less?,
            override val typeArgs: TypeArgs?,
            override val rAngle: Keyword.Greater?,
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
                override val lAngle: Keyword.Less?,
                override val typeArgs: TypeArgs?,
                override val rAngle: Keyword.Greater?,
                override var tag: Any? = null,
            ) : Node, NameWithTypeArgs
        }

        /**
         * AST node corresponds to KtDynamicType.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         */
        data class DynamicType(
            override val modifiers: Modifiers?,
            override var tag: Any? = null,
        ) : Type

        /**
         * AST node corresponds to KtFunctionType.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property contextReceiver context receivers if exists, otherwise `null`.
         * @property receiverType receiver type if exists, otherwise `null`.
         * @property dotSymbol `.` if exists, otherwise `null`.
         * @property params parameters of the function type if exists, otherwise `null`.
         * @property returnType return type of the function type.
         */
        data class FunctionType(
            override val modifiers: Modifiers?,
            val contextReceiver: ContextReceiver?,
            val receiverType: Type?,
            val dotSymbol: Keyword.Dot?,
            val lPar: Keyword.LPar?,
            val params: FunctionTypeParams?,
            val rPar: Keyword.RPar?,
            val returnType: Type,
            override var tag: Any? = null,
        ) : Type {

            /**
             * AST node corresponds to KtParameterList under KtFunctionType.
             */
            data class FunctionTypeParams(
                override val elements: List<FunctionTypeParam>,
                override val trailingComma: Keyword.Comma?,
                override var tag: Any? = null,
            ) : CommaSeparatedNodeList<FunctionTypeParam>()

            /**
             * AST node that represents a formal function parameter of a function type. For example, `x: Int` in `(x: Int) -> Unit` is a function parameter. The node corresponds to KtParameter inside KtFunctionType.
             * Unlike [FunctionParam], [name] is optional, but [type] is mandatory.
             *
             * @property name name of the parameter if exists, otherwise `null`.
             * @property type type of the parameter.
             */
            data class FunctionTypeParam(
                val name: Expression.NameExpression?,
                val type: Type,
                override var tag: Any? = null,
            ) : Node
        }
    }

    /**
     * AST node corresponds to KtTypeArgumentList.
     */
    data class TypeArgs(
        override val elements: List<TypeArg>,
        override val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : CommaSeparatedNodeList<TypeArg>()

    /**
     * Common interface for AST node that represents an actual type argument. For example, `Int` in `listOf<Int>()` is a type argument. The node corresponds to KtTypeProjection.
     *
     * @property modifiers modifiers if exists, otherwise `null`.
     * @property type type if exists, otherwise `null`.
     * @property asterisk `*` if exists, otherwise `null`.
     */
    sealed interface TypeArg : Node, WithModifiers {
        val type: Type?
        val asterisk: Keyword.Asterisk?

        /**
         * AST node that represents a type projection.
         *
         * @property modifiers modifiers if exists, otherwise `null`.
         * @property type type
         * @property asterisk always `null`.
         */
        data class TypeProjection(
            override val modifiers: Modifiers?,
            override val type: Type,
            override var tag: Any? = null,
        ) : TypeArg {
            override val asterisk = null
        }

        /**
         * AST node that represents a star projection.
         *
         * @property modifiers always `null`.
         * @property type always `null`.
         * @property asterisk asterisk keyword.
         */
        data class StarProjection(
            override val asterisk: Keyword.Asterisk,
            override var tag: Any? = null,
        ) : TypeArg {
            override val modifiers = null
            override val type = null
        }
    }

    /**
     * AST node corresponds to KtValueArgumentList or KtInitializerList.
     */
    data class ValueArgs(
        override val elements: List<ValueArg>,
        override val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : CommaSeparatedNodeList<ValueArg>()

    /**
     * AST node that represents an actual value argument of a function call. For example, `foo(1, 2)` has two value arguments `1` and `2`. The node corresponds to KtValueArgument.
     *
     * @property name name of the argument if exists, otherwise `null`.
     * @property asterisk spread operator if exists, otherwise `null`.
     * @property expression expression of the argument.
     */
    data class ValueArg(
        val name: Expression.NameExpression?,
        val asterisk: Keyword.Asterisk?,
        val expression: Expression,
        override var tag: Any? = null,
    ) : Node

    /**
     * Common interface for AST nodes that represent expressions.
     */
    sealed interface Expression : Statement {

        /**
         * Common interface for AST nodes that can have a label.
         *
         * @property label label if exists, otherwise `null`.
         */
        interface WithLabel {
            val label: NameExpression?
        }

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
            val lPar: Keyword.LPar,
            val condition: Expression,
            val rPar: Keyword.RPar,
            val body: Expression,
            val elseKeyword: Keyword.Else?,
            val elseBody: Expression?,
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
                override val lPar: Keyword.LPar,
                override val params: FunctionParams,
                override val rPar: Keyword.RPar,
                val block: BlockExpression,
                override var tag: Any? = null,
            ) : Node, WithFunctionParams
        }

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
         * Common interface for [PrefixUnaryExpression] and [PostfixUnaryExpression]. The node corresponds to KtUnaryExpression
         *
         * @property expression operand expression.
         * @property operator unary operator.
         */
        sealed interface UnaryExpression : Expression {
            val expression: Expression
            val operator: UnaryOperator

            /**
             * Common interface for AST nodes that represent unary operators.
             */
            sealed interface UnaryOperator : Keyword
        }

        /**
         * AST node corresponds to KtPrefixExpression.
         *
         * @property operator unary operator.
         * @property expression operand expression.
         */
        data class PrefixUnaryExpression(
            override val operator: UnaryExpression.UnaryOperator,
            override val expression: Expression,
            override var tag: Any? = null,
        ) : UnaryExpression

        /**
         * AST node corresponds to KtPostfixExpression.
         *
         * @property expression operand expression.
         * @property operator unary operator.
         */
        data class PostfixUnaryExpression(
            override val expression: Expression,
            override val operator: UnaryExpression.UnaryOperator,
            override var tag: Any? = null,
        ) : UnaryExpression

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
            val rhs: Type,
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
             * @property text string of this entry.
             */
            data class LiteralStringEntry(
                override val text: String,
                override var tag: Any? = null,
            ) : StringEntry, SimpleTextNode

            /**
             * AST node corresponds to KtEscapeStringTemplateEntry.
             *
             * @property text string of this entry starting with backslash.
             */
            data class EscapeStringEntry(
                override val text: String,
                override var tag: Any? = null,
            ) : StringEntry, SimpleTextNode {
                init {
                    require(text.startsWith('\\')) {
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
        sealed interface ConstantLiteralExpression : Expression, SimpleTextNode {
            override val text: String
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
            val lBrace: Keyword.LBrace,
            val lambdaBody: LambdaBody?,
            val rBrace: Keyword.RBrace,
            override var tag: Any? = null,
        ) : Expression {

            /**
             * AST node corresponds to KtBlockExpression in lambda body.
             * In lambda expression, left and right braces are not included in [LambdaBody], but are included in [LambdaExpression].
             * This means:
             *
             * [LambdaExpression] = { [LambdaParam], [LambdaParam] -> [LambdaBody] }
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
            override val label: NameExpression?,
            override var tag: Any? = null,
        ) : Expression, WithLabel

        /**
         * AST node corresponds to KtSuperExpression or KtConstructorDelegationReferenceExpression whose text is "super".
         *
         * @property typeArgType type of type argument if exists, otherwise `null`.
         * @property label label of this expression if exists, otherwise `null`.
         */
        data class SuperExpression(
            val typeArgType: Type?,
            override val label: NameExpression?,
            override var tag: Any? = null,
        ) : Expression, WithLabel

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
             * @property whenConditions list of conditions.
             * @property trailingComma trailing comma of conditions if exists, otherwise `null`.
             * @property elseKeyword else keyword if exists, otherwise `null`.
             * @property body body expression of this branch.
             */
            sealed interface WhenBranch : Node {
                val whenConditions: List<WhenCondition>
                val trailingComma: Keyword.Comma?
                val elseKeyword: Keyword.Else?
                val body: Expression
            }

            /**
             * AST node that represents when branch with conditions.
             *
             * @property whenConditions non-empty list of conditions.
             * @property trailingComma trailing comma of conditions if exists, otherwise `null`.
             * @property elseKeyword always `null`.
             * @property body body expression of this branch.
             */
            data class ConditionalWhenBranch(
                override val whenConditions: List<WhenCondition>,
                override val trailingComma: Keyword.Comma?,
                override val body: Expression,
                override var tag: Any? = null,
            ) : WhenBranch {
                override val elseKeyword = null

                init {
                    require(whenConditions.isNotEmpty()) { "whenConditions must not be empty" }
                }
            }

            /**
             * AST node that represents when branch with else keyword.
             *
             * @property whenConditions always empty list.
             * @property trailingComma always `null`.
             * @property elseKeyword else keyword.
             * @property body body expression of this branch.
             */
            data class ElseWhenBranch(
                override val elseKeyword: Keyword.Else,
                override val body: Expression,
                override var tag: Any? = null,
            ) : WhenBranch {
                override val whenConditions = listOf<WhenCondition>()
                override val trailingComma = null
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
             * @property expression operand of [operator] or condition expression, otherwise `null`.
             * @property type operand of [operator], otherwise `null`.
             */
            sealed interface WhenCondition : Node {
                val operator: WhenConditionOperator?
                val expression: Expression?
                val type: Type?
            }

            /**
             * AST node corresponds to KtWhenConditionWithExpression.
             *
             * @property operator always `null`.
             * @property expression condition expression.
             * @property type always `null`.
             */
            data class ExpressionWhenCondition(
                override val expression: Expression,
                override var tag: Any? = null,
            ) : WhenCondition {
                override val operator = null
                override val type = null
            }

            /**
             * AST node corresponds to KtWhenConditionInRange.
             *
             * @property operator operator of this condition.
             * @property expression operand of [operator].
             * @property type always `null`.
             */
            data class RangeWhenCondition(
                override val operator: WhenConditionRangeOperator,
                override val expression: Expression,
                override var tag: Any? = null,
            ) : WhenCondition {
                override val type = null
            }

            /**
             * AST node corresponds to KtWhenConditionIsPattern.
             *
             * @property operator operator of this condition.
             * @property expression always `null`.
             * @property type operand of [operator].
             */
            data class TypeWhenCondition(
                override val operator: WhenConditionTypeOperator,
                override val type: Type,
                override var tag: Any? = null,
            ) : WhenCondition {
                override val expression = null
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
            override val label: NameExpression?,
            val expression: Expression?,
            override var tag: Any? = null,
        ) : Expression, WithLabel

        /**
         * AST node corresponds to KtContinueExpression.
         *
         * @property label label of this continue expression if exists, otherwise `null`.
         */
        data class ContinueExpression(
            override val label: NameExpression?,
            override var tag: Any? = null,
        ) : Expression, WithLabel

        /**
         * AST node corresponds to KtBreakExpression.
         *
         * @property label label of this break expression if exists, otherwise `null`.
         */
        data class BreakExpression(
            override val label: NameExpression?,
            override var tag: Any? = null,
        ) : Expression, WithLabel

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
         * @property statement statement labeled by [label].
         */
        data class LabeledExpression(
            val label: NameExpression,
            val statement: Statement,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node corresponds to KtAnnotatedExpression.
         *
         * @property annotationSets list of annotation sets.
         * @property statement statement annotated by [annotationSets].
         */
        data class AnnotatedExpression(
            override val annotationSets: List<Modifier.AnnotationSet>,
            val statement: Statement,
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
            override val lAngle: Keyword.Less?,
            override val typeArgs: TypeArgs?,
            override val rAngle: Keyword.Greater?,
            override val lPar: Keyword.LPar?,
            override val args: ValueArgs?,
            override val rPar: Keyword.RPar?,
            val lambdaArg: LambdaArg?,
            override var tag: Any? = null,
        ) : Expression, WithTypeArgs, WithValueArgs {
            /**
             * AST node corresponds to KtLambdaArgument.
             *
             * @property annotationSets list of annotation sets.
             * @property label label of this lambda argument if exists, otherwise `null`.
             * @property expression lambda expression.
             */
            data class LambdaArg(
                override val annotationSets: List<Modifier.AnnotationSet>,
                override val label: NameExpression?,
                val expression: LambdaExpression,
                override var tag: Any? = null,
            ) : Node, WithAnnotationSets, WithLabel
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
            val lBrace: Keyword.LBrace,
            override val statements: List<Statement>,
            val rBrace: Keyword.RBrace,
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
    ) : CommaSeparatedNodeList<LambdaParam>()

    /**
     * AST node that represents a formal parameter of lambda expression. For example, `x` in `{ x -> ... }` is a lambda parameter. The node corresponds to KtParameter under KtLambdaExpression.
     *
     * @property lPar left parenthesis of this parameter if exists, otherwise `null`.
     * @property variables list of variables.
     * @property trailingComma trailing comma of [variables] if exists, otherwise `null`.
     * @property rPar right parenthesis of this parameter if exists, otherwise `null`.
     * @property colon colon symbol if exists, otherwise `null`.
     * @property destructType type of destructuring if exists, otherwise `null`.
     */
    data class LambdaParam(
        val lPar: Keyword.LPar?,
        val variables: List<Variable>,
        val trailingComma: Keyword.Comma?,
        val rPar: Keyword.RPar?,
        val colon: Keyword.Colon?,
        val destructType: Type?,
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
                override val lPar: Keyword.LPar?,
                override val args: ValueArgs?,
                override val rPar: Keyword.RPar?,
                override var tag: Any? = null,
            ) : Node, WithValueArgs
        }

        /**
         * Common interface for keyword modifiers.
         */
        sealed interface KeywordModifier : Modifier, Keyword
    }

    /**
     * AST node that represents a context receiver. The node corresponds to KtContextReceiverList.
     *
     * @property receiverTypes receiver types.
     */
    data class ContextReceiver(
        val lPar: Keyword.LPar,
        val receiverTypes: ContextReceiverTypes,
        val rPar: Keyword.RPar,
        override var tag: Any? = null,
    ) : Node

    /**
     * Virtual AST node that represents a list of context receiver types.
     */
    data class ContextReceiverTypes(
        override val elements: List<Type>,
        override val trailingComma: Keyword.Comma?,
        override var tag: Any? = null,
    ) : CommaSeparatedNodeList<Type>()

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
            ) : CommaSeparatedNodeList<TypeConstraint>() {
                override val trailingComma: Keyword.Comma? = null // Trailing comma is not allowed.
            }

            /**
             * AST node corresponds to KtTypeConstraint.
             *
             * @property annotationSets list of annotation sets.
             * @property name name of this type constraint.
             * @property type type of this type constraint.
             */
            data class TypeConstraint(
                override val annotationSets: List<Modifier.AnnotationSet>,
                val name: Expression.NameExpression,
                val type: Type,
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
            val lBracket: Keyword.LBracket,
            val contractEffects: ContractEffects,
            val rBracket: Keyword.RBracket,
            override var tag: Any? = null,
        ) : PostModifier {
            /**
             * AST node corresponds to KtContractEffectList.
             */
            data class ContractEffects(
                override val elements: List<Expression>,
                override val trailingComma: Keyword.Comma?,
                override var tag: Any? = null,
            ) : CommaSeparatedNodeList<Expression>()
        }
    }

    /**
     * Common interface for keywords.
     */
    sealed interface Keyword : SimpleTextNode {
        /**
         * Common interface for val or var keywords.
         */
        sealed interface ValOrVarKeyword : Keyword

        data class Package(override var tag: Any? = null) : Keyword {
            override val text = "package"
        }

        data class Import(override var tag: Any? = null) : Keyword {
            override val text = "import"
        }

        data class Class(override var tag: Any? = null) : Keyword,
            Declaration.ClassDeclaration.ClassDeclarationKeyword {
            override val text = "class"
        }

        data class Object(override var tag: Any? = null) : Keyword,
            Declaration.ClassDeclaration.ClassDeclarationKeyword {
            override val text = "object"
        }

        data class Interface(override var tag: Any? = null) : Keyword,
            Declaration.ClassDeclaration.ClassDeclarationKeyword {
            override val text = "interface"
        }

        data class Constructor(override var tag: Any? = null) : Keyword {
            override val text = "constructor"
        }

        data class Val(override var tag: Any? = null) : Keyword, ValOrVarKeyword {
            override val text = "val"
        }

        data class Var(override var tag: Any? = null) : Keyword, ValOrVarKeyword {
            override val text = "var"
        }

        data class For(override var tag: Any? = null) : Keyword {
            override val text = "for"
        }

        data class While(override var tag: Any? = null) : Keyword {
            override val text = "while"
        }

        data class Do(override var tag: Any? = null) : Keyword {
            override val text = "do"
        }

        data class If(override var tag: Any? = null) : Keyword {
            override val text = "if"
        }

        data class Else(override var tag: Any? = null) : Keyword {
            override val text = "else"
        }

        data class Catch(override var tag: Any? = null) : Keyword {
            override val text = "catch"
        }

        data class When(override var tag: Any? = null) : Keyword {
            override val text = "when"
        }

        data class By(override var tag: Any? = null) : Keyword {
            override val text = "by"
        }

        data class Contract(override var tag: Any? = null) : Keyword {
            override val text = "contract"
        }

        data class Where(override var tag: Any? = null) : Keyword {
            override val text = "where"
        }

        data class Field(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text = "field"
        }

        data class File(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text = "file"
        }

        data class Property(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text = "property"
        }

        data class Get(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text = "get"
        }

        data class Set(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text = "set"
        }

        data class Receiver(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text = "receiver"
        }

        data class Param(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text = "param"
        }

        data class SetParam(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text = "setparam"
        }

        data class Delegate(override var tag: Any? = null) : Keyword, Modifier.AnnotationSet.AnnotationTarget {
            override val text = "delegate"
        }

        data class Equal(override var tag: Any? = null) : Keyword {
            override val text = "="
        }

        data class Comma(override var tag: Any? = null) : Keyword {
            override val text = ","
        }

        data class LPar(override var tag: Any? = null) : Keyword {
            override val text = "("
        }

        data class RPar(override var tag: Any? = null) : Keyword {
            override val text = ")"
        }

        data class LBracket(override var tag: Any? = null) : Keyword {
            override val text = "["
        }

        data class RBracket(override var tag: Any? = null) : Keyword {
            override val text = "]"
        }

        data class LBrace(override var tag: Any? = null) : Keyword {
            override val text = "{"
        }

        data class RBrace(override var tag: Any? = null) : Keyword {
            override val text = "}"
        }

        data class At(override var tag: Any? = null) : Keyword {
            override val text = "@"
        }

        data class Asterisk(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "*"
        }

        data class Slash(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "/"
        }

        data class Percent(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "%"
        }

        data class Plus(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator,
            Expression.UnaryExpression.UnaryOperator {
            override val text = "+"
        }

        data class Minus(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator,
            Expression.UnaryExpression.UnaryOperator {
            override val text = "-"
        }

        data class In(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator,
            Modifier.KeywordModifier,
            Expression.WhenExpression.WhenConditionRangeOperator {
            override val text = "in"
        }

        data class NotIn(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator,
            Expression.WhenExpression.WhenConditionRangeOperator {
            override val text = "!in"
        }

        data class Greater(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = ">"
        }

        data class GreaterEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = ">="
        }

        data class Less(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "<"
        }

        data class LessEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "<="
        }

        data class EqualEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "=="
        }

        data class NotEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "!="
        }

        data class AsteriskEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "*="
        }

        data class SlashEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "/="
        }

        data class PercentEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "%="
        }

        data class PlusEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "+="
        }

        data class MinusEqual(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "-="
        }

        data class OrOr(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "||"
        }

        data class AndAnd(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "&&"
        }

        data class QuestionColon(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "?:"
        }

        data class DotDot(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = ".."
        }

        data class DotDotLess(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "..<"
        }

        data class Dot(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "."
        }

        data class QuestionDot(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "?."
        }

        data class Question(override var tag: Any? = null) : Keyword, Expression.BinaryExpression.BinaryOperator {
            override val text = "?"
        }

        data class PlusPlus(override var tag: Any? = null) : Keyword, Expression.UnaryExpression.UnaryOperator {
            override val text = "++"
        }

        data class MinusMinus(override var tag: Any? = null) : Keyword, Expression.UnaryExpression.UnaryOperator {
            override val text = "--"
        }

        data class Not(override var tag: Any? = null) : Keyword, Expression.UnaryExpression.UnaryOperator {
            override val text = "!"
        }

        data class NotNot(override var tag: Any? = null) : Keyword, Expression.UnaryExpression.UnaryOperator {
            override val text = "!!"
        }

        data class As(override var tag: Any? = null) : Keyword, Expression.BinaryTypeExpression.BinaryTypeOperator {
            override val text = "as"
        }

        data class AsQuestion(override var tag: Any? = null) : Keyword,
            Expression.BinaryTypeExpression.BinaryTypeOperator {
            override val text = "as?"
        }

        data class Colon(override var tag: Any? = null) : Keyword, Expression.BinaryTypeExpression.BinaryTypeOperator {
            override val text = ":"
        }

        data class Is(override var tag: Any? = null) : Keyword, Expression.BinaryTypeExpression.BinaryTypeOperator,
            Expression.WhenExpression.WhenConditionTypeOperator {
            override val text = "is"
        }

        data class NotIs(override var tag: Any? = null) : Keyword, Expression.BinaryTypeExpression.BinaryTypeOperator,
            Expression.WhenExpression.WhenConditionTypeOperator {
            override val text = "!is"
        }

        data class Abstract(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "abstract"
        }

        data class Final(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "final"
        }

        data class Open(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "open"
        }

        data class Annotation(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "annotation"
        }

        data class Sealed(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "sealed"
        }

        data class Data(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "data"
        }

        data class Override(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "override"
        }

        data class LateInit(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "lateinit"
        }

        data class Inner(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "inner"
        }

        data class Enum(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "enum"
        }

        data class Companion(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "companion"
        }

        data class Value(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "value"
        }

        data class Private(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "private"
        }

        data class Protected(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "protected"
        }

        data class Public(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "public"
        }

        data class Internal(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "internal"
        }

        data class Out(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "out"
        }

        data class Noinline(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "noinline"
        }

        data class CrossInline(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "crossinline"
        }

        data class Vararg(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "vararg"
        }

        data class Reified(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "reified"
        }

        data class TailRec(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "tailrec"
        }

        data class Operator(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "operator"
        }

        data class Infix(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "infix"
        }

        data class Inline(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "inline"
        }

        data class External(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "external"
        }

        data class Suspend(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "suspend"
        }

        data class Const(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "const"
        }

        data class Fun(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "fun"
        }

        data class Actual(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "actual"
        }

        data class Expect(override var tag: Any? = null) : Keyword, Modifier.KeywordModifier {
            override val text = "expect"
        }
    }

    /**
     * Common interface for extra nodes.
     *
     * @property text string representation of the node.
     */
    sealed interface Extra : SimpleTextNode {
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
            override var tag: Any? = null,
        ) : Extra {
            override val text = ";"
        }
    }
}