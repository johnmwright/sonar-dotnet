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
package org.sonar.plugins.csharp.resharper.profiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.csharp.resharper.ReSharperConstants;
import org.sonar.plugins.csharp.resharper.profiles.utils.ReSharperRule;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
* Class that allows to import ReSharper rule definition files into a Sonar Rule Profile
*/
public class ReSharperProfileImporter extends ProfileImporter {


    private static final Logger LOG = LoggerFactory.getLogger(ReSharperProfileImporter.class);

    private RuleFinder ruleFinder;
  private String languageKey;

  public static class CSharpRegularReSharperProfileImporter extends ReSharperProfileImporter {
    public CSharpRegularReSharperProfileImporter(RuleFinder ruleFinder) {
      super("cs", ReSharperConstants.REPOSITORY_KEY, ReSharperConstants.REPOSITORY_NAME, ruleFinder);
    }
  }

  public static class VbNetRegularReSharperProfileImporter extends ReSharperProfileImporter {
    public VbNetRegularReSharperProfileImporter(RuleFinder ruleFinder) {
      super("vbnet", ReSharperConstants.REPOSITORY_KEY, ReSharperConstants.REPOSITORY_NAME, ruleFinder);
    }
  }


  protected ReSharperProfileImporter(String languageKey, String repositoryKey, String repositoryName, RuleFinder ruleFinder) {
    super(repositoryKey + "-" + languageKey, repositoryName);
    setSupportedLanguages(languageKey);
    this.ruleFinder = ruleFinder;
    this.languageKey = languageKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create();
    profile.setLanguage(languageKey);

//        <IssueType Id="ClassNeverInstantiated.Global"
//           Enabled="True"
//                   Category="Potential Code Quality Issues"
//                   Description="Class is never instantiated: Non-private accessibility"
//                   Severity="SUGGESTION" />


      List<ReSharperRule> rules = parseRules(reader, messages);

      for (ReSharperRule reSharperRule : rules) {
          String ruleName = reSharperRule.getId();
          Rule rule = ruleFinder.find(RuleQuery.create().withRepositoryKey(getKey()).withKey(ruleName));

          if (rule != null) {
              RulePriority sonarPriority = reSharperRule.getSonarPriority();
              profile.activateRule(rule, sonarPriority);
              LOG.info("Activating profile rule " + rule.getKey() + " with priority " + sonarPriority);
          }
      }

      return profile;
  }

    private List<ReSharperRule> parseRules(Reader reader, ValidationMessages messages) {
        List<ReSharperRule> result = new ArrayList<ReSharperRule>();

        try {
//            InputSource source = new InputSource(IOUtils.toString(reader));

//            StringWriter writer = new StringWriter();
//            IOUtils.copy(reader, writer);
//            String theString = writer.toString();
//            LOG.info("FILE: " + theString);
           // throw new SonarException("FILE: " + theString);

            InputSource source = new InputSource(reader);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            LOG.info("Running XPATH for //IssueType");
            NodeList nodes = (NodeList) xpath.evaluate("//IssueType",source, XPathConstants.NODESET);

            if (nodes == null)  {
                LOG.info("no IssueType nodes found in profile file");
            }
            else {

                int count = nodes.getLength();
                LOG.info("Found " + count + " nodes" );

                // For each rule we extract the elements
                for (int idxRule = 0; idxRule < count; idxRule++) {
                    Element ruleElement = (Element) nodes.item(idxRule);
                    ReSharperRule rule = new ReSharperRule();
                    String ruleId = ruleElement.getAttribute("Id");
                    rule.setId(ruleId);

                    String active = ruleElement.getAttribute("Enabled");
                    rule.setEnabled(active.toLowerCase().contains("true"));

                    String category = ruleElement.getAttribute("Category");
                    rule.setCategory(category);

                    String description = ruleElement.getAttribute("Description");
                    rule.setDescription(description);

                    String severity = ruleElement.getAttribute("Severity");
                    rule.setSeverity(ReSharperRule.ReSharperSeverity.valueOf(severity));

                    result.add(rule);
                }
            }
        } catch (XPathExpressionException e) {
          messages.addErrorText("xpath exception while parsing resharper config file: " + e.getMessage());

        } catch (Exception e) {
          messages.addErrorText("Failed to read the profile to import: " + e.getMessage());
      }
        return result;
    }

}
