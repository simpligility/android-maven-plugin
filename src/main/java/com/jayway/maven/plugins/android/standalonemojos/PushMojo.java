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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.DeviceCallback;
import com.jayway.maven.plugins.android.common.LogSyncProgressMonitor;

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
     * If you specify a directory, all containing files will be pushed recursively.
     *
     * @parameter expression="${android.push.source}"
     * @required
     */
    private File source;

    /**
     * The destination file name as absolute path on the emulator or device.
     * If the last character is a "/" it will be assumed that the original
     * base filename should be preserved and a target directory is specified.
     * This works analogous if the source is a directory.
     *
     * @parameter expression="${android.push.destination}"
     * @required
     */
    private String destination;

    public void execute() throws MojoExecutionException, MojoFailureException {
        
        final Map<String, String> sourceDestinationMap = calculateSourceDestinationMapping();
        
        doWithDevices(new DeviceCallback() {
            public void doWithDevice(final IDevice device) throws MojoExecutionException {
                // message will be set in for each loop according to the processed files
                String message = "";
                
                try {
                    SyncService syncService = device.getSyncService();
                    
                    for (Map.Entry<String, String> pushFileEntry : sourceDestinationMap.entrySet()) {
                        String sourcePath = pushFileEntry.getKey();
                        String destinationPath = pushFileEntry.getValue();
                        
                        message = "Push of " + sourcePath + " to " +
                        destinationPath + " on ";

                        syncService.pushFile(sourcePath, destinationPath,
                                new LogSyncProgressMonitor(getLog()));
                        
                        getLog().info(message + device.getSerialNumber()
                                + " (avdName=" + device.getAvdName() + ") successful.");
                    }
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

    /**
     * Calculates a map which contains all files to be pushed to the device or
     * emulator. The source filename works as the key while the value is the
     * destination.
     * 
     * @return a map with file source -> destination pairs
     * @throws MojoExecutionException
     */
    private Map<String, String> calculateSourceDestinationMapping()
            throws MojoExecutionException {
        Map<String, String> result = new HashMap<String, String>();

        final String destinationPath;
        if (destination.endsWith("/")) {
            destinationPath = destination + source.getName();
        } else {
            destinationPath = destination;
        }

        if (source.isFile()) {
            // only put the source in
            final String sourcePath = source.getAbsolutePath();
            result.put(sourcePath, destinationPath);
        } else if (source.isDirectory()) {
            // find recursively all files to be pushed
            @SuppressWarnings("unchecked")
            Collection<File> filesList = FileUtils.listFiles(source, null, true);
            for (File file : filesList) {
                // make the file's path relative - this is kind of a hack but it
                // works just fine in this controlled environment
                String filePath = file.getAbsolutePath().substring(
                        source.getAbsolutePath().length());

                result.put(file.getAbsolutePath(), destinationPath + filePath);
            }
        } else {
            throw new MojoExecutionException(
                    "Cannot execute push goal: File or directory "
                            + source.getAbsolutePath() + " does not exist.");
        }
        return result;
    }
}
