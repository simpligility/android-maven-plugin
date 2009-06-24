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
    private final String env_ANDROID_SDK_11 = System.getenv("ANDROID_SDK_11");
    private final String env_ANDROID_SDK_15 = System.getenv("ANDROID_SDK_15");

    private final AndroidSdk sdk_1_1;
    private final AndroidSdk sdk_1_5_platform_1_5;
    private final AndroidSdk sdk_1_5_platform_default;

    public SdkTestSupport() {
        Assert.assertNotNull("For running the tests, you must have environment variable ANDROID_SDK_11 set to a valid Android SDK 1.1 directory.", env_ANDROID_SDK_11);
        sdk_1_1 = new AndroidSdk(new File(env_ANDROID_SDK_11), "1.1");

        Assert.assertNotNull("For running the tests, you must have environment variable ANDROID_SDK_15 set to a valid Android SDK 1.5 directory.", env_ANDROID_SDK_15);
        sdk_1_5_platform_1_5 = new AndroidSdk(new File(env_ANDROID_SDK_15), "1.5");

        Assert.assertNotNull("For running the tests, you must have environment variable ANDROID_SDK_15 set to a valid Android SDK 1.5 directory.", env_ANDROID_SDK_15);
        sdk_1_5_platform_default = new AndroidSdk(new File(env_ANDROID_SDK_15), null);
    }

    public String getEnv_ANDROID_SDK_11() {
        return env_ANDROID_SDK_11;
    }

    public String getEnv_ANDROID_SDK_15() {
        return env_ANDROID_SDK_15;
    }

    public AndroidSdk getSdk_1_1() {
        return sdk_1_1;
    }

    public AndroidSdk getSdk_1_5_platform_1_5() {
        return sdk_1_5_platform_1_5;
    }

    public AndroidSdk getSdk_1_5_platform_default() {
        return sdk_1_5_platform_default;
    }
}
