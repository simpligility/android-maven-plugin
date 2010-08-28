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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

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
 * @requiresDependencyResolution compile
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

    	try {
	        extractSourceDependencies();
	
	        final String[]    relativeAidlFileNames1   = findRelativeAidlFileNames(sourceDirectory                 );
	        final String[]    relativeAidlFileNames2   = findRelativeAidlFileNames(extractedDependenciesJavaSources);
	        final Set<String> relativeAidlFileNamesSet = new HashSet<String>() {
	            {
	                addAll(Arrays.asList(relativeAidlFileNames1));
	                addAll(Arrays.asList(relativeAidlFileNames2));
	            }
	        };
	
	        if (deleteConflictingFiles) {
	            deleteConflictingRJavaFiles       (sourceDirectory                                           );
	            deleteConflictingManifestJavaFiles(sourceDirectory                                           );
	            deleteConflictingThumbsDb         (resourceDirectory                                         );
	            deleteConflictingAidlJavaFiles    (sourceDirectory, relativeAidlFileNamesSet                 );
	            deleteConflictingAidlJavaFiles    (extractedDependenciesJavaSources, relativeAidlFileNamesSet);
	        }
	
	        generateR        (                                                        );
	        generateAidlFiles(sourceDirectory, relativeAidlFileNames1                 );
	        generateAidlFiles(extractedDependenciesJavaSources, relativeAidlFileNames2);
    	} catch (MojoExecutionException e) {
    		getLog().error("Error when generating sources.", e);
    		throw e;
    	} 
    }

    protected void extractSourceDependencies() throws MojoExecutionException {
        for (Artifact artifact : getRelevantDependencyArtifacts()) {
            String type = artifact.getType();
            if (type.equals("apksources")) {
                getLog().debug("Detected apksources dependency " + artifact + " with file " + artifact.getFile() + ". Will resolve and extract...");

                //When using maven under eclipse the artifact will by default point to a directory, which isn't correct.
                //To work around this we'll first try to get the archive from the local repo, and only if it isn't found there we'll do a normal resolve.
                File apksourcesFile = new File(getLocalRepository().getBasedir(), getLocalRepository().pathOf(artifact));
                if (apksourcesFile.isDirectory()) {
                    apksourcesFile = resolveArtifactToFile(artifact);
                }
                getLog().debug("Extracting " + apksourcesFile + "...");
                extractApksources(apksourcesFile);
            }
        }
        projectHelper.addResource(project, extractedDependenciesJavaResources.getAbsolutePath(), null, null);
        project.addCompileSourceRoot(extractedDependenciesJavaSources.getAbsolutePath());
    }

    private void extractApksources(File apksourcesFile) throws MojoExecutionException {
    	if (apksourcesFile.isDirectory()) {
    		getLog().warn("The apksources artifact points to '"+apksourcesFile+"' which is a directory; skipping unpacking it.");
    		return;
    	}
        final UnArchiver unArchiver = new ZipUnArchiver(apksourcesFile){
            @Override
            protected Logger getLogger() {
                return new ConsoleLogger(Logger.LEVEL_DEBUG, "dependencies-unarchiver");
            }
        };
        extractedDependenciesDirectory.mkdirs();
        unArchiver.setDestDirectory(extractedDependenciesDirectory);
        try {
            unArchiver.extract();
        } catch (ArchiverException e) {
            throw new MojoExecutionException("ArchiverException while extracting " + apksourcesFile.getAbsolutePath() + ". Message: " + e.getLocalizedMessage(), e);
        }
    }


    private void deleteConflictingManifestJavaFiles(File sourceDirectory) throws MojoExecutionException {
        final int numberOfFilesDeleted = deleteFilesFromDirectory(sourceDirectory, "**/Manifest.java");
        if (numberOfFilesDeleted > 0) {
            getLog().info("Deleted " + numberOfFilesDeleted + " conflicting Manifest.java file(s) in source directory. If you use Eclipse, please Refresh (F5) the project to regain it.");
        }
    }
    private void deleteConflictingRJavaFiles(File sourceDirectory) throws MojoExecutionException {
        final int numberOfFilesDeleted = deleteFilesFromDirectory(sourceDirectory, "**/R.java");
        if (numberOfFilesDeleted > 0) {
            getLog().info("Deleted " + numberOfFilesDeleted + " conflicting R.java file(s) in source directory. If you use Eclipse, please Refresh (F5) the project to regain it.");
        }
    }

    private void deleteConflictingThumbsDb(File resourceDirectory) throws MojoExecutionException {
        //Get rid of this annoying Thumbs.db problem on windows
        final int numberOfFilesDeleted = deleteFilesFromDirectory(resourceDirectory, "**/Thumbs.db");
        if (numberOfFilesDeleted > 0) {
            getLog().info("Deleted " + numberOfFilesDeleted + " Thumbs.db file(s) in resource directory.");
        }
    }

    private void deleteConflictingAidlJavaFiles(File sourceDirectory, Collection<String> relativeAidlFileNames) throws MojoExecutionException {
        for (String relativeAidlFileName : relativeAidlFileNames) {

            final String relativeJavaFileName                 = relativeAidlFileName.substring(0, relativeAidlFileName.lastIndexOf(".")) + ".java";
            final File   conflictingJavaFileInSourceDirectory = new File(sourceDirectory, relativeJavaFileName);

            if (conflictingJavaFileInSourceDirectory.exists()) {

                //We should only delete files which define interfaces, not files that specify a parcelable
                //Note that this code should probably be expanded if more cases where a java file is generated are added to the AIDL spec.
                boolean shouldDelete = false;
                try {
                    LineIterator lineIterator = FileUtils.lineIterator(conflictingJavaFileInSourceDirectory);
                    try {
                        while (!shouldDelete && lineIterator.hasNext()) {
                            //If the file contains "interface" it's an AIDL file which will generate a java file, and we should therefore remove the java file.
                            if (lineIterator.nextLine().contains("interface")) {
                                shouldDelete = true;
                            }
                        }
                    } finally {
                        lineIterator.close(); // Closes quietly
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException("Could not inspect aidl file \"" + conflictingJavaFileInSourceDirectory.getAbsolutePath() + "\".");
                }

                if (shouldDelete) {
                    final boolean successfullyDeleted = conflictingJavaFileInSourceDirectory.delete();
                    if (successfullyDeleted) {
                        getLog().info("Deleted conflicting file in source directory: \"" + conflictingJavaFileInSourceDirectory + "\". If you use Eclipse, please Refresh (F5) the project to regain them.");
                    } else {
                        throw new MojoExecutionException("Failed to delete conflicting file in source directory: \"" + conflictingJavaFileInSourceDirectory + "\"");
                    }
                }
            }
        }
    }

    private void generateR() throws MojoExecutionException {

        String generatedSourcesRDirectoryName = project.getBuild().getDirectory() + File.separator + "generated-sources" + File.separator + "r";
        new File(generatedSourcesRDirectoryName).mkdirs();
        File[] overlayDirectories;
        
        if (resourceOverlayDirectories == null || resourceOverlayDirectories.length == 0) {
        	overlayDirectories = new File[] {resourceOverlayDirectory};
        } else {
        	overlayDirectories = resourceOverlayDirectories;
        }
        
        if (!combinedRes.exists()) {
	        if (!combinedRes.mkdirs()) {
	        	throw new MojoExecutionException("Could not create directory for combined resources at " + combinedRes.getAbsolutePath());
	        }
        }
        if (extractedDependenciesRes.exists()) {
        	try {
        	    getLog().info("Copying dependency resource files to combined resource directory.");
				org.apache.commons.io.FileUtils.copyDirectory(extractedDependenciesRes, combinedRes);
			}
			catch (IOException e) {
				throw new MojoExecutionException("", e);
			}	
        }
        if (resourceDirectory.exists()) {
        	try {
        	    getLog().info("Copying local resource files to combined resource directory.");
				org.apache.commons.io.FileUtils.copyDirectory(resourceDirectory, combinedRes);
			}
			catch (IOException e) {
				throw new MojoExecutionException("", e);
			}	
        }
        
        
        

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        List<String> commands = new ArrayList<String>();
        commands.add("package");
        commands.add("-m");
        commands.add("-J"                                 );
        commands.add(generatedSourcesRDirectoryName       );
        commands.add("-M"                                 );
        commands.add(androidManifestFile.getAbsolutePath());
        for(File resOverlayDir : overlayDirectories) {
        	if (resOverlayDir != null && resOverlayDir.exists()) {
        		commands.add("-S");
        		commands.add(resOverlayDir.getAbsolutePath());
        	}
        }
        if (combinedRes.exists()) {
            commands.add("-S"                               );
            commands.add(combinedRes.getAbsolutePath());
        }
        if (assetsDirectory.exists()) {
            commands.add("-A"                             );
            commands.add(assetsDirectory.getAbsolutePath());
        }
        if (extractedDependenciesAssets.exists()) {
            commands.add("-A"                                         );
            commands.add(extractedDependenciesAssets.getAbsolutePath());
        }
        commands.add("-I"                                             );
        commands.add(getAndroidSdk().getAndroidJar().getAbsolutePath());
        if (StringUtils.isNotBlank(configurations)) {
        	commands.add("-c"          );
        	commands.add(configurations);
        }
        getLog().info(getAndroidSdk().getPathForTool("aapt") + " " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("aapt"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

        project.addCompileSourceRoot(generatedSourcesRDirectoryName);
    }

    private void generateAidlFiles(File sourceDirectory, String[] relativeAidlFileNames) throws MojoExecutionException {
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

            commands.add("-I" + sourceDirectory);
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

    private String[] findRelativeAidlFileNames(File sourceDirectory) {
        String[] relativeAidlFileNames = findFilesInDirectory(sourceDirectory, "**/*.aidl");
        getLog().info("ANDROID-904-002: Found aidl files: Count = " + relativeAidlFileNames.length);
        return relativeAidlFileNames;
    }
}
