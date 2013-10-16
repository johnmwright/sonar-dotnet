/*
 * Sonar .NET Plugin :: ReSharper
 * Copyright (C) 2013 John M. Wright
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
package com.wrightfully.sonar.plugins.dotnet.resharper;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.Violation;
import org.sonar.plugins.dotnet.api.DotNetResourceBridge;
import org.sonar.plugins.dotnet.api.DotNetResourceBridges;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;
import org.sonar.plugins.dotnet.api.utils.ResourceHelper;
import org.sonar.test.TestUtils;
import org.mockito.ArgumentCaptor;

import java.lang.Throwable;
import java.util.List;
import java.io.File;
import java.lang.Exception;
import java.nio.charset.Charset;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

import static org.fest.assertions.Assertions.assertThat;

public class ReSharperResultParserTest {

    private SensorContext _context;
    private DotNetResourceBridge _resourcesBridge;
    private ReSharperResultParser _parser;
    private Project _project;

    private Rule _rudRule;
    private Rule _cnigRule;
    private Rule _uplRule;
    private Rule _suvkeRule;
    private Rule _suvke2Rule;
    private Rule _rtscRule;
    private Rule _missingRule;


    @Before
    public void init() throws Exception {
        _context = mock(SensorContext.class);

        _resourcesBridge = mock(DotNetResourceBridge.class);
        when(_resourcesBridge.getLanguageKey()).thenReturn("cs");
        DotNetResourceBridges bridges = new DotNetResourceBridges(new DotNetResourceBridge[] {_resourcesBridge});

        File resultFile = TestUtils.getResource("/solution/Example/resharper-results-example_sln.xml");
        final File soltionFolder = new File(resultFile.getParent());
        if (!soltionFolder.exists())
            throw new Exception("Solution folder path does not exist: " + soltionFolder.getPath());
        String sourcePath = soltionFolder.getCanonicalPath();

        _project = mock(Project.class);
        ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
        when(fileSystem.getSourceDirs()).thenReturn(Lists.newArrayList(new File(sourcePath)));
        when(fileSystem.getSourceCharset()).thenReturn(Charset.forName("UTF-8"));
        when(_project.getFileSystem()).thenReturn(fileSystem);
        when(_project.getLanguageKey()).thenReturn("cs");
        when(_project.getName()).thenReturn("Example.Application");

        when(fileSystem.resolvePath(anyString())).thenAnswer(new Answer<File>(){
            public File answer(InvocationOnMock invocation) throws Throwable {
                String param = (String)invocation.getArguments()[0];
                return new File(soltionFolder + "/" + param);
            }
        });

        MicrosoftWindowsEnvironment env = mock(MicrosoftWindowsEnvironment.class);
        VisualStudioProject vsProject = mock(VisualStudioProject.class);
        VisualStudioSolution solution = mock(VisualStudioSolution.class);

        when(env.getCurrentSolution()).thenReturn(solution);
        when(vsProject.getName()).thenReturn("Example.Application");
        when(vsProject.contains(any(File.class))).thenReturn(true);
        when(solution.getProjectFromSonarProject(any(Project.class))).thenReturn(vsProject);
        when(solution.getProject(any(File.class))).thenReturn(vsProject);

        ResourceHelper resourceHelper = mock(ResourceHelper.class);
        when(resourceHelper.isResourceInProject(any(Resource.class), any(Project.class))).thenReturn(true);

        _parser = new ReSharperResultParser(env, _project, _context, newRuleFinder(), bridges, resourceHelper);

        _rudRule = Rule.create(ReSharperConstants.REPOSITORY_KEY, "RedundantUsingDirective", "RedundantUsingDirective")
                .setConfigKey("ReSharperInspectCode#RedundantUsingDirective");

        _cnigRule= Rule.create(ReSharperConstants.REPOSITORY_KEY, "ClassNeverInstantiated.Global", "ClassNeverInstantiated.Global")
                .setConfigKey("ReSharperInspectCode#ClassNeverInstantiated.Global");

        _uplRule = Rule.create(ReSharperConstants.REPOSITORY_KEY, "UnusedParameter.Local", "UnusedParameter.Local")
                .setConfigKey("ReSharperInspectCode#UnusedParameter.Local");

        _suvkeRule= Rule.create(ReSharperConstants.REPOSITORY_KEY, "SuggestUseVarKeywordEvident", "SuggestUseVarKeywordEvident")
                .setConfigKey("ReSharperInspectCode#SuggestUseVarKeywordEvident");

        _suvke2Rule= Rule.create(ReSharperConstants.REPOSITORY_KEY, "SuggestUseVarKeywordEverywhere", "SuggestUseVarKeywordEverywhere")
                .setConfigKey("ReSharperInspectCode#SuggestUseVarKeywordEverywhere");

        _rtscRule= Rule.create(ReSharperConstants.REPOSITORY_KEY, "RedundantToStringCall", "RedundantToStringCall")
                .setConfigKey("ReSharperInspectCode#RedundantToStringCall");

        _missingRule= Rule.create(ReSharperConstants.REPOSITORY_KEY, "Sonar.UnknownIssueType", "Sonar.UnknownIssueType")
                .setConfigKey("ReSharperInspectCode#Sonar.UnknownIssueType");

    }


    @Test
    public void testParseFileReturnsAllIssues() throws Exception{

        ArgumentCaptor<Violation> violationArg = ArgumentCaptor.forClass(Violation.class);
        Violation capturedViolation = null;

        File resultFile = TestUtils.getResource("/solution/Example/resharper-results-example_sln.xml");
        _parser.parse(resultFile);

        // Verify calls on context to save violations
        verify(_context, times(13)).saveViolation(violationArg.capture());
        List<Violation> capturedViolations = violationArg.getAllValues();


//        <Issue TypeId="RedundantUsingDirective" File="Example.Application\Program.cs" Offset="910-943" Line="22" Message="Using directive is not required by the code and can be safely removed" />
        capturedViolation = capturedViolations.get(0);
        assertViolation(capturedViolation,  _rudRule,  "Program.cs", 22, "Using directive is not required by the code and can be safely removed" );


//        <Issue TypeId="RedundantUsingDirective" File="Example.Application\Program.cs" Offset="945-963" Line="23" Message="Using directive is not required by the code and can be safely removed" />
        capturedViolation = capturedViolations.get(1);
        assertViolation(capturedViolation,  _rudRule,  "Program.cs", 23, "Using directive is not required by the code and can be safely removed" );

//        <Issue TypeId="RedundantUsingDirective" File="Example.Application\Program.cs" Offset="965-983" Line="24" Message="Using directive is not required by the code and can be safely removed" />
        capturedViolation = capturedViolations.get(2);
        assertViolation(capturedViolation,  _rudRule,  "Program.cs", 24, "Using directive is not required by the code and can be safely removed" );

//        <Issue TypeId="ClassNeverInstantiated.Global" File="Example.Application\Program.cs" Offset="1050-1057" Line="29" Message="Class 'Program' is never instantiated" />
        capturedViolation = capturedViolations.get(3);
        assertViolation(capturedViolation,  _cnigRule,  "Program.cs", 29, "Class 'Program' is never instantiated" );

//        <Issue TypeId="UnusedParameter.Local" File="Example.Application\Program.cs" Offset="1094-1098" Line="31" Message="Parameter 'args' is never used" />
        capturedViolation = capturedViolations.get(4);
        assertViolation(capturedViolation,  _uplRule,  "Program.cs", 31, "Parameter 'args' is never used" );

//        <Issue TypeId="SuggestUseVarKeywordEvident" File="Example.Application\Program.cs" Offset="1114-1122" Line="33" Message="Use implicitly typed local variable declaration" />
        capturedViolation = capturedViolations.get(5);
        assertViolation(capturedViolation,  _suvkeRule,  "Program.cs", 33, "Use implicitly typed local variable declaration" );

//        <Issue TypeId="SuggestUseVarKeywordEvident" File="Example.Application\Program.cs" Offset="1152-1157" Line="34" Message="Use implicitly typed local variable declaration" />
        capturedViolation = capturedViolations.get(6);
        assertViolation(capturedViolation,  _suvkeRule,  "Program.cs", 34, "Use implicitly typed local variable declaration" );

//        <Issue TypeId="SuggestUseVarKeywordEvident" File="Example.Application\Program.cs" Offset="1196-1201" Line="35" Message="Use implicitly typed local variable declaration" />
        capturedViolation = capturedViolations.get(7);
        assertViolation(capturedViolation,  _suvkeRule,  "Program.cs", 35, "Use implicitly typed local variable declaration" );

//        <Issue TypeId="SuggestUseVarKeywordEvident" File="Example.Application\Program.cs" Offset="1240-1245" Line="36" Message="Use implicitly typed local variable declaration" />
        capturedViolation = capturedViolations.get(8);
        assertViolation(capturedViolation,  _suvkeRule,  "Program.cs", 36, "Use implicitly typed local variable declaration" );

//        <Issue TypeId="SuggestUseVarKeywordEvident" File="Example.Application\Program.cs" Offset="1284-1289" Line="37" Message="Use implicitly typed local variable declaration" />
        capturedViolation = capturedViolations.get(9);
        assertViolation(capturedViolation,  _suvkeRule,  "Program.cs", 37, "Use implicitly typed local variable declaration" );

//        <Issue TypeId="SuggestUseVarKeywordEverywhere" File="Example.Application\Program.cs" Offset="1328-1334" Line="38" Message="Use implicitly typed local variable declaration" />
        capturedViolation = capturedViolations.get(10);
        assertViolation(capturedViolation,  _suvke2Rule,  "Program.cs", 38, "Use implicitly typed local variable declaration" );

//        <Issue TypeId="RedundantToStringCall" File="Example.Application\Program.cs" Offset="1533-1541" Line="42" Message="Redundant 'Object.ToString()' call" />
        capturedViolation = capturedViolations.get(11);
        assertViolation(capturedViolation,  _rtscRule,  "Program.cs", 42, "Redundant 'Object.ToString()' call" );

//        <Issue TypeId="RedundantUsingDirective" File="Example.Application\Properties\AssemblyInfo.cs" Offset="921-959" Line="22" Message="Using directive is not required by the code and can be safely removed" />
        capturedViolation = capturedViolations.get(12);
        assertViolation(capturedViolation,  _rudRule,  "AssemblyInfo.cs", 22, "Using directive is not required by the code and can be safely removed" );

    }

    @Test
    public void testParseFileWithUnknownRules() throws Exception{

        ArgumentCaptor<Violation> violationArg = ArgumentCaptor.forClass(Violation.class);
        Violation capturedViolation = null;

        File resultFile = TestUtils.getResource("/solution/Example/resharper-results-example_sln-missing.xml");
        _parser.parse(resultFile);

        // Verify calls on context to save violations
        verify(_context, times(1)).saveViolation(violationArg.capture());
        List<Violation> capturedViolations = violationArg.getAllValues();

        capturedViolation = capturedViolations.get(0);

        assertThat(capturedViolation.getRule()).isEqualTo(_missingRule);

        Resource resource = capturedViolation.getResource();
        assertThat(resource).isNotNull();
        assertThat(resource).isEqualTo(_project);

        assertThat(capturedViolation.getLineId()).isNull();

        String expectedMessage = "The following IssueTypes are not known to the SonarQube ReSharper plugin.\n"+
        "Add the following text to the 'ReSharper custom rules' property in the Settings UI to add local support for these rules and submit them to https://github.com/johnmwright/sonar-dotnet-resharper/issues so that they can be included in future releases.\n"+
        "<IssueType Id=\"UnknownRule1\" Category=\"Redundancies in Symbol Declarations\" Description=\"Type or type member is never used: Non-private accessibility\" Severity=\"SUGGESTION\" />\n"+
        "<IssueType Id=\"UnknownRule2\" Category=\"Redundancies in Symbol Declarations\" Description=\"Type member is never accessed via base type: Non-private accessibility\" Severity=\"SUGGESTION\" />\n";

        assertThat(capturedViolation.getMessage()).isEqualTo(expectedMessage);

    }

    private void assertViolation(Violation violation, Rule expectedRule, String expectedResourceName, int expectedLineNumber, String expectedMessage )
    {
        assertThat(violation.getRule()).isEqualTo(expectedRule);

        if (expectedResourceName == null)
        {
            assertThat(violation.getResource()).isNull();
        } else {
            Resource resource = violation.getResource();
            assertThat(resource).isNotNull();
            assertThat(resource.getName()).isEqualTo(expectedResourceName);
        }
        assertThat(violation.getLineId()).isEqualTo(expectedLineNumber);
        assertThat(violation.getMessage()).isEqualTo(expectedMessage);
    }

    private RuleFinder newRuleFinder() {
        RuleFinder ruleFinder = mock(RuleFinder.class);
        when(ruleFinder.find((RuleQuery) anyObject())).thenAnswer(new Answer<Rule>() {

            public Rule answer(InvocationOnMock iom) throws Throwable {
                RuleQuery query = (RuleQuery) iom.getArguments()[0];

                String ruleKey = query.getConfigKey();

                if (ruleKey.equals("ReSharperInspectCode#RedundantUsingDirective")) {
                        return _rudRule;
                } else if (ruleKey.equals("ReSharperInspectCode#ClassNeverInstantiated.Global")) {
                    return _cnigRule;
                } else if (ruleKey.equals("ReSharperInspectCode#UnusedParameter.Local"))   {
                    return _uplRule;
                } else if (ruleKey.equals("ReSharperInspectCode#SuggestUseVarKeywordEvident")){
                    return _suvkeRule;
                } else if (ruleKey.equals("ReSharperInspectCode#SuggestUseVarKeywordEverywhere")){
                    return _suvke2Rule;
                } else if (ruleKey.equals("ReSharperInspectCode#RedundantToStringCall")){
                    return _rtscRule;
                } else if (ruleKey.equals("ReSharperInspectCode#UnknownRule1")) {
                    return null;
                } else if (ruleKey.equals("ReSharperInspectCode#UnknownRule2")) {
                    return null;
                } else if (ruleKey.equals("ReSharperInspectCode#UnknownRule3")) {
                    return null;
                } else if (ruleKey.equals("ReSharperInspectCode#Sonar.UnknownIssueType")) {
                    return _missingRule;
                } else {
                    Rule fakeRule = Rule.create(ReSharperConstants.REPOSITORY_KEY, query.getKey(), "Fake rule")
                            .setConfigKey(query.getConfigKey());
                    return fakeRule;
                }

            }
        });
        return ruleFinder;
    }



}
