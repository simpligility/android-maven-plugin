package com.jayway.maven.plugins.android;

/**
 * Configuration for the zipalign command. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.AbstractZipalignMojo} and used there.
 *
 * @author Manfred Moser
 */
public class Zipalign {

    /**
     * @parameter
     * @default=false
     */
    private Boolean enabled;

    /**
     * @parameter
     * @default=true
     */
    private Boolean verbose;


    /**
     * @parameter
     */
    private String inputApk;

    /**
     * @parameter
     */
    private String outputApk;


    public Boolean isEnabled() {
        return enabled;
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
