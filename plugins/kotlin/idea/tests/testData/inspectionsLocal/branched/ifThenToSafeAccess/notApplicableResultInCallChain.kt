// FIR_COMPARISON
// PROBLEM: none
// WITH_STDLIB
// DISABLE_ERRORS

val someNullableString: String? = ""
fun String.bar(): Result<String> = Result.success("")
val result = if<caret> (someNullableString == null) {
    null
} else {
    someNullableString.bar().getOrNull().let { }
}