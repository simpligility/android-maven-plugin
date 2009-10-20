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
import com.jayway.maven.plugins.android.AndroidSigner;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.Sign;
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
import java.util.List;

/**
 * Creates the apk file. By default signs it with debug keystore.<br/>
 * Change that by setting configuration parameter <code>&lt;sign&gt;&lt;debug&gt;false&lt;/debug&gt;&lt;/sign&gt;</code>.
 * @goal apk
 * @phase package
 * @author hugo.josefson@jayway.com
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
     *     <li><code>true</code> = sign with the debug keystore.
     *     <li><code>false</code> = don't sign with the debug keystore.
     *     <li><code>auto</code> (default) = sign with debug keystore, unless another keystore is defined. (Signing with
     * other keystores is not yet implemented. See
     * <a href="http://code.google.com/p/maven-android-plugin/issues/detail?id=2">Issue 2</a>.)
     * </ul></p>
     * <p>Can also be configured from command-line with parameter <code>-Dandroid.sign.debug</code>.</p>
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


    public void execute() throws MojoExecutionException, MojoFailureException {

        generateIntermediateAp_();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        File outputFile = new File(project.getBuild().getDirectory(),  project.getBuild().getFinalName() + ".apk");

        List<String> commands = new ArrayList<String>();
        commands.add(outputFile.getAbsolutePath());

        if(!getAndroidSigner().isSignWithDebugKeyStore()) {
            commands.add("-u");
        }
        
        commands.add("-z");
        commands.add(new File(project.getBuild().getDirectory(),  project.getBuild().getFinalName() + ".ap_").getAbsolutePath());
        commands.add("-f");
        commands.add( new File(project.getBuild().getDirectory(),  "classes.dex").getAbsolutePath());
        commands.add("-rf");
        commands.add(new File(project.getBuild().getDirectory(), "classes").getAbsolutePath());

        for (Artifact artifact : (List<Artifact>) project.getCompileArtifacts()) {
            if (artifact.getGroupId().equals("android")) {
                continue;
            }

            if (artifact.getFile().isDirectory()) {
                throw new MojoExecutionException("Dependent artifact is directory: Directory = " + artifact.getFile().getAbsolutePath());
            }

            commands.add("-rj");
            commands.add(artifact.getFile().getAbsolutePath());
        }


        getLog().info(getAndroidSdk().getPathForTool("apkbuilder")+" " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("apkbuilder"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

        // Set the generated .apk file as the main artifact (because the pom states <packaging>apk</packaging>)
        project.getArtifact().setFile(outputFile);

        if (attachJar){
            // Also attach the normal .jar, so it can be depended on by for example an instrumentation project if they need access to our R.java and other things.
            projectHelper.attachArtifact(project, "jar", new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".jar"));
        }

        if (attachSources){
            // Also attach an .apksources, containing sources from this project.
            final File apksources = createApkSourcesFile();
            projectHelper.attachArtifact(project, "apksources", apksources);
        }
    }

    protected File createApkSourcesFile() throws MojoExecutionException {
        final File apksources = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".apksources");
        FileUtils.deleteQuietly(apksources);

        try {
            JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setDestFile(apksources);

            addDirectory(jarArchiver, assetsDirectory, "assets"       );
            addDirectory(jarArchiver, resourceDirectory, "res"        );
            addDirectory(jarArchiver, sourceDirectory, "src/main/java");
            addJavaResources(jarArchiver, (List<Resource>) project.getBuild().getResources());

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
     * @param jarArchiver
     * @param directory The directory to add.
     * @param prefix An optional prefix for where in the Jar file the directory's contents should go.
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
        if (extractedDependenciesRes.exists()) {
            commands.add("-S");
            commands.add(extractedDependenciesRes.getAbsolutePath());
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
        getLog().info(getAndroidSdk().getPathForTool("aapt")+" " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("aapt"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }


    protected AndroidSigner getAndroidSigner(){
        if (sign == null){
            return new AndroidSigner(signDebug);
        }else{
            return new AndroidSigner(sign.getDebug());
        }
    }
}
