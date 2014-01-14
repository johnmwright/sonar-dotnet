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

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.plugins.csharp.api.CSharp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CSharpSourceImporterTest {

  private CSharp language;
  private CSharpSourceImporter importer;
  private Configuration configuration;
  private Project project;

  @Before
  public void init() {
    language = mock(CSharp.class);
    importer = new CSharpSourceImporter(language);

      configuration= mock(Configuration.class);
      when(configuration.getBoolean(CoreProperties.CORE_IMPORT_SOURCES_PROPERTY,
              CoreProperties.CORE_IMPORT_SOURCES_DEFAULT_VALUE)).thenReturn(true);

      project = mock(Project.class);
      when(project.getConfiguration()).thenReturn(configuration);
  }

  @Test
  public void testShouldNotExecuteOnRootProject() {
    when(project.isRoot()).thenReturn(true);
    when(project.getLanguage()).thenReturn(language);
    assertFalse(importer.shouldExecuteOnProject(project));
  }

  @Test
  public void testShouldNotExecuteOnOtherLanguageProject() {
    AbstractLanguage java = mock(Java.class);
    when(project.getName()).thenReturn("Project #1");
    when(project.getLanguage()).thenReturn(java);
    assertFalse(importer.shouldExecuteOnProject(project));
  }

  @Test
  public void testShouldExecuteOnNormalProject() {
    when(project.getName()).thenReturn("Project #1");
    when(project.getLanguage()).thenReturn(language);
    assertTrue(importer.shouldExecuteOnProject(project));
  }

  @Test
  public void testShouldExecuteOnTestProject() {
    when(project.getName()).thenReturn("Project Test");
    when(project.getLanguage()).thenReturn(language);
    assertTrue(importer.shouldExecuteOnProject(project));
  }

}
