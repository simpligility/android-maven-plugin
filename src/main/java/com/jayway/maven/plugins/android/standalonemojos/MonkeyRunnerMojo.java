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

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.DeviceCallback;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.common.DeviceHelper;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.MonkeyRunner;
import com.jayway.maven.plugins.android.configuration.Program;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.os.Os;
import org.codehaus.plexus.util.cli.shell.BourneShell;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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
import java.util.Date;
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
                AndroidTestRunListener testRunListener = new AndroidTestRunListener( project, device );
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
     * AndroidTestRunListener produces a nice output for the log for the test run as well as an xml file compatible with
     * the junit xml report file format understood by many tools.
     * <p/>
     * It will do so for each device/emulator the tests run on.
     */
    public class AndroidTestRunListener implements ITestRunListener
    {
        private static final String SCREENSHOT_SUFFIX = "_screenshot.png";

        /**
         * the indent used in the log to group items that belong together visually *
         */
        private static final String INDENT = "  ";

        /**
         * Junit report schema documentation is sparse. Here are some hints
         * 
         * @see "http://mail-archives.apache.org/mod_mbox/ ant-dev/200902.mbox/%3
         *      Cdffc72020902241548l4316d645w2e98caf5f0aac770
         * @mail.gmail.com%3E"
         * @see "http://junitpdfreport.sourceforge.net/managedcontent/PdfTranslation"
         */
        private static final String TAG_TESTSUITES = "testsuites";

        private static final String TAG_TESTSUITE = "testsuite";
        private static final String ATTR_TESTSUITE_ERRORS = "errors";
        private static final String ATTR_TESTSUITE_FAILURES = "failures";
        private static final String ATTR_TESTSUITE_IGNORED = "ignored";
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

        /**
         * time format for the output of milliseconds in seconds in the xml file *
         */
        private final NumberFormat timeFormatter = new DecimalFormat( "#0.0000" );

        private int testCount = 0;
        private int testRunCount = 0;
        private int testIgnoredCount = 0;
        private int testFailureCount = 0;
        private int testErrorCount = 0;
        private String testRunFailureCause = null;

        private final MavenProject project;
        /**
         * the emulator or device we are running the tests on *
         */
        private final IDevice device;

        private final String deviceLogLinePrefix;

        // junit xml report related fields
        private Document junitReport;
        private Node testSuiteNode;

        /**
         * node for the current test case for junit report
         */
        private Node currentTestCaseNode;
        /**
         * start time of current test case in millis, reset with each test start
         */
        private long currentTestCaseStartTime;

        // we track if we have problems and then report upstream
        private boolean threwException = false;
        private final StringBuilder exceptionMessages = new StringBuilder();

        /**
         * Create a new test run listener.
         * 
         * @param project
         *            the test project.
         * @param device
         *            the device on which test is executed.
         */
        public AndroidTestRunListener( MavenProject project, IDevice device )
        {
            this.project = project;
            this.device = device;
            this.deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );
        }

        @Override
        public void testRunStarted( String runName, int tCount )
        {

            this.testCount = tCount;
            getLog().info( deviceLogLinePrefix + INDENT + "Run started: " + runName + ", " + testCount + " tests:" );

            if ( parsedCreateReport )
            {
                try
                {
                    DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
                    DocumentBuilder parser = null;
                    parser = fact.newDocumentBuilder();
                    junitReport = parser.newDocument();
                    Node testSuitesNode = junitReport.createElement( TAG_TESTSUITES );
                    junitReport.appendChild( testSuitesNode );
                    testSuiteNode = junitReport.createElement( TAG_TESTSUITE );
                    NamedNodeMap testSuiteAttributes = testSuiteNode.getAttributes();
                    Attr nameAttr = junitReport.createAttribute( ATTR_TESTSUITE_NAME );
                    nameAttr.setValue( runName );
                    testSuiteAttributes.setNamedItem( nameAttr );
                    Attr hostnameAttr = junitReport.createAttribute( ATTR_TESTSUITE_HOSTNAME );
                    hostnameAttr.setValue( DeviceHelper.getDescriptiveName( device ) );
                    testSuiteAttributes.setNamedItem( hostnameAttr );
                    Node propertiesNode = junitReport.createElement( TAG_PROPERTIES );
                    Node propertyNode;
                    NamedNodeMap propertyAttributes;
                    Attr propNameAttr;
                    Attr propValueAttr;
                    for ( Map.Entry< Object, Object > systemProperty : System.getProperties().entrySet() )
                    {
                        propertyNode = junitReport.createElement( TAG_PROPERTY );
                        propertyAttributes = propertyNode.getAttributes();
                        propNameAttr = junitReport.createAttribute( ATTR_PROPERTY_NAME );
                        propNameAttr.setValue( systemProperty.getKey().toString() );
                        propertyAttributes.setNamedItem( propNameAttr );
                        propValueAttr = junitReport.createAttribute( ATTR_PROPERTY_VALUE );
                        propValueAttr.setValue( systemProperty.getValue().toString() );
                        propertyAttributes.setNamedItem( propValueAttr );
                        propertiesNode.appendChild( propertyNode );
                    }
                    Map< String, String > deviceProperties = device.getProperties();
                    for ( Map.Entry< String, String > deviceProperty : deviceProperties.entrySet() )
                    {
                        propertyNode = junitReport.createElement( TAG_PROPERTY );
                        propertyAttributes = propertyNode.getAttributes();
                        propNameAttr = junitReport.createAttribute( ATTR_PROPERTY_NAME );
                        propNameAttr.setValue( deviceProperty.getKey() );
                        propertyAttributes.setNamedItem( propNameAttr );
                        propValueAttr = junitReport.createAttribute( ATTR_PROPERTY_VALUE );
                        propValueAttr.setValue( deviceProperty.getValue() );
                        propertyAttributes.setNamedItem( propValueAttr );
                        propertiesNode.appendChild( propertyNode );
                    }
                    testSuiteNode.appendChild( propertiesNode );
                    testSuitesNode.appendChild( testSuiteNode );
                }
                catch ( ParserConfigurationException e )
                {
                    threwException = true;
                    exceptionMessages.append( "Failed to create document" );
                    exceptionMessages.append( e.getMessage() );
                }
            }
        }

        @Override
        public void testStarted( TestIdentifier testIdentifier )
        {
            testRunCount++;
            getLog().info(
                    deviceLogLinePrefix
                            + String.format( "%1$s%1$sStart [%2$d/%3$d]: %4$s", INDENT, testRunCount, testCount,
                                    testIdentifier.toString() ) );

            if ( parsedCreateReport )
            { // reset start time for each test run
                currentTestCaseStartTime = new Date().getTime();
                currentTestCaseNode = junitReport.createElement( TAG_TESTCASE );
                NamedNodeMap testCaseAttributes = currentTestCaseNode.getAttributes();
                Attr classAttr = junitReport.createAttribute( ATTR_TESTCASE_CLASSNAME );
                classAttr.setValue( testIdentifier.getClassName() );
                testCaseAttributes.setNamedItem( classAttr );
                Attr methodAttr = junitReport.createAttribute( ATTR_TESTCASE_NAME );
                methodAttr.setValue( testIdentifier.getTestName() );
                testCaseAttributes.setNamedItem( methodAttr );
            }
        }

        @Override
        public void testIgnored( TestIdentifier testIdentifier )
        {
            ++testIgnoredCount;

            getLog().info( deviceLogLinePrefix + INDENT + INDENT + testIdentifier.toString() );
        }

        @Override
        public void testFailed( TestIdentifier testIdentifier, String trace )
        {
            ++testErrorCount;

            getLog().info( deviceLogLinePrefix + INDENT + INDENT + testIdentifier.toString() );
            getLog().info( deviceLogLinePrefix + INDENT + INDENT + trace );

            if ( parsedCreateReport )
            {
                Node errorFailureNode;
                NamedNodeMap errorfailureAttributes;

                errorFailureNode = junitReport.createElement( TAG_ERROR );
                errorfailureAttributes = errorFailureNode.getAttributes();

                errorFailureNode.setTextContent( trace );
                Attr msgAttr = junitReport.createAttribute( ATTR_MESSAGE );
                msgAttr.setValue( parseForMessage( trace ) );
                errorfailureAttributes.setNamedItem( msgAttr );
                Attr typeAttr = junitReport.createAttribute( ATTR_TYPE );
                typeAttr.setValue( parseForException( trace ) );
                errorfailureAttributes.setNamedItem( typeAttr );
                currentTestCaseNode.appendChild( errorFailureNode );
            }
        }

        @Override
        public void testAssumptionFailure( TestIdentifier testIdentifier, String trace )
        {

            ++testFailureCount;

            getLog().info( deviceLogLinePrefix + INDENT + INDENT + testIdentifier.toString() );
            getLog().info( deviceLogLinePrefix + INDENT + INDENT + trace );

            if ( parsedCreateReport )
            {
                Node errorFailureNode;
                NamedNodeMap errorfailureAttributes;

                errorFailureNode = junitReport.createElement( TAG_FAILURE );
                errorfailureAttributes = errorFailureNode.getAttributes();

                errorFailureNode.setTextContent( trace );
                Attr msgAttr = junitReport.createAttribute( ATTR_MESSAGE );
                msgAttr.setValue( parseForMessage( trace ) );
                errorfailureAttributes.setNamedItem( msgAttr );
                Attr typeAttr = junitReport.createAttribute( ATTR_TYPE );
                typeAttr.setValue( parseForException( trace ) );
                errorfailureAttributes.setNamedItem( typeAttr );
                currentTestCaseNode.appendChild( errorFailureNode );
            }
        }

        private void executeOnAdbShell( String command )
        {
            try
            {
                device.executeShellCommand( command, new IShellOutputReceiver()
                {
                    @Override
                    public boolean isCancelled()
                    {
                        return false;
                    }

                    @Override
                    public void flush()
                    {
                    }

                    @Override
                    public void addOutput( byte[] data, int offset, int length )
                    {
                    }
                } );
            }
            catch ( TimeoutException e )
            {
                getLog().error( e );
            }
            catch ( AdbCommandRejectedException e )
            {
                getLog().error( e );
            }
            catch ( ShellCommandUnresponsiveException e )
            {
                getLog().error( e );
            }
            catch ( IOException e )
            {
                getLog().error( e );
            }
        }

        @Override
        public void testEnded( TestIdentifier testIdentifier, Map< String, String > testMetrics )
        {
            getLog().info(
                    deviceLogLinePrefix
                            + String.format( "%1$s%1$sEnd [%2$d/%3$d]: %4$s", INDENT, testRunCount, testCount,
                                    testIdentifier.toString() ) );
            logMetrics( testMetrics );

            if ( parsedCreateReport )
            {
                testSuiteNode.appendChild( currentTestCaseNode );
                NamedNodeMap testCaseAttributes = currentTestCaseNode.getAttributes();
                Attr timeAttr = junitReport.createAttribute( ATTR_TESTCASE_TIME );
                long now = new Date().getTime();
                double seconds = ( now - currentTestCaseStartTime ) / 1000.0;
                timeAttr.setValue( timeFormatter.format( seconds ) );
                testCaseAttributes.setNamedItem( timeAttr );
            }
        }

        @Override
        public void testRunEnded( long elapsedTime, Map< String, String > runMetrics )
        {
            getLog().info( deviceLogLinePrefix + INDENT + "Run ended: " + elapsedTime + " ms" );
            if ( hasFailuresOrErrors() )
            {
                getLog().error( deviceLogLinePrefix + INDENT + "FAILURES!!!" );
            }
            getLog().info(
                    INDENT + "Tests run: " + testRunCount
                            + ( testRunCount < testCount ? " (of " + testCount + ")" : "" ) + ",  Failures: "
                            + testFailureCount + ",  Errors: " + testErrorCount
                            + ",  Ignored: " + testIgnoredCount );

            if ( parsedCreateReport )
            {
                NamedNodeMap testSuiteAttributes = testSuiteNode.getAttributes();
                Attr testCountAttr = junitReport.createAttribute( ATTR_TESTSUITE_TESTS );
                testCountAttr.setValue( Integer.toString( testCount ) );
                testSuiteAttributes.setNamedItem( testCountAttr );
                Attr testFailuresAttr = junitReport.createAttribute( ATTR_TESTSUITE_FAILURES );
                testFailuresAttr.setValue( Integer.toString( testFailureCount ) );
                testSuiteAttributes.setNamedItem( testFailuresAttr );
                Attr testErrorsAttr = junitReport.createAttribute( ATTR_TESTSUITE_ERRORS );
                testErrorsAttr.setValue( Integer.toString( testErrorCount ) );
                testSuiteAttributes.setNamedItem( testErrorsAttr );
                Attr testIgnoredAttr = junitReport.createAttribute( ATTR_TESTSUITE_IGNORED );
                testIgnoredAttr.setValue( Integer.toString( testIgnoredCount ) );
                testSuiteAttributes.setNamedItem( testIgnoredAttr );
                Attr timeAttr = junitReport.createAttribute( ATTR_TESTSUITE_TIME );
                timeAttr.setValue( timeFormatter.format( elapsedTime / 1000.0 ) );
                testSuiteAttributes.setNamedItem( timeAttr );
                Attr timeStampAttr = junitReport.createAttribute( ATTR_TESTSUITE_TIMESTAMP );
                timeStampAttr.setValue( new Date().toString() );
                testSuiteAttributes.setNamedItem( timeStampAttr );
            }

            logMetrics( runMetrics );

            if ( parsedCreateReport )
            {
                writeJunitReportToFile();
            }
        }

        @Override
        public void testRunFailed( String errorMessage )
        {
            testRunFailureCause = errorMessage;
            getLog().info( deviceLogLinePrefix + INDENT + "Run failed: " + errorMessage );
        }

        @Override
        public void testRunStopped( long elapsedTime )
        {
            getLog().info( deviceLogLinePrefix + INDENT + "Run stopped:" + elapsedTime );
        }

        /**
         * Parse a trace string for the message in it. Assumes that the message is located after ":" and before "\r\n".
         * 
         * @param trace
         * @return message or empty string
         */
        private String parseForMessage( String trace )
        {
            if ( StringUtils.isNotBlank( trace ) )
            {
                String newline = "\r\n";
                // if there is message like
                // junit.junit.framework.AssertionFailedError ... there is no
                // message
                int messageEnd = trace.indexOf( newline );
                boolean hasMessage = !trace.startsWith( "junit." ) && messageEnd > 0;
                if ( hasMessage )
                {
                    int messageStart = trace.indexOf( ":" ) + 2;
                    if ( messageStart > messageEnd )
                    {
                        messageEnd = trace.indexOf( newline + "at" );
                        // match start of stack trace "\r\nat org.junit....."
                        if ( messageStart > messageEnd )
                        {
                            // ':' wasn't found in message but in stack trace
                            messageStart = 0;
                        }
                    }
                    return trace.substring( messageStart, messageEnd );
                }
                else
                {
                    return StringUtils.EMPTY;
                }
            }
            else
            {
                return StringUtils.EMPTY;
            }
        }

        /**
         * Parse a trace string for the exception class. Assumes that it is the start of the trace and ends at the first
         * ":".
         * 
         * @param trace
         * @return Exception class as string or empty string
         */
        private String parseForException( String trace )
        {
            if ( StringUtils.isNotBlank( trace ) )
            {
                return trace.substring( 0, trace.indexOf( ":" ) );
            }
            else
            {
                return StringUtils.EMPTY;
            }
        }

        /**
         * Write the junit report xml file.
         */
        private void writeJunitReportToFile()
        {
            TransformerFactory xfactory = TransformerFactory.newInstance();
            Transformer xformer = null;
            try
            {
                xformer = xfactory.newTransformer();
            }
            catch ( TransformerConfigurationException e )
            {
                e.printStackTrace();
            }
            Source source = new DOMSource( junitReport );

            FileWriter writer = null;
            try
            {
                String directory = new StringBuilder().append( targetDirectory )
                        .append( "/surefire-reports" ).toString();

                FileUtils.forceMkdir( new File( directory ) );

                String fileName = new StringBuilder().append( directory ).append( "/TEST-" )
                        .append( DeviceHelper.getDescriptiveName( device ) ).append( ".xml" ).toString();
                File reportFile = new File( fileName );
                writer = new FileWriter( reportFile );
                Result result = new StreamResult( writer );

                xformer.transform( source, result );
                getLog().info( deviceLogLinePrefix + "Report file written to " + reportFile.getAbsolutePath() );
            }
            catch ( IOException e )
            {
                threwException = true;
                exceptionMessages.append( "Failed to write test report file" );
                exceptionMessages.append( e.getMessage() );
            }
            catch ( TransformerException e )
            {
                threwException = true;
                exceptionMessages.append( "Failed to transform document to write to test report file" );
                exceptionMessages.append( e.getMessage() );
            }
            finally
            {
                IOUtils.closeQuietly( writer );
            }
        }

        /**
         * Log all the metrics out in to key: value lines.
         * 
         * @param metrics
         */
        private void logMetrics( Map< String, String > metrics )
        {
            for ( Map.Entry< String, String > entry : metrics.entrySet() )
            {
                getLog().info( deviceLogLinePrefix + INDENT + INDENT + entry.getKey() + ": " + entry.getValue() );
            }
        }

        /**
         * @return if any failures or errors occurred in the test run.
         */
        public boolean hasFailuresOrErrors()
        {
            return testErrorCount > 0 || testFailureCount > 0;
        }

        /**
         * @return if the test run itself failed - a failure in the test infrastructure, not a test failure.
         */
        public boolean testRunFailed()
        {
            return testRunFailureCause != null;
        }

        /**
         * @return the cause of test failure if any.
         */
        public String getTestRunFailureCause()
        {
            return testRunFailureCause;
        }

        /**
         * @return if any exception was thrown during the test run on the build system (not the Android device or
         *         emulator)
         */
        public boolean threwException()
        {
            return threwException;
        }

        /**
         * @return all exception messages thrown during test execution on the test run time (not the Android device or
         *         emulator)
         */
        public String getExceptionMessages()
        {
            return exceptionMessages.toString();
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
