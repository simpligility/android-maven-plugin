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

import org.junit.Assert;

import java.io.File;

/**
 * @author hugo.josefson@jayway.com
 */
public class SdkTestSupport {
    private final String env_ANDROID_HOME = System.getenv("ANDROID_HOME");

    private final AndroidSdk sdk_with_platform_default;

    public SdkTestSupport() {
        Assert.assertNotNull("For running the tests, you must have environment variable ANDROID_HOME set to a valid Android SDK 22 directory.", env_ANDROID_HOME);

        sdk_with_platform_default = new AndroidSdk(new File(env_ANDROID_HOME), "19");
    }

    public String getEnv_ANDROID_HOME() {
        return env_ANDROID_HOME;
    }

    public AndroidSdk getSdk_with_platform_default() {
        return sdk_with_platform_default;
    }
}
