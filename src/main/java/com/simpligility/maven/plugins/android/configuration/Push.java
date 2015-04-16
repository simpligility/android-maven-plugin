package com.simpligility.maven.plugins.android.configuration;

/**
 * Configuration for the integration test runs. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.simpligility.maven.plugins.android.standalonemojos.PushMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Push
{
    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.standalonemojos.PushMojo#source}
     */
    private String source;
    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.standalonemojos.PushMojo#destination}
     */
    private String destination;

    public String getSource()
    {
        return source;
    }

    public String getDestination()
    {
        return destination;
    }
}
