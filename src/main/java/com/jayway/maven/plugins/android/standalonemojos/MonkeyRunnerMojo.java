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
package com.jayway.maven.plugins.android.standalonemojos;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.AndroidTestRunListener;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.DeviceCallback;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.MonkeyRunner;
import com.jayway.maven.plugins.android.configuration.Program;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.interpolation.os.Os;
import org.codehaus.plexus.util.cli.shell.BourneShell;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Can execute monkey runner programs.<br/>
 * Implements parsing parameters from pom or command line arguments and sets useful defaults as well. This goal will
 * invoke monkey runner scripts. If the application crashes during the exercise, this goal can fail the build. <br />
 * A typical usage of this goal can be found at <a
 * href="https://github.com/stephanenicolas/Quality-Tools-for-Android">Quality tools for Android project</a>.
 * 
 * @see <a href="http://developer.android.com/tools/help/monkey.html">Monkey docs by Google</a>
 * @see <a href="http://stackoverflow.com/q/3968064/693752">Stack Over Flow thread for parsing monkey output.</a>
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
@SuppressWarnings( "unused" )
@Mojo( name = "monkeyrunner" )
public class MonkeyRunnerMojo extends AbstractAndroidMojo
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
     * The configuration for the monkey runner goal.
     * 
     * <pre>
     * &lt;monkeyrunner&gt;
     *   &lt;skip&gt;false&lt;/skip&gt;
     * &lt;/monkeyrunner&gt;
     * </pre>
     * 
     * Full configuration can use these parameters.
     * 
     * <pre>
     *  &lt;monkeyrunner&gt;
     *    &lt;skip&gt;false&lt;/skip&gt;
     *    &lt;createReport&gt;true&lt;/createReport&gt;
     *  &lt;/monkeyrunner&gt;
     * </pre>
     * 
     * Alternatively to the plugin configuration values can also be configured as properties on the command line as
     * android.lint.* or in pom or settings file as properties like lint*.
     */
    @Parameter
    @ConfigPojo
    private MonkeyRunner monkeyrunner;

    /**
     * Enables or disables monkey runner test goal. If <code>true</code> it will be skipped; if <code>false</code>, it
     * will be run. Defaults to true.
     */
    @Parameter( property = "android.monkeyrunner.skip" )
    private Boolean monkeyRunnerSkip;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedSkip;

    /**
     * (Optional) Specifies a .jar file containing a plugin for monkeyrunner. To learn more about monkeyrunner plugins,
     * see <a href="http://developer.android.com/tools/help/monkeyrunner_concepts.html#Plugins">Extending monkeyrunner
     * with plugins</a>. You can add as many plugins as you want.
     * 
     * Defaults to no plugins.
     */
    @Parameter( property = "android.monkeyrunner.plugins" )
    private String[] monkeyPlugins;

    @PullParameter( defaultValueGetterMethod = "getPlugins" )
    private String[] parsedPlugins;

    /**
     * Runs the contents of the file as a Python program.
     * 
     * <pre>
     * &lt;programs&gt;
     *   &lt;program&gt;
     *     &lt;filename&gt;foo.py&lt;/filename&gt;
     *     &lt;options&gt;bar&lt;/options&gt;
     *   &lt;program&gt;
     *   &lt;program&gt;
     *     &lt;filename&gt;foo2.py&lt;/filename&gt;
     *   &lt;program&gt;
     *   [..]
     * &lt;/programs&gt;
     * </pre>
     */
    @Parameter
    @PullParameter( required = false, defaultValueGetterMethod = "getPrograms" )
    private List< Program > parsedPrograms;

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
     *
     */
    @Parameter( property = "android.monkeyrunner.createReport" )
    private Boolean monkeyCreateReport;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedCreateReport;

    /**
     * Decides whether or not to inject device serial number as a parameter to each monkey runner script. The parameter
     * will be the first parameter passed to the script. This parameter allows to support monkey runner tests on
     * multiple devices. In that case, monkey runner scripts have to be modified to take the new parameter into account.
     * Follow that <a href="http://stackoverflow.com/a/13460438/693752">thread on stack over flow to learn more about
     * it</a>.
     */
    @Parameter( property = "android.monkeyrunner.injectDeviceSerialNumberIntoScript" )
    private Boolean monkeyInjectDeviceSerialNumberIntoScript;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedInjectDeviceSerialNumberIntoScript;

    private long elapsedTime;

    private ITestRunListener[] mTestListeners;

    private Map< String, String > runMetrics;

    private String mRunName;

    private int eventCount;

    private TestIdentifier mCurrentTestIndentifier;

    private MonkeyRunnerErrorListener errorListener;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ConfigHandler configHandler = new ConfigHandler( this, this.session, this.execution );
        configHandler.parseConfiguration();

        doWithDevices( new DeviceCallback()
        {
            @Override
            public void doWithDevice( IDevice device ) throws MojoExecutionException, MojoFailureException
            {
                AndroidTestRunListener testRunListener = new AndroidTestRunListener( project, device, getLog(),
                        parsedCreateReport, false, "", "", targetDirectory );
                if ( isEnableIntegrationTest() )
                {
                    run( device, testRunListener );
                }
            }
        } );
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
     * @param device
     *            the device on which tests are going to be executed.
     * @param iTestRunListeners
     *            test run listeners.
     * @throws MojoExecutionException
     *             if exercising app threw an exception and isIgnoreTestFailures is false..
     * @throws MojoFailureException
     *             if exercising app failed and isIgnoreTestFailures is false.
     */
    protected void run( IDevice device, ITestRunListener... iTestRunListeners ) throws MojoExecutionException,
            MojoFailureException
    {

        this.mTestListeners = iTestRunListeners;

        getLog().debug( "Parsed values for Android Monkey Runner invocation: " );

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        if ( !Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            executor.setCustomShell( new CustomBourneShell() );
        }
        executor.setLogger( this.getLog() );

        String command = getAndroidSdk().getMonkeyRunnerPath();

        List< String > pluginParameters = new ArrayList< String >();

        if ( parsedPlugins != null && parsedPlugins.length != 0 )
        {
            for ( String plugin : parsedPlugins )
            {
                String pluginFilePath = new File( project.getBasedir(), plugin ).getAbsolutePath();
                pluginParameters.add( "-plugin " + pluginFilePath );
            }
        }

        if ( parsedPrograms != null && !parsedPrograms.isEmpty() )
        {
            handleTestRunStarted();
            errorListener = new MonkeyRunnerErrorListener();
            executor.setErrorListener( errorListener );

            for ( Program program : parsedPrograms )
            {
                List< String > parameters = new ArrayList< String >( pluginParameters );

                String programFileName = new File( project.getBasedir(), program.getFilename() ).getAbsolutePath();
                parameters.add( programFileName );
                String testName = programFileName;
                if ( testName.contains( "/" ) )
                {
                    testName.substring( testName.indexOf( '/' ) + 1 );
                }
                mCurrentTestIndentifier = new TestIdentifier( "MonkeyTest ", testName );

                String programOptions = program.getOptions();
                if ( parsedInjectDeviceSerialNumberIntoScript != null && parsedInjectDeviceSerialNumberIntoScript )
                {
                    parameters.add( device.getSerialNumber() );
                }
                if ( programOptions != null && !StringUtils.isEmpty( programOptions ) )
                {
                    parameters.add( programOptions );
                }

                try
                {
                    getLog().info( "Running command: " + command );
                    getLog().info( "with parameters: " + parameters );
                    handleTestStarted();
                    executor.setCaptureStdOut( true );
                    executor.executeCommand( command, parameters, true );
                    handleTestEnded();
                }
                catch ( ExecutionException e )
                {
                    getLog().info( "Monkey runner produced errors" );
                    handleTestRunFailed( e.getMessage() );

                    if ( !isIgnoreTestFailures() )
                    {
                        getLog().info( "Project is configured to fail on error." );
                        getLog().info(
                                "Inspect monkey runner reports or re-run with -X to see monkey runner errors in log" );
                        getLog().info( "Failing build as configured. Ignore following error message." );
                        if ( errorListener.hasError )
                        {
                            getLog().info( "Stack trace is:" );
                            getLog().info( errorListener.getStackTrace() );
                        }
                        throw new MojoExecutionException( "", e );
                    }
                }

                if ( errorListener.hasError() )
                {
                    handleCrash();
                }
            }
            handleTestRunEnded();
        }

        getLog().info( "Monkey runner test runs completed successfully." );
    }

    private void handleTestRunStarted()
    {
        runMetrics = new HashMap< String, String >();
        elapsedTime = System.currentTimeMillis();
        for ( ITestRunListener listener : mTestListeners )
        {
            listener.testRunStarted( mRunName, eventCount );
        }
    }

    private void handleTestRunFailed( String error )
    {
        for ( ITestRunListener listener : mTestListeners )
        {
            listener.testRunFailed( error );
        }
    }

    private void handleTestRunEnded()
    {
        elapsedTime = System.currentTimeMillis() - elapsedTime;

        for ( ITestRunListener listener : mTestListeners )
        {
            listener.testRunEnded( elapsedTime, runMetrics );
        }
    }

    private void handleTestStarted()
    {
        System.out.println( "TEST START " + mTestListeners.length );
        for ( ITestRunListener listener : mTestListeners )
        {
            listener.testStarted( mCurrentTestIndentifier );
        }
    }

    private void handleTestEnded()
    {
        if ( mCurrentTestIndentifier != null )
        {
            for ( ITestRunListener listener : mTestListeners )
            {
                listener.testEnded( mCurrentTestIndentifier, new HashMap< String, String >() );
            }
            mCurrentTestIndentifier = null;
        }
    }

    private void handleCrash()
    {

        String trace = errorListener.getStackTrace();

        for ( ITestRunListener listener : mTestListeners )
        {
            listener.testFailed( mCurrentTestIndentifier, trace );
        }
        mCurrentTestIndentifier = null;

    }

    private final class MonkeyRunnerErrorListener implements CommandExecutor.ErrorListener
    {
        private StringBuilder stackTraceBuilder = new StringBuilder();
        private boolean hasError = false;

        @Override
        public boolean isError( String error )
        {

            // Unconditionally ignore *All* build warning if configured to
            if ( isIgnoreTestFailures() )
            {
                return false;
            }

            if ( hasError )
            {
                stackTraceBuilder.append( error ).append( '\n' );
            }

            final Pattern pattern = Pattern.compile( ".*error.*|.*exception.*", Pattern.CASE_INSENSITIVE );
            final Matcher matcher = pattern.matcher( error );

            // If the the reg.exp actually matches, we can safely say this is not an error
            // since in theory the user told us so
            if ( matcher.matches() )
            {
                hasError = true;
                stackTraceBuilder.append( error ).append( '\n' );
                return true;
            }

            // Otherwise, it is just another error
            return false;
        }

        public String getStackTrace()
        {
            if ( hasError )
            {
                return stackTraceBuilder.toString();
            }
            else
            {
                return null;
            }
        }

        public boolean hasError()
        {
            return hasError;
        }
    }

    /**
     * @return default plugins.
     */
    public String[] getPlugins()
    {
        return parsedPlugins;
    }

    private static final class CustomBourneShell extends BourneShell
    {
        @Override
        public List< String > getShellArgsList()
        {
            List< String > shellArgs = new ArrayList< String >();
            List< String > existingShellArgs = super.getShellArgsList();

            if ( existingShellArgs != null && !existingShellArgs.isEmpty() )
            {
                shellArgs.addAll( existingShellArgs );
            }

            return shellArgs;
        }

        @Override
        public String[] getShellArgs()
        {
            String[] shellArgs = super.getShellArgs();
            if ( shellArgs == null )
            {
                shellArgs = new String[ 0 ];
            }

            return shellArgs;
        }

    }

    public List< Program > getPrograms()
    {
        // return null if not set
        return parsedPrograms;
    }
}
