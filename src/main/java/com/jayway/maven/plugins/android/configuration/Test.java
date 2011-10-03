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
    private boolean debug;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private boolean coverage;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private boolean logOnly;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private String testSize;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.AbstractInstrumentationMojo#testSkip}
      */
    private boolean createReport;
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

    public boolean isDebug() {
        return debug;
    }

    public boolean isCoverage() {
        return coverage;
    }

    public boolean isLogOnly() {
        return logOnly;
    }

    public String getTestSize() {
        return testSize;
    }

    public boolean isCreateReport() {
        return createReport;
    }

    public List getPackages() {
        return packages;
    }

    public List getClasses() {
        return classes;
    }
}
