package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
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
public class EmulatorStopMojo extends AbstractEmulatorMojo {

    /**
     * Start the Android Emulator.
     * @throws org.apache.maven.plugin.MojoExecutionException
     * @throws org.apache.maven.plugin.MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        stopAndroidEmulator();
    }
}
