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
package org.jvending.masa.plugin.aidl;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jvending.masa.CommandExecutor;
import org.jvending.masa.ExecutionException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @goal generate
 * @requiresProject true
 * @description
 */
public class AidlGeneratorMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(project.getBuild().getSourceDirectory());

        List<String> excludeList = new ArrayList<String>();
        //target files
        excludeList.add("target/**");

        List<String> includeList = new ArrayList<String>();
        includeList.add("**/*.aidl");
        String[] includes = new String[includeList.size()];
        directoryScanner.setIncludes((includeList.toArray(includes)));
        directoryScanner.addDefaultExcludes();

        directoryScanner.scan();
        String[] files = directoryScanner.getIncludedFiles();
        getLog().info("ANDROID-904-002: Found aidl files: Count = " + files.length);
        if (files.length == 0) {
            return;
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        for (String file : files) {
            List<String> commands = new ArrayList<String>();
            commands.add("-p" + System.getenv().get("ANDROID_SDK") + "/tools/lib/framework.aidl");
            commands.add("-I" + project.getBuild().getSourceDirectory());
            commands.add((new File(project.getBuild().getSourceDirectory(), file).getAbsolutePath()));
            try {
                executor.executeCommand("aidl", commands, project.getBasedir(), false);
            } catch (ExecutionException e) {
                throw new MojoExecutionException("", e);
            }

        }
    }
}
