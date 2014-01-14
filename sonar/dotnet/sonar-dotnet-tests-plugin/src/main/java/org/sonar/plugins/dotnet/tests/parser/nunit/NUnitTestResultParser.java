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
package org.sonar.plugins.dotnet.tests.parser.nunit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.TestCase;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.dotnet.api.DotNetResourceBridge;
import org.sonar.plugins.dotnet.api.DotNetResourceBridges;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.tests.DotNetTestsConstants;
import org.sonar.plugins.dotnet.tests.model.TestFileDetails;
import org.sonar.plugins.dotnet.tests.parser.DotNetTestResultParser;
import org.sonar.plugins.dotnet.tests.parser.StaxHelper;
import org.sonar.plugins.dotnet.tests.model.TestCaseDetail;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.util.*;

import static org.sonar.plugins.dotnet.tests.parser.StaxHelper.advanceCursor;
import static org.sonar.plugins.dotnet.tests.parser.StaxHelper.findElementName;

public class NUnitTestResultParser extends DotNetTestResultParser {


    public String getUnitTestReportsKey(){return  DotNetTestsConstants.NUNIT_REPORTS_KEY;}
    public String getIntegrationTestReportsKey(){ return  DotNetTestsConstants.NUNIT_IT_REPORTS_KEY;}

    private static final Logger LOG = LoggerFactory.getLogger(NUnitTestResultParser.class);

    private final DotNetResourceBridge resourceBridge;
    private final VisualStudioProject vsProject;

    public NUnitTestResultParser(DotNetResourceBridges dotNetResourceBridges, Project project, VisualStudioProject vsProject){
        this.resourceBridge = dotNetResourceBridges.getBridge(project.getLanguageKey());
        this.vsProject = vsProject;
    }


    private class ParseContext {

        private final String reportType;

        private Map<org.sonar.api.resources.File, TestFileDetails> fixtureMap = Maps.newHashMap();

        private Stack<String> namespaces = new Stack<String>();
        private Stack<String> fixtureNames = new Stack<String>();

        public ParseContext(String reportType) {
            this.reportType = reportType;
        }

        public String getReportType() {
            return this.reportType;
        }

        public void popNamespaceOffStack() {
            namespaces.pop();
            LOG.debug("Namespace popped: {}", getCurrentNamespace());
        }

        public void pushNamespaceToStack(String namespaceName) {
            namespaces.push(namespaceName);
            LOG.debug("Namespace pushed: {}", getCurrentNamespace());
        }

        public String getCurrentNamespace() {
            return StringUtils.join(Collections.list(namespaces.elements()), ".");
        }


        public void enterFixture(String name) {
            this.fixtureNames.push(getCurrentNamespace() + "." + name);
        }

        public void leaveFixture(){
            this.fixtureNames.pop();
        }

        public String getCurrentFixtureName() {
            return fixtureNames.peek();
        }

        public Collection<TestFileDetails> getFileDetails()  {
            return fixtureMap.values();
        }

        public TestFileDetails getFixtureDetails(org.sonar.api.resources.File sonarFile) {
            if (!fixtureMap.containsKey(sonarFile))
            {
                fixtureMap.put(sonarFile, new TestFileDetails(sonarFile));
            }
            return fixtureMap.get(sonarFile);
        }
    }

    @Override
    public Set<TestFileDetails> parse(Collection<File> reportFiles, String reportType) {

        LOG.debug("running parse in NUnit parser");
        Set<TestFileDetails> results = new HashSet<TestFileDetails>();

        for (File report : reportFiles ) {

            LOG.debug("Parsing NUnit report {}", report.getName());

            ParseContext fileContext = new ParseContext(reportType);

            try {

                SMInputFactory inf = new SMInputFactory(XMLInputFactory.newInstance());
                SMHierarchicCursor rootCursor = inf.rootElementCursor(report);
                advanceCursor(rootCursor);
                LOG.debug("rootCursor is at : {}", findElementName(rootCursor));

                SMInputCursor mainCursor = rootCursor.childElementCursor();

                while (mainCursor.getNext() != null) {

                    String nodeName =mainCursor.getQName().getLocalPart();

                    LOG.debug("cursor is at node {}", nodeName);
                    if (nodeName.equals("test-suite")) {
                            parseTestSuiteNode(mainCursor, fileContext);
                    } else {
                        LOG.debug("Found non-test-suite top-level node: " + nodeName);
                    }
                }

                results.addAll(fileContext.getFileDetails());
            } catch (XMLStreamException e) {
                throw new SonarException("Error parsing NUnit report", e);
            }
        }
        return results;
    }


    private void parseTestSuiteNode(SMInputCursor mainCursor, ParseContext fileContext) throws XMLStreamException {

        String nodeName = mainCursor.getQName().getLocalPart();

        if (!nodeName.equals("test-suite")) {
            throw new SonarException("Incoming node was not a test-suite");
        }

        String type = mainCursor.getAttrValue("type");
        LOG.debug("parsing test-suite node with type {}", type);

        if (type.equals("TestFixture")) {
            parseTestFixtureNode(mainCursor, fileContext);
        } else if (type.equals("Assembly") && !isAssemblyNodeAMatchForCurrentProject(mainCursor)) {
              //do nothing for this node

        } else if (type.equals("ParameterizedTest")){

            LOG.debug("found ParameterizedTests");
            parseResultsNode(mainCursor, fileContext);

        } else {

            String currentNamespace = fileContext.getCurrentNamespace();

            if (type.equals("Namespace")) {
                String namespaceName = mainCursor.getAttrValue("name");
                fileContext.pushNamespaceToStack(namespaceName);
                LOG.debug("Using namespace name {}", fileContext.getCurrentNamespace());
            }

            parseResultsNode(mainCursor, fileContext);

            if (type.equals("Namespace"))     {
                fileContext.popNamespaceOffStack();
            }

            if (!currentNamespace.equals(fileContext.getCurrentNamespace())) {
                throw new SonarException("Namespace collection corrupted. Expected " + currentNamespace + " but found " + fileContext.getCurrentNamespace());
            }
        }

    }

    private void parseResultsNode(SMInputCursor mainCursor, ParseContext fileContext) throws XMLStreamException {
        //<results> can have a collection of "test-suite" and/or "test-case" nodes

        SMInputCursor resultsCursor = mainCursor.childElementCursor();

        while (resultsCursor.getNext() != null) {

            String childNodeName =resultsCursor.getQName().getLocalPart();

            if (childNodeName.equals("results")){

                SMInputCursor resultsValuesCursor = resultsCursor.childElementCursor();

                while (resultsValuesCursor.getNext() != null) {

                    String resultsChildNodeName =resultsValuesCursor.getQName().getLocalPart();

                    if (resultsChildNodeName.equals("test-suite")) {

                        parseTestSuiteNode(resultsValuesCursor, fileContext);

                    } else if (resultsChildNodeName.equals("test-case"))  {
                        parseTestCase(resultsValuesCursor, fileContext);

                    } else {
                        LOG.debug("found unsupported node {}", resultsChildNodeName);
                    }
                }
            }

        }
    }

    private void parseTestFixtureNode(SMInputCursor mainCursor, ParseContext fileContext) throws XMLStreamException {

        String fixtureName = mainCursor.getAttrValue("name").replaceAll("\\+", "\\.");
        fileContext.enterFixture(fixtureName);
        parseResultsNode(mainCursor, fileContext);
        fileContext.leaveFixture();
    }

    private void parseTestCase(SMInputCursor resultsCursor, ParseContext fileContext) throws XMLStreamException {

        String wasExecuted = resultsCursor.getAttrValue("executed");
        String status = resultsCursor.getAttrValue("result");
        String time = resultsCursor.getAttrValue("time");  //optional
        String testName = resultsCursor.getAttrValue("name");

        if (!wasExecuted.equals("True") && status == null){
            LOG.debug("TestCase {} was not executed", testName);
            return;// null;
        }

        org.sonar.api.resources.File sonarFile = getSonarFileForTestCase(testName, fileContext.getCurrentFixtureName() );
        if (sonarFile == null) {
            LOG.warn("unable to find source file for {} in {}", testName, fileContext.getCurrentFixtureName());
            return;// null;
        }

        TestFileDetails testFileDetails = fileContext.getFixtureDetails(sonarFile);

        TestCaseDetail testDetail = testFileDetails.addTestCase(testName);

        if (testDetail == null) {
            LOG.error("unable to create MutableTestCase for {}", testName);
            return;// null;
        }

        testDetail.setType(fileContext.getReportType());
        testDetail.setStatus( convertNunitStatusToTestStatus(status));
        if (time != null) {
            testDetail.setDurationInMs(Long.valueOf(Math.round(Float.parseFloat(time) * 1000)));
        } else {
            testDetail.setDurationInMs(Long.valueOf(0));
        }

        SMInputCursor childCursor = resultsCursor.childElementCursor();

        while (childCursor.getNext() != null) {

            String childNodeName =childCursor.getQName().getLocalPart();

            if (childNodeName.equals("failure") || childNodeName.equals("reason")){

                SMInputCursor failureDetailsCursor = childCursor.childElementCursor();


                while (failureDetailsCursor.getNext() != null){

                    String subchildNodeName =failureDetailsCursor.getQName().getLocalPart();

                    if (subchildNodeName.equals("message")) {
                        LOG.debug("found test-case failure message");

                        String message = failureDetailsCursor.collectDescendantText();
                        testDetail.setMessage(message);

                    } else if (subchildNodeName.equals("stack-trace")) {
                        LOG.debug("found test-case failure stack-trace");

                        String stackTrace = failureDetailsCursor.collectDescendantText();
                        testDetail.setStackTrace(stackTrace);

                    }
                }
            }
        }
//        return testDetail;
    }

    private org.sonar.api.resources.File getSonarFileForTestCase(String testCase, String fixtureName) throws XMLStreamException {
        LOG.debug("Getting Sonar File for TestCase {}", testCase);
        String testCaseName = testCase.replaceAll("\\+", "\\.");

        if (!testCaseName.endsWith(")")){
            testCaseName += "()";
        }

        String fullyQualifiedTypeName = convertParameterizedTestToMethodSignature(testCaseName, fixtureName);

        LOG.debug("checking for Resource for TestCase {}", fullyQualifiedTypeName);

        Resource<?> typeResource = resourceBridge.getFromMemberName(fullyQualifiedTypeName);
        if (typeResource == null){
            LOG.error("could not find Resource for method {}", fullyQualifiedTypeName);

            //In the case where the test method itself is in a base class, it will append the
            //base class name to the end of the test fixture class, like this: Namespace.TestClass.BaseClass#Method();
            //In these cases, though, the test will likely exist for multiple classes, so in that cases, we want to
            //associate them with the fixture class, not the base class, so that we don't lose the context


            // if can't find method, just associate to Type of test fixture. Will be the same, except for partial classes
            typeResource = resourceBridge.getFromTypeName(fixtureName);

            if (typeResource == null){
                LOG.error("could not find Resource for fixture type {}", fixtureName);
            }


            return null;
//              throw new SonarException(String.format("could not find Resource for fixture type %s", fixtureName));

        }

        if (!(typeResource instanceof org.sonar.api.resources.File)){
            throw new SonarException(String.format("could not convert type %s to File", fullyQualifiedTypeName));
        }

        org.sonar.api.resources.File sonarFile = (org.sonar.api.resources.File)typeResource;

        LOG.debug("source file for {} is {}", fullyQualifiedTypeName, sonarFile.getLongName());

        return sonarFile;
    }

    private String convertParameterizedTestToMethodSignature(String fullyQualifiedTypeName, String fixtureName) {

        String methodWithoutParams = StringUtils.substringBefore(fullyQualifiedTypeName, "(");    //this could incorrectly include params with casting??
      //  String className = StringUtils.substringBeforeLast(methodWithoutParams, ".");
        String methodName = StringUtils.substringAfterLast(methodWithoutParams, ".");

        String allParams = StringUtils.substringBetween(fullyQualifiedTypeName, "(", ")"); //this could incorrectly include params inside a string param
        String[] params = StringUtils.split(allParams, ",");  //this could incorrectly include commas inside a string param

        Collection<String> typedParams = Lists.newArrayList();

        for (String param : params) {
            String type = guessTypeFromParamValue(param.trim());
            typedParams.add(type);
        }

         String paramTypeSignature = "(" + StringUtils.join(typedParams, ", ") + ")";


        return fixtureName + "#" + methodName + paramTypeSignature;
    }

    private String guessTypeFromParamValue(String param) {

        //TODO: determine type of each param from input values
        //Note: nunit requires they be compile-time constants, so only need to handle primative types
        //
        // foreach, convert to type:
        //    if in quotes -> string              //X
        //    if "True" or "False", bool          //X
        //    if number and has ".", double      //X
        //    if number and does not have ".", int   //X
        //    if number + "m" or "d" (other shorthands)...


        //TODO: int vs Int32, bool vs Boolean, String vs string, etc

        if (param.startsWith("\"")){
            return "string";
        }

        if (param.equals("True") || param.equals("False")){
            return "bool";
        }

        if (StringUtils.isNumeric(param)){
            return "int";
        }

        if (StringUtils.isNumeric(StringUtils.remove(param, '.')))
        {
            return "double";
        }

        return "object";
    }

    private boolean isAssemblyNodeAMatchForCurrentProject(SMInputCursor nodeCursor){

        if (!"Assembly".equals(StaxHelper.findAttributeValue(nodeCursor, "type"))) {
            throw new SonarException("Input node is not Assembly type");
        }

        String assemblyName =StaxHelper.findAttributeValue(nodeCursor,"name");
        LOG.debug("Using assembly name {}", assemblyName);

        File assemblyFile = new File(assemblyName);
        String assemblyFileName = assemblyFile.getName();

        String projectAssembly = vsProject.getArtifactName();
        boolean isAMatch = projectAssembly.equals(assemblyFileName);

        if (!isAMatch) {
            LOG.debug("Assembly {} is not a match for the current project's artifact ({})", assemblyFileName, projectAssembly);
        }

        return isAMatch;
    }

    private TestCase.Status convertNunitStatusToTestStatus(String nunitStatus){

        String upperedStatus = nunitStatus.toUpperCase();

        if (upperedStatus.equals("SUCCESS")){
            return TestCase.Status.OK;
        } else if (upperedStatus.equals("IGNORED") || upperedStatus.equals("INCONCLUSIVE")){
            return TestCase.Status.SKIPPED;
        } else {
            return TestCase.Status.valueOf(upperedStatus);
        }


    }
}