/*
 * Copyright (C) 2007-2008 JVending Masa
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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
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

    /**
     * Specifies which device to connect to, by serial number. Special values "usb" and "emulator" are also valid, for
     * selecting the only USB connected device or the only running emulator, respectively.
     *
     * @parameter expression="${android.device}"
     */
    protected String device;

    public void execute() throws MojoExecutionException, MojoFailureException {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        List<String> commands = new ArrayList<String>();

        // Check if a specific device should be used
        if (StringUtils.isNotBlank(device)) {
            if ("usb".equals(device)) {
                commands.add("-d");
            } else if ("emulator".equals(device)) {
                commands.add("-e");
            } else {
                commands.add("-s");
                commands.add(device);
            }
        }

        commands.add("push");
        commands.add(source.getAbsolutePath());
        commands.add(destination);

        final String pathForAdb = getAndroidSdk().getPathForTool("adb");
        getLog().info(pathForAdb + " " + commands.toString());
        try {
            executor.executeCommand(pathForAdb, commands, null, false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("Push failed.", e);
        }
    }
}
