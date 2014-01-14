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

import com.google.common.collect.Lists;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.File;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;

import java.util.Collection;

public class TestFileDetails {
    private final File sonarFile;
    private final Collection<TestCaseDetail> testCaseDetails = Lists.newArrayList();

    public TestFileDetails(File sonarFile) {
        this.sonarFile = sonarFile;
    }

    public TestCaseDetail addTestCase(String testName) {
        TestCaseDetail  testCase = new TestCaseDetail(testName);
        testCaseDetails.add(testCase);
        return testCase;
    }

    public Collection<TestCaseDetail> getTestCases(){
        return testCaseDetails;
    }

    public File getSonarFile() {
        return this.sonarFile;
    }

    public int getTestCount() {
        return testCaseDetails.size();
    }

    public int getSkippedCount() {
        return getCountByType(TestCase.Status.SKIPPED);
    }


    public int getErrorsCount() {
        return getCountByType(TestCase.Status.ERROR);
    }

    public int getFailureCount() {
        return getCountByType(TestCase.Status.FAILURE);
    }


    private int getCountByType(TestCase.Status status){
        int counter = 0;
        for(TestCaseDetail testcase : testCaseDetails){
            if (testcase.getStatus() == status){
                counter++;
            }
        }
        return counter;
    }

    public Long getTotalExecutionTimeInMS() {
        Long counter = Long.valueOf(0);
        for(TestCaseDetail testcase : testCaseDetails){
            counter += testcase.getDurationInMs();
        }
        return counter;
    }

    public void publishTestPlan(ResourcePerspectives resourcePerspectives) {

        MutableTestPlan testPlan = resourcePerspectives.as(MutableTestPlan.class, this.getSonarFile());

        for (TestCaseDetail testCaseDetail : this.getTestCases()){
            MutableTestCase testCase =  testPlan.addTestCase(testCaseDetail.getName());
            testCaseDetail.populateTestCase(testCase);
        }

    }
}
