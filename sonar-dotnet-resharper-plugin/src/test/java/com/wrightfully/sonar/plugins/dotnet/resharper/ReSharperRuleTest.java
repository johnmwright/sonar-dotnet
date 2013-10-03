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
import org.sonar.api.rules.*;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.Violation;
import org.sonar.plugins.dotnet.api.DotNetResourceBridge;
import org.sonar.plugins.dotnet.api.DotNetResourceBridges;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;
import org.sonar.plugins.dotnet.api.utils.ResourceHelper;
import org.sonar.test.TestUtils;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import java.io.File;
import java.nio.charset.Charset;
import org.sonar.api.rules.RulePriority;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperRule;
import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperRule.*;


import static org.fest.assertions.Assertions.assertThat;

public class ReSharperRuleTest {

    @Test
    public void testReSharperRuleAllFields() throws Exception {

        ReSharperRule reSharperRule = new ReSharperRule();
        reSharperRule.setId("MyId");
        reSharperRule.setEnabled(true);
        reSharperRule.setCategory("This is my category");
        reSharperRule.setDescription("A rather boring description");
        reSharperRule.setWikiLink("http://foo.bar/baz/bin");
        reSharperRule.setSeverity(ReSharperSeverity.HINT);

        Rule sonarRule = reSharperRule.toSonarRule();

        assertThat(sonarRule.getKey()).isEqualTo("MyId");
        assertThat(sonarRule.getName()).isEqualTo("MyId");
        assertThat(sonarRule.getSeverity()).isEqualTo(RulePriority.INFO);
        assertThat(sonarRule.getConfigKey()).isEqualTo("ReSharperInspectCode#MyId");
        assertThat(sonarRule.getDescription()).isEqualTo("A rather boring description<br />http://foo.bar/baz/bin<br />(Category: This is my category)");
    }


    @Test
    public void testReSharperRuleNoCategory() throws Exception {

        ReSharperRule reSharperRule = new ReSharperRule();
        reSharperRule.setId("MyId");
        reSharperRule.setEnabled(true);
        reSharperRule.setDescription("A rather boring description");
        reSharperRule.setWikiLink("http://foo.bar/baz/bin");
        reSharperRule.setSeverity(ReSharperSeverity.HINT);

        Rule sonarRule = reSharperRule.toSonarRule();

        assertThat(sonarRule.getKey()).isEqualTo("MyId");
        assertThat(sonarRule.getName()).isEqualTo("MyId");
        assertThat(sonarRule.getSeverity()).isEqualTo(RulePriority.INFO);
        assertThat(sonarRule.getConfigKey()).isEqualTo("ReSharperInspectCode#MyId");
        assertThat(sonarRule.getDescription()).isEqualTo("A rather boring description<br />http://foo.bar/baz/bin");
    }

    @Test
    public void testReSharperRuleNoWiki() throws Exception {

        ReSharperRule reSharperRule = new ReSharperRule();
        reSharperRule.setId("MyId");
        reSharperRule.setEnabled(true);
        reSharperRule.setCategory("This is my category");
        reSharperRule.setDescription("A rather boring description");
        reSharperRule.setSeverity(ReSharperSeverity.HINT);

        Rule sonarRule = reSharperRule.toSonarRule();

        assertThat(sonarRule.getKey()).isEqualTo("MyId");
        assertThat(sonarRule.getName()).isEqualTo("MyId");
        assertThat(sonarRule.getSeverity()).isEqualTo(RulePriority.INFO);
        assertThat(sonarRule.getConfigKey()).isEqualTo("ReSharperInspectCode#MyId");
        assertThat(sonarRule.getDescription()).isEqualTo("A rather boring description<br />(Category: This is my category)");
    }

    @Test
    public void testReSharperRuleNoCategoryNoWiki() throws Exception {

        ReSharperRule reSharperRule = new ReSharperRule();
        reSharperRule.setId("MyId");
        reSharperRule.setEnabled(true);
        reSharperRule.setDescription("A rather boring description");
        reSharperRule.setSeverity(ReSharperSeverity.HINT);

        Rule sonarRule = reSharperRule.toSonarRule();

        assertThat(sonarRule.getKey()).isEqualTo("MyId");
        assertThat(sonarRule.getName()).isEqualTo("MyId");
        assertThat(sonarRule.getSeverity()).isEqualTo(RulePriority.INFO);
        assertThat(sonarRule.getConfigKey()).isEqualTo("ReSharperInspectCode#MyId");
        assertThat(sonarRule.getDescription()).isEqualTo("A rather boring description");
    }



    @Test
    public void testReSharperRuleReSharperSeverityToSonarPriority() throws Exception {

        /*
            ReSharperSeverity.ERROR => RulePriority.BLOCKER;
            ReSharperSeverity.WARNING => RulePriority.CRITICAL;
            ReSharperSeverity.SUGGESTION => RulePriority.MINOR;
            ReSharperSeverity.HINT => RulePriority.INFO ;
        */


        ReSharperRule reSharperRule = new ReSharperRule();

        reSharperRule.setSeverity(ReSharperSeverity.ERROR);
        assertThat(reSharperRule.getSonarPriority()).isEqualTo(RulePriority.BLOCKER);


        reSharperRule.setSeverity(ReSharperSeverity.WARNING);
        assertThat(reSharperRule.getSonarPriority()).isEqualTo(RulePriority.CRITICAL);


        reSharperRule.setSeverity(ReSharperSeverity.SUGGESTION);
        assertThat(reSharperRule.getSonarPriority()).isEqualTo(RulePriority.MINOR);


        reSharperRule.setSeverity(ReSharperSeverity.HINT);
        assertThat(reSharperRule.getSonarPriority()).isEqualTo(RulePriority.INFO);
    }

    @Test
    public void testReSharperRuleSonarPriorityToReSharperSeverity() throws Exception {

        /*
            RulePriority.BLOCKER => ReSharperSeverity.ERROR;
            RulePriority.CRITICAL => ReSharperSeverity.WARNING ;
            RulePriority.MAJOR => ReSharperSeverity.WARNING ;
            RulePriority.MINOR => ReSharperSeverity.SUGGESTION;
            RulePriority.INFO => ReSharperSeverity.HINT;
        */


        ReSharperRule reSharperRule = new ReSharperRule();

        reSharperRule.setSonarPriority(RulePriority.BLOCKER);
        assertThat(reSharperRule.getSeverity()).isEqualTo(ReSharperSeverity.ERROR);


        reSharperRule.setSonarPriority(RulePriority.CRITICAL);
        assertThat(reSharperRule.getSeverity()).isEqualTo(ReSharperSeverity.WARNING);


        reSharperRule.setSonarPriority(RulePriority.MAJOR);
        assertThat(reSharperRule.getSeverity()).isEqualTo(ReSharperSeverity.WARNING);


        reSharperRule.setSonarPriority(RulePriority.MINOR);
        assertThat(reSharperRule.getSeverity()).isEqualTo(ReSharperSeverity.SUGGESTION);


        reSharperRule.setSonarPriority(RulePriority.INFO);
        assertThat(reSharperRule.getSeverity()).isEqualTo(ReSharperSeverity.HINT);
    }


    @Test
    public void testReSharperRuleCreateFromActiveRule() throws Exception {

        Rule sonarRule = Rule.create(ReSharperConstants.REPOSITORY_KEY+"-cs", "MyId", "MyId")
                .setDescription("A rather boring description<br />http://foo.bar/baz/bin<br />(Category: This is my category)")
                .setConfigKey("ReSharperInspectCode#MyId")
                .setSeverity(RulePriority.INFO);


        RulesProfile profile = RulesProfile.create("Sonar way", "cs");
        ActiveRule activeRule = profile.activateRule(sonarRule, RulePriority.INFO);

        ReSharperRule reSharperRule = ReSharperRule.createFromActiveRule(activeRule);
        assertThat(reSharperRule.getId()).isEqualTo("MyId");
        assertThat(reSharperRule.isEnabled()).isEqualTo(true);
        assertThat(reSharperRule.getDescription()).isEqualTo("A rather boring description<br />http://foo.bar/baz/bin<br />(Category: This is my category)");
        assertThat(reSharperRule.getSeverity()).isEqualTo(ReSharperSeverity.HINT);

    }


}
