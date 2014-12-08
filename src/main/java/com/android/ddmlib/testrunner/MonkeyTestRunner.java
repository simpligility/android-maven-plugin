package com.android.ddmlib.testrunner;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

/**
 * Runs a Monkey test command remotely and reports results.
 */
public class MonkeyTestRunner
{

    private IDevice mRemoteDevice;
    // default to no timeout
    private int mMaxTimeToOutputResponse = 0;
    private String mRunName = null;

    /** map of name-value instrumentation argument pairs */
    private List< Entry< String, String >> mArgList;
    private MonkeyResultParser mParser;
    private final int eventCount;
    private boolean ignoreCrashes;
    private boolean debugNoEvents;
    private boolean hprof;
    private boolean ignoreTimeouts;
    private boolean ignoreSecurityExceptions;
    private boolean killProcessAfterError;
    private boolean monitorNativeCrashes;

    private static final String LOG_TAG = "RemoteAndroidTest";

    // defined instrumentation argument names
    private static final String SEED_ARG_NAME = "-s";
    private static final String THROTTLE_ARG_NAME = "--throttle";
    private static final String PERCENT_TOUCH_ARG_NAME = "--pct-touch";
    private static final String PERCENT_MOTION_ARG_NAME = "--pct-motion";
    private static final String PERCENT_TRACKBALL_ARG_NAME = "--pct-trackball";
    private static final String PERCENT_NAV_ARG_NAME = "--pct-nav";
    private static final String PERCENT_MAJORNAV_ARG_NAME = "--pct-majornav";
    private static final String PERCENT_SYSKEYS_ARG_NAME = "--pct-syskeys";
    private static final String PERCENT_APPSWITCH_ARG_NAME = "--pct-appswitch";
    private static final String PERCENT_ANYEVENT_ARG_NAME = "--pct-anyevent";
    private static final String PACKAGE_ARG_NAME = "-p";
    private static final String CATEGORY_ARG_NAME = "-c";

    public MonkeyTestRunner( int eventCount, IDevice remoteDevice )
    {
        this.eventCount = eventCount;
        mRemoteDevice = remoteDevice;
        mArgList = new ArrayList< Entry< String, String >>();
    }

    /**
     * {@inheritDoc}
     */
    public void addArg( String name, String value )
    {
        if ( name == null || value == null )
        {
            throw new IllegalArgumentException( "name or value arguments cannot be null" );
        }
        mArgList.add( new AbstractMap.SimpleImmutableEntry< String, String >( name, value ) );
    }

    /**
     * {@inheritDoc}
     */

    public void addBooleanArg( String name, boolean value )
    {
        addArg( name, Boolean.toString( value ) );
    }

    /**
     * {@inheritDoc}
     */
    public void addLongArg( String name, long value )
    {
        addArg( name, Long.toString( value ) );
    }

    public void setSeed( long seed )
    {
        addLongArg( SEED_ARG_NAME, seed );
    }

    public void setThrottle( long throttle )
    {
        addLongArg( THROTTLE_ARG_NAME, throttle );
    }

    public void setPercentTouch( long percent )
    {
        addLongArg( PERCENT_TOUCH_ARG_NAME, percent );
    }

    public void setPercentMotion( long percent )
    {
        addLongArg( PERCENT_MOTION_ARG_NAME, percent );
    }

    public void setPercentTrackball( long percent )
    {
        addLongArg( PERCENT_TRACKBALL_ARG_NAME, percent );
    }

    public void setPercentNav( long percent )
    {
        addLongArg( PERCENT_NAV_ARG_NAME, percent );
    }

    public void setPercentMajorNav( long percent )
    {
        addLongArg( PERCENT_MAJORNAV_ARG_NAME, percent );
    }

    public void setPercentSyskeys( long percent )
    {
        addLongArg( PERCENT_SYSKEYS_ARG_NAME, percent );
    }

    public void setPercentAppswitch( long percent )
    {
        addLongArg( PERCENT_APPSWITCH_ARG_NAME, percent );
    }

    public void setPercentAnyEvent( int percent )
    {
        addLongArg( PERCENT_ANYEVENT_ARG_NAME, percent );
    }

    public void setPackages( String[] packages )
    {
        for ( String packageName : packages )
        {
            addArg( PACKAGE_ARG_NAME, packageName );
        }
    }

    public void setCategories( String[] categories )
    {
        for ( String category : categories )
        {
            addArg( CATEGORY_ARG_NAME, category );
        }
    }

    public void setDebugNoEvents( boolean debugNoEvents )
    {
        this.debugNoEvents = debugNoEvents;
    }

    public void setHprof( boolean hprof )
    {
        this.hprof = hprof;
    }

    public void setIgnoreCrashes( boolean ignoreCrashes )
    {
        this.ignoreCrashes = ignoreCrashes;
    }

    public void setIgnoreTimeouts( boolean ignoreTimeouts )
    {
        this.ignoreTimeouts = ignoreTimeouts;
    }

    public void setIgnoreSecurityExceptions( boolean ignoreSecurityExceptions )
    {
        this.ignoreSecurityExceptions = ignoreSecurityExceptions;
    }

    public void setKillProcessAfterError( boolean killProcessAfterError )
    {
        this.killProcessAfterError = killProcessAfterError;
    }

    public void setMonitorNativeCrash( boolean monitorNativeCrashes )
    {
        this.monitorNativeCrashes = monitorNativeCrashes;
    }

    /**
     * {@inheritDoc}
     */

    public void setMaxtimeToOutputResponse( int maxTimeToOutputResponse )
    {
        mMaxTimeToOutputResponse = maxTimeToOutputResponse;
    }

    /**
     * {@inheritDoc}
     */

    public void setRunName( String runName )
    {
        mRunName = runName;
    }

    /**
     * {@inheritDoc}
     */

    public void run( ITestRunListener... listeners ) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException
    {
        run( Arrays.asList( listeners ) );
    }

    /**
     * {@inheritDoc}
     */
    public void run( Collection< ITestRunListener > listeners ) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException
    {
        final String runCaseCommandStr = String.format( "monkey -v -v -v %1$s %2$s", buildArgsCommand(),
                Long.toString( eventCount ) );
        Log.i( LOG_TAG, String.format( "Running %1$s on %2$s", runCaseCommandStr, mRemoteDevice.getSerialNumber() ) );
        mParser = new MonkeyResultParser( mRunName, listeners );

        try
        {
            mRemoteDevice.executeShellCommand( runCaseCommandStr, mParser, mMaxTimeToOutputResponse );
        }
        catch ( IOException e )
        {
            Log.w( LOG_TAG,
                    String.format( "IOException %1$s when running monkey tests on %3$s", e.toString(),
                            mRemoteDevice.getSerialNumber() ) );
            // rely on parser to communicate results to listeners
            mParser.handleTestRunFailed( e.toString() );
            throw e;
        }
        catch ( ShellCommandUnresponsiveException e )
        {
            Log.w( LOG_TAG,
                    String.format( "ShellCommandUnresponsiveException %1$s when running monkey tests on %3$s",
                            e.toString(), mRemoteDevice.getSerialNumber() ) );
            mParser.handleTestRunFailed( String.format( "Failed to receive adb shell test output within %1$d ms. "
                    + "Test may have timed out, or adb connection to device became unresponsive",
                    mMaxTimeToOutputResponse ) );
            throw e;
        }
        catch ( TimeoutException e )
        {
            Log.w( LOG_TAG,
                    String.format( "TimeoutException when running monkey tests on %2$s",
                            mRemoteDevice.getSerialNumber() ) );
            mParser.handleTestRunFailed( e.toString() );
            throw e;
        }
        catch ( AdbCommandRejectedException e )
        {
            Log.w( LOG_TAG,
                    String.format( "AdbCommandRejectedException %1$s when running monkey tests %2$s on %3$s",
                            e.toString(), mRemoteDevice.getSerialNumber() ) );
            mParser.handleTestRunFailed( e.toString() );
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancel()
    {
        if ( mParser != null )
        {
            mParser.cancel();
        }
    }

    /**
     * Returns the full instrumentation command line syntax for the provided instrumentation arguments. Returns an empty
     * string if no arguments were specified.
     */
    private String buildArgsCommand()
    {
        StringBuilder commandBuilder = new StringBuilder();
        for ( Entry< String, String > argPair : mArgList )
        {
            final String argCmd = String.format( " %1$s %2$s", argPair.getKey(), argPair.getValue() );
            commandBuilder.append( argCmd );
        }

        if ( debugNoEvents )
        {
            commandBuilder.append( " --dbg-no-events" );
        }
        if ( hprof )
        {
            commandBuilder.append( " --hprof" );
        }
        if ( ignoreCrashes )
        {
            commandBuilder.append( " --ignore-crashes" );
        }
        if ( ignoreTimeouts )
        {
            commandBuilder.append( " --ignore-timeouts" );
        }
        if ( ignoreSecurityExceptions )
        {
            commandBuilder.append( " --ignore-security-exceptions" );
        }
        if ( killProcessAfterError )
        {
            commandBuilder.append( " --kill-process-after-error" );
        }
        if ( monitorNativeCrashes )
        {
            commandBuilder.append( " --monitor-native-crashes" );
        }
        return commandBuilder.toString();
    }

    private class MonkeyResultParser extends MultiLineReceiver
    {

        private static final String CRASH_KEY = "// CRASH:";
        private static final String SHORT_MESSAGE_KEY = "// Short Msg:";
        private static final String LONG_MESSAGE_KEY = "// Long Msg:";
        private static final String BUILD_LABEL_KEY = "// Build Label:";
        private static final String BUILD_CHANGELIST_KEY = "// Build Changelist:";
        private static final String BUILD_TIME_KEY = "// Build Time:";
        private static final String EMPTY_KEY = "//";
        private static final String SENDING_KEY = ":Sending";
        private static final String SWITCHING_KEY = ":Switch";
        private static final String MONKEY_KEY = ":Monkey:";

        private final Collection< ITestRunListener > mTestListeners;

        private String runName;
        private boolean canceled;
        private TestIdentifier mCurrentTestIndentifier;
        private long elapsedTime;
        private HashMap< String, String > runMetrics = new HashMap< String, String >();

        private MonkeyResultParser( String runName, Collection< ITestRunListener > listeners )
        {
            this.runName = runName;
            mTestListeners = new ArrayList< ITestRunListener >( listeners );
        }

        public void cancel()
        {
            canceled = true;
        }

        @Override
        public boolean isCancelled()
        {
            return canceled;
        }

        @Override
        public void done()
        {
            handleTestEnd();
            handleTestRunEnded();
            super.done();
        }

        @Override
        public void processNewLines( String[] lines )
        {
            for ( int indexLine = 0; indexLine < lines.length; indexLine++ )
            {
                String line = lines[ indexLine ];
                Log.v( "monkey receiver:" + runName, line );

                if ( line.startsWith( MONKEY_KEY ) )
                {
                    handleTestRunStarted();
                }
                if ( line.startsWith( SHORT_MESSAGE_KEY ) )
                {
                    runMetrics.put( "ShortMsg", line.substring( SHORT_MESSAGE_KEY.length() - 1 ) );
                }
                if ( line.startsWith( LONG_MESSAGE_KEY ) )
                {
                    runMetrics.put( "LongMsg", line.substring( LONG_MESSAGE_KEY.length() - 1 ) );
                }
                if ( line.startsWith( BUILD_LABEL_KEY ) )
                {
                    runMetrics.put( "BuildLabel", line.substring( BUILD_LABEL_KEY.length() - 1 ) );
                }
                if ( line.startsWith( BUILD_CHANGELIST_KEY ) )
                {
                    runMetrics.put( "BuildChangeList", line.substring( BUILD_CHANGELIST_KEY.length() - 1 ) );
                }
                if ( line.startsWith( BUILD_TIME_KEY ) )
                {
                    runMetrics.put( "BuildTime", line.substring( BUILD_TIME_KEY.length() - 1 ) );
                }

                if ( line.startsWith( SENDING_KEY ) || line.startsWith( SWITCHING_KEY ) )
                {
                    handleTestEnd();
                    handleTestStarted( line );
                }

                if ( line.startsWith( CRASH_KEY ) )
                {
                    Log.d( "monkey received crash:", line );
                    indexLine = handleCrash( lines, indexLine );
                    handleTestEnd();
                }
            }
        }

        private void handleTestRunStarted()
        {
            elapsedTime = System.currentTimeMillis();
            for ( ITestRunListener listener : mTestListeners )
            {
                listener.testRunStarted( mRunName, eventCount );
            }
        }

        public void handleTestRunFailed( String error )
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

        private void handleTestStarted( String line )
        {
            mCurrentTestIndentifier = new TestIdentifier( "MonkeyTest", line );
            for ( ITestRunListener listener : mTestListeners )
            {
                listener.testStarted( mCurrentTestIndentifier );
            }
        }

        private void handleTestEnd()
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

        private int handleCrash( String[] lines, int indexLine )
        {
            StringBuilder errorBuilder = new StringBuilder();
            boolean errorEnd = false;
            boolean errorStart = false;
            do
            {
                String line = lines[ indexLine ];
                if ( line.startsWith( BUILD_TIME_KEY ) )
                {
                    errorStart = true;
                }
                indexLine++;
            } while ( !errorStart );

            // indexLine point to the first line of the stack trace now
            int firstLine = indexLine;

            do
            {
                String line = lines[ indexLine ];
                if ( line.equals( EMPTY_KEY ) )
                {
                    errorEnd = true;
                }
                else
                {
                    String stackTraceLine = lines[ indexLine ];
                    stackTraceLine = stackTraceLine.substring( indexLine == firstLine ? 3 : 4 );
                    errorBuilder.append( stackTraceLine ).append( "\n" );
                }
                indexLine++;
            } while ( !errorEnd );

            String trace = errorBuilder.toString();

            for ( ITestRunListener listener : mTestListeners )
            {
                listener.testFailed( mCurrentTestIndentifier, trace );
            }
            mCurrentTestIndentifier = null;
            return indexLine;
        }
    }
}
