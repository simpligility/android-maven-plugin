/*
 * Copyright (C) 2007-2008 JVending Masa
 * Copyright (C) 2011 Jayway AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.io.IOException;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
import com.jayway.maven.plugins.android.common.LogSyncProgressMonitor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.DeviceCallback;

/**
 * Copy file to all the attached (or specified) devices/emulators.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 *
 * @goal push
 * @requiresProject false
 */
public class PushMojo extends AbstractAndroidMojo {

    /**
     * The file name of the local filesystem file to push to the emulator or
     * device either as absolute path or relative to the execution folder.
     *
     * @parameter expression="${android.push.source}"
     * @required
     */
    private File source;

    /**
     * The destination file name as absolute path on the emulator or device.
     * If the last character is a "/" it will be assumed that the original
     * base filename should be preserved and a target directory is specified.
     *
     * @parameter expression="${android.push.destination}"
     * @required
     */
    private String destination;

    public void execute() throws MojoExecutionException, MojoFailureException {

        final String sourcePath = source.getAbsolutePath();
        final String destinationPath;
        if (destination.endsWith("/")) {
            destinationPath = destination + source.getName();
        } else {
            destinationPath = destination;
        }
        final String message = "Push of " + sourcePath + " to " +
            destinationPath + " on ";

        doWithDevices(new DeviceCallback() {
            public void doWithDevice(final IDevice device) throws MojoExecutionException {
                try {
                    SyncService syncService = device.getSyncService();
                    syncService.pushFile(sourcePath, destinationPath,
                        new LogSyncProgressMonitor(getLog()));
                    getLog().info(message + device.getSerialNumber()
                        + " (avdName=" + device.getAvdName() + ") successful.");

                    //TODO this could be enhanced to push multiple files and
                    // even directories using the push method. would need
                    // different parameters though so maybe later
                } catch (SyncException e) {
                    throw new MojoExecutionException(message
                        + device.getSerialNumber()  + " (avdName="
                        + device.getAvdName() + ") failed.", e);
                } catch (IOException e) {
                    throw new MojoExecutionException(message
                        + device.getSerialNumber()  + " (avdName="
                        + device.getAvdName() + ") failed.", e);
                } catch (TimeoutException e) {
                    throw new MojoExecutionException(message
                        + device.getSerialNumber()  + " (avdName="
                        + device.getAvdName() + ") failed.", e);
                } catch (AdbCommandRejectedException e) {
                    throw new MojoExecutionException(message
                        + device.getSerialNumber()  + " (avdName="
                        + device.getAvdName() + ") failed.", e);
                }
            }
        });
    }
}
