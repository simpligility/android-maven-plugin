package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the lint command. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.standalonemojos.LintMojo} and used there.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
public class Lint {

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

    public Boolean isSkip() {
        return skip;
    }

    public Boolean getNowarn() {
        return nowarn;
    }

    public Boolean getWall() {
        return wall;
    }

    public Boolean getWerror() {
        return werror;
    }

    public String getConfig() {
        return config;
    }

    public Boolean getFullpath() {
        return fullpath;
    }

    public Boolean getShowall() {
        return showall;
    }

    public Boolean getNolines() {
        return nolines;
    }

    public String getHtml() {
        return html;
    }

    public String getSimplehtml() {
        return simplehtml;
    }

    public String getXml() {
        return xml;
    }

    public String getSources() {
        return sources;
    }

    public String getClasspath() {
        return classpath;
    }

    public String getLibraries() {
        return libraries;
    }

}
