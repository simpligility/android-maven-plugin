/*
 * Copyright (C) 2009 Jayway AB
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
package com.jayway.maven.plugins.android.phase08package;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the apk file. By default signs it with debug keystore.<br/>
 * Change that by setting configuration parameter <code>signWithDebugKeystore</code> to <code>false</code>.
 * @goal apk
 * @phase package
 * @author hugo.josefson@jayway.com
 */
public class ApkMojo extends AbstractAndroidMojo {

    /**
     * <p>Whether to sign with the debug key.</p>
     * @parameter default-value = "true"
     */
    private boolean signWithDebugKeystore;

    public void execute() throws MojoExecutionException, MojoFailureException {

        generateIntermediateAp_();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        File outputFile = new File(project.getBuild().getDirectory(),  project.getBuild().getFinalName() + ".apk");

        List<String> commands = new ArrayList<String>();
        commands.add(outputFile.getAbsolutePath());

        if(!signWithDebugKeystore) {
            commands.add("-u");
        }
        
        commands.add("-z");
        commands.add(new File(project.getBuild().getDirectory(),  project.getBuild().getFinalName() + ".ap_").getAbsolutePath());
        commands.add("-f");
        commands.add( new File(project.getBuild().getDirectory(),  "classes.dex").getAbsolutePath());
        commands.add("-rf");
        // TODO: This should be src/main/resources instead:
        commands.add(new File(project.getBuild().getSourceDirectory()).getAbsolutePath());
        
        getLog().info(getAndroidSdk().getPathForTool("apkbuilder")+" " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("apkbuilder"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

        // Set the generated .apk file as the main artifact (because the pom states <packaging>apk</packaging>)
        project.getArtifact().setFile(outputFile);

        // Also attach the normal .jar, so it can be depended on by for example an Android platformtest projects if they need access to our R.java and other things.
        projectHelper.attachArtifact(project, "jar", new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".jar"));
    }

    /**
     * Generates an intermediate apk file (actually .ap_) containing the resources and assets.
     * @throws MojoExecutionException
     */
    private void generateIntermediateAp_() throws MojoExecutionException {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        File androidJar = getAndroidSdk().getAndroidJar();
        File outputFile = new File(project.getBuild().getDirectory(),  project.getBuild().getFinalName() + ".ap_");

        List<String> commands = new ArrayList<String>();
        commands.add("package");
        commands.add("-f");
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
        commands.add("-F");
        commands.add(outputFile.getAbsolutePath());
        getLog().info(getAndroidSdk().getPathForTool("aapt")+" " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("aapt"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }
}
