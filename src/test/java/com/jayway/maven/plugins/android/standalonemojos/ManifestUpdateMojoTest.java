package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;

public class ManifestUpdateMojoTest extends AbstractAndroidMojoTestCase<ManifestUpdateMojo> {
    @Override
    public String getPluginGoalName() {
        return "manifest-update";
    }

    public void testAndroidApplicationChanges() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/application-changes");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }    
    
    public void testBasicAndroidProjectVersion() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/basic-android-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    public void testBasicAndroidProjectManifest() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/basic-android-project-manifest");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    public void testBasicJarProject() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/basic-jar-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        Assert.assertFalse("Should not have an AndroidManifest for a jar project", manifestFile.exists());
    }

    public void testVersionlessAndroidProject() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/versionless-android-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "androidManifest.xml"); // intentionally small lowercase 'a'
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    public void testManyVersionsAndroidProject() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/manyversions-android-project");
        for (int i = 0; i < 50; i++) { // Simulate 50 runs of the mojo
            mojo.execute();
        }
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    public void testMinorVersionAndroidProject() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/minorversion-android-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    public void testWhenNewVersionHasLessDigitsItshouldBePaddedSoVersionCodeIsNotLess() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/differentLengthVersion-android-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    public void testVersionCodeUpdateAndIncrementFail() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/bad-android-project1");
        try {
			mojo.execute();
        } catch (MojoFailureException e) {
	        Assert.assertTrue(e.getMessage().startsWith("versionCodeAutoIncrement, versionCodeUpdateFromVersion and versionCode"));
	        return;
        }
		Assert.assertTrue("bad-android-project1 did not throw MojoFailureException", false);
    }

	public void testVersionCodeAndVersionCodeUpdateFail() throws Exception {
	    ManifestUpdateMojo mojo = createMojo("manifest-tests/bad-android-project2");
	    try {
			mojo.execute();
	    } catch (MojoFailureException e) {
		    Assert.assertTrue(e.getMessage().startsWith("versionCodeAutoIncrement, versionCodeUpdateFromVersion and versionCode"));
		    return;
	    }
		Assert.assertTrue("bad-android-project2 did not throw MojoFailureException", false);
	}

	public void testVersionCodeAndVersionIncrementFail() throws Exception {
	    ManifestUpdateMojo mojo = createMojo("manifest-tests/bad-android-project3");
	    try {
			mojo.execute();
	    } catch (MojoFailureException e) {
		    Assert.assertTrue(e.getMessage().startsWith("versionCodeAutoIncrement, versionCodeUpdateFromVersion and versionCode"));
		    return;
	    }
		Assert.assertTrue("bad-android-project3 did not throw MojoFailureException", false);
	}

    public void testSupportsScreensUpdate() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/supports-screens-android-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        // this asserts that:
        // 1) the values of anyDensity, largeScreens and normalScreens will be changed via the POM
        // 2) the value of smallScreens will remain untouched (not overridden in POM)
        // 3) the value of xlargeScreens will be added (defined in POM but not in Manifest)
        // 4) the value of resizeable will be ignored (undefined in both Manifest and POM)
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    public void DISABLED_testCompatibleScreensUpdate() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/compatible-screens-android-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        // this asserts that:
        // 1) the screen small/ldpi will be changed via the POM
        // 2) the screen normal/mdpi will remain untouched (overridden in POM but equal)
        // 3) the screen normal/hdpi will remain untouched (not overridden in POM)
        // 4) the screen large/xhdpi will be added (defined in POM but not in Manifest)
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    public void testProviderAuthoritiesUpdate() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/provider-authorities-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    private void assertExpectedAndroidManifest(File manifestFile, File testdir) throws IOException {
        File expectFile = new File(testdir, "AndroidManifest-expected.xml");
        // different white space causes issues when between going Windows and *nix via git and wrongly configured
        // autocrlf .. since we dont need to worry about whitespace.. we strip it out
        String actual = StringUtils.deleteWhitespace(FileUtils.readFileToString(manifestFile));
        String expected = StringUtils.deleteWhitespace(FileUtils.readFileToString(expectFile));
        Assert.assertEquals(expected, actual);
    }
}
