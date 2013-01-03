package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the lint command. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo} and used there.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * @author Manfred Moser <manfred@simpligility.com>
 * @see com.jayway.maven.plugins.android.standalonemojos.LintMojo
 */
public class Lint
{
    private Boolean failOnError;
    private Boolean skip;

    // ---------------
    // Enabled Checks
    // ---------------
    private Boolean nowarn;
    private Boolean wall;
    private Boolean werror;
    private String config;

    // ---------------
    // Output Options
    // ---------------
    private Boolean fullpath;
    private Boolean showall;
    private Boolean nolines;
    private Boolean url;
    private String html;
    private String simplehtml;
    private String xml;

    // ---------------
    // Project Options
    // ---------------
    private String sources;
    private String classpath;
    private String libraries;

    // ---------------
    // Getters
    // ---------------

    public final Boolean isSkip()
    {
        return skip;
    }

    public final Boolean getNowarn()
    {
        return nowarn;
    }

    public final Boolean getWall()
    {
        return wall;
    }

    public final Boolean getWerror()
    {
        return werror;
    }

    public final String getConfig()
    {
        return config;
    }

    public final Boolean getFullpath()
    {
        return fullpath;
    }

    public final Boolean getShowall()
    {
        return showall;
    }

    public final Boolean getNolines()
    {
        return nolines;
    }

    public final String getHtml()
    {
        return html;
    }

    public final String getSimplehtml()
    {
        return simplehtml;
    }

    public final String getXml()
    {
        return xml;
    }

    public final String getSources()
    {
        return sources;
    }

    public final String getClasspath()
    {
        return classpath;
    }

    public final String getLibraries()
    {
        return libraries;
    }
}
