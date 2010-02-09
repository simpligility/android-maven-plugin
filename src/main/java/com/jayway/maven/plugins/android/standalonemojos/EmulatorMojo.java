package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;



import java.util.ArrayList;
import java.util.List;

/**
 * Emulator Mojo provides features to start the Android Emulator with a specified Android Virtual Device (avd)
 * @author Manfred Moser <manfred@simpligility.com>
 * @goal emulator
 * @requiresProject false
 */
public class EmulatorMojo extends AbstractAndroidMojo {

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
    private long wait = 5000;

    /**
     * Additional command line options for the emulator start up.
     * @parameter expression="${android.emulator.options}"
     */
    private String options;

    /**
     * Start the emulator.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     * @todo maybe attach this mojo to a phase with a wrapper mojo (but maybe make it optional somehow)
     * @todo not start emulator if one is already running
     * @todo terminate emulator (this might be with another mojo and another wrapper for the correct phase)
     */
    public void execute() throws MojoExecutionException, MojoFailureException {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        String androidVirtualDevice;
        if (StringUtils.isEmpty(avd))
        {
            androidVirtualDevice = "Default";
        }
        else
        {
            androidVirtualDevice = avd;
        }

        List<String> commands = new ArrayList<String>();
        commands.add("-avd");
        commands.add(androidVirtualDevice);
        if (!StringUtils.isEmpty(options))
        {
            commands.add(options);
        }

        getLog().info(getAndroidSdk().getPathForTool("emulator")+" " + commands.toString());

        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("emulator"), commands);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

         // wait for the emulator to start up 
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            // swallow
        }
    }
}
