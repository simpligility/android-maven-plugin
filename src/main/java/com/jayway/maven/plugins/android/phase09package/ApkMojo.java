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
package com.jayway.maven.plugins.android.phase09package;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.AndroidSigner;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.Sign;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.dependency.utils.resolvers.DefaultArtifactsResolver;

import java.io.*;
import java.util.*;

/**
 * Creates the apk file. By default signs it with debug keystore.<br/>
 * Change that by setting configuration parameter <code>&lt;sign&gt;&lt;debug&gt;false&lt;/debug&gt;&lt;/sign&gt;</code>.
 *
 * @author hugo.josefson@jayway.com
 * @goal apk
 * @phase package
 * @requiresDependencyResolution compile
 */
public class ApkMojo extends AbstractAndroidMojo {


    /**
     * <p>How to sign the apk.</p>
     * <p>Looks like this:</p>
     * <pre>
     * &lt;sign&gt;
     *     &lt;debug&gt;auto&lt;/debug&gt;
     * &lt;/sign&gt;
     * </pre>
     * <p>Valid values for <code>&lt;debug&gt;</code>are:
     * <ul>
     * <li><code>true</code> = sign with the debug keystore.
     * <li><code>false</code> = don't sign with the debug keystore.
     * <li><code>auto</code> (default) = sign with debug keystore, unless another keystore is defined. (Signing with
     * other keystores is not yet implemented. See
     * <a href="http://code.google.com/p/maven-android-plugin/issues/detail?id=2">Issue 2</a>.)
     * </ul></p>
     * <p>Can also be configured from command-line with parameter <code>-Dandroid.sign.debug</code>.</p>
     *
     * @parameter
     */
    private Sign sign;

    /**
     * <p>Parameter designed to pick up <code>-Dandroid.sign.debug</code> in case there is no pom with a
     * <code>&lt;sign&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link Sign#debug}.</p>
     *
     * @parameter expression="${android.sign.debug}" default-value="auto"
     * @readonly
     */
    private String signDebug;

    /**
     * <p>Root folder containing native libraries to include in the application package.</p>
     *
     * @parameter default-value="${project.basedir}/libs"
     */
    private File nativeLibrariesDirectory;

     /**
     * <p>Temporary folder for collecting native libraries.</p>
     *
     * @parameter default-value="${project.build.directory}/libs"
     * @readonly
     */
    private File nativeLibrariesOutputDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        // Make an early exit if we're not supposed to generate the APK
        if (!generateApk) {
            return;
        }

        generateIntermediateAp_();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        File outputFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() +
                ANDROID_PACKAGE_EXTENSTION);

        List<String> commands = new ArrayList<String>();
        commands.add(outputFile.getAbsolutePath());

        if (!getAndroidSigner().isSignWithDebugKeyStore()) {
            commands.add("-u");
        }

        commands.add("-z");
        commands.add(new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_").getAbsolutePath());
        commands.add("-f");
        commands.add(new File(project.getBuild().getDirectory(), "classes.dex").getAbsolutePath());
        commands.add("-rf");
        commands.add(new File(project.getBuild().getDirectory(), "classes").getAbsolutePath());

        // Process the native libraries, looking both in the current build directory as well as
        // at the dependencies declared in the pom.  Currently, all .so files are automatically included
        processNativeLibraries(commands);

        for (Artifact artifact : getRelevantCompileArtifacts()) {
            commands.add("-rj");
            commands.add(artifact.getFile().getAbsolutePath());
        }


        getLog().info(getAndroidSdk().getPathForTool("apkbuilder") + " " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("apkbuilder"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

        // Set the generated .apk file as the main artifact (because the pom states <packaging>apk</packaging>)
        project.getArtifact().setFile(outputFile);
    }

    private void processNativeLibraries(final List<String> commands) throws MojoExecutionException
    {
        final Set<Artifact> artifacts = getNativeDependenciesArtifacts();

        final boolean hasValidNativeLibrariesDirectory = nativeLibrariesDirectory != null && nativeLibrariesDirectory.exists();

        if (artifacts.isEmpty() && hasValidNativeLibrariesDirectory)
        {
            getLog().debug("No native library dependencies detected, will point directly to " + nativeLibrariesDirectory);

            // Point directly to the directory in this case - no need to copy files around
            commands.add("-nf");
            commands.add(nativeLibrariesDirectory.getAbsolutePath());
        }
        else if (!artifacts.isEmpty() || hasValidNativeLibrariesDirectory)
        {
            // In this case, we may have both .so files in it's normal location
            // as well as .so dependencies

            // Create the ${project.build.outputDirectory}/libs
            File destinationDirectory = new File(nativeLibrariesOutputDirectory.getAbsolutePath());
            destinationDirectory.mkdirs();

             // Point directly to the directory
            commands.add("-nf");
            commands.add(destinationDirectory.getAbsolutePath());


            // If we have a valid native libs, copy those files - these already come in the structure required
            if (hasValidNativeLibrariesDirectory)
            {
                getLog().debug("Copying existing native libraries from " + nativeLibrariesDirectory);
                try
                {
                    org.apache.commons.io.FileUtils.copyDirectory(nativeLibrariesDirectory,destinationDirectory, new FileFilter()
                    {
                        public boolean accept(final File pathname)
                        {
                            return pathname.getName().endsWith(".so");
                        }
                    });
                }
                catch (IOException e)
                {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }

            if (!artifacts.isEmpty())
            {
                File finalDestinationDirectory = new File(destinationDirectory,"armeabi/");

                getLog().debug("Copying native library dependencies to " + finalDestinationDirectory);

                finalDestinationDirectory.mkdirs();

                final DefaultArtifactsResolver artifactsResolver = new DefaultArtifactsResolver(this.artifactResolver, this.localRepository, this.remoteRepositories, true);

                final Set<Artifact> resolvedArtifacts = artifactsResolver.resolve(artifacts, getLog());

                for (Artifact resolvedArtifact : resolvedArtifacts)
                {
                    final File artifactFile = resolvedArtifact.getFile();
                    try
                    {
                        final String artifactId = resolvedArtifact.getArtifactId();
                        final String filename = artifactId.startsWith("lib") ? artifactId + ".so" : "lib" + artifactId + ".so";
                        final File file = new File(finalDestinationDirectory, filename);
                        getLog().debug("Copying native dependency " + artifactId + " (" + resolvedArtifact.getGroupId() + ") to " + file );
                        org.apache.commons.io.FileUtils.copyFile(artifactFile, file);
                    }
                    catch (Exception e)
                    {
                        throw new MojoExecutionException("Could not copy native dependency.", e);
                    }
                }
            }
        }
    }

    private Set<Artifact> getNativeDependenciesArtifacts()
    {
        Set<Artifact> filteredArtifacts = new HashSet<Artifact>();
        final Set<Artifact> allArtifacts = project.getDependencyArtifacts();

        for (Artifact artifact : allArtifacts)
        {
            if ("so".equals(artifact.getType()) && (Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) || Artifact.SCOPE_RUNTIME.equals( artifact.getScope() )))
            {
                filteredArtifacts.add(artifact);
            }
        }

        return filteredArtifacts;
    }



    /**
     * Generates an intermediate apk file (actually .ap_) containing the resources and assets.
     *
     * @throws MojoExecutionException
     */
    private void generateIntermediateAp_() throws MojoExecutionException {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        File[] overlayDirectories;

        if (resourceOverlayDirectories == null || resourceOverlayDirectories.length == 0) {
            overlayDirectories = new File[]{resourceOverlayDirectory};
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
            } catch (IOException e) {
                throw new MojoExecutionException("", e);
            }
        }
        if (resourceDirectory.exists()) {
            try {
                getLog().info("Copying local resource files to combined resource directory.");
                org.apache.commons.io.FileUtils.copyDirectory(resourceDirectory, combinedRes);
            } catch (IOException e) {
                throw new MojoExecutionException("", e);
            }
        }

        File androidJar = getAndroidSdk().getAndroidJar();
        File outputFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_");

        List<String> commands = new ArrayList<String>();
        commands.add("package");
        commands.add("-f");
        commands.add("-M");
        commands.add(androidManifestFile.getAbsolutePath());
        for (File resOverlayDir : overlayDirectories) {
            if (resOverlayDir != null && resOverlayDir.exists()) {
                commands.add("-S");
                commands.add(resOverlayDir.getAbsolutePath());
            }
        }
        if (combinedRes.exists()) {
            commands.add("-S");
            commands.add(combinedRes.getAbsolutePath());
        }
        if (assetsDirectory.exists()) {
            commands.add("-A");
            commands.add(assetsDirectory.getAbsolutePath());
        }
        if (extractedDependenciesAssets.exists()) {
            commands.add("-A");
            commands.add(extractedDependenciesAssets.getAbsolutePath());
        }
        commands.add("-I");
        commands.add(androidJar.getAbsolutePath());
        commands.add("-F");
        commands.add(outputFile.getAbsolutePath());
        if (StringUtils.isNotBlank(configurations)) {
            commands.add("-c");
            commands.add(configurations);
        }
        getLog().info(getAndroidSdk().getPathForTool("aapt") + " " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("aapt"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }


    protected AndroidSigner getAndroidSigner() {
        if (sign == null) {
            return new AndroidSigner(signDebug);
        } else {
            return new AndroidSigner(sign.getDebug());
        }
    }
}
