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
package com.simpligility.maven.plugins.android;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.simpligility.maven.plugins.android.asm.AndroidTestFinder;
import com.simpligility.maven.plugins.android.common.DeviceHelper;
import com.simpligility.maven.plugins.android.configuration.Test;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AbstractInstrumentationMojo implements running the instrumentation
 * tests.
 *
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 */
public abstract class AbstractInstrumentationMojo extends AbstractAndroidMojo
{

    /**
     * -Dmaven.test.skip is commonly used with Maven to skip tests. We honor it too.
     */
    @Parameter( property = "maven.test.skip", defaultValue = "false", readonly = true )
    private boolean mavenTestSkip;

    /**
     * -DskipTests is commonly used with Maven to skip tests. We honor it too.
     */
    @Parameter( property = "skipTests", defaultValue = "false", readonly = true )
    private boolean mavenSkipTests;

    /**
     * -Dmaven.test.failure.ignore is commonly used with Maven to ignore test failures. We honor it too.
     * Ignore or not tests failures. If <code>true</code> they will be ignored; if
     * <code>false</code>, they will not. Default value is <code>false</code>.
     */
    @Parameter( property = "maven.test.failure.ignore", defaultValue = "false", readonly = true )
    private boolean mavenIgnoreTestFailure;

    /**
     * -Dmaven.test.error.ignore is commonly used with Maven to ignore test errors. We honor it too.
     * Ignore or not tests errors. If <code>true</code> they will be ignored; if
     * <code>false</code>, they will not. Default value is <code>false</code>.
     */
    @Parameter( property = "maven.test.error.ignore", defaultValue = "false", readonly = true )
    private boolean mavenIgnoreTestError;

    /**
     * The configuration to use for running instrumentation tests. Complete configuration
     * is possible in the plugin configuration:
     * <pre>
     * &lt;test&gt;
     *   &lt;skip&gt;true|false|auto&lt;/skip&gt;
     *   &lt;instrumentationPackage&gt;packageName&lt;/instrumentationPackage&gt;
     *   &lt;instrumentationRunner&gt;className&lt;/instrumentationRunner&gt;
     *   &lt;debug&gt;true|false&lt;/debug&gt;
     *   &lt;coverage&gt;true|false&lt;/coverage&gt;
     *   &lt;coverageFile&gt;&lt;/coverageFile&gt;
     *   &lt;logOnly&gt;true|false&lt;/logOnly&gt;  avd
     *   &lt;testSize&gt;small|medium|large&lt;/testSize&gt;
     *   &lt;createReport&gt;true|false&lt;/createReport&gt;
     *   &lt;classes&gt;
     *     &lt;class&gt;your.package.name.YourTestClass&lt;/class&gt;
     *   &lt;/classes&gt;
     *   &lt;packages&gt;
     *     &lt;package&gt;your.package.name&lt;/package&gt;
     *   &lt;/packages&gt;
     *   &lt;instrumentationArgs&gt;
     *     &lt;instrumentationArg&gt;key value&lt;/instrumentationArg&gt;
     *   &lt;/instrumentationArgs&gt;
     * &lt;/test&gt;
     * </pre>
     */
    @Parameter
    private Test test;

    /**
     * Enables or disables integration test related goals. If <code>true</code> they will be skipped; if
     * <code>false</code>, they will be run. If <code>auto</code>, they will run if any of the classes inherit from any
     * class in <code>junit.framework.**</code> or <code>android.test.**</code>.
     */
    @Parameter( property = "android.test.skip", defaultValue = "auto" )
    private String testSkip;

    /**
     * Enables or disables integration safe failure.
     * If <code>true</code> build will not stop on test failure or error.
     */
    @Parameter( property = "android.test.failsafe", defaultValue = "true" )
    private Boolean testFailSafe;

    /**
     * Package name of the apk we wish to instrument. If not specified, it is inferred from
     * <code>AndroidManifest.xml</code>.
     */
    @Parameter( property = "android.test.instrumentationPackage" )
    private String testInstrumentationPackage;

    /**
     * Class name of test runner. If not specified, it is inferred from <code>AndroidManifest.xml</code>.
     */
    @Parameter( property = "android.test.instrumentationRunner" )
    private String testInstrumentationRunner;

    /**
     * Enable debug causing the test runner to wait until debugger is
     * connected with the Android debug bridge (adb).
     */
    @Parameter( property = "android.test.debug", defaultValue = "false" )
    private Boolean testDebug;

    /**
     * Enable or disable code coverage for this instrumentation test run.
     */
    @Parameter( property = "android.test.coverage", defaultValue = "false" )
    private Boolean testCoverage;

    /**
     * Location on device into which coverage should be stored (blank for
     * Android default /data/data/your.package.here/files/coverage.ec).
     */
    @Parameter( property = "android.test.coverageFile" )
    private String testCoverageFile;

    /**
     * Enable this flag to run a log only and not execute the tests.
     */
    @Parameter( property = "android.test.logonly", defaultValue = "false" )
    private Boolean testLogOnly;

    /**
     * If specified only execute tests of certain size as defined by
     * the Android instrumentation testing SmallTest, MediumTest and
     * LargeTest annotations. Use "small", "medium" or "large" as values.
     *
     * @see com.android.ddmlib.testrunner.IRemoteAndroidTestRunner
     */
    @Parameter( property = "android.test.testsize" )
    private String testTestSize;

    /**
     * Create a junit xml format compatible output file containing
     * the test results for each device the instrumentation tests run
     * on.
     * <br /><br />
     * The files are stored in target/surefire-reports and named TEST-deviceid.xml.
     * The deviceid for an emulator is deviceSerialNumber_avdName_manufacturer_model.
     * The serial number is commonly emulator-5554 for the first emulator started
     * with numbers increasing. avdName is as defined in the SDK tool. The
     * manufacturer is typically "unknown" and the model is typically "sdk".
     * The deviceid for an actual devices is
     * deviceSerialNumber_manufacturer_model.
     * <br /><br />
     * The file contains system properties from the system running
     * the Android Maven Plugin (JVM) and device properties from the
     * device/emulator the tests are running on.
     * <br /><br />
     * The file contains a single TestSuite for all tests and a
     * TestCase for each test method. Errors and failures are logged
     * in the file and the system log with full stack traces and other
     * details available.
     */
    @Parameter( property = "android.test.createreport", defaultValue = "true" )
    private Boolean testCreateReport;

    /**
     * <p>Whether to execute tests only in given packages as part of the instrumentation tests.</p>
     * <pre>
     * &lt;packages&gt;
     *     &lt;package&gt;your.package.name&lt;/package&gt;
     * &lt;/packages&gt;
     * </pre>
     * or as e.g. -Dandroid.test.packages=package1,package2
     */
    @Parameter( property = "android.test.packages" )
    protected List<String> testPackages;

    /**
     * <p>Whether to execute test classes which are specified as part of the instrumentation tests.</p>
     * <pre>
     * &lt;classes&gt;
     *     &lt;class&gt;your.package.name.YourTestClass&lt;/class&gt;
     * &lt;/classes&gt;
     * </pre>
     * or as e.g. -Dandroid.test.classes=class1,class2
     */
    @Parameter( property = "android.test.classes" )
    protected List<String> testClasses;


    /**
     * <p>Whether to execute tests which are annotated with the given annotations.</p>
     * <pre>
     * &lt;annotations&gt;
     *     &lt;annotation&gt;your.package.name.YourAnnotation&lt;/annotation&gt;
     * &lt;/annotations&gt;
     * </pre>
     * or as e.g. -Dandroid.test.annotations=annotation1,annotation2
     */
    @Parameter( property = "android.test.annotations" )
    protected List<String> testAnnotations;

    /**
     * <p>Whether to execute tests which are <strong>not</strong> annotated with the given annotations.</p>
     * <pre>
     * &lt;excludeAnnotations&gt;
     *     &lt;excludeAnnotation&gt;your.package.name.YourAnnotation&lt;/excludeAnnotation&gt;
     * &lt;/excludeAnnotations&gt;
     * </pre>
     * or as e.g. -Dandroid.test.excludeAnnotations=annotation1,annotation2
     */
    @Parameter( property = "android.test.excludeAnnotations" )
    protected List<String> testExcludeAnnotations;

    /**
     * <p>Extra instrumentation arguments.</p>
     * <pre>
     * &lt;instrumentationArgs&gt;
     *     &lt;instrumentationArg&gt;key value&lt;/instrumentationArg&gt;
     *     &lt;instrumentationArg&gt;key 'value with spaces'&lt;/instrumentationArg&gt;
     * &lt;/instrumentationArgs&gt;
     * </pre>
     * or as e.g. -Dandroid.test.instrumentationArgs="key1 value1","key2 'value with spaces'"
     */
    @Parameter( property = "android.test.instrumentationArgs" )
    protected List<String> testInstrumentationArgs;

    private boolean classesExists;
    private boolean packagesExists;

    // the parsed parameters from the plugin config or properties from command line or pom or settings
    private String parsedSkip;
    private String parsedInstrumentationPackage;
    private String parsedInstrumentationRunner;
    private List<String> parsedClasses;
    private List<String> parsedPackages;
    private List<String> parsedAnnotations;
    private List<String> parsedExcludeAnnotations;
    private Map<String, String> parsedInstrumentationArgs;
    private String parsedTestSize;
    private Boolean parsedCoverage;
    private String parsedCoverageFile;
    private Boolean parsedDebug;
    private Boolean parsedLogOnly;
    private Boolean parsedCreateReport;

    private String packagesList;

    protected void instrument() throws MojoExecutionException, MojoFailureException
    {
        parseConfiguration();

        if ( parsedInstrumentationPackage == null )
        {
            parsedInstrumentationPackage = extractPackageNameFromAndroidManifest( destinationManifestFile );
        }

        if ( parsedInstrumentationRunner == null )
        {
            parsedInstrumentationRunner = extractInstrumentationRunnerFromAndroidManifest( destinationManifestFile );
        }

        // only run Tests in specific package
        packagesList = buildCommaSeparatedString( parsedPackages );
        packagesExists = StringUtils.isNotBlank( packagesList );

        if ( parsedClasses != null )
        {
            classesExists = parsedClasses.size() > 0;
        }
        else
        {
            classesExists = false;
        }

        if ( classesExists && packagesExists )
        {
            // if both packages and classes are specified --> ERROR
            throw new  MojoFailureException( "packages and classes are mutually exclusive. They cannot be specified at"
                    + " the same time. Please specify either packages or classes. For details, see "
                    + "http://developer.android.com/guide/developing/testing/testing_otheride.html" );
        }

        DeviceCallback instrumentationTestExecutor = new DeviceCallback()
        {
            public void doWithDevice( final IDevice device ) throws MojoExecutionException, MojoFailureException
            {
                String deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );

                RemoteAndroidTestRunner remoteAndroidTestRunner = new RemoteAndroidTestRunner(
                        parsedInstrumentationPackage, parsedInstrumentationRunner, device );

                if ( packagesExists )
                {
                    for ( String str : packagesList.split( "," ) )
                    {
                        remoteAndroidTestRunner.setTestPackageName( str );
                        getLog().info( deviceLogLinePrefix + "Running tests for specified test package: " + str );
                    }
                }

                if ( classesExists )
                {
                    remoteAndroidTestRunner
                            .setClassNames( parsedClasses.toArray( new String[ parsedClasses.size() ] ) );
                    getLog().info( deviceLogLinePrefix + "Running tests for specified test classes/methods: " 
                            + parsedClasses );
                }

                if ( parsedAnnotations != null )
                {
                    for ( String annotation : parsedAnnotations )
                    {
                        remoteAndroidTestRunner.addInstrumentationArg( "annotation", annotation );
                    }
                }

                if ( parsedExcludeAnnotations != null )
                {
                    for ( String annotation : parsedExcludeAnnotations )
                    {
                        remoteAndroidTestRunner.addInstrumentationArg( "notAnnotation", annotation );
                    }

                }

                remoteAndroidTestRunner.setDebug( parsedDebug );
                remoteAndroidTestRunner.setCoverage( parsedCoverage );
                if ( ! "".equals( parsedCoverageFile ) )
                {
                    remoteAndroidTestRunner.addInstrumentationArg( "coverageFile", parsedCoverageFile );
                }
                remoteAndroidTestRunner.setLogOnly( parsedLogOnly );

                if ( StringUtils.isNotBlank( parsedTestSize ) )
                {
                    IRemoteAndroidTestRunner.TestSize validSize = IRemoteAndroidTestRunner.TestSize
                            .getTestSize( parsedTestSize );
                    remoteAndroidTestRunner.setTestSize( validSize );
                }

                addAllInstrumentationArgs( remoteAndroidTestRunner, parsedInstrumentationArgs );

                getLog().info( deviceLogLinePrefix +  "Running instrumentation tests in " 
                        + parsedInstrumentationPackage );
                try
                {
                    AndroidTestRunListener testRunListener = new AndroidTestRunListener( project, device, getLog(),
                            parsedCreateReport, false, "", "", targetDirectory );
                    remoteAndroidTestRunner.run( testRunListener );
                    if ( testRunListener.hasFailuresOrErrors() && !testFailSafe )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix + "Tests failed on device." );
                    }
                    if ( testRunListener.testRunFailed() && !testFailSafe  )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix + "Test run failed to complete: " 
                                + testRunListener.getTestRunFailureCause() );
                    }
                    if ( testRunListener.threwException() && !testFailSafe  )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix +  testRunListener.getExceptionMessages() );
                    }
                }
                catch ( TimeoutException e )
                {
                    throw new MojoExecutionException( deviceLogLinePrefix + "timeout", e );
                }
                catch ( AdbCommandRejectedException e )
                {
                    throw new MojoExecutionException( deviceLogLinePrefix + "adb command rejected", e );
                }
                catch ( ShellCommandUnresponsiveException e )
                {
                    throw new MojoExecutionException( deviceLogLinePrefix + "shell command " + "unresponsive", e );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( deviceLogLinePrefix + "IO problem", e );
                }
            }
        };

        instrumentationTestExecutor = new ScreenshotServiceWrapper( instrumentationTestExecutor, project, getLog() );

        doWithDevices( instrumentationTestExecutor );
    }

    private void addAllInstrumentationArgs(
            final RemoteAndroidTestRunner remoteAndroidTestRunner,
            final Map<String, String> parsedInstrumentationArgs )
    {
        for ( final Map.Entry<String, String> entry : parsedInstrumentationArgs.entrySet() )
        {
            remoteAndroidTestRunner.addInstrumentationArg( entry.getKey(), entry.getValue() );
        }
    }

    private void parseConfiguration()
    {
        // we got config in pom ... lets use it,
        if ( test != null )
        {
            if ( StringUtils.isNotEmpty( test.getSkip() ) )
            {
                parsedSkip = test.getSkip();
            }
            else
            {
                parsedSkip = testSkip;
            }
            if ( StringUtils.isNotEmpty( test.getInstrumentationPackage() ) )
            {
                parsedInstrumentationPackage = test.getInstrumentationPackage();
            }
            else
            {
                parsedInstrumentationPackage = testInstrumentationPackage;
            }
            if ( StringUtils.isNotEmpty( test.getInstrumentationRunner() ) )
            {
                parsedInstrumentationRunner = test.getInstrumentationRunner();
            }
            else
            {
                parsedInstrumentationRunner = testInstrumentationRunner;
            }
            if ( test.getClasses() != null && ! test.getClasses().isEmpty() )
            {
                parsedClasses = test.getClasses();
            }
            else
            {
                parsedClasses = testClasses;
            }
            if ( test.getAnnotations() != null && ! test.getAnnotations().isEmpty() )
            {
                parsedAnnotations = test.getAnnotations();
            }
            else
            {
                parsedAnnotations =  testAnnotations;
            }
            if ( test.getExcludeAnnotations() != null && ! test.getExcludeAnnotations().isEmpty() )
            {
                parsedExcludeAnnotations = test.getExcludeAnnotations();
            }
            else
            {
                parsedExcludeAnnotations = testExcludeAnnotations;
            }
            if ( test.getPackages() != null && ! test.getPackages().isEmpty() )
            {
                parsedPackages = test.getPackages();
            }
            else
            {
                parsedPackages = testPackages;
            }
            if ( StringUtils.isNotEmpty( test.getTestSize() ) )
            {
                parsedTestSize = test.getTestSize();
            }
            else
            {
                parsedTestSize = testTestSize;
            }
            if ( test.isCoverage() != null )
            {
                parsedCoverage = test.isCoverage();
            }
            else
            {
                parsedCoverage = testCoverage;
            }
            if ( test.getCoverageFile() != null )
            {
                parsedCoverageFile = test.getCoverageFile();
            }
            else
            {
                parsedCoverageFile = "";
            }
            if ( test.isDebug() != null )
            {
                parsedDebug = test.isDebug();
            }
            else
            {
                parsedDebug = testDebug;
            }
            if ( test.isLogOnly() != null )
            {
                parsedLogOnly = test.isLogOnly();
            }
            else
            {
                parsedLogOnly = testLogOnly;
            }
            if ( test.isCreateReport() != null )
            {
                parsedCreateReport = test.isCreateReport();
            }
            else
            {
                parsedCreateReport = testCreateReport;
            }

            parsedInstrumentationArgs = InstrumentationArgumentParser.parse( test.getInstrumentationArgs() );
        }
        // no pom, we take properties
        else
        {
            parsedSkip = testSkip;
            parsedInstrumentationPackage = testInstrumentationPackage;
            parsedInstrumentationRunner = testInstrumentationRunner;
            parsedClasses = testClasses;
            parsedAnnotations = testAnnotations;
            parsedExcludeAnnotations = testExcludeAnnotations;
            parsedPackages = testPackages;
            parsedTestSize = testTestSize;
            parsedCoverage = testCoverage;
            parsedCoverageFile = testCoverageFile;
            parsedDebug = testDebug;
            parsedLogOnly = testLogOnly;
            parsedCreateReport = testCreateReport;
            parsedInstrumentationArgs = InstrumentationArgumentParser.parse( testInstrumentationArgs );
        }
    }

    /**
     * Whether or not to execute integration test related goals. Reads from configuration parameter
     * <code>enableIntegrationTest</code>, but can be overridden with <code>-Dmaven.test.skip</code>.
     *
     * @return <code>true</code> if integration test goals should be executed, <code>false</code> otherwise.
     */
    protected boolean isEnableIntegrationTest() throws MojoFailureException, MojoExecutionException
    {
        parseConfiguration();
        if ( mavenTestSkip )
        {
            getLog().info( "maven.test.skip set - skipping tests" );
            return false;
        }

        if ( mavenSkipTests )
        {
            getLog().info( "maven.skip.tests set - skipping tests" );
            return false;
        }

        if ( "true".equalsIgnoreCase( parsedSkip ) )
        {
            getLog().info( "android.test.skip set - skipping tests" );
            return false;
        }

        if ( "false".equalsIgnoreCase( parsedSkip ) )
        {
            return true;
        }

        if ( parsedSkip == null || "auto".equalsIgnoreCase( parsedSkip ) )
        {
            if ( extractInstrumentationRunnerFromAndroidManifest( destinationManifestFile ) == null )
            {
                getLog().info( "No InstrumentationRunner found - skipping tests" );
                return false;
            }
            return AndroidTestFinder.containsAndroidTests( projectOutputDirectory );
        }

        throw new MojoFailureException( "android.test.skip must be configured as 'true', 'false' or 'auto'." );

    }

    /**
     * Helper method to build a comma separated string from a list.
     * Blank strings are filtered out
     *
     * @param lines A list of strings
     * @return Comma separated String from given list
     */
    protected static String buildCommaSeparatedString( List<String> lines )
    {
        if ( lines == null || lines.size() == 0 )
        {
            return null;
        }

        List<String> strings = new ArrayList<String>( lines.size() );
        for ( String str : lines )
        { // filter out blank strings
            if ( StringUtils.isNotBlank( str ) )
            {
                strings.add( StringUtils.trimToEmpty( str ) );
            }
        }

        return StringUtils.join( strings, "," );
    }

}
