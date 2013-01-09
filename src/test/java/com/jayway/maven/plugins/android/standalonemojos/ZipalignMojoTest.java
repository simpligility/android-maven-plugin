package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import org.apache.maven.project.MavenProject;
import org.powermock.reflect.Whitebox;

import java.io.File;

/**
 * User: Eugen
 */
public class ZipalignMojoTest extends AbstractAndroidMojoTestCase<ZipalignMojo>
{
    @Override
    public String getPluginGoalName()
    {
        return "zipalign";
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception
     */
    public void testDefaultZipalignConfig() throws Exception
    {
        ZipalignMojo mojo = createMojo( "zipalign-config-project0" );
        final ConfigHandler cfh = new ConfigHandler( mojo );
        cfh.parseConfiguration();

        Boolean skip = Whitebox.getInternalState( mojo, "parsedSkip" );
        assertTrue("zipalign 'skip' parameter should be true", skip);

        Boolean verbose = Whitebox.getInternalState( mojo, "parsedVerbose" );
        assertFalse( "zipalign 'verbose' parameter should be false", verbose );

        MavenProject project = Whitebox.getInternalState( mojo, "project" );

        String inputApk = Whitebox.getInternalState( mojo, "parsedInputApk" );
        File inputApkFile = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".apk" );
        assertEquals( "zipalign 'inputApk' parameter should be equal", inputApkFile.getAbsolutePath(), inputApk );

        String outputApk = Whitebox.getInternalState( mojo, "parsedOutputApk" );
        File outputApkFile = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName()
                + "-aligned.apk" );
        assertEquals( "zipalign 'outputApk' parameter should be equal", outputApkFile.getAbsolutePath(), outputApk );
    }
}
