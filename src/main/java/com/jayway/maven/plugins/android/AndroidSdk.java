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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;

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
     * property name for platform version in sdk source.properties file.
     */
    private static final String PLATFORM_VERSION_PROPERTY = "Platform.Version";
    /**
     * property name for api level version in sdk source.properties file.
     */
    private static final String API_LEVEL_PROPERTY = "AndroidVersion.ApiLevel";
    /**
     * property name for the sdk tools revision in sdk/tools/lib source.properties
     */
    private static final String SDK_TOOLS_REVISION_PROPERTY = "Pkg.Revision";

    /**
     * folder name for the sdk sub folder that contains the different platform versions.
     */
    private static final String PLATFORMS_FOLDER_NAME = "platforms";

    /**
     * folder name for the sdk sub folder that contains the platform tools.
     */
    private static final String PLATFORM_TOOLS_FOLDER_NAME = "platform-tools";

    /**
     * folder name for the sdk sub folder that contains the build tools.
     */
    private static final String BUILD_TOOLS_FOLDER_NAME = "build-tools";

    private static final String PARAMETER_MESSAGE = "Please provide a proper Android SDK directory path as "
            + "configuration parameter <sdk><path>...</path></sdk> in the plugin <configuration/>. As an alternative,"
            + " you may add the parameter to commandline: -Dandroid.sdk.path=... or set environment variable "
            + AbstractAndroidMojo.ENV_ANDROID_HOME + ".";

    private static final class Platform
    {
        final String name;
        final String apiLevel;
        final String path;

        public Platform( String name, String apiLevel, String path )
        {
            super();
            this.name = name;
            this.apiLevel = apiLevel;
            this.path = path;
        }
    }

    private final File sdkPath;
    private final Platform platform;
    private int sdkMajorVersion;

    private Set< Platform > availablePlatforms;

    public AndroidSdk( File sdkPath, String platformOrApiLevel )
    {
        this.sdkPath = sdkPath;
        findAvailablePlatforms();
        loadSDKToolsMajorVersion();

        if ( platformOrApiLevel == null )
        {
            platform = null;
            // letting this through to preserve compatibility for now
        }
        else
        {
            platform = findPlatformByNameOrApiLevel( platformOrApiLevel );
            if ( platform == null )
            {
                throw new InvalidSdkException( "Invalid SDK: Platform/API level " + platformOrApiLevel
                        + " not available. This command should give you all you need:\n" + sdkPath.getAbsolutePath()
                        + File.separator + "tools" + File.separator + "android update sdk --no-ui --obsolete --force" );
            }
        }
    }

    private Platform findPlatformByNameOrApiLevel( String platformOrApiLevel )
    {
        for ( Platform p : availablePlatforms )
        {
            if ( p.name.equals( platformOrApiLevel ) || p.apiLevel.equals( platformOrApiLevel ) )
            {
                return p;
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
        LAYOUT_1_5, LAYOUT_2_3
    }

    public Layout getLayout()
    {

        assertPathIsDirectory( sdkPath );

        final File platformTools = new File( sdkPath, PLATFORM_TOOLS_FOLDER_NAME );
        if ( platformTools.exists() && platformTools.isDirectory() )
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
     * Returns the complete path for a tool, based on this SDK.
     * 
     * @param tool
     *            which tool, for example <code>adb</code> or <code>dx.jar</code>.
     * @return the complete path as a <code>String</code>, including the tool's filename.
     */
    public String getPathForTool( String tool )
    {

        String[] possiblePaths =
        { sdkPath + "/" + PLATFORM_TOOLS_FOLDER_NAME + "/" + tool,
                sdkPath + "/" + PLATFORM_TOOLS_FOLDER_NAME + "/" + tool + ".exe",
                sdkPath + "/" + PLATFORM_TOOLS_FOLDER_NAME + "/" + tool + ".bat",
                sdkPath + "/" + PLATFORM_TOOLS_FOLDER_NAME + "/lib/" + tool, getPlatform() + "/tools/" + tool,
                getPlatform() + "/tools/" + tool + ".exe", getPlatform() + "/tools/" + tool + ".bat",
                getPlatform() + "/tools/lib/" + tool, sdkPath + "/tools/" + tool, sdkPath + "/tools/" + tool + ".exe",
                sdkPath + "/tools/" + tool + ".bat", sdkPath + "/tools/lib/" + tool,
                sdkPath + "/" + BUILD_TOOLS_FOLDER_NAME + "/17.0.0/" + tool, 
                sdkPath + "/" + BUILD_TOOLS_FOLDER_NAME + "/17.0.0/" + tool + ".bat",
                sdkPath + "/" + BUILD_TOOLS_FOLDER_NAME + "/17.0.0/" + tool + ".exe",
                sdkPath + "/" + BUILD_TOOLS_FOLDER_NAME + "/17.0.0/lib/" + tool };

        for ( String possiblePath : possiblePaths )
        {
            File file = new File( possiblePath );
            if ( file.exists() && !file.isDirectory() )
            {
                return file.getAbsolutePath();
            }
        }

        throw new InvalidSdkException( "Could not find tool '" + tool + "'. " + PARAMETER_MESSAGE );
    }

    /**
     * Get the android debug tool path (adb).
     * 
     * @return
     */
    public String getAdbPath()
    {
        return getPathForTool( "adb" );
    }

    /**
     * Get the android zipalign path.
     * 
     * @return
     */
    public String getZipalignPath()
    {
        return getPathForTool( "zipalign" );
    }

    /**
     * Get the android lint path.
     * 
     * @return
     */
    public String getLintPath()
    {
        return getPathForTool( "lint" );
    }

    /**
     * Get the android monkey runner path.
     * 
     * @return
     */
    public String getMonkeyRunnerPath()
    {
        return getPathForTool( "monkeyrunner" );
    }

    /**
     * Returns the complete path for <code>framework.aidl</code>, based on this SDK.
     * 
     * @return the complete path as a <code>String</code>, including the filename.
     */
    public String getPathForFrameworkAidl()
    {
        final Layout layout = getLayout();
        switch ( layout )
        {
            case LAYOUT_1_5: // intentional fall-through
            case LAYOUT_2_3:
                return getPlatform() + "/framework.aidl";
            default:
                throw new InvalidSdkException( "Unsupported layout \"" + layout + "\"! " + PARAMETER_MESSAGE );
        }
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
        final Layout layout = getLayout();
        switch ( layout )
        {
            case LAYOUT_1_5: // intentional fall-through
            case LAYOUT_2_3:
                return new File( getPlatform() + "/android.jar" );
            default:
                throw new MojoExecutionException( "Invalid Layout \"" + getLayout() + "\"! " + PARAMETER_MESSAGE );
        }
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
     * "latest" version. This is actually broken as it performs a lexicographic sort rather than sorting the versions in
     * proper order.
     */
    public File getPlatform()
    {
        assertPathIsDirectory( sdkPath );

        final File platformsDirectory = new File( sdkPath, PLATFORMS_FOLDER_NAME );
        assertPathIsDirectory( platformsDirectory );

        final File platformDirectory;
        if ( platform == null )
        {
            final File[] platformDirectories = platformsDirectory.listFiles();
            Arrays.sort( platformDirectories );
            platformDirectory = platformDirectories[ platformDirectories.length - 1 ];
        }
        else
        {
            platformDirectory = new File( platform.path );
        }
        assertPathIsDirectory( platformDirectory );
        return platformDirectory;
    }

    /**
     * Initialize the maps matching platform and api levels from the source properties files.
     * 
     * @throws InvalidSdkException
     */
    private void findAvailablePlatforms()
    {
        availablePlatforms = new HashSet< Platform >();

        ArrayList< File > platformDirectories = getPlatformDirectories();
        for ( File pDir : platformDirectories )
        {
            File propFile = new File( pDir, SOURCE_PROPERTIES_FILENAME );
            Properties properties = new Properties();
            try
            {
                properties.load( new FileInputStream( propFile ) );
            }
            catch ( IOException e )
            {
                throw new InvalidSdkException( "Error reading " + propFile.getAbsoluteFile() );
            }
            if ( properties.containsKey( PLATFORM_VERSION_PROPERTY ) && properties.containsKey( API_LEVEL_PROPERTY ) )
            {
                String platform = properties.getProperty( PLATFORM_VERSION_PROPERTY );
                String apiLevel = properties.getProperty( API_LEVEL_PROPERTY );
                availablePlatforms.add( new Platform( platform, apiLevel, pDir.getAbsolutePath() ) );
            }
        }
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
     * Gets the source properties files from all locally installed platforms.
     * 
     * @return
     */
    private ArrayList< File > getPlatformDirectories()
    {
        ArrayList< File > sourcePropertyFiles = new ArrayList< File >();
        final File platformsDirectory = new File( sdkPath, PLATFORMS_FOLDER_NAME );
        assertPathIsDirectory( platformsDirectory );
        final File[] platformDirectories = platformsDirectory.listFiles();
        for ( File file : platformDirectories )
        {
            // only looking in android- folder so only works on reasonably new sdk revisions..
            if ( file.isDirectory() && file.getName().startsWith( "android-" ) )
            {
                sourcePropertyFiles.add( file );
            }
        }
        return sourcePropertyFiles;
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
