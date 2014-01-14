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

import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.TestCase;

public class TestCaseDetail {
    private final String testName;
    private String type;
    private TestCase.Status status;
    private Long durationInMs;
    private String message;
    private String stackTrace;

    public TestCaseDetail(String testName) {

        this.testName = testName;
    }

    public String getName() {
        return this.testName;
    }

    public void populateTestCase(MutableTestCase testCase) {
        testCase.setType(this.type);
        testCase.setStatus(this.status);
        testCase.setDurationInMs(this.durationInMs);

        if (this.stackTrace != null) {
            testCase.setStackTrace(this.stackTrace);
        }

        if (this.message != null) {
            testCase.setMessage(this.message);
        }

    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setStatus(TestCase.Status status) {
        this.status = status;
    }

    public TestCase.Status getStatus() {
        return status;
    }

    public void setDurationInMs(Long durationInMs) {
        this.durationInMs = durationInMs;
    }

    public Long getDurationInMs() {
        return durationInMs;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
