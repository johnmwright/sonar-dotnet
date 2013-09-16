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
package org.sonar.plugins.csharp.resharper;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.rules.XMLRuleParser;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the ReSharper rules configuration file.
 */
public class ReSharperRuleRepository extends RuleRepository {

    private String repositoryKey;
    private ServerFileSystem fileSystem;
    private XMLRuleParser xmlRuleParser;
    private Settings settings;

    public ReSharperRuleRepository(String repoKey, String languageKey, ServerFileSystem fileSystem, XMLRuleParser xmlRuleParser,
                                   Settings settings) {
        super(repoKey, languageKey);
        setName(ReSharperConstants.REPOSITORY_NAME);
        this.repositoryKey = repoKey;
        this.fileSystem = fileSystem;
        this.xmlRuleParser = xmlRuleParser;
        this.settings = settings;
    }

    @Override
    public List<Rule> createRules() {
        List<Rule> rules = new ArrayList<Rule>();

        //TODO: for each of these, allow the user to use the ReSharper report style of:
        // <IssueTypes>
        //   <IssueType .../>
        // so that they can just copy/paste from the report files and not convert to sonar-specific format

        // ReSharper rules
        rules.addAll(xmlRuleParser.parse(ReSharperRuleRepository.class.getResourceAsStream("/org/sonar/plugins/csharp/resharper/rules/rules.xml")));

        // Custom rules:
        // - old fashion: XML files in the file system
        for (File userExtensionXml : fileSystem.getExtensions(repositoryKey, "xml")) {
            rules.addAll(xmlRuleParser.parse(userExtensionXml));
        }

        // - new fashion: through the Web interface
        String customRules = settings.getString(ReSharperConstants.CUSTOM_RULES_PROP_KEY);
        if (StringUtils.isNotBlank(customRules)) {
            rules.addAll(xmlRuleParser.parse(new StringReader(customRules)));
        }

        return rules;
    }

}
