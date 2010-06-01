/*
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

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Represents an Android SDK.
 *
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class AndroidSdk {
    private final File   path;
    private final String platform;
    private static final String PARAMETER_MESSAGE = "Please provide a proper Android SDK directory path as configuration parameter <sdk><path>...</path></sdk> in the plugin <configuration/>. As an alternative, you may add the parameter to commandline: -Dandroid.sdk.path=... or set environment variable " + AbstractAndroidMojo.ENV_ANDROID_HOME + ".";


    /**
     * Maps from Platform to API Level and reverse. E.g. 2.2. to 8 and 8 to 2.2.
     * should really use multimap from google collections but for this one use case not worth including..
     */
    public static HashMap<String, String> installedPlatforms2ApiLevels;
    public static HashMap<String, String> installedApiLevels2Platforms;
    /** property file in each platform folder with details about platform. */
    private static final String SOURCE_PROPERTIES_FILENAME = "source.properties";
    /** property name for platform version in sdk source.properties file. */
    private static final String PLATFORM_VERSION_PROPERTY = "Platform.Version";
    /** property name for api level version in sdk source.properties file. */
    private static final String API_LEVEL_PROPERTY = "AndroidVersion.ApiLevel";

    /** folder name for the sdk sub folder that contains the different platform versions. */
    private static final String PLATFORMS_FOLDER_NAME = "platforms";

    public AndroidSdk(File path, String platformOrApiLevel) {
        this.path = path;
        initPlatformAndApiLevels();

        if (platformOrApiLevel == null) {
            this.platform = platformOrApiLevel;
            // letting this through to preserve compatibility for now
        } else if (!(installedApiLevels2Platforms.containsKey(platformOrApiLevel)
                || installedPlatforms2ApiLevels.containsKey(platformOrApiLevel))) {
            throw new InvalidSdkException("Invalid SDK: Platform/API level " + platformOrApiLevel + " not available.") ;
        } else if (installedApiLevels2Platforms.containsKey(platformOrApiLevel)) {
            this.platform = platformOrApiLevel;
        } else if (installedPlatforms2ApiLevels.containsKey(platformOrApiLevel)) {
            this.platform = installedPlatforms2ApiLevels.get(platformOrApiLevel);
        } else {
            throw new InvalidSdkException("Invalid SDK: Platform/API level " + platformOrApiLevel + " not available.");
        }
    }

    public enum Layout{LAYOUT_1_1, LAYOUT_1_5};

    public Layout getLayout() {

        assertPathIsDirectory(path);

        final File platforms = new File(path, PLATFORMS_FOLDER_NAME);
        if (platforms.exists() && platforms.isDirectory()){
            return Layout.LAYOUT_1_5;
        }

        final File androidJar = new File(path, "android.jar");
        if (androidJar.exists() && androidJar.isFile()) {
            return Layout.LAYOUT_1_1;
        }

        throw new InvalidSdkException("Android SDK could not be identified from path \"" + path +"\". " + PARAMETER_MESSAGE);
    }

    private void assertPathIsDirectory(final File path) {
        if (path == null) {
            throw new InvalidSdkException(PARAMETER_MESSAGE);
        }
        if (!path.isDirectory()) {
            throw new InvalidSdkException("Path \"" + path + "\" is not a directory. " + PARAMETER_MESSAGE);
        }
    }

    private static final Set<String> commonToolsIn11And15 = new HashSet<String>() {
        {
            add("adb"            );
            add("android"        );
            add("apkbuilder"     );
            add("ddms"           );
            add("dmtracedump"    );
            add("draw9patch"     );
            add("emulator"       );
            add("hierarchyviewer");
            add("hprof-conv"     );
            add("mksdcard"       );
            add("sqlite3"        );
            add("traceview"      );
            add("zipalign"      );
        }
    };

    /**
     * Returns the complete path for a tool, based on this SDK.
     * TODO: Implementation should try to find the tool in the different directories, instead of relying on a manually maintained list of where they are. (i.e. remove commonToolsIn11And15, and make lookup automatic based on which tools can actually be found where.)
     * @param tool which tool, for example <code>adb</code>.
     * @return the complete path as a <code>String</code>, including the tool's filename.
     */
    public String getPathForTool(String tool) {
        if (getLayout() == Layout.LAYOUT_1_1) {
            return path + "/tools/" + tool;
        }

        if (getLayout() == Layout.LAYOUT_1_5) {
            if (commonToolsIn11And15.contains(tool)) {
                return path + "/tools/" + tool;
            }else {

                return getPlatform() + "/tools/" + tool;
            }
        }

        throw new InvalidSdkException("Unsupported layout \"" + getLayout() + "\"! " + PARAMETER_MESSAGE);
    }

    /**
     * Get the emulator path.
     * @return
     */
    public String getEmulatorPath()
    {
        return getPathForTool("emulator");
    }

    /**
     * Get the android debug tool path (adb).
     * @return
     */
    public String getAdbPath()
    {
        return getPathForTool("adb");
    }

    /**
     * Get the android debug tool path (adb).
     * @return
     */
    public String getZipalignPath()
    {
        return getPathForTool("zipalign");
    }

    /**
     * Returns the complete path for <code>framework.aidl</code>, based on this SDK.
     * @return the complete path as a <code>String</code>, including the filename.
     */
    public String getPathForFrameworkAidl() {
        if (getLayout() == Layout.LAYOUT_1_1) {
            return path + "/tools/lib/framework.aidl";
        }

        if (getLayout() == Layout.LAYOUT_1_5) {
            return getPlatform() + "/framework.aidl";
        }

        throw new InvalidSdkException("Unsupported layout \"" + getLayout() + "\"! " + PARAMETER_MESSAGE);
    }

    /**
     * Resolves the android.jar from this SDK.
     *
     * @return a <code>File</code> pointing to the android.jar file.
     * @throws org.apache.maven.plugin.MojoExecutionException if the file can not be resolved.
     */
    public File getAndroidJar() throws MojoExecutionException {
        if (getLayout() == Layout.LAYOUT_1_1) {
            return new File(path + "/android.jar");
        }

        if (getLayout() == Layout.LAYOUT_1_5) {
            return new File(getPlatform() + "/android.jar");
        }

        throw new MojoExecutionException("Invalid Layout \"" + getLayout() + "\"! " + PARAMETER_MESSAGE);
    }

    public File getPlatform() {
        assertPathIsDirectory(path);

        if (getLayout() == Layout.LAYOUT_1_1){
            return path;
        }

        final File platformsDirectory = new File(path, PLATFORMS_FOLDER_NAME);
        assertPathIsDirectory(platformsDirectory);

        if (platform == null){
            final File[] platformDirectories = platformsDirectory.listFiles();
            Arrays.sort(platformDirectories);
            return platformDirectories[platformDirectories.length-1];
        }else{
            final File platformDirectory = new File(platformsDirectory, "android-" + platform);
            assertPathIsDirectory(platformDirectory);
            return platformDirectory;
        }
    }

    /**
     * Initialize the maps matching platform and api levels form the source properties files.
     * @throws InvalidSdkException
     */
    private void initPlatformAndApiLevels() throws InvalidSdkException{
        installedApiLevels2Platforms = new HashMap<String, String>();
        installedPlatforms2ApiLevels = new HashMap<String, String>();
        
        ArrayList<File> sourcePropertyFiles = getSourcePropertyFiles();
        for (File file : sourcePropertyFiles) {
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(file));
            } catch (IOException e) {
                throw new InvalidSdkException("Error reading " + file.getAbsoluteFile());
            }
            if (properties.containsKey(PLATFORM_VERSION_PROPERTY) && properties.containsKey(API_LEVEL_PROPERTY)) {
                String platform = properties.getProperty(PLATFORM_VERSION_PROPERTY);
                String apiLevel = properties.getProperty(API_LEVEL_PROPERTY);
                installedApiLevels2Platforms.put(apiLevel, platform);
                installedPlatforms2ApiLevels.put(platform, apiLevel);
            }
        }
    }

    /**
     * Gets the source properties files from all locally installed platforms.
     * @return
     */
    private ArrayList<File> getSourcePropertyFiles() {
        ArrayList<File> sourcePropertyFiles = new ArrayList<File>();
        final File platformsDirectory = new File(path, PLATFORMS_FOLDER_NAME);
        assertPathIsDirectory(platformsDirectory);
        final File[] platformDirectories = platformsDirectory.listFiles();
        for (File file: platformDirectories)
        {
            // only looking in android- folder so only works on reasonably new sdk revisions..
            if (file.isDirectory() && file.getName().startsWith("android-")) sourcePropertyFiles.add(new File(file, SOURCE_PROPERTIES_FILENAME));
        }
        return sourcePropertyFiles;
    }

}