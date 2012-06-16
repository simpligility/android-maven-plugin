package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the zipalign command. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.standalonemojos.ZipalignMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Zipalign {
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ZipalignMojo#zipalignSkip}
     */
    private Boolean skip;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ZipalignMojo#zipalignVerbose}
     */
    private Boolean verbose;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ZipalignMojo#zipalignInputApk}
     */
    private String inputApk;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ZipalignMojo#zipalignOutputApk}
     */
    private String outputApk;


    public Boolean isSkip() {
        return skip;
    }

    public Boolean isVerbose() {
        return verbose;
    }

    public String getInputApk() {
        return inputApk;
    }

    public String getOutputApk() {
        return outputApk;
    }
}
