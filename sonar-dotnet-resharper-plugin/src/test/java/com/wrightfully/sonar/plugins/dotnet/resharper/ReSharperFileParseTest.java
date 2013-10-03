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

import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperRule;
import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperRule.ReSharperSeverity;
import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperFileParser;

import java.io.File;
import java.nio.charset.Charset;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.sonar.api.utils.ValidationMessages;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

import static org.fest.assertions.Assertions.assertThat;


public class ReSharperFileParseTest {
    @Before
    public void init() {

    }

    @Test
    public void testEmptyFileCreatesError() throws Exception {

        //Arrange
        String emptyFileValue = "";
        Reader fakeFileReader = new StringReader(emptyFileValue);
        ValidationMessages messages = ValidationMessages.create();

        //Act
        List<ReSharperRule> results = ReSharperFileParser.parseRules(fakeFileReader, messages);

        //Assert
        assertThat(results).isEmpty();
        assertThat(messages.hasErrors()).isTrue();
        assertThat(messages.getErrors()).hasSize(1);
        assertThat((String)(messages.getErrors().toArray()[0])).startsWith("xpath exception while parsing resharper config file");
    }


    @Test
    public void testNonXmlFileCreatesError() throws Exception {

        //Arrange
        String badFileValue = "This is a text file";
        Reader fakeFileReader = new StringReader(badFileValue);
        ValidationMessages messages = ValidationMessages.create();

        //Act
        List<ReSharperRule> results = ReSharperFileParser.parseRules(fakeFileReader, messages);

        //Assert
        assertThat(results).isEmpty();
        assertThat(messages.hasErrors()).isTrue();
        assertThat(messages.getErrors()).hasSize(1);
        assertThat((String)(messages.getErrors().toArray()[0])).startsWith("xpath exception while parsing resharper config file");
    }


    @Test
    public void testFileWithNoIssueTypeNodesCreatesError() throws Exception {

        //Arrange
        String badFileValue = "<Results><IssueTypes></IssueTypes></Results>";
        Reader fakeFileReader = new StringReader(badFileValue);
        ValidationMessages messages = ValidationMessages.create();

        //Act
        List<ReSharperRule> results = ReSharperFileParser.parseRules(fakeFileReader, messages);

        //Assert
        assertThat(results).isEmpty();
        assertThat(messages.hasErrors()).isTrue();
        assertThat(messages.getErrors()).containsOnly("No IssueType nodes found in profile file");
    }


    @Test
    public void testSingleIssueTypeMappingWithWiki() throws Exception {

        //Arrange
        String badFileValue = "<Results><IssueTypes>" +
                "<IssueType Id=\"SuggestUseVarKeywordEvident\" Category=\"Language Usage Opportunities\" Description=\"Use 'var' keyword when initializer explicitly declares type\" Severity=\"SUGGESTION\" WikiUrl=\"http://confluence.jetbrains.net/display/ReSharper/Use+'var'+keyword+when+initializer+explicitly+declares+type\" />" +
                "</IssueTypes></Results>";
        Reader fakeFileReader = new StringReader(badFileValue);
        ValidationMessages messages = ValidationMessages.create();

        //Act
        List<ReSharperRule> results = ReSharperFileParser.parseRules(fakeFileReader, messages);

        //Assert
        assertThat(results).hasSize(1);
        assertThat(messages.hasErrors()).isFalse();
        assertThat(messages.hasWarnings()).isFalse();
        assertThat(messages.hasInfos()).isFalse();

        ReSharperRule rule = results.get(0);
        assertThat(rule.getId()).isEqualTo("SuggestUseVarKeywordEvident");
        assertThat(rule.getCategory()).isEqualTo("Language Usage Opportunities");
        assertThat(rule.getDescription()).isEqualTo("Use 'var' keyword when initializer explicitly declares type");
        assertThat(rule.getWikiLink()).isEqualTo("http://confluence.jetbrains.net/display/ReSharper/Use+'var'+keyword+when+initializer+explicitly+declares+type");
        assertThat(rule.getSeverity()).isEqualTo(ReSharperSeverity.SUGGESTION);

    }

    @Test
    public void testSingleIssueTypeMappingWithNoWiki() throws Exception {

        //Arrange
        String badFileValue = "<Results><IssueTypes>" +
                "<IssueType Id=\"SuggestUseVarKeywordEvident\" Category=\"Language Usage Opportunities\" Description=\"Use 'var' keyword when initializer explicitly declares type\" Severity=\"SUGGESTION\"  />" +
                "</IssueTypes></Results>";
        Reader fakeFileReader = new StringReader(badFileValue);
        ValidationMessages messages = ValidationMessages.create();

        //Act
        List<ReSharperRule> results = ReSharperFileParser.parseRules(fakeFileReader, messages);

        //Assert
        assertThat(results).hasSize(1);
        assertThat(messages.hasErrors()).isFalse();
        assertThat(messages.hasWarnings()).isFalse();
        assertThat(messages.hasInfos()).isFalse();

        ReSharperRule rule = results.get(0);
        assertThat(rule.getId()).isEqualTo("SuggestUseVarKeywordEvident");
        assertThat(rule.getCategory()).isEqualTo("Language Usage Opportunities");
        assertThat(rule.getDescription()).isEqualTo("Use 'var' keyword when initializer explicitly declares type");
        assertThat(rule.getWikiLink()).isEqualTo("");
        assertThat(rule.getSeverity()).isEqualTo(ReSharperSeverity.SUGGESTION);

    }

    @Test
    public void testMultipleIssueTypeMappingReturnsCorrectNumberOfRules() throws Exception {

        //Arrange
        String fileValue = "<Results><IssueTypes>" +
                "    <IssueType Id=\"ReturnTypeCanBeEnumerable.Global\" Category=\"Common Practices and Code Improvements\" Description=\"Return type can be IEnumerable&lt;T&gt;: Non-private accessibility\" Severity=\"SUGGESTION\" />\n" +
                "    <IssueType Id=\"SuggestUseVarKeywordEvident\" Category=\"Language Usage Opportunities\" Description=\"Use 'var' keyword when initializer explicitly declares type\" Severity=\"SUGGESTION\" WikiUrl=\"http://confluence.jetbrains.net/display/ReSharper/Use+'var'+keyword+when+initializer+explicitly+declares+type\" />\n" +
                "    <IssueType Id=\"UnassignedField.Compiler\" Category=\"Compiler Warnings\" Description=\"Unassigned field\" Severity=\"WARNING\" />\n" +
                "</IssueTypes></Results>";
        Reader fakeFileReader = new StringReader(fileValue);
        ValidationMessages messages = ValidationMessages.create();

        //Act
        List<ReSharperRule> results = ReSharperFileParser.parseRules(fakeFileReader, messages);

        //Assert
        assertThat(results).hasSize(3);
        assertThat(messages.hasErrors()).isFalse();
        assertThat(messages.hasWarnings()).isFalse();
        assertThat(messages.hasInfos()).isFalse();

    }
}