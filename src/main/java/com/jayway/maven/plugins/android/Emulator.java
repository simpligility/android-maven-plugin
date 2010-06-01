package com.jayway.maven.plugins.android;

/**
 * Configuration for the Android Emulator. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.AbstractAndroidMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Emulator {

    /**
     * Name of the Android Virtual Device (avd) that will be started by the emulator. Default value is "Default"
     * @parameter expression="${android.emulator.avd}"
     */
    private String avd;

    /**
     * Wait time for the emulator start up.
     * @parameter expression="${android.emulator.wait}"
     *
     */
    private String wait;

    /**
     * Additional command line options for the emulator start up. This option can be used to pass any additional
     * options desired to the invocation of the emulator. Use emulator -help for more details. An example would be
     * "-no-skin".
     * @parameter expression="${android.emulator.options}"
     */
    private String options;

    public String getAvd() {
        return avd;
    }

    public String getWait() {
        return wait;
    }

    public String getOptions() {
        return options;
    }
}
