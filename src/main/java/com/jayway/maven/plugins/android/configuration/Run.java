package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the Run goal.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 * @see com.jayway.maven.plugins.android.standalonemojos.RunMojo
 */
public class Run
{

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.RunMojo#runDebug}
     */
    protected Boolean debug;

    public Boolean isDebug()
    {
        return debug;
    }
}
