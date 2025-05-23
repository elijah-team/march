// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.run

import com.intellij.execution.RunManager.Companion.getInstance
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.PossiblyDumbAware
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.refactoring.RefactoringFactory
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.fixtures.EditorTestFixture
import com.intellij.util.containers.addIfNotNull
import org.jdom.Element
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File

private const val RUN_PREFIX = "// RUN:"
private const val RUN_FILE_PREFIX = "// RUN_FILE:"
private const val NO_CONFIGURATION_ATTRIBUTE = "// NO_CONFIGURATION_IN_DUMB_MODE"

abstract class AbstractRunConfigurationTest  : AbstractRunConfigurationBaseTest() {
    fun testDependencyModuleClasspath() {
        configureProject()
        val configuredModule = defaultConfiguredModule
        val configuredModuleWithDependency = getConfiguredModule("moduleWithDependency")

        ModuleRootModificationUtil.addDependency(configuredModuleWithDependency.module, configuredModule.module)

        val kotlinRunConfiguration = createConfigurationFromMain(project, "some.test.main")
        kotlinRunConfiguration.setModule(configuredModuleWithDependency.module)

        val javaParameters = getJavaRunParameters(kotlinRunConfiguration)

        assertTrue(javaParameters.classPath.rootDirs.contains(configuredModule.srcOutputDir))
        assertTrue(javaParameters.classPath.rootDirs.contains(configuredModuleWithDependency.srcOutputDir))
    }

    fun testLongCommandLine() {
        configureProject()
        ModuleRootModificationUtil.addDependency(module, createLibraryWithLongPaths(project))

        val kotlinRunConfiguration = createConfigurationFromMain(project, "some.test.main")
        kotlinRunConfiguration.setModule(module)

        val javaParameters = getJavaRunParameters(kotlinRunConfiguration)
        val commandLine = javaParameters.toCommandLine().commandLineString
        assert(commandLine.length > javaParameters.classPath.pathList.joinToString(File.pathSeparator).length) {
            "Wrong command line length: \ncommand line = $commandLine, \nclasspath = ${javaParameters.classPath.pathList.joinToString()}"
        }
    }

    fun testClassesAndObjects() = checkClasses()

    fun testTopLevelAndObject() = checkClasses()

    fun testRunConfigurationProducerDumbMode() = checkClasses()

    fun testInJsModule() = checkClasses(Platform.JavaScript)

    fun testApplicationConfiguration() {
        configureProject()

        val manager = getInstance(project)
        val configuration = ApplicationConfiguration("some.main()", project)
        val mainFunction = findMainFunction(project, "some.main")
        val lightElements = mainFunction.containingKtFile.toLightElements()
        val psiClass = lightElements.single() as PsiClass
        assertEquals("MainKt", psiClass.name)
        configuration.setMainClass(psiClass)
        val settings = RunnerAndConfigurationSettingsImpl(manager as RunManagerImpl, configuration)
        settings.checkSettings(null)

        // modify file: `main` becomes `_main`, hence no main function
        val containingFile = mainFunction.containingFile
        val virtualFile = containingFile.virtualFile
        val editorTestFixture = editorTestFixture(virtualFile)
        val editor = editorTestFixture.editor
        val offset = editor.document.text.indexOf("main").takeIf { it >= 0 } ?: error("no `main` marker")
        editor.caretModel.moveToOffset(offset)
        editorTestFixture.type('_')

        FileDocumentManager.getInstance().saveDocument(editor.document)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        // recheck
        try {
            settings.checkSettings(null)
            fail("There is no Main method in class some.MainKt")
        } catch (e: RuntimeConfigurationWarning) {
            assertEquals("Main method not found in class some.MainKt", e.message)
        }
    }

    private fun editorTestFixture(virtualFile: VirtualFile): EditorTestFixture {
        val fragmentEditor = FileEditorManagerEx.getInstanceEx(project).openTextEditor(
            OpenFileDescriptor(project, virtualFile, 0), true
        ) ?: error("unable to open file")
        return EditorTestFixture(project, fragmentEditor, virtualFile)
    }

    fun testRedirectInputPath() {
        configureProject()

        val runConfiguration1 = createConfigurationFromMain(project, "some.main")
        runConfiguration1.inputRedirectOptions.apply {
            isRedirectInput = true
            redirectInputPath = "someFile"
        }

        val elementWrite = Element("temp")
        runConfiguration1.writeExternal(elementWrite)

        val runConfiguration2 = createConfigurationFromMain(project, "some.main")
        runConfiguration2.readExternal(elementWrite)

        assertEquals(runConfiguration1.inputRedirectOptions.isRedirectInput, runConfiguration2.inputRedirectOptions.isRedirectInput)
        assertEquals(runConfiguration1.inputRedirectOptions.redirectInputPath, runConfiguration2.inputRedirectOptions.redirectInputPath)
    }

    fun testIsEditableInADumbMode() {
        configureProject()

        val runConfiguration = createConfigurationFromObject("foo.Bar")

        with(runConfiguration.factory!!) {
            assertTrue(isEditableInDumbMode)
            assertTrue(safeAs<PossiblyDumbAware>()!!.isDumbAware)
        }
    }

    fun testUpdateOnClassRename() {
        configureProject()

        val runConfiguration = createConfigurationFromObject("renameTest.Foo")

        val obj = KotlinFullClassNameIndex.get("renameTest.Foo", project, project.allScope()).single()
        val rename = RefactoringFactory.getInstance(project).createRename(obj, "Bar")
        rename.run()

        assertEquals("renameTest.Bar", runConfiguration.runClass)
    }

    fun testUpdateOnPackageRename() {
        configureProject()

        val runConfiguration = createConfigurationFromObject("renameTest.Foo")

        val pkg = JavaPsiFacade.getInstance(project).findPackage("renameTest") ?: error("Package 'renameTest' not found")
        val rename = RefactoringFactory.getInstance(project).createRename(pkg, "afterRenameTest")
        rename.run()

        assertEquals("afterRenameTest.Foo", runConfiguration.runClass)
    }

    fun testWithModuleForJdk6() {
        checkModuleInfoName(null, Platform.Jvm(IdeaTestUtil.getMockJdk16()))
    }

    fun testWithModuleForJdk9() {
        checkModuleInfoName("MAIN", Platform.Jvm(IdeaTestUtil.getMockJdk9()))
    }

    fun testWithModuleForJdk9WithoutModuleInfo() {
        checkModuleInfoName(null, Platform.Jvm(IdeaTestUtil.getMockJdk9()))
    }

    private fun checkModuleInfoName(moduleName: String?, platform: Platform) {
        configureProject(platform)

        val javaParameters = getJavaRunParameters(createConfigurationFromMain(project, "some.main"))
        assertEquals(moduleName, javaParameters.moduleName)
    }

    private fun checkClasses(platform: Platform = Platform.Jvm()) {
        configureProject(platform)
        val srcDir = defaultConfiguredModule.srcDir ?: error("Module doesn't have a production source set")

        val expectedClasses = ArrayList<String>()
        val actualClasses = ArrayList<String>()
        val actualClassesInDumb = mutableListOf<String>()
        var expectedFileRun: String? = null

        val fileName = "test.kt"
        val testKtVirtualFile = srcDir.findFileByRelativePath(fileName) ?: error("Can't find VirtualFile for $fileName")
        val testFile = PsiManager.getInstance(project).findFile(testKtVirtualFile) ?: error("Can't find PSI for $fileName")

        val visitor = object : KtTreeVisitorVoid() {
            override fun visitComment(comment: PsiComment) {
                val declaration = comment.getStrictParentOfType<KtNamedDeclaration>()
                val text = comment.text ?: return
                when {
                    text.startsWith(RUN_PREFIX) -> {
                        val expectedClass = text.substring(RUN_PREFIX.length).trim()
                        if (expectedClass.isNotEmpty()) expectedClasses.add(expectedClass)
                        check(declaration != null)

                        val actualClass = getKotlinRunConfiguration(declaration)?.runClass
                        actualClasses.addIfNotNull(actualClass)

                        // TODO: KTIJ-33073 the result of `getKotlinRunConfiguration` is cached and reused in `getKotlinRunConfigurationInDumbMode`,
                        // so effectively this check is useless. To properly check the dumb mode the test should either drop caches between such invocations
                        // or check them separately and only in the dumb mode
                        //val actualClassInDumbMode = getKotlinRunConfigurationInDumbMode(declaration, platform)?.runClass
                        //actualClassesInDumb.addIfNotNull(actualClassInDumbMode)
                    }
                    text.startsWith(NO_CONFIGURATION_ATTRIBUTE) -> {
                        check(declaration != null)
                        assertNull(getKotlinRunConfigurationInDumbMode(declaration, platform))
                    }
                    text.startsWith(RUN_FILE_PREFIX) -> {
                        val fileRun = text.substring(RUN_FILE_PREFIX.length).trim()
                        if (fileRun.isNotEmpty()) {
                            check(expectedFileRun == null) { "The only one `$RUN_FILE_PREFIX` should be declared: `$expectedFileRun`, but `$fileRun`" }
                            expectedFileRun = fileRun
                        }
                    }
                }
            }
        }

        testFile.accept(visitor)
        assertEquals(expectedClasses, actualClasses)
        //assertEquals(expectedClasses, actualClassesInDumb)
        expectedFileRun?.let {
            val actualFileRun = getKotlinRunConfiguration(testFile)?.runClass

            assertEquals(it, actualFileRun)
        }
    }

    private fun getKotlinRunConfigurationInDumbMode(psiElement: PsiElement, platform: Platform): KotlinRunConfiguration? {
        if (platform == Platform.JavaScript) return null

        return DumbModeTestUtils.computeInDumbModeSynchronously(project) {
            val runConfiguration = getKotlinRunConfiguration(psiElement)

            runConfiguration
        }
    }

    private fun getKotlinRunConfiguration(psiElement: PsiElement): KotlinRunConfiguration? {
        val context = ConfigurationContext(psiElement)
        return context.configuration?.configuration as? KotlinRunConfiguration
    }

    private fun createConfigurationFromObject(@Suppress("SameParameterValue") objectFqn: String): KotlinRunConfiguration {
        val obj = KotlinFullClassNameIndex.get(objectFqn, project, project.allScope()).single()
        val mainFunction = obj.declarations.single { it is KtFunction && it.getName() == "main" }
        return createConfigurationFromElement(mainFunction, true) as KotlinRunConfiguration
    }

    override fun getTestDataDirectory() = IDEA_TEST_DATA_DIR.resolve("run")
}
