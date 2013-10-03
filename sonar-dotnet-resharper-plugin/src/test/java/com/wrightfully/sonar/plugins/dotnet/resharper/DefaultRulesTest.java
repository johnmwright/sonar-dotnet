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
import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperFileParser;

import java.io.File;
import java.nio.charset.Charset;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStreamReader;
import java.util.HashSet;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

import static org.fest.assertions.Assertions.assertThat;


public class DefaultRulesTest {

    List<ReSharperRule> _rules;

    @Before
    public void init() {
        Reader fileReader = new InputStreamReader(getClass().getResourceAsStream("/com/wrightfully/sonar/plugins/dotnet/resharper/rules/DefaultRules.ReSharper"));
        _rules = ReSharperFileParser.parseRules(fileReader);
    }

    @Test
    public void testDefaultRulesContainsNoDuplicates() throws Exception {
        assertThat(_rules).onProperty("id").doesNotHaveDuplicates();
    }

    @Test
    public void testDefaultRulesContainsSpecialUnknownTypeRule() throws Exception {
        assertThat(_rules).onProperty("id").contains("Sonar.UnknownIssueType");
    }
}