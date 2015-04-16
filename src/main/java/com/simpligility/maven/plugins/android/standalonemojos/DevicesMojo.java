package com.simpligility.maven.plugins.android.standalonemojos;

import com.android.ddmlib.IDevice;
import com.simpligility.maven.plugins.android.AbstractAndroidMojo;
import com.simpligility.maven.plugins.android.DeviceCallback;
import com.simpligility.maven.plugins.android.common.DeviceHelper;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * DevicesMojo lists all attached devices and emulators found with the android debug bridge. It uses the same
 * naming convention for the emulator as used in other places in the Android Maven Plugin and adds the status
 * of the device in the list.
 * <p/>
 * TODO The goal is very simple and could be enhanced for better display, a verbose option to display and to take the
 * android.device paramter into account.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
@Mojo( name = "devices", requiresProject = false )
public class DevicesMojo extends AbstractAndroidMojo
{
    /**
     * Display a list of attached devices.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        doWithDevices( new DeviceCallback()
        {
            public void doWithDevice( final IDevice device ) throws MojoExecutionException
            {
                getLog().info( DeviceHelper.getDescriptiveNameWithStatus( device ) );
            }
        } );
    }
}
