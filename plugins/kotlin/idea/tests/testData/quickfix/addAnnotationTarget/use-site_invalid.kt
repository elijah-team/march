// "Add annotation target" "false"
// ACTION: Create test
// ACTION: Extract 'Test' from current file
// ACTION: Make internal
// ACTION: Make private
// ERROR: '@get:' annotations could be applied only to property declarations
// ERROR: This annotation is not applicable to target 'class' and use site target '@get'
annotation class Ann

<caret>@get:Ann
class Test(val foo: String)
