package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the Android Emulator. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.AbstractAndroidMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Emulator
{

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractEmulatorMojo#emulatorAvd}
     */
    private String avd;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractEmulatorMojo#emulatorWait}
     */
    private String wait;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.AbstractEmulatorMojo#emulatorOptions}
     */
    private String options;

    public String getAvd()
    {
        return avd;
    }

    public String getWait()
    {
        return wait;
    }

    public String getOptions()
    {
        return options;
    }
}
