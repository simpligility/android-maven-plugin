package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the ui automator test runs. This class is only the definition of the parameters that are shadowed
 * in {@link com.jayway.maven.plugins.android.standalonemojos.AutomatorMojo} and used there.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
public class Automator
{
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.AutomatorMojo#testSkip}
     */
    private Boolean skip;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.AutomatorMojo#jarFile}
     */
    private String jarFile;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.AutomatorMojo#testClassOrMethods}
     */
    private String[] testClassOrMethods;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.AutomatorMojo#noHup}
     */
    private Boolean noHup = false;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.AutomatorMojo#debug}
     */
    private Boolean debug = false;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.AutomatorMojo#useDump}
     */
    private Boolean useDump = false;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.AutomatorMojo#dumpFilePath}
     */
    private String dumpFilePath;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.AutomatorMojo#createReport}
     */
    private Boolean createReport;

    public Boolean isSkip()
    {
        return skip;
    }

    public String getJarFile()
    {
        return jarFile;
    }

    public String[] getTestClassOrMethods()
    {
        return testClassOrMethods;
    }

    public Boolean getNoHup()
    {
        return noHup;
    }

    public Boolean getDebug()
    {
        return debug;
    }

    public Boolean getUseDump()
    {
        return useDump;
    }

    public String getDumpFilePath()
    {
        return dumpFilePath;
    }

    public Boolean isCreateReport()
    {
        return createReport;
    }

}
