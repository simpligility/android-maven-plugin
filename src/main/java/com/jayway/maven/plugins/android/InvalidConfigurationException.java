package com.jayway.maven.plugins.android;

/**
 * Exception for notifying about an invalid plugin configuration.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class InvalidConfigurationException  extends RuntimeException
{
    public InvalidConfigurationException( String message )
    {
        super( message );
    }
}
