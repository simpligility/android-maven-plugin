package com.simpligility.maven.plugins.android.standalonemojos;

import com.simpligility.maven.plugins.android.AbstractEmulatorMojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * EmulatorStartMojo can stop the Android Emulator with a specified Android Virtual Device (avd).
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
@Mojo( name = "emulator-stop", requiresProject = false )
public class EmulatorStopMojo extends AbstractEmulatorMojo
{

    /**
     * Stop the emulator(s).
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        stopAndroidEmulator();
    }
}
