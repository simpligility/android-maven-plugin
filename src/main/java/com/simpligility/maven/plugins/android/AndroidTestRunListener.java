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
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.surefire.ObjectFactory;
import org.apache.maven.surefire.Testsuite;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
     * time format for the output of milliseconds in seconds in the xml file *
     */
    private final NumberFormat timeFormatter = new DecimalFormat( "#0.000" );

    private int testCount = 0;
    private int testRunCount = 0;
    private int testIgnoredCount = 0;
    private int testFailureCount = 0;
    private int testErrorCount = 0;
    private String testRunFailureCause = null;

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

    private final ObjectFactory objectFactory = new ObjectFactory();
    private Testsuite report;
    private Testsuite.Testcase currentTestCase;

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
     * @param device
     *            the device on which test is executed.
     */
    public AndroidTestRunListener( IDevice device, Log log, Boolean createReport,
                                   Boolean takeScreenshotOnFailure, String screenshotsPathOnDevice,
                                   String reportSuffix, File targetDirectory )
    {
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
            report = new Testsuite();
            report.setName( runName );
            final Testsuite.Properties props = new Testsuite.Properties();
            report.getProperties().add( props );
            for ( Map.Entry< Object, Object > systemProperty : System.getProperties().entrySet() )
            {
                final Testsuite.Properties.Property property = new Testsuite.Properties.Property();
                property.setName( systemProperty.getKey().toString() );
                property.setValue( systemProperty.getValue().toString() );
                props.getProperty().add( property );
            }
            Map< String, String > deviceProperties = device.getProperties();
            for ( Map.Entry< String, String > deviceProperty : deviceProperties.entrySet() )
            {
                final Testsuite.Properties.Property property = new Testsuite.Properties.Property();
                property.setName( deviceProperty.getKey() );
                property.setValue( deviceProperty.getValue() );
                props.getProperty().add( property );
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
            currentTestCaseStartTime = System.currentTimeMillis();
            currentTestCase = new Testsuite.Testcase();
            currentTestCase.setClassname( testIdentifier.getClassName() );
            currentTestCase.setName( testIdentifier.getTestName() );
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
            final Testsuite.Testcase.Error error = new Testsuite.Testcase.Error();
            error.setValue( trace );
            error.setMessage( parseForMessage( trace ) );
            error.setType( parseForException( trace ) );
            currentTestCase.setError( objectFactory.createTestsuiteTestcaseError( error ) );
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
            final Testsuite.Testcase.Failure failure = new Testsuite.Testcase.Failure();
            failure.setValue( trace );
            failure.setMessage( parseForMessage( trace ) );
            failure.setType( parseForException( trace ) );
            currentTestCase.getFailure().add( failure );
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
        catch ( TimeoutException | AdbCommandRejectedException | IOException | ShellCommandUnresponsiveException e )
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
            double seconds = ( System.currentTimeMillis() - currentTestCaseStartTime ) / 1000.0;
            currentTestCase.setTime( timeFormatter.format( seconds ) );
            report.getTestcase().add( currentTestCase );
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
            report.setTests( Integer.toString( testCount ) );
            report.setFailures( Integer.toString( testFailureCount ) );
            report.setErrors( Integer.toString( testErrorCount ) );
            report.setSkipped( Integer.toString( testIgnoredCount ) );
            report.setTime( timeFormatter.format( elapsedTime / 1000.0 ) );
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
     * @param trace stack trace from android tests
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
     * @param trace stack trace from android tests
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
        try
        {
            final String directory = String.valueOf( targetDirectory ) + "/surefire-reports";
            FileUtils.forceMkdir( new File ( directory ) );
            final StringBuilder b = new StringBuilder( directory ).append( "/TEST-" )
                    .append( DeviceHelper.getDescriptiveName( device ) );

            if ( StringUtils.isNotBlank( reportSuffix ) )
            {
                //Safety first
                b.append( reportSuffix.replace( "/", "" ).replace( "\\", "" ) );
            }

            final File reportFile = new File( b.append( ".xml" ).toString() );
            final JAXBContext jaxbContext = JAXBContext.newInstance( ObjectFactory.class );
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal( report, reportFile );

            getLog().info( deviceLogLinePrefix + "Report file written to " + reportFile.getAbsolutePath() );
        }
        catch ( IOException e )
        {
            threwException = true;
            exceptionMessages.append( "Failed to write test report file" );
            exceptionMessages.append( e.getMessage() );
        }
        catch ( JAXBException e )
        {
            threwException = true;
            exceptionMessages.append( "Failed to create jaxb context" );
            exceptionMessages.append( e.getMessage() );
        }
    }

    /**
     * Log all the metrics out in to key: value lines.
     *
     * @param metrics key-value pairs reported at the end of a test run
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
