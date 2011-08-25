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
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.DeviceCallback;

/**
 * Copy file from all the attached (or specified) devices/emulators.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 *
 * @goal pull
 * @requiresProject false
 */
public class PullMojo extends AbstractAndroidMojo {

    /**
     * The path of the source file on the emulator/device.
     * @parameter expression="${android.pull.source}"
     * @required
     */
    private String source;

    /**
     * The path of the destination to copy the file to. If this is a
     * directory the original file name will be used. Otherwise the new
     * filename will override the source one.
     *
     * @parameter expression="${android.pull.destination}"
     * @required
     */
    private File destination;

	/**
	 * Create destination directory if it doesn't exist (using
	 * File.mkdirs()). Because we don't know whether the specified
	 * destination is a directory or file (unless it exists in
	 * which case this flag is ignored) we assume that unless
	 * the destination explicity ends with / it will be treated
	 * as a file (and removed to get the destination directory)
	 *
	 * @parameter default-value=false expression="${android.pull.createDestDir}"
	 */
	private Boolean createDestDir;
	
    public void execute() throws MojoExecutionException, MojoFailureException {
        final String sourcePath = source;
        final String destinationPath;
        if(destination.isDirectory()) {
            String sourceFileName = source.substring(source.lastIndexOf("/"),
                source.length());
            destinationPath = destination.getAbsolutePath() + sourceFileName;
        } else {
            destinationPath = destination.getAbsolutePath();
        }
	    if (createDestDir && !destination.exists()) {
		    String destPath = destination.getAbsolutePath();
		    destPath = FilenameUtils.getFullPath(destPath);
		    File destFile = new File(destPath);
		    if (!destFile.exists()) {
				getLog().info("Creating destination directory " + destFile);
			    destFile.mkdirs();
		    }
        }

        final String message = "Pull of " + source + " to " + destinationPath +
            " from ";

        doWithDevices(new DeviceCallback() {
            public void doWithDevice(final IDevice device) throws MojoExecutionException {
                 try {
                    SyncService syncService = device.getSyncService();
                    syncService.pullFile(sourcePath, destinationPath,
                        new LogSyncProgressMonitor(getLog()));
                    getLog().info(message + device.getSerialNumber()
                        + " (avdName=" + device.getAvdName() + ") successful.");

                    //TODO this could be enhanced to pull multiple files and
                    // even directories using the pull method. would need
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
