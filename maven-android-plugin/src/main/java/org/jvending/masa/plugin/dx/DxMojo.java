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
package org.jvending.masa.plugin.dx;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;
import org.jvending.masa.CommandExecutor;
import org.jvending.masa.ExecutionException;
import org.jvending.masa.plugin.AbstractAndroidMojo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @goal dx
 * @phase process-classes
 * @description
 */
public class DxMojo extends AbstractAndroidMojo {

    /**
     * Extra JVM Arguments
     *
     * @parameter
     * @optional
     */
    private String[] jvmArguments;

    public void execute() throws MojoExecutionException, MojoFailureException {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        File outputFile = new File(project.getBuild().getDirectory() + File.separator + "classes.dex");
        File inputFile  = new File(project.getBuild().getDirectory() + File.separator + project.getBuild().getFinalName() + ".jar");

        //Unpackage all dependent and main classes
        File outputDirectory = new File(project.getBuild().getDirectory(), "android-classes");
        for (Artifact artifact : (List<Artifact>) project.getCompileArtifacts()) {
            if (artifact.getGroupId().equals("android")) {
                continue;
            }
            
            if(artifact.getFile().isDirectory())  {
                throw new MojoExecutionException("Dependent artifact is directory: Directory = "
                        + artifact.getFile().getAbsolutePath());
            }

            try {
                unjar(new JarFile(artifact.getFile()), outputDirectory);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to jar file: File = " + artifact.getFile().getAbsolutePath(), e);
            }
        }

        try {
            unjar(new JarFile(inputFile), outputDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("", e);
        }

        List<String> commands = new ArrayList<String>();
        if (jvmArguments != null){
            for (String jvmArgument : jvmArguments) {
                if (jvmArgument != null){
                    if (jvmArgument.startsWith("-")){
                        jvmArgument = jvmArgument.substring(1);
                    }
                    commands.add("-J" + jvmArgument);
                }
            }
        }
        commands.add("--dex");
        commands.add("--output=" + outputFile.getAbsolutePath());
        commands.add(outputDirectory.getAbsolutePath());
        getLog().info("dx " + commands.toString());
        try {
            executor.executeCommand("dx", commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

        projectHelper.attachArtifact(project, "jar", project.getArtifact().getClassifier(), inputFile);
    }

    private static void unjar(JarFile jarFile, File outputDirectory) throws IOException {
        for (Enumeration en = jarFile.entries(); en.hasMoreElements();) {
            JarEntry entry = (JarEntry) en.nextElement();
            File entryFile = new File(outputDirectory, entry.getName());
            if (!entryFile.getParentFile().exists() && !entry.getName().startsWith("META-INF")) {
                entryFile.getParentFile().mkdirs();
            }
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                IOUtil.copy(jarFile.getInputStream(entry), new FileOutputStream(entryFile));
            }
        }
    }
}
