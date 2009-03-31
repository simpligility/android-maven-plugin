/*
 * Copyright (C) 2007-2008 JVending Masa
 * Copyright (C) 2009 Jayway AB
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
package com.jayway.maven.plugins.android.phase01generatesources;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates java files based on aidl files.
 * @goal generateAidl
 * @requiresProject true
 * @author hugo.josefson@jayway.com
 */
public class GenerateAidlMojo extends AbstractAndroidMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(project.getBuild().getSourceDirectory());

        //TODO: this exclusion should not be needed if project.getBuild().getSourceDirectory() defaults to src/main/java 
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

        File generatedSourcesDirectory = new File(project.getBuild().getDirectory() + File.separator
                + "generated-sources" + File.separator + "aidl");
        generatedSourcesDirectory.mkdirs();

        for (String file : files) {
            List<String> commands = new ArrayList<String>();
            // TODO: DON'T use System.getenv for this! Use proper plugin configuration parameters,
            // TODO: (which may pull from environment/ANDROID_SDK for their default values.)
            if (System.getenv().get("ANDROID_SDK") != null) {
                commands.add("-p" + System.getenv().get("ANDROID_SDK") + "/tools/lib/framework.aidl");
            }
            File targetDirectory = new File(generatedSourcesDirectory, new File(file).getParent());
            targetDirectory.mkdirs();

            String fileName = new File(file).getName();

            commands.add("-I" + project.getBuild().getSourceDirectory());
            commands.add((new File(project.getBuild().getSourceDirectory(), file).getAbsolutePath()));
            commands.add(new File(targetDirectory , fileName.substring(0, fileName.lastIndexOf(".")) + ".java").getAbsolutePath());
            try {
                executor.executeCommand("aidl", commands, project.getBasedir(), false);
            } catch (ExecutionException e) {
                throw new MojoExecutionException("", e);
            }
        }

        project.addCompileSourceRoot(generatedSourcesDirectory.getPath());
        
    }
}
