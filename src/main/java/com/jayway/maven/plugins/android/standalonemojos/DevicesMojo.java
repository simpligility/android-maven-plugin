package com.jayway.maven.plugins.android.standalonemojos;

import com.android.ddmlib.IDevice;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.DeviceCallback;
import com.jayway.maven.plugins.android.common.DeviceHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * DevicesMojo lists all attached devices and emulators found with the android debug bridge.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 * @goal devices
 * @requiresProject false
 */
public class DevicesMojo extends AbstractAndroidMojo {
    /**
     * Display a list of attached devices.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     * @throws org.apache.maven.plugin.MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        doWithDevices(
            new DeviceCallback() {
                public void doWithDevice(final IDevice device) throws MojoExecutionException {
                    getLog().info(DeviceHelper.getDescriptiveNameWithStatus(device));
                }
            }
        );
    }

}
