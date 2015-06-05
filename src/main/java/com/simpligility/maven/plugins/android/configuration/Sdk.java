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
package com.simpligility.maven.plugins.android.configuration;

import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Configuration for an Android SDK. Only receives config parameter values, and there is no logic in here. Logic is in
 * {@link com.simpligility.maven.plugins.android.AndroidSdk}.
 *
 * @author hugo.josefson@jayway.com
 */
public class Sdk
{

    /**
     * Directory of the installed Android SDK, for example <code>/opt/android-sdk-linux_x86-1.5_r1</code>
     */
    @Parameter ( property = "android.sdk.path", required = true )
    private File path;

    /**
     * <p>Chosen platform version. Valid values are whichever platforms are available in the SDK, under the directory
     * <code>platforms</code>. Defaults to the highest available one if not set.</p>
     * <p>Note: this parameter is just the version number, without <code>"android-"</code> in the
     * beginning.</p>
     */
    @Parameter ( property = "android.sdk.platform" )
    private String platform;

    /**
     * <p>Chosen Build-Tools version. Valid values are whichever build-tools are available in the SDK,
     * under the directory <code>buildTools</code>. Defaults to the latest available one if not set.</p>
     */
    @Parameter ( property = "android.sdk.buildTools" )
    private String buildTools;

    public File getPath()
    {
        return path;
    }

    public String getPlatform()
    {
        return platform;
    }

    public String getBuildTools()
    {
        return buildTools;
    }
}
