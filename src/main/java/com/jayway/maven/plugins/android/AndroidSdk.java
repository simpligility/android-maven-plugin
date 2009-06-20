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
import org.apache.commons.io.filefilter.NameFileFilter;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Represents an Android SDK.
 *
 * TODO: do more thorough parameter checking
 *
 * @author hugo.josefson@jayway.com
 */
public class AndroidSdk {
    private final File   path;
    private final String platform;

    public AndroidSdk(File path, String platform) {
        this.path = path;
        this.platform = platform;
    }

    public enum Layout{LAYOUT_1_1, LAYOUT_1_5}

    public Layout getLayout() {

        assertPathIsDirectory(path);

        final File platforms = new File(path, "platforms");
        if (platforms.exists() && platforms.isDirectory()){
            return Layout.LAYOUT_1_5;
        }

        final File androidJar = new File(path, "android.jar");
        if (androidJar.exists() && androidJar.isFile()) {
            return Layout.LAYOUT_1_1;
        }

        throw new InvalidSdkException("Android SDK could not be identified from path \"" + path +"\"");
    }

    private void assertPathIsDirectory(final File path) {
        if (path == null) throw new InvalidSdkException("Path is null!");
        if (!path.isDirectory()) throw new InvalidSdkException("Path \"" + path + "\" is not a directory.");
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
        }
    };

    /**
     * Returns the complete path for a tool, based on this SDK.
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

        throw new InvalidSdkException("Unsupported layout \"" + getLayout() + "\"!");
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

        throw new InvalidSdkException("Unsupported layout \"" + getLayout() + "\"!");
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

        throw new MojoExecutionException("Invalid Layout \"" + getLayout() + "\"!");
    }

    public File getPlatform() {
        assertPathIsDirectory(path);

        if (getLayout() == Layout.LAYOUT_1_1){
            return path;
        }

        final File platformsDirectory = new File(path, "platforms");
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
}