/*
 * Sonar .NET Plugin :: .NET Tests
 * Copyright (C) 2010 Jose Chillan, Alexandre Victoor, John M. Wright and SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dotnet.tests.parser.nunit;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.api.test.Testable;
import org.sonar.plugins.dotnet.api.DotNetConstants;
import org.sonar.plugins.dotnet.api.DotNetResourceBridge;
import org.sonar.plugins.dotnet.api.DotNetResourceBridges;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.tests.model.TestFileDetails;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;


public class NUnitTestResultParserTest {

    DotNetResourceBridges bridges;
    Project project;
    VisualStudioProject vsProject;
    ResourcePerspectives perspectives;

    Resource testClassResource;
    Resource testClass2Resource;
    Resource testParitalClassResource;
    Resource testParitalClass2ndResource;
    Resource testNestedOutterClassResource;

    MutableTestPlan testClassPlan;
    MutableTestPlan testClass2Plan;
    MutableTestPlan testParcialClassPlan;
    MutableTestPlan testParcialClass2ndPlan;
    MutableTestPlan testNestedOutterClassPlan;

    MutableTestCase caseClass1SimpleTest;
    MutableTestCase caseClass1WithInputTrues;
    MutableTestCase caseClass1WithInputMixed;


    MutableTestCase caseLowLevelClass1IgnoredTest;
    MutableTestCase caseLowLevelClass1NestedNamespaceTest;
    MutableTestCase caseLowLevelClass1ExceptionTest;


    MutableTestCase caseNestedClass1UnnestedTest;
    MutableTestCase caseNestedClass1InnerNestedTest;
    MutableTestCase caseNestedClass1DoubleNestedTest;

    MutableTestCase casePartialClass2ndFileTest;
    MutableTestCase casePartialClassMainFileTest;


    @Before
    public void init() {

        DotNetResourceBridge resourcesBridge = mock(DotNetResourceBridge.class);
        when(resourcesBridge.getLanguageKey()).thenReturn(DotNetConstants.CSHARP_LANGUAGE_KEY);
        bridges = new DotNetResourceBridges(new DotNetResourceBridge[] {resourcesBridge});

        perspectives = mock(ResourcePerspectives.class);

        project = mock(Project.class);
        when(project.getLanguageKey()).thenReturn(DotNetConstants.CSHARP_LANGUAGE_KEY);

        vsProject = mock(VisualStudioProject.class);
        when(vsProject.getArtifactName()).thenReturn("ClassLibrary1.Test.dll");

        initTopLevelClass1(resourcesBridge);
        initLowLevelClass1(resourcesBridge);
        initPartialClass(resourcesBridge);
        initNestedClass(resourcesBridge);



    }

    private void initNestedClass(DotNetResourceBridge resourcesBridge) {
        File testNestedOutterClassFile = TestUtils.getResource("/nunit/NUnitExample/ClassLibrary1.Test/NestedClassesOutter.cs");
        testNestedOutterClassResource = new org.sonar.api.resources.File(testNestedOutterClassFile.getPath(), testNestedOutterClassFile.getName());
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.NestedClassesOutter#UnnestedTest()")).thenReturn(testNestedOutterClassResource);
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.NestedClassesOutter.NestedClassesInner.DoubleNestedClassesInner#DoubleNestedTest()")).thenReturn(testNestedOutterClassResource);
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.NestedClassesOutter.NestedClassesInner#InnerNestedTest()")).thenReturn(testNestedOutterClassResource);
        testNestedOutterClassPlan = mock(MutableTestPlan.class);
        when(perspectives.as(MutableTestPlan.class, testNestedOutterClassResource)).thenReturn(testNestedOutterClassPlan);

        caseNestedClass1UnnestedTest = mock(MutableTestCase.class);
        when(testNestedOutterClassPlan.addTestCase("ClassLibrary1.Test.NestedClassesOutter.UnnestedTest")).thenReturn(caseNestedClass1UnnestedTest);

        caseNestedClass1InnerNestedTest = mock(MutableTestCase.class);
        when(testNestedOutterClassPlan.addTestCase("ClassLibrary1.Test.NestedClassesOutter+NestedClassesInner.InnerNestedTest")).thenReturn(caseNestedClass1InnerNestedTest);

        caseNestedClass1DoubleNestedTest = mock(MutableTestCase.class);
        when(testNestedOutterClassPlan.addTestCase("ClassLibrary1.Test.NestedClassesOutter+NestedClassesInner+DoubleNestedClassesInner.DoubleNestedTest")).thenReturn(caseNestedClass1DoubleNestedTest);


    }

    private void initPartialClass(DotNetResourceBridge resourcesBridge) {
        File testParcialClassFile = TestUtils.getResource("/nunit/NUnitExample/ClassLibrary1.Test/Foo/PartialClassTest.cs");
        testParitalClassResource = new org.sonar.api.resources.File(testParcialClassFile.getPath(), testParcialClassFile.getName());
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Foo.PartialClassTests#TestInMainFile()")).thenReturn(testParitalClassResource);
        testParcialClassPlan = mock(MutableTestPlan.class);
        when(perspectives.as(MutableTestPlan.class, testParitalClassResource)).thenReturn(testParcialClassPlan);

        casePartialClassMainFileTest = mock(MutableTestCase.class);
        when(testParcialClassPlan.addTestCase("ClassLibrary1.Test.Foo.PartialClassTests.TestInMainFile")).thenReturn(casePartialClassMainFileTest);


        File testParcialClass2ndFile = TestUtils.getResource("/nunit/NUnitExample/ClassLibrary1.Test/Foo/PartialClassTest.2ndFile.cs");
        testParitalClass2ndResource = new org.sonar.api.resources.File(testParcialClass2ndFile.getPath(), testParcialClass2ndFile.getName());
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Foo.PartialClassTests#TestIn2ndFile()")).thenReturn(testParitalClass2ndResource);
        testParcialClass2ndPlan = mock(MutableTestPlan.class);
        when(perspectives.as(MutableTestPlan.class, testParitalClass2ndResource)).thenReturn(testParcialClass2ndPlan);

        casePartialClass2ndFileTest = mock(MutableTestCase.class);
        when(testParcialClass2ndPlan.addTestCase("ClassLibrary1.Test.Foo.PartialClassTests.TestIn2ndFile")).thenReturn(casePartialClass2ndFileTest);

    }

    private void initLowLevelClass1(DotNetResourceBridge resourcesBridge) {
        File testClass2File = TestUtils.getResource("/nunit/NUnitExample/ClassLibrary1.Test/Foo/Bar/Class1Tests.cs");
        testClass2Resource = new org.sonar.api.resources.File(testClass2File.getPath(), testClass2File.getName());
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Foo.Bar.Class1Tests#IgnoredTest()")).thenReturn(testClass2Resource);
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Foo.Bar.Class1Tests#TestNestedNamespaceClass()")).thenReturn(testClass2Resource);
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Foo.Bar.Class1Tests#TestWillThrowExeption()")).thenReturn(testClass2Resource);
        testClass2Plan = mock(MutableTestPlan.class);
        when(perspectives.as(MutableTestPlan.class, testClass2Resource)).thenReturn(testClass2Plan);

        caseLowLevelClass1IgnoredTest = mock(MutableTestCase.class);
        when(testClass2Plan.addTestCase("ClassLibrary1.Test.Foo.Bar.Class1Tests.IgnoredTest")).thenReturn(caseLowLevelClass1IgnoredTest);

        caseLowLevelClass1NestedNamespaceTest = mock(MutableTestCase.class);
        when(testClass2Plan.addTestCase("ClassLibrary1.Test.Foo.Bar.Class1Tests.TestNestedNamespaceClass")).thenReturn(caseLowLevelClass1NestedNamespaceTest);

        caseLowLevelClass1ExceptionTest = mock(MutableTestCase.class);
        when(testClass2Plan.addTestCase("ClassLibrary1.Test.Foo.Bar.Class1Tests.TestWillThrowExeption")).thenReturn(caseLowLevelClass1ExceptionTest);

    }

    private void initTopLevelClass1(DotNetResourceBridge resourcesBridge) {
        File testClassFile = TestUtils.getResource("/nunit/NUnitExample/ClassLibrary1.Test/Class1Tests.cs");
        testClassResource = new org.sonar.api.resources.File(testClassFile.getPath(), testClassFile.getName());
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Class1Tests#SimpleTest_NoAsserts()")).thenReturn(testClassResource);
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Class1Tests#TestWithInput(bool, bool)")).thenReturn(testClassResource);
        testClassPlan = mock(MutableTestPlan.class);
        when(perspectives.as(MutableTestPlan.class, testClassResource)).thenReturn(testClassPlan);

        caseClass1SimpleTest = mock(MutableTestCase.class);
        when(testClassPlan.addTestCase("ClassLibrary1.Test.Class1Tests.SimpleTest_NoAsserts")).thenReturn(caseClass1SimpleTest);

        caseClass1WithInputTrues = mock(MutableTestCase.class);
        when(testClassPlan.addTestCase("ClassLibrary1.Test.Class1Tests.TestWithInput(True,True)")).thenReturn(caseClass1WithInputTrues) ;

        caseClass1WithInputMixed = mock(MutableTestCase.class);
        when(testClassPlan.addTestCase("ClassLibrary1.Test.Class1Tests.TestWithInput(False,True)")).thenReturn(caseClass1WithInputMixed);
    }


    @Test
    public void testParseTestFile() throws Exception {

        File resultFile = TestUtils.getResource("/nunit/NUnitExample/TestResult.xml");
        Collection<File> files = new HashSet<File>();
        files.add(resultFile);

        NUnitTestResultParser parser = new NUnitTestResultParser(bridges, project, vsProject);

        Set<TestFileDetails> results = parser.parse(files, TestCase.TYPE_UNIT );
        for(TestFileDetails testFile : results){
            testFile.publishTestPlan(perspectives);
        }


        AssertTopLevelClass1TestsDetails();
        AssertLowLevelClass1TestsDetails();
        AssertNestedClassesTestDetails();
        AssertPartialClassTestDetails();

        assertThat(results.size(), is(5));


//        Boolean topLevelClass1Found = false;
//        Boolean lowLevelClass1Found = false;
//        Boolean partialClassFound = false;
//      boolean nestedClassFound = false;
//        boolean partialClass2ndFound = false;
//
//        for(MutableTestPlan details : results)
//        {
//            org.sonar.api.resources.File sourceFile = details.getSourceFile();
//
//            String fileName = sourceFile.getLongName();
//
//            if (StringUtils.contains(fileName, "ClassLibrary1.Test/Foo/Bar/Class1Tests.cs")){
//                lowLevelClass1Found = true;
//                AssertLowLevelClass1TestsDetails(details);
//            } else if (StringUtils.contains(fileName, "ClassLibrary1.Test/Class1Tests.cs")){
//                topLevelClass1Found = true;
//                AssertTopLevelClass1TestsDetails(details);
//            } else if (StringUtils.contains(fileName, "ClassLibrary1.Test/Foo/PartialClassTest.cs")){
//                partialClassFound = true;
//            } else if (StringUtils.contains(fileName, "ClassLibrary1.Test/Foo/PartialClassTest.2ndFile.cs")){
//                partialClass2ndFound = true;
//            }  else if (StringUtils.contains(fileName, "ClassLibrary1.Test/NestedClassesOutter.cs")){
//
//                nestedClassFound = true;
//                AssertNestedClassesTestDetails(details);
//
////                String testName = details.getDetails().get(0).getName();
////
////                if (testName.endsWith("UnnestedTest")) {
////
////                    outterNestedClassFound = true;
////                    AssertOutterNestedClassesTestDetails(details);
////
////                } else if (testName.endsWith("InnerNestedTest")) {
////
////                    innerNestedClassFound = true;
////                    AssertInnerNestedClassesTestDetails(details);
////
////                } else if (testName.endsWith("DoubleNestedTest")) {
////
////                    doubleNestedClassFound = true;
////                    AssertDoubleNestedClassesTestDetails(details);
////                }
//            }
//        }


//        assertThat(lowLevelClass1Found, is(true));
//        assertThat(topLevelClass1Found, is(true));
//        assertThat(partialClassFound, is(true));
//        assertThat(outterNestedClassFound, is(true));
//        assertThat(innerNestedClassFound, is(true));
//        assertThat(doubleNestedClassFound, is(true));
//        assertThat(partialClass2ndFound, is(true));
//        assertThat(nestedClassFound, is(true));

    }

    private void AssertTopLevelClass1TestsDetails() {

        verify(perspectives, atLeastOnce()).as(MutableTestPlan.class, testClassResource);

        verify(testClassPlan, times(1)).addTestCase("ClassLibrary1.Test.Class1Tests.SimpleTest_NoAsserts");
        verify(caseClass1SimpleTest, times(1)).setStatus(TestCase.Status.OK);
        verify(caseClass1SimpleTest, never()).setMessage(any(String.class));
        verify(caseClass1SimpleTest, times(1)).setType(TestCase.TYPE_UNIT);
        verify(caseClass1SimpleTest, times(1)).setDurationInMs(Long.valueOf(50));
        verify(caseClass1SimpleTest, never()).setStackTrace(any(String.class));
        verify(caseClass1SimpleTest, never()).setCoverageBlock(any(Testable.class), any(List.class));


        verify(testClassPlan, times(1)).addTestCase("ClassLibrary1.Test.Class1Tests.TestWithInput(True,True)") ;
        verify(caseClass1WithInputTrues, times(1)).setStatus(TestCase.Status.OK);
        verify(caseClass1WithInputTrues, never()).setMessage(any(String.class));
        verify(caseClass1WithInputTrues, times(1)).setType(TestCase.TYPE_UNIT);
        verify(caseClass1WithInputTrues, times(1)).setDurationInMs(Long.valueOf(0));
        verify(caseClass1WithInputTrues, never()).setStackTrace(any(String.class));
        verify(caseClass1WithInputTrues, never()).setCoverageBlock(any(Testable.class), any(List.class));


        verify(testClassPlan, times(1)).addTestCase("ClassLibrary1.Test.Class1Tests.TestWithInput(False,True)");
        verify(caseClass1WithInputMixed, times(1)).setStatus(TestCase.Status.FAILURE);
        verify(caseClass1WithInputMixed).setMessage("  output did not meet expectations (input False, expectedOutput True)\n" +
                "  Expected: True\n" +
                "  But was:  False\n");
        verify(caseClass1WithInputMixed, times(1)).setType(TestCase.TYPE_UNIT);
        verify(caseClass1WithInputMixed, times(1)).setDurationInMs(Long.valueOf(40));
        verify(caseClass1WithInputMixed, times(1)).setStackTrace("at ClassLibrary1.Test.Class1Tests.TestWithInput(Boolean input, Boolean expectedOutput) in c:\\Users\\jwright\\Documents\\GitHub\\sonar-dotnet\\sonar\\dotnet\\sonar-dotnet-tests-plugin\\src\\test\\resources\\nunit\\NUnitExample\\ClassLibrary1.Test\\Class1Tests.cs:line 29\n");
        verify(caseClass1WithInputMixed, never()).setCoverageBlock(any(Testable.class), any(List.class));

    }

    private void AssertLowLevelClass1TestsDetails() {

        verify(perspectives, atLeastOnce()).as(MutableTestPlan.class, testClass2Resource);

        verify(testClass2Plan, times(1)).addTestCase("ClassLibrary1.Test.Foo.Bar.Class1Tests.IgnoredTest");
        verify(caseLowLevelClass1IgnoredTest, times(1)).setType(TestCase.TYPE_UNIT);
        verify(caseLowLevelClass1IgnoredTest, times(1)).setStatus(TestCase.Status.SKIPPED);
        verify(caseLowLevelClass1IgnoredTest, times(1)).setMessage("Test is ignored");
        verify(caseLowLevelClass1IgnoredTest, times(1)).setDurationInMs(Long.valueOf(0));
        verify(caseLowLevelClass1IgnoredTest, never()).setStackTrace(any(String.class));
        verify(caseLowLevelClass1IgnoredTest, never()).setCoverageBlock(any(Testable.class), any(List.class));


        verify(testClass2Plan, times(1)).addTestCase("ClassLibrary1.Test.Foo.Bar.Class1Tests.TestNestedNamespaceClass");
        verify(caseLowLevelClass1NestedNamespaceTest, times(1)).setStatus(TestCase.Status.OK);
        verify(caseLowLevelClass1NestedNamespaceTest, never()).setMessage(any(String.class));
        verify(caseLowLevelClass1NestedNamespaceTest, times(1)).setType(TestCase.TYPE_UNIT);
        verify(caseLowLevelClass1NestedNamespaceTest, times(1)).setDurationInMs(Long.valueOf(2));
        verify(caseLowLevelClass1NestedNamespaceTest, never()).setStackTrace(any(String.class));
        verify(caseLowLevelClass1NestedNamespaceTest, never()).setCoverageBlock(any(Testable.class), any(List.class));

        verify(testClass2Plan, times(1)).addTestCase("ClassLibrary1.Test.Foo.Bar.Class1Tests.TestWillThrowExeption");
        verify(caseLowLevelClass1ExceptionTest, times(1)).setStatus(TestCase.Status.ERROR);
        verify(caseLowLevelClass1ExceptionTest, times(1)).setMessage("System.Exception : something bad happened");
        verify(caseLowLevelClass1ExceptionTest, times(1)).setType(TestCase.TYPE_UNIT);
        verify(caseLowLevelClass1ExceptionTest, times(1)).setDurationInMs(Long.valueOf(8));
        verify(caseLowLevelClass1ExceptionTest, times(1)).setStackTrace("at ClassLibrary1.Class1.ThrowException() in c:\\Users\\jwright\\Documents\\GitHub\\sonar-dotnet\\sonar\\dotnet\\sonar-dotnet-tests-plugin\\src\\test\\resources\\nunit\\NUnitExample\\ClassLibrary1\\Class1.cs:line 24\n" +
                "at ClassLibrary1.Test.Foo.Bar.Class1Tests.TestWillThrowExeption() in c:\\Users\\jwright\\Documents\\GitHub\\sonar-dotnet\\sonar\\dotnet\\sonar-dotnet-tests-plugin\\src\\test\\resources\\nunit\\NUnitExample\\ClassLibrary1.Test\\Foo\\Bar\\Class1Tests.cs:line 20\n");
        verify(caseLowLevelClass1ExceptionTest, never()).setCoverageBlock(any(Testable.class), any(List.class));

    }

    private void AssertNestedClassesTestDetails() {

        verify(perspectives, atLeastOnce()).as(MutableTestPlan.class, testNestedOutterClassResource);

        verify(testNestedOutterClassPlan, times(1)).addTestCase("ClassLibrary1.Test.NestedClassesOutter.UnnestedTest");
        verify(caseNestedClass1UnnestedTest, times(1)).setStatus(TestCase.Status.OK);
        verify(caseNestedClass1UnnestedTest, never()).setMessage(any(String.class));
        verify(caseNestedClass1UnnestedTest, times(1)).setType(TestCase.TYPE_UNIT);
        verify(caseNestedClass1UnnestedTest, times(1)).setDurationInMs(Long.valueOf(0));
        verify(caseNestedClass1UnnestedTest, never()).setStackTrace(any(String.class));
        verify(caseNestedClass1UnnestedTest, never()).setCoverageBlock(any(Testable.class), any(List.class));

        verify(testNestedOutterClassPlan, times(1)).addTestCase("ClassLibrary1.Test.NestedClassesOutter+NestedClassesInner.InnerNestedTest");
        verify(caseNestedClass1InnerNestedTest, times(1)).setStatus(TestCase.Status.OK);
        verify(caseNestedClass1InnerNestedTest, never()).setMessage(any(String.class));
        verify(caseNestedClass1InnerNestedTest, times(1)).setType(TestCase.TYPE_UNIT);
        verify(caseNestedClass1InnerNestedTest, times(1)).setDurationInMs(Long.valueOf(0));
        verify(caseNestedClass1InnerNestedTest, never()).setStackTrace(any(String.class));
        verify(caseNestedClass1InnerNestedTest, never()).setCoverageBlock(any(Testable.class), any(List.class));

        verify(testNestedOutterClassPlan, times(1)).addTestCase("ClassLibrary1.Test.NestedClassesOutter+NestedClassesInner+DoubleNestedClassesInner.DoubleNestedTest");
        verify(caseNestedClass1DoubleNestedTest, times(1)).setStatus(TestCase.Status.OK);
        verify(caseNestedClass1DoubleNestedTest, never()).setMessage(any(String.class));
        verify(caseNestedClass1DoubleNestedTest, times(1)).setType(TestCase.TYPE_UNIT);
        verify(caseNestedClass1DoubleNestedTest, times(1)).setDurationInMs(Long.valueOf(0));
        verify(caseNestedClass1DoubleNestedTest, never()).setStackTrace(any(String.class));
        verify(caseNestedClass1DoubleNestedTest, never()).setCoverageBlock(any(Testable.class), any(List.class));

    }

    private void AssertPartialClassTestDetails() {

        verify(perspectives, atLeastOnce()).as(MutableTestPlan.class, testParitalClassResource);
        verify(perspectives, atLeastOnce()).as(MutableTestPlan.class, testParitalClass2ndResource);


        verify(testParcialClassPlan, times(1)).addTestCase("ClassLibrary1.Test.Foo.PartialClassTests.TestInMainFile");
        verify(casePartialClassMainFileTest, times(1)).setStatus(TestCase.Status.OK);
        verify(casePartialClassMainFileTest, never()).setMessage(any(String.class));
        verify(casePartialClassMainFileTest, times(1)).setType(TestCase.TYPE_UNIT);
        verify(casePartialClassMainFileTest, times(1)).setDurationInMs(Long.valueOf(0));
        verify(casePartialClassMainFileTest, never()).setStackTrace(any(String.class));
        verify(casePartialClassMainFileTest, never()).setCoverageBlock(any(Testable.class), any(List.class));

        verify(testParcialClass2ndPlan, times(1)).addTestCase("ClassLibrary1.Test.Foo.PartialClassTests.TestIn2ndFile");
        verify(casePartialClass2ndFileTest, times(1)).setStatus(TestCase.Status.OK);
        verify(casePartialClass2ndFileTest, never()).setMessage(any(String.class));
        verify(casePartialClass2ndFileTest, times(1)).setType(TestCase.TYPE_UNIT);
        verify(casePartialClass2ndFileTest, times(1)).setDurationInMs(Long.valueOf(0));
        verify(casePartialClass2ndFileTest, never()).setStackTrace(any(String.class));
        verify(casePartialClass2ndFileTest, never()).setCoverageBlock(any(Testable.class), any(List.class));


    }

}
