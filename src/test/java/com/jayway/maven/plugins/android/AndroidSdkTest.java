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
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Excercises the {@link AndroidSdk} class.
 *
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class AndroidSdkTest {
    
    private SdkTestSupport sdkTestSupport; 
    
    @Before
    public void setUp(){
        sdkTestSupport = new SdkTestSupport();
    }

    @Test
    public void givenToolAdbThenPathIsPlatformTools() {
        final String pathForTool =sdkTestSupport.getSdk_with_platform_1_5().getPathForTool("adb");
        Assert.assertEquals(new File(sdkTestSupport.getEnv_ANDROID_HOME() + "/platform-tools/adb").getAbsolutePath(), pathForTool);
    }

    @Test
    public void givenToolAndroidThenPathIsCommon() {
        final String pathForTool =sdkTestSupport.getSdk_with_platform_1_5().getPathForTool("android");
        Assert.assertEquals(new File(sdkTestSupport.getEnv_ANDROID_HOME() + "/tools/android").getAbsolutePath(), pathForTool);
    }

    @Test
    public void givenToolAaptAndPlatform1dot1ThenPathIsPlatformTools() {
        final AndroidSdk sdk = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "2");
        final String pathForTool = sdk.getPathForTool("aapt");
        Assert.assertEquals(new File(sdkTestSupport.getEnv_ANDROID_HOME() + "/platform-tools/aapt"), new File(pathForTool));
    }

    @Test
    public void givenToolAaptAndPlatform1dot5ThenPathIsPlatformTools() {
        final AndroidSdk sdk = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "3");
        final String pathForTool = sdk.getPathForTool("aapt");
        Assert.assertEquals(new File(sdkTestSupport.getEnv_ANDROID_HOME() + "/platform-tools/aapt"), new File(pathForTool));
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
    public void givenDefaultSdkThenLayoutIs23(){
        Assert.assertEquals(sdkTestSupport.getSdk_with_platform_default().getLayout(), AndroidSdk.Layout.LAYOUT_2_3);
    }

    @Test
    public void givenPlatform1dot5ThenPlatformis1dot5() throws IllegalAccessException {
        final File path = (File) ReflectionUtils.getValueIncludingSuperclasses("sdkPath",sdkTestSupport.getSdk_with_platform_1_5());
        Assert.assertEquals(new File(path, "/platforms/android-3"),sdkTestSupport.getSdk_with_platform_1_5().getPlatform());
    }

    @Test
    public void givenPlatformNullThenPlatformisSomethingValidLooking() throws IllegalAccessException, URISyntaxException {
        final File path = (File) ReflectionUtils.getValueIncludingSuperclasses("sdkPath",sdkTestSupport.getSdk_with_platform_default());
        final File platform = sdkTestSupport.getSdk_with_platform_default().getPlatform();
        final String platformPath = platform.getAbsolutePath();
        final String pathPath = path.getAbsolutePath();
        final String regex = new File(pathPath + "/platforms/android-.*").toURI().toString();
        Assert.assertTrue(new File(platformPath).toURI().toString().matches(regex));
    }

    /**
     * Test all available platforms and api level versions. All have to be installed locally
     * for this test to pass including the obsolete ones.
     */
    @Test
    public void validPlatformsAndApiLevels() {
        final AndroidSdk sdk2 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "2");
        final AndroidSdk sdk3 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "3");
        final AndroidSdk sdk4 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "4");
        final AndroidSdk sdk5 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "5");
        final AndroidSdk sdk6 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "6");
        final AndroidSdk sdk7 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "7");
        final AndroidSdk sdk8 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "8");
        final AndroidSdk sdk9 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "9");

        final AndroidSdk sdk1_1 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "1.1");
        final AndroidSdk sdk1_5 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "1.5");
        final AndroidSdk sdk1_6 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "1.6");
        final AndroidSdk sdk2_0 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "2.0");
        final AndroidSdk sdk2_0_1 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "2.0.1");
        final AndroidSdk sdk2_1 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "2.1");
        final AndroidSdk sdk2_2 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "2.2");
        final AndroidSdk sdk2_3 = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "2.3");
    }

    @Test(expected = InvalidSdkException.class)
    public void invalidPlatformAndApiLevels() {
        final AndroidSdk invalid = new AndroidSdk(new File(sdkTestSupport.getEnv_ANDROID_HOME()), "invalid");

    }


}
