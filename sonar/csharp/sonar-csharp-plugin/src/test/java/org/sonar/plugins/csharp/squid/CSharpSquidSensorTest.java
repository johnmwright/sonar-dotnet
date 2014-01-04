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
package org.sonar.plugins.csharp.squid;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.batch.ResourceCreationLock;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.*;
import org.sonar.plugins.csharp.api.CSharp;
import org.sonar.plugins.csharp.api.CSharpConstants;
import org.sonar.plugins.csharp.core.CSharpCorePlugin;
import org.sonar.plugins.dotnet.api.DotNetConfiguration;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;

import java.io.File;
import java.nio.charset.Charset;

import static org.mockito.Mockito.*;

public class CSharpSquidSensorTest {

  private CSharpSquidSensor sensor;
  private Settings settings;

  @Before
  public void init() {
    settings = new Settings(new PropertyDefinitions(CSharpCorePlugin.class));
    DotNetConfiguration dotNetConfiguration = new DotNetConfiguration(settings);
    CSharp language = new CSharp(dotNetConfiguration);
    CSharpResourcesBridge cSharpResourcesBridge = mock(CSharpResourcesBridge.class);
    ResourceCreationLock resourceCreationLock = mock(ResourceCreationLock.class);
    MicrosoftWindowsEnvironment microsoftWindowsEnvironment = mock(MicrosoftWindowsEnvironment.class);
    RulesProfile profile = mock(RulesProfile.class);
    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    FileLinesContext flc = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(Matchers.any(Resource.class))).thenReturn(flc);
    sensor = new CSharpSquidSensor(dotNetConfiguration, language, cSharpResourcesBridge, resourceCreationLock,
        microsoftWindowsEnvironment, profile, noSonarFilter, fileLinesContextFactory);
  }

  @Test
  public void analyse() {
    settings.setProperty(CSharpSquidConstants.SSLR_INCLUDE_TEST_SOURCES_KEY, false);
    ProjectFileSystem projectFileSystem = mock(ProjectFileSystem.class);
    when(projectFileSystem.getSourceCharset()).thenReturn(Charset.forName("UTF-8"));
    InputFile inputFile = InputFileUtils.create(
        new File("src/test/resources/src/main/"),
        new File("src/test/resources/src/main/CSharpSquidSensor.cs"));
    when(projectFileSystem.mainFiles(CSharpConstants.LANGUAGE_KEY)).thenReturn(ImmutableList.of(inputFile));
    when(projectFileSystem.getSourceDirs()).thenReturn(ImmutableList.of(new File("src/test/resources/src/main/")));

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(projectFileSystem);

    SensorContext context = mock(SensorContext.class);

    sensor.analyse(project, context);

    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.FILES), Mockito.eq(1.0));
    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.CLASSES), Mockito.eq(3.0));
    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.FUNCTIONS), Mockito.eq(31.0));
    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.LINES), Mockito.eq(363.0));
    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.NCLOC), Mockito.eq(278.0));
    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.STATEMENTS), Mockito.eq(144.0));
    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.ACCESSORS), Mockito.eq(10.0));
    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.COMPLEXITY), Mockito.eq(72.0));
    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.COMMENT_BLANK_LINES), Mockito.eq(0.0));
    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.COMMENTED_OUT_CODE_LINES), Mockito.eq(0.0));
    verify(context).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.COMMENT_LINES), Mockito.eq(33.0));
  }

    @Test
    public void analyseWithTestSources() {
        settings.setProperty(CSharpSquidConstants.SSLR_INCLUDE_TEST_SOURCES_KEY, true);
        ProjectFileSystem projectFileSystem = mock(ProjectFileSystem.class);
        when(projectFileSystem.getSourceCharset()).thenReturn(Charset.forName("UTF-8"));
        InputFile mainInputFile = InputFileUtils.create(
                new File("src/test/resources/src/main/"),
                new File("src/test/resources/src/main/CSharpSquidSensor.cs"));

        InputFile testInputFile = InputFileUtils.create(
                new File("src/test/resources/src/test/"),
                new File("src/test/resources/src/test/CSharpSquidSensorTestClass.cs"));

        when(projectFileSystem.mainFiles(CSharpConstants.LANGUAGE_KEY)).thenReturn(ImmutableList.of(mainInputFile));
        when(projectFileSystem.testFiles(CSharpConstants.LANGUAGE_KEY)).thenReturn(ImmutableList.of(testInputFile));
        when(projectFileSystem.getSourceDirs()).thenReturn(ImmutableList.of(new File("src/test/resources/src/main/")));
        when(projectFileSystem.getTestDirs()).thenReturn(ImmutableList.of(new File("src/test/resources/src/test/")));

        Project project = mock(Project.class);
        when(project.getFileSystem()).thenReturn(projectFileSystem);

        SensorContext context = mock(SensorContext.class);

        sensor.analyse(project, context);

        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.FILES), Mockito.eq(1.0));
        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.CLASSES), Mockito.eq(3.0));
        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.FUNCTIONS), Mockito.eq(31.0));
        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.LINES), Mockito.eq(363.0));
        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.NCLOC), Mockito.eq(278.0));
        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.STATEMENTS), Mockito.eq(144.0));
        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.ACCESSORS), Mockito.eq(10.0));
        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.COMPLEXITY), Mockito.eq(72.0));
        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.COMMENT_BLANK_LINES), Mockito.eq(0.0));
        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.COMMENTED_OUT_CODE_LINES), Mockito.eq(0.0));
        verify(context, times(2)).saveMeasure(Mockito.any(Resource.class), Mockito.eq(CoreMetrics.COMMENT_LINES), Mockito.eq(33.0));

    }

}
