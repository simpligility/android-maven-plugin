package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for proguard.
 *
 */
public class Proguard {

    /**
     * The proguard configuration file.
     *
     * @parameter
     *
     */
    private String config = "proguard.cfg";

    public String getConfig() {
        return config;
    }
}
