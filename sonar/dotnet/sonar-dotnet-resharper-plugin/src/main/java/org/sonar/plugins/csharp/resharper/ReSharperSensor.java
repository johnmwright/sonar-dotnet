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

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.csharp.resharper.profiles.ReSharperProfileExporter;
import org.sonar.plugins.dotnet.api.DotNetConfiguration;
import org.sonar.plugins.dotnet.api.DotNetConstants;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;
import org.sonar.plugins.dotnet.api.sensor.AbstractRuleBasedDotNetSensor;
import org.sonar.plugins.dotnet.api.utils.FileFinder;

import java.io.File;
import java.util.Collection;

/**
* Collects the ReSharper reporting into sonar.
*/
public abstract class ReSharperSensor extends AbstractRuleBasedDotNetSensor {

  private static final Logger LOG = LoggerFactory.getLogger(ReSharperSensor.class);

  private ProjectFileSystem fileSystem;
  private RulesProfile rulesProfile;
  private ReSharperResultParser resharperResultParser;

  @DependsUpon(DotNetConstants.CORE_PLUGIN_EXECUTED)
  public static class CSharpRegularReSharperSensor extends ReSharperSensor {
    public CSharpRegularReSharperSensor(ProjectFileSystem fileSystem, RulesProfile rulesProfile, ReSharperProfileExporter.CSharpRegularReSharperProfileExporter profileExporter,
        ReSharperResultParser resharperResultParser, DotNetConfiguration configuration, MicrosoftWindowsEnvironment microsoftWindowsEnvironment) {
      super(fileSystem, rulesProfile, profileExporter, resharperResultParser, configuration, microsoftWindowsEnvironment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getSupportedLanguages() {
      return new String[] {"cs"};
    }
  }

  @DependsUpon(DotNetConstants.CORE_PLUGIN_EXECUTED)
  public static class VbNetRegularReSharperSensor extends ReSharperSensor {
    public VbNetRegularReSharperSensor(ProjectFileSystem fileSystem, RulesProfile rulesProfile, ReSharperProfileExporter.VbNetRegularReSharperProfileExporter profileExporter,
        ReSharperResultParser resharperResultParser, DotNetConfiguration configuration, MicrosoftWindowsEnvironment microsoftWindowsEnvironment) {
      super(fileSystem, rulesProfile, profileExporter, resharperResultParser, configuration, microsoftWindowsEnvironment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getSupportedLanguages() {
      return new String[] {"vbnet"};
    }
  }


  /**
   * Constructs a {@link org.sonar.plugins.csharp.resharper.ReSharperSensor}.
   *
   */
  protected ReSharperSensor(ProjectFileSystem fileSystem, RulesProfile rulesProfile,   ReSharperProfileExporter profileExporter,
      ReSharperResultParser resharperResultParser, DotNetConfiguration configuration, MicrosoftWindowsEnvironment microsoftWindowsEnvironment) {
    super(configuration, rulesProfile, profileExporter, microsoftWindowsEnvironment, "ReSharper", configuration.getString(ReSharperConstants.MODE));
    this.fileSystem = fileSystem;
    this.rulesProfile = rulesProfile;

    this.resharperResultParser = resharperResultParser;

  }

  /**
   * {@inheritDoc}
   */
  public void analyse(Project project, SensorContext context) {

    final Collection<File> reportFiles;
    String reportDefaultPath = getMicrosoftWindowsEnvironment().getWorkingDirectory() + "/" + ReSharperConstants.RESHARPER_REPORT_XML;
    if (MODE_REUSE_REPORT.equalsIgnoreCase(getExecutionMode())) {
      String reportPath = configuration.getString(ReSharperConstants.REPORTS_PATH_KEY);
      if (StringUtils.isEmpty(reportPath)) {
        reportPath = reportDefaultPath;
      }
      VisualStudioSolution vsSolution = getVSSolution();
      VisualStudioProject vsProject = getVSProject(project);
      reportFiles = FileFinder.findFiles(vsSolution, vsProject, reportPath);

        if (reportFiles.isEmpty()){
            new SonarException("No ReSharper reports found. Make sure to set " + ReSharperConstants.REPORTS_PATH_KEY);
        }

      LOG.info("Reusing ReSharper reports: " + Joiner.on(" ").join(reportFiles));
    } else {
        throw new SonarException("ReSharper direct execution not yet supported.");
//      // prepare config file for FxCop
//      File fxCopConfigFile = generateConfigurationFile();
//      // and run FxCop
//      try {
//        FxCopRunner runner = FxCopRunner.create(configuration.getString(FxCopConstants.INSTALL_DIR_KEY));
//        launchFxCop(project, runner, fxCopConfigFile);
//      } catch (FxCopException e) {
//        throw new SonarException("FxCop execution failed.", e);
//      }
//      File projectDir = fileSystem.getBasedir();
//      reportFiles = Collections.singleton(new File(projectDir, reportDefaultPath));
    }

    // and analyze results
    for (File reportFile : reportFiles) {
      analyseResults(reportFile);
    }
  }

  private void analyseResults(File reportFile) {
    if (reportFile.exists()) {
      LOG.debug("ReSharper report found at location {}", reportFile);
      resharperResultParser.parse(reportFile);
    } else {
      LOG.warn("No ReSharper report found for path {}", reportFile);
    }
  }

}
