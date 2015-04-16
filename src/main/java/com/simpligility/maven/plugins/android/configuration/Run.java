package com.simpligility.maven.plugins.android.configuration;

/**
 * Configuration for the Run goal.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 * @see com.simpligility.maven.plugins.android.standalonemojos.RunMojo
 */
public class Run
{

    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.standalonemojos.RunMojo#runDebug}
     */
    protected String debug;

    public String isDebug()
    {
        return debug;
    }
}
