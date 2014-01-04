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
package org.sonar.plugins.dotnet.tests;

import org.sonar.api.*;
import org.sonar.plugins.dotnet.api.sensor.AbstractDotNetSensor;

import java.util.ArrayList;
import java.util.List;

/**
 * .NET Tests plugin class.
 */
@Properties({
  @Property(key = DotNetTestsConstants.MODE_KEY, defaultValue = AbstractDotNetSensor.MODE_REUSE_REPORT,
          name = "Activation mode", description = "Possible values : 'skip' and 'reuseReport' (default).",
    global = false, project = false, type = PropertyType.SINGLE_SELECT_LIST,
    options = {AbstractDotNetSensor.MODE_SKIP, AbstractDotNetSensor.MODE_REUSE_REPORT}),
  @Property(key = DotNetTestsConstants.IT_MODE_KEY, defaultValue = AbstractDotNetSensor.MODE_REUSE_REPORT,
        name = "Integration Tests Activation mode", description = "Possible values : 'skip' and 'reuseReport' (default).",
        global = false, project = false, type = PropertyType.SINGLE_SELECT_LIST,
        options = {AbstractDotNetSensor.MODE_SKIP, AbstractDotNetSensor.MODE_REUSE_REPORT}),
  @Property(key = DotNetTestsConstants.NUNIT_REPORTS_KEY, defaultValue = "", name = "Path of the NUnit test report file(s)",
    description = "Path to the NUnit test report file(s) used when reuse report mode is activated. "
      + "This can be an absolute path, or a path relative to the solution base directory.", global = false, project = false),
    @Property(key = DotNetTestsConstants.NUNIT_IT_REPORTS_KEY, defaultValue = "", name = "Path of the NUnit integration test report file(s)",
            description = "Path to the NUnit integration test report file(s) used when reuse report mode is activated. "
                    + "This can be an absolute path, or a path relative to the solution base directory.", global = false, project = false),
})
public class DotNetTestsPlugin extends SonarPlugin {

  /**
   * {@inheritDoc}
   */
  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> extensions = new ArrayList<Class<? extends Extension>>();

    // Sensors
    extensions.add(DotNetTestsSensor.class);
//    extensions.add(CoverageReportSensor.class);

    return extensions;
  }
}
