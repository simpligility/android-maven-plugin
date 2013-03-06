package com.android.ddmlib.testrunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

/**
 * Runs a Android test command remotely and reports results.
 */
public class UIAutomatorRemoteAndroidTestRunner implements IRemoteAndroidTestRunner
{

    private IDevice mRemoteDevice;
    // default to no timeout
    private int mMaxTimeToOutputResponse = 0;
    private String mRunName = null;

    /** map of name-value instrumentation argument pairs */
    private Map< String, String > mArgMap;
    private InstrumentationResultParser mParser;
    private final String jarFile;
    private final String classOrMethod;

    private static final String LOG_TAG = "RemoteAndroidTest";
    private static final String DEFAULT_RUNNER_NAME = "android.test.InstrumentationTestRunner";

    private static final char CLASS_SEPARATOR = ',';
    private static final char METHOD_SEPARATOR = '#';
    private static final char RUNNER_SEPARATOR = '/';

    // defined instrumentation argument names
    private static final String CLASS_ARG_NAME = "class";
    private static final String LOG_ARG_NAME = "log";
    private static final String DEBUG_ARG_NAME = "debug";
    private static final String COVERAGE_ARG_NAME = "coverage";
    private static final String PACKAGE_ARG_NAME = "package";
    private static final String SIZE_ARG_NAME = "size";

    public UIAutomatorRemoteAndroidTestRunner( String jarFile, String classOrMethod, IDevice remoteDevice )
    {
        this.jarFile = jarFile;
        this.classOrMethod = classOrMethod;
        mRemoteDevice = remoteDevice;
        mArgMap = new Hashtable< String, String >();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClassName( String className )
    {
        addInstrumentationArg( CLASS_ARG_NAME, className );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClassNames( String[] classNames )
    {
        StringBuilder classArgBuilder = new StringBuilder();

        for ( int i = 0; i < classNames.length; i++ )
        {
            if ( i != 0 )
            {
                classArgBuilder.append( CLASS_SEPARATOR );
            }
            classArgBuilder.append( classNames[ i ] );
        }
        setClassName( classArgBuilder.toString() );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMethodName( String className, String testName )
    {
        setClassName( className + METHOD_SEPARATOR + testName );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTestPackageName( String packageName )
    {
        addInstrumentationArg( PACKAGE_ARG_NAME, packageName );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInstrumentationArg( String name, String value )
    {
        if ( name == null || value == null )
        {
            throw new IllegalArgumentException( "name or value arguments cannot be null" );
        }
        mArgMap.put( name, value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeInstrumentationArg( String name )
    {
        if ( name == null )
        {
            throw new IllegalArgumentException( "name argument cannot be null" );
        }
        mArgMap.remove( name );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addBooleanArg( String name, boolean value )
    {
        addInstrumentationArg( name, Boolean.toString( value ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLogOnly( boolean logOnly )
    {
        addBooleanArg( LOG_ARG_NAME, logOnly );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDebug( boolean debug )
    {
        addBooleanArg( DEBUG_ARG_NAME, debug );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCoverage( boolean coverage )
    {
        addBooleanArg( COVERAGE_ARG_NAME, coverage );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTestSize( TestSize size )
    {
        addInstrumentationArg( SIZE_ARG_NAME, size.getRunnerValue() );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxtimeToOutputResponse( int maxTimeToOutputResponse )
    {
        mMaxTimeToOutputResponse = maxTimeToOutputResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRunName( String runName )
    {
        mRunName = runName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run( ITestRunListener... listeners ) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException
    {
        run( Arrays.asList( listeners ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run( Collection< ITestRunListener > listeners ) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException
    {
        final String runCaseCommandStr = String.format( "uiautomator runtest %1$s -c %2$s", getJarFile(),
                getClassOrMethodName() );
        Log.i( LOG_TAG, String.format( "Running %1$s on %2$s", runCaseCommandStr, mRemoteDevice.getSerialNumber() ) );
        mParser = new InstrumentationResultParser( "toto", listeners );

        try
        {
            mRemoteDevice.executeShellCommand( runCaseCommandStr, mParser, mMaxTimeToOutputResponse );
        }
        catch ( IOException e )
        {
            Log.w( LOG_TAG, String.format( "IOException %1$s when running tests %2$s on %3$s", e.toString(),
                    getPackageName(), mRemoteDevice.getSerialNumber() ) );
            // rely on parser to communicate results to listeners
            mParser.handleTestRunFailed( e.toString() );
            throw e;
        }
        catch ( ShellCommandUnresponsiveException e )
        {
            Log.w( LOG_TAG,
                    String.format( "ShellCommandUnresponsiveException %1$s when running tests %2$s on %3$s",
                            e.toString(), getPackageName(), mRemoteDevice.getSerialNumber() ) );
            mParser.handleTestRunFailed( String.format( "Failed to receive adb shell test output within %1$d ms. "
                    + "Test may have timed out, or adb connection to device became unresponsive",
                    mMaxTimeToOutputResponse ) );
            throw e;
        }
        catch ( TimeoutException e )
        {
            Log.w( LOG_TAG,
                    String.format( "TimeoutException when running tests %1$s on %2$s", getPackageName(),
                            mRemoteDevice.getSerialNumber() ) );
            mParser.handleTestRunFailed( e.toString() );
            throw e;
        }
        catch ( AdbCommandRejectedException e )
        {
            Log.w( LOG_TAG, String.format( "AdbCommandRejectedException %1$s when running tests %2$s on %3$s",
                    e.toString(), getPackageName(), mRemoteDevice.getSerialNumber() ) );
            mParser.handleTestRunFailed( e.toString() );
            throw e;
        }
    }

    private String getClassOrMethodName()
    {
        return classOrMethod;
    }

    private String getJarFile()
    {
        return jarFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    private String getArgsCommand()
    {
        StringBuilder commandBuilder = new StringBuilder();
        for ( Entry< String, String > argPair : mArgMap.entrySet() )
        {
            final String argCmd = String.format( " -e %1$s %2$s", argPair.getKey(), argPair.getValue() );
            commandBuilder.append( argCmd );
        }
        return commandBuilder.toString();
    }

    @Override
    public String getPackageName()
    {
        return null;
    }

    @Override
    public String getRunnerName()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
