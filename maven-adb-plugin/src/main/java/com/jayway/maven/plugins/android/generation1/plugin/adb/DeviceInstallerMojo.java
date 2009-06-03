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
package com.jayway.maven.plugins.android.generation1.plugin.adb;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import com.jayway.maven.plugins.android.generation1.CommandExecutor;
import com.jayway.maven.plugins.android.generation1.ExecutionException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @goal install
 * @phase install
 * @description
 */
public class DeviceInstallerMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if(System.getProperty("masa.debug") == null) {
            getLog().info("Debug flag not set. Skipping emulator install");
            return;
        }
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        File inputFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".apk");

        List<String> commands = new ArrayList<String>();
        commands.add("install");
        commands.add("-r");
        commands.add(inputFile.getAbsolutePath());
        getLog().info("adb " + commands.toString());
        try {
            executor.executeCommand("adb", commands);
        } catch (ExecutionException e) {
        }
    }
}
