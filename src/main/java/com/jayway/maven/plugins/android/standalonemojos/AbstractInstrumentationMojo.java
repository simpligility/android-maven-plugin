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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
                    remoteAndroidTestRunner.run(new AndroidTestRunListener());
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
     * run.
     */
    private class AndroidTestRunListener implements ITestRunListener {
        private static final String INDENT = "  ";

        public void testRunStarted(String runName, int testCount) {
            getLog().info(INDENT + "Run started: " + runName + ", " +
                "" + testCount + " tests:");
        }

        public void testStarted(TestIdentifier test) {
            getLog().info(INDENT + INDENT +"Start: " + test.toString());
        }

        public void testFailed(TestFailure status, TestIdentifier test, String trace) {
            getLog().info(INDENT + INDENT +status.name() + ":" + test.toString());
            getLog().info(INDENT + INDENT + trace);
        }

        public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
            getLog().info( INDENT + INDENT +"End: " + test.toString());
            logMetrics(testMetrics);
        }

        public void testRunFailed(String errorMessage) {
            getLog().info(INDENT +"Run failed: " + errorMessage);
        }

        public void testRunStopped(long elapsedTime) {
            getLog().info(INDENT +"Run stopped:" + elapsedTime);
        }

        public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
            getLog().info(INDENT +"Run ended:" + elapsedTime);
            logMetrics(runMetrics);
        }

        private void logMetrics(Map<String, String> metrics) {
            for (Map.Entry<String, String> entry : metrics.entrySet()) {
                getLog().info(INDENT + INDENT + entry.getKey() + ": "
                    + entry.getValue());
            }
        }
    }
}
