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

package org.sonar.plugins.dotnet.tests.model;

import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * A report for a unit test file.
 */
public class TestFileDetails {

  private Project project;
  private final org.sonar.api.resources.File sourceFile;
  private int errors = 0;
  private int skipped = 0;
  private int timeMS = 0;
  private int failures = 0;
  private List<TestCaseDetail> details;

  public void merge(TestFileDetails report) {
    for (TestCaseDetail detail : report.details) {
      addDetail(detail);
    }
  }

  public TestFileDetails(File sourceFile) {
    details = new ArrayList<TestCaseDetail>();
      this.sourceFile = sourceFile;
  }

  public int getErrors() {
    return errors;
  }

  public int getSkipped() {
    return skipped;
  }

  public int getTests() {
    return details.size();
  }

  public int getTimeMS() {
    return timeMS;
  }

  public int getFailures() {
    return failures;
  }

  public List<TestCaseDetail> getDetails() {
    return details;
  }

  public void addDetail(TestCaseDetail detail) {
    this.details.add(detail);
    TestStatus status = detail.getStatus();
    switch (status) {
      case FAILED:
        failures++;
        break;
      case ERROR:
        errors++;
        break;
      case SKIPPED:
        skipped++;
        break;
      case SUCCESS:
        break;
      default:
        // do nothing
    }

      //NOTE: There's some losiness using this method, as the test fixtures themselves
      //may have some overhead that's not accounted for in the test cases but is provided
      //at a higher reported level, but since we can't gaurantee the file-to-type mapping
      //above the test case level due to partial classes, this has to do.
      timeMS += detail.getTimeMillis();
  }

  /**
   * Returns the Project.
   */
  public Project getProject() {
    return this.project;
  }

  /**
   * Sets the Project.
   *
   */
  public void setProject(Project project) {
    this.project = project;
  }

  /**
   * Returns the sourceFile.
   * 
   * @return The sourceFile to return.
   */
  public org.sonar.api.resources.File getSourceFile() {
    return this.sourceFile;
  }

  @Override
  public String toString() {
    return "Project=" + project + ", file:" + sourceFile + "(time=" + timeMS / 1000. + "s, tests=" + getTests() + ", failures=" + failures
      + ", ignored=" + skipped + ")";
  }

    public String asXML() {
        StringBuilder testCaseDetails = new StringBuilder(256);

        testCaseDetails.append("<tests-details>");
        for (TestCaseDetail detail : getDetails()) {
            testCaseDetails.append(detail.asXML());
        }
        testCaseDetails.append("</tests-details>");

        return testCaseDetails.toString();
    }
}
