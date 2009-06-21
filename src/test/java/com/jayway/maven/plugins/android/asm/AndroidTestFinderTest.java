package com.jayway.maven.plugins.android.asm;

import org.junit.Test;
import org.junit.Assert;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

/**
 * Exercises the {@link com.jayway.maven.plugins.android.asm.AndroidTestFinder} class.
 *
 * @author hugo.josefson@jayway.com
 */
public class AndroidTestFinderTest {
    @Test
    public void givenLocalAndroidAppDirectoryThenNoTests() throws MojoExecutionException {
        final boolean result = AndroidTestFinder.containsAndroidTests(new File("/home/hugo/code/way/maven-android-plugin-samples/apidemos-15/apidemos-15-app/target/android-classes"));
        Assert.assertFalse("apidemos-15-app should not contain any tests.", result);
    }
    @Test
    public void givenLocalPlatformTestDirectoryThenItContainsTests() throws MojoExecutionException {
        final boolean result = AndroidTestFinder.containsAndroidTests(new File("/home/hugo/code/way/maven-android-plugin-samples/apidemos-15/apidemos-15-platformtests/target/android-classes"));
        Assert.assertTrue("apidemos-15-platformtests should contain tests.", result);
    }
}
