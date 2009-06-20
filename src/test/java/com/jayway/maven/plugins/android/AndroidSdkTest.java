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

import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Excercises the {@link AndroidSdk} class.
 *
 * @author hugo.josefson@jayway.com
 */
public class AndroidSdkTest {
    private static final String ENV_ANDROID_SDK_11 = System.getenv("ANDROID_SDK_11");
    private static final String ENV_ANDROID_SDK_15 = System.getenv("ANDROID_SDK_15");

    public static final AndroidSdk SDK_1_1;
    static{
        Assert.assertNotNull("For running the tests, you must have environment variable ANDROID_SDK_11 set to a valid Android SDK 1.1 directory.", ENV_ANDROID_SDK_11);
        SDK_1_1 = new AndroidSdk(new File(ENV_ANDROID_SDK_11), "1.1");
        
    }

    public static final AndroidSdk SDK_1_5_PLATFORM_1_5;
    static{
        Assert.assertNotNull("For running the tests, you must have environment variable ANDROID_SDK_15 set to a valid Android SDK 1.5 directory.", ENV_ANDROID_SDK_15);
        SDK_1_5_PLATFORM_1_5 = new AndroidSdk(new File(ENV_ANDROID_SDK_15), "1.5");
        
    }

    public static final AndroidSdk SDK_1_5_PLATFORM_DEFAULT;
    static{
        Assert.assertNotNull("For running the tests, you must have environment variable ANDROID_SDK_15 set to a valid Android SDK 1.5 directory.", ENV_ANDROID_SDK_15);
        SDK_1_5_PLATFORM_DEFAULT = new AndroidSdk(new File(ENV_ANDROID_SDK_15), null);

    }

    @Test
    public void givenLayout1dot1AndToolAdbThenPathIs1dot1Style() {
        final String pathForTool = SDK_1_1.getPathForTool("adb");
        Assert.assertEquals(ENV_ANDROID_SDK_11 + "/tools/adb", pathForTool);
    }

    @Test
    public void givenLayout1dot1AndToolAndroidThenPathIs1dot1Style() {
        final String pathForTool = SDK_1_1.getPathForTool("android");
        Assert.assertEquals(ENV_ANDROID_SDK_11 + "/tools/android", pathForTool);
    }

    @Test
    public void givenLayout1dot1AndToolAaptThenPathIs1dot1Style() {
        final String pathForTool = SDK_1_1.getPathForTool("aapt");
        Assert.assertEquals(ENV_ANDROID_SDK_11 + "/tools/aapt", pathForTool);
    }

    @Test
    public void givenLayout1dot5AndToolAdbThenPathIsCommon() {
        final String pathForTool = SDK_1_5_PLATFORM_1_5.getPathForTool("adb");
        Assert.assertEquals(ENV_ANDROID_SDK_15 + "/tools/adb", pathForTool);
    }

    @Test
    public void givenLayout1dot5AndToolAndroidThenPathIsCommon() {
        final String pathForTool = SDK_1_5_PLATFORM_1_5.getPathForTool("android");
        Assert.assertEquals(ENV_ANDROID_SDK_15 + "/tools/android", pathForTool);
    }

    @Test
    public void givenLayout1dot5AndToolAaptAndPlatform1dot1ThenPathIsPlatform1dot1() {
        final AndroidSdk sdk = new AndroidSdk(new File(ENV_ANDROID_SDK_15), "1.1");
        final String pathForTool = sdk.getPathForTool("aapt");
        Assert.assertEquals(ENV_ANDROID_SDK_15 + "/platforms/android-1.1/tools/aapt", pathForTool);
    }

    @Test
    public void givenLayout1dot5AndToolAaptAndPlatform1dot5ThenPathIsPlatform1dot5() {
        final AndroidSdk sdk = new AndroidSdk(new File(ENV_ANDROID_SDK_15), "1.5");
        final String pathForTool = sdk.getPathForTool("aapt");
        Assert.assertEquals(ENV_ANDROID_SDK_15 + "/platforms/android-1.5/tools/aapt", pathForTool);
    }

    @Test(expected = InvalidSdkException.class)
    public void givenLayout1dot5AndToolAaptAndPlatform1dot6ThenException() {
        new AndroidSdk(new File(ENV_ANDROID_SDK_15), "1.6").getPathForTool("aapt");
    }

    @Test(expected = InvalidSdkException.class)
    public void givenInvalidSdkPathThenException() throws IOException {
        new AndroidSdk(File.createTempFile("maven-android-plugin", "test"), null).getLayout();
    }

    @Test
    public void givenSdk11PathThenLayoutIs11(){
        Assert.assertEquals(new AndroidSdk(new File(ENV_ANDROID_SDK_11), null).getLayout(), AndroidSdk.Layout.LAYOUT_1_1);
    }

    @Test
    public void givenSdk15PathThenLayoutIs15(){
        Assert.assertEquals(SDK_1_5_PLATFORM_DEFAULT.getLayout(), AndroidSdk.Layout.LAYOUT_1_5);
    }

    @Test
    public void givenSdk1dodt1ThenPlatformEqualsPath() throws IllegalAccessException {
        final File path = (File) ReflectionUtils.getValueIncludingSuperclasses("path", SDK_1_1);
        Assert.assertEquals(path, SDK_1_1.getPlatform());
    }

    @Test
    public void givenSdk1dodt5AndPlatform1dot5ThenPlatformis1dot5() throws IllegalAccessException {
        final File path = (File) ReflectionUtils.getValueIncludingSuperclasses("path", SDK_1_5_PLATFORM_1_5);
        Assert.assertEquals(new File(path, "/platforms/android-1.5"), SDK_1_5_PLATFORM_1_5.getPlatform());
    }

    @Test
    public void givenSdk1dodt5AndPlatformNullThenPlatformis1dot5() throws IllegalAccessException {
        final File path = (File) ReflectionUtils.getValueIncludingSuperclasses("path", SDK_1_5_PLATFORM_DEFAULT);
        Assert.assertEquals(new File(path, "/platforms/android-1.5"), SDK_1_5_PLATFORM_DEFAULT.getPlatform());
    }
}
