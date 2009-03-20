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
package org.jvending.masa.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.resolvers.ArtifactsResolver;
import org.apache.maven.plugin.dependency.utils.resolvers.DefaultArtifactsResolver;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.jvending.masa.CommandExecutor;
import org.jvending.masa.ExecutionException;

import java.io.File;
import java.util.*;

/**
 * Contains common fields and methods for android mojos.
 *
 * @author hugo.josefson@jayway.se
 */
public abstract class AbstractAndroidMojo extends AbstractMojo {
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    /**
     * @parameter default-value="res"
     */
    protected File resourceDirectory;
    /**
     * @parameter default-value="assets"
     */
    protected File assetsDirectory;
    /**
     * @parameter
     */
    protected File androidManifestFile;
    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected org.apache.maven.artifact.resolver.ArtifactResolver artifactResolver;
    /**
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;
    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private java.util.List remoteRepositories;
    /**
     * @component
     */
    private ArtifactFactory artifactFactory;
    /**
     * @parameter expression="${androidVersion}"
     *            default-value="1.1_r1"
     */
    private String androidVersion;
    /**
     * Maven ProjectHelper.
     *
     * @component
     * @readonly
     */
    protected MavenProjectHelper projectHelper;

    /**
     * Resolves the android.jar, using {@link #androidVersion} as the artifact's version.
     *
     * @return a <code>File</code> pointing to the android.jar file.
     * @throws org.apache.maven.plugin.MojoExecutionException if the artifact can not be resolved.
     */
    protected File resolveAndroidJar() throws MojoExecutionException {
        Artifact artifact = artifactFactory.createArtifact("android", "android", androidVersion, "jar", "jar");

        // resolve the android jar artifact
        File androidJar = resolveArtifactToFile(artifact);

        getLog().debug("Found android.jar at " + androidJar);
        return androidJar;
    }

    /**
     * Attempts to resolve an {@link Artifact} to a {@link File}.
     * @param artifact to resolve
     * @return a {@link File} to the resolved artifact, never <code>null</code>.
     * @throws MojoExecutionException if the artifact could not be resolved. 
     */
    protected File resolveArtifactToFile(Artifact artifact) throws MojoExecutionException {
        final ArtifactsResolver artifactsResolver = new DefaultArtifactsResolver( this.artifactResolver, this.localRepository, this.remoteRepositories, true );
        final HashSet artifacts = new HashSet();
        artifacts.add(artifact);
        File jar = null;
        final Set resolvedArtifacts = artifactsResolver.resolve(artifacts, getLog());
        for (Iterator it = resolvedArtifacts.iterator(); it.hasNext();) {
            Artifact resolvedArtifact = (Artifact) it.next();
            jar = resolvedArtifact.getFile();
        }
        if (jar == null){
            throw new MojoExecutionException("Could not resolve artifact " + artifact.getId() + ". Please install it with \"mvn install:install-file ...\" or deploy it to a repository with \"mvn deploy:deploy-file ...\"");
        }
        
        return jar;
    }

    /**
     * Installs an apk file to a connected emulator or usb device.
     * @param apkFile the file to install
     * @throws MojoExecutionException If there is a problem installing the apk file.
     */
    protected void installApkToDevice(File apkFile) throws MojoExecutionException {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        List<String> commands = new ArrayList<String>();
        commands.add("install");
        commands.add("-r");
        commands.add(apkFile.getAbsolutePath());
        getLog().info("adb " + commands.toString());
        try {
            executor.executeCommand("adb", commands, false);
            getLog().debug(executor.getStandardOut());
            getLog().debug(executor.getStandardError());
        } catch (ExecutionException e) {
            getLog().error(executor.getStandardOut());
            getLog().error(executor.getStandardError());
            throw new MojoExecutionException("Error installing " + apkFile + " to device.", e);
        }
    }
}
