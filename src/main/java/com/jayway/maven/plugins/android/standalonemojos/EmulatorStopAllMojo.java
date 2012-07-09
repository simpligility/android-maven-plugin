package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractEmulatorMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * EmulatorStopeAllMojo will stop all attached devices.
 *
 * @author Bryan O'Neil <bryan.oneil@hotmail.com>
 * @goal emulator-stop-all
 * @requiresProject false
 */
public class EmulatorStopAllMojo extends AbstractEmulatorMojo
{

    /**
     * Start the Android Emulator.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *
     * @throws org.apache.maven.plugin.MojoFailureException
     *
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        stopAndroidEmulators();
    }
}
