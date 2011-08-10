/*
 * Copyright (C) 2009-2011 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android.standalonemojos;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.jayway.maven.plugins.android.AbstractIntegrationtestMojo;
import com.jayway.maven.plugins.android.DeviceCallback;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


import static com.android.ddmlib.testrunner.ITestRunListener.TestFailure.ERROR;

/**
 * @author hugo.josefson@jayway.com
 */
public abstract class AbstractInstrumentationMojo extends AbstractIntegrationtestMojo {
    /**
     * Package name of the apk we wish to instrument. If not specified, it is inferred from
     * <code>AndroidManifest.xml</code>.
     *
     * @optional
     * @parameter expression="${android.test.instrumentationPackage}
     */
    private String instrumentationPackage;

    /**
     * Class name of test runner. If not specified, it is inferred from <code>AndroidManifest.xml</code>.
     *
     * @optional
     * @parameter expression="${android.test.instrumentationRunner}"
     */
    private String instrumentationRunner;

    /**
     * Enable debug causing the test runner to wait until debugger is
     * connected.
     * @optional
     * @parameter default-value=false expression="${android.test.debug}"
     */
    private boolean testDebug;


    /**
     * Enable or disable code coverage for this test run.
     * @optional
     * @parameter default-value=false expression="${android.test.coverage}"
     */
    private boolean testCoverage;

    /**
     * Enable this flag to run a log only and not execute the tests
     * @optional
     * @parameter default-value=false expression="${android.test.logonly}"
     */
    private boolean testLogOnly;

    /**
     * If specified only execute tests of certain size as defined by the
     * SmallTest, MediumTest and LargeTest annotations. Use "small",
     * "medium" or "large" as values.
     * @see com.android.ddmlib.testrunner.IRemoteAndroidTestRunner
     *
     * @optional
     * @parameter expression="${android.test.testsize}"
     */
    private String testSize;

    /**
     * Create
     * @optional
     * @parameter default-value=true expression="${android.test.createreport}"
     */
    private boolean testCreateReport;

    /**
     * @optional
     * @parameter default-value="surefire-reports" expressions=${android.test.reportdirectory}
     */
    private String testReportDirectory;

    private boolean testClassesExists;
    private boolean testPackagesExists;
    private String testPackages;
    private String[] testClassesArray;


    protected void instrument() throws MojoExecutionException, MojoFailureException {
        if (instrumentationPackage == null) {
            instrumentationPackage = extractPackageNameFromAndroidManifest(androidManifestFile);
        }

        if (instrumentationRunner == null) {
            instrumentationRunner = extractInstrumentationRunnerFromAndroidManifest(androidManifestFile);
        }

        // only run Tests in specific package
        testPackages = buildTestPackagesString();
        testPackagesExists = StringUtils.isNotBlank(testPackages);

        if (testClasses != null) {
            testClassesArray = (String[]) testClasses.toArray();
            testClassesExists = testClassesArray.length > 0;
        } else {
            testClassesExists = false;
        }

        if(testClassesExists && testPackagesExists) {
            // if both testPackages and testClasses are specified --> ERROR
            throw new MojoFailureException("testPackages and testClasses are mutual exclusive. They cannot be specified at the same time. " +
                "Please specify either testPackages or testClasses! For details, see http://developer.android.com/guide/developing/testing/testing_otheride.html");
        }

        doWithDevices(new DeviceCallback() {
            public void doWithDevice(final IDevice device) throws MojoExecutionException, MojoFailureException {
                List<String> commands = new ArrayList<String>();

                RemoteAndroidTestRunner remoteAndroidTestRunner =
                    new RemoteAndroidTestRunner(instrumentationPackage, instrumentationRunner, device);

                if(testPackagesExists) {
                    remoteAndroidTestRunner.setTestPackageName(testPackages);
                    getLog().info("Running tests for specified test packages: " + testPackages);
                }

                if(testClassesExists) {
                    remoteAndroidTestRunner.setClassNames(testClassesArray);
                    getLog().info("Running tests for specified test " +
                        "classes/methods: " + Arrays.toString(testClassesArray));
                }

                remoteAndroidTestRunner.setDebug(testDebug);
                remoteAndroidTestRunner.setCoverage(testCoverage);
                remoteAndroidTestRunner.setLogOnly(testLogOnly);

                if (StringUtils.isNotBlank(testSize)) {
                    IRemoteAndroidTestRunner.TestSize validSize =
                        IRemoteAndroidTestRunner.TestSize.getTestSize(testSize);
                    remoteAndroidTestRunner.setTestSize(validSize);
                }

                getLog().info("Running instrumentation tests in " + instrumentationPackage + " on " +
                    device.getSerialNumber() + " (avdName=" + device.getAvdName() + ")");
                try {
                    AndroidTestRunListener testRunListener = new AndroidTestRunListener(project, device);
                    remoteAndroidTestRunner.run(testRunListener);
                    if (testRunListener.hasFailuresOrErrors()) {
                        throw new MojoFailureException("Tests failed on device.");
                    }
                } catch (TimeoutException e) {
                    throw new MojoExecutionException("timeout", e);
                } catch (AdbCommandRejectedException e) {
                    throw new MojoExecutionException("adb command rejected", e);
                } catch (ShellCommandUnresponsiveException e) {
                    throw new MojoExecutionException("shell command " +
                        "unresponsive", e);
                } catch (IOException e) {
                    throw new MojoExecutionException("IO problem", e);
                }
            }
        });
    }

    /**
     * AndroidTestRunListener produces a nice output for the log for the test
     * run as well as an xml file compatible with the junit xml report file
     * format understood by many tools.
     *
     * It will do so for each device/emulator the tests run on.
     */
    private class AndroidTestRunListener implements ITestRunListener {
        /** the indent used in the log to group items that belong together visually **/
        private static final String INDENT = "  ";

        /**
         * Junit report schema documentation is sparse. Here are some hints
         * @see "http://mail-archives.apache.org/mod_mbox/ant-dev/200902.mbox/%3Cdffc72020902241548l4316d645w2e98caf5f0aac770@mail.gmail.com%3E"
         * @see "http://junitpdfreport.sourceforge.net/managedcontent/PdfTranslation"
         */
        private static final String TAG_TESTSUITES = "testsuites";

        private static final String TAG_TESTSUITE = "testsuite";
        private static final String ATTR_TESTSUITE_ERRORS = "errors";
        private static final String ATTR_TESTSUITE_FAILURES = "failures";
        private static final String ATTR_TESTSUITE_HOSTNAME = "hostname";
        private static final String ATTR_TESTSUITE_NAME = "name";
        private static final String ATTR_TESTSUITE_TESTS = "tests";
        private static final String ATTR_TESTSUITE_TIME = "time";
        private static final String ATTR_TESTSUITE_TIMESTAMP = "timestamp";

        private static final String TAG_PROPERTIES = "properties";
        private static final String TAG_PROPERTY = "property";
        private static final String ATTR_PROPERTY_NAME = "name";
        private static final String ATTR_PROPERTY_VALUE = "value";

        private static final String TAG_TESTCASE = "testcase";
        private static final String ATTR_TESTCASE_NAME = "name";
        private static final String ATTR_TESTCASE_CLASSNAME = "classname";
        private static final String ATTR_TESTCASE_TIME = "time";

        private static final String TAG_ERROR = "error";
        private static final String TAG_FAILURE = "failure";
        private static final String ATTR_MESSAGE = "message";
        private static final String ATTR_TYPE = "type";


        /** time format for the output of milliseconds in seconds in the xml file **/
        private  final NumberFormat timeFormatter = new DecimalFormat("#0.0000");

        private int testCount = 0;
        private int testFailureCount = 0;
        private int testErrorCount = 0;

        private MavenProject project;
        private IDevice device;

        private long mTestStarted;
        private long mRunStarted;

        private Document junitReport;
        private Node testSuiteNode;
        private Node currentTestCaseNode;

        public AndroidTestRunListener(MavenProject project, IDevice device) {
            this.project = project;
            this.device = device;
        }

        public void testRunStarted(String runName, int testCount) {
            this.testCount = testCount;
            mRunStarted = new Date().getTime();
            getLog().info(INDENT + "Run started: " + runName + ", " +
                "" + testCount + " tests:");

            if (testCreateReport) {
                try {
                    DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
                    DocumentBuilder parser = null;
                    parser = fact.newDocumentBuilder();
                    junitReport = parser.newDocument();


                    Node testSuitesNode = junitReport.createElement(TAG_TESTSUITES);
                    junitReport.appendChild(testSuitesNode);

                    testSuiteNode = junitReport.createElement(TAG_TESTSUITE);
                    NamedNodeMap testSuiteAttributes = testSuiteNode.getAttributes();

                    Attr nameAttr = junitReport.createAttribute(ATTR_TESTSUITE_NAME);
                    nameAttr.setValue(runName);
                    testSuiteAttributes.setNamedItem(nameAttr);

                    Attr hostnameAttr = junitReport.createAttribute(ATTR_TESTSUITE_HOSTNAME);
                    hostnameAttr.setValue(getDeviceIdentifier());
                    testSuiteAttributes.setNamedItem(hostnameAttr);

                    Node propertiesNode = junitReport.createElement(TAG_PROPERTIES);
                    for (Map.Entry<Object, Object> property : System.getProperties().entrySet()) {
                        Node propertyNode = junitReport.createElement(TAG_PROPERTY);
                        NamedNodeMap propertyAttributes = propertyNode.getAttributes();

                        Attr propNameAttr = junitReport.createAttribute(ATTR_PROPERTY_NAME);
                        propNameAttr.setValue(property.getKey().toString());
                        propertyAttributes.setNamedItem(propNameAttr);

                        Attr propValueAttr = junitReport.createAttribute(ATTR_PROPERTY_VALUE);
                        propValueAttr.setValue(property.getValue().toString());
                        propertyAttributes.setNamedItem(propValueAttr);

                        propertiesNode.appendChild(propertyNode);

                    }
                    testSuiteNode.appendChild(propertiesNode);

                    testSuitesNode.appendChild(testSuiteNode);

                } catch (ParserConfigurationException e) {
//                    throw new MojoExecutionException("Failed to create junit document", e);
                }
            }
        }

        public void testStarted(TestIdentifier test) {
            mTestStarted = new Date().getTime();
            getLog().info(INDENT + INDENT +"Start: " + test.toString());

            if (testCreateReport) {
                currentTestCaseNode = junitReport.createElement(TAG_TESTCASE);
                NamedNodeMap testCaseAttributes = currentTestCaseNode.getAttributes();

                Attr classAttr = junitReport.createAttribute(ATTR_TESTCASE_CLASSNAME);
                classAttr.setValue(test.getClassName());
                testCaseAttributes.setNamedItem(classAttr);

                Attr methodAttr = junitReport.createAttribute(ATTR_TESTCASE_NAME);
                methodAttr.setValue(test.getTestName());
                testCaseAttributes.setNamedItem(methodAttr);
            }
        }

        public void testFailed(TestFailure status, TestIdentifier test, String trace) {
            if (status==ERROR) {
                ++testErrorCount;
            } else {
                ++testFailureCount;
            }
            getLog().info(INDENT + INDENT + status.name() + ":" + test.toString());
            getLog().info(INDENT + INDENT + trace);

            if (testCreateReport) {
                Node errorFailureNode;
                NamedNodeMap errorfailureAttributes;
                if (status == ERROR) {
                    errorFailureNode = junitReport.createElement(TAG_ERROR);
                    errorfailureAttributes = errorFailureNode.getAttributes();
                } else {
                    errorFailureNode = junitReport.createElement(TAG_FAILURE);
                    errorfailureAttributes= errorFailureNode.getAttributes();
                }

                errorFailureNode.setTextContent(trace);

                Attr msgAttr = junitReport.createAttribute(ATTR_MESSAGE);
                msgAttr.setValue(parseForMessage(trace));
                errorfailureAttributes.setNamedItem(msgAttr);

                Attr typeAttr = junitReport.createAttribute(ATTR_TYPE);
                typeAttr.setValue(parseForException(trace));
                errorfailureAttributes.setNamedItem(typeAttr);

                currentTestCaseNode.appendChild(errorFailureNode);
            }
        }

        public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
            getLog().info( INDENT + INDENT +"End: " + test.toString());
            logMetrics(testMetrics);

            if (testCreateReport) {
                testSuiteNode.appendChild(currentTestCaseNode);
                NamedNodeMap testCaseAttributes = currentTestCaseNode.getAttributes();

                Attr timeAttr = junitReport.createAttribute(ATTR_TESTCASE_TIME);
                timeAttr.setValue(getTestDuration());
                testCaseAttributes.setNamedItem(timeAttr);
            }
        }

        public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
            getLog().info(INDENT +"Run ended: " + elapsedTime + " ms");
            if (hasFailuresOrErrors()) {
                getLog().error(INDENT + "FAILURES!!!");
            }
            getLog().info(INDENT + "Tests run: " + testCount + ",  Failures: "
                    + testFailureCount + ",  Errors: " + testErrorCount);
            if (testCreateReport) {
                NamedNodeMap testSuiteAttributes = testSuiteNode.getAttributes();

                Attr testCountAttr = junitReport.createAttribute(ATTR_TESTSUITE_TESTS);
                testCountAttr.setValue(Integer.toString(testCount));
                testSuiteAttributes.setNamedItem(testCountAttr);

                Attr testFailuresAttr = junitReport.createAttribute(ATTR_TESTSUITE_FAILURES);
                testFailuresAttr.setValue(Integer.toString(testFailureCount));
                testSuiteAttributes.setNamedItem(testFailuresAttr);

                Attr testErrorsAttr = junitReport.createAttribute(ATTR_TESTSUITE_ERRORS);
                testErrorsAttr.setValue(Integer.toString(testErrorCount));
                testSuiteAttributes.setNamedItem(testErrorsAttr);

                Attr timeAttr = junitReport.createAttribute(ATTR_TESTSUITE_TIME);
                timeAttr.setValue(timeFormatter.format(elapsedTime / 1000.0));
                testSuiteAttributes.setNamedItem(timeAttr);

                Attr timeStampAttr = junitReport.createAttribute(ATTR_TESTSUITE_TIMESTAMP);
                timeStampAttr.setValue(
                        new Date().toString());
                testSuiteAttributes.setNamedItem(timeStampAttr);
            }

            logMetrics(runMetrics);

            if (testCreateReport) {
                writeJunitReportToFile();
            }
        }


        private String getTestDuration() {
            long now = new Date().getTime();
            double seconds = (now - mTestStarted)/1000.0;
            return timeFormatter.format(seconds);
        }

        public void testRunFailed(String errorMessage) {
            getLog().info(INDENT +"Run failed: " + errorMessage);
        }

        public void testRunStopped(long elapsedTime) {
            getLog().info(INDENT +"Run stopped:" + elapsedTime);
        }

        private String parseForMessage(String trace) {
            return trace.substring(trace.indexOf(":") + 2, trace.indexOf("\r\n"));
        }

        private String parseForException(String trace) {
            return trace.substring(0, trace.indexOf(":"));
        }

        private void writeJunitReportToFile() {
            TransformerFactory xfactory = TransformerFactory.newInstance();
            Transformer xformer = null;
            try {
                xformer = xfactory.newTransformer();
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            }
            Source source = new DOMSource(junitReport);

            FileWriter writer = null;
            try {
                String directory =  new StringBuilder()
                        .append(project.getBuild().getDirectory())
                        .append("/")
                        .append(testReportDirectory).toString();

                createDirectoryIfNecessary(directory);

                String fileName = new StringBuilder()
                        .append(project.getBuild().getDirectory())
                        .append("/")
                        .append(testReportDirectory)
                        .append("/TEST-")
                        .append(getDeviceIdentifier())
                        .append(".xml")
                        .toString();
                File reportFile = new File(fileName);
                writer = new FileWriter(reportFile);
                Result result = new StreamResult(writer);

                xformer.transform(source, result);
                getLog().info("Report file written to " + reportFile.getAbsolutePath());
            } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (TransformerException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } finally {
                IOUtils.closeQuietly(writer);
            }
        }

        private void logMetrics(Map<String, String> metrics) {
            for (Map.Entry<String, String> entry : metrics.entrySet()) {
                getLog().info(INDENT + INDENT + entry.getKey() + ": "
                    + entry.getValue());
            }
        }

        public boolean hasFailuresOrErrors() {
            return testErrorCount > 0 || testFailureCount > 0;
        }

        private String getDeviceIdentifier() {
            StringBuilder identfier = new StringBuilder()
                    .append(device.getSerialNumber());
             if (device.getAvdName() != null) {
                identfier.append("_").append(device.getAvdName());
             }
            return identfier.toString();
        }
    }

    private void createDirectoryIfNecessary(String testReportDirectory) {
        File directory = new File(testReportDirectory);
        if (!directory.isDirectory()) {
            directory.mkdir();
        }
    }
}
