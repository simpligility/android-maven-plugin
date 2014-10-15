package com.jayway.maven.plugins.android.phase09package;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.ILogger;
import org.apache.maven.plugin.logging.Log;

public class MavenAndroidLogger implements ILogger
{
    private final Log log;

    public MavenAndroidLogger( Log log )
    {
        this.log = log;
    }

    @Override
    public void error( @Nullable Throwable throwable, @Nullable String s, Object... objects )
    {
        log.error( getFormattedMessage( s, objects ), throwable );
    }

    @Override
    public void warning( @NonNull String s, Object... objects )
    {
        log.warn( getFormattedMessage( s, objects ) );
    }

    @Override
    public void info( @NonNull String s, Object... objects )
    {
        log.info( getFormattedMessage( s, objects ) );
    }

    @Override
    public void verbose( @NonNull String s, Object... objects )
    {
        log.debug( getFormattedMessage( s, objects ) );
    }

    private CharSequence getFormattedMessage( String messageFormat, Object... arguments )
    {
        if ( arguments == null || arguments.length == 0 )
        {
            return messageFormat;
        }

        return String.format( messageFormat, arguments );
    }
}
