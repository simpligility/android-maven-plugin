package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractEmulatorMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * EmulatorStartMojo can start the Android Emulator with a specified Android Virtual Device (avd).
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
@Mojo( name = "emulator-start", requiresProject = false )
public class EmulatorStartMojo extends AbstractEmulatorMojo
{

    /**
     * Start the Android Emulator.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        startAndroidEmulator();
    }

}
