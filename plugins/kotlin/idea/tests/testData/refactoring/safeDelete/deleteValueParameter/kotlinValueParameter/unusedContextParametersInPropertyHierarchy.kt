// COMPILER_ARGUMENTS: -Xcontext-parameters
// LANGUAGE_VERSION: 2.2

open class AAA {
    context(<caret>a: String)
    open val p: Int
        get() = 42
}

class BBB: AAA() {

    context(a: String)
    override val p: Int
        get() = super.p
}


// IGNORE_K1