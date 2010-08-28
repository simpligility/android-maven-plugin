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

import java.io.File;

/**
 * Configuration for an Android SDK. Only receives config parameter values, and there is no logic in here. Logic is in
 * {@link AndroidSdk}.
 *
 * @author hugo.josefson@jayway.com
 */
public class Sdk {

    /**
     * Directory of the installed Android SDK, for example <code>/opt/android-sdk-linux_x86-1.5_r1</code>
     *
     * @parameter expression="${android.sdk.path}"
     * @required
     */
    private File path;

    /**
     * <p>Chosen platform version. Valid values are whichever platforms are available in the SDK, under the directory
     * <code>platforms</code>. Defaults to the highest available one if not set.</p>
     * <p>Note: this parameter is just the version number, without <code>"android-"</code> in the
     * beginning.</p>
     *
     * @parameter expression="${android.sdk.platform}"
     */
    private String platform;

    public File getPath() {
        return path;
    }

    public String getPlatform() {
        return platform;
    }
}
