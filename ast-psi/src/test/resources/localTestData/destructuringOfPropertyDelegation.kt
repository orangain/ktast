internal object Foo {
    override fun Bar() {
        val (counter, setCounter) by remember { mutableStateOf(0) }
    }
}
