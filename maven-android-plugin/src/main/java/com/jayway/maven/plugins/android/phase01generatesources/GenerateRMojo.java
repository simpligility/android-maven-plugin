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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates <code>R.java</code> based on resources specified by the <code>resources</code> configuration parameter.<br/>
 * As a side-effect, also deletes any <code>Thumbs.db</code> files found in the resource directory.<br/>
 * @goal generateR
 * @phase generate-sources
 * @requiresDependencyResolution compile
 * @author hugo.josefson@jayway.com
 */
public class GenerateRMojo extends AbstractAndroidMojo {

    /**
     * Make package directories in the directory where files are copied to.
     * @parameter default-value=true
     */
    private boolean createPackageDirectories;

    public void execute() throws MojoExecutionException, MojoFailureException {
        // System.out.println("RS = " + resourceDirectory.getAbsolutePath());
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        // TODO: don't mess with the resource directory directly,
        // TODO: but copy it to target,
        // TODO: delete Thumbs.db there,
        // TODO: and then generate R.java from that directory 
        //Get rid of this annoying Thumbs.db problem on windows
        File thumbs = new File(resourceDirectory, "drawable/Thumbs.db");
        if (thumbs.exists()) {
            getLog().info("Deleting thumbs.db from resource directory");
            thumbs.delete();
        }
        
        String generatedSourceDirectoryName = project.getBuild().getDirectory() + File.separator + "generated-sources"
                + File.separator + "r";
        new File(generatedSourceDirectoryName).mkdirs();

        File androidJar = resolveAndroidJar();

        List<String> commands = new ArrayList<String>();
        commands.add("package");
        if (createPackageDirectories) {
            commands.add("-m");
        }
        commands.add("-J");
        commands.add(generatedSourceDirectoryName);
        commands.add("-M");
        commands.add(androidManifestFile.getAbsolutePath());
        if (resourceDirectory.exists()) {
            commands.add("-S");
            commands.add(resourceDirectory.getAbsolutePath());
        }
        if (assetsDirectory.exists()) {
            commands.add("-A");
            commands.add(assetsDirectory.getAbsolutePath());
        }
        commands.add("-I");
        commands.add(androidJar.getAbsolutePath());
        getLog().info("aapt " + commands.toString());
        try {
            executor.executeCommand("aapt", commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

        project.addCompileSourceRoot(generatedSourceDirectoryName);

    }
}
