# Ktast [![](https://jitpack.io/v/orangain/ktast.svg)](https://jitpack.io/#orangain/ktast) [![Java CI](https://github.com/orangain/ktast/actions/workflows/java_ci.yaml/badge.svg)](https://github.com/orangain/ktast/actions/workflows/java_ci.yaml)

Ktast is a simple library to manipulate Kotlin source code as a set of AST objects. Features:

* Simple, immutable, hierarchical [set of data classes](ast/src/commonMain/kotlin/ktast/ast/Node.kt) representing
  Kotlin AST
* Simple [writer implementation](ast/src/commonMain/kotlin/ktast/ast/Writer.kt) (some advanced features not yet
  supported)
* Support for [regular](ast/src/commonMain/kotlin/ktast/ast/Visitor.kt) and
  [mutable](ast/src/commonMain/kotlin/ktast/ast/MutableVisitor.kt) visitors
* Basic support for blank-line and comment map (some advanced use cases not yet supported)
* Support for [parsing](ast-psi/src/main/kotlin/ktast/ast/psi/Parser.kt) (via Kotlin compiler's parser) and
  [converting](ast-psi/src/main/kotlin/ktast/ast/psi/Converter.kt) to the AST

The project is a simple one and probably will not support a lot of features. It was created to facilitate advanced
Kotlin code generation beyond the string-based versions that exist.

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
    implementation("com.github.orangain.ktast:ast-psi:0.8.2")
}
```

The library `ast-psi` transitively depends on the Kotlin compiler.

While the parser only works from JVM projects, the AST itself (and writers/visitors) can be used from other
multiplatform projects. If you need the AST only, instead use:

```kts
dependencies {
    implementation("com.github.orangain.ktast:ast:0.8.2")
}
```

### Documentation

API document is available at:

https://orangain.github.io/ktast/0.8.2/api/

### Examples

Examples below are simple Kotlin scripts.

#### Parse code

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
// The file var is now a ktast.ast.Node.File that is used in future examples...
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

#### Write code

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

#### View nodes of a type

This will get all strings:

```kotlin
import ktast.ast.Node
import ktast.ast.Visitor

// ...

var strings = emptyList<String>()
Visitor.visit(file) { v, _ ->
    if (v is Node.Expression.StringTemplate.Entry.Regular) strings += v.str
}
// Prints [Hello, World!, Hello, again!]
println(strings)
```

The first parameter of the lambda is the nullable node and the second parameter is the parent. There is a `tag` var on
each node that can be used to store per-node state if desired.

#### Modify nodes

This will change "Hello, World!" and "Hello, again!" to "Howdy, World!" and "Howdy, again":

```kotlin
import ktast.ast.MutableVisitor

// ...

val newFile = MutableVisitor.preVisit(file) { v, _ ->
    if (v !is Node.Expression.StringTemplate.Entry.Regular) v
    else v.copy(str = v.str.replace("Hello", "Howdy"))
}
```

Now `newFile` is a transformed version of `file`. As before, the first parameter of the lambda is the nullable node and
the second parameter is the parent. The difference between `preVisit` (as used here) and `postVisit` is that `preVisit`
is called before children are traversed/mutated whereas `postVisit` is called afterwards.

Note, since `extraMap` support relies on object identities and this creates entirely new objects in the immutable tree,
the extra map becomes invalid on this step. This is not solved in the library yet, but could be done fairly easily.

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
