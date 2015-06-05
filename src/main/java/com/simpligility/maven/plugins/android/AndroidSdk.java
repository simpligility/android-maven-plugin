/*
 * Copyright (C) 2009, 2010 Jayway AB
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
package com.simpligility.maven.plugins.android;

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkManager;
import com.android.sdklib.repository.FullRevision;
import com.android.utils.NullLogger;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.codehaus.plexus.interpolation.os.Os;

/**
 * Represents an Android SDK.
 * 
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class AndroidSdk
{
    /**
     * the default API level for the SDK used as a fall back if none is supplied, 
     * should ideally point to the latest available version
     */
    private static final String DEFAULT_ANDROID_API_LEVEL = "22";
    /**
     * property file in each platform folder with details about platform.
     */
    private static final String SOURCE_PROPERTIES_FILENAME = "source.properties";
    /**
     * property name for the sdk tools revision in sdk/tools/lib source.properties
     */
    private static final String SDK_TOOLS_REVISION_PROPERTY = "Pkg.Revision";

    /**
     * folder name for the sdk sub folder that contains the different platform versions.
     */
    private static final String PLATFORMS_FOLDER_NAME = "platforms";

    private static final String PARAMETER_MESSAGE = "Please provide a proper Android SDK directory path as "
            + "configuration parameter <sdk><path>...</path></sdk> in the plugin <configuration/>. As an alternative,"
            + " you may add the parameter to commandline: -Dandroid.sdk.path=... or set environment variable "
            + AbstractAndroidMojo.ENV_ANDROID_HOME + ".";

    private final File sdkPath;
    private File platformToolsPath;
    private File toolsPath;

    private final IAndroidTarget androidTarget;
    private SdkManager sdkManager;
    private int sdkMajorVersion;
    private String buildToolsVersion;

    public AndroidSdk( File sdkPath, String apiLevel )
    {
        this( sdkPath, apiLevel, null );
    }

    public AndroidSdk( File sdkPath, String apiLevel, @Nullable String buildToolsVersion )
    {
        this.sdkPath = sdkPath;
        this.buildToolsVersion = buildToolsVersion;

        if ( sdkPath != null )
        {
            sdkManager = SdkManager.createManager( sdkPath.getPath(), new NullLogger() );
            platformToolsPath = new File( sdkPath, SdkConstants.FD_PLATFORM_TOOLS );
            toolsPath = new File( sdkPath, SdkConstants.FD_TOOLS );

            if ( sdkManager == null )
            {
                throw invalidSdkException( sdkPath, apiLevel );
            }
        }
        loadSDKToolsMajorVersion();

        if ( apiLevel == null )
        {
            apiLevel = DEFAULT_ANDROID_API_LEVEL;
        }

        androidTarget = findPlatformByApiLevel( apiLevel );
        if ( androidTarget == null )
        {
            throw invalidSdkException( sdkPath, apiLevel );
        }
    }

    private InvalidSdkException invalidSdkException( File sdkPath, String platformOrApiLevel )
    {
        throw new InvalidSdkException( "Invalid SDK: Platform/API level " + platformOrApiLevel
                + " not available. This command should give you all you need:\n" + sdkPath.getAbsolutePath()
                + File.separator + "tools" + File.separator + "android update sdk --no-ui --obsolete --force" );
    }

    private IAndroidTarget findPlatformByApiLevel( String apiLevel )
    {
        // try find by api level first
        AndroidVersion version = null;
        try
        {
            version = new AndroidVersion( apiLevel );
            String hashString = AndroidTargetHash.getPlatformHashString( version );
            IAndroidTarget target = sdkManager.getTargetFromHashString( hashString );

            if ( target != null )
            {
                return target;
            }
        }
        catch ( AndroidVersion.AndroidVersionException ignore )
        {
            throw new InvalidSdkException( "Error AndroidVersion: " + ignore.getMessage() );
        }

        // fallback to searching for platform on standard Android platforms (isPlatform() is true)
        for ( IAndroidTarget t: sdkManager.getTargets() )
        {
            if ( t.isPlatform() && t.getVersionName().equals( apiLevel ) )
            {
                return t;
            }
        }
        return null;
    }

    private void assertPathIsDirectory( final File path )
    {
        if ( path == null )
        {
            throw new InvalidSdkException( PARAMETER_MESSAGE );
        }
        if ( !path.isDirectory() )
        {
            throw new InvalidSdkException( "Path \"" + path + "\" is not a directory. " + PARAMETER_MESSAGE );
        }
    }

    /**
     * Get the aapt tool path.
     *
     * @return
     */
    public String getAaptPath()
    {
        return getPathForBuildTool( BuildToolInfo.PathId.AAPT );
    }

    /**
     * Get the aild tool path
     * @return
     */
    public String getAidlPath()
    {
        return getPathForBuildTool( BuildToolInfo.PathId.AIDL );
    }

    /**
     * Get the path for dx.jar
     * @return
     */
    public String getDxJarPath()
    {
        return getPathForBuildTool( BuildToolInfo.PathId.DX_JAR );
    }

    /**
     * Get the android debug tool path (adb).
     *
     * @return
     */
    public String getAdbPath()
    {
        return getPathForPlatformTool( SdkConstants.FN_ADB );
    }

    /**
     * Get the android zipalign path.
     *
     * @return
     */
    public String getZipalignPath()
    {
        return getPathForBuildTool( BuildToolInfo.PathId.ZIP_ALIGN );
    }

    /**
     * Get the android lint path.
     * 
     * @return
     */
    public String getLintPath()
    {
        return getPathForTool( "lint" + ext( ".bat", "" ) );
    }

    /**
     * Get the android monkey runner path.
     * 
     * @return
     */
    public String getMonkeyRunnerPath()
    {
        return getPathForTool( "monkeyrunner" + ext( ".bat", "" ) );
    }

    /**
     * Get the apkbuilder path.
     *
     * @return
     */
    public String getApkBuilderPath()
    {
        return getPathForTool( "apkbuilder" + ext( ".bat", "" ) );
    }

    /**
     * Get the android tool path.
     *
     * @return
     */
    public String getAndroidPath()
    {
        return getPathForTool( SdkConstants.androidCmdName() );
    }

    /**
     * Get the path to the tools directory.
     * @return
     */
    public File getToolsPath()
    {
        return toolsPath;
    }

    private String getPathForBuildTool( BuildToolInfo.PathId pathId )
    {
        return getBuildToolInfo().getPath( pathId );
    }
    
    private BuildToolInfo getBuildToolInfo()
    {
        //First we use the build tools specified in the pom file
        if ( buildToolsVersion != null && !buildToolsVersion.equals( "" ) )
        {
            BuildToolInfo buildToolInfo = sdkManager.getBuildTool( FullRevision.parseRevision( buildToolsVersion ) );
            if ( buildToolInfo != null )
            {
                return buildToolInfo;
            }
            //Since we cannot find the build tool specified by the user we make it fail
            // instead of using the latest build tool version
            throw new InvalidSdkException( "Invalid SDK: Build-tools " + buildToolsVersion + " not found."
                    + " Check your Android SDK to install the build tools " + buildToolsVersion );
        }

        if ( androidTarget != null )
        {
            BuildToolInfo buildToolInfo = androidTarget.getBuildToolInfo();
            if ( buildToolInfo != null ) 
            {
                return buildToolInfo;
            }
        }
        // if no valid target is defined, or it has no build tools installed, try to use the latest
        BuildToolInfo latestBuildToolInfo = sdkManager.getLatestBuildTool();
        if ( latestBuildToolInfo == null )
        {
            throw new InvalidSdkException( "Invalid SDK: Build-tools not found. Check the content of '" 
                + sdkPath.getAbsolutePath() + File.separator + "build-tools', or run '" 
                + sdkPath.getAbsolutePath() + File.separator + "tools" + File.separator 
                + "android sdk' to install them" );
        }
        return latestBuildToolInfo;
    }

    private String getPathForPlatformTool( String tool )
    {
        return new File( platformToolsPath, tool ).getAbsolutePath();
    }

    private String getPathForTool( String tool )
    {
        return new File( toolsPath, tool ).getAbsolutePath();
    }

    private static String ext( String windowsExtension, String nonWindowsExtension )
    {
        if ( SdkConstants.currentPlatform() == SdkConstants.PLATFORM_WINDOWS )
        {
            return windowsExtension;
        }
        else
        {
            return nonWindowsExtension;
        }
    }

    /**
     * Returns the complete path for <code>framework.aidl</code>, based on this SDK.
     * 
     * @return the complete path as a <code>String</code>, including the filename.
     */
    public String getPathForFrameworkAidl()
    {
        return androidTarget.getPath( IAndroidTarget.ANDROID_AIDL );
    }

    /**
     * Returns the mainDexClasses script file, based on this SDK and OS.
     * Assumes that the script is in build-tools\VERSION\ directory.
     * 
     * <b>NOTE</b>: This file is found in version 21.1.1+ of build-tools.  
     * 
     * @return mainDexClasses file
     * @throws MojoExecutionException when the file is not found 
     */
    public File getMainDexClasses() throws MojoExecutionException 
    {
        final File location = getBuildToolInfo().getLocation();
        String mainDexClassesScript = "mainDexClasses";   
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) ) 
        {
            mainDexClassesScript += ".bat";   
        }               
        File mainDexClasses = new File( location, mainDexClassesScript );
        if ( !mainDexClasses.exists() ) 
        {
            throw new MojoExecutionException( "No " + mainDexClassesScript + " found in " + location );
        }
        return mainDexClasses;
    }

    /**
     * Resolves the android.jar from this SDK.
     * 
     * @return a <code>File</code> pointing to the android.jar file.
     * @throws org.apache.maven.plugin.MojoExecutionException
     *             if the file can not be resolved.
     */
    public File getAndroidJar() throws MojoExecutionException
    {
        final String androidJarPath = androidTarget.getPath( IAndroidTarget.ANDROID_JAR );
        if ( androidJarPath == null )
        {
            throw new MojoExecutionException( "No AndroidJar found for " + androidTarget );
        }
        return new File ( androidJarPath );
    }
  
    /**
     * Resolves the path for this SDK.
     * 
     * @return a <code>File</code> pointing to the SDk Directory.
     * @throws org.apache.maven.plugin.MojoExecutionException
     *             if the file can not be resolved.
     */
    public File getSdkPath() throws MojoExecutionException
    {
        if ( sdkPath.exists() )
        {
            return sdkPath;
        }
        throw new MojoExecutionException( "Can't find the SDK directory : " + sdkPath.getAbsolutePath() );
    }

    /**
     * This method returns the previously specified version. However, if none have been specified it returns the
     * "latest" version.
     */
    public File getPlatform()
    {
        assertPathIsDirectory( sdkPath );

        final File platformsDirectory = new File( sdkPath, PLATFORMS_FOLDER_NAME );
        assertPathIsDirectory( platformsDirectory );

        final File platformDirectory;
        if ( androidTarget == null )
        {
            IAndroidTarget latestTarget = null;
            for ( IAndroidTarget target:  sdkManager.getTargets() )
            {
                if ( target.isPlatform() )
                {
                    if ( latestTarget == null
                            || target.getVersion().getApiLevel() > latestTarget.getVersion().getApiLevel() )
                    {
                        latestTarget = target;
                    }
                }
            }
            platformDirectory = new File ( latestTarget.getLocation() );
        }
        else
        {
            platformDirectory = new File( androidTarget.getLocation() );
        }
        assertPathIsDirectory( platformDirectory );
        return platformDirectory;
    }

    /**
     * Loads the SDK Tools version
     */
    private void loadSDKToolsMajorVersion()
    {
        File propFile = new File( sdkPath, "tools/" + SOURCE_PROPERTIES_FILENAME );
        Properties properties = new Properties();
        try
        {
            properties.load( new FileInputStream( propFile ) );
        }
        catch ( IOException e )
        {
            throw new InvalidSdkException( "Error reading " + propFile.getAbsoluteFile() );
        }

        if ( properties.containsKey( SDK_TOOLS_REVISION_PROPERTY ) )
        {
            try
            {
                String versionString = properties.getProperty( SDK_TOOLS_REVISION_PROPERTY );
                String majorVersion;
                if ( versionString.matches( ".*[\\.| ].*" ) )
                {
                    String[] versions = versionString.split( "[\\.| ]" );
                    majorVersion = versions[ 0 ];
                }
                else
                {
                    majorVersion = versionString;
                }
                sdkMajorVersion = Integer.parseInt( majorVersion );
            }
            catch ( NumberFormatException e )
            {
                throw new InvalidSdkException( "Error - The property '" + SDK_TOOLS_REVISION_PROPERTY
                        + "' in the SDK source.properties file  number is not an Integer: "
                        + properties.getProperty( SDK_TOOLS_REVISION_PROPERTY ) );
            }
        }
    }

    /**
     * Returns the version of the SDK Tools.
     * 
     * @return
     */
    public int getSdkMajorVersion()
    {
        return sdkMajorVersion;
    }
}
