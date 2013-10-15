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

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.ValidationMessages;

import java.io.InputStreamReader;

public class ReSharperSonarWayProfile extends ProfileDefinition {

    public final class SonarWayProfileCSharp extends ReSharperSonarWayProfile {
        public SonarWayProfileCSharp(ReSharperProfileImporter.CSharpRegularReSharperProfileImporter profileImporter) {
            super(profileImporter, "cs");
        }
    }


    public final class SonarWayProfileVbNet extends ReSharperSonarWayProfile {
        public SonarWayProfileVbNet(ReSharperProfileImporter.VbNetRegularReSharperProfileImporter profileImporter) {
            super(profileImporter, "vbnet");
        }
    }

    private ReSharperProfileImporter profileImporter;
    private String languageKey;

    protected ReSharperSonarWayProfile(ReSharperProfileImporter profileImporter, String languageKey) {
        this.profileImporter = profileImporter;
        this.languageKey = languageKey;
    }

    public RulesProfile createProfile(ValidationMessages messages) {
        RulesProfile profile = profileImporter.importProfile(
                new InputStreamReader(getClass().getResourceAsStream("/com/wrightfully/sonar/plugins/dotnet/resharper/rules/DefaultRules.ReSharper")), messages);
        profile.setLanguage(languageKey);
        profile.setName("Sonar way");
        return profile;
    }

}