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
package com.jayway.maven.plugins.android.phase08preparepackage;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.configuration.Dex;
import com.jayway.maven.plugins.android.phase04processclasses.ProguardMojo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts compiled Java classes to the Android dex format.
 *
 * @author hugo.josefson@jayway.com
 * @goal dex
 * @phase prepare-package
 * @requiresDependencyResolution compile
 */
public class DexMojo extends AbstractAndroidMojo {

    /**
     * Configuration for the dex command execution. It can be configured in the plugin configuration like so
     * <pre>
     * &lt;dex&gt;
     *   &lt;jvmArguments&gt;
     *     &lt;jvmArgument&gt;-Xms256m&lt;/jvmArgument&gt;
     *     &lt;jvmArgument&gt;-Xmx512m&lt;/jvmArgument&gt;
     *   &lt;/jvmArguments&gt;
     *   &lt;coreLibrary&gt;true|false&lt;/coreLibrary&gt;
     *   &lt;noLocals&gt;true|false&lt;/noLocals&gt;
     *   &lt;optimize&gt;true|false&lt;/optimize&gt;
     * &lt;/dex&gt;
     * </pre>
     *
     * or via properties dex.* or command line parameters android.dex.*
     *
     * @parameter
     */
    private Dex dex;
    /**
     * Extra JVM Arguments. Using these you can e.g. increase memory for the jvm running the build.
     *
     * @parameter expression="${android.dex.jvmArguments}" default-value="-Xmx1024M"
     * @optional
     */
    private String[] dexJvmArguments;

    /**
     * Decides whether to pass the --core-library flag to dx.
     *
     * @parameter expression="${android.dex.coreLibrary}" default-value="false"
     */
    private boolean dexCoreLibrary;

    /**
     * Decides whether to pass the --no-locals flag to dx.
     *
     * @parameter expression="${android.dex.noLocals}" default-value="false"
     */
    private boolean dexNoLocals;

    /**                                         
     * Decides whether to pass the --no-optimize flag to dx.
     *
     * @parameter expression="${android.dex.optimize}" default-value="true"
     */
    private boolean dexOptimize;

    private String[] parsedJvmArguments;
    private boolean parsedCoreLibrary;
    private boolean parsedNoLocals;
    private boolean parsedOptimize;

    public void execute() throws MojoExecutionException, MojoFailureException {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        File outputFile = new File(project.getBuild().getDirectory() + File.separator + "classes.dex");

        Set<File> inputFiles = getDexInputFiles();

        parseConfiguration();
        
        if (generateApk) {
            runDex(executor, outputFile, inputFiles);
        }

        if (attachJar) {
            File jarFile = new File(project.getBuild().getDirectory() + File.separator + project.getBuild().getFinalName() + ".jar");
            projectHelper.attachArtifact(project, "jar", project.getArtifact().getClassifier(), jarFile);
        }

        if (attachSources) {
            // Also attach an .apksources, containing sources from this project.
            final File apksources = createApkSourcesFile();
            projectHelper.attachArtifact(project, "apksources", apksources);
        }
    }

    /**
     * Gets the input files for dex.  This is a combination of directories and jar files.
     *
     * @return
     */
    private Set<File> getDexInputFiles() {

        Set<File> inputs = new HashSet<File>();

        // ugly, don't know a better way to get this in mvn
        File proguardJar = new File(project.getBuild().getDirectory(), ProguardMojo.PROGUARD_OBFUSCATED_JAR);

        getLog().debug("Checking for existence of: " + proguardJar.toString());

        if (proguardJar.exists()) {
            // progurad has been run, use this jar
            getLog().debug("Obfuscated jar exists, using that as input");
            inputs.add(proguardJar);
        } else {
            getLog().debug("Using non-obfuscated input");
            // no proguard, use original config
            inputs.add(new File(project.getBuild().getOutputDirectory()));
            for (Artifact artifact : getAllRelevantDependencyArtifacts()) {
                inputs.add(artifact.getFile().getAbsoluteFile());
            }
        }

        return inputs;
    }

    private void parseConfiguration() {
        // config in pom found
        if (dex != null) {
            // the if statements make sure that properties/command line parameter overrides configuration
            // and that the dafaults apply in all cases;
            if (dex.getJvmArguments() == null) {
                parsedJvmArguments =  dexJvmArguments;
            } else {
                parsedJvmArguments = dex.getJvmArguments();
            }
            if (dex.isCoreLibrary() == null) {
                parsedCoreLibrary = dexCoreLibrary;
            } else {
                parsedCoreLibrary = dex.isCoreLibrary();
            }
            if (dex.isNoLocals() == null) {
                parsedNoLocals = dexNoLocals;
            } else {
                parsedNoLocals = dex.isNoLocals();
            }
            if (dex.isOptimize() == null) {
                parsedOptimize = dexOptimize;
            } else {
                parsedOptimize = dex.isOptimize();
            }
        } else {
            parsedJvmArguments = dexJvmArguments;
            parsedCoreLibrary = dexCoreLibrary;
            parsedNoLocals = dexNoLocals;
            parsedOptimize = dexOptimize;
        }
    }

    private void runDex(CommandExecutor executor, File outputFile,
                        Set<File> inputFiles) throws MojoExecutionException {
        List<String> commands = new ArrayList<String>();
        if (parsedJvmArguments != null) {
            for (String jvmArgument : parsedJvmArguments) {
                 // preserve backward compatibility allowing argument with or without dash (e.g. Xmx512m as well as
                 // -Xmx512m should work) (see http://code.google.com/p/maven-android-plugin/issues/detail?id=153)
                 if (!jvmArgument.startsWith("-")) {
                        jvmArgument = "-" + jvmArgument;
                 }
                getLog().debug("Adding jvm argument " + jvmArgument);
                commands.add(jvmArgument);
            }
        }
        commands.add("-jar");
        commands.add(getAndroidSdk().getPathForTool("dx.jar"));
        commands.add("--dex");
        if (!parsedOptimize) {
            commands.add("--no-optimize");
        }
        if (parsedCoreLibrary) {
            commands.add("--core-library");
        }
        commands.add("--output=" + outputFile.getAbsolutePath());
        if (parsedNoLocals) {
        	commands.add("--no-locals");
        }

        for (File inputFile : inputFiles) {
            getLog().debug("Adding dex input: " + inputFile.getAbsolutePath());
            commands.add(inputFile.getAbsolutePath());
        }

        final String javaExecutable = getJavaExecutable().getAbsolutePath();
        getLog().info(javaExecutable + " " + commands.toString());
        try {
            executor.executeCommand(javaExecutable, commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }

    /**
     * Figure out the full path to the current java executable.
     *
     * @return the full path to the current java executable.
     */
    private static File getJavaExecutable() {
        final String javaHome = System.getProperty("java.home");
        final String slash = File.separator;
        return new File(javaHome + slash + "bin" + slash + "java");
    }

    protected File createApkSourcesFile() throws MojoExecutionException {
        final File apksources = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".apksources");
        FileUtils.deleteQuietly(apksources);

        try {
            JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setDestFile(apksources);

            addDirectory(jarArchiver, assetsDirectory, "assets");
            addDirectory(jarArchiver, resourceDirectory, "res");
            addDirectory(jarArchiver, sourceDirectory, "src/main/java");
            addJavaResources(jarArchiver, project.getBuild().getResources());

            jarArchiver.createArchive();
        } catch (ArchiverException e) {
            throw new MojoExecutionException("ArchiverException while creating .apksource file.", e);
        } catch (IOException e) {
            throw new MojoExecutionException("IOException while creating .apksource file.", e);
        }

        return apksources;
    }

    /**
     * Makes sure the string ends with "/"
     *
     * @param prefix any string, or null.
     * @return the prefix with a "/" at the end, never null.
     */
    protected String endWithSlash(String prefix) {
        prefix = StringUtils.defaultIfEmpty(prefix, "/");
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix;
    }

    /**
     * Adds a directory to a {@link JarArchiver} with a directory prefix.
     *
     * @param jarArchiver
     * @param directory   The directory to add.
     * @param prefix      An optional prefix for where in the Jar file the directory's contents should go.
     * @throws ArchiverException
     */
    protected void addDirectory(JarArchiver jarArchiver, File directory, String prefix) throws ArchiverException {
        if (directory != null && directory.exists()) {
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setPrefix(endWithSlash(prefix));
            fileSet.setDirectory(directory);
            jarArchiver.addFileSet(fileSet);
        }
    }

    protected void addJavaResources(JarArchiver jarArchiver, List<Resource> javaResources) throws ArchiverException {
        for (Resource javaResource : javaResources) {
            addJavaResource(jarArchiver, javaResource);
        }
    }

    /**
     * Adds a Java Resources directory (typically "src/main/resources") to a {@link JarArchiver}.
     *
     * @param jarArchiver
     * @param javaResource The Java resource to add.
     * @throws ArchiverException
     */
    protected void addJavaResource(JarArchiver jarArchiver, Resource javaResource) throws ArchiverException {
        if (javaResource != null) {
            final File javaResourceDirectory = new File(javaResource.getDirectory());
            if (javaResourceDirectory.exists()) {
                final DefaultFileSet javaResourceFileSet = new DefaultFileSet();
                javaResourceFileSet.setDirectory(javaResourceDirectory);
                javaResourceFileSet.setPrefix(endWithSlash("src/main/resources"));
                jarArchiver.addFileSet(javaResourceFileSet);
            }
        }
    }
}
