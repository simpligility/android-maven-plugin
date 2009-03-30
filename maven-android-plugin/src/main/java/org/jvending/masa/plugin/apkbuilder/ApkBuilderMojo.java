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
package org.jvending.masa.plugin.apkbuilder;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jvending.masa.CommandExecutor;
import org.jvending.masa.ExecutionException;
import org.jvending.masa.plugin.AbstractAndroidMojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the apk file. By default signs it with debug keystore. Change that with configuration parameter
 * <code>isDelaySigned</code>
 * @goal apkbuilderBuild
 * @phase package
 */
public class ApkBuilderMojo extends AbstractAndroidMojo {

    /**
     * <p>Whether to <em>not</em> sign with the debug key.</p>
     * <p><em>TODO: rename to signWithDebugKeystore, and invert meaning of parameter</em></p>
     * @parameter default-value = "false"
     */
    private boolean isDelaySigned;

    public void execute() throws MojoExecutionException, MojoFailureException {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        File outputFile = new File(project.getBuild().getDirectory(),  project.getBuild().getFinalName() + ".apk");

        List<String> commands = new ArrayList<String>();
        commands.add(outputFile.getAbsolutePath());

        if(isDelaySigned) {
            commands.add("-u");
        }
        
        commands.add("-z");
        commands.add(new File(project.getBuild().getDirectory(),  project.getBuild().getFinalName() + ".ap_").getAbsolutePath());
        commands.add("-f");
        commands.add( new File(project.getBuild().getDirectory(),  "classes.dex").getAbsolutePath());
        commands.add("-rf");
        // TODO: This should be src/main/resources instead:
        commands.add(new File(project.getBuild().getSourceDirectory()).getAbsolutePath());
        
        getLog().info("apkbuilder " + commands.toString());
        try {
            executor.executeCommand("apkbuilder", commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

        //Set the generated .apk file as the main artifact (because the pom states <packaging>android:apk</packaging>)
        project.getArtifact().setFile(outputFile);

        //Also attach the normal .jar, so it can be depended on by for example android:apk:platformTest projects if they need access to our R.java and other things.
        projectHelper.attachArtifact(project, "jar", new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".jar"));
    }
}
