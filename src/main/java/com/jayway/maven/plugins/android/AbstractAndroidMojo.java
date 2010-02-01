/*
 * Copyright (C) 2009, 2010 Jayway AB
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
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.apache.commons.jxpath.xml.DocumentContainer;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
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

import static org.apache.commons.lang.StringUtils.isBlank;

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
     * The android resources overlay directory. This will be overriden 
     * by resourceOverlayDirectories if present. 
     * @parameter default-value="${project.basedir}/res-overlay"
     */
    protected File resourceOverlayDirectory;

    /**
     * The android resources overlay directories. If this is specified,
     * the {@link #resourceOverlayDirectory} parameter will be ignored.
     * @parameter 
     */
    protected File[] resourceOverlayDirectories;

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
     * The combined resources directory. This will contain both the resources found in "res" as well as any resources contained in a apksources dependency.
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
     *     &lt;path&gt;/opt/android-sdk-linux&lt;/path&gt;
     *     &lt;platform&gt;2.1&lt;/platform&gt;
     * &lt;/sdk&gt;
     * </pre>
     * <p>The <code>&lt;platform&gt;</code> parameter is optional, and corresponds to the
     * <code>platforms/android-*</code> directories in the Android SDK directory. Default is the latest available
     * version, so you only need to set it if you for example want to use platform 1.1 from an 1.5 SDK. Has no effect
     * when used on an Android SDK 1.1.</p>
     * <p>The <code>&lt;path&gt;</code> parameter is optional. The default is the setting of the ANDROID_HOME environment
     * variable. The parameter can be used to override this setting with a different environment variable like this:</p>
     * <pre>
     * &lt;sdk&gt;
     *     &lt;path&gt;${env.ANDROID_SDK}&lt;/path&gt;
     * &lt;/sdk&gt;
     * </pre>
     * <p>or just with a hardcoded absolute path. The parameters can also be configured from command-line with parameters
     * <code>-Dandroid.sdk.path</code> and <code>-Dandroid.sdk.platform</code>.</p>
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
     * <p>Parameter designed to pick up environment variable <code>ANDROID_HOME</code> in case
     * <code>android.sdk.path</code> is not configured.</p>
     *
     * @parameter expression="${env.ANDROID_HOME}"
     * @readonly
     */
    private String envANDROID_HOME;

    /**
     * The <code>ANDROID_HOME</code> environment variable name.
     */
    public static final String ENV_ANDROID_HOME = "ANDROID_HOME";

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
     * Accessor for the local repository.
     * @return The local repository.
     */
    protected ArtifactRepository getLocalRepository()
    {
    	return localRepository;
    }

    /**
     * Which dependency scopes should not be included when unpacking dependencies into the apk.
     */
    protected static final List<String> EXCLUDED_DEPENDENCY_SCOPES = Arrays.asList("provided", "system", "import");

    /**
     * @return a {@code Set} of dependencies which may be extracted and otherwise included in other artifacts. Never
     * {@code null}. This excludes artifacts of the {@code EXCLUDED_DEPENDENCY_SCOPES} scopes.
     */
    protected Set<Artifact> getRelevantCompileArtifacts() {
        final List<Artifact> allArtifacts = (List<Artifact>) project.getCompileArtifacts();
        final Set<Artifact> results = filterOutIrrelevantArtifacts(allArtifacts);
        return results;
    }

    /**
     * @return a {@code Set} of direct project dependencies. Never {@code null}. This excludes artifacts of the {@code
     * EXCLUDED_DEPENDENCY_SCOPES} scopes.
     */
    protected Set<Artifact> getRelevantDependencyArtifacts() {
        final Set<Artifact> allArtifacts = (Set<Artifact>) project.getDependencyArtifacts();
        final Set<Artifact> results = filterOutIrrelevantArtifacts(allArtifacts);
        return results;
    }

    private Set<Artifact> filterOutIrrelevantArtifacts(Iterable<Artifact> allArtifacts) {
        final Set<Artifact> results = new HashSet<Artifact>();
        for (Artifact artifact : allArtifacts) {
            if (artifact == null){
                continue;
            }

            if (EXCLUDED_DEPENDENCY_SCOPES.contains(artifact.getScope())){
                continue;
            }

            // TODO: this statement must be retired in version 3.0, but we can't do that yet because we promised to not break backwards compatibility within the 2.x series.
            if (artifact.getGroupId().equals("android")) {
                getLog().warn("Excluding the android.jar from being unpacked into your apk file, based on its <groupId>android</groupId>. Please set <scope>provided</scope> in that dependency, because that is the correct way, and the only which will work in the future.");
                continue;
            }

            results.add(artifact);
        }
        return results;
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

    /**
     * Attempts to find the instrumentation test runner from inside the AndroidManifest.xml file.
     *
     * @param androidManifestFile the AndroidManifest.xml file to inspect.
     * @return the instrumentation test runner declared in AndroidManifest.xml, or {@code null} if it is not declared.
     * @throws MojoExecutionException
     */
    protected String extractInstrumentationRunnerFromAndroidManifest(File androidManifestFile) throws MojoExecutionException {
        final URL xmlURL;
        try {
            xmlURL = androidManifestFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error while trying to figure out instrumentation runner from inside AndroidManifest.xml file " + androidManifestFile, e);
        }
        final DocumentContainer documentContainer     = new DocumentContainer(xmlURL);
        final Object            instrumentationRunner;
        try {
            instrumentationRunner = JXPathContext.newContext(documentContainer).getValue("manifest//instrumentation/@android:name", String.class);
        } catch (JXPathNotFoundException e) {
            return null;
        }
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
     * <p>Returns the Android SDK to use.</p>
     *
     * <p>Current implementation looks for <code>&lt;sdk&gt;&lt;path&gt;</code> configuration in pom, then System
     * property <code>android.sdk.path</code>, then environment variable <code>ANDROID_HOME</code>.
     *
     * <p>This is where we collect all logic for how to lookup where it is, and which one to choose. The lookup is
     * based on available parameters. This method should be the only one you should need to look at to understand how
     * the Android SDK is chosen, and from where on disk.</p>
     *
     * @return the Android SDK to use.
     */
    protected AndroidSdk getAndroidSdk() throws MojoExecutionException {
        File   chosenSdkPath;
        String chosenSdkPlatform;

        if (sdk != null) {
            // An <sdk> tag exists in the pom.

            if (sdk.getPath() != null){
                // An <sdk><path> tag is set in the pom.

                chosenSdkPath = sdk.getPath();
            }else{
                // There is no <sdk><path> tag in the pom.

                if (sdkPath != null){
                    // -Dandroid.sdk.path is set on command line, or via <properties><sdk.path>...
                    chosenSdkPath = sdkPath;
                }else{
                    // No -Dandroid.sdk.path is set on command line, or via <properties><sdk.path>...
                    chosenSdkPath = new File(getAndroidHomeOrThrow());
                }
            }

            // Use <sdk><platform> from pom if it's there, otherwise try -Dandroid.sdk.platform from command line or <properties><sdk.platform>...
            if (!isBlank(sdk.getPlatform())){
                chosenSdkPlatform = sdk.getPlatform();
            }else{
                chosenSdkPlatform = sdkPlatform;
            }
        } else {
            // There is no <sdk> tag in the pom.

            if (sdkPath != null){
                // -Dandroid.sdk.path is set on command line, or via <properties><sdk.path>...
                chosenSdkPath = sdkPath;
            }else{
                // No -Dandroid.sdk.path is set on command line, or via <properties><sdk.path>...
                chosenSdkPath = new File(getAndroidHomeOrThrow());
            }

            // Use any -Dandroid.sdk.platform from command line or <properties><sdk.platform>...
            chosenSdkPlatform = sdkPlatform;
        }
        
        return new AndroidSdk(chosenSdkPath, chosenSdkPlatform);
    }

    private String getAndroidHomeOrThrow() throws MojoExecutionException {
        final String androidHome = System.getenv(ENV_ANDROID_HOME);
        if (isBlank(androidHome)){
            throw new MojoExecutionException("No Android SDK path could be found. You may configure it in the pom using <sdk><path>...</path></sdk> or <properties><sdk.path>...</sdk.path></properties> or on command-line using -Dandroid.sdk.path=... or by setting environment variable " + ENV_ANDROID_HOME);
        }
        return androidHome;
    }

}
