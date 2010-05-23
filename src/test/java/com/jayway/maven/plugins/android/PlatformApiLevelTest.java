package com.jayway.maven.plugins.android;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class PlatformApiLevelTest {

    @Test
    public void checkTotalNumber() {
        Assert.assertEquals(7, PlatformApiLevel.values().length);
    }

    @Test
    public void checkApiLevelLookup() {
        Assert.assertEquals("1.1", PlatformApiLevel.findPlatform("2"));
        Assert.assertEquals("1.5", PlatformApiLevel.findPlatform("3"));
        Assert.assertEquals("1.6", PlatformApiLevel.findPlatform("4"));
        Assert.assertEquals("2.0", PlatformApiLevel.findPlatform("5"));
        Assert.assertEquals("2.01", PlatformApiLevel.findPlatform("6"));
        Assert.assertEquals("2.1", PlatformApiLevel.findPlatform("7"));
        Assert.assertEquals("2.2", PlatformApiLevel.findPlatform("8"));
    }

    @Test
    public void checkPlatformLookup() {
        Assert.assertEquals("2", PlatformApiLevel.findApiLevel("1.1"));
        Assert.assertEquals("3", PlatformApiLevel.findApiLevel("1.5"));
        Assert.assertEquals("4", PlatformApiLevel.findApiLevel("1.6"));
        Assert.assertEquals("5", PlatformApiLevel.findApiLevel("2.0"));
        Assert.assertEquals("6", PlatformApiLevel.findApiLevel("2.01"));
        Assert.assertEquals("7", PlatformApiLevel.findApiLevel("2.1"));
        Assert.assertEquals("8", PlatformApiLevel.findApiLevel("2.2"));
    }

    @Test
    public void checkValidPlatform() {
        Assert.assertTrue(PlatformApiLevel.isValidApiLevel("2"));
        Assert.assertTrue(PlatformApiLevel.isValidApiLevel("3"));
        Assert.assertTrue(PlatformApiLevel.isValidApiLevel("4"));
        Assert.assertTrue(PlatformApiLevel.isValidApiLevel("5"));
        Assert.assertTrue(PlatformApiLevel.isValidApiLevel("6"));
        Assert.assertTrue(PlatformApiLevel.isValidApiLevel("7"));
        Assert.assertTrue(PlatformApiLevel.isValidApiLevel("8"));

        Assert.assertFalse(PlatformApiLevel.isValidApiLevel("something"));
        Assert.assertFalse(PlatformApiLevel.isValidApiLevel("1.0"));
    }

    @Test
    public void checkValidApiLevel() {

        Assert.assertTrue(PlatformApiLevel.isValidPlatform("1.1"));
        Assert.assertTrue(PlatformApiLevel.isValidPlatform("1.5"));
        Assert.assertTrue(PlatformApiLevel.isValidPlatform("1.6"));
        Assert.assertTrue(PlatformApiLevel.isValidPlatform("2.0"));
        Assert.assertTrue(PlatformApiLevel.isValidPlatform("2.01"));
        Assert.assertTrue(PlatformApiLevel.isValidPlatform("2.1"));
        Assert.assertTrue(PlatformApiLevel.isValidPlatform("2.2"));

        Assert.assertFalse(PlatformApiLevel.isValidPlatform("something"));
        Assert.assertFalse(PlatformApiLevel.isValidPlatform("1.0"));
    }
}
