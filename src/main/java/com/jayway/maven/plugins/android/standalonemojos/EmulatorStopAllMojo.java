package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractEmulatorMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * EmulatorStopeAllMojo will stop all attached devices.
 *
 * @author Bryan O'Neil <bryan.oneil@hotmail.com>
 */
@Mojo( name = "emulator-stop-all", requiresProject = false )
public class EmulatorStopAllMojo extends AbstractEmulatorMojo
{

    /**
     * Stop all attached emulators.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        stopAndroidEmulators();
    }
}
