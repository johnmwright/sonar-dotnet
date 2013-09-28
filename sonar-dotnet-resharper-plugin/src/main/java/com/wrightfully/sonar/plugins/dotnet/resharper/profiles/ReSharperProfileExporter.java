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
package com.wrightfully.sonar.plugins.dotnet.resharper.profiles;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.SonarException;
import com.wrightfully.sonar.plugins.dotnet.resharper.ReSharperConstants;
import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.utils.ReSharperRule;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that allows to export a Sonar profile into a ReSharper rule definition file.
 */
public class ReSharperProfileExporter extends ProfileExporter {

    public static class CSharpRegularReSharperProfileExporter extends ReSharperProfileExporter {
        public CSharpRegularReSharperProfileExporter() {
            super("cs", ReSharperConstants.REPOSITORY_KEY, ReSharperConstants.REPOSITORY_NAME);
        }
    }

    public static class VbNetRegularReSharperProfileExporter extends ReSharperProfileExporter {
        public VbNetRegularReSharperProfileExporter() {
            super("vbnet", ReSharperConstants.REPOSITORY_KEY, ReSharperConstants.REPOSITORY_NAME);
        }
    }

    protected ReSharperProfileExporter(String languageKey, String repositoryKey, String repositoryName) {
        super(repositoryKey + "-" + languageKey, repositoryName);
        setSupportedLanguages(languageKey);
        setMimeType("application/xml");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportProfile(RulesProfile profile, Writer writer) {
        try {
            printRules(profile, writer);
        } catch (IOException e) {
            throw new SonarException("Error while generating the ReSharper profile to export: " + profile, e);
        }
    }

    private void printRules(RulesProfile profile, Writer writer) throws IOException {
        //Create a file that matches the format of the ReSharper inspectcode.exe output

        writer.append("<Report>\n");
        writer.append("  <IssueTypes>");

        List<ActiveRule> activeRules = profile.getActiveRulesByRepository(getKey());
        List<ReSharperRule> rules = transformIntoReSharperRules(activeRules);

        // print out each rule
        for (ReSharperRule rule : rules) {
            printRule(writer, rule);
        }

        writer.append("  </IssueTypes>");
        writer.append("</Report>");
    }


    private void printRule(Writer writer, ReSharperRule resharperRule) throws IOException {
        // This is generally what the output will look like:
        //        <IssueType Id="ClassNeverInstantiated.Global"
        //                   Enabled="True"
        //                   Category="Potential Code Quality Issues"
        //                   Description="Class is never instantiated: Non-private accessibility"
        //                   Severity="SUGGESTION" />

        writer.append("    <IssueType");
        writer.append(" Id=\"");
        StringEscapeUtils.escapeXml(writer, resharperRule.getId());
        writer.append("\" Enabled=\"True\"");
        writer.append(" Category=\"");
        StringEscapeUtils.escapeXml(writer, resharperRule.getCategory());
        writer.append("\" Description\"");
        StringEscapeUtils.escapeXml(writer, resharperRule.getDescription());
        writer.append("\" Severity\"");
        StringEscapeUtils.escapeXml(writer, resharperRule.getSeverity().toString());
        writer.append("\"/>\n");
    }

    private List<ReSharperRule> transformIntoReSharperRules(List<ActiveRule> activeRulesByPlugin) {
        List<ReSharperRule> result = new ArrayList<ReSharperRule>();

//        <rule key="ConvertToConstant.Global">
//          <name><![CDATA[ConvertToConstant.Global]]></name>
//          <configKey><![CDATA[ReSharperInspectCode#ConvertToConstant.Global]]></configKey>
//          <description><![CDATA[Convert local variable or field to constant: Non-private accessibility<br/>(Category: Common Practices and Code Improvements)]]></description>
//        </rule>


        for (ActiveRule activeRule : activeRulesByPlugin) {
            // Extracts the rule's information
            Rule rule = activeRule.getRule();
            String name = rule.getName();
            String rawDesc = rule.getDescription();

            String description = StringUtils.substringBefore(rawDesc, "<br/>(Category: ");
            String category = StringUtils.stripEnd(StringUtils.substringAfter(rawDesc, "<br/>(Category: "), ")");


            // Creates the ReSharper rule
            ReSharperRule resharperRule = new ReSharperRule();
            resharperRule.setEnabled(true);
            resharperRule.setId(name);
            resharperRule.setCategory(category);
            resharperRule.setDescription(description);

            RulePriority priority = activeRule.getSeverity();
            if (priority != null) {
                resharperRule.setSonarPriority(priority);
            }

            result.add(resharperRule);
        }
        return result;
    }



}
