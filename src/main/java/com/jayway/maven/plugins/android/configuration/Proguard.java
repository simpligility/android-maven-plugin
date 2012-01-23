package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for proguard.
 * @author Matthias Kaeppler
 * @author Manfred Moser
 */
public class Proguard {

    /**
     * Whether ProGuard is enabled or not.
     */
    private Boolean skip;

    /**
     * Path to the ProGuard configuration file (relative to project root).
     */
    private String config;
    
    private String proguardJarPath;

    private String[] jvmArguments;

    private boolean filterMavenDescriptor = false;
    private boolean filterManifest = false;

    public Boolean isSkip() {
        return skip;
    }

    public String getConfig() {
        return config;
    }

    public String getProguardJarPath() {
        return proguardJarPath;
    }

    public String[] getJvmArguments() {
        return jvmArguments;
    }

    public Boolean isFilterMavenDescriptor() {
        return filterMavenDescriptor;
    }

    public Boolean isFilterManifest() {
        return filterManifest;
    }
}
