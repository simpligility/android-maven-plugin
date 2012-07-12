package com.jayway.maven.plugins.android.configuration;

import java.util.List;

/**
 * Configuration for the integration test runs. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Test
{
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
     */
    private String skip;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testInstrumentationPackage}
     */
    private String instrumentationPackage;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testInstrumentationRunner}
     */
    private String instrumentationRunner;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testDebug}
     */
    private Boolean debug;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testCoverage}
     */
    private Boolean coverage;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testCoverageFile}
     */
    private String coverageFile;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testLogOnly}
     */
    private Boolean logOnly;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSize}
     */
    private String testSize;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testCreateReport}
     */
    private Boolean createReport;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testPackages}
     */
    protected List<String> packages;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testClasses}
     */
    protected List<String> classes;

    public String getSkip()
    {
        return skip;
    }

    public String getInstrumentationPackage()
    {
        return instrumentationPackage;
    }

    public String getInstrumentationRunner()
    {
        return instrumentationRunner;
    }

    public Boolean isDebug()
    {
        return debug;
    }

    public Boolean isCoverage()
    {
        return coverage;
    }

    public String getCoverageFile()
    {
        return coverageFile;
    }

    public Boolean isLogOnly()
    {
        return logOnly;
    }

    public String getTestSize()
    {
        return testSize;
    }

    public Boolean isCreateReport()
    {
        return createReport;
    }

    public List<String> getPackages()
    {
        return packages;
    }

    public List<String> getClasses()
    {
        return classes;
    }
}
