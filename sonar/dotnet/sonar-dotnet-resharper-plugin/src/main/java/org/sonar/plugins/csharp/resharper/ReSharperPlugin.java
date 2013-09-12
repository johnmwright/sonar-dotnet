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

import org.sonar.api.*;
import org.sonar.plugins.csharp.resharper.profiles.ReSharperProfileExporter;
import org.sonar.plugins.csharp.resharper.profiles.ReSharperProfileImporter;
import org.sonar.plugins.csharp.resharper.profiles.SonarWayProfileCSharp;
import org.sonar.plugins.csharp.resharper.profiles.SonarWayProfileVbNet;
import org.sonar.plugins.dotnet.api.sensor.AbstractDotNetSensor;

import java.util.ArrayList;
import java.util.List;

/**
* Main class of the ReSharper plugin.
*/
@Properties({
  @Property(key = ReSharperConstants.MODE, defaultValue = AbstractDotNetSensor.MODE_SKIP, name = "ReSharper activation mode",
    description = "Possible values : empty (defaults to 'skip'), 'skip' and 'reuseReport'.", global = false, project = false,
    type = PropertyType.SINGLE_SELECT_LIST, options = {AbstractDotNetSensor.MODE_SKIP, AbstractDotNetSensor.MODE_REUSE_REPORT}),
  @Property(key = ReSharperConstants.REPORTS_PATH_KEY, defaultValue = "", name = "Name of the ReSharper report files",
    description = "Name of the ReSharper report file used when reuse report mode is activated. "
      + "This can be an absolute path, or a path relative to each project base directory.", global = false, project = false)
})
public class ReSharperPlugin extends SonarPlugin {

  /**
   * {@inheritDoc}
   */
  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> list = new ArrayList<Class<? extends Extension>>();

    // sensors
    list.add(ReSharperSensor.CSharpRegularReSharperSensor.class);
    list.add(ReSharperSensor.VbNetRegularReSharperSensor.class);

    // Rules and profiles
    list.add(ReSharperRuleRepositoryProvider.class);
    list.add(ReSharperProfileExporter.CSharpRegularReSharperProfileExporter.class);
    list.add(ReSharperProfileExporter.VbNetRegularReSharperProfileExporter.class);
    list.add(ReSharperProfileImporter.CSharpRegularReSharperProfileImporter.class);
    list.add(ReSharperProfileImporter.VbNetRegularReSharperProfileImporter.class);
    list.add(SonarWayProfileCSharp.class);
    list.add(SonarWayProfileVbNet.class);

    // Running ReSharper
    list.add(ReSharperResultParser.class);

    return list;
  }
}
