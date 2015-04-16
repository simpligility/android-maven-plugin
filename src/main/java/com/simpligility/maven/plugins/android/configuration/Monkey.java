package com.simpligility.maven.plugins.android.configuration;

/**
 * Configuration for the monkey test runs. This class is only the definition of the parameters that are shadowed in
 * {@link com.simpligility.maven.plugins.android.standalonemojos.MonkeyMojo} and used there.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
public class Monkey
{
    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.standalonemojos.UIAutomatorMojo#testSkip}
     */
    private Boolean skip;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#eventCount}
     */
    private Integer eventCount;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#seed}
     */
    private Long seed;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#throttle}
     */
    private Long throttle;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#percentTouch}
     */
    private Integer percentTouch;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#percentMotion}
     */
    private Integer percentMotion;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#percentTrackball}
     */
    private Integer percentTrackball;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#percentNav}
     */
    private Integer percentNav;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#percentMajorNav}
     */
    private Integer percentMajorNav;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#percentSyskeys}
     */
    private Integer percentSyskeys;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#percentAppswitch}
     */
    private Integer percentAppswitch;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#percentAnyevent}
     */
    private Integer percentAnyevent;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#packages}
     */
    private String[] packages;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#categories}
     */
    private String[] categories;
    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.standalonemojos.UIAutomatorMojo#debugNoEvents}
     */
    private Boolean debugNoEvents;
    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.standalonemojos.UIAutomatorMojo#hprof}
     */
    private Boolean hprof;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#ignoreCrashes}
     */
    private Boolean ignoreCrashes;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#ignoreTimeouts}
     */
    private Boolean ignoreTimeouts;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#ignoreSecurityExceptions}
     */
    private Boolean ignoreSecurityExceptions;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#killProcessAfterError}
     */
    private Boolean killProcessAfterError;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#monitorNativeCrashes}
     */
    private Boolean monitorNativeCrashes;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#createReport}
     */
    private Boolean createReport;

    public Boolean isSkip()
    {
        return skip;
    }

    public Integer getEventCount()
    {
        return eventCount;
    }

    public Long getSeed()
    {
        return seed;
    }

    public Long isThrottle()
    {
        return throttle;
    }

    public Integer getPercentTouch()
    {
        return percentTouch;
    }

    public Integer getPercentMotion()
    {
        return percentMotion;
    }

    public Integer getPercentTrackball()
    {
        return percentTrackball;
    }

    public Integer getPercentNav()
    {
        return percentNav;
    }

    public Integer getPercentMajorNav()
    {
        return percentMajorNav;
    }

    public Integer getPercentSyskeys()
    {
        return percentSyskeys;
    }

    public Integer getPercentAppswitch()
    {
        return percentAppswitch;
    }

    public Integer getPercentAnyevent()
    {
        return percentAnyevent;
    }

    public String[] getPackages()
    {
        return packages;
    }

    public String[] getCategories()
    {
        return categories;
    }

    public Boolean isDebugNoEvents()
    {
        return skip;
    }

    public Boolean hProf()
    {
        return skip;
    }

    public Boolean isIgnoreTimeouts()
    {
        return ignoreTimeouts;
    }

    public Boolean isIgnoreSecurityExceptions()
    {
        return ignoreSecurityExceptions;
    }

    public Boolean isKillProcessAfterError()
    {
        return killProcessAfterError;
    }

    public Boolean isMonitorNativeErrors()
    {
        return monitorNativeCrashes;
    }

    public Boolean isCreateReport()
    {
        return createReport;
    }
}
