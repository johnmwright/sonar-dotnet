/*
 * Sonar C# Plugin :: Core
 * Copyright (C) 2010 Jose Chillan, Alexandre Victoor and SonarSource
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

package org.sonar.plugins.csharp.core;

import org.sonar.api.CoreProperties;
import org.sonar.api.Extension;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.csharp.api.CSharp;
import org.sonar.plugins.csharp.squid.CSharpResourcesBridge;
import org.sonar.plugins.csharp.squid.CSharpRuleProfile;
import org.sonar.plugins.csharp.squid.CSharpRuleRepository;
import org.sonar.plugins.csharp.squid.CSharpSquidConstants;
import org.sonar.plugins.csharp.squid.CSharpSquidSensor;
import org.sonar.plugins.csharp.squid.colorizer.CSharpSourceCodeColorizer;
import org.sonar.plugins.csharp.squid.cpd.CSharpCPDMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * C# Core plugin class.
 */
@Properties({
  @Property(key = CSharpSquidConstants.CPD_MINIMUM_TOKENS_PROPERTY, defaultValue = "" + CoreProperties.CPD_MINIMUM_TOKENS_DEFAULT_VALUE,
    name = "Minimum tokens",
    description = "The number of duplicate tokens above which a block is considered as a duplication in a C# program.", global = true,
    project = true, type = PropertyType.INTEGER),
  @Property(key = CSharpSquidConstants.CPD_IGNORE_LITERALS_PROPERTY, defaultValue = CSharpSquidConstants.CPD_IGNORE_LITERALS_DEFVALUE
    + "", name = "Ignore literals", description = "if true, CPD ignores literal value differences when evaluating a duplicate block. "
    + "This means that 'my first text'; and 'my second text'; will be seen as equivalent.", project = true, global = true,
    type = PropertyType.BOOLEAN),
  @Property(
    key = CSharpSquidConstants.IGNORE_HEADER_COMMENTS,
    defaultValue = "true",
    name = "Ignore header comments",
    description = "Set to 'true' to enable, or 'false' to disable.",
    project = true, global = true,
    type = PropertyType.BOOLEAN),
    @Property(
    key = CSharpSquidConstants.SSLR_INCLUDE_TEST_SOURCES_KEY,
    defaultValue = "false",
    name = "Include Test Sources",
    description = "If true, sources from test projects will be included in C# squid parsing.",
    project = true, global = true,
    type = PropertyType.BOOLEAN)
})
public class CSharpCorePlugin extends SonarPlugin {

  /**
   * {@inheritDoc}
   */
  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> extensions = new ArrayList<Class<? extends Extension>>();

    extensions.add(CSharp.class);
    extensions.add(CSharpProjectInitializer.class);

    // Sensors
    extensions.add(CSharpSourceImporter.class);

    // Common Rules
    extensions.add(CSharpCommonRulesEngineProvider.class);

    // C# Squid
    extensions.add(CSharpCPDMapping.class);
    extensions.add(CSharpSourceCodeColorizer.class);
    extensions.add(CSharpSquidSensor.class);
    extensions.add(CSharpResourcesBridge.class);

    // rules
    extensions.add(CSharpRuleRepository.class);
    extensions.add(CSharpRuleProfile.class);
    
    return extensions;
  }
}
