package com.jayway.maven.plugins.android;

/**
 * Configuration for the jarsigner command. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.AbstractJarsignerMojo} and used there.
 *
 * @author Manfred Moser
 */
public class Jarsigner {

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
    private String keystore;

    /**
     * @parameter
     */
    private String storepass;

    /**
     * @parameter
     */
    private String keypass;

    /**
     * @parameter
     */
    private String inputApk;

    public Boolean isEnabled() {
        return enabled;
    }

    public Boolean isVerbose() {
        return verbose;
    }

    public String getKeystore() {
        return keystore;
    }

    public String getStorepass() {
        return storepass;
    }

    public String getKeypass() {
        return keypass;
    }

    public String getInputApk() {
        return inputApk;
    }
}