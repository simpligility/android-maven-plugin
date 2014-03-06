/*
 * Copyright (C) 2009-2011 Jayway AB
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

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.jayway.maven.plugins.android.common.AndroidExtension;
import com.jayway.maven.plugins.android.common.BuildHelper;
import com.jayway.maven.plugins.android.common.DependencyResolver;
import com.jayway.maven.plugins.android.common.DeviceHelper;
import com.jayway.maven.plugins.android.common.MavenToPlexusLogAdapter;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.configuration.Ndk;
import com.jayway.maven.plugins.android.configuration.Sdk;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.apache.commons.jxpath.xml.DocumentContainer;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Contains common fields and methods for android mojos.
 *
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 * @author William Ferguson <william.ferguson@xandar.com.au>
 * @author Malachi de AElfweald malachid@gmail.com
 * @author Roy Clarkson <rclarkson@gopivotal.com>
 */
public abstract class AbstractAndroidMojo extends AbstractMojo
{

    public static final List<String> SUPPORTED_PACKAGING_TYPES = new ArrayList<String>();

    static
    {
        SUPPORTED_PACKAGING_TYPES.add( AndroidExtension.APK );
    }

    /**
     * Android Debug Bridge initialization timeout in milliseconds.
     */
    private static final long ADB_TIMEOUT_MS = 60L * 1000;

    /**
     * The <code>ANDROID_NDK_HOME</code> environment variable name.
     */
    public static final String ENV_ANDROID_NDK_HOME = "ANDROID_NDK_HOME";

    /**
     * <p>The Android NDK to use.</p>
     * <p>Looks like this:</p>
     * <pre>
     * &lt;ndk&gt;
     *     &lt;path&gt;/opt/android-ndk-r4&lt;/path&gt;
     * &lt;/ndk&gt;
     * </pre>
     * <p>The <code>&lt;path&gt;</code> parameter is optional. The default is the setting of the ANDROID_NDK_HOME
     * environment variable. The parameter can be used to override this setting with a different environment variable
     * like this:</p>
     * <pre>
     * &lt;ndk&gt;
     *     &lt;path&gt;${env.ANDROID_NDK_HOME}&lt;/path&gt;
     * &lt;/ndk&gt;
     * </pre>
     * <p>or just with a hardcoded absolute path. The parameters can also be configured from command-line with parameter
     * <code>-Dandroid.ndk.path</code>.</p>
     *
     * @parameter
     */
    @ConfigPojo( prefix = "ndk" )
    private Ndk ndk;

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
     * @parameter expression="${mojoExecution}"
     * @readonly
     * @required
     *
     */
    protected MojoExecution execution;

    /**
     * The java sources directory.
     *
     * @parameter default-value="${project.build.sourceDirectory}"
     * @readonly
     */
    protected File sourceDirectory;

    /**
     * The java target directory. Ie target/classes.
     *
     * @parameter default-value="${project.build.directory}"
     * @readonly
     */
    protected File targetDirectory;

    /**
     * The android resources directory.
     *
     * @parameter default-value="${project.basedir}/res"
     */
    protected File resourceDirectory;

    /**
     * <p>Root folder containing native libraries to include in the application package.</p>
     *
     * @parameter expression="${android.nativeLibrariesDirectory}" default-value="${project.basedir}/libs"
     */
    protected File nativeLibrariesDirectory;


    /**
     * The android resources overlay directory. This will be overridden
     * by resourceOverlayDirectories if present.
     *
     * @parameter default-value="${project.basedir}/res-overlay"
     */
    protected File resourceOverlayDirectory;

    /**
     * The android resources overlay directories. If this is specified,
     * the {@link #resourceOverlayDirectory} parameter will be ignored.
     *
     * @parameter
     */
    protected File[] resourceOverlayDirectories;

    /**
     * The android assets directory.
     *
     * @parameter default-value="${project.basedir}/assets"
     */
    protected File assetsDirectory;

    /**
     * Source <code>AndroidManifest.xml</code> file to copy into the {@link #androidManifestFile} location.
     *
     * @parameter expression="${source.manifestFile}"
     */
    protected File sourceManifestFile;

    /**
     * The <code>AndroidManifest.xml</code> file.
     *
     * @parameter expression="${android.manifestFile}" default-value="${project.basedir}/AndroidManifest.xml"
     */
    protected File androidManifestFile;

    /**
     * <p>A possibly new package name for the application. This value will be passed on to the aapt
     * parameter --rename-manifest-package. Look to aapt for more help on this. </p>
     *
     * @parameter expression="${android.renameManifestPackage}"
     */
    protected String renameManifestPackage;

    /**
     * @parameter expression="${project.build.directory}/generated-sources/extracted-dependencies"
     * @readonly
     */
    protected File extractedDependenciesDirectory;

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
     * The combined assets directory. This will contain both the assets found in "assets" as well as any assets
     * contained in a apksources, apklib or aar dependencies.
     *
     * @parameter expression="${project.build.directory}/generated-sources/combined-assets"
     * @readonly
     */
    protected File combinedAssets;

    /**
     * Specifies which the serial number of the device to connect to. Using the special values "usb" or
     * "emulator" is also valid. "usb" will connect to all actual devices connected (via usb). "emulator" will
     * connect to all emulators connected. Multiple devices will be iterated over in terms of goals to run. All
     * device interaction goals support this so you can e.. deploy the apk to all attached emulators and devices.
     * Goals supporting this are devices, deploy, undeploy, redeploy, pull, push and instrument.
     *
     * @parameter expression="${android.device}"
     */
    protected String device;

    /**
     * <p>Specifies a list of serial numbers of each device you want to connect to. Using the special values "usb" or
     * "emulator" is also valid. "usb" will connect to all actual devices connected (via usb). "emulator" will
     * connect to all emulators connected. Multiple devices will be iterated over in terms of goals to run. All
     * device interaction goals support this so you can e.. deploy the apk to all attached emulators and devices.
     * Goals supporting this are devices, deploy, undeploy, redeploy, pull, push and instrument.</p>
     * <pre>
     * &lt;devices&gt;
     *     &lt;device&gt;usb&lt;/device&gt;
     *     &lt;device&gt;emulator-5554&lt;/device&gt;
     * &lt;/devices&gt;
     * </pre>
     * <p>This parameter can also be configured from command-line with
     * parameter <code>-Dandroid.devices=usb,emulator</code>.</p>
     *
     * @parameter expression="${android.devices}"
     */
    protected String[] devices;
    
    /**
     * <p>Specifies the number of threads to use for deploying and testing on attached devices.
     * 
     * <p>This parameter can also be configured from command-line with
     * parameter <code>-Dandroid.deviceThreads=2</code>.</p>
     *
     * @parameter expression="${android.deviceThreads}"
     */
    protected int deviceThreads;

    /**
     * A selection of configurations to be included in the APK as a comma separated list. This will limit the
     * configurations for a certain type. For example, specifying <code>hdpi</code> will exclude all resource folders
     * with the <code>mdpi</code> or <code>ldpi</code> modifiers, but won't affect language or orientation modifiers.
     * For more information about this option, look in the aapt command line help.
     *
     * @parameter expression="${android.configurations}"
     */
    protected String configurations;

    /**
     * A list of extra arguments that must be passed to aapt.
     *
     * @parameter expression="${android.aaptExtraArgs}"
     */
    protected String[] aaptExtraArgs;

    /**
     * Automatically create a ProGuard configuration file that will guard Activity classes and the like that are 
     * defined in the AndroidManifest.xml. This files is then automatically used in the proguard mojo execution, 
     * if enabled.
     *
     * @parameter expression="${android.proguardFile}"
     */
    protected File proguardFile;

    /**
     * Decides whether the Apk should be generated or not. If set to false, dx and apkBuilder will not run. This is
     * probably most useful for a project used to generate apk sources to be inherited into another application
     * project.
     *
     * @parameter expression="${android.generateApk}" default-value="true"
     */
    protected boolean generateApk;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    protected RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    protected RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of project dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    protected List<RemoteRepository> projectRepos;

    /**
     * @component
     * @required
     * @readonly
     */
    private ArtifactHandler artifactHandler;

    /**
     * Generates R.java into a different package.
     *
     * @parameter expression="${android.customPackage}"
     */
    protected String customPackage;

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
     * version, so you only need to set it if you for example want to use platform 1.5 but also have e.g. 2.2 installed.
     * Has no effect when used on an Android SDK 1.1. The parameter can also be coded as the API level. Therefore valid
     * values are 1.1, 1.5, 1.6, 2.0, 2.01, 2.1, 2.2 and so as well as 3, 4, 5, 6, 7, 8... 16. If a platform/api level 
     * is not installed on the machine an error message will be produced. </p>
     * <p>The <code>&lt;path&gt;</code> parameter is optional. The default is the setting of the ANDROID_HOME
     * environment variable. The parameter can be used to override this setting with a different environment variable
     * like this:</p>
     * <pre>
     * &lt;sdk&gt;
     *     &lt;path&gt;${env.ANDROID_SDK}&lt;/path&gt;
     * &lt;/sdk&gt;
     * </pre>
     * <p>or just with a hard-coded absolute path. The parameters can also be configured from command-line with
     * parameters <code>-Dandroid.sdk.path</code> and <code>-Dandroid.sdk.platform</code>.</p>
     *
     * @parameter
     */
    private Sdk sdk;

    /**
     * <p>Parameter designed to pick up <code>-Dandroid.sdk.path</code> in case there is no pom with an
     * <code>&lt;sdk&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link com.jayway.maven.plugins.android.configuration.Sdk#path}.</p>
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
    private String envAndroidHome;

    /**
     * The <code>ANDROID_HOME</code> environment variable name.
     */
    public static final String ENV_ANDROID_HOME = "ANDROID_HOME";

    /**
     * <p>Parameter designed to pick up <code>-Dandroid.sdk.platform</code> in case there is no pom with an
     * <code>&lt;sdk&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link com.jayway.maven.plugins.android.configuration.Sdk#platform}.</p>
     *
     * @parameter expression="${android.sdk.platform}"
     * @readonly
     */
    private String sdkPlatform;

    /**
     * <p>Whether to undeploy an apk from the device before deploying it.</p>
     * <p/>
     * <p>Only has effect when running <code>mvn android:deploy</code> in an Android application project manually, or
     * when running <code>mvn integration-test</code> (or <code>mvn install</code>) in a project with instrumentation
     * tests.
     * </p>
     * <p/>
     * <p>It is useful to keep this set to <code>true</code> at all times, because if an apk with the same package was
     * previously signed with a different keystore, and deployed to the device, deployment will fail becuase your
     * keystore is different.</p>
     *
     * @parameter default-value=false
     * expression="${android.undeployBeforeDeploy}"
     */
    protected boolean undeployBeforeDeploy;

    /**
     * <p>Whether to attach the normal .jar file to the build, so it can be depended on by for example integration-tests
     * which may then access {@code R.java} from this project.</p>
     * <p>Only disable it if you know you won't need it for any integration-tests. Otherwise, leave it enabled.</p>
     *
     * @parameter default-value=true
     * expression="${android.attachJar}"
     */
    protected boolean attachJar;

    /**
     * <p>Whether to attach sources to the build, which can be depended on by other {@code apk} projects, for including
     * them in their builds.</p>
     * <p>Enabling this setting is only required if this project's source code and/or res(ources) will be included in
     * other projects, using the Maven &lt;dependency&gt; tag.</p>
     *
     * @parameter default-value=false
     * expression="${android.attachSources}"
     */
    protected boolean attachSources;

    /**
     * <p>Parameter designed to pick up <code>-Dandroid.ndk.path</code> in case there is no pom with an
     * <code>&lt;ndk&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link com.jayway.maven.plugins.android.configuration.Ndk#path}.</p>
     *
     * @parameter expression="${android.ndk.path}"
     * @readonly
     */
    private File ndkPath;

    /**
     * Whether to create a release build (default is false / debug build). This affect BuildConfig generation 
     * and apk generation at this stage, but should probably affect other aspects of the build.
     * @parameter expression="${android.release}" default-value="false"
     */
    protected boolean release;

    private BuildHelper buildHelper;

    /**
     *
     */
    private static final Object ADB_LOCK = new Object();
    /**
     *
     */
    private static boolean adbInitialized = false;

    protected final DependencyResolver getDependencyResolver()
    {
        return new DependencyResolver(
                new MavenToPlexusLogAdapter( getLog() ),
                repoSystem, repoSession, projectRepos, artifactHandler
        );
    }

    /**
     * @return a {@code Set} of dependencies which may be extracted and otherwise included in other artifacts. Never
     *         {@code null}. This excludes artifacts of the {@code EXCLUDED_DEPENDENCY_SCOPES} scopes.
     */
    protected Set<Artifact> getRelevantCompileArtifacts()
    {
        final List<Artifact> allArtifacts = project.getCompileArtifacts();
        return getBuildHelper().getFilteredArtifacts( allArtifacts );
    }

    /**
     * @return a {@code Set} of direct project dependencies. Never {@code null}. This excludes artifacts of the {@code
     *         EXCLUDED_DEPENDENCY_SCOPES} scopes.
     */
    protected Set<Artifact> getDirectDependencyArtifacts()
    {
        final Set<Artifact> allArtifacts = project.getDependencyArtifacts();
        return getBuildHelper().getFilteredArtifacts( allArtifacts );
    }

    /**
     * Provides transitive dependency artifacts only defined types based on {@code types} argument
     * or all types if {@code types} argument is empty
     *
     * @param types artifact types to be selected
     * @return a {@code List} of all project dependencies. Never {@code null}. This excludes artifacts of the {@code
     *         EXCLUDED_DEPENDENCY_SCOPES} scopes. And
     *         This should maintain dependency order to comply with library project resource precedence.
     */
    protected Set<Artifact> getTransitiveDependencyArtifacts( String... types )
    {
        return getBuildHelper().getFilteredArtifacts( project.getArtifacts(), types );
    }

    /**
     * Attempts to resolve an {@link Artifact} to a {@link File}.
     *
     * @param artifact to resolve
     * @return a {@link File} to the resolved artifact, never <code>null</code>.
     * @throws MojoExecutionException if the artifact could not be resolved.
     */
    protected File resolveArtifactToFile( Artifact artifact ) throws MojoExecutionException
    {
        return getBuildHelper().resolveArtifactToFile( artifact );
    }

    /**
     * Initialize the Android Debug Bridge and wait for it to start. Does not reinitialize it if it has
     * already been initialized (that would through and IllegalStateException...). Synchronized sine
     * the init call in the library is also synchronized .. just in case.
     *
     * @return
     */
    protected AndroidDebugBridge initAndroidDebugBridge() throws MojoExecutionException
    {
        synchronized ( ADB_LOCK )
        {
            if ( ! adbInitialized )
            {
                AndroidDebugBridge.init( false );
                adbInitialized = true;
            }
            AndroidDebugBridge androidDebugBridge = AndroidDebugBridge
                    .createBridge( getAndroidSdk().getAdbPath(), false );
            waitUntilConnected( androidDebugBridge );
            return androidDebugBridge;
        }
    }

    /**
     * Run a wait loop until adb is connected or trials run out. This method seems to work more reliably then using a
     * listener.
     *
     * @param adb
     */
    private void waitUntilConnected( AndroidDebugBridge adb )
    {
        int trials = 10;
        final int connectionWaitTime = 50;
        while ( trials > 0 )
        {
            try
            {
                Thread.sleep( connectionWaitTime );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
            if ( adb.isConnected() )
            {
                break;
            }
            trials--;
        }
    }

    /**
     * Wait for the Android Debug Bridge to return an initial device list.
     *
     * @param androidDebugBridge
     * @throws MojoExecutionException
     */
    protected void waitForInitialDeviceList( final AndroidDebugBridge androidDebugBridge ) throws MojoExecutionException
    {
        if ( ! androidDebugBridge.hasInitialDeviceList() )
        {
            getLog().info( "Waiting for initial device list from the Android Debug Bridge" );
            long limitTime = System.currentTimeMillis() + ADB_TIMEOUT_MS;
            while ( ! androidDebugBridge.hasInitialDeviceList() && ( System.currentTimeMillis() < limitTime ) )
            {
                try
                {
                    Thread.sleep( 1000 );
                }
                catch ( InterruptedException e )
                {
                    throw new MojoExecutionException(
                            "Interrupted waiting for initial device list from Android Debug Bridge" );
                }
            }
            if ( ! androidDebugBridge.hasInitialDeviceList() )
            {
                getLog().error( "Did not receive initial device list from the Android Debug Bridge." );
            }
        }
    }

    /**
     * Deploys an apk file to a connected emulator or usb device.
     *
     * @param apkFile the file to deploy
     * @throws MojoExecutionException If there is a problem deploying the apk file.
     */
    protected void deployApk( final File apkFile ) throws MojoExecutionException, MojoFailureException
    {
        if ( undeployBeforeDeploy )
        {
            undeployApk( apkFile );
        }
        doWithDevices( new DeviceCallback()
        {
            public void doWithDevice( final IDevice device ) throws MojoExecutionException
            {
                String deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );
                try
                {
                    String result = device.installPackage( apkFile.getAbsolutePath(), true );
                    // according to the docs for installPackage, not null response is error
                    if ( result != null )
                    {
                        throw new MojoExecutionException( deviceLogLinePrefix
                                + "Install of " + apkFile.getAbsolutePath()
                                + " failed - [" + result + "]" );
                    }
                    getLog().info( deviceLogLinePrefix + "Successfully installed " + apkFile.getAbsolutePath() ); 
                    getLog().debug( " to " + DeviceHelper.getDescriptiveName( device ) );
                }
                catch ( InstallException e )
                {
                    throw new MojoExecutionException( deviceLogLinePrefix + "Install of " + apkFile.getAbsolutePath()
                            + " failed.", e );
                }
            }
        } );
    }

    /**
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected void deployDependencies() throws MojoExecutionException, MojoFailureException
    {
        Set<Artifact> directDependentArtifacts = project.getDependencyArtifacts();
        if ( directDependentArtifacts != null )
        {
            for ( Artifact artifact : directDependentArtifacts )
            {
                String type = artifact.getType();
                if ( type.equals( APK ) )
                {
                    getLog().debug( "Detected apk dependency " + artifact + ". Will resolve and deploy to device..." );
                    final File targetApkFile = resolveArtifactToFile( artifact );
                    if ( undeployBeforeDeploy )
                    {
                        getLog().debug( "Attempting undeploy of " + targetApkFile + " from device..." );
                        undeployApk( targetApkFile );
                    }
                    getLog().debug( "Deploying " + targetApkFile + " to device..." );
                    deployApk( targetApkFile );
                }
            }
        }
    }

    /**
     * Deploy the apk built with the current projects to all attached devices and emulators. 
     * Skips other projects in a multi-module build without terminating.
     * 
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected void deployBuiltApk() throws MojoExecutionException, MojoFailureException
    {
        if ( project.getPackaging().equals( APK ) )
        {
            File apkFile = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + "." + APK );
            deployApk( apkFile );
        }
        else 
        {
            getLog().info( "Project packaging is not apk, skipping deployment." );
        }
    }


    /**
     * Performs the callback action on the devices determined by
     * {@link #shouldDoWithThisDevice(com.android.ddmlib.IDevice)}
     *
     * @param deviceCallback the action to perform on each device
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          in case there is a problem
     * @throws org.apache.maven.plugin.MojoFailureException
     *          in case there is a problem
     */
    protected void doWithDevices( final DeviceCallback deviceCallback )
            throws MojoExecutionException, MojoFailureException
    {
        final AndroidDebugBridge androidDebugBridge = initAndroidDebugBridge();

        if ( !androidDebugBridge.isConnected() )
        {
            throw new MojoExecutionException( "Android Debug Bridge is not connected." );
        }

        waitForInitialDeviceList( androidDebugBridge );
        List<IDevice> devices = Arrays.asList( androidDebugBridge.getDevices() );
        int numberOfDevices = devices.size();
        getLog().debug( "Found " + numberOfDevices + " devices connected with the Android Debug Bridge" );
        if ( devices.size() == 0 )
        {
            throw new MojoExecutionException( "No online devices attached." );
        }

        int threadCount = getDeviceThreads();
        if ( getDeviceThreads() == 0 )
        {
            getLog().info( "android.devicesThreads parameter not set, using a thread for each attached device" );
            threadCount = numberOfDevices;
        }
        else
        {
            getLog().info( "android.devicesThreads parameter set to " + getDeviceThreads() );
        }

        boolean shouldRunOnAllDevices = getDevices().size() == 0;
        if ( shouldRunOnAllDevices )
        {
            getLog().info( "android.devices parameter not set, using all attached devices" );
        }
        else
        {
            getLog().info( "android.devices parameter set to " + getDevices().toString() );
        }

        ArrayList<DoThread> doThreads = new ArrayList<DoThread>();
        ExecutorService executor = Executors.newFixedThreadPool( threadCount );
        for ( final IDevice idevice : devices )
        {
            if ( shouldRunOnAllDevices )
            {
                String deviceType = idevice.isEmulator() ? "Emulator " : "Device ";
                getLog().info( deviceType + DeviceHelper.getDescriptiveName( idevice ) + " found." );
            }
            if ( shouldRunOnAllDevices || shouldDoWithThisDevice( idevice ) )
            {
                DoThread deviceDoThread = new DoThread() {
                    public void runDo() throws MojoFailureException, MojoExecutionException
                    {
                        deviceCallback.doWithDevice( idevice );
                    }
                };
                doThreads.add( deviceDoThread );
                executor.execute( deviceDoThread );
            }
        }
        executor.shutdown();
        while ( ! executor.isTerminated() )
        {
            // waiting for threads finish
        }
        throwAnyDoThreadErrors( doThreads );

        if ( ! shouldRunOnAllDevices && doThreads.isEmpty() )
        {
            throw new MojoExecutionException( "No device found for android.device=" + getDevices().toString() );
        }
    }

    private void throwAnyDoThreadErrors( ArrayList<DoThread> doThreads ) throws MojoExecutionException,
            MojoFailureException
    {
        for ( DoThread deviceDoThread : doThreads )
        {
            if ( deviceDoThread.failure != null )
            {
                throw deviceDoThread.failure;
            }
            if ( deviceDoThread.execution != null )
            {
                throw deviceDoThread.execution;
            }
        }
    }

    /**
     * Determines if this {@link IDevice}(s) should be used
     *
     * @param idevice the device to check
     * @return if the device should be used
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          in case there is a problem
     * @throws org.apache.maven.plugin.MojoFailureException
     *          in case there is a problem
     */
    private boolean shouldDoWithThisDevice( IDevice idevice ) throws MojoExecutionException, MojoFailureException
    {

        for ( String device : getDevices() )
        {
            // use specified device or all emulators or all devices
            if ( "emulator".equals( device ) && idevice.isEmulator() )
            {
                return true;
            }

            if ( "usb".equals( device ) && ! idevice.isEmulator() )
            {
                return true;
            }

            if ( idevice.isEmulator() && ( device.equalsIgnoreCase( idevice.getAvdName() ) || device
                    .equalsIgnoreCase( idevice.getSerialNumber() ) ) )
            {
                return true;
            }

            if ( ! idevice.isEmulator() && device.equals( idevice.getSerialNumber() ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Undeploys an apk from a connected emulator or usb device. Also deletes the application's data and cache
     * directories on the device.
     *
     * @param apkFile the file to undeploy
     * @return <code>true</code> if successfully undeployed, <code>false</code> otherwise.
     */
    protected boolean undeployApk( File apkFile ) throws MojoExecutionException, MojoFailureException
    {
        final String packageName;
        packageName = extractPackageNameFromApk( apkFile );
        return undeployApk( packageName );
    }

    /**
     * Undeploys an apk, specified by package name, from a connected emulator
     * or usb device. Also deletes the application's data and cache
     * directories on the device.
     *
     * @param packageName the package name to undeploy.
     * @return <code>true</code> if successfully undeployed, <code>false</code> otherwise.
     */
    protected boolean undeployApk( final String packageName ) throws MojoExecutionException, MojoFailureException
    {

        final AtomicBoolean result = new AtomicBoolean( true ); // if no devices are present, it counts as successful

        doWithDevices( new DeviceCallback()
        {
            public void doWithDevice( final IDevice device ) throws MojoExecutionException
            {
                String deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );
                try
                {
                    device.uninstallPackage( packageName );
                    getLog().info( deviceLogLinePrefix + "Successfully uninstalled " + packageName );
                    getLog().debug( " from " + DeviceHelper.getDescriptiveName( device ) );
                    result.set( true );
                }
                catch ( InstallException e )
                {
                    result.set( false );
                    throw new MojoExecutionException( deviceLogLinePrefix + "Uninstall of " + packageName
                            + " failed.", e );
                }
            }
        } );

        return result.get();
    }

    /**
     * Extracts the package name from an apk file.
     *
     * @param apkFile apk file to extract package name from.
     * @return the package name from inside the apk file.
     */
    protected String extractPackageNameFromApk( File apkFile ) throws MojoExecutionException
    {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );
        executor.setCaptureStdOut( true );
        executor.setCaptureStdErr( true );
        List<String> commands = new ArrayList<String>();
        commands.add( "dump" );
        commands.add( "xmltree" );
        commands.add( apkFile.getAbsolutePath() );
        commands.add( "AndroidManifest.xml" );
        getLog().info( getAndroidSdk().getAaptPath() + " " + commands.toString() );
        try
        {
            executor.executeCommand( getAndroidSdk().getAaptPath(), commands, false );
            final String xmlTree = executor.getStandardOut();
            return extractPackageNameFromAndroidManifestXmlTree( xmlTree );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException(
                    "Error while trying to figure out package name from inside apk file " + apkFile );
        }
        finally
        {
            String errout = executor.getStandardError();
            if ( ( errout != null ) && ( errout.trim().length() > 0 ) )
            {
                getLog().error( errout );
            }
        }
    }

    /**
     * Extracts the package name from an XmlTree dump of AndroidManifest.xml by the <code>aapt</code> tool.
     *
     * @param aaptDumpXmlTree output from <code>aapt dump xmltree &lt;apkFile&gt; AndroidManifest.xml
     * @return the package name from inside the apkFile.
     */
    protected String extractPackageNameFromAndroidManifestXmlTree( String aaptDumpXmlTree )
    {
        final Scanner scanner = new Scanner( aaptDumpXmlTree );
        // Finds the root element named "manifest".
        scanner.findWithinHorizon( "^E: manifest", 0 );
        // Finds the manifest element's attribute named "package".
        scanner.findWithinHorizon( "  A: package=\"", 0 );
        // Extracts the package value including the trailing double quote.
        String packageName = scanner.next( ".*?\"" );
        // Removes the double quote.
        packageName = packageName.substring( 0, packageName.length() - 1 );
        return packageName;
    }

    /**
     *
     * @param androidManifestFile
     * @return
     * @throws MojoExecutionException
     */
    protected String extractPackageNameFromAndroidManifest( File androidManifestFile ) throws MojoExecutionException
    {
        final URL xmlURL;
        try
        {
            xmlURL = androidManifestFile.toURI().toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException(
                    "Error while trying to figure out package name from inside AndroidManifest.xml file "
                            + androidManifestFile, e );
        }
        final DocumentContainer documentContainer = new DocumentContainer( xmlURL );
        final Object packageName = JXPathContext.newContext( documentContainer )
                .getValue( "manifest/@package", String.class );
        getLog().debug( "Extracting package " + packageName + " from Manifest : "  + androidManifestFile );
        return ( String ) packageName;
    }

    /**
     * Attempts to find the instrumentation test runner from inside the AndroidManifest.xml file.
     *
     * @param androidManifestFile the AndroidManifest.xml file to inspect.
     * @return the instrumentation test runner declared in AndroidManifest.xml, or {@code null} if it is not declared.
     * @throws MojoExecutionException
     */
    protected String extractInstrumentationRunnerFromAndroidManifest( File androidManifestFile )
            throws MojoExecutionException
    {
        final URL xmlURL;
        try
        {
            xmlURL = androidManifestFile.toURI().toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException(
                    "Error while trying to figure out instrumentation runner from inside AndroidManifest.xml file "
                            + androidManifestFile, e );
        }
        final DocumentContainer documentContainer = new DocumentContainer( xmlURL );
        final Object instrumentationRunner;
        try
        {
            instrumentationRunner = JXPathContext.newContext( documentContainer )
                    .getValue( "manifest//instrumentation/@android:name", String.class );
        }
        catch ( JXPathNotFoundException e )
        {
            return null;
        }
        return ( String ) instrumentationRunner;
    }

    /**
     * TODO .. not used. Delete?
     *
     * @param baseDirectory
     * @param includes
     * @return
     * @throws MojoExecutionException
     */
    protected int deleteFilesFromDirectory( File baseDirectory, String... includes ) throws MojoExecutionException
    {
        final String[] files = findFilesInDirectory( baseDirectory, includes );
        if ( files == null )
        {
            return 0;
        }

        for ( String file : files )
        {
            final boolean successfullyDeleted = new File( baseDirectory, file ).delete();
            if ( ! successfullyDeleted )
            {
                throw new MojoExecutionException( "Failed to delete \"" + file + "\"" );
            }
        }
        return files.length;
    }

    /**
     * Finds files.
     *
     * @param baseDirectory Directory to find files in.
     * @param includes      Ant-style include statements, for example <code>"** /*.aidl"</code> (but without the space
     *                      in the middle)
     * @return <code>String[]</code> of the files' paths and names, relative to <code>baseDirectory</code>. Empty
     *         <code>String[]</code> if <code>baseDirectory</code> does not exist.
     */
    protected String[] findFilesInDirectory( File baseDirectory, String... includes )
    {
        if ( baseDirectory.exists() )
        {
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir( baseDirectory );

            directoryScanner.setIncludes( includes );
            directoryScanner.addDefaultExcludes();

            directoryScanner.scan();
            String[] files = directoryScanner.getIncludedFiles();
            return files;
        }
        else
        {
            return new String[ 0 ];
        }

    }


    /**
     * <p>Returns the Android SDK to use.</p>
     * <p/>
     * <p>Current implementation looks for System property <code>android.sdk.path</code>, then
     * <code>&lt;sdk&gt;&lt;path&gt;</code> configuration in pom, then environment variable <code>ANDROID_HOME</code>.
     * <p/>
     * <p>This is where we collect all logic for how to lookup where it is, and which one to choose. The lookup is
     * based on available parameters. This method should be the only one you should need to look at to understand how
     * the Android SDK is chosen, and from where on disk.</p>
     *
     * @return the Android SDK to use.
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if no Android SDK path configuration is available at all.
     */
    protected AndroidSdk getAndroidSdk() throws MojoExecutionException
    {
        File chosenSdkPath;
        String chosenSdkPlatform;

        if ( sdk != null )
        {
            // An <sdk> tag exists in the pom.

            if ( sdk.getPath() != null )
            {
                // An <sdk><path> tag is set in the pom.

                chosenSdkPath = sdk.getPath();
            }
            else
            {
                // There is no <sdk><path> tag in the pom.

                if ( sdkPath != null )
                {
                    // -Dandroid.sdk.path is set on command line, or via <properties><android.sdk.path>...
                    chosenSdkPath = sdkPath;
                }
                else
                {
                    // No -Dandroid.sdk.path is set on command line, or via <properties><android.sdk.path>...
                    chosenSdkPath = new File( getAndroidHomeOrThrow() );
                }
            }

            // Use <sdk><platform> from pom if it's there, otherwise try -Dandroid.sdk.platform from command line or
            // <properties><sdk.platform>...
            if ( ! isBlank( sdk.getPlatform() ) )
            {
                chosenSdkPlatform = sdk.getPlatform();
            }
            else
            {
                chosenSdkPlatform = sdkPlatform;
            }
        }
        else
        {
            // There is no <sdk> tag in the pom.

            if ( sdkPath != null )
            {
                // -Dandroid.sdk.path is set on command line, or via <properties><android.sdk.path>...
                chosenSdkPath = sdkPath;
            }
            else
            {
                // No -Dandroid.sdk.path is set on command line, or via <properties><android.sdk.path>...
                chosenSdkPath = new File( getAndroidHomeOrThrow() );
            }

            // Use any -Dandroid.sdk.platform from command line or <properties><sdk.platform>...
            chosenSdkPlatform = sdkPlatform;
        }

        return new AndroidSdk( chosenSdkPath, chosenSdkPlatform );
    }

    /**
     *
     * @return
     * @throws MojoExecutionException
     */
    private String getAndroidHomeOrThrow() throws MojoExecutionException
    {
        final String androidHome = System.getenv( ENV_ANDROID_HOME );
        if ( isBlank( androidHome ) )
        {
            throw new MojoExecutionException( "No Android SDK path could be found. You may configure it in the "
                    + "plugin configuration section in the pom file using <sdk><path>...</path></sdk> or "
                    + "<properties><android.sdk.path>...</android.sdk.path></properties> or on command-line "
                    + "using -Dandroid.sdk.path=... or by setting environment variable " + ENV_ANDROID_HOME );
        }
        return androidHome;
    }

    protected final File getUnpackedLibsDirectory()
    {
        return getBuildHelper().getUnpackedLibsFolder();
    }

    public final File getUnpackedLibFolder( Artifact artifact )
    {
        return getBuildHelper().getUnpackedLibFolder( artifact );
    }

    protected final File getUnpackedAarClassesJar( Artifact artifact )
    {
        return getBuildHelper().getUnpackedClassesJar( artifact );
    }

    protected final File getUnpackedAarJavaResourcesFolder( Artifact artifact )
    {
        return new File( getUnpackedLibFolder( artifact ), "java-resources" );
    }

    protected final File getUnpackedApkLibSourceFolder( Artifact artifact )
    {
        return getBuildHelper().getUnpackedApkLibSourceFolder( artifact );
    }

    protected final File getUnpackedLibResourceFolder( Artifact artifact )
    {
        return getBuildHelper().getUnpackedLibResourceFolder( artifact );
    }

    protected final File getUnpackedLibAssetsFolder( Artifact artifact )
    {
        return getBuildHelper().getUnpackedLibAssetsFolder( artifact );
    }

    /**
     * @param artifact  Android dependency that is being referenced.
     * @return Folder where the unpacked native libraries are located.
     */
    public final File getUnpackedLibNativesFolder( Artifact artifact )
    {
        return getBuildHelper().getUnpackedLibNativesFolder( artifact );
    }

    // TODO Replace this with a non-static method (could even replace it with one of the methods above).
    public static File getLibraryUnpackDirectory( File unpackedApkLibsDirectory, Artifact artifact )
    {
        return new File( unpackedApkLibsDirectory.getAbsolutePath(), artifact.getArtifactId() );
    }

    /**
     * <p>Returns the Android NDK to use.</p>
     * <p/>
     * <p>Current implementation looks for <code>&lt;ndk&gt;&lt;path&gt;</code> configuration in pom, then System
     * property <code>android.ndk.path</code>, then environment variable <code>ANDROID_NDK_HOME</code>.
     * <p/>
     * <p>This is where we collect all logic for how to lookup where it is, and which one to choose. The lookup is
     * based on available parameters. This method should be the only one you should need to look at to understand how
     * the Android NDK is chosen, and from where on disk.</p>
     *
     * @return the Android NDK to use.
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if no Android NDK path configuration is available at all.
     */
    protected AndroidNdk getAndroidNdk() throws MojoExecutionException
    {
        File chosenNdkPath = null;
        // There is no <ndk> tag in the pom.
        if ( ndkPath != null )
        {
            // -Dandroid.ndk.path is set on command line, or via <properties><ndk.path>...
            chosenNdkPath = ndkPath;
        }
        else if ( ndk != null && ndk.getPath() != null )
        {
            chosenNdkPath = ndk.getPath();
        }
        else
        {
            // No -Dandroid.ndk.path is set on command line, or via <properties><ndk.path>...
            chosenNdkPath = new File( getAndroidNdkHomeOrThrow() );
        }
        return new AndroidNdk( chosenNdkPath );
    }


    /**
     *
     * @return
     * @throws MojoExecutionException
     */
    private String getAndroidNdkHomeOrThrow() throws MojoExecutionException
    {
        final String androidHome = System.getenv( ENV_ANDROID_NDK_HOME );
        if ( isBlank( androidHome ) )
        {
            throw new MojoExecutionException( "No Android NDK path could be found. You may configure it in the pom "
                    + "using <ndk><path>...</path></ndk> or <properties><ndk.path>...</ndk.path></properties> or on "
                    + "command-line using -Dandroid.ndk.path=... or by setting environment variable "
                    + ENV_ANDROID_NDK_HOME );
        }
        return androidHome;
    }

    /**
     * Get the resource directories if defined or the resource directory
     * @return
     */
    public File[] getResourceOverlayDirectories()
    {
        File[] overlayDirectories;

        if ( resourceOverlayDirectories == null || resourceOverlayDirectories.length == 0 )
        {
            overlayDirectories = new File[]{ resourceOverlayDirectory };
        }
        else
        {
            overlayDirectories = resourceOverlayDirectories;
        }

        return overlayDirectories;
    }

    private Set<String> getDevices()
    {
        Set<String> list = new HashSet<String>();

        if ( StringUtils.isNotBlank( device ) )
        {
            list.add( device );
        }

        list.addAll( Arrays.asList( devices ) );

        return list;
    }
    
    private int getDeviceThreads()
    {
        return deviceThreads;
    }

    private abstract class DoThread extends Thread
    {
        private MojoFailureException failure;
        private MojoExecutionException execution;

        public final void run()
        {
            try
            {
                runDo();
            }
            catch ( MojoFailureException e )
            {
                failure = e;
            }
            catch ( MojoExecutionException e )
            {
                execution = e;
            }
        }

        protected abstract void runDo() throws MojoFailureException, MojoExecutionException;
    }

    /**
     * @return True if this project constructs an APK as opposed to an AAR or APKLIB.
     */
    protected final boolean isAPKBuild()
    {
        return getBuildHelper().isAPKBuild( project );
    }

    /**
     * Copies the files contained within the source folder to the target folder.
     * <p>
     * The the target folder doesn't exist it will be created.
     * </p>
     *
     * @param sourceFolder      Folder from which to copy the resources.
     * @param targetFolder      Folder to which to copy the files.
     * @throws MojoExecutionException if the files cannot be copied.
     */
    protected final void copyFolder( File sourceFolder, File targetFolder ) throws MojoExecutionException
    {
        copyFolder( sourceFolder, targetFolder, TrueFileFilter.TRUE );
    }

    private void copyFolder( File sourceFolder, File targetFolder, FileFilter filter ) throws MojoExecutionException
    {
        if ( !sourceFolder.exists() )
        {
            return;
        }

        try
        {
            getLog().debug( "Copying " + sourceFolder + " to " + targetFolder );
            if ( ! targetFolder.exists() )
            {
                if ( ! targetFolder.mkdirs() )
                {
                    throw new MojoExecutionException( "Could not create target directory " + targetFolder );
                }
            }
            FileUtils.copyDirectory( sourceFolder, targetFolder, filter );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not copy source folder to target folder", e );
        }

    }

    protected final BuildHelper getBuildHelper()
    {
        if ( buildHelper == null )
        {
            buildHelper = new BuildHelper(
                repoSystem, repoSession,
                project,
                new MavenToPlexusLogAdapter( getLog() )
            );
        }
        return buildHelper;
    }
}
