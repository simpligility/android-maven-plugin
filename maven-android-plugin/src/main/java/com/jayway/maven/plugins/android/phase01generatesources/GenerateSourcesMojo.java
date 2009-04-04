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
 * Generates java files based on aidl files.<br/>
 * If the configuration parameter <code>deleteConflictingFiles</code> is <code>true</code> (which it is by default), this
 * goal has the following side-effect:
 * <ul>
 * <li>deletes any <code>.java</code> files with the same name as an <code>.aidl</code> file found in the source
 * directory.</li>
 * </ul>
 * Generates <code>R.java</code> based on resources specified by the <code>resources</code> configuration parameter.<br/>
 * If the configuration parameter <code>deleteConflictingFiles</code> is <code>true</code> (which it is by default), this
 * goal has the following side-effects:
 * <ul>
 * <li>deletes any <code>Thumbs.db</code> files found in the resource directory.</li>
 * <li>deletes any <code>R.java</code> files found in the source directory.</li>
 * </ul>
 * @goal generate-sources
 * @phase generate-sources
 * @requiresProject true
 * @author hugo.josefson@jayway.com
 */
public class GenerateSourcesMojo extends AbstractAndroidMojo {

    /**
     * Make package directories in the directory where files are copied to.
     * @parameter default-value=true
     */
    private boolean createPackageDirectories;
    
    /**
     * <p>Whether to delete any <code>R.java</code> file, and <code>.java</code> files with the same name as
     * <code>.aidl</code> files, found in the source directory.</p>
     *
     * <p>Enable when using Eclipse and the standard Eclipse Android plugin, to work around the fact that it creates
     * <code>R.java</code>, and <code>.java</code> files from your <code>.aidl</code> files, in the wrong place
     * (from a Maven perspective.) Don't worry, Eclipse automatically recreates them when you refresh the Eclipse
     * project.</p>
     *
     * @parameter default-value=true
     *            expression="${android.deleteConflictingFiles}"
     *
     */
    protected boolean deleteConflictingFiles;

    public void execute() throws MojoExecutionException, MojoFailureException {

        deleteConflictingRFiles();
        generateR();
        
        generateAidl();
    }

    private void deleteConflictingRFiles() throws MojoExecutionException {
        if (deleteConflictingFiles){
            final int numberOfRFilesDeleted = deleteFilesFromDirectory(project.getBuild().getSourceDirectory(), "**/R.java");
            if (numberOfRFilesDeleted > 0){
                getLog().info("Deleted " + numberOfRFilesDeleted + " conflicting R.java file(s) in source directory. If you use Eclipse, please Refresh (F5) the project to regain it.");
            }

            //Get rid of this annoying Thumbs.db problem on windows
            File thumbs = new File(resourceDirectory, "drawable/Thumbs.db");
            if (thumbs.exists()) {
                getLog().info("Deleting thumbs.db from resource directory");
                thumbs.delete();
            }
        }
    }

    private void generateR() throws MojoExecutionException {



        String generatedSourcesRDirectoryName = project.getBuild().getDirectory() + File.separator + "generated-sources" + File.separator + "r";
        new File(generatedSourcesRDirectoryName).mkdirs();

        File androidJar = resolveAndroidJar();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        List<String> commands = new ArrayList<String>();
        commands.add("package");
        if (createPackageDirectories) {
            commands.add("-m");
        }
        commands.add("-J");
        commands.add(generatedSourcesRDirectoryName);
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

        project.addCompileSourceRoot(generatedSourcesRDirectoryName);
    }

    private void generateAidl() throws MojoExecutionException {
        final String sourceDirectory = project.getBuild().getSourceDirectory();

        String[] relativeAidlFileNames = findFilesInDirectory(sourceDirectory, "**/*.aidl");
        getLog().info("ANDROID-904-002: Found aidl files: Count = " + relativeAidlFileNames.length);
        if (relativeAidlFileNames.length == 0) {
            return;
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        File generatedSourcesAidlDirectory = new File(project.getBuild().getDirectory() + File.separator + "generated-sources" + File.separator + "aidl");
        generatedSourcesAidlDirectory.mkdirs();

        int numberOfAidlJavaFilesDeleted = 0;
        for (String relativeAidlFileName : relativeAidlFileNames) {
            List<String> commands = new ArrayList<String>();
            // TODO: DON'T use System.getenv for this! Use proper plugin configuration parameters,
            // TODO: (which may pull from environment/ANDROID_SDK for their default values.)
            if (System.getenv().get("ANDROID_SDK") != null) {
                commands.add("-p" + System.getenv().get("ANDROID_SDK") + "/tools/lib/framework.aidl");
            }
            File targetDirectory = new File(generatedSourcesAidlDirectory, new File(relativeAidlFileName).getParent());
            targetDirectory.mkdirs();

            final String shortAidlFileName         = new File(relativeAidlFileName).getName();
            final String shortJavaFileName         = shortAidlFileName.substring(0, shortAidlFileName.lastIndexOf("."))       + ".java";
            final String relativeJavaFileName      = relativeAidlFileName.substring(0, relativeAidlFileName.lastIndexOf(".")) + ".java";
            final File   aidlFileInSourceDirectory = new File(sourceDirectory, relativeAidlFileName);

            if (deleteConflictingFiles) {
                final File javaFileInSourceDirectory = new File(sourceDirectory, relativeJavaFileName);

                if (javaFileInSourceDirectory.exists()) {
                    final boolean successfullyDeleted = javaFileInSourceDirectory.delete();
                    if (successfullyDeleted) {
                        numberOfAidlJavaFilesDeleted++;
                    } else {
                        throw new MojoExecutionException("Failed to delete \"" + javaFileInSourceDirectory + "\"");
                    }
                }
            }

            commands.add("-I" + sourceDirectory);
            commands.add(aidlFileInSourceDirectory.getAbsolutePath());
            commands.add(new File(targetDirectory , shortJavaFileName).getAbsolutePath());
            try {
                executor.executeCommand("aidl", commands, project.getBasedir(), false);
            } catch (ExecutionException e) {
                throw new MojoExecutionException("", e);
            }
        }

        if (numberOfAidlJavaFilesDeleted > 0){
            getLog().info("Deleted " + numberOfAidlJavaFilesDeleted + " conflicting aidl-generated *.java file(s) in source directory. If you use Eclipse, please Refresh (F5) the project to regain them.");
        }

        project.addCompileSourceRoot(generatedSourcesAidlDirectory.getPath());
    }
}
