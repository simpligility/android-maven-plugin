package com.jayway.maven.plugins.android;

/**
 * For integrationtest related Mojos.
 * 
 * @author hugo.josefson@jayway.com
 */
public abstract class AbstractIntegrationtestMojo extends AbstractAndroidMojo {
    /**
     * -Dmaven.test.skip is commonly used with Maven to skip tests. We honor it too.
     *
     * @parameter expression="${maven.test.skip}" default-value=false
     * @readonly
     */
    private boolean mavenTestSkip;

    /**
     * Enables integration test related goals. If <code>false</code>, they will be skipped.
     * @parameter expression="${android.enableIntegrationTest}" default-value=true
     */
    private boolean enableIntegrationTest;

    /**
     * Whether or not to execute integration test related goals. Reads from configuration parameter
     * <code>enableIntegrationTest</code>, but can be overridden with <code>-Dmaven.test.skip</code>.
     * @return <code>true</code> if integration test goals should be executed, <code>false</code> otherwise.
     */
    protected boolean isEnableIntegrationTest() {
        if (mavenTestSkip){
            return false;
        }

        return enableIntegrationTest;
    }
}
