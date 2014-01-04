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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Details for a test case.
 */
public class TestCaseDetail {

  private String name;
  private String fixtureName;
  private TestStatus status;
  private String stackTrace;
  private String errorMessage;
  private int timeMillis = 0;
  private org.sonar.api.resources.File sourceFile;

  /**
   * Constructs an empty @link{TestCaseDetail}.
   */
  public TestCaseDetail() {

  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public String getFormatedStackTrace() {
    return StringEscapeUtils.escapeXml(stackTrace);
  }

  public void setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getFormatedErrorMessage() {
    return StringEscapeUtils.escapeXml(StringUtils.remove(errorMessage, "\n\t"));
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public int getTimeMillis() {
    return timeMillis;
  }

  public void setTimeMillis(int timeMS) {
    this.timeMillis = timeMS;
  }

  @Override
  public String toString() {
    return "Test " + name + "(" + status + ", time=" + timeMillis * 0.001 + ")";
  }

  public String asXML() {
      StringBuilder testCaseDetails = new StringBuilder(256);

      testCaseDetails.append("<testcase status=\"").append(getStatus().getSonarStatus()).append("\"")
                     .append(" time=\"").append(getTimeMillis()).append("\"")
                     .append(" name=\"").append(getName()).append("\"");

      boolean isError = (getStatus() == TestStatus.ERROR);

      if (isError || (getStatus() == TestStatus.FAILED)) {

          testCaseDetails.append(">").append(isError ? "<error message=\"" : "<failure message=\"").append(getFormatedErrorMessage())
                  .append("\">").append("<![CDATA[").append(getFormatedStackTrace()).append("]]>")
                  .append(isError ? "</error>" : "</failure>").append("</testcase>");

      } else {
          testCaseDetails.append("/>");
      }

      return testCaseDetails.toString();
  }

  /**
   * Returns the status.
   * 
   * @return The status to return.
   */
  public TestStatus getStatus() {
    return this.status;
  }

  /**
   * Sets the status.
   * 
   * @param status
   *          The status to set.
   */
  public void setStatus(TestStatus status) {
    this.status = status;
  }

  /**
   * Returns the testFile.
   * 
   * @return The testFile to return.
   */
  public org.sonar.api.resources.File getSourceFile() {
    return this.sourceFile;
  }

  /**
   * Sets the testFile.
   * 
   * @param testFile
   *          The testFile to set.
   */
  public void setSourceFile(org.sonar.api.resources.File testFile) {
    this.sourceFile = testFile;
  }


    public String getFixtureName() {
        return fixtureName;
    }

    public void setFixtureName(String fixtureName) {
        this.fixtureName = fixtureName;
    }


}
