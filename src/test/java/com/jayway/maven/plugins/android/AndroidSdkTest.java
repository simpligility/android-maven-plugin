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
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Excercises the {@link AndroidSdk} class.
 *
 * @author hugo.josefson@jayway.com
 */
public class AndroidSdkTest {
    
    private SdkTestSupport sdkTestSupport; 
    
    @Before
    public void setUp(){
        sdkTestSupport = new SdkTestSupport();
    }

    @Test
    public void givenToolAdbThenPathIsCommon() {
        final String pathForTool =sdkTestSupport.getSdk_with_platform_1_5().getPathForTool("adb");
        Assert.assertEquals(sdkTestSupport.getEnv_ANDROID_HOME() + "/tools/adb", pathForTool);
    }

    @Test
    public void givenToolAndroidThenPathIsCommon() {
        final String pathForTool =sdkTestSupport.getSdk_with_platform_1_5().getPathForTool("android");
        Assert.assertEquals(sdkTestSupport.getEnv_ANDROID_HOME() + "/tools/android", pathForTool);
    }

    @Test
    public void givenToolAaptAndPlatform1dot1ThenPathIsPlatform1dot1() {
        final AndroidSdk sdk = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "2");
        final String pathForTool = sdk.getPathForTool("aapt");
        Assert.assertEquals(sdkTestSupport.getEnv_ANDROID_HOME() + "/platforms/android-2/tools/aapt", pathForTool);
    }

    @Test
    public void givenToolAaptAndPlatform1dot5ThenPathIsPlatform1dot5() {
        final AndroidSdk sdk = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "3");
        final String pathForTool = sdk.getPathForTool("aapt");
        Assert.assertEquals(sdkTestSupport.getEnv_ANDROID_HOME() + "/platforms/android-3/tools/aapt", pathForTool);
    }

    @Test(expected = InvalidSdkException.class)
    public void givenInvalidSdkPathThenException() throws IOException {
        new AndroidSdk(File.createTempFile("maven-android-plugin", "test"), null).getLayout();
    }

    @Test(expected = InvalidSdkException.class)
    public void givenInvalidPlatformStringThenException() throws IOException {
        final AndroidSdk sdk = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "invalidplatform");
    }

    @Test
    public void givenSdk15PathThenLayoutIs15(){
        Assert.assertEquals(sdkTestSupport.getSdk_with_platform_default().getLayout(), AndroidSdk.Layout.LAYOUT_1_5);
    }

    @Test
    public void givenPlatform1dot5ThenPlatformis1dot5() throws IllegalAccessException {
        final File path = (File) ReflectionUtils.getValueIncludingSuperclasses("path",sdkTestSupport.getSdk_with_platform_1_5());
        Assert.assertEquals(new File(path, "/platforms/android-3"),sdkTestSupport.getSdk_with_platform_1_5().getPlatform());
    }

    @Test
    public void givenPlatformNullThenPlatformisSomethingValidLooking() throws IllegalAccessException {
        final File path = (File) ReflectionUtils.getValueIncludingSuperclasses("path",sdkTestSupport.getSdk_with_platform_default());
        final File platform = sdkTestSupport.getSdk_with_platform_default().getPlatform();
        final String platformPath = platform.getAbsolutePath();
        final String pathPath = path.getAbsolutePath();
        final String regex = pathPath + "/platforms/android-[0-9]+.*";
//        System.out.println("platformPath = " + platformPath);
//        System.out.println("pathPath = " + pathPath);
//        System.out.println("regex = " + regex);
        Assert.assertTrue(platformPath.matches(regex));
    }
}
