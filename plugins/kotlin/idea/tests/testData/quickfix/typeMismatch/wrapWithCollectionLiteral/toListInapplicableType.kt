// "class org.jetbrains.kotlin.idea.quickfix.WrapWithCollectionLiteralCallFix" "false"
// ERROR: Type mismatch: inferred type is Int but List<String> was expected
// K2_AFTER_ERROR: Argument type mismatch: actual type is 'Int', but 'List<String>' was expected.

fun foo(a: Int) {
    bar(a<caret>)
}

fun bar(a: List<String>) {}
