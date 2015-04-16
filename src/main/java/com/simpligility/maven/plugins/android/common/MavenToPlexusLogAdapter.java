package com.simpligility.maven.plugins.android.common;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;

/**
 * Maps from a Maven Logger to a Plexus Log.
 */
public final class MavenToPlexusLogAdapter implements Logger
{

    private final Log delegate;

    public MavenToPlexusLogAdapter( Log delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public void fatalError( String s )
    {
        delegate.error( s );
    }

    @Override
    public void fatalError( String s, Throwable throwable )
    {
        delegate.error( s, throwable );
    }

    @Override
    public void error( String content, Throwable error )
    {
        delegate.error( content, error );
    }

    @Override
    public void error( String content )
    {
        delegate.error( content );
    }

    @Override
    public void warn( String content )
    {
        delegate.warn( content );
    }

    @Override
    public void warn( String content, Throwable error )
    {
        delegate.warn( content, error );
    }

    @Override
    public void info( String content )
    {
        delegate.info( content );
    }

    @Override
    public void info( String content, Throwable error )
    {
        delegate.info( content, error );
    }

    @Override
    public void debug( String content )
    {
        delegate.debug( content );
    }

    @Override
    public void debug( String content, Throwable error )
    {
        delegate.debug( content, error );
    }

    @Override
    public boolean isDebugEnabled()
    {
        return delegate.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled()
    {
        return delegate.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled()
    {
        return delegate.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled()
    {
        return delegate.isErrorEnabled();
    }

    @Override
    public boolean isFatalErrorEnabled()
    {
        return delegate.isErrorEnabled();
    }

    @Override
    public int getThreshold()
    {
        if ( delegate.isErrorEnabled() )
        {
            return Logger.LEVEL_ERROR;
        }
        else if ( delegate.isWarnEnabled() )
        {
            return Logger.LEVEL_WARN;
        }
        else if ( delegate.isInfoEnabled() )
        {
            return Logger.LEVEL_INFO;
        }

        return Logger.LEVEL_DEBUG;
    }

    @Override
    public void setThreshold( int i )
    {
        // ignored
    }

    @Override
    public Logger getChildLogger( String s )
    {
        return null;
    }

    @Override
    public String getName()
    {
        return delegate.toString();
    }
}
