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
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Excercises the {@link AndroidSdk} class.
 *
 * @author hugo.josefson@jayway.com
 */
public class AndroidSdkTest {
    private static final File THEPATH = new File("THEPATH");

    @Test(expected = MojoExecutionException.class)
    public void givenLayout1dot0ThenException() throws MojoExecutionException {
        constructAndroidSdkWith("1.0", null, null).getPathForTool("adb");
    }

    @Test
    public void givenLayout1dot1ThenNoException() throws MojoExecutionException {
        constructAndroidSdkWith("1.1", null, null).getPathForTool("adb");
    }

    @Test
    public void givenLayout1dot5ThenNoException() throws MojoExecutionException {
        constructAndroidSdkWith("1.5", null, null).getPathForTool("adb");
    }

    @Test(expected = MojoExecutionException.class)
    public void givenLayout2dot0ThenException() throws MojoExecutionException {
        constructAndroidSdkWith("2.0", null, null).getPathForTool("adb");
    }

    @Test
    public void givenLayout1dot1AndToolAdbThenPathIs1dot1Style() throws MojoExecutionException {
        final AndroidSdk sdk = constructAndroidSdkWith("1.1", THEPATH, null);
        final String pathForTool = sdk.getPathForTool("adb");
        Assert.assertEquals("THEPATH/tools/adb", pathForTool);
    }

    @Test
    public void givenLayout1dot1AndToolAndroidThenPathIs1dot1Style() throws MojoExecutionException {
        final AndroidSdk sdk = constructAndroidSdkWith("1.1", THEPATH, null);
        final String pathForTool = sdk.getPathForTool("android");
        Assert.assertEquals("THEPATH/tools/android", pathForTool);
    }

    @Test
    public void givenLayout1dot1AndToolAaptThenPathIs1dot1Style() throws MojoExecutionException {
        final AndroidSdk sdk = constructAndroidSdkWith("1.1", THEPATH, null);
        final String pathForTool = sdk.getPathForTool("aapt");
        Assert.assertEquals("THEPATH/tools/aapt", pathForTool);
    }

    @Test
    public void givenLayout1dot5AndToolAdbThenPathIsCommon() throws MojoExecutionException {
        final AndroidSdk sdk = constructAndroidSdkWith("1.5", THEPATH, null);
        final String pathForTool = sdk.getPathForTool("adb");
        Assert.assertEquals("THEPATH/tools/adb", pathForTool);
    }

    @Test
    public void givenLayout1dot5AndToolAndroidThenPathIsCommon() throws MojoExecutionException {
        final AndroidSdk sdk = constructAndroidSdkWith("1.5", THEPATH, null);
        final String pathForTool = sdk.getPathForTool("android");
        Assert.assertEquals("THEPATH/tools/android", pathForTool);
    }

    @Test
    public void givenLayout1dot5AndToolAaptAndPlatform1dot1ThenPathIsPlatform1dot1() throws MojoExecutionException {
        final AndroidSdk sdk = constructAndroidSdkWith("1.5", THEPATH, "1.1");
        final String pathForTool = sdk.getPathForTool("aapt");
        Assert.assertEquals("THEPATH/platforms/android-1.1/tools/aapt", pathForTool);
    }

    @Test
    public void givenLayout1dot5AndToolAaptAndPlatform1dot5ThenPathIsPlatform1dot5() throws MojoExecutionException {
        final AndroidSdk sdk = constructAndroidSdkWith("1.5", THEPATH, "1.5");
        final String pathForTool = sdk.getPathForTool("aapt");
        Assert.assertEquals("THEPATH/platforms/android-1.5/tools/aapt", pathForTool);
    }

    @Test(expected = MojoExecutionException.class)
    public void givenLayout1dot5AndToolAaptAndPlatform1dot6ThenException() throws MojoExecutionException {
        constructAndroidSdkWith("1.5", null, "1.6").getPathForTool("aapt");
    }

    /**
     * Constructs a testable instance of {@link AndroidSdk} with specified values set.
     *
     * @param layout   The <code>layout</code> value to set inside the <code>AndroidSdk</code>.
     * @param path     The <code>path</code> value to set inside the <code>AndroidSdk</code>.
     * @param platform The <code>platform</code> value to set inside the <code>AndroidSdk</code>.
     * @return an instance to test
     */
    protected static AndroidSdk constructAndroidSdkWith(String layout, File path, String platform) {
        final AndroidSdk sdk = new AndroidSdk();
        try {
            ReflectionUtils.setVariableValueInObject(sdk, "layout", layout);
            ReflectionUtils.setVariableValueInObject(sdk, "path", path);
            ReflectionUtils.setVariableValueInObject(sdk, "platform", platform);
        } catch (IllegalAccessException e) {
            // Retrow unchecked, so callers won't have to care.
            throw new RuntimeException(e);
        }
        return sdk;
    }
}
