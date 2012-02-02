package com.jayway.maven.plugins.android.common;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Johan Lindquist
 */
public class NativeHelperTest {

    @Test
    public void invalidVersions()
    {
        String[] versions = {"r4", "r5", "r5b", "r5c", "r6", "r6b"};

        for (int i = 0; i < versions.length; i++) {
            String version = versions[i];
            try {
                NativeHelper.validateNDKVersion(7,version);
                Assert.fail("Version should fail: " + version);
            } catch (MojoExecutionException e) {
            }
        }
    }

    @Test
    public void validVersions()
    {
        String[] versions = {"r7", "r8a", "r8z", "r10", "r19b", "r25", "r100", "r100b"};

        for (int i = 0; i < versions.length; i++) {
            String version = versions[i];
            try {
                NativeHelper.validateNDKVersion(7,version);
            } catch (MojoExecutionException e) {
                Assert.fail("Version should not fail: " + version);
            }
        }
    }


}
