// "Import function 'importedFunA'" "true"
// ERROR: Unresolved reference: importedFunA

import editor.completion.apx.importedFunA as funA
fun context() {
    val funA = 42
    funA()
}
// IGNORE_K2