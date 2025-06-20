// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.structureView;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode;
import org.jetbrains.kotlin.idea.base.test.TestRoot;
import org.jetbrains.kotlin.idea.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.idea.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

/**
 * This class is generated by {@link org.jetbrains.kotlin.testGenerator.generator.TestGenerator}.
 * DO NOT MODIFY MANUALLY.
 */
@SuppressWarnings("all")
@TestRoot("idea/tests")
@TestDataPath("$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
@TestMetadata("testData/structureView/fileStructure")
public class KotlinFileStructureTestGenerated extends AbstractKotlinFileStructureTest {
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public final KotlinPluginMode getPluginMode() {
        return KotlinPluginMode.K1;
    }

    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    @TestMetadata("AnonymousObjectMembers.kt")
    public void testAnonymousObjectMembers() throws Exception {
        runTest("testData/structureView/fileStructure/AnonymousObjectMembers.kt");
    }

    @TestMetadata("CheckLocationForKotlin.kt")
    public void testCheckLocationForKotlin() throws Exception {
        runTest("testData/structureView/fileStructure/CheckLocationForKotlin.kt");
    }

    @TestMetadata("CheckMemberLocationForJava.kt")
    public void testCheckMemberLocationForJava() throws Exception {
        runTest("testData/structureView/fileStructure/CheckMemberLocationForJava.kt");
    }

    @TestMetadata("DoNotShowParentsInLocationJava.kt")
    public void testDoNotShowParentsInLocationJava() throws Exception {
        runTest("testData/structureView/fileStructure/DoNotShowParentsInLocationJava.kt");
    }

    @TestMetadata("EmptyFile.kt")
    public void testEmptyFile() throws Exception {
        runTest("testData/structureView/fileStructure/EmptyFile.kt");
    }

    @TestMetadata("InheritedDelegationMethods.kt")
    public void testInheritedDelegationMethods() throws Exception {
        runTest("testData/structureView/fileStructure/InheritedDelegationMethods.kt");
    }

    @TestMetadata("InheritedInnerClasses.kt")
    public void testInheritedInnerClasses() throws Exception {
        runTest("testData/structureView/fileStructure/InheritedInnerClasses.kt");
    }

    @TestMetadata("InheritedJavaMembers.kt")
    public void testInheritedJavaMembers() throws Exception {
        runTest("testData/structureView/fileStructure/InheritedJavaMembers.kt");
    }

    @TestMetadata("InheritedLocalKotlin.kt")
    public void testInheritedLocalKotlin() throws Exception {
        runTest("testData/structureView/fileStructure/InheritedLocalKotlin.kt");
    }

    @TestMetadata("InheritedMembers.kt")
    public void testInheritedMembers() throws Exception {
        runTest("testData/structureView/fileStructure/InheritedMembers.kt");
    }

    @TestMetadata("InheritedMembersOfEnum.kt")
    public void testInheritedMembersOfEnum() throws Exception {
        runTest("testData/structureView/fileStructure/InheritedMembersOfEnum.kt");
    }

    @TestMetadata("InheritedMembersWithSubstitutedTypes.kt")
    public void testInheritedMembersWithSubstitutedTypes() throws Exception {
        runTest("testData/structureView/fileStructure/InheritedMembersWithSubstitutedTypes.kt");
    }

    @TestMetadata("InheritedSAMConversion.kt")
    public void testInheritedSAMConversion() throws Exception {
        runTest("testData/structureView/fileStructure/InheritedSAMConversion.kt");
    }

    @TestMetadata("InheritedSynthesizedFromDataClass.kt")
    public void testInheritedSynthesizedFromDataClass() throws Exception {
        runTest("testData/structureView/fileStructure/InheritedSynthesizedFromDataClass.kt");
    }

    @TestMetadata("LocalElements.kt")
    public void testLocalElements() throws Exception {
        runTest("testData/structureView/fileStructure/LocalElements.kt");
    }

    @TestMetadata("Render.kt")
    public void testRender() throws Exception {
        runTest("testData/structureView/fileStructure/Render.kt");
    }

    @TestMetadata("Script.kts")
    public void testScript() throws Exception {
        runTest("testData/structureView/fileStructure/Script.kts");
    }

    @TestMetadata("SeveralClasses.kt")
    public void testSeveralClasses() throws Exception {
        runTest("testData/structureView/fileStructure/SeveralClasses.kt");
    }

    @TestMetadata("Simple.kt")
    public void testSimple() throws Exception {
        runTest("testData/structureView/fileStructure/Simple.kt");
    }

    @TestMetadata("Varargs.kt")
    public void testVarargs() throws Exception {
        runTest("testData/structureView/fileStructure/Varargs.kt");
    }
}
