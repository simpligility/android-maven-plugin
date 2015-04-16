package com.simpligility.maven.plugins.android.configuration;

import java.io.File;

/**
 * Configuration container for proguard without default values.
 *
 * @author Matthias Kaeppler
 * @author Manfred Moser
 * @author Michal Harakal
 * @see com.simpligility.maven.plugins.android.phase04processclasses.ProguardMojo
 */
public class Proguard
{

    /**
     * Whether ProGuard is enabled or not.
     */
    private Boolean skip;
    /**
     * Path to the ProGuard configuration file (relative to project root).
     */
    private File config;
    private String[] configs;
    private String proguardJarPath;
    private File outputDirectory;
    private String[] jvmArguments;
    private Boolean filterMavenDescriptor;
    private Boolean filterManifest;
    private String customFilter;
    private Boolean includeJdkLibs;
    private String[] options;
    private Boolean attachMap;

    public Boolean isSkip()
    {
        return skip;
    }

    public File getConfig()
    {
        return config;
    }

    public String[] getConfigs()
    {
        return configs;
    }

    public String getProguardJarPath()
    {
        return proguardJarPath;
    }
    
    public File getOutputDirectory()
    {
        return outputDirectory;
    }
   
    public String[] getJvmArguments()
    {
        return jvmArguments;
    }

    public Boolean isFilterMavenDescriptor()
    {
        return filterMavenDescriptor;
    }
    
    public String getCustomFilter()
    {
        return customFilter;
    }

    public Boolean isFilterManifest()
    {
        return filterManifest;
    }
    
    public Boolean isIncludeJdkLibs()
    {
        return includeJdkLibs;
    }

    public String[] getOptions()
    {
        return options;
    }
}
