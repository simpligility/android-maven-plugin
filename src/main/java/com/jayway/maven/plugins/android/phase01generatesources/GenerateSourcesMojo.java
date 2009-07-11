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
 * Generates java files based on aidl files.<br/>
 * <br/>
 * If the configuration parameter <code>deleteConflictingFiles</code> is <code>true</code> (which it is by default), this
 * goal has the following side-effects:
 * <ul>
 * <li>deletes any <code>R.java</code> files found in the source directory.</li>
 * <li>deletes any <code>.java</code> files with the same name as an <code>.aidl</code> file found in the source
 * directory.</li>
 * <li>deletes any <code>Thumbs.db</code> files found in the resource directory.</li>
 * </ul>
 * @goal generate-sources
 * @phase generate-sources
 * @requiresProject true
 * @author hugo.josefson@jayway.com
 */
public class GenerateSourcesMojo extends AbstractAndroidMojo {

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
        String[] relativeAidlFileNames = findRelativeAidlFileNames();

        if (deleteConflictingFiles) {
            deleteConflictingRJavaFiles       (                     );
            deleteConflictingManifestJavaFiles(                     );
            deleteConflictingThumbsDb         (                     );
            deleteConflictingAidlJavaFiles    (relativeAidlFileNames);
        }

        generateR        (                     );
        generateAidlFiles(relativeAidlFileNames);

    }

    private void deleteConflictingManifestJavaFiles() throws MojoExecutionException {
        final int numberOfFilesDeleted = deleteFilesFromDirectory(sourceDirectory, "**/Manifest.java");
        if (numberOfFilesDeleted > 0) {
            getLog().info("Deleted " + numberOfFilesDeleted + " conflicting Manifest.java file(s) in source directory. If you use Eclipse, please Refresh (F5) the project to regain it.");
        }
    }
    private void deleteConflictingRJavaFiles() throws MojoExecutionException {
        final int numberOfFilesDeleted = deleteFilesFromDirectory(sourceDirectory, "**/R.java");
        if (numberOfFilesDeleted > 0) {
            getLog().info("Deleted " + numberOfFilesDeleted + " conflicting R.java file(s) in source directory. If you use Eclipse, please Refresh (F5) the project to regain it.");
        }
    }

    private void deleteConflictingThumbsDb() throws MojoExecutionException {
        //Get rid of this annoying Thumbs.db problem on windows
        final int numberOfFilesDeleted = deleteFilesFromDirectory(resourceDirectory, "**/Thumbs.db");
        if (numberOfFilesDeleted > 0) {
            getLog().info("Deleted " + numberOfFilesDeleted + " Thumbs.db file(s) in resource directory.");
        }
    }

    private void deleteConflictingAidlJavaFiles(String[] relativeAidlFileNames) throws MojoExecutionException {
        for (String relativeAidlFileName : relativeAidlFileNames) {

            final String relativeJavaFileName                 = relativeAidlFileName.substring(0, relativeAidlFileName.lastIndexOf(".")) + ".java";


            final File   conflictingJavaFileInSourceDirectory = new File(sourceDirectory, relativeJavaFileName);

            if (conflictingJavaFileInSourceDirectory.exists()) {
                final boolean successfullyDeleted = conflictingJavaFileInSourceDirectory.delete();
                if (successfullyDeleted) {
                    getLog().info("Deleted conflicting file in source directory: \"" + conflictingJavaFileInSourceDirectory + "\". If you use Eclipse, please Refresh (F5) the project to regain them.");
                }else {
                    throw new MojoExecutionException("Failed to delete conflicting file in source directory: \"" + conflictingJavaFileInSourceDirectory + "\"");
                }
            }


        }

    }

    private void generateR() throws MojoExecutionException {

        String generatedSourcesRDirectoryName = project.getBuild().getDirectory() + File.separator + "generated-sources" + File.separator + "r";
        new File(generatedSourcesRDirectoryName).mkdirs();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        List<String> commands = new ArrayList<String>();
        commands.add("package");
        commands.add("-m");
        commands.add("-J"                                 );
        commands.add(generatedSourcesRDirectoryName       );
        commands.add("-M"                                 );
        commands.add(androidManifestFile.getAbsolutePath());
        if (resourceDirectory.exists()) {
            commands.add("-S"                               );
            commands.add(resourceDirectory.getAbsolutePath());
        }
        if (assetsDirectory.exists()) {
            commands.add("-A"                             );
            commands.add(assetsDirectory.getAbsolutePath());
        }
        commands.add("-I"                                 );
        commands.add(getAndroidSdk().getAndroidJar().getAbsolutePath());
        getLog().info(getAndroidSdk().getPathForTool("aapt") + " " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("aapt"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

        project.addCompileSourceRoot(generatedSourcesRDirectoryName);
    }

    private void generateAidlFiles(String[] relativeAidlFileNames) throws MojoExecutionException {
        for (String relativeAidlFileName : relativeAidlFileNames) {
            List<String> commands       = new ArrayList<String>();
            commands.add("-p" + getAndroidSdk().getPathForFrameworkAidl());

            File generatedSourcesAidlDirectory = new File(project.getBuild().getDirectory() + File.separator + "generated-sources" + File.separator + "aidl");
            generatedSourcesAidlDirectory.mkdirs();
            project.addCompileSourceRoot(generatedSourcesAidlDirectory.getPath());
            File targetDirectory = new File(generatedSourcesAidlDirectory, new File(relativeAidlFileName).getParent());
            targetDirectory.mkdirs();

            final String shortAidlFileName         = new File(relativeAidlFileName).getName();
            final String shortJavaFileName         = shortAidlFileName.substring(0, shortAidlFileName.lastIndexOf(".")) + ".java";
            final File   aidlFileInSourceDirectory = new File(sourceDirectory, relativeAidlFileName);

            commands.add("-I" + sourceDirectory                                         );
            commands.add(aidlFileInSourceDirectory.getAbsolutePath()                    );
            commands.add(new File(targetDirectory , shortJavaFileName).getAbsolutePath());
            try {
                CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
                executor.setLogger(this.getLog());

                executor.executeCommand(getAndroidSdk().getPathForTool("aidl"), commands, project.getBasedir(), false);
            } catch (ExecutionException e) {
                throw new MojoExecutionException("", e);
            }
        }

    }

    private String[] findRelativeAidlFileNames() {
        String[] relativeAidlFileNames = findFilesInDirectory(sourceDirectory, "**/*.aidl");
        getLog().info("ANDROID-904-002: Found aidl files: Count = " + relativeAidlFileNames.length);
        return relativeAidlFileNames;
    }
}
