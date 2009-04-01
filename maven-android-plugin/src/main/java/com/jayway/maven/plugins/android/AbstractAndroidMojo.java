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
package com.jayway.maven.plugins.android;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.xml.DocumentContainer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.resolvers.ArtifactsResolver;
import org.apache.maven.plugin.dependency.utils.resolvers.DefaultArtifactsResolver;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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
     * The maven session.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * The android resources directory.
     * @parameter default-value="res"
     */
    protected File resourceDirectory;

    /**
     * The android assets directory.
     * @parameter default-value="assets"
     */
    protected File assetsDirectory;

    /**
     * The <code>AndroidManifest.xml</code> file.
     * @parameter default-value="${project.basedir}/AndroidManifest.xml"
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
     * @readonly
     * @required
     */
    private ArtifactFactory artifactFactory;

    /**
     * Which version of Android SDK to use.
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
     * <p>Whether to uninstall an apk from the device before installing it.</p>
     *
     * <p>Only has effect when running <code>mvn android:installApkToDevice</code> in a project with
     * <code>&lt;packaging&gt;android:apk&lt;/packaging&gt;</code> manually, or when running
     * <code>mvn integration-test</code> (or <code>mvn install</code>) in a project with
     * <code>&lt;packaging&gt;android:apk:platformTest&lt;/packaging&gt;</code>.</p>
     *
     * <p>It is useful to keep this set to <code>true</code> at all times, because if an apk with the same package was
     * previously signed with a different keystore, and installed to the device, installation will fail becuase your
     * keystore is different.</p>
     *
     * @parameter default-value=false
     *            expression="${android.uninstallApkBeforeInstallingToDevice}"
     *
     */
    protected boolean uninstallApkBeforeInstallingToDevice;

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
     *            expression="${android.deleteMalplacedFiles}"
     *
     */
    protected boolean deleteMalplacedFiles;

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
        final HashSet<Artifact> artifacts = new HashSet<Artifact>();
        artifacts.add(artifact);
        File jar = null;
        final Set<Artifact> resolvedArtifacts = artifactsResolver.resolve(artifacts, getLog());
        for (Artifact resolvedArtifact : resolvedArtifacts) {
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
            final String standardOut = executor.getStandardOut();
            if (standardOut != null && standardOut.contains("Failure")){
                throw new MojoExecutionException("Error installing " + apkFile + " to device. You might want to add command line parameter -Dandroid.uninstallApkBeforeInstallingToDevice=true or add plugin configuration tag <uninstallApkBeforeInstallingToDevice>true</uninstallApkBeforeInstallingToDevice>\n" + standardOut);
            }
        } catch (ExecutionException e) {
            getLog().error(executor.getStandardOut());
            getLog().error(executor.getStandardError());
            throw new MojoExecutionException("Error installing " + apkFile + " to device.", e);
        }
    }

    /**
     * Uninstalls an apk from a connected emulator or usb device. Also deletes the application's data and cache
     * directories on the device.
     * @param apkFile the file to uninstall
     * @return <code>true</code> if successfully uninstalled, <code>false</code> otherwise.
     */
    protected boolean uninstallApkFromDevice(File apkFile) throws MojoExecutionException {
        return uninstallApkFromDevice(apkFile, true);
    }

    /**
     * Uninstalls an apk from a connected emulator or usb device. Also deletes the application's data and cache
     * directories on the device.
     * @param apkFile the file to uninstall
     * @param deleteDataAndCacheDirectoriesOnDevice <code>true</code> to delete the application's data and cache
     * directories on the device, <code>false</code> to keep them.
     * @return <code>true</code> if successfully uninstalled, <code>false</code> otherwise.
     */
    protected boolean uninstallApkFromDevice(File apkFile, boolean deleteDataAndCacheDirectoriesOnDevice) throws MojoExecutionException {
        final String packageName;
        packageName = extractPackageNameFromApk(apkFile);
        return uninstallApkFromDevice(packageName, deleteDataAndCacheDirectoriesOnDevice);
    }

    /**
     * Uninstalls an apk, specified by package name, from a connected emulator or usb device. Also deletes the
     * application's data and cache directories on the device.
     * @param packageName the package name to uninstall.
     * @return <code>true</code> if successfully uninstalled, <code>false</code> otherwise.
     */
    protected boolean uninstallApkFromDevice(String packageName) {
        return uninstallApkFromDevice(packageName, true);
    }

    /**
     * Uninstalls an apk, specified by package name, from a connected emulator or usb device.
     * @param packageName the package name to uninstall.
     * @param deleteDataAndCacheDirectoriesOnDevice <code>true</code> to delete the application's data and cache
     * directories on the device, <code>false</code> to keep them.
     * @return <code>true</code> if successfully uninstalled, <code>false</code> otherwise.
     */
    protected boolean uninstallApkFromDevice(String packageName, boolean deleteDataAndCacheDirectoriesOnDevice) {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        List<String> commands = new ArrayList<String>();
        commands.add("uninstall");
        if (!deleteDataAndCacheDirectoriesOnDevice){
            commands.add("-k");  // ('-k' means keep the data and cache directories)
        }
        commands.add(packageName);
        getLog().info("adb " + commands.toString());
        try {
            executor.executeCommand("adb", commands, false);
            getLog().debug(executor.getStandardOut());
            getLog().debug(executor.getStandardError());
            return true;
        } catch (ExecutionException e) {
            getLog().error(executor.getStandardOut());
            getLog().error(executor.getStandardError());
            return false;
        }
    }

    /**
     * Extracts the package name from an apk file.
     * @param apkFile apk file to extract package name from.
     * @return the package name from inside the apk file.
     */
    protected String extractPackageNameFromApk(File apkFile) throws MojoExecutionException {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        List<String> commands = new ArrayList<String>();
        commands.add("dump");
        commands.add("xmltree");
        commands.add(apkFile.getAbsolutePath());
        commands.add("AndroidManifest.xml");
        getLog().info("aapt " + commands.toString());
        try {
            executor.executeCommand("aapt", commands, true);
            final String xmlTree = executor.getStandardOut();
            return extractPackageNameFromAndroidManifestXmlTree(xmlTree);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("Error while trying to figure out package name from inside apk file " + apkFile);
        }finally{
            getLog().error(executor.getStandardError());
        }
    }

    /**
     * Extracts the package name from an XmlTree dump of AndroidManifest.xml by the <code>aapt</code> tool.
     * @param aaptDumpXmlTree output from <code>aapt dump xmltree &lt;apkFile&gt; AndroidManifest.xml
     * @return the package name from inside the apkFile.
     */
    protected String extractPackageNameFromAndroidManifestXmlTree(String aaptDumpXmlTree){
        final Scanner scanner = new Scanner(aaptDumpXmlTree);
        // Finds the root element named "manifest".
        scanner.findWithinHorizon("^E: manifest", 0);
        // Finds the manifest element's attribute named "package".
        scanner.findWithinHorizon("  A: package=\"", 0);
        // Extracts the package value including the trailing double quote.
        String packageName =  scanner.next(".*?\"");
        // Removes the double quote.
        packageName = packageName.substring(0, packageName.length() - 1);
        return packageName;
    }

    protected String extractPackageNameFromAndroidManifest(File androidManifestFile) throws MojoExecutionException {
        final URL xmlURL;
        try {
            xmlURL = androidManifestFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error while trying to figure out package name from inside apk file " + androidManifestFile, e);
        }
        final DocumentContainer documentContainer = new DocumentContainer(xmlURL);
        final Object            packageName       = JXPathContext.newContext(documentContainer).getValue("manifest/@package", String.class);
        return (String) packageName;
    }

    protected int deleteFilesFromDirectory(String baseDirectory, String... includes) throws MojoExecutionException {
        final String[] files = findFilesInDirectory(baseDirectory, includes);
        if (files == null){
            return 0;
        }
        
        for (String file : files) {
            final boolean successfullyDeleted = new File(baseDirectory, file).delete();
            if (!successfullyDeleted){
                throw new MojoExecutionException("Failed to delete \"" + file +"\"");
            }
        }
        return files.length;
    }

    protected String[] findFilesInDirectory(String baseDirectory, String... includes) {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(baseDirectory);

        directoryScanner.setIncludes(includes);
        directoryScanner.addDefaultExcludes();

        directoryScanner.scan();
        String[] files = directoryScanner.getIncludedFiles();
        return files;

    }
}
