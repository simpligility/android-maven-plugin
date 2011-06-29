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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.android.ddmlib.IDevice;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.DeviceCallback;
import com.jayway.maven.plugins.android.ExecutionException;

/**
 * Copy file/dir to device.
 *
 * @goal push
 * @requiresProject false
 */
public class PushMojo extends AbstractAndroidMojo {

    /**
     * @parameter expression="${android.source}"
     * @required
     */
    private File source;

    /**
     * @parameter expression="${android.destination}"
     * @required
     */
    private String destination;

    public void execute() throws MojoExecutionException, MojoFailureException {

        final String pathForAdb = getAndroidSdk().getAdbPath();
        final CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        doWithDevices(new DeviceCallback() {
            public void doWithDevice(final IDevice device) throws MojoExecutionException {
                final List<String> commands = new ArrayList<String>();
                addDeviceParameter(commands, device);
                commands.add("push");
                commands.add(source.getAbsolutePath());
                commands.add(destination);

                getLog().info(pathForAdb + " " + commands.toString());
                try {
                    executor.executeCommand(pathForAdb, commands, null, false);
                } catch (ExecutionException e) {
                    throw new MojoExecutionException("Pull failed.", e);
                }
            }
        });

    }
}
