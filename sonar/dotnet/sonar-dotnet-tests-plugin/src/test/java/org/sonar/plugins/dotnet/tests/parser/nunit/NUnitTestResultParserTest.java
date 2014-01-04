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

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.dotnet.api.DotNetConstants;
import org.sonar.plugins.dotnet.api.DotNetResourceBridge;
import org.sonar.plugins.dotnet.api.DotNetResourceBridges;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.tests.model.TestCaseDetail;
import org.sonar.plugins.dotnet.tests.model.TestFileDetails;
import org.sonar.plugins.dotnet.tests.model.TestStatus;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class NUnitTestResultParserTest {


    @Test
    public void testParseTestFile() throws Exception {

        DotNetResourceBridge resourcesBridge = mock(DotNetResourceBridge.class);
        when(resourcesBridge.getLanguageKey()).thenReturn(DotNetConstants.CSHARP_LANGUAGE_KEY);

        File testClassFile = TestUtils.getResource("/nunit/NUnitExample/ClassLibrary1.Test/Class1Tests.cs");
        Resource testClassResource = new org.sonar.api.resources.File(testClassFile.getPath(), testClassFile.getName());
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Class1Tests#SimpleTest_NoAsserts()")).thenReturn(testClassResource);
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Class1Tests#TestWithInput(bool, bool)")).thenReturn(testClassResource);
//        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Class1Tests.TestWithInput(True, True)")).thenReturn(testClassResource);


        File testClass2File = TestUtils.getResource("/nunit/NUnitExample/ClassLibrary1.Test/Foo/Bar/Class1Tests.cs");
        Resource testClass2Resource = new org.sonar.api.resources.File(testClass2File.getPath(), testClass2File.getName());
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Foo.Bar.Class1Tests#IgnoredTest()")).thenReturn(testClass2Resource);
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Foo.Bar.Class1Tests#TestNestedNamespaceClass()")).thenReturn(testClass2Resource);
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Foo.Bar.Class1Tests#TestWillThrowExeption()")).thenReturn(testClass2Resource);


        File testParcialClassFile = TestUtils.getResource("/nunit/NUnitExample/ClassLibrary1.Test/Foo/PartialClassTest.cs");
        Resource testParitalClassResource = new org.sonar.api.resources.File(testParcialClassFile.getPath(), testParcialClassFile.getName());
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Foo.PartialClassTests#TestInMainFile()")).thenReturn(testParitalClassResource);

        File testParcialClass2ndFile = TestUtils.getResource("/nunit/NUnitExample/ClassLibrary1.Test/Foo/PartialClassTest.2ndFile.cs");
        Resource testParitalClass2ndResource = new org.sonar.api.resources.File(testParcialClass2ndFile.getPath(), testParcialClass2ndFile.getName());
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.Foo.PartialClassTests#TestIn2ndFile()")).thenReturn(testParitalClass2ndResource);



        File testNestedOutterClassFile = TestUtils.getResource("/nunit/NUnitExample/ClassLibrary1.Test/NestedClassesOutter.cs");
        Resource testNestedOutterClassResource = new org.sonar.api.resources.File(testNestedOutterClassFile.getPath(), testNestedOutterClassFile.getName());
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.NestedClassesOutter#UnnestedTest()")).thenReturn(testNestedOutterClassResource);
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.NestedClassesOutter.NestedClassesInner.DoubleNestedClassesInner#DoubleNestedTest()")).thenReturn(testNestedOutterClassResource);
        when(resourcesBridge.getFromMemberName("ClassLibrary1.Test.NestedClassesOutter.NestedClassesInner#InnerNestedTest()")).thenReturn(testNestedOutterClassResource);


        DotNetResourceBridges bridges = new DotNetResourceBridges(new DotNetResourceBridge[] {resourcesBridge});

        Project project = mock(Project.class);
        when(project.getLanguageKey()).thenReturn(DotNetConstants.CSHARP_LANGUAGE_KEY);


        File resultFile = TestUtils.getResource("/nunit/NUnitExample/TestResult.xml");

        VisualStudioProject vsProject = mock(VisualStudioProject.class);
        when(vsProject.getArtifactName()).thenReturn("ClassLibrary1.Test.dll");


        Collection<File> files = new HashSet<File>();
        files.add(resultFile);
        NUnitTestResultParser parser = new NUnitTestResultParser(bridges, project, vsProject, files);

        Set<TestFileDetails> results = parser.parse();

        assertThat(results.size(), is(5));

        Boolean topLevelClass1Found = false;
        Boolean lowLevelClass1Found = false;
        Boolean partialClassFound = false;
//        Boolean innerNestedClassFound = false;
//        Boolean outterNestedClassFound = false;
//        Boolean doubleNestedClassFound = false;
        boolean nestedClassFound = false;
        boolean partialClass2ndFound = false;

        for(TestFileDetails details : results)
        {
            org.sonar.api.resources.File sourceFile = details.getSourceFile();

            String fileName = sourceFile.getLongName();

            if (StringUtils.contains(fileName, "ClassLibrary1.Test/Foo/Bar/Class1Tests.cs")){
                lowLevelClass1Found = true;
                AssertLowLevelClass1TestsDetails(details);
            } else if (StringUtils.contains(fileName, "ClassLibrary1.Test/Class1Tests.cs")){
                topLevelClass1Found = true;
                AssertTopLevelClass1TestsDetails(details);
            } else if (StringUtils.contains(fileName, "ClassLibrary1.Test/Foo/PartialClassTest.cs")){
                partialClassFound = true;
            } else if (StringUtils.contains(fileName, "ClassLibrary1.Test/Foo/PartialClassTest.2ndFile.cs")){
                partialClass2ndFound = true;
            }  else if (StringUtils.contains(fileName, "ClassLibrary1.Test/NestedClassesOutter.cs")){

                nestedClassFound = true;
                AssertNestedClassesTestDetails(details);

//                String testName = details.getDetails().get(0).getName();
//
//                if (testName.endsWith("UnnestedTest")) {
//
//                    outterNestedClassFound = true;
//                    AssertOutterNestedClassesTestDetails(details);
//
//                } else if (testName.endsWith("InnerNestedTest")) {
//
//                    innerNestedClassFound = true;
//                    AssertInnerNestedClassesTestDetails(details);
//
//                } else if (testName.endsWith("DoubleNestedTest")) {
//
//                    doubleNestedClassFound = true;
//                    AssertDoubleNestedClassesTestDetails(details);
//                }
            }
        }


        assertThat(lowLevelClass1Found, is(true));
        assertThat(topLevelClass1Found, is(true));
        assertThat(partialClassFound, is(true));
//        assertThat(outterNestedClassFound, is(true));
//        assertThat(innerNestedClassFound, is(true));
//        assertThat(doubleNestedClassFound, is(true));
        assertThat(partialClass2ndFound, is(true));
        assertThat(nestedClassFound, is(true));

    }

    private void AssertTopLevelClass1TestsDetails(TestFileDetails details1) {
        assertThat(details1.getErrors(), is(0));
        assertThat(details1.getFailures(), is(1));
        assertThat(details1.getSkipped(), is(0));
        assertThat(details1.getTests(), is(3)) ;
        assertThat(details1.getTimeMS(), is(90));


        List<TestCaseDetail> testCases = details1.getDetails();
        assertThat(testCases.size(), is(3));


        TestCaseDetail testCase0 = testCases.get(0);
        assertThat(testCase0.getName(), is("ClassLibrary1.Test.Class1Tests.SimpleTest_NoAsserts"));
        assertThat(testCase0.getStatus(), is(TestStatus.SUCCESS));

        TestCaseDetail testCase1 = testCases.get(2);
        assertThat(testCase1.getName(), is("ClassLibrary1.Test.Class1Tests.TestWithInput(True,True)"));
        assertThat(testCase1.getStatus(), is(TestStatus.SUCCESS));

        TestCaseDetail testCase2 = testCases.get(1);
        assertThat(testCase2.getName(), is("ClassLibrary1.Test.Class1Tests.TestWithInput(False,True)"));
        assertThat(testCase2.getStatus(), is(TestStatus.FAILED));
        assertThat(testCase2.getErrorMessage(), is("  output did not meet expectations (input False, expectedOutput True)\n" +
                "  Expected: True\n" +
                "  But was:  False\n"));
        //todo: assert stacktrace

        //todo assert xml values
    }

    private void AssertLowLevelClass1TestsDetails(TestFileDetails details1) {
        assertThat(details1.getErrors(), is(1));
        assertThat(details1.getFailures(), is(0));
        assertThat(details1.getSkipped(), is(1));
        assertThat(details1.getTests(), is(3)) ;
        assertThat(details1.getTimeMS(), is(10));


        List<TestCaseDetail> testCases = details1.getDetails();
        assertThat(testCases.size(), is(3));


        TestCaseDetail testCase0 = testCases.get(0);
        assertThat(testCase0.getName(), is("ClassLibrary1.Test.Foo.Bar.Class1Tests.IgnoredTest"));
        assertThat(testCase0.getStatus(), is(TestStatus.SKIPPED));
        assertThat(testCase0.getErrorMessage(), is("Test is ignored"));

        TestCaseDetail testCase1 = testCases.get(1);
        assertThat(testCase1.getName(), is("ClassLibrary1.Test.Foo.Bar.Class1Tests.TestNestedNamespaceClass"));
        assertThat(testCase1.getStatus(), is(TestStatus.SUCCESS));

        TestCaseDetail testCase2 = testCases.get(2);
        assertThat(testCase2.getName(), is("ClassLibrary1.Test.Foo.Bar.Class1Tests.TestWillThrowExeption"));
        assertThat(testCase2.getStatus(), is(TestStatus.ERROR));
        assertThat(testCase2.getErrorMessage(), is("System.Exception : something bad happened"));
          //todo: assert stacktrace

        //todo assert xml values
    }

//
//    private void AssertInnerNestedClassesTestDetails(TestFileDetails details1) {
//        assertThat(details1.getErrors(), is(0));
//        assertThat(details1.getFailures(), is(0));
//        assertThat(details1.getSkipped(), is(0));
//        assertThat(details1.getTests(), is(1)) ;
//        assertThat(details1.getTimeMS(), is(2));
//
//        List<TestCaseDetail> testCases = details1.getDetails();
//        assertThat(testCases.size(), is(1));
//
//        TestCaseDetail testCase0 = testCases.get(0);
//        assertThat(testCase0.getName(), is("ClassLibrary1.Test.NestedClassesOutter+NestedClassesInner.InnerNestedTest"));
//        assertThat(testCase0.getStatus(), is(TestStatus.SUCCESS));
//
//
//    }
//
//    private void AssertOutterNestedClassesTestDetails(TestFileDetails details1) {
//        assertThat(details1.getErrors(), is(0));
//        assertThat(details1.getFailures(), is(0));
//        assertThat(details1.getSkipped(), is(0));
//        assertThat(details1.getTests(), is(1)) ;
//        assertThat(details1.getTimeMS(), is(2));
//
//        List<TestCaseDetail> testCases = details1.getDetails();
//        assertThat(testCases.size(), is(1));
//
//
//
//        TestCaseDetail testCase1 = testCases.get(0);
//        assertThat(testCase1.getName(), is("ClassLibrary1.Test.NestedClassesOutter.UnnestedTest"));
//        assertThat(testCase1.getStatus(), is(TestStatus.SUCCESS));
//
//    }
//
//
//    private void AssertDoubleNestedClassesTestDetails(TestFileDetails details1) {
//        assertThat(details1.getErrors(), is(0));
//        assertThat(details1.getFailures(), is(0));
//        assertThat(details1.getSkipped(), is(0));
//        assertThat(details1.getTests(), is(1)) ;
//        assertThat(details1.getTimeMS(), is(2));
//
//        List<TestCaseDetail> testCases = details1.getDetails();
//        assertThat(testCases.size(), is(1));
//
//
//        TestCaseDetail testCase0 = testCases.get(0);
//        assertThat(testCase0.getName(), is("ClassLibrary1.Test.NestedClassesOutter+NestedClassesInner+DoubleNestedClassesInner.DoubleNestedTest"));
//        assertThat(testCase0.getStatus(), is(TestStatus.SUCCESS));
//
//
//    }



    private void AssertNestedClassesTestDetails(TestFileDetails details1) {
        assertThat(details1.getErrors(), is(0));
        assertThat(details1.getFailures(), is(0));
        assertThat(details1.getSkipped(), is(0));
        assertThat(details1.getTests(), is(3)) ;
        assertThat(details1.getTimeMS(), is(0));

        List<TestCaseDetail> testCases = details1.getDetails();
        assertThat(testCases.size(), is(3));


        TestCaseDetail testCase1 = testCases.get(0);
        assertThat(testCase1.getName(), is("ClassLibrary1.Test.NestedClassesOutter.UnnestedTest"));
        assertThat(testCase1.getStatus(), is(TestStatus.SUCCESS));

        TestCaseDetail testCase0 = testCases.get(1);
        assertThat(testCase0.getName(), is("ClassLibrary1.Test.NestedClassesOutter+NestedClassesInner.InnerNestedTest"));
        assertThat(testCase0.getStatus(), is(TestStatus.SUCCESS));


        TestCaseDetail testCase2 = testCases.get(2);
        assertThat(testCase2.getName(), is("ClassLibrary1.Test.NestedClassesOutter+NestedClassesInner+DoubleNestedClassesInner.DoubleNestedTest"));
        assertThat(testCase2.getStatus(), is(TestStatus.SUCCESS));


    }


}
