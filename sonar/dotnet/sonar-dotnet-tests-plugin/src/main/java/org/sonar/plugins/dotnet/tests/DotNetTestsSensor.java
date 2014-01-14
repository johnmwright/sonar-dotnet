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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.TestCase;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.plugins.dotnet.api.DotNetConfiguration;
import org.sonar.plugins.dotnet.api.DotNetConstants;
import org.sonar.plugins.dotnet.api.DotNetResourceBridges;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;
import org.sonar.plugins.dotnet.api.sensor.AbstractRegularDotNetSensor;
import org.sonar.plugins.dotnet.api.utils.FileFinder;
import org.sonar.plugins.dotnet.tests.model.TestFileDetails;
import org.sonar.plugins.dotnet.tests.parser.DotNetTestResultParser;
import org.sonar.plugins.dotnet.tests.parser.nunit.NUnitTestResultParser;

import java.io.File;
import java.util.Collection;

@DependsUpon(DotNetConstants.CORE_PLUGIN_EXECUTED)
public class DotNetTestsSensor extends AbstractRegularDotNetSensor {

  private static final Logger LOG = LoggerFactory.getLogger(DotNetTestsSensor.class);
  private final ResourcePerspectives resourcePerspectives;

  private DotNetConfiguration configuration;
  private DotNetResourceBridges bridges;
  /**
   * Constructs a {@link org.sonar.plugins.dotnet.tests.DotNetTestsSensor}.
   *
   * @param configuration
   * @param microsoftWindowsEnvironment
   */
  public DotNetTestsSensor(DotNetConfiguration configuration, MicrosoftWindowsEnvironment microsoftWindowsEnvironment, DotNetResourceBridges bridges, ResourcePerspectives resourcePerspectives) {
    super(configuration, microsoftWindowsEnvironment, ".NET Tests", configuration.getString(DotNetTestsConstants.MODE_KEY));
    this.configuration = configuration;

    this.bridges = bridges;
    this.resourcePerspectives = resourcePerspectives;
  }


    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getSupportedLanguages() {
        return DotNetConstants.DOTNET_LANGUAGE_KEYS;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isTestSensor() {
        return true;
    }

    private Collection<DotNetTestResultParser> getParsers(Project project) {

        Collection<DotNetTestResultParser> parsers = Lists.newArrayList();

        VisualStudioProject vsProject = getVSProject(project);

        parsers.add(new NUnitTestResultParser(bridges, project, vsProject));

        //TODO: add other parsers here


        return parsers;
    }

    /**
     * {@inheritDoc}
     */
    public void analyse(Project project, SensorContext context) {
        LOG.debug(".NET Tests plugin starting...");
        Collection<DotNetTestResultParser> parsers = getParsers(project);

        collect(context, parsers, project);
    }


    protected Collection<File> findTestReportsToAnalyse(Project project, String parserReportFileKey) {

        VisualStudioSolution vsSolution = getVSSolution();
        VisualStudioProject vsProject = getVSProject(project);

        String reportPath = configuration.getString(parserReportFileKey);
        LOG.debug("{} reportPath={}", parserReportFileKey, reportPath);
        Collection<File> reports = FileFinder.findFiles(vsSolution, vsProject, reportPath);

        LOG.info("Reusing Test Reports ({}): {}", parserReportFileKey, Joiner.on("; ").join(reports));

        return reports;
    }

    private void collect(SensorContext context, Collection<DotNetTestResultParser> parsers, Project project) {

      Collection<TestFileDetails> tests = Lists.newArrayList();
      for (DotNetTestResultParser parser: parsers) {

        Collection<File> reports = findTestReportsToAnalyse(project, parser.getUnitTestReportsKey());
        tests.addAll(parser.parse(reports, TestCase.TYPE_UNIT));

        Collection<File> itReports = findTestReportsToAnalyse(project, parser.getIntegrationTestReportsKey());
        tests.addAll(parser.parse(itReports, TestCase.TYPE_INTEGRATION));

      }


      if (tests.isEmpty()) {
        LOG.warn("No .NET Tests report file found.");
        context.saveMeasure(CoreMetrics.TESTS, 0.0);

      } else {

        for (TestFileDetails testFixtureFile : tests) {
          saveFileMeasures(testFixtureFile, context);
          testFixtureFile.publishTestPlan(resourcePerspectives);
        }

      }
    }


    protected void saveFileMeasures(TestFileDetails testReport, SensorContext context) {

            LOG.debug("Collecting test data for file {}", testReport);
            org.sonar.api.resources.File sourceFile = testReport.getSonarFile();

            int testsCount = testReport.getTestCount() - testReport.getSkippedCount();
            saveFileMeasure(sourceFile, context, CoreMetrics.SKIPPED_TESTS, testReport.getSkippedCount());
            saveFileMeasure(sourceFile, context, CoreMetrics.TESTS, testsCount);
            saveFileMeasure(sourceFile, context, CoreMetrics.TEST_ERRORS, testReport.getErrorsCount());
            saveFileMeasure(sourceFile, context, CoreMetrics.TEST_FAILURES, testReport.getFailureCount());
            saveFileMeasure(sourceFile, context, CoreMetrics.TEST_EXECUTION_TIME, testReport.getTotalExecutionTimeInMS());
//                saveFileMeasure(sourceFile, context, TestMetrics.COUNT_ASSERTS, testReport.getAsserts());
            int passedTests = testsCount - testReport.getErrorsCount() - testReport.getFailureCount();
            if (testsCount > 0) {
                double percentage = (float) passedTests * 100 / (float) testsCount;
                saveFileMeasure(sourceFile, context, CoreMetrics.TEST_SUCCESS_DENSITY, ParsingUtils.scaleValue(percentage));
            }

    }

    private void saveFileMeasure(Resource testFile, SensorContext context, Metric metric, double value) {
        if (!Double.isNaN(value)) {
            LOG.debug("saving measure {} for file {} with value " + value, metric.getName(), testFile.getName());
            context.saveMeasure(testFile, metric, value);
        }
    }
}
