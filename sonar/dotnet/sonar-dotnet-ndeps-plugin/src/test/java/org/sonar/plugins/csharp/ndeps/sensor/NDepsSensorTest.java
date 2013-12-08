/*
 * Sonar .NET Plugin :: NDeps
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
package org.sonar.plugins.csharp.ndeps.sensor;

import org.sonar.plugins.csharp.ndeps.NDepsPlugin;
import org.sonar.plugins.csharp.ndeps.sensor.NDepsSensor;
import org.sonar.plugins.csharp.ndeps.common.NDepsConstants;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.dotnet.tools.ndeps.NDepsCommandBuilder;
import org.sonar.dotnet.tools.ndeps.NDepsRunner;
import org.sonar.plugins.dotnet.api.DotNetConfiguration;
import org.sonar.plugins.dotnet.api.DotNetConstants;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;
import org.sonar.plugins.dotnet.core.DotNetCorePlugin;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NDepsSensorTest {

  private ProjectFileSystem fileSystem;
  private VisualStudioSolution solution;
  private VisualStudioProject vsProject1;
  private VisualStudioProject vsProject2;
  private VisualStudioProject vsProject3;
  private MicrosoftWindowsEnvironment microsoftWindowsEnvironment;
  private Settings configuration;
  private NDepsResultParser nDepsResultParser;
  private NDepsSensor nDepsSensor;
  private File reportFile;

  @Before
  public void init() {
    fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSonarWorkingDirectory()).thenReturn(TestUtils.getResource("/Sensor"));

    vsProject1 = mock(VisualStudioProject.class);
    when(vsProject1.getName()).thenReturn("Project #1");
    when(vsProject1.getGeneratedAssemblies(anyString(), anyString())).thenReturn(Collections.singleton(new File("toto.dll")));
    when(vsProject1.getDirectory()).thenReturn(TestUtils.getResource("/"));
    vsProject2 = mock(VisualStudioProject.class);
    when(vsProject2.getName()).thenReturn("Project Test");
    when(vsProject2.isTest()).thenReturn(true);
    vsProject3 = mock(VisualStudioProject.class);
    when(vsProject3.getName()).thenReturn("Web project");
    when(vsProject3.isWebProject()).thenReturn(true);
    solution = mock(VisualStudioSolution.class);
    when(solution.getProjects()).thenReturn(Lists.newArrayList(vsProject1, vsProject2, vsProject3));

    microsoftWindowsEnvironment = new MicrosoftWindowsEnvironment();
    microsoftWindowsEnvironment.setCurrentSolution(solution);

    configuration = new Settings(new PropertyDefinitions(new DotNetCorePlugin(), new NDepsPlugin()));

    nDepsResultParser = mock(NDepsResultParser.class);
    nDepsSensor = new NDepsSensor(fileSystem, microsoftWindowsEnvironment, new DotNetConfiguration(configuration), nDepsResultParser, mock(RulesProfile.class));

    reportFile = TestUtils.getResource("/ndeps-report.xml");
  }

  @Test
  public void shouldExecuteOnProject() throws Exception {
    Project project = new Project("");
    project.setLanguageKey(DotNetConstants.CSHARP_LANGUAGE_KEY);
    project.setParent(project);
    project.setName("Project #1");
    assertThat(nDepsSensor.shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void shouldExecuteOnProjectWithAdvancedConfiguration() throws Exception {
    Project project = new Project("");
    project.setLanguageKey(DotNetConstants.CSHARP_LANGUAGE_KEY);
    project.setParent(project);
    project.setName("Project #1");

    configuration.setProperty(DotNetConstants.ASSEMBLIES_TO_SCAN_KEY, "**/*.dll");
    assertThat(nDepsSensor.shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void shouldNotExecuteOnProjectWithAdvancedConfiguration() throws Exception {
    Project project = new Project("");
    project.setLanguageKey(DotNetConstants.CSHARP_LANGUAGE_KEY);
    project.setParent(project);
    project.setName("Project #1");

    configuration.setProperty(DotNetConstants.ASSEMBLIES_TO_SCAN_KEY, "**/*.dll,**/*.exe");
    assertThat(nDepsSensor.shouldExecuteOnProject(project), is(false));
  }

  @Test
  public void shouldNotExecuteOnWebProject() throws Exception {
    Project project = new Project("");
    project.setLanguageKey(DotNetConstants.CSHARP_LANGUAGE_KEY);
    project.setParent(project);
    project.setName("Web project");
    assertThat(nDepsSensor.shouldExecuteOnProject(project), is(false));
  }

  @Test
  public void shouldNotExecuteOnRootProject() throws Exception {
    Project project = new Project("");
    project.setLanguageKey(DotNetConstants.CSHARP_LANGUAGE_KEY);
    project.setParent(project);
    project.setName("Root project");
    assertThat(nDepsSensor.shouldExecuteOnProject(project), is(false));
  }

  @Test
  public void shouldNotAnalyseResultsForUnexistingFile() {
    nDepsSensor.analyseResults(mock(Project.class), new File("target/foo.txt"));
    verify(nDepsResultParser, never()).parse("compile", reportFile);
  }

  @Test
  public void shouldAnalyseResults() {
    nDepsSensor.analyseResults(mock(Project.class), reportFile);
    verify(nDepsResultParser).parse("compile", reportFile);
  }

  @Test
  public void shouldAnalyseResultsForTestProject() {
    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project Test");
    nDepsSensor.analyseResults(project, reportFile);
    verify(nDepsResultParser).parse("test", reportFile);
  }

  @Test
  public void shouldLaunchNDeps() throws Exception {
    configuration.setProperty(DotNetConstants.BUILD_CONFIGURATION_KEY, "Release");
    configuration.setProperty(NDepsConstants.TIMEOUT_MINUTES_KEY, 3);
    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project #1");

    NDepsCommandBuilder builder = NDepsCommandBuilder.createBuilder(solution, vsProject1);
    NDepsRunner runner = mock(NDepsRunner.class);
    when(runner.createCommandBuilder(solution, vsProject1)).thenReturn(builder);

    nDepsSensor.launchNDeps(project, runner);
    verify(runner, times(1)).createCommandBuilder(solution, vsProject1);
    verify(runner, times(1)).execute(builder, 3);
  }

}
