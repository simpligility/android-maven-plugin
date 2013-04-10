package com.jayway.maven.plugins.android.configuration;

import java.util.List;

/**
 * Configuration for the monkey runner tests runs. This class is only the definition of the parameters that are shadowed
 * in {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner} and used there.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
public class MonkeyRunner
{
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#testSkip}
     */
    private Boolean skip;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#programs}
     */
    private List< Program > programs;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#plugins}
     */
    private String[] plugins;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#createReport}
     */
    private Boolean createReport;
    /**
     * Mirror of
     * {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#injectDeviceSerialNumberIntoScript}
     */
    private Boolean injectDeviceSerialNumberIntoScript;

    public boolean isSkip()
    {
        return skip;
    }

    public List< Program > getPrograms()
    {
        return programs;
    }

    public String[] getPlugins()
    {
        return plugins;
    }

    public Boolean isCreateReport()
    {
        return createReport;
    }

    public Boolean isInjectDeviceSerialNumberIntoScript()
    {
        return injectDeviceSerialNumberIntoScript;
    }
}
