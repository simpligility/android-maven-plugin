package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the monkey test runs. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyMojo} and used there.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
public class Monkey
{
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.UIAutomatorMojo#testSkip}
     */
    private Boolean skip;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#eventCount}
     */
    private Long eventCount;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.Monkey#seed}
     */
    private Long seed;

    public Boolean isSkip()
    {
        return skip;
    }

    public Long getEventCount()
    {
        return eventCount;
    }

    public Long getSeed()
    {
        return seed;
    }
}
