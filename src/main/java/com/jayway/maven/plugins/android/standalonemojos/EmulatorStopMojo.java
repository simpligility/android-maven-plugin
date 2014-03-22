package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractEmulatorMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * EmulatorStartMojo can stop the Android Emulator with a specified Android Virtual Device (avd).
 *
 * @author Manfred Moser <manfred@simpligility.com>
 * @goal emulator-stop
 * @requiresProject false
 */
public class EmulatorStopMojo extends AbstractEmulatorMojo
{

    /**
     * Stop the emulator(s).
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void doExecute() throws MojoExecutionException, MojoFailureException
    {
        stopAndroidEmulator();
    }
}
