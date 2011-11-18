package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for proguard.
 *
 */
public class Proguard {

    /**
     * Whether ProGuard is enabled or not.
     * 
     * @parameter default-value=true
     * 
     */
    private boolean skip = true;

    /**
     * Path to the ProGuard configuration file (relative to project root).
     * 
     * @parameter default-value="proguard.cfg"
     * 
     */
    private String config = "proguard.cfg";

    public String getConfig() {
        return config;
    }

    public boolean isSkip() {
        return skip;
    }
}
