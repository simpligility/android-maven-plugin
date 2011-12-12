package com.jayway.maven.plugins.android.configuration;

import java.util.List;

/**
 * Configuration for the integration test runs. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Test {
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private String skip;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private String instrumentationPackage;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private String instrumentationRunner;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private Boolean debug;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private Boolean coverage;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private Boolean logOnly;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private String testSize;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private Boolean createReport;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    protected List packages;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    protected List classes;

    public String getSkip() {
        return skip;
    }

    public String getInstrumentationPackage() {
        return instrumentationPackage;
    }

    public String getInstrumentationRunner() {
        return instrumentationRunner;
    }

    public Boolean isDebug() {
        return debug;
    }

    public Boolean isCoverage() {
        return coverage;
    }

    public Boolean isLogOnly() {
        return logOnly;
    }

    public String getTestSize() {
        return testSize;
    }

    public Boolean isCreateReport() {
        return createReport;
    }

    public List getPackages() {
        return packages;
    }

    public List getClasses() {
        return classes;
    }
}
