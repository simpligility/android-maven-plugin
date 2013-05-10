package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the ui automator test runs. This class is only the definition of the parameters that are shadowed
 * in {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo} and used there.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
public class UIAutomator
{
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#testSkip}
     */
    private Boolean skip;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#jarFile}
     */
    private String jarFile;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#testClassOrMethods}
     */
    private String[] testClassOrMethods;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#noHup}
     */
    private Boolean noHup = false;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#debug}
     */
    private Boolean debug = false;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#useDump}
     */
    private Boolean useDump = false;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#dumpFilePath}
     */
    private String dumpFilePath;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#createReport}
     */
    private Boolean createReport;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#reportSuffix}
     */
    private String reportSuffix;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#takeScreenshotOnFailure}
     */
    private Boolean takeScreenshotOnFailure;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#screenshotsPathOnDevice}
     */
    private String screenshotsPathOnDevice;

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

    public String getReportSuffix()
    {
        return reportSuffix;
    }

    public Boolean isTakeScreenshotOnFailure()
    {
        return takeScreenshotOnFailure;
    }

    public String getScreenshotsPathOnDevice()
    {
        return screenshotsPathOnDevice;
    }
}
