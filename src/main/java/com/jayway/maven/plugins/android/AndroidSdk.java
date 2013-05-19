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
package com.jayway.maven.plugins.android;

import com.android.SdkConstants;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkManager;
import com.android.utils.NullLogger;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Represents an Android SDK.
 * 
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class AndroidSdk
{

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

    private IAndroidTarget androidTarget;
    private SdkManager sdkManager;
    private int sdkMajorVersion;

    public AndroidSdk( File sdkPath, String platformOrApiLevel )
    {
        this.sdkPath = sdkPath;

        System.out.println( "AndroidSdk " + sdkPath + "," + platformOrApiLevel );

        if ( sdkPath != null )
        {
            sdkManager = SdkManager.createManager( sdkPath.getPath(), new NullLogger() );
            platformToolsPath = new File( sdkPath, SdkConstants.FD_PLATFORM_TOOLS );
            toolsPath = new File( sdkPath, SdkConstants.FD_TOOLS );

            if ( sdkManager == null )
            {
                throw invalidSdkException( sdkPath, platformOrApiLevel );
            }
        }
        loadSDKToolsMajorVersion();

        if ( platformOrApiLevel == null )
        {
            androidTarget = null;
            // letting this through to preserve compatibility for now
        }
        else
        {
            androidTarget = findPlatformByNameOrApiLevel( platformOrApiLevel );
            if ( androidTarget == null )
            {
                throw invalidSdkException( sdkPath, platformOrApiLevel );
            }
        }
    }

    private InvalidSdkException invalidSdkException( File sdkPath, String platformOrApiLevel )
    {
        throw new InvalidSdkException( "Invalid SDK: Platform/API level " + platformOrApiLevel
                + " not available. This command should give you all you need:\n" + sdkPath.getAbsolutePath()
                + File.separator + "tools" + File.separator + "android update sdk --no-ui --obsolete --force" );
    }

    private IAndroidTarget findPlatformByNameOrApiLevel( String platformOrApiLevel )
    {
        // try find by api level first
        IAndroidTarget target = sdkManager.getTargetFromHashString(
                IAndroidTarget.PLATFORM_HASH_PREFIX + platformOrApiLevel );
        if ( target != null )
        {
            return target;
        }
        System.out.println( "Failed to find target by hash: "
                + IAndroidTarget.PLATFORM_HASH_PREFIX + platformOrApiLevel );


        // fallback to searching for platform on standard Android platforms (isPlatform() is true)
        for ( IAndroidTarget t: sdkManager.getTargets() )
        {
            System.out.println( "Checking " + t.getVersionName() + " against " + platformOrApiLevel );
            if ( t.isPlatform() && t.getVersionName().equals( platformOrApiLevel ) )
            {
                return t;
            }
        }
        return null;
    }



    /**
     * The file system layout of the SDK. Should probably be removed since the 15 layout is very old and probably wont
     * work completely. No urgency though..
     */
    public enum Layout
    {
        LAYOUT_1_5, LAYOUT_2_3;
    }
    public Layout getLayout()
    {

        assertPathIsDirectory( sdkPath );


        if ( platformToolsPath.exists() && platformToolsPath.isDirectory() )
        {
            return Layout.LAYOUT_2_3;
        }

        final File platforms = new File( sdkPath, PLATFORMS_FOLDER_NAME );
        if ( platforms.exists() && platforms.isDirectory() )
        {
            return Layout.LAYOUT_1_5;
        }

        throw new InvalidSdkException( "Android SDK could not be identified from path \"" + sdkPath + "\". "
                + PARAMETER_MESSAGE );
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
        return getPathForPlatformTool( SdkConstants.FN_ZIPALIGN );
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
        if ( androidTarget != null )
        {
            return androidTarget.getBuildToolInfo().getPath( pathId );
        }
        // if no valid target is defined used the latest
        return sdkManager.getLatestBuildTool().getPath( pathId );
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
     * Resolves the android.jar from this SDK.
     * 
     * @return a <code>File</code> pointing to the android.jar file.
     * @throws org.apache.maven.plugin.MojoExecutionException
     *             if the file can not be resolved.
     */
    public File getAndroidJar() throws MojoExecutionException
    {
        return new File( androidTarget.getPath( IAndroidTarget.ANDROID_JAR ) );
    }

    /**
     * Resolves the sdklib.jar from this SDK.
     * 
     * @return a <code>File</code> pointing to the sdklib.jar file.
     * @throws org.apache.maven.plugin.MojoExecutionException
     *             if the file can not be resolved.
     */
    public File getSDKLibJar() throws MojoExecutionException
    {
        // The file is sdkPath/tools/lib/sdklib.jar
        File sdklib = new File( sdkPath + "/tools/lib/sdklib.jar" );
        if ( sdklib.exists() )
        {
            return sdklib;
        }
        throw new MojoExecutionException( "Can't find the 'sdklib.jar' : " + sdklib.getAbsolutePath() );
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
