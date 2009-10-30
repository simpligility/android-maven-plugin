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
package com.jayway.maven.plugins.android;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.xml.DocumentContainer;
import org.apache.commons.lang.StringUtils;
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
 * @author hugo.josefson@jayway.com
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
     * The java sources directory.
     * @parameter default-value="${project.build.sourceDirectory}"
     * @readonly
     */
    protected File sourceDirectory;

    /**
     * The android resources directory.
     * @parameter default-value="${project.basedir}/res"
     */
    protected File resourceDirectory;

    /**
     * The android assets directory.
     * @parameter default-value="${project.basedir}/assets"
     */
    protected File assetsDirectory;

    /**
     * The <code>AndroidManifest.xml</code> file.
     * @parameter default-value="${project.basedir}/AndroidManifest.xml"
     */
    protected File androidManifestFile;

    /**
     * @parameter expression="${project.build.directory}/generated-sources/extracted-dependencies"
     * @readonly
     */
    protected File extractedDependenciesDirectory;

    /**
     * @parameter expression="${project.build.directory}/generated-sources/extracted-dependencies/res"
     * @readonly
     */
    protected File extractedDependenciesRes;
    /**
     * @parameter expression="${project.build.directory}/generated-sources/extracted-dependencies/assets"
     * @readonly
     */
    protected File extractedDependenciesAssets;
    /**
     * @parameter expression="${project.build.directory}/generated-sources/extracted-dependencies/src/main/java"
     * @readonly
     */
    protected File extractedDependenciesJavaSources;
    /**
     * @parameter expression="${project.build.directory}/generated-sources/extracted-dependencies/src/main/resources"
     * @readonly
     */
    protected File extractedDependenciesJavaResources;
    
    /**
     * @parameter expression="${project.build.directory}/generated-sources/combined-resources/res"
     * @readonly
     */
    protected File combinedRes;
    
    /**
     * Specifies which device to connect to, by serial number. Special values "usb" and "emulator" are also valid, for
     * selecting the only USB connected device or the only running emulator, respectively.
     * @parameter expression="${android.device}"
     */
    protected String device;

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
     * Maven ProjectHelper.
     *
     * @component
     * @readonly
     */
    protected MavenProjectHelper projectHelper;

    /**
     * <p>The Android SDK to use.</p>
     * <p>Looks like this:</p>
     * <pre>
     * &lt;sdk&gt;
     *     &lt;path&gt;/opt/android-sdk-linux_x86-1.5_r2&lt;/path&gt;
     *     &lt;platform&gt;1.1&lt;/platform&gt;
     * &lt;/sdk&gt;
     * </pre>
     * <p>The <code>&lt;platform&gt;</code> parameter is optional, and corresponds to the
     * <code>platforms/android-*</code> directories in the Android SDK directory. Default is the latest available
     * version, so you only need to set it if you for example want to use platform 1.1 from an 1.5 SDK. Has no effect
     * when used on an Android SDK 1.1.</p>
     * <p>If you want to use an environment variable for SDK path, do for example like this:</p>
     * <pre>
     * &lt;sdk&gt;
     *     &lt;path&gt;${env.ANDROID_SDK}&lt;/path&gt;
     * &lt;/sdk&gt;
     * </pre>
     * <p>Can also be configured from command-line with parameters <code>-Dandroid.sdk.path</code> and <code>-Dandroid.sdk.platform</code>.</p>
     * @parameter
     */
    private Sdk sdk;

    /**
     * <p>Parameter designed to pick up <code>-Dandroid.sdk.path</code> in case there is no pom with an
     * <code>&lt;sdk&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link Sdk#path}.</p>
     *
     * @parameter expression="${android.sdk.path}"
     * @readonly
     */
    private File sdkPath;



    /**
     * <p>Parameter designed to pick up <code>-Dandroid.sdk.platform</code> in case there is no pom with an
     * <code>&lt;sdk&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link Sdk#platform}.</p>
     *
     * @parameter expression="${android.sdk.platform}"
     * @readonly
     */
    private String sdkPlatform;

    /**
     * <p>Whether to undeploy an apk from the device before deploying it.</p>
     *
     * <p>Only has effect when running <code>mvn android:deploy</code> in an Android application project manually, or
     * when running <code>mvn integration-test</code> (or <code>mvn install</code>) in a project with instrumentation
     * tests.
     * </p>
     *
     * <p>It is useful to keep this set to <code>true</code> at all times, because if an apk with the same package was
     * previously signed with a different keystore, and deployed to the device, deployment will fail becuase your
     * keystore is different.</p>
     *
     * @parameter default-value=false
     *            expression="${android.undeployBeforeDeploy}"
     *
     */
    protected boolean undeployBeforeDeploy;

    /**
     * <p>Whether to attach the normal .jar file to the build, so it can be depended on by for example integration-tests
     * which may then access {@code R.java} from this project.</p>
     * <p>Only disable it if you know you won't need it for any integration-tests. Otherwise, leave it enabled.</p>
     *
     * @parameter default-value=true
     *            expression="${android.attachJar}"
     *
     */
    protected boolean attachJar;

    /**
     * <p>Whether to attach sources to the build, which can be depended on by other {@code apk} projects, for including
     * them in their builds.</p>
     * <p>Enabling this setting is only required if this project's source code and/or res(ources) will be included in
     * other projects, using the Maven &lt;dependency&gt; tag.</p>
     *
     * @parameter default-value=false
     *            expression="${android.attachSources}"
     *
     */
    protected boolean attachSources;

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
     * Deploys an apk file to a connected emulator or usb device.
     * @param apkFile the file to deploy
     * @throws MojoExecutionException If there is a problem deploying the apk file.
     */
    protected void deployApk(File apkFile) throws MojoExecutionException {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        List<String> commands = new ArrayList<String>();

        // Check if a specific device should be used
        if (StringUtils.isNotBlank(device)) {
            if ("usb".equals(device)) {
                commands.add("-d");
            } else if ("emulator".equals(device)) {
                commands.add("-e");
            } else {
                commands.add("-s");
                commands.add(device);
            }
        }

        commands.add("install");
        commands.add("-r");
        commands.add(apkFile.getAbsolutePath());
        getLog().info(getAndroidSdk().getPathForTool("adb")+" " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("adb"), commands, false);
            final String standardOut = executor.getStandardOut();
            if (standardOut != null && standardOut.contains("Failure")){
                throw new MojoExecutionException("Error deploying " + apkFile + " to device. You might want to add command line parameter -Dandroid.undeployBeforeDeploy=true or add plugin configuration tag <undeployBeforeDeploy>true</undeployBeforeDeploy>\n" + standardOut);
            }
        } catch (ExecutionException e) {
            getLog().error(executor.getStandardOut());
            getLog().error(executor.getStandardError());
            throw new MojoExecutionException("Error deploying " + apkFile + " to device.", e);
        }
    }

    /**
     * Undeploys an apk from a connected emulator or usb device. Also deletes the application's data and cache
     * directories on the device.
     * @param apkFile the file to undeploy
     * @return <code>true</code> if successfully uninstalled, <code>false</code> otherwise.
     */
    protected boolean undeployApk(File apkFile) throws MojoExecutionException {
        return undeployApk(apkFile, true);
    }

    /**
     * Undeploys an apk from a connected emulator or usb device. Also deletes the application's data and cache
     * directories on the device.
     * @param apkFile the file to undeploy
     * @param deleteDataAndCacheDirectoriesOnDevice <code>true</code> to delete the application's data and cache
     * directories on the device, <code>false</code> to keep them.
     * @return <code>true</code> if successfully undeployed, <code>false</code> otherwise.
     */
    protected boolean undeployApk(File apkFile, boolean deleteDataAndCacheDirectoriesOnDevice) throws MojoExecutionException {
        final String packageName;
        packageName = extractPackageNameFromApk(apkFile);
        return undeployApk(packageName, deleteDataAndCacheDirectoriesOnDevice);
    }

    /**
     * Undeploys an apk, specified by package name, from a connected emulator or usb device. Also deletes the
     * application's data and cache directories on the device.
     * @param packageName the package name to undeploy.
     * @return <code>true</code> if successfully undeployed, <code>false</code> otherwise.
     */
    protected boolean undeployApk(String packageName) throws MojoExecutionException {
        return undeployApk(packageName, true);
    }

    /**
     * Undeploys an apk, specified by package name, from a connected emulator or usb device.
     * @param packageName the package name to undeploy.
     * @param deleteDataAndCacheDirectoriesOnDevice <code>true</code> to delete the application's data and cache
     * directories on the device, <code>false</code> to keep them.
     * @return <code>true</code> if successfully undeployed, <code>false</code> otherwise.
     */
    protected boolean undeployApk(String packageName, boolean deleteDataAndCacheDirectoriesOnDevice) throws MojoExecutionException {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        List<String> commands = new ArrayList<String>();
        commands.add("uninstall");
        if (!deleteDataAndCacheDirectoriesOnDevice){
            commands.add("-k");  // ('-k' means keep the data and cache directories)
        }
        commands.add(packageName);
        getLog().info(getAndroidSdk().getPathForTool("adb")+" " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("adb"), commands, false);
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
        getLog().info(getAndroidSdk().getPathForTool("aapt")+" " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("aapt"), commands, true);
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
            throw new MojoExecutionException("Error while trying to figure out package name from inside AndroidManifest.xml file " + androidManifestFile, e);
        }
        final DocumentContainer documentContainer = new DocumentContainer(xmlURL);
        final Object            packageName       = JXPathContext.newContext(documentContainer).getValue("manifest/@package", String.class);
        return (String) packageName;
    }

    protected String extractInstrumentationRunnerFromAndroidManifest(File androidManifestFile) throws MojoExecutionException {
        final URL xmlURL;
        try {
            xmlURL = androidManifestFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error while trying to figure out instrumentation runner from inside AndroidManifest.xml file " + androidManifestFile, e);
        }
        final DocumentContainer documentContainer = new DocumentContainer(xmlURL);
        final Object            instrumentationRunner        = JXPathContext.newContext(documentContainer).getValue("manifest//instrumentation/@android:name", String.class);
        return (String) instrumentationRunner;
    }

    protected int deleteFilesFromDirectory(File baseDirectory, String... includes) throws MojoExecutionException {
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

    /**
     * Finds files.
     * @param baseDirectory Directory to find files in.
     * @param includes Ant-style include statements, for example <code>"** /*.aidl"</code> (but without the space in the middle)
     * @return <code>String[]</code> of the files' paths and names, relative to <code>baseDirectory</code>. Empty <code>String[]</code> if <code>baseDirectory</code> does not exist.
     */
    protected String[] findFilesInDirectory(File baseDirectory, String... includes) {
        if (baseDirectory.exists()){
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir(baseDirectory);

            directoryScanner.setIncludes(includes);
            directoryScanner.addDefaultExcludes();

            directoryScanner.scan();
            String[] files = directoryScanner.getIncludedFiles();
            return files;
        }else{
            return new String[0];
        }

    }

    /**
     * Returns the Android SDK to use.
     * @return the Android SDK to use.
     */
    protected AndroidSdk getAndroidSdk(){
        if (sdk==null){
            return new AndroidSdk(sdkPath, sdkPlatform);
        }else{
            return new AndroidSdk(sdk.getPath(), sdk.getPlatform());
        }
    }

}
