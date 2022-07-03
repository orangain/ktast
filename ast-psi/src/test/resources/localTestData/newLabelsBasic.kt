// This file is based kotlin/compiler/testData/psi/newLabels/basic.kt but extra lambda argument is removed.
fun foo() {
    x1.filter b@ {
        return 1
    }

    loop1@ for (i in 1..100) {
        return 4
    }

    loop2@ for (i in 1..100) {
        return@loop2 4
        return@loop2 5
    }

    label1@ val x = 1

    1 + label3@ 3 + 4

    l1@ foo bar l2@ baz // binary expression

    return (a@ 1)
}