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
        val modifiers: List<Modifier>
        override val annotationSets: List<Modifier.AnnotationSet>
            get() = modifiers.mapNotNull { it as? Modifier.AnnotationSet }
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
     * Common interface for AST nodes that have type parameters.
     *
     * @property lAngle left angle bracket of the type parameters if exists, otherwise `null`.
     * @property typeParams list of type parameters.
     * @property rAngle right angle bracket of the type parameters if exists, otherwise `null`.
     */
    interface WithTypeParams {
        val lAngle: Keyword.Less?
        val typeParams: List<TypeParam>
        val rAngle: Keyword.Greater?
    }

    /**
     * Common interface for AST nodes that have function parameters.
     *
     * @property lPar left parenthesis of the function parameters if exists, otherwise `null`.
     * @property params list of function parameters.
     * @property rPar right parenthesis of the function parameters if exists, otherwise `null`.
     */
    interface WithFunctionParams {
        val lPar: Keyword.LPar?
        val params: List<FunctionParam>
        val rPar: Keyword.RPar?
    }

    /**
     * Common interface for AST nodes that have type arguments.
     *
     * @property lAngle left angle bracket of the type arguments if exists, otherwise `null`.
     * @property typeArgs list of type arguments.
     * @property rAngle right angle bracket of the type arguments if exists, otherwise `null`.
     */
    interface WithTypeArgs {
        val lAngle: Keyword.Less?
        val typeArgs: List<TypeArg>
        val rAngle: Keyword.Greater?
    }

    /**
     * Common interface for AST nodes that have value arguments.
     *
     * @property lPar left parenthesis of the value arguments if exists, otherwise `null`.
     * @property args list of value arguments.
     * @property rPar right parenthesis of the value arguments if exists, otherwise `null`.
     */
    interface WithValueArgs {
        val lPar: Keyword.LPar?
        val args: List<ValueArg>
        val rPar: Keyword.RPar?
    }

    /**
     * Common interface for AST nodes that have statements.
     *
     * @property statements list of statements.
     */
    interface WithStatements {
        val statements: List<Statement>
    }

    /**
     * Common interface for AST nodes that have declarations.
     *
     * @property declarations list of declarations.
     */
    interface WithDeclarations {
        val declarations: List<Declaration>
    }

    /**
     * Common interface for AST nodes that can be root node.
     *
     * @property packageDirective package directive if exists, otherwise `null`.
     * @property importDirectives list of import directives.
     */
    sealed interface KotlinEntry : Node, WithAnnotationSets {
        val packageDirective: PackageDirective?
        val importDirectives: List<ImportDirective>
    }

    /**
     * AST node that represents whole Kotlin file. Tha node corresponds to KtFile.
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
    ) : Node, KotlinEntry, WithDeclarations

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
     * AST node that represents a package directive. The node corresponds to KtPackageDirective.
     *
     * @property names list of names separated by dots.
     */
    data class PackageDirective(
        val names: List<Expression.NameExpression>,
        override var tag: Any? = null,
    ) : Node

    /**
     * AST node that represents an import directive. The node corresponds to KtImportDirective.
     *
     * @property names list of names separated by dots.
     * @property aliasName import alias name if exists, otherwise `null`.
     */
    data class ImportDirective(
        val names: List<Expression.NameExpression>,
        val aliasName: Expression.NameExpression?,
        override var tag: Any? = null,
    ) : Node

    /**
     * Common interface for [Declaration], [Expression] and loop statements.
     */
    sealed interface Statement : Node {

        /**
         * AST node that represents a for statement. The node corresponds to KtForExpression.
         *
         * ```
         * for ([loopParam] in [loopRange]) [body]
         * ```
         *
         * @property lPar left parenthesis of the loop condition.
         * @property loopParam loop parameter before `in` keyword.
         * @property inKeyword `in` keyword.
         * @property loopRange loop range expression after `in` keyword.
         * @property rPar right parenthesis of the loop condition.
         * @property body body expression.
         */
        data class ForStatement(
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
         *
         * @property lPar left parenthesis of the condition.
         * @property condition condition expression.
         * @property rPar right parenthesis of the condition.
         * @property body body expression.
         */
        sealed interface WhileStatementBase : Statement {
            val lPar: Keyword.LPar
            val condition: Expression
            val rPar: Keyword.RPar
            val body: Expression
        }

        /**
         * AST node that represents a while statement. The node corresponds to KtWhileExpression.
         */
        data class WhileStatement(
            override val lPar: Keyword.LPar,
            override val condition: Expression,
            override val rPar: Keyword.RPar,
            override val body: Expression,
            override var tag: Any? = null,
        ) : WhileStatementBase

        /**
         * AST node that represents a do-while statement. The node corresponds to KtDoWhileExpression.
         */
        data class DoWhileStatement(
            override val body: Expression,
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
         * AST node that represents a class, object or interface declaration. The node corresponds to KtClassOrObject.
         *
         * @property modifiers list of modifiers.
         * @property classDeclarationKeyword class declaration keyword.
         * @property name name of the class. If the object is anonymous, the name is `null`.
         * @property lAngle left angle bracket of the type parameters.
         * @property typeParams list of type parameters.
         * @property rAngle right angle bracket of the type parameters.
         * @property primaryConstructor primary constructor if exists, otherwise `null`.
         * @property classParents list of class parents.
         * @property typeConstraintSet type constraint set if exists, otherwise `null`.
         * @property classBody class body if exists, otherwise `null`.
         */
        data class ClassDeclaration(
            override val modifiers: List<Modifier>,
            val classDeclarationKeyword: ClassDeclarationKeyword,
            val name: Expression.NameExpression?,
            override val lAngle: Keyword.Less?,
            override val typeParams: List<TypeParam>,
            override val rAngle: Keyword.Greater?,
            val primaryConstructor: PrimaryConstructor?,
            val classParents: List<ClassParent>,
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
            val isCompanion = modifiers.any { it is Keyword.Companion }

            /**
             * Returns `true` if the node has an enum modifier, `false` otherwise.
             */
            val isEnum = modifiers.any { it is Keyword.Enum }

            /**
             * Common interface for keyword nodes that are used to declare a class.
             */
            sealed interface ClassDeclarationKeyword : Keyword

            /**
             * AST node that represents a parent of the class. The node corresponds to KtSuperTypeListEntry.
             *
             * @property type type of the parent.
             * @property lPar left parenthesis of the value arguments.
             * @property args list of value arguments of the parent call.
             * @property rPar right parenthesis of the value arguments.
             * @property expression expression of the delegation if exists, otherwise `null`.
             */
            sealed interface ClassParent : Node, WithValueArgs {
                val type: Type
                val expression: Expression?
            }

            /**
             * ClassParent node that represents constructor invocation. The node corresponds to KtSuperTypeCallEntry.
             *
             * @property type type of the parent.
             * @property args list of value arguments of the parent call.
             * @property expression always `null`.
             */
            data class ConstructorClassParent(
                override val type: Type.SimpleType,
                override val lPar: Keyword.LPar,
                override val args: List<ValueArg>,
                override val rPar: Keyword.RPar,
                override var tag: Any? = null,
            ) : ClassParent {
                override val expression: Expression? = null
            }

            /**
             * ClassParent node that represents explicit delegation. The node corresponds to KtDelegatedSuperTypeEntry.
             *
             * @property type type of the interface delegated to.
             * @property args always empty list.
             * @property expression expression of the delegation.
             */
            data class DelegationClassParent(
                override val type: Type,
                override val expression: Expression,
                override var tag: Any? = null,
            ) : ClassParent {
                override val lPar: Keyword.LPar? = null
                override val args: List<ValueArg> = listOf()
                override val rPar: Keyword.RPar? = null
            }

            /**
             * ClassParent node that represents just a type. The node corresponds to KtSuperTypeEntry.
             *
             * @property type type of the parent.
             * @property lPar always `null`.
             * @property args always empty list.
             * @property rPar always `null`.
             * @property expression always `null`.
             */
            data class TypeClassParent(
                override val type: Type,
                override var tag: Any? = null,
            ) : ClassParent {
                override val lPar: Keyword.LPar? = null
                override val args: List<ValueArg> = listOf()
                override val rPar: Keyword.RPar? = null
                override val expression: Expression? = null
            }

            /**
             * AST node corresponds to KtPrimaryConstructor.
             *
             * @property modifiers list of modifiers.
             * @property constructorKeyword `constructor` keyword if exists, otherwise `null`.
             * @property params list of parameters of the constructor.
             */
            data class PrimaryConstructor(
                override val modifiers: List<Modifier>,
                val constructorKeyword: Keyword.Constructor?,
                override val lPar: Keyword.LPar?,
                override val params: List<FunctionParam>,
                override val rPar: Keyword.RPar?,
                override var tag: Any? = null,
            ) : Node, WithModifiers, WithFunctionParams

            /**
             * AST node corresponds to KtClassBody.
             *
             * @property enumEntries list of enum entries.
             * @property declarations list of declarations.
             */
            data class ClassBody(
                val enumEntries: List<EnumEntry>,
                override val declarations: List<Declaration>,
                override var tag: Any? = null,
            ) : Node, WithDeclarations {

                /**
                 * AST node corresponds to KtEnumEntry.
                 *
                 * @property modifiers list of modifiers.
                 * @property name name of the enum entry.
                 * @property args list of value arguments of the enum entry.
                 * @property classBody class body of the enum entry if exists, otherwise `null`.
                 */
                data class EnumEntry(
                    override val modifiers: List<Modifier>,
                    val name: Expression.NameExpression,
                    override val lPar: Keyword.LPar?,
                    override val args: List<ValueArg>,
                    override val rPar: Keyword.RPar?,
                    val classBody: ClassBody?,
                    override var tag: Any? = null,
                ) : Node, WithModifiers, WithValueArgs

                /**
                 * AST node that represents an init block, a.k.a. initializer. The node corresponds to KtAnonymousInitializer.
                 *
                 * @property block block of the initializer.
                 */
                data class Initializer(
                    val block: Expression.BlockExpression,
                    override var tag: Any? = null,
                ) : Declaration

                /**
                 * AST node corresponds to KtSecondaryConstructor.
                 *
                 * @property modifiers list of modifiers.
                 * @property constructorKeyword `constructor` keyword.
                 * @property params list of parameters of the secondary constructor.
                 * @property delegationCall delegation call expression of the secondary constructor if exists, otherwise `null`.
                 * @property block block of the constructor if exists, otherwise `null`.
                 */
                data class SecondaryConstructor(
                    override val modifiers: List<Modifier>,
                    val constructorKeyword: Keyword.Constructor,
                    override val lPar: Keyword.LPar?,
                    override val params: List<FunctionParam>,
                    override val rPar: Keyword.RPar?,
                    val delegationCall: Expression.CallExpression?,
                    val block: Expression.BlockExpression?,
                    override var tag: Any? = null,
                ) : Declaration, WithModifiers, WithFunctionParams
            }
        }

        /**
         * AST node that represents a function declaration. The node corresponds to KtNamedFunction.
         *
         * @property modifiers list of modifiers.
         * @property typeParams list of type parameters of the function.
         * @property receiverType receiver type of the function if exists, otherwise `null`.
         * @property name name of the function. If the function is anonymous, the name is `null`.
         * @property params list of parameters of the function.
         * @property returnType return type of the function if exists, otherwise `null`.
         * @property postModifiers post-modifiers of the function.
         * @property body body of the function if exists, otherwise `null`.
         */
        data class FunctionDeclaration(
            override val modifiers: List<Modifier>,
            override val lAngle: Keyword.Less?,
            override val typeParams: List<TypeParam>,
            override val rAngle: Keyword.Greater?,
            val receiverType: Type?,
            val name: Expression.NameExpression?,
            override val lPar: Keyword.LPar?,
            override val params: List<FunctionParam>,
            override val rPar: Keyword.RPar?,
            val returnType: Type?,
            override val postModifiers: List<PostModifier>,
            val body: Expression?,
            override var tag: Any? = null,
        ) : Declaration, WithModifiers, WithTypeParams, WithFunctionParams, WithPostModifiers

        /**
         * AST node that represents a property declaration. The node corresponds to KtProperty or KtDestructuringDeclaration.
         *
         * @property modifiers list of modifiers.
         * @property valOrVarKeyword `val` or `var` keyword.
         * @property typeParams list of type parameters of the property.
         * @property receiverType receiver type of the property if exists, otherwise `null`.
         * @property lPar `(` keyword if exists, otherwise `null`. When there are two or more variables, the keyword must exist.
         * @property variables variables of the property. Always at least one, more than one means destructuring.
         * @property rPar `)` keyword if exists, otherwise `null`. When there are two or more variables, the keyword must exist.
         * @property typeConstraintSet type constraint set of the property if exists, otherwise `null`.
         * @property initializerExpression initializer expression of the property if exists, otherwise `null`. When the property has a delegate, the initializer must be `null`.
         * @property delegateExpression property delegate of the property if exists, otherwise `null`. When the property has an initializer, the delegate must be `null`.
         * @property accessors accessors of the property.
         */
        data class PropertyDeclaration(
            override val modifiers: List<Modifier>,
            val valOrVarKeyword: Keyword.ValOrVarKeyword,
            override val lAngle: Keyword.Less?,
            override val typeParams: List<TypeParam>,
            override val rAngle: Keyword.Greater?,
            val receiverType: Type?,
            val lPar: Keyword.LPar?,
            val variables: List<Variable>,
            val rPar: Keyword.RPar?,
            val typeConstraintSet: PostModifier.TypeConstraintSet?,
            val initializerExpression: Expression?,
            val delegateExpression: Expression?,
            val accessors: List<Accessor>,
            override var tag: Any? = null,
        ) : Declaration, WithModifiers, WithTypeParams {
            init {
                require(initializerExpression == null || delegateExpression == null) {
                    "Either initializerExpression or delegateExpression must be null"
                }
                if (variables.size >= 2) {
                    require(lPar != null && rPar != null) { "lPar and rPar are required when there are multiple variables" }
                }
            }

            /**
             * AST node corresponds to KtPropertyAccessor.
             *
             * @property lPar left parenthesis if exists, otherwise `null`.
             * @property rPar right parenthesis if exists, otherwise `null`.
             */
            sealed interface Accessor : Node, WithModifiers, WithPostModifiers {
                val lPar: Keyword.LPar?
                val rPar: Keyword.RPar?
                val body: Expression?
            }

            /**
             * AST node that represents a property getter.
             *
             * @property modifiers list of modifiers.
             * @property type return type of the getter if exists, otherwise `null`.
             * @property postModifiers post-modifiers of the getter.
             * @property body body of the getter if exists, otherwise `null`.
             */
            data class Getter(
                override val modifiers: List<Modifier>,
                override val lPar: Keyword.LPar?,
                override val rPar: Keyword.RPar?,
                val type: Type?,
                override val postModifiers: List<PostModifier>,
                override val body: Expression?,
                override var tag: Any? = null,
            ) : Accessor

            /**
             * AST node that represents a property setter.
             *
             * @property modifiers list of modifiers.
             * @property params list of parameters of the setter.
             * @property postModifiers post-modifiers of the setter.
             * @property body body of the setter if exists, otherwise `null`.
             */
            data class Setter(
                override val modifiers: List<Modifier>,
                override val lPar: Keyword.LPar?,
                val params: List<LambdaParam>,
                override val rPar: Keyword.RPar?,
                override val postModifiers: List<PostModifier>,
                override val body: Expression?,
                override var tag: Any? = null,
            ) : Accessor {
                init {
                    if (params.isEmpty()) {
                        require(body == null) { "body must be null when params is empty" }
                    } else {
                        require(body != null) { "body must be non-null when params is non-empty" }
                    }
                }
            }
        }

        /**
         * AST node that represents a type alias declaration. The node corresponds to KtTypeAlias.
         *
         * @property modifiers list of modifiers.
         * @property name name of the type alias.
         * @property typeParams list of type parameters of the type alias.
         * @property type existing type of the type alias.
         */
        data class TypeAliasDeclaration(
            override val modifiers: List<Modifier>,
            val name: Expression.NameExpression,
            override val lAngle: Keyword.Less?,
            override val typeParams: List<TypeParam>,
            override val rAngle: Keyword.Greater?,
            val type: Type,
            override var tag: Any? = null,
        ) : Declaration, WithModifiers, WithTypeParams
    }

    /**
     * Common interface for AST nodes that represent types.
     */
    sealed interface Type : Node, WithModifiers {

        /**
         * AST node that represents a type surrounded by parentheses. The node corresponds to KtTypeReference or KtNullableType having `(` and `)` as children.
         *
         * @property modifiers list of modifiers.
         * @property lPar `(` symbol.
         * @property innerType inner type.
         * @property rPar `)` symbol.
         */
        data class ParenthesizedType(
            override val modifiers: List<Modifier>,
            val lPar: Keyword.LPar,
            val innerType: Type,
            val rPar: Keyword.RPar,
            override var tag: Any? = null,
        ) : Type

        /**
         * AST node that represents a nullable type. The node corresponds to KtNullableType and modifiers of its parent.
         *
         * @property modifiers list of modifiers.
         * @property innerType inner type.
         * @property questionMark `?` symbol.
         */
        data class NullableType(
            override val modifiers: List<Modifier>,
            val innerType: Type,
            val questionMark: Keyword.Question,
            override var tag: Any? = null,
        ) : Type

        /**
         * AST node that represents a simple named type. The node corresponds to KtUserType and modifiers of its parent.
         *
         * @property modifiers list of modifiers.
         * @property pieces list of pieces. The piece represents a pair of a name and type arguments.
         */
        data class SimpleType(
            override val modifiers: List<Modifier>,
            val pieces: List<SimpleTypePiece>,
            override var tag: Any? = null,
        ) : Type {
            /**
             * AST node corresponds to KtUserType used as a piece.
             *
             * @property name name of the piece.
             */
            data class SimpleTypePiece(
                val name: Expression.NameExpression,
                override val lAngle: Keyword.Less?,
                override val typeArgs: List<TypeArg>,
                override val rAngle: Keyword.Greater?,
                override var tag: Any? = null,
            ) : Node, WithTypeArgs
        }

        /**
         * AST node that represents a dynamic type. The node corresponds to KtDynamicType and modifiers of its parent.
         *
         * @property modifiers list of modifiers.
         */
        data class DynamicType(
            override val modifiers: List<Modifier>,
            override var tag: Any? = null,
        ) : Type

        /**
         * AST node that represents a function type. The node corresponds to KtFunctionType and modifiers of its parent.
         *
         * @property modifiers list of modifiers.
         * @property contextReceiver context receivers if exists, otherwise `null`.
         * @property receiverType receiver type if exists, otherwise `null`.
         * @property lPar left parenthesis if exists, otherwise `null`.
         * @property params list of parameters of the function type.
         * @property rPar right parenthesis if exists, otherwise `null`.
         * @property returnType return type of the function type.
         */
        data class FunctionType(
            override val modifiers: List<Modifier>,
            val contextReceiver: ContextReceiver?,
            val receiverType: Type?,
            val lPar: Keyword.LPar?,
            val params: List<FunctionTypeParam>,
            val rPar: Keyword.RPar?,
            val returnType: Type,
            override var tag: Any? = null,
        ) : Type {

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
         * AST node that represents an if expression. The node corresponds to KtIfExpression.
         *
         * @property lPar left parenthesis of the condition.
         * @property condition condition expression.
         * @property rPar right parenthesis of the condition.
         * @property body body expression.
         * @property elseBody else body expression if exists, otherwise `null`.
         */
        data class IfExpression(
            val lPar: Keyword.LPar,
            val condition: Expression,
            val rPar: Keyword.RPar,
            val body: Expression,
            val elseBody: Expression?,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node that represents a try expression. The node corresponds to KtTryExpression.
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
             * AST node that represents a catch clause. The node corresponds to KtCatchClause.
             *
             * @property params list of parameters of the catch clause.
             * @property block block expression.
             */
            data class CatchClause(
                override val lPar: Keyword.LPar,
                override val params: List<FunctionParam>,
                override val rPar: Keyword.RPar,
                val block: BlockExpression,
                override var tag: Any? = null,
            ) : Node, WithFunctionParams {
                init {
                    require(params.isNotEmpty()) { "catch clause must have at least one parameter" }
                }
            }
        }

        /**
         * AST node that represents a when expression. The node corresponds to KtWhenExpression.
         *
         * @property whenKeyword keyword of when expression.
         * @property subject subject of when expression if exists, otherwise `null`.
         * @property whenBranches list of when branches.
         */
        data class WhenExpression(
            val whenKeyword: Keyword.When,
            val subject: WhenSubject?,
            val whenBranches: List<WhenBranch>,
            override var tag: Any? = null,
        ) : Expression {
            /**
             * AST node that represents a subject of when expression. The node corresponds to a part of KtWhenExpression.
             *
             * @property lPar left parenthesis of when subject.
             * @property annotationSets list of annotation sets.
             * @property valKeyword `val` keyword if exists, otherwise `null`.
             * @property variable variable of when subject if exists, otherwise `null`.
             * @property expression expression of when subject.
             * @property rPar right parenthesis of when subject.
             */
            data class WhenSubject(
                val lPar: Keyword.LPar,
                override val annotationSets: List<Modifier.AnnotationSet>,
                val valKeyword: Keyword.Val?,
                val variable: Variable?,
                val expression: Expression,
                val rPar: Keyword.RPar,
                override var tag: Any? = null,
            ) : Node, WithAnnotationSets

            /**
             * Common interface for when branches. The node corresponds to KtWhenEntry.
             *
             * @property whenConditions list of conditions.
             * @property arrow arrow symbol.
             * @property body body expression of this branch.
             */
            sealed interface WhenBranch : Node {
                val whenConditions: List<WhenCondition>
                val arrow: Keyword.Arrow
                val body: Expression
            }

            /**
             * AST node that represents a when branch with conditions.
             *
             * @property whenConditions non-empty list of conditions.
             * @property body body expression of this branch.
             */
            data class ConditionalWhenBranch(
                override val whenConditions: List<WhenCondition>,
                override val arrow: Keyword.Arrow,
                override val body: Expression,
                override var tag: Any? = null,
            ) : WhenBranch {
                init {
                    require(whenConditions.isNotEmpty()) { "whenConditions must not be empty" }
                }
            }

            /**
             * AST node that represents a when branch with else keyword.
             *
             * @property whenConditions always empty list.
             * @property body body expression of this branch.
             */
            data class ElseWhenBranch(
                override val arrow: Keyword.Arrow,
                override val body: Expression,
                override var tag: Any? = null,
            ) : WhenBranch {
                override val whenConditions = listOf<WhenCondition>()
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
             * Common interface for when conditions. The node corresponds to KtWhenCondition.
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
             * AST node that represents a when condition using expression. The node corresponds to KtWhenConditionWithExpression.
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
             * AST node that represents a when condition using range. The node corresponds to KtWhenConditionInRange.
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
             * AST node that represents a when condition using type. The node corresponds to KtWhenConditionIsPattern.
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
         * AST node that represents a throw expression. The node corresponds to KtThrowExpression.
         *
         * @property expression expression to be thrown.
         */
        data class ThrowExpression(
            val expression: Expression,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node that represents a return expression. The node corresponds to KtReturnExpression.
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
         * AST node that represents a continue expression. The node corresponds to KtContinueExpression.
         *
         * @property label label of this continue expression if exists, otherwise `null`.
         */
        data class ContinueExpression(
            override val label: NameExpression?,
            override var tag: Any? = null,
        ) : Expression, WithLabel

        /**
         * AST node that represents a break expression. The node corresponds to KtBreakExpression.
         *
         * @property label label of this break expression if exists, otherwise `null`.
         */
        data class BreakExpression(
            override val label: NameExpression?,
            override var tag: Any? = null,
        ) : Expression, WithLabel

        /**
         * AST node that represents a block expression. The node corresponds to KtBlockExpression.
         *
         * @property statements list of statements.
         */
        data class BlockExpression(
            override val statements: List<Statement>,
            override var tag: Any? = null,
        ) : Expression, WithStatements

        /**
         * AST node that represents a call expression. The node corresponds to KtCallElement.
         *
         * @property calleeExpression callee expression.
         * @property typeArgs list of type arguments.
         * @property args list of value arguments.
         * @property lambdaArg lambda argument if exists, otherwise `null`.
         */
        data class CallExpression(
            val calleeExpression: Expression,
            override val lAngle: Keyword.Less?,
            override val typeArgs: List<TypeArg>,
            override val rAngle: Keyword.Greater?,
            override val lPar: Keyword.LPar?,
            override val args: List<ValueArg>,
            override val rPar: Keyword.RPar?,
            val lambdaArg: LambdaArg?,
            override var tag: Any? = null,
        ) : Expression, WithTypeArgs, WithValueArgs {
            /**
             * AST node that represents a lambda argument. The node corresponds to KtLambdaArgument.
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
         * AST node that represents a lambda expression. The node corresponds to KtLambdaExpression.
         *
         * [LambdaExpression] = { [LambdaParam], [LambdaParam] -> [Statement] [Statement]... }
         *
         * @property params list of parameters in the lambda expression.
         * @property arrow arrow symbol of the lambda expression if exists, otherwise `null`.
         * @property statements list of statements in the lambda expression.
         */
        data class LambdaExpression(
            val params: List<LambdaParam>,
            val arrow: Keyword.Arrow?,
            override val statements: List<Statement>,
            override var tag: Any? = null,
        ) : Expression, WithStatements

        /**
         * AST node that represents a binary expression. The node corresponds to KtBinaryExpression or KtQualifiedExpression.
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
         * AST node that represents a prefix unary expression. The node corresponds to KtPrefixExpression.
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
         * AST node that represents a postfix unary expression. The node corresponds to KtPostfixExpression.
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
         * AST node that represents a binary type expression. The node corresponds to KtBinaryExpressionWithTypeRHS or KtIsExpression.
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
         * Common interface for [CallableReferenceExpression] and [ClassLiteralExpression]. The node corresponds to KtDoubleColonExpression.
         *
         * @property lhs left-hand side expression if exists, otherwise `null`.
         * @property questionMarks list of question marks after [lhs].
         */
        sealed interface DoubleColonExpression : Expression {
            val lhs: Expression?
            val questionMarks: List<Keyword.Question>
        }

        /**
         * AST node that represents a callable reference expression. The node corresponds to KtCallableReferenceExpression.
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
         * AST node that represents a class literal expression. The node corresponds to KtClassLiteralExpression.
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
         * AST node that represents an expression surrounded by parentheses. The node corresponds to KtParenthesizedExpression.
         *
         * @property innerExpression expression inside parentheses.
         */
        data class ParenthesizedExpression(
            val innerExpression: Expression,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node that represents a string literal expression. The node corresponds to KtStringTemplateExpression.
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
             * Common interface for string entries. The node corresponds to KtStringTemplateEntry.
             */
            sealed interface StringEntry : Node

            /**
             * AST node that represents a literal string entry, i.e. a normal string entry. The node corresponds to KtLiteralStringTemplateEntry.
             *
             * @property text string of this entry.
             */
            data class LiteralStringEntry(
                override val text: String,
                override var tag: Any? = null,
            ) : StringEntry, SimpleTextNode

            /**
             * AST node that represents an escape string entry that starts with backslash. The node corresponds to KtEscapeStringTemplateEntry.
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
             * AST node that represents a template string entry with expression. The node corresponds to KtStringTemplateEntryWithExpression.
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
         * Common interface for constant literal expression. The AST node corresponds to KtConstantExpression.
         *
         * @property text string representation of this constant.
         */
        sealed interface ConstantLiteralExpression : Expression, SimpleTextNode {
            override val text: String
        }

        /**
         * AST node that represents a boolean literal expression. The node corresponds to KtConstantExpression whose expressionType is KtNodeTypes.BOOLEAN_CONSTANT.
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
         * AST node that represents a character literal expression. The node corresponds to KtConstantExpression whose expressionType is KtNodeTypes.CHARACTER_CONSTANT.
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
         * AST node that represents an integer literal expression. The node corresponds to KtConstantExpression whose expressionType is KtNodeTypes.INTEGER_CONSTANT.
         *
         * @property text string representation of this constant.
         */
        data class IntegerLiteralExpression(
            override val text: String,
            override var tag: Any? = null,
        ) : ConstantLiteralExpression

        /**
         * AST node that represents a real number literal expression. The node corresponds to KtConstantExpression whose expressionType is KtNodeTypes.FLOAT_CONSTANT.
         *
         * @property text string representation of this constant.
         */

        data class RealLiteralExpression(
            override val text: String,
            override var tag: Any? = null,
        ) : ConstantLiteralExpression

        /**
         * AST node that represents a null literal expression. The node corresponds to KtConstantExpression whose expressionType is KtNodeTypes.NULL.
         */
        data class NullLiteralExpression(
            override var tag: Any? = null,
        ) : ConstantLiteralExpression {
            override val text: String
                get() = "null"
        }

        /**
         * AST node that represents an object literal expression. The node corresponds to KtObjectLiteralExpression.
         *
         * @property declaration class declaration of this object literal expression.
         */
        data class ObjectLiteralExpression(
            val declaration: Declaration.ClassDeclaration,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node that represents a collection literal expression. The node corresponds to KtCollectionLiteralExpression.
         *
         * @property expressions list of element expressions.
         */
        data class CollectionLiteralExpression(
            val expressions: List<Expression>,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node that represents a "this" expression. The node corresponds to KtThisExpression or KtConstructorDelegationReferenceExpression whose text is "this".
         *
         * @property label label of this expression if exists, otherwise `null`.
         */
        data class ThisExpression(
            override val label: NameExpression?,
            override var tag: Any? = null,
        ) : Expression, WithLabel

        /**
         * AST node that represents a super expression. The node corresponds to KtSuperExpression or KtConstructorDelegationReferenceExpression whose text is "super".
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
         * AST node that represents a name expression, i.e. an identifier. The node corresponds to KtValueArgumentName, KtSimpleNameExpression or PsiElement whose elementType is IDENTIFIER.
         *
         * @property text string representation of the name expression.
         */
        data class NameExpression(
            override val text: String,
            override var tag: Any? = null,
        ) : Expression, BinaryExpression.BinaryOperator

        /**
         * AST node that represents an expression prefixed by a label. The node corresponds to KtLabeledExpression.
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
         * AST node that represents an expression prefixed by annotation sets. The node corresponds to KtAnnotatedExpression.
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
         * AST node that represents an expression followed by index access. The node corresponds to KtArrayAccessExpression.
         *
         * @property expression collection expression.
         * @property indices list of index expressions.
         */
        data class IndexedAccessExpression(
            val expression: Expression,
            val indices: List<Expression>,
            override var tag: Any? = null,
        ) : Expression

        /**
         * AST node that represents an anonymous function expression. The node corresponds to KtNamedFunction in expression context.
         *
         * @property function function declaration.
         */
        data class AnonymousFunctionExpression(
            val function: Declaration.FunctionDeclaration,
            override var tag: Any? = null,
        ) : Expression
    }

    /**
     * AST node that represents a formal type parameter of a function or a class. For example, `T` in `fun <T> f()` is a type parameter. The node corresponds to KtTypeParameter.
     *
     * @property modifiers list of modifiers.
     * @property name name of the type parameter.
     * @property type type of the type parameter if exists, otherwise `null`.
     */
    data class TypeParam(
        override val modifiers: List<Modifier>,
        val name: Expression.NameExpression,
        val type: Type?,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * AST node that represents a formal function parameter of a function declaration. For example, `x: Int` in `fun f(x: Int)` is a function parameter. The node corresponds to KtParameter inside KtNamedFunction.
     *
     * @property modifiers list of modifiers.
     * @property valOrVarKeyword `val` or `var` keyword if exists, otherwise `null`.
     * @property name name of the parameter.
     * @property type type of the parameter. Can be `null` for anonymous function parameters.
     * @property defaultValue default value of the parameter if exists, otherwise `null`.
     */
    data class FunctionParam(
        override val modifiers: List<Modifier>,
        val valOrVarKeyword: Keyword.ValOrVarKeyword?,
        val name: Expression.NameExpression,
        val type: Type?,
        val defaultValue: Expression?,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * AST node that represents a formal parameter of lambda expression. For example, `x` in `{ x -> ... }` is a lambda parameter. The node corresponds to KtParameter under KtLambdaExpression.
     *
     * @property lPar left parenthesis of this parameter if exists, otherwise `null`.
     * @property variables list of variables.
     * @property rPar right parenthesis of this parameter if exists, otherwise `null`.
     * @property destructType type of destructuring if exists, otherwise `null`.
     */
    data class LambdaParam(
        val lPar: Keyword.LPar?,
        val variables: List<Variable>,
        val rPar: Keyword.RPar?,
        val destructType: Type?,
        override var tag: Any? = null,
    ) : Node {
        init {
            if (variables.size >= 2) {
                require(lPar != null && rPar != null) { "lPar and rPar are required when there are multiple variables" }
            }
        }
    }

    /**
     * AST node that represents a variable. The node corresponds to KtDestructuringDeclarationEntry, a part of KtProperty, or KtParameter whose child is IDENTIFIER.
     *
     * @property annotationSets list of annotation sets.
     * @property name name of the variable.
     * @property type type of the variable if exists, otherwise `null`.
     */
    data class Variable(
        override val annotationSets: List<Modifier.AnnotationSet>,
        val name: Expression.NameExpression,
        val type: Type?,
        override var tag: Any? = null,
    ) : Node, WithAnnotationSets

    /**
     * AST node that represents an actual type argument. For example, `Int` in `listOf<Int>()` is a type argument. The node corresponds to KtTypeProjection.
     *
     * @property modifiers list of modifiers.
     * @property type projection type. When the type argument is a star projection, this is [Type.SimpleType] that has a single [Type.SimpleType.SimpleTypePiece] whose name is "*".
     */
    data class TypeArg(
        override val modifiers: List<Modifier>,
        val type: Type,
        override var tag: Any? = null,
    ) : Node, WithModifiers

    /**
     * AST node that represents an actual value argument of a function call. For example, `foo(1, 2)` has two value arguments `1` and `2`. The node corresponds to KtValueArgument.
     *
     * @property name name of the argument if exists, otherwise `null`.
     * @property spreadOperator spread operator if exists, otherwise `null`.
     * @property expression expression of the argument.
     */
    data class ValueArg(
        val name: Expression.NameExpression?,
        val spreadOperator: Keyword.Asterisk?,
        val expression: Expression,
        override var tag: Any? = null,
    ) : Node

    /**
     * AST node that represents a context receiver. The node corresponds to KtContextReceiverList.
     *
     * @property lPar left parenthesis of the receiver types.
     * @property receiverTypes list of receiver types.
     * @property rPar right parenthesis of the receiver types.
     */
    data class ContextReceiver(
        val lPar: Keyword.LPar,
        val receiverTypes: List<Type>,
        val rPar: Keyword.RPar,
        override var tag: Any? = null,
    ) : Node

    /**
     * Common interface for modifiers.
     */
    sealed interface Modifier : Node {
        /**
         * AST node that represents an annotation sets. The node corresponds to KtAnnotation or KtAnnotationEntry not under KtAnnotation.
         *
         * @property target target keyword if exists, otherwise `null`.
         * @property lBracket left bracket symbol if exists, otherwise `null`.
         * @property annotations list of annotations.
         * @property rBracket right bracket symbol if exists, otherwise `null`.
         */
        data class AnnotationSet(
            val target: AnnotationTarget?,
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
             * AST node that represents an annotation. The node corresponds to KtAnnotationEntry under KtAnnotation or virtual AST node corresponds to KtAnnotationEntry not under KtAnnotation.
             *
             * @property type type of this annotation.
             * @property args list of value arguments.
             */
            data class Annotation(
                val type: Type.SimpleType,
                override val lPar: Keyword.LPar?,
                override val args: List<ValueArg>,
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
     * Common interface for post-modifiers.
     */
    sealed interface PostModifier : Node {
        /**
         * AST node that represents a type constraint set. The node corresponds to a pair of "where" keyword and KtTypeConstraintList.
         *
         * @property constraints type constraints.
         */
        data class TypeConstraintSet(
            val constraints: List<TypeConstraint>,
            override var tag: Any? = null,
        ) : PostModifier {

            /**
             * AST node that represents a type constraint. The node corresponds to KtTypeConstraint.
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
         * AST node that represents a contract. The node corresponds to a pair of "contract" keyword and KtContractEffectList.
         *
         * @property lBracket left bracket symbol of the contract effects.
         * @property contractEffects contract effect expressions.
         * @property rBracket right bracket symbol of the contract effects.
         */
        data class Contract(
            val lBracket: Keyword.LBracket,
            val contractEffects: List<Expression>,
            val rBracket: Keyword.RBracket,
            override var tag: Any? = null,
        ) : PostModifier
    }

    /**
     * Common interface for keywords.
     */
    sealed interface Keyword : SimpleTextNode {
        /**
         * Common interface for val or var keywords.
         */
        sealed interface ValOrVarKeyword : Keyword

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

        data class When(override var tag: Any? = null) : Keyword {
            override val text = "when"
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

        data class Arrow(override var tag: Any? = null) : Keyword {
            override val text = "->"
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
     */
    sealed interface Extra : SimpleTextNode {
        /**
         * AST node that represents a whitespace. The node corresponds to PsiWhiteSpace.
         *
         * @property text string representation of the node.
         */
        data class Whitespace(
            override val text: String,
            override var tag: Any? = null,
        ) : Extra

        /**
         * AST node that represents a comment. The node corresponds to PsiComment.
         *
         * @property text string representation of the node. It contains comment markers, e.g. "//".
         */
        data class Comment(
            override val text: String,
            override var tag: Any? = null,
        ) : Extra

        /**
         * AST node that represents a semicolon. The node corresponds to PsiElement whose elementType is SEMICOLON.
         *
         * @property text always be ";".
         */
        data class Semicolon(
            override var tag: Any? = null,
        ) : Extra {
            override val text = ";"
        }

        /**
         * AST node that represents a trailing comma of a list.
         *
         * @property text always be ",".
         */
        data class TrailingComma(
            override var tag: Any? = null,
        ) : Extra {
            override val text = ","
        }
    }
}