package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration container for proguard without default values.
 *
 * @author Matthias Kaeppler
 * @author Manfred Moser
 * @author Michal Harakal
 * @see com.jayway.maven.plugins.android.phase04processclasses.ProguardMojo
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
    private String config;
    private String[] configs;
    private String proguardJarPath;
    private String outputDirectory;
    private String[] jvmArguments;
    private Boolean filterMavenDescriptor;
    private Boolean filterManifest;
    private Boolean includeJdkLibs;
    private String[] options;

	public Boolean isSkip()
    {
        return skip;
    }

    public String getConfig()
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
    
    public String getOutputDirectory()
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
