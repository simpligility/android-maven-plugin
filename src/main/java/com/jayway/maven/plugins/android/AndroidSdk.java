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
     */
    private File path;

    /**
     * Directory layout of this SDK. Valid values are <code>1.1</code> and <code>1.5</code>.
     *
     * @parameter
     */
    private String layout;

    /**
     * Chosen platform version. Valid values are same as <code>layout</code>, and lower.
     */
    private              String      platform;

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


    public String getPathForTool(String tool) throws MojoExecutionException {
        if ("1.1".equals(layout)) {
            return path + "/tools/" + tool;
        }

        if ("1.5".equals(layout)) {
            if (commonToolsIn11And15.contains(tool)) {
                return path + "/tools/" + tool;
            }else {
                if (layout.compareTo(platform) < 0) {
                    throw new MojoExecutionException("Platform version \""+platform+"\" must not be greater than layout \"" + layout + "\"!");
                }
                return path + "/platforms/android-" + platform + "/tools/" + tool;
            }
        }

        throw new MojoExecutionException("Invalid Layout \"" + layout + "\"!");
    }

    public String getPathForFrameworkAidl() throws MojoExecutionException {
        if ("1.1".equals(layout)) {
            return path + "/tools/lib/framework.aidl";
        }

        if ("1.5".equals(layout)) {
            if (layout.compareTo(platform) < 0) {
                throw new MojoExecutionException("Platform version \""+platform+"\" must not be greater than layout \"" + layout + "\"!");
            }
            return path + "/platforms/android-" + platform + "/framework.aidl";
        }

        throw new MojoExecutionException("Invalid Layout \"" + layout + "\"!");
    }

}
