package com.jayway.maven.plugins.android.configuration;

import java.util.List;

/**
 * Configuration for the ui automator test runs. This class is only the definition of the parameters that are shadowed
 * in {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorTestMojo} and used there.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
public class UIAutomatorTest
{
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorTestMojo#testSkip}
     */
    private Boolean skip;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorTestMojo#jarFile}
     */
    private String jarFile;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorTestMojo#testClassOrMethod}
     */
    private List< String > testClassOrMethod;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorTestMojo#noHup}
     */
    private Boolean noHup = false;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorTestMojo#debug}
     */
    private Boolean debug = false;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorTestMojo#useDump}
     */
    private Boolean useDump = false;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorTestMojo#dumpFilePath}
     */
    private String dumpFilePath;

    public Boolean getSkip()
    {
        return skip;
    }

    public String getJarFile()
    {
        return jarFile;
    }

    public List< String > getTestClassOrMethod()
    {
        return testClassOrMethod;
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

}
