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

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.ValidationMessages;

import java.io.InputStreamReader;

public final class SonarWayProfileVbNet extends ProfileDefinition {

  private ReSharperProfileImporter profileImporter;

  public SonarWayProfileVbNet(ReSharperProfileImporter.VbNetRegularReSharperProfileImporter profileImporter) {
    this.profileImporter = profileImporter;
  }

  public RulesProfile createProfile(ValidationMessages messages) {
    RulesProfile profile = profileImporter.importProfile(
        new InputStreamReader(getClass().getResourceAsStream("/org/sonar/plugins/csharp/resharper/rules/DefaultRules.ReSharper")), messages);
    profile.setLanguage("vbnet");
    profile.setName("Sonar way");
    return profile;
  }
}
