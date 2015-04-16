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

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.MonkeyTestRunner;
import com.simpligility.maven.plugins.android.AbstractAndroidMojo;
import com.simpligility.maven.plugins.android.AndroidTestRunListener;
import com.simpligility.maven.plugins.android.DeviceCallback;
import com.simpligility.maven.plugins.android.common.DeviceHelper;
import com.simpligility.maven.plugins.android.config.ConfigHandler;
import com.simpligility.maven.plugins.android.config.ConfigPojo;
import com.simpligility.maven.plugins.android.config.PullParameter;
import com.simpligility.maven.plugins.android.configuration.Monkey;

/**
 * Can execute tests using UI/Application Exerciser Monkey.<br/>
 * Implements parsing parameters from pom or command line arguments and sets useful defaults as well. This goal will
 * invoke Android Monkey exerciser. If the application crashes during the exercise, this goal can fail the build. <br />
 * A typical usage of this goal can be found at <a
 * href="https://github.com/stephanenicolas/Quality-Tools-for-Android">Quality tools for Android project</a>.
 * 
 * @see <a href="http://developer.android.com/tools/help/monkey.html">Monkey docs by Google</a>
 * @see <a href="http://stackoverflow.com/q/3968064/693752">Stack Over Flow thread for parsing monkey output.</a>
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
@SuppressWarnings( "unused" )
@Mojo( name = "monkey" )
public class MonkeyMojo extends AbstractAndroidMojo
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
     * honor it too.
     */
    @Parameter( property = "maven.test.failure.ignore", defaultValue = "false", readonly = true )
    private boolean mavenTestFailureIgnore;

    /**
     * -Dmaven.test.failure.ignore is commonly used with Maven to prevent failure of build when (some) tests fail. We
     * honor it too.
     */
    @Parameter( property = "testFailureIgnore", defaultValue = "false", readonly = true )
    private boolean mavenIgnoreTestFailure;

    /**
     * The configuration for the ui automator goal. As soon as a lint goal is invoked the command will be executed
     * unless the skip parameter is set. A minimal configuration that will run lint and produce a XML report in
     * ${project.build.directory}/lint/lint-results.xml is
     * 
     * <pre>
     * &lt;monkey&gt;
     *   &lt;skip&gt;false&lt;/skip&gt;
     * &lt;/monkey&gt;
     * </pre>
     * 
     * Full configuration can use these parameters.
     * 
     * <pre>
     *  &lt;monkey&gt;
     *    &lt;skip&gt;false&lt;/skip&gt;
     *    &lt;eventCount&gt;5000&lt;/eventCount&gt;
     *    &lt;seed&gt;123456&lt;/seed&gt;
     *    &lt;throttle&gt;10&lt;/throttle&gt;
     *    &lt;percentTouch&gt;10&lt;/percentTouch&gt;
     *    &lt;percentMotion&gt;10&lt;/percentMotion&gt;
     *    &lt;percentTrackball&gt;10&lt;/percentTrackball&gt;
     *    &lt;percentNav&gt;10&lt;/percentNav&gt;
     *    &lt;percentMajorNav&gt;10&lt;/percentMajorNav&gt;
     *    &lt;percentSyskeys&gt;10&lt;/percentSyskeys&gt;
     *    &lt;percentAppswitch&gt;10&lt;/percentAppswitch&gt;
     *    &lt;percentAnyevent&gt;10&lt;/percentAnyevent&gt;
     *    &lt;packages&gt;
     *        &lt;package&gt;com.foo&lt;/package&gt;
     *        &lt;package&gt;com.bar&lt;/package&gt;
     *    &lt;/packages&gt;
     *    &lt;categories&gt;
     *        &lt;category&gt;foo&lt;/category&gt;
     *        &lt;category&gt;bar&lt;/category&gt;
     *    &lt;/categories&gt;
     *    &lt;debugNoEvents&gt;true&lt;/debugNoEvents&gt;
     *    &lt;hprof&gt;true&lt;/hprof&gt;
     *    &lt;ignoreCrashes&gt;true&lt;/ignoreCrashes&gt;
     *    &lt;ignoreTimeouts&gt;true&lt;/ignoreTimeouts&gt;
     *    &lt;ignoreSecurityExceptions&gt;true&lt;/ignoreSecurityExceptions&gt;
     *    &lt;killProcessAfterError&gt;true&lt;/killProcessAfterError&gt;
     *    &lt;monitorNativeCrashes&gt;true&lt;/monitorNativeCrashes&gt;
     *    &lt;createReport&gt;true&lt;/createReport&gt;
     *  &lt;/monkey&gt;
     * </pre>
     * 
     * Alternatively to the plugin configuration values can also be configured as properties on the command line as
     * android.lint.* or in pom or settings file as properties like lint*.
     */
    @Parameter
    @ConfigPojo
    private Monkey monkey;

    /**
     * Enables or disables monkey test goal. If <code>true</code> it will be skipped; if <code>false</code>, it will be
     * run. Defaults to true.
     */
    @Parameter( property = "android.monkey.skip" )
    private Boolean monkeySkip;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedSkip;

    /**
     * Number of generated events. Defaults to 1000.
     */
    @Parameter( property = "android.monkey.eventCount" )
    private Integer monkeyEventCount;

    @PullParameter( required = true, defaultValue = "1000" )
    private Integer parsedEventCount;

    /**
     * Seed value for pseudo-random number generator. If you re-run the Monkey with the same seed value, it will
     * generate the same sequence of events.
     */
    @Parameter( property = "android.monkey.seed" )
    private Long monkeySeed;

    @PullParameter( required = false, defaultValueGetterMethod = "getSeed" )
    private Long parsedSeed;

    /**
     * Inserts a fixed delay between events. You can use this option to slow down the Monkey. If not specified, there is
     * no delay and the events are generated as rapidly as possible.
     */
    @Parameter( property = "android.monkey.throttle" )
    private Long monkeyThrottle;

    @PullParameter( required = false, defaultValueGetterMethod = "getThrottle" )
    private Long parsedThrottle;

    /**
     * Adjust percentage of touch events. (Touch events are a down-up event in a single place on the screen.)
     */
    @Parameter( property = "android.monkey.percentTouch" )
    private Integer monkeyPercentTouch;

    @PullParameter( required = false, defaultValueGetterMethod = "getPercentTouch" )
    private Integer parsedPercentTouch;

    /**
     * Adjust percentage of motion events. (Motion events consist of a down event somewhere on the screen, a series of
     * pseudo-random movements, and an up event.)
     */
    @Parameter( property = "android.monkey.percentMotion" )
    private Integer monkeyPercentMotion;

    @PullParameter( required = false, defaultValueGetterMethod = "getPercentMotion" )
    private Integer parsedPercentMotion;

    /**
     * Adjust percentage of trackball events. (Trackball events consist of one or more random movements, sometimes
     * followed by a click.)
     */
    @Parameter( property = "android.monkey.percentTrackball" )
    private Integer monkeyPercentTrackball;

    @PullParameter( required = false, defaultValueGetterMethod = "getPercentTrackball" )
    private Integer parsedPercentTrackball;

    /**
     * Adjust percentage of "basic" navigation events. (Navigation events consist of up/down/left/right, as input from a
     * directional input device.)
     */
    @Parameter( property = "android.monkey.percentNav" )
    private Integer monkeyPercentNav;

    @PullParameter( required = false, defaultValueGetterMethod = "getPercentNav" )
    private Integer parsedPercentNav;

    /**
     * Adjust percentage of "major" navigation events. (These are navigation events that will typically cause actions
     * within your UI, such as the center button in a 5-way pad, the back key, or the menu key.)
     */
    @Parameter( property = "android.monkey.percentMajorNav" )
    private Integer monkeyPercentMajorNav;

    @PullParameter( required = false, defaultValueGetterMethod = "getPercentMajorNav" )
    private Integer parsedPercentMajorNav;

    /**
     * Adjust percentage of "system" key events. (These are keys that are generally reserved for use by the system, such
     * as Home, Back, Start Call, End Call, or Volume controls.) Defaults to null.
     */
    @Parameter( property = "android.monkey.percentSyskeys" )
    private Integer monkeyPercentSyskeys;

    @PullParameter( required = false, defaultValueGetterMethod = "getPercentSyskeys" )
    private Integer parsedPercentSyskeys;

    /**
     * Adjust percentage of activity launches. At random intervals, the Monkey will issue a startActivity() call, as a
     * way of maximizing coverage of all activities within your package.
     */
    @Parameter( property = "android.monkey.percentAppswitch" )
    private Integer monkeyPercentAppswitch;

    @PullParameter( required = false, defaultValueGetterMethod = "getPercentAppswitch" )
    private Integer parsedPercentAppswitch;

    /**
     * Adjust percentage of other types of events. This is a catch-all for all other types of events such as keypresses,
     * other less-used buttons on the device, and so forth.
     */
    @Parameter( property = "android.monkey.percentAnyevent" )
    private Integer monkeyPercentAnyEvent;

    @PullParameter( required = false, defaultValueGetterMethod = "getPercentAnyevent" )
    private Integer parsedPercentAnyevent;

    /**
     * If you specify one or more packages this way, the Monkey will only allow the system to visit activities within
     * those packages. If your application requires access to activities in other packages (e.g. to select a contact)
     * you'll need to specify those packages as well. If you don't specify any packages, the Monkey will allow the
     * system to launch activities in all packages.
     */
    @Parameter( property = "android.monkey.packages" )
    private String[] monkeyPackages;

    @PullParameter( required = false, defaultValueGetterMethod = "getPackages" )
    private String[] parsedPackages;

    /**
     * If you specify one or more categories this way, the Monkey will only allow the system to visit activities that
     * are listed with one of the specified categories. If you don't specify any categories, the Monkey will select
     * activities listed with the category Intent.CATEGORY_LAUNCHER or Intent.CATEGORY_MONKEY.
     */
    @Parameter( property = "android.monkey.categories" )
    private String[] monkeyCategories;

    @PullParameter( required = false, defaultValueGetterMethod = "getCategories" )
    private String[] parsedCategories;

    /**
     * When specified, the Monkey will perform the initial launch into a test activity, but will not generate any
     * further events. For best results, combine with -v, one or more package constraints, and a non-zero throttle to
     * keep the Monkey running for 30 seconds or more. This provides an environment in which you can monitor package
     * transitions invoked by your application.
     */
    @Parameter( property = "android.monkey.debugNoEvents" )
    private Boolean monkeyDebugNoEvents;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedDebugNoEvents;

    /**
     * If set, this option will generate profiling reports immediately before and after the Monkey event sequence. This
     * will generate large (~5Mb) files in data/misc, so use with care. See Traceview for more information on trace
     * files.
     */
    @Parameter( property = "android.monkey.Hprof" )
    private Boolean monkeyHprof;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedHprof;

    /**
     * Normally, the Monkey will stop when the application crashes or experiences any type of unhandled exception. If
     * you specify this option, the Monkey will continue to send events to the system, until the count is completed.
     * Settings this option is different to setting testFailureIgnore or maven.test.failure.ignore to true, it will
     * impact monkey run but not the result of the maven build.
     */
    @Parameter( property = "android.monkey.ignoreCrashes" )
    private Boolean monkeyIgnoreCrashes;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedIgnoreCrashes;

    /**
     * Normally, the Monkey will stop when the application experiences any type of timeout error such as a
     * "Application Not Responding" dialog. If you specify this option, the Monkey will continue to send events to the
     * system, until the count is completed.
     *
     * Defaults to false.
     */
    @Parameter( property = "android.monkey.IgnoreTimeouts" )
    private Boolean monkeyIgnoreTimeouts;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedIgnoreTimeouts;

    /**
     * Normally, the Monkey will stop when the application experiences any type of permissions error, for example if it
     * attempts to launch an activity that requires certain permissions. If you specify this option, the Monkey will
     * continue to send events to the system, until the count is completed. *
     * 
     * Defaults to false.
     */
    @Parameter( property = "android.monkey.IgnoreSecurityExceptions" )
    private Boolean monkeyIgnoreSecurityExceptions;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedIgnoreSecurityExceptions;

    /**
     * Normally, when the Monkey stops due to an error, the application that failed will be left running. When this
     * option is set, it will signal the system to stop the process in which the error occurred. Note, under a normal
     * (successful) completion, the launched process(es) are not stopped, and the device is simply left in the last
     * state after the final event.
     * 
     * Defaults to false.
     */
    @Parameter( property = "android.monkey.KillProcessAfterError" )
    private Boolean monkeyKillProcessAfterError;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedKillProcessAfterError;

    /**
     * Watches for and reports crashes occurring in the Android system native code. If --kill-process-after-error is
     * set, the system will stop.
     * 
     * Defaults to false.
     */
    @Parameter( property = "android.monkey.MonitorNativeCrashes" )
    private Boolean monkeyMonitorNativeCrashes;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedMonitorNativeCrashes;

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
     * 
     * Defaults to false.
     */
    @Parameter( property = "android.monkey.createReport" )
    private Boolean monkeyCreateReport;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedCreateReport;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ConfigHandler configHandler = new ConfigHandler( this, this.session, this.execution );
        configHandler.parseConfiguration();

        if ( isEnableIntegrationTest() )
        {
            exerciseApp();
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
     *             if exercising app threw an exception and isIgnoreTestFailures is false..
     * @throws MojoFailureException
     *             if exercising app failed and isIgnoreTestFailures is false.
     */
    protected void exerciseApp() throws MojoExecutionException, MojoFailureException
    {

        getLog().debug( "Parsed values for Android Monkey invocation: " );
        getLog().debug( "seed:" + parsedSeed );

        DeviceCallback instrumentationTestExecutor = new DeviceCallback()
        {
            @Override
            public void doWithDevice( final IDevice device ) throws MojoExecutionException, MojoFailureException
            {
                String deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );

                MonkeyTestRunner monkeyTestRunner = new MonkeyTestRunner( parsedEventCount, device );

                monkeyTestRunner.setRunName( "ui monkey tests" );
                if ( parsedSeed != null )
                {
                    monkeyTestRunner.setSeed( parsedSeed );
                }
                if ( parsedPercentTouch != null )
                {
                    monkeyTestRunner.setPercentTouch( parsedPercentTouch );
                }
                if ( parsedPercentMotion != null )
                {
                    monkeyTestRunner.setPercentTouch( parsedPercentMotion );
                }
                if ( parsedPercentTrackball != null )
                {
                    monkeyTestRunner.setPercentTrackball( parsedPercentTrackball );
                }
                if ( parsedPercentNav != null )
                {
                    monkeyTestRunner.setPercentNav( parsedPercentNav );
                }
                if ( parsedPercentMajorNav != null )
                {
                    monkeyTestRunner.setPercentMajorNav( parsedPercentMajorNav );
                }
                if ( parsedPercentSyskeys != null )
                {
                    monkeyTestRunner.setPercentSyskeys( parsedPercentSyskeys );
                }
                if ( parsedPercentAppswitch != null )
                {
                    monkeyTestRunner.setPercentAppswitch( parsedPercentAppswitch );
                }
                if ( parsedPercentAnyevent != null )
                {
                    monkeyTestRunner.setPercentAnyEvent( parsedPercentAnyevent );
                }
                if ( parsedPackages != null )
                {
                    monkeyTestRunner.setPackages( parsedPackages );
                }
                if ( parsedCategories != null )
                {
                    monkeyTestRunner.setCategories( parsedCategories );
                }
                monkeyTestRunner.setDebugNoEvents( parsedDebugNoEvents );
                monkeyTestRunner.setHprof( parsedHprof );
                monkeyTestRunner.setIgnoreCrashes( parsedIgnoreCrashes );
                monkeyTestRunner.setIgnoreTimeouts( parsedIgnoreTimeouts );
                monkeyTestRunner.setIgnoreSecurityExceptions( parsedIgnoreSecurityExceptions );
                monkeyTestRunner.setKillProcessAfterError( parsedKillProcessAfterError );
                monkeyTestRunner.setMonitorNativeCrash( parsedMonitorNativeCrashes );

                getLog().info( deviceLogLinePrefix + "Running ui monkey tests" );
                try
                {
                    AndroidTestRunListener testRunListener = new AndroidTestRunListener( project, device, getLog(),
                            parsedCreateReport, false, "", "", targetDirectory );
                    monkeyTestRunner.run( testRunListener );
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

        doWithDevices( instrumentationTestExecutor );
    }

    /**
     * @return default seed.
     */
    // used via PullParameter annotation - do not remove
    private Long getSeed()
    {
        return parsedSeed;
    }

    /**
     * @return default throttle.
     */
    // used via PullParameter annotation - do not remove
    private Long getThrottle()
    {
        return parsedThrottle;
    }

    /**
     * @return default percentTouch.
     */
    // used via PullParameter annotation - do not remove
    private Integer getPercentTouch()
    {
        return parsedPercentTouch;
    }

    /**
     * @return default percentMotion.
     */
    // used via PullParameter annotation - do not remove
    private Integer getPercentMotion()
    {
        return parsedPercentMotion;
    }

    /**
     * @return default percentTrackball.
     */
    // used via PullParameter annotation - do not remove
    private Integer getPercentTrackball()
    {
        return parsedPercentTrackball;
    }

    /**
     * @return default percentNav.
     */
    // used via PullParameter annotation - do not remove
    private Integer getPercentNav()
    {
        return parsedPercentNav;
    }

    /**
     * @return default percentMajorNav.
     */
    // used via PullParameter annotation - do not remove
    private Integer getPercentMajorNav()
    {
        return parsedPercentMajorNav;
    }

    /**
     * @return default percentSyskeys.
     */
    // used via PullParameter annotation - do not remove
    private Integer getPercentSyskeys()
    {
        return parsedPercentSyskeys;
    }

    /**
     * @return default percentAppSwitch.
     */
    // used via PullParameter annotation - do not remove
    private Integer getPercentAppswitch()
    {
        return parsedPercentAppswitch;
    }

    /**
     * @return default percentAnyEvent.
     */
    // used via PullParameter annotation - do not remove
    private Integer getPercentAnyevent()
    {
        return parsedPercentAnyevent;
    }

    /**
     * @return default packages.
     */
    public String[] getPackages()
    {
        return parsedPackages;
    }

    /**
     * @return default categories.
     */
    public String[] getCategories()
    {
        return parsedCategories;
    }
}
