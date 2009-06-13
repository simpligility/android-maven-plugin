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
import java.util.Set;
import java.util.HashSet;

/**
 * Configuration for an Android SDK.
 *
 * TODO: do more thorough parameter checking
 *
 * @author hugo.josefson@jayway.com
 */
public class AndroidSdk {

    /**
     * Directory of the installed Android SDK, for example <code>/opt/android-sdk-linux_x86-1.5_r1</code>
     *
     * @parameter expression="${android.sdk.path}"
     *            default-value="${env.ANDROID_SDK}"
     *            property="path"
     */
    private File _path;

    /**
     * Directory layout of this SDK. Valid values are <code>1.1</code> and <code>1.5</code>.
     *
     * @parameter property="layout"
     */
    private String _layout;

    /**
     * Chosen platform version. Valid values are same as <code>layout</code>, and lower.
     *
     * @parameter property="platform"
     */
    private String _platform;

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
     * @throws MojoExecutionException if the combinations of this SDK's parameters and the tool do not match.
     */
    public String getPathForTool(String tool) throws MojoExecutionException {
        if ("1.1".equals(_layout)) {
            return _path + "/tools/" + tool;
        }

        if ("1.5".equals(_layout)) {
            if (commonToolsIn11And15.contains(tool)) {
                return _path + "/tools/" + tool;
            }else {
                if (_layout.compareTo(_platform) < 0) {
                    throw new MojoExecutionException("Platform version \""+ _platform +"\" must not be greater than layout \"" + _layout + "\"!");
                }
                return _path + "/platforms/android-" + _platform + "/tools/" + tool;
            }
        }

        throw new MojoExecutionException("Invalid Layout \"" + _layout + "\"!");
    }

    /**
     * Returns the complete path for <code>framework.aidl</code>, based on this SDK.
     * @return the complete path as a <code>String</code>, including the filename.
     * @throws MojoExecutionException if the combination of this SDK's parameters do not match.
     */
    public String getPathForFrameworkAidl() throws MojoExecutionException {
        if ("1.1".equals(_layout)) {
            return _path + "/tools/lib/framework.aidl";
        }

        if ("1.5".equals(_layout)) {
            if (_layout.compareTo(_platform) < 0) {
                throw new MojoExecutionException("Platform version \""+ _platform +"\" must not be greater than layout \"" + _layout + "\"!");
            }
            return _path + "/platforms/android-" + _platform + "/framework.aidl";
        }

        throw new MojoExecutionException("Invalid Layout \"" + _layout + "\"!");
    }

    /**
     * Resolves the android.jar from this SDK.
     *
     * @return a <code>File</code> pointing to the android.jar file.
     * @throws org.apache.maven.plugin.MojoExecutionException if the file can not be resolved.
     */
    public File getAndroidJar() throws MojoExecutionException {
        if ("1.1".equals(_layout)) {
            return new File(_path + "/android.jar");
        }

        if ("1.5".equals(_layout)) {
            if (_layout.compareTo(_platform) < 0) {
                throw new MojoExecutionException("Platform version \"" + _platform + "\" must not be greater than layout \"" + _layout + "\"!");
            }
            return new File(_path + "/platforms/android-" + _platform + "/android.jar");
        }

        throw new MojoExecutionException("Invalid Layout \"" + _layout + "\"!");
    }

    public File getPath() {
        return _path;
    }

    public void setPath(File path) {
        this._path = path;
    }

    public String getLayout() {
        return _layout;
    }

    public void setLayout(String layout) {
        this._layout = layout;
    }

    public String getPlatform() {
        return _platform;
    }


    public void setPlatform(String platform) {
        this._platform = platform;
    }
}
