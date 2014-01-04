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

/**
 * Constants of the Tests plugin.
 */
public final class DotNetTestsConstants {

    private DotNetTestsConstants() {
    }

    public static final String MODE_KEY = "sonar.dotnet.tests.mode";
    public static final String IT_MODE_KEY = "sonar.dotnet.tests.it.mode";

    //***** NUNIT
    public static final String NUNIT_REPORTS_KEY = "sonar.dotnet.tests.nunit.reports.path";
    public static final String NUNIT_IT_REPORTS_KEY = "sonar.dotnet.tests.nunit.it.reports.path";


}
