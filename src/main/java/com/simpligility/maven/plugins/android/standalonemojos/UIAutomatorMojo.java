/*
 * Copyright (C) 2009 Jayway AB
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
package com.simpligility.maven.plugins.android.standalonemojos;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.UIAutomatorRemoteAndroidTestRunner;
import com.simpligility.maven.plugins.android.AbstractAndroidMojo;
import com.simpligility.maven.plugins.android.AndroidTestRunListener;
import com.simpligility.maven.plugins.android.DeviceCallback;
import com.simpligility.maven.plugins.android.ScreenshotServiceWrapper;
import com.simpligility.maven.plugins.android.common.DeviceHelper;
import com.simpligility.maven.plugins.android.config.ConfigHandler;
import com.simpligility.maven.plugins.android.config.ConfigPojo;
import com.simpligility.maven.plugins.android.config.PullParameter;
import com.simpligility.maven.plugins.android.configuration.UIAutomator;

/**
 * Can execute tests using ui uiautomator.<br/>
 * Implements parsing parameters from pom or command line arguments and sets useful defaults as well. This goal is meant
 * to execute a special <i>java project</i> dedicated to UI testing via UIAutomator. It will build the jar of the
 * project, dex it and send it to dalvik cache of a rooted device or to an emulator. If you use a rooted device, refer
 * to <a href="http://stackoverflow.com/a/13805869/693752">this thread on stack over flow</a>. <br />
 * <br />
 * The tests are executed via ui automator. A surefire compatible test report can be generated and its location will be
 * logged during build. <br />
 * <br />
 * To use this goal, you will need to place the uiautomator.jar file (part of the Android SDK >= 16) on a nexus
 * repository. <br />
 * <br />
 * A typical usage of this goal can be found at <a
 * href="https://github.com/stephanenicolas/Quality-Tools-for-Android">Quality tools for Android project</a>.
 * 
 * @see <a href="http://developer.android.com/tools/testing/testing_ui.html">Android UI testing doc</a>
 * @see <a href="http://developer.android.com/tools/help/uiautomator/index.html">UI Automator manual page</a>
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
@SuppressWarnings( "unused" )
@Mojo( name = "uiautomator", requiresProject = false )
public class UIAutomatorMojo extends AbstractAndroidMojo
{
    /**
     * -Dmaven.test.skip is commonly used with Maven to skip tests. We honor it.
     */
    @Parameter( property = "maven.test.skip", defaultValue = "false", readonly = true )
    private boolean mavenTestSkip;

    /**
     * -DskipTests is commonly used with Maven to skip tests. We honor it too.
     */
    @Parameter( property = "skipTests", defaultValue = "false", readonly = true )
    private boolean mavenSkipTests;

    /**
     * -Dmaven.test.failure.ignore is commonly used with Maven to prevent failure of build when (some) tests fail. We
     * honor it too. Builds will still fail if tests can't complete or throw an exception.
     */
    @Parameter( property = "maven.test.failure.ignore", defaultValue = "false", readonly = true )
    private boolean mavenTestFailureIgnore;

    /**
     * -Dmaven.test.failure.ignore is commonly used with Maven to prevent failure of build when (some) tests fail. We
     * honor it too. Builds will still fail if tests can't complete or throw an exception.
     */
    @Parameter( property = "testFailureIgnore", defaultValue = "false", readonly = true )
    private boolean mavenIgnoreTestFailure;

    /**
     * The configuration for the ui automator goal. As soon as a lint goal is invoked the command will be executed
     * unless the skip parameter is set. A minimal configuration that will run lint and produce a XML report in
     * ${project.build.directory}/lint/lint-results.xml is
     * 
     * <pre>
     * &lt;uiautomator&gt;
     *   &lt;skip&gt;false&lt;/skip&gt;
     * &lt;/uiautomator&gt;
     * </pre>
     * 
     * Full configuration can use these parameters.
     * 
     * <pre>
     * &lt;uiautomator&gt;
     *   &lt;skip&gt;false&lt;/skip&gt;
     *   &lt;testClassOrMethods&gt;
     *     &lt;testClassOrMethod&gt;com.foo.SampleTest&lt;/testClassOrMethod&gt;
     *     &lt;testClassOrMethod&gt;com.bar.CalculatorTest#testCalculatorApp&lt;/testClassOrMethod&gt;
     *   &lt;/testClassOrMethods&gt;
     *   &lt;createReport&gt;true&lt;/createReport&gt;
     *   &lt;takeScreenshotOnFailure&gt;true&lt;/takeScreenshotOnFailure&gt;
     *   &lt;screenshotsPathOnDevice&gt;/sdcard/uiautomator-screenshots/&lt;/screenshotsPathOnDevice&gt;
     *   &lt;propertiesKeyPrefix&gt;UIA&lt;/propertiesKeyPrefix&gt;
     * &lt;/uiautomator&gt;
     * </pre>
     * 
     * 
     * Alternatively to the plugin configuration values can also be configured as properties on the command line as
     * android.lint.* or in pom or settings file as properties like lint*.
     */
    @Parameter
    @ConfigPojo
    private UIAutomator uiautomator;

    /**
     * Enables or disables uiautomator test goal. If <code>true</code> it will be skipped; if <code>false</code>, it
     * will be run.
     */
    @Parameter( property = "android.uiautomator.skip" )
    private Boolean uiautomatorSkip;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedSkip;

    /**
     * Jar file that will be run during ui uiautomator tests.
     */
    @Parameter( property = "android.uiautomator.jarFile" )
    private String uiautomatorJarFile;

    @PullParameter( defaultValueGetterMethod = "getJarFile" )
    private String parsedJarFile;

    /**
     * Test class or methods to execute during uiautomator tests. Each class or method must be fully qualified with the
     * package name, in one of these formats:
     * <ul>
     * <li>package_name.class_name
     * <li>package_name.class_name#method_name
     * </ul>
     */
    @Parameter( property = "android.uiautomator.testClassOrMethod" )
    private String[] uiautomatorTestClassOrMethods;

    @PullParameter( required = false, defaultValueGetterMethod = "getTestClassOrMethods" )
    private String[] parsedTestClassOrMethods;

    /**
     * Decides whether to run the test to completion on the device even if its parent process is terminated (for
     * example, if the device is disconnected).
     */
    @Parameter( property = "android.uiautomator.noHup" )
    private Boolean uiautomatorNoHup;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedNoHup;

    /**
     * Decides whether to wait for debugger to connect before starting.
     */
    @Parameter( property = "android.uiautomator.debug" )
    private Boolean uiautomatorDebug = false;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedDebug;

    /**
     * Decides whether to use a dump file or not.
     */
    @Parameter( property = "android.uiautomator.useDump" )
    private Boolean uiautomatorUseDump;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedUseDump;

    /**
     * Generate an XML file with a dump of the current UI hierarchy. If a filepath is not specified, by default, the
     * generated dump file is stored on the device in this location /storage/sdcard0/window_dump.xml.
     */
    @Parameter( property = "android.uiautomator.dumpFilePath" )
    private String uiautomatorDumpFilePath;

    @PullParameter( required = false, defaultValue = "/storage/sdcard0/window_dump.xml" )
    private String parsedDumpFilePath;

    /**
     * Create a junit xml format compatible output file containing the test results for each device the instrumentation
     * tests run on. <br />
     * <br />
     * The files are stored in target/surefire-reports and named TEST-deviceid.xml. The deviceid for an emulator is
     * deviceSerialNumber_avdName_manufacturer_model. The serial number is commonly emulator-5554 for the first emulator
     * started with numbers increasing. avdName is as defined in the SDK tool. The manufacturer is typically "unknown"
     * and the model is typically "sdk".<br />
     * The deviceid for an actual devices is deviceSerialNumber_manufacturer_model. <br />
     * <br />
     * The file contains system properties from the system running the Android Maven Plugin (JVM) and device properties
     * from the device/emulator the tests are running on. <br />
     * <br />
     * The file contains a single TestSuite for all tests and a TestCase for each test method. Errors and failures are
     * logged in the file and the system log with full stack traces and other details available.
     */
    @Parameter( property = "android.uiautomator.createReport" )
    private Boolean uiautomatorCreateReport;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedCreateReport;

    /**
     * Adds a suffix to the report name. For example if parameter reportSuffix is "-mySpecialReport",
     * the name of the report will be TEST-deviceid-mySpecialReport.xml
     *
     * Defaults to null. Hence, in the default case, the name of the report will be TEST-deviceid.xml.
     */
    @Parameter( property = "android.uiautomator.reportSuffix" )
    private String uiautomatorReportSuffix;

    @PullParameter( required = false, defaultValueGetterMethod = "getReportSuffix" )
    private String parsedReportSuffix;

    /**
     * Decides whether or not to take screenshots when tests execution results in failure or error. Screenshots use the
     * utiliy screencap that is usually available within emulator/devices with SDK >= 16.
     */
    @Parameter( property = "android.uiautomator.takeScreenshotOnFailure" )
    private Boolean uiautomatorTakeScreenshotOnFailure;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedTakeScreenshotOnFailure;

    /**
     * Location of the screenshots on device. This value is only taken into account if takeScreenshotOnFailure = true.
     * If a filepath is not specified, by default, the screenshots will be located at /sdcard/uiautomator-screenshots/.
     */
    @Parameter( property = "android.uiautomator.screenshotsPathOnDevice" )
    private String uiautomatorScreenshotsPathOnDevice;

    @PullParameter( required = false, defaultValue = "/sdcard/uiautomator-screenshots/" )
    private String parsedScreenshotsPathOnDevice;
    
    /**
     * <p>Specifies a prefix for custom user properties that will be sent 
     * through to UIAutomator with the <code>"-e key value"</code> parameter.</p>
     * 
     * <p>If any user property is needed in a test case, this is the way to send it through.
     * User credentials for example.</p>
     * 
     * <p>If no prefix value is specified no user property will be sent.</p>
     * 
     * <p>Usage example:</p>
     * <p><code>&lt;propertiesKeyPrefix&gt;UIA&lt;/propertiesKeyPrefix&gt;</code></p>
     * <p>And run it with:</p>
     * <p><code>&gt; mvn &lt;goal&gt; "-DUIAkey=value"</code></p>
     * <p>would become <code>"-e key value"</code> as it would be runned from adb</p>
     */
    @Parameter( property = "android.uiautomator.propertiesKeyPrefix" )
    private String uiautomatorPropertiesKeyPrefix;

    @PullParameter( required = false, defaultValueGetterMethod = "getPropertiesKeyPrefix" )
    private String parsedPropertiesKeyPrefix;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ConfigHandler configHandler = new ConfigHandler( this, this.session, this.execution );
        configHandler.parseConfiguration();

        if ( isEnableIntegrationTest() )
        {
            playTests();
        }
    }

    /**
     * Whether or not tests are enabled.
     * 
     * @return a boolean indicating whether or not tests are enabled.
     */
    protected boolean isEnableIntegrationTest()
    {
        return !parsedSkip && !mavenTestSkip && !mavenSkipTests;
    }

    /**
     * Whether or not test failures should be ignored.
     * 
     * @return a boolean indicating whether or not test failures should be ignored.
     */
    protected boolean isIgnoreTestFailures()
    {
        return mavenIgnoreTestFailure || mavenTestFailureIgnore;
    }

    /**
     * Actually plays tests.
     * 
     * @throws MojoExecutionException
     *             if at least a test threw an exception and isIgnoreTestFailures is false..
     * @throws MojoFailureException
     *             if at least a test failed and isIgnoreTestFailures is false.
     */
    protected void playTests() throws MojoExecutionException, MojoFailureException
    {

        getLog().debug( "Parsed values for Android UI UIAutomator invocation: " );
        getLog().debug( "jarFile:" + parsedJarFile );
        String testClassOrMethodString = buildSpaceSeparatedString( parsedTestClassOrMethods );
        getLog().debug( "testClassOrMethod:" + testClassOrMethodString );
        getLog().debug( "createReport:" + parsedCreateReport );

        DeviceCallback instrumentationTestExecutor = new DeviceCallback()
        {
            @Override
            public void doWithDevice( final IDevice device ) throws MojoExecutionException, MojoFailureException
            {
                String deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );

                UIAutomatorRemoteAndroidTestRunner automatorRemoteAndroidTestRunner //
                = new UIAutomatorRemoteAndroidTestRunner( parsedJarFile, device );

                automatorRemoteAndroidTestRunner.setRunName( "ui uiautomator tests" );
                automatorRemoteAndroidTestRunner.setDebug( uiautomatorDebug );
                automatorRemoteAndroidTestRunner.setTestClassOrMethods( parsedTestClassOrMethods );
                automatorRemoteAndroidTestRunner.setNoHup( parsedNoHup );
                automatorRemoteAndroidTestRunner.setUserProperties( session.getUserProperties(), 
                        parsedPropertiesKeyPrefix );
                
                if ( parsedUseDump )
                {
                    automatorRemoteAndroidTestRunner.setDumpFilePath( parsedDumpFilePath );
                }

                getLog().info( deviceLogLinePrefix + "Running ui uiautomator tests in" + parsedJarFile );
                try
                {
                    AndroidTestRunListener testRunListener = new AndroidTestRunListener( project, device, getLog(),
                            parsedCreateReport, parsedTakeScreenshotOnFailure, parsedScreenshotsPathOnDevice,
                            parsedReportSuffix, targetDirectory );
                    automatorRemoteAndroidTestRunner.run( testRunListener );
                    if ( testRunListener.hasFailuresOrErrors() && !isIgnoreTestFailures() )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix + "Tests failed on device." );
                    }
                    if ( testRunListener.testRunFailed() )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix + "Test run failed to complete: "
                                + testRunListener.getTestRunFailureCause() );
                    }
                    if ( testRunListener.threwException() && !isIgnoreTestFailures() )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix + testRunListener.getExceptionMessages() );
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

    private String getJarFile()
    {
        if ( parsedJarFile == null )
        {
            File jarFilePath = new File( targetDirectory + File.separator
                    + finalName + ".jar" );
            return jarFilePath.getName();
        }
        return parsedJarFile;
    }

    private String[] getTestClassOrMethods()
    {
        // null if not overriden by configuration
        return parsedTestClassOrMethods;
    }

    private String getReportSuffix()
    {
        return parsedReportSuffix;
    }
    
    private String getPropertiesKeyPrefix()
    {
        return parsedPropertiesKeyPrefix;
    }

    /**
     * Helper method to build a comma separated string from a list. Blank strings are filtered out
     * 
     * @param lines
     *            A list of strings
     * @return Comma separated String from given list
     */
    protected static String buildSpaceSeparatedString( String[] lines )
    {
        if ( lines == null || lines.length == 0 )
        {
            return null;
        }

        return StringUtils.join( lines, " " );
    }
}
