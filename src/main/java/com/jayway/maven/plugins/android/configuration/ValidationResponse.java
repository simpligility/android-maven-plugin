package com.jayway.maven.plugins.android.configuration;
/**
 * ValidationResponse wraps a validation message and result flag 
 * allows the using class to decide how to react to a validation 
 * failure.
 * 
 * @author Manfred Moser <manfred@simpligility.com>
 * @see DeployApk
 */
public final class ValidationResponse 
{
    private final boolean valid;
    private final String message;
    
    public ValidationResponse( final boolean valid, final String message ) 
    {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid()
    {
        return valid;
    }

    public String getMessage()
    {
        return message;
    }
}
