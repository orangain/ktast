package localTestData

class A

class B : B1()
open class B1

class C(val a: C1, val b: () -> Unit): C1 by a
interface C1

class D(val a: () -> Unit): () -> Unit by a

abstract class E : E1, () -> Unit
interface E1

