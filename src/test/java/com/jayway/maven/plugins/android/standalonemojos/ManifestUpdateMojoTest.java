package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ManifestUpdateMojoTest extends AbstractAndroidMojoTestCase<ManifestUpdateMojo> {
    @Override
    public String getPluginGoalName() {
        return "manifest-update";
    }

    @Test
    public void testBasicAndroidProjectVersion() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/basic-android-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }

	@Test
    public void testBasicAndroidProjectManifest() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/basic-android-project-manifest");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    @Test
    public void testBasicJarProject() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/basic-jar-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        Assert.assertFalse("Should not have an AndroidManifest for a jar project", manifestFile.exists());
    }

    @Test
    public void testVersionlessAndroidProject() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/versionless-android-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "androidManifest.xml"); // intentionally small lowercase 'a'
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    @Test
    public void testManyVersionsAndroidProject() throws Exception {
        ManifestUpdateMojo mojo = createMojo("manifest-tests/manyversions-android-project");
        for (int i = 0; i < 50; i++) { // Simulate 50 runs of the mojo
            mojo.execute();
        }
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }

	@Test
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

    private void assertExpectedAndroidManifest(File manifestFile, File testdir) throws IOException {
        File expectFile = new File(testdir, "AndroidManifest-expected.xml");
        String actual = FileUtils.readFileToString(manifestFile);
        String expected = FileUtils.readFileToString(expectFile);
        Assert.assertEquals(expected, actual);
    }
}
