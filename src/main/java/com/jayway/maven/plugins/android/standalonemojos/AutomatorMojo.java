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

import static com.android.ddmlib.testrunner.ITestRunListener.TestFailure.ERROR;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Map;

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.ddmlib.testrunner.UIAutomatorRemoteAndroidTestRunner;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.DeviceCallback;
import com.jayway.maven.plugins.android.ScreenshotServiceWrapper;
import com.jayway.maven.plugins.android.common.DeviceHelper;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.Automator;

/**
 * Can execute tests using ui automator.<br/>
 * Called automatically when the lifecycle reaches phase <code>install</code>.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * @goal automator
 * @requiresProject false
 */
@SuppressWarnings( "unused" )
public class AutomatorMojo extends AbstractAndroidMojo
{

    /**
     * @parameter
     */
    @ConfigPojo
    private Automator automator;

    /**
     * Enables or disables ui automator test goal. If <code>true</code> it will be skipped; if <code>false</code>, it
     * will be run.
     * 
     * @parameter expression="${android.automator.skip}"
     */
    private Boolean automatorSkip;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedSkip;

    /**
     * Jar file that will be run during ui automator tests.
     * 
     * @parameter expression="${android.automator.jarFile}"
     */
    private String automatorJarFile;

    @PullParameter( defaultValueGetterMethod = "getJarFile" )
    private String parsedJarFile;

    /**
     * Test class or methods to execute during ui automator tests. Each class or method must be fully qualified with the
     * package name, in one of these formats:
     * <ul>
     * <li>package_name.class_name
     * <li>package_name.class_name#method_name
     * </ul>
     * 
     * @parameter expression="${android.automator.testClassOrMethod}"
     * 
     */
    private String automatorTestClassOrMethods;

    @PullParameter( required = false, defaultValue = "getTestClassOrMethods" )
    private String[] parsedTestClassOrMethods;

    /**
     * Decides whether to run the test to completion on the device even if its parent process is terminated (for
     * example, if the device is disconnected).
     * 
     * @parameter expression="${android.automator.noHup}"
     * 
     */
    private Boolean automatorNoHup;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedNoHup;

    /**
     * Decides whether to wait for debugger to connect before starting.
     * 
     * @parameter expression="${android.automator.debug}"
     * 
     */
    private Boolean automatorDebug = false;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedDebug;

    /**
     * Decides whether to use a dump file or not.
     * 
     * @parameter expression="${android.automator.useDump}"
     * 
     */
    private Boolean automatorUseDump;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedUseDump;

    /**
     * Generate an XML file with a dump of the current UI hierarchy. If a filepath is not specified, by default, the
     * generated dump file is stored on the device in this location /storage/sdcard0/window_dump.xml.
     * 
     * @parameter expression="${android.automator.dumpFilePath}"
     * 
     */
    private String automatorDumpFilePath;

    @PullParameter( required = false, defaultValue = "/storage/sdcard0/window_dump.xml" )
    private String parsedDumpFilePath;

    /**
     * 
     * @parameter expression="${android.automator.createReport}"
     * 
     */
    private Boolean automatorCreateReport;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedCreateReport;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ConfigHandler configHandler = new ConfigHandler( this );
        configHandler.parseConfiguration();

        instrument();
    }

    protected void instrument() throws MojoExecutionException, MojoFailureException
    {

        getLog().debug( "Parsed values for Android UI Automator invocation: " );
        getLog().debug( "automator:" + automator );
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

                automatorRemoteAndroidTestRunner.setRunName( "ui automator tests" );
                automatorRemoteAndroidTestRunner.setDebug( automatorDebug );
                automatorRemoteAndroidTestRunner
                        .setTestClassOrMethodsSpaceSeparated( buildSpaceSeparatedString( parsedTestClassOrMethods ) );
                automatorRemoteAndroidTestRunner.setNoHup( parsedNoHup );

                if ( parsedUseDump )
                {
                    automatorRemoteAndroidTestRunner.setDumpFilePath( parsedDumpFilePath );
                }

                getLog().info( deviceLogLinePrefix + "Running ui automator tests in" + parsedJarFile );
                try
                {
                    AndroidTestRunListener testRunListener = new AndroidTestRunListener( project, device );
                    automatorRemoteAndroidTestRunner.run( testRunListener );
                    if ( testRunListener.hasFailuresOrErrors() )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix + "Tests failed on device." );
                    }
                    if ( testRunListener.testRunFailed() )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix + "Test run failed to complete: "
                                + testRunListener.getTestRunFailureCause() );
                    }
                    if ( testRunListener.threwException() )
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

    private void parseConfiguration()
    {
        // we got config in pom ... lets use it,
        if ( automator != null )
        {
            if ( automator.isSkip() != null )
            {
                parsedSkip = automator.isSkip();
            }
            else
            {
                parsedSkip = automatorSkip;
            }
            if ( !StringUtils.isEmpty( automator.getJarFile() ) )
            {
                parsedJarFile = automator.getJarFile();
            }
            else
            {
                parsedJarFile = automatorJarFile;
            }
            if ( automator.getTestClassOrMethods() != null && automator.getTestClassOrMethods().length != 0 )
            {
                parsedTestClassOrMethods = automator.getTestClassOrMethods();
            }
            else
            {
                parsedTestClassOrMethods = automatorTestClassOrMethods.split( " " );
            }

        }
        // no pom, we take properties
        else
        {
            parsedSkip = automatorSkip;
            parsedJarFile = automatorJarFile;
            parsedTestClassOrMethods = automatorTestClassOrMethods.split( " " );
            parsedNoHup = automatorNoHup;
            parsedDebug = automatorDebug;
            parsedUseDump = automatorUseDump;
            parsedDumpFilePath = automatorDumpFilePath;
        }
    }

    private String getJarFile()
    {
        if ( parsedJarFile == null )
        {
            File jarFilePath = new File( project.getBuild().getDirectory() + File.separator
                    + project.getBuild().getFinalName() + ".jar" );
            return jarFilePath.getName();
        }
        return parsedJarFile;
    }

    private String[] getTestClassOrMethods()
    {
        // null if not overriden by configuration
        return parsedTestClassOrMethods;
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

    /**
     * AndroidTestRunListener produces a nice output for the log for the test run as well as an xml file compatible with
     * the junit xml report file format understood by many tools.
     * <p/>
     * It will do so for each device/emulator the tests run on.
     */
    public class AndroidTestRunListener implements ITestRunListener
    {
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

        public AndroidTestRunListener( MavenProject project, IDevice device )
        {
            this.project = project;
            this.device = device;
            this.deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );
        }

        @Override
        public void testRunStarted( String runName, int testCount )
        {
            this.testCount = testCount;
            getLog().info( deviceLogLinePrefix + INDENT + "Run started: " + runName + ", " + testCount + " tests:" );

            // steff
            // TODO

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

            // steff
            // TODO
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
        public void testFailed( TestFailure status, TestIdentifier testIdentifier, String trace )
        {
            if ( status == ERROR )
            {
                ++testErrorCount;
            }
            else
            {
                ++testFailureCount;
            }
            getLog().info( deviceLogLinePrefix + INDENT + INDENT + status.name() + ":" + testIdentifier.toString() );
            getLog().info( deviceLogLinePrefix + INDENT + INDENT + trace );

            // steff
            // TODO
            if ( parsedCreateReport )
            {
                Node errorFailureNode;
                NamedNodeMap errorfailureAttributes;
                if ( status == ERROR )
                {
                    errorFailureNode = junitReport.createElement( TAG_ERROR );
                    errorfailureAttributes = errorFailureNode.getAttributes();
                }
                else
                {
                    errorFailureNode = junitReport.createElement( TAG_FAILURE );
                    errorfailureAttributes = errorFailureNode.getAttributes();
                }
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
        public void testEnded( TestIdentifier testIdentifier, Map< String, String > testMetrics )
        {
            getLog().info(
                    deviceLogLinePrefix
                            + String.format( "%1$s%1$sEnd [%2$d/%3$d]: %4$s", INDENT, testRunCount, testCount,
                                    testIdentifier.toString() ) );
            logMetrics( testMetrics );

            // steff
            // TODO
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
                            + testFailureCount + ",  Errors: " + testErrorCount );

            // steff
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
                Attr timeAttr = junitReport.createAttribute( ATTR_TESTSUITE_TIME );
                timeAttr.setValue( timeFormatter.format( elapsedTime / 1000.0 ) );
                testSuiteAttributes.setNamedItem( timeAttr );
                Attr timeStampAttr = junitReport.createAttribute( ATTR_TESTSUITE_TIMESTAMP );
                timeStampAttr.setValue( new Date().toString() );
                testSuiteAttributes.setNamedItem( timeStampAttr );
            }

            logMetrics( runMetrics );

            // steff
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
                String directory = new StringBuilder().append( project.getBuild().getDirectory() )
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
}
