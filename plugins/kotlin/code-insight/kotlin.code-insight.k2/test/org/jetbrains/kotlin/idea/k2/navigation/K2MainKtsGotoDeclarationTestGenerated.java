// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.k2.navigation;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode;
import org.jetbrains.kotlin.idea.base.test.TestRoot;
import org.jetbrains.kotlin.idea.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.idea.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;
import org.jetbrains.kotlin.idea.k2.AbstractScriptGotoDeclarationMultifileTest;

/**
 * This class is generated by {@link org.jetbrains.kotlin.testGenerator.generator.TestGenerator}.
 * DO NOT MODIFY MANUALLY.
 */
@SuppressWarnings("all")
@TestRoot("code-insight/kotlin.code-insight.k2")
@TestDataPath("$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
@TestMetadata("../../idea/tests/testData/mainKts/navigation/gotoDeclaration")
public class K2MainKtsGotoDeclarationTestGenerated extends AbstractScriptGotoDeclarationMultifileTest {
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public final KotlinPluginMode getPluginMode() {
        return KotlinPluginMode.K2;
    }

    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    @TestMetadata("gotoKtsStdlibSources.test")
    public void testGotoKtsStdlibSources() throws Exception {
        runTest("../../idea/tests/testData/mainKts/navigation/gotoDeclaration/gotoKtsStdlibSources.test");
    }

    @TestMetadata("gotoMainKtsImportedScript.test")
    public void testGotoMainKtsImportedScript() throws Exception {
        runTest("../../idea/tests/testData/mainKts/navigation/gotoDeclaration/gotoMainKtsImportedScript.test");
    }

    @TestMetadata("gotoMainKtsImportedScriptSymbols.test")
    public void testGotoMainKtsImportedScriptSymbols() throws Exception {
        runTest("../../idea/tests/testData/mainKts/navigation/gotoDeclaration/gotoMainKtsImportedScriptSymbols.test");
    }

    @TestMetadata("gotoMainKtsImportedScriptTransitiveSymbols.test")
    public void testGotoMainKtsImportedScriptTransitiveSymbols() throws Exception {
        runTest("../../idea/tests/testData/mainKts/navigation/gotoDeclaration/gotoMainKtsImportedScriptTransitiveSymbols.test");
    }

    @TestMetadata("gotoMainKtsStdlibSources.test")
    public void testGotoMainKtsStdlibSources() throws Exception {
        runTest("../../idea/tests/testData/mainKts/navigation/gotoDeclaration/gotoMainKtsStdlibSources.test");
    }
}
