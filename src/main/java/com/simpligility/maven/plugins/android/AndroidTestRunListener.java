package com.simpligility.maven.plugins.android;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.simpligility.maven.plugins.android.common.DeviceHelper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
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
import java.util.Date;
import java.util.Map;

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
    private final Log log;
    private final Boolean createReport;
    private final Boolean takeScreenshotOnFailure;
    private final String screenshotsPathOnDevice;
    private final String reportSuffix;
    private final File targetDirectory;

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
    public AndroidTestRunListener( MavenProject project, IDevice device, Log log, Boolean createReport,
                                   Boolean takeScreenshotOnFailure, String screenshotsPathOnDevice,
                                   String reportSuffix, File targetDirectory )
    {
        this.project = project;
        this.device = device;
        this.deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );
        this.log = log;
        this.createReport = createReport;
        this.takeScreenshotOnFailure = takeScreenshotOnFailure;
        this.screenshotsPathOnDevice = screenshotsPathOnDevice;
        this.reportSuffix = reportSuffix;
        this.targetDirectory = targetDirectory;
    }

    public Log getLog()
    {
        return this.log;
    }

    @Override
    public void testRunStarted( String runName, int tCount )
    {
        if ( takeScreenshotOnFailure )
        {
            executeOnAdbShell( "rm -f " + screenshotsPathOnDevice + "/*screenshot.png" );
            executeOnAdbShell( "mkdir " + screenshotsPathOnDevice );
        }

        this.testCount = tCount;
        getLog().info( deviceLogLinePrefix + INDENT + "Run started: " + runName + ", " + testCount + " tests:" );

        if ( createReport )
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
    public void testIgnored( TestIdentifier testIdentifier )
    {
        ++testIgnoredCount;

        getLog().info( deviceLogLinePrefix + INDENT + INDENT + testIdentifier.toString() );

    }

    @Override
    public void testStarted( TestIdentifier testIdentifier )
    {
        testRunCount++;
        getLog().info(
                deviceLogLinePrefix
                        + String.format( "%1$s%1$sStart [%2$d/%3$d]: %4$s", INDENT, testRunCount, testCount,
                        testIdentifier.toString() ) );

        if ( createReport )
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
    public void testFailed( TestIdentifier testIdentifier, String trace )
    {
        if ( takeScreenshotOnFailure )
        {
            String suffix = "_error";
            String filepath = testIdentifier.getTestName() + suffix + SCREENSHOT_SUFFIX;

            executeOnAdbShell( "screencap -p " + screenshotsPathOnDevice + "/" + filepath );
            getLog().info( deviceLogLinePrefix + INDENT + INDENT + filepath + " saved." );
        }

        ++testErrorCount;

        getLog().info( deviceLogLinePrefix + INDENT + INDENT + testIdentifier.toString() );
        getLog().info( deviceLogLinePrefix + INDENT + INDENT + trace );

        if ( createReport )
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
        if ( takeScreenshotOnFailure )
        {
            String suffix = "_failure";
            String filepath = testIdentifier.getTestName() + suffix + SCREENSHOT_SUFFIX;

            executeOnAdbShell( "screencap -p " + screenshotsPathOnDevice + "/" + filepath );
            getLog().info( deviceLogLinePrefix + INDENT + INDENT + filepath + " saved." );
        }

        ++testFailureCount;

        getLog().info( deviceLogLinePrefix + INDENT + INDENT + testIdentifier.toString() );
        getLog().info( deviceLogLinePrefix + INDENT + INDENT + trace );

        if ( createReport )
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

        if ( createReport )
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

        if ( createReport )
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

        if ( createReport )
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

            StringBuilder sb = new StringBuilder();

            sb.append( directory ).append( "/TEST-" )
                    .append( DeviceHelper.getDescriptiveName( device ) );

            if ( StringUtils.isNotBlank( reportSuffix ) )
            {
                //Safety first
                sb.append( reportSuffix.replace( "/", "" ).replace( "\\", "" ) );
            }

            String fileName = sb.append( ".xml" ).toString();

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
