package com.android.ddmlib.testrunner;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
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
    private InstrumentationResultParser mParser;
    private boolean noHup;
    private final long eventCount;

    private static final String LOG_TAG = "RemoteAndroidTest";

    // defined instrumentation argument names
    private static final String SEED_ARG_NAME = "class";

    public MonkeyTestRunner( long eventCount, IDevice remoteDevice )
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

    /**
     * {@inheritDoc}
     */

    /**
     * {@inheritDoc}
     */
    public void setSeed( long seed )
    {
        addLongArg( SEED_ARG_NAME, seed );
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
        final String runCaseCommandStr = String.format( "monkey %1$s %2$s", buildArgsCommand(),
                Long.toString( eventCount ) );
        Log.i( LOG_TAG, String.format( "Running %1$s on %2$s", runCaseCommandStr, mRemoteDevice.getSerialNumber() ) );
        mParser = new InstrumentationResultParser( mRunName, listeners );

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

        if ( noHup )
        {
            commandBuilder.append( " --nohup" );
        }
        return commandBuilder.toString();
    }
}
