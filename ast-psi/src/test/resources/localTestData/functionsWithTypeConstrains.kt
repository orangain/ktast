// The following is based kotlin/compiler/testData/psi/contracts/FunctionsWithTypeConstraintsAndContract.kt but type arguments are moved immediately after fun keyword.

// the following functions have type constraints and contracts written in different order
// any order is correct

fun <T, E>someFunctionWithTypeConstraints(arg: E?, block: () -> T): String
    contract [
        returns() implies (arg != null),
        callsInPlace(block, EXACTLY_ONCE),
    ]
    where T : MyClass,
          E : MyOtherClass
{
    block()
    arg ?: throw NullArgumentException()
    return "some string"
}

fun <D, T>(data: D?, arg: T?, block: () -> Unit)
    where D : SuperType,
          T : SomeType
    contract [
        returns() implies (data != null),
        returns() implies (arg != null)
    ]
{
    require(data != null)
    require(arg != null)
    block()
}

// The following is based kotlin/compiler/testData/psi/FunctionsWithoutName.kt but type arguments are moved immediately after fun keyword.
fun ()
fun T.()
fun T.(a : foo) : bar
fun <T : (a) -> b>T.(a : foo) : bar

fun ();
fun T.();
fun T.(a : foo) : bar;
fun <T : (a) -> b>T.(a : foo) : bar;

fun () {}
fun @[a] T.() {}
fun @[a] T.(a : foo) : bar {}
fun <T :  (a) -> b>@[a()] T.(a : foo) : bar {}
fun <T : @[a]  (a) -> b>@[a()] T.(a : foo) : bar {}

fun A?.() : bar?
fun A? .() : bar?

// The following content is based kotlin/compiler/testData/psi/examples/util/Comparison.kt but type arguments are moved immediately after fun keyword.
fun <in T : Comparable<T>>naturalOrder(a : T, b : T) : Int = a.compareTo(b)

fun castingNaturalOrder(a : Object, b : Object) : Int = (a as Comparable<Object>).compareTo(b as Comparable<Object>)

enum class ComparisonResult {
    LS, EQ, GR;
}

fun <T> asMatchableComparison(cmp : Comparison<T>) : MatchableComparison<T> = {a, b ->
    val res = cmp(a, b)
    if (res == 0) return ComparisonResult.EQ
    if (res < 0) return ComparisonResult.LS
    return ComparisonResult.GR
}

// The following content is based kotlin/compiler/testData/psi/FunctionExpressions.kt but type arguments are moved immediately after fun keyword.
val a = fun ()
val a = fun name()
val a = fun T.name()
val a = fun @[a] T.(a : foo) : bar
val a = fun @[a] T.name(a : foo) : bar
val a = fun <T : (a) -> b> @[a()] T.(a : foo) : bar

fun c() = fun ();
fun c() = fun name();
fun c() = fun @[a] T.();
fun c() = fun @[a] T.(a : foo) : bar;
fun c() = fun <T : (a) -> b>@[a()] T.(a : foo) : bar;

val d = fun () = a
val d = fun name() = a
val a = @[a] fun ()

val b = fun <T> () where T: A

fun outer() {
    bar(fun () {})
    bar(fun name() {})
    bar(fun @[a] T.() {})
    bar(fun @[a] T.name() {})

    bar(fun @[a] T.(a : foo) : bar {})
    bar(fun <T :  (a) -> b> @[a()] T.(a : foo) : bar {})

    bar {fun <T : @[a]  (a) -> b> @[a()] T.(a : foo) : bar {}}

    bar {fun A?.() : bar?}
    bar {fun A? .() : bar?}

    bar(fun () = a)
    bar(fun name() = a)
    bar(@[a] fun name() = a)
}
