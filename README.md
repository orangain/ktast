# Ktast [![](https://jitpack.io/v/orangain/ktast.svg)](https://jitpack.io/#orangain/ktast) [![Java CI](https://github.com/orangain/ktast/actions/workflows/java_ci.yaml/badge.svg)](https://github.com/orangain/ktast/actions/workflows/java_ci.yaml)

ktast is a simple library that represents Kotlin source code as an Abstract Syntax Tree (AST), allowing for node
manipulation. It can parse Kotlin source code into an AST, make modifications, and write it back to the source code. The
features of the ktast are as follows:

* It supports the grammar of the latest version of Kotlin
* AST nodes are represented as immutable data classes, and the copy method can be utilized
* It can handle whitespaces and comments while maintaining them

It includes the following components:

* Hierarchically organized [node classes](https://orangain.github.io/ktast/latest/api/ast/ktast.ast/-node/index.html)
* A [Parser](https://orangain.github.io/ktast/latest/api/ast-psi/ktast.ast.psi/-parser/index.html) to parse source code,
  and a [Converter](https://orangain.github.io/ktast/latest/api/ast-psi/ktast.ast.psi/-converter/index.html) to
  transform the result into AST nodes
* A [Writer](https://orangain.github.io/ktast/latest/api/ast/ktast.ast/-writer/index.html) to write back the AST nodes
* A [Visitor](https://orangain.github.io/ktast/latest/api/ast/ktast.ast/-visitor/index.html) to traverse a node tree and
  a [MutableVisitor](https://orangain.github.io/ktast/latest/api/ast/ktast.ast/-mutable-visitor/index.html) to modify
  the tree

## Related work

Ktast is a fork of [Kastree](https://github.com/cretz/kastree). Unfortunately Kastree is currently not being actively
developed.
We are grateful to Chad Retz and contributors for the great work. Without their effort, we cannot build this library.

Another kotlin AST parsing library is [kotlinx.ast](https://github.com/kotlinx/ast). It does not use the Kotlin
Compiler, but uses ANTLR and official Kotlin Grammar. Currently, it seems that the library's summary AST classes are
limited.

[ktcodeshift](https://github.com/orangain/ktcodeshift) is a toolkit for running codemods over multiple Kotlin files
built on top of ktast. It is useful when modifying Kotlin source codes.

## Usage

### Getting started

Ktast is available on [JitPack](https://jitpack.io/). Add the following configurations to your Gradle build settings:

```kts
repositories {
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation("com.github.orangain.ktast:ast-psi:0.8.3")
}
```

The library `ast-psi` transitively depends on the Kotlin compiler.

While the parser only works from JVM projects, the AST itself (and writers/visitors) can be used from other
multiplatform projects. If you need the AST only, instead use:

```kts
dependencies {
    implementation("com.github.orangain.ktast:ast:0.8.3")
}
```

### Documentation

API document is available at:

https://orangain.github.io/ktast/latest/api/

### Examples

Examples below are simple Kotlin scripts.

#### Parsing code

In this example, we use the wrapper around the Kotlin compiler's parser:

```kotlin
import ktast.ast.psi.Parser

val code = """
    package foo

    fun bar() {
        // Print hello
        println("Hello, World!")
    }

    fun baz() = println("Hello, again!")
""".trimIndent()
// Call the parser with the code
val file = Parser.parseFile(code)
// The file var is now a ktast.ast.Node.KotlinFile that is used in future examples...
```

The `file` variable has the full AST. Note, if you want to parse with blank line and comment information, you can create
a converter that holds the extras:

```kotlin
import ktast.ast.psi.ConverterWithExtras

// ...

val extrasMap = ConverterWithExtras()
val file = Parser(extrasMap).parseFile(code)
// extrasMap is an instance of ktast.ast.ExtrasMap
```

#### Writing code

To write the code created above, simply use the writer

```kotlin
import ktast.ast.Writer

// ...

println(Writer.write(file))
```

This outputs a string of the written code from the AST `file` object. To include the extra blank line and comment info
from the previous parse example, pass in the extras map:

```kotlin
// ...

println(Writer.write(file, extrasMap))
```

This outputs the code with the comments.

#### Visiting nodes

This will get all strings:

```kotlin
import ktast.ast.Node
import ktast.ast.Visitor

// ...

val strings = mutableListOf<String>()
Visitor.traverse(file) { path ->
    val node = path.node
    if (node is Node.Expression.StringLiteralExpression.LiteralStringEntry) {
        strings.add(node.text)
    }
}
// Prints [Hello, World!, Hello, again!]
println(strings)
```

The parameter of the lambda is
a [NodePath](https://orangain.github.io/ktast/latest/api/ast/ktast.ast/-node-path/index.html) object that has `node`
and `parent` NodePath. There is a `tag` var on each node that can be used to store per-node state if desired.

#### Modifying nodes

This will change "Hello, World!" and "Hello, again!" to "Howdy, World!" and "Howdy, again":

```kotlin
import ktast.ast.MutableVisitor

// ...

val newFile = MutableVisitor.traverse(file) { path ->
    val node = path.node
    if (node !is Node.Expression.StringLiteralExpression.LiteralStringEntry) node
    else node.copy(text = node.text.replace("Hello", "Howdy"))
}
```

Now `newFile` is a transformed version of `file`. As before, the parameter of the lambda is a NodePath object.

Note, since `extraMap` support relies on object identities and this creates entirely new objects in the immutable tree,
the extra map becomes invalid on this step. When you mutate an AST tree, it is recommended to
use [ConverterWithMutableExtras](https://orangain.github.io/ktast/latest/api/ast-psi/ktast.ast.psi/-converter-with-mutable-extras/index.html)
that
implements [MutableExtrasMap](https://orangain.github.io/ktast/latest/api/ast/ktast.ast/-mutable-extras-map/index.html)
interface.

## Running tests

The tests rely on a checked out version of the [Kotlin source repository](https://github.com/JetBrains/kotlin) since it
uses the test data there to build a corpus to test against. The path to the base of the repo needs to be set as the
`KOTLIN_REPO` environment variable. For example, run:

```
KOTLIN_REPO=~/kotlin ./gradlew :ast-psi:test
```

This will ignore all Kotlin files with expected parse errors and only test against the ones that are valid (178 as of
this writing). The test parses the Kotlin code into this AST, then re-writes this AST, then re-parses what was just
written and confirms it matches the original AST field for field.
