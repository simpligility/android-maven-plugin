package com.jayway.maven.plugins.android.configuration;

import java.util.List;

/**
 * Configuration for the integration test runs. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.jayway.maven.plugins.android.standalonemojos.AbstractInstrumentationMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Test {
    /**
     * Package name of the apk we wish to instrument. If not specified, it is inferred from
     * <code>AndroidManifest.xml</code>.
     *
     * @optional
     * @parameter expression="${android.test.instrumentationPackage}
     */
    private String instrumentationPackage;

    /**
     * Class name of test runner. If not specified, it is inferred from <code>AndroidManifest.xml</code>.
     *
     * @optional
     * @parameter expression="${android.test.instrumentationRunner}"
     */
    private String instrumentationRunner;

    /**
     * Enable debug causing the test runner to wait until debugger is
     * connected with the Android debug bridge (adb).
     *
     * @optional
     * @parameter default-value=false expression="${android.test.debug}"
     */
    private boolean debug;


    /**
     * Enable or disable code coverage for this instrumentation test
     * run.
     *
     * @optional
     * @parameter default-value=false expression="${android.test.coverage}"
     */
    private boolean coverage;

    /**
     * Enable this flag to run a log only and not execute the tests.
     *
     * @optional
     * @parameter default-value=false expression="${android.test.logonly}"
     */
    private boolean logOnly;

    /**
     * If specified only execute tests of certain testSize as defined by
     * the Android instrumentation testing SmallTest, MediumTest and
     * LargeTest annotations. Use "small", "medium" or "large" as values.
     *
     * @see com.android.ddmlib.testrunner.IRemoteAndroidTestRunner
     *
     * @optional
     * @parameter expression="${android.test.testsize}"
     */
    private String testSize;

    /**
     * Create a junit xml format compatible output file containing
     * the test results for each device the instrumentation tests run
     * on.
     * <br /><br />
     * The files are stored in target/surefire-reports and named TEST-deviceid.xml.
     * The deviceid for an emulator is deviceSerialNumber_avdName_manufacturer_model.
     * The serial number is commonly emulator-5554 for the first emulator started
     * with numbers increasing. avdName is as defined in the SDK tool. The
     * manufacturer is typically "unknown" and the model is typically "sdk".
     * The deviceid for an actual devices is
     * deviceSerialNumber_manufacturer_model.
     * <br /><br />
     * The file contains system properties from the system running
     * the Maven Android Plugin (JVM) and device properties from the
     * device/emulator the tests are running on.
     * <br /><br />
     * The file contains a single TestSuite for all tests and a
     * TestCase for each test method. Errors and failures are logged
     * in the file and the system log with full stack traces and other
     * details available.
     *
     * @optional
     * @parameter default-value=true expression="${android.test.createreport}"
     */
    private boolean createReport;

    /**
     * <p>Whether to execute tests only in given packages</p>
     * <pre>
     * &lt;test&gt;
     *   &lt;packages&gt;
     *     &lt;package&gt;your.package.name&lt;/package&gt;
     *   &lt;/packages&gt;
     * &lt;/test&gt;
     * </pre>
     *
     * @parameter
     */
    protected List packages;

    /**
     * <p>Whether to execute test classes which are specified.</p>
     * <pre>
     * &lt;test&gt;
     *   &lt;classes&gt;
     *     &lt;class&gt;your.package.name.YourTestClass&lt;/class&gt;
     *   &lt;/classes&gt;
     * &lt;/test&gt;
     * </pre>
     *
     * @parameter
     */
    protected List classes;

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
