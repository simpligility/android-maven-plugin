package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;

public class VersionUpdateMojoTest extends AbstractAndroidMojoTestCase<VersionUpdateMojo> {
    @Override
    public String getPluginGoalName() {
        return "version-update";
    }

    @Test
    public void testBasicAndroidProject() throws Exception {
        VersionUpdateMojo mojo = createMojo("version-tests/basic-android-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    @Test
    public void testBasicJarProject() throws Exception {
        VersionUpdateMojo mojo = createMojo("version-tests/basic-jar-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "AndroidManifest.xml");
        Assert.assertFalse("Should not have an AndroidManifest for a jar project", manifestFile.exists());
    }

    @Test
    public void testVersionlessAndroidProject() throws Exception {
        VersionUpdateMojo mojo = createMojo("version-tests/versionless-android-project");
        mojo.execute();
        File dir = getProjectDir(mojo);
        File manifestFile = new File(dir, "androidManifest.xml"); // intentionally small lowercase 'a'
        assertExpectedAndroidManifest(manifestFile, dir);
    }

    private void assertExpectedAndroidManifest(File manifestFile, File testdir) throws IOException {
        File expectFile = new File(testdir, "AndroidManifest-expected.xml");
        String actual = FileUtils.readFileToString(manifestFile);
        String expected = FileUtils.readFileToString(expectFile);
        Assert.assertEquals(expected, actual);
    }
}
