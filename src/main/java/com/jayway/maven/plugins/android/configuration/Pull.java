package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the integration test runs. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Pull {
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.PullMojo#source}
      */
    private String source;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.PullMojo#destination}
      */
    private String destination;

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }
}
