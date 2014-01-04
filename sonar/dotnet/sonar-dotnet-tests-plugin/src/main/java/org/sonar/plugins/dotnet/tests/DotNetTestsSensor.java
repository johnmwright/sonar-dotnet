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
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.plugins.dotnet.api.DotNetConfiguration;
import org.sonar.plugins.dotnet.api.DotNetConstants;
import org.sonar.plugins.dotnet.api.DotNetResourceBridges;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;
import org.sonar.plugins.dotnet.api.sensor.AbstractDotNetSensor;
import org.sonar.plugins.dotnet.api.sensor.AbstractRegularDotNetSensor;
import org.sonar.plugins.dotnet.api.utils.FileFinder;
import org.sonar.plugins.dotnet.tests.model.TestFileDetails;
import org.sonar.plugins.dotnet.tests.parser.DotNetTestResultParser;
import org.sonar.plugins.dotnet.tests.parser.nunit.NUnitTestResultParser;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@DependsUpon(DotNetConstants.CORE_PLUGIN_EXECUTED)
public class DotNetTestsSensor extends AbstractRegularDotNetSensor {

  private static final Logger LOG = LoggerFactory.getLogger(DotNetTestsSensor.class);

  private DotNetConfiguration configuration;
  private DotNetResourceBridges bridges;
  /**
   * Constructs a {@link org.sonar.plugins.dotnet.tests.DotNetTestsSensor}.
   *
   * @param configuration
   * @param microsoftWindowsEnvironment
   */
  public DotNetTestsSensor(DotNetConfiguration configuration, MicrosoftWindowsEnvironment microsoftWindowsEnvironment, DotNetResourceBridges bridges) {
    super(configuration, microsoftWindowsEnvironment, ".NET Tests", configuration.getString(DotNetTestsConstants.MODE_KEY));
    this.configuration = configuration;

      this.bridges = bridges;

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

        Collection<File> nunitTestReportFiles = findTestReportsToAnalyse(project, DotNetTestsConstants.NUNIT_REPORTS_KEY, DotNetTestsConstants.NUNIT_IT_REPORTS_KEY);
        if (nunitTestReportFiles.isEmpty()) {
            LOG.info("No NUnit .NET Tests report file found.");
        } else {
            parsers.add(new NUnitTestResultParser(bridges, project, vsProject, nunitTestReportFiles));
        }

        //TODO: add other parsers here


        return parsers;
    }

    /**
     * {@inheritDoc}
     */
    public void analyse(Project project, SensorContext context) {
        LOG.debug(".NET Tests plugin starting...");
        Collection<DotNetTestResultParser> parsers = getParsers(project);

        if (parsers.isEmpty()) {
            LOG.warn("No .NET Tests report file found.");
            context.saveMeasure(CoreMetrics.TESTS, 0.0);
            return;
        }

        collect(context, parsers);

    }


    protected Collection<File> findTestReportsToAnalyse(Project project, String parserReportFileKey, String parserItReportFileKey) {

        Collection<File> reports = Lists.newArrayList();

        VisualStudioSolution vsSolution = getVSSolution();
        VisualStudioProject vsProject = getVSProject(project);

        String reportPath = configuration.getString(parserReportFileKey);
        LOG.info("{} reportPath={}",parserReportFileKey,reportPath);
        reports.addAll(FileFinder.findFiles(vsSolution, vsProject, reportPath));

        String itExecutionMode = configuration.getString(DotNetTestsConstants.IT_MODE_KEY);
        if (!AbstractDotNetSensor.MODE_SKIP.equals(itExecutionMode)) {
            String itReportPath = configuration.getString(parserItReportFileKey);
            LOG.info("{} itReportPath={}",parserItReportFileKey,itReportPath);
            reports.addAll(FileFinder.findFiles(vsSolution, vsProject, itReportPath));
        }

        LOG.info("Reusing Test Reports: {}", Joiner.on("; ").join(reports));

        return reports;
    }

    private void collect(SensorContext context, Collection<DotNetTestResultParser> parsers) {
        Map<String, TestFileDetails> fileTestMap = Maps.newHashMap();

        for (DotNetTestResultParser parser: parsers) {
            Collection<TestFileDetails> tests = parser.parse();
            for (TestFileDetails test : tests) {
                collectTest(test, fileTestMap);
            }
        }
        LOG.debug("Found {} test data", fileTestMap.size());

        Set<String> filesAlreadyTreated = new HashSet<String>();

        for (TestFileDetails testReport : fileTestMap.values()) {
            saveFileMeasures(testReport, context, filesAlreadyTreated);
        }
    }


    protected void collectTest(TestFileDetails test, Map<String, TestFileDetails> fileTestMap) {
        org.sonar.api.resources.File file = test.getSourceFile();
        if (fileTestMap.containsKey(file.getKey())) {
            LOG.debug("merging details for {}", file.getKey());
            fileTestMap.get(file.getKey()).merge(test);
        } else {
            LOG.debug("collecting details for {}", file.getKey());
            fileTestMap.put(file.getKey(), test);
        }
    }


    protected void saveFileMeasures(TestFileDetails testReport, SensorContext context, Set<String> filesAlreadyTreated) {
        org.sonar.api.resources.File sourceFile = testReport.getSourceFile();
        if (sourceFile != null && !filesAlreadyTreated.contains(sourceFile.getKey())) {
            LOG.debug("Collecting test data for file {}", sourceFile);
            filesAlreadyTreated.add(sourceFile.getKey());
            int testsCount = testReport.getTests() - testReport.getSkipped();
            saveFileMeasure(sourceFile, context, CoreMetrics.SKIPPED_TESTS, testReport.getSkipped());
            saveFileMeasure(sourceFile, context, CoreMetrics.TESTS, testsCount);
            saveFileMeasure(sourceFile, context, CoreMetrics.TEST_ERRORS, testReport.getErrors());
            saveFileMeasure(sourceFile, context, CoreMetrics.TEST_FAILURES, testReport.getFailures());
            saveFileMeasure(sourceFile, context, CoreMetrics.TEST_EXECUTION_TIME, testReport.getTimeMS());
//                saveFileMeasure(sourceFile, context, TestMetrics.COUNT_ASSERTS, testReport.getAsserts());
            int passedTests = testsCount - testReport.getErrors() - testReport.getFailures();
            if (testsCount > 0) {
                double percentage = (float) passedTests * 100 / (float) testsCount;
                saveFileMeasure(sourceFile, context, CoreMetrics.TEST_SUCCESS_DENSITY, ParsingUtils.scaleValue(percentage));
            }
            saveTestsDetails(sourceFile, context, testReport);

        } else {
            if (sourceFile == null) {
                LOG.error("Source file not found for test report " + testReport);
            } else {
                LOG.error("Source file measures already saved for test report " + testReport);
            }
        }
    }

    private void saveFileMeasure(Resource testFile, SensorContext context, Metric metric, double value) {
        if (!Double.isNaN(value)) {
            LOG.debug("saving measure {} for file {} with value " + value, metric.getName(), testFile.getName());
            context.saveMeasure(testFile, metric, value);
        }
    }

    /**
     * Stores the test details in XML format.
     *
     * @param testFile
     * @param context
     * @param fileReport
     */
    private void saveTestsDetails(org.sonar.api.resources.File testFile, SensorContext context, TestFileDetails fileReport) {
        String testCaseDetails = fileReport.asXML();
        context.saveMeasure(testFile, new Measure(CoreMetrics.TEST_DATA, testCaseDetails));
        LOG.debug("test detail : {}", testCaseDetails);
    }

}
