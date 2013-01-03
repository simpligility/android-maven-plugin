package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the lint command. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo} and used there.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
public class Lint
{

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintFailOnError}
     */
    private Boolean failOnError;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintSkip}
     */
    private Boolean skip;

    // ---------------
    // Enabled Checks
    // ---------------

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintNoWarn}
     */
    private Boolean nowarn;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintWall}
     */
    private Boolean wall;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintWerror}
     */
    private Boolean werror;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintConfig}
     */
    private String config;

    // ---------------
    // Output Options
    // ---------------

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintFullpath}
     */
    private Boolean fullpath;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintShowall}
     */
    private Boolean showall;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintNolines}
     */
    private Boolean nolines;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintUrl}
     */
    private Boolean url;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintHtml}
     */
    private String html;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintSimplehtml}
     */
    private String simplehtml;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintXml}
     */
    private String xml;

    // ---------------
    // Project Options
    // ---------------

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintSources}
     */
    private String sources;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintClasspath}
     */
    private String classpath;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo#lintLibraries}
     */
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
