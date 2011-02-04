package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class VersionUpdateMojoTest {
    @Rule
    public TestName name = new TestName();

    @Test
    public void testBasicAndroidProject() throws Exception {
        File testdir = executeMojo("version-tests/basic-android-project");
        assertExpectedAndroidManifest(testdir);
    }

    @Test
    public void testBasicJarProject() throws Exception {
        File testdir = executeMojo("version-tests/basic-jar-project");
        File manifestFile = new File(testdir, "AndroidManifest.xml");
        Assert.assertFalse("Should not have an AndroidManifest for a jar project", manifestFile.exists());
    }

    @Test
    public void testVersionlessAndroidProject() throws Exception {
        File testdir = executeMojo("version-tests/versionless-android-project");
        assertExpectedAndroidManifest(testdir);
    }

    private void assertExpectedAndroidManifest(File testdir) throws IOException {
        File manifestFile = new File(testdir, "AndroidManifest.xml");
        File expectFile = new File(testdir, "AndroidManifest-expected.xml");
        String actual = FileUtils.readFileToString(manifestFile);
        String expected = FileUtils.readFileToString(expectFile);
        Assert.assertEquals(expected, actual);
    }

    /**
     * Copy the project specified into a temporary testing directory. Execute the mojo. And return the testdir for the
     * project.
     * 
     * @param resourceProject
     *            the resourceProject path.
     * @return the directory for the temporary testing directory that was used.
     * @throws Exception
     *             if unable to execute the mojo
     */
    private File executeMojo(String resourceProject) throws Exception {
        // Establish test details project example
        String testResourcePath = "src/test/resources/" + resourceProject;
        testResourcePath = FilenameUtils.separatorsToSystem(testResourcePath);
        File exampleDir = new File(getBasedir(), testResourcePath);
        Assert.assertTrue("Path should exist: " + exampleDir, exampleDir.exists());

        // Establish the temporary testing directory.
        String testingPath = "target/tests/" + this.getClass().getSimpleName() + "." + name.getMethodName();
        testingPath = FilenameUtils.separatorsToSystem(testingPath);
        File testingDir = new File(getBasedir(), testingPath);

        if (testingDir.exists()) {
            FileUtils.cleanDirectory(testingDir);
        } else {
            Assert.assertTrue("Could not create directory: " + testingDir, testingDir.mkdirs());
        }

        // Copy project example into temporary testing directory
        // to avoid messing up the good source copy, as mojo can change
        // the AndroidManifest.xml file.
        FileUtils.copyDirectory(exampleDir, testingDir);

        // Prepare MavenProject
        MavenProject stub = new MojoProjectStub(testingDir);

        // Setup Mojo
        VersionUpdateMojo mojo = new VersionUpdateMojo();
        setMojoVariable(mojo, "project", stub);

        // Execute Mojo
        mojo.execute();

        // Return temporary testing directory
        return testingDir;
    }

    /**
     * Basedir for the maven-android-plugin project itself.
     */
    private File getBasedir() {
        // Use "basedir" that surefire provides.
        String basedirPath = System.getProperty("basedir");
        if (StringUtils.isNotBlank(basedirPath)) {
            return new File(basedirPath);
        }

        // Use the java system default for CWD.
        return SystemUtils.getUserDir();
    }

    /**
     * Convenience method to set values to variables in mojo's that don't have setters
     */
    protected void setMojoVariable(Mojo mojo, String variable, Object value) throws IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(variable, mojo.getClass());
        field.setAccessible(true);
        field.set(mojo, value);
    }
}
