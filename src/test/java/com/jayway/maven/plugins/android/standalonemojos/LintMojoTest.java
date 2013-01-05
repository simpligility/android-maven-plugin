package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.powermock.reflect.Whitebox;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.jayway.maven.plugins.android.config.ConfigHandler;

public class LintMojoTest extends AbstractAndroidMojoTestCase< LintMojo >
{
    @Override
    public String getPluginGoalName()
    {
        return "lint";
    }

    public void testDefaultLintConfig() throws Exception
    {
        LintMojo mojo = createMojo( "lint-config-project1" );
        final ConfigHandler cfh = new ConfigHandler( mojo );
        cfh.parseConfiguration();
        MavenProject project = Whitebox.getInternalState( mojo, "project" );

        Boolean lintSkip = Whitebox.getInternalState( mojo, "parsedSkip" );
        Boolean lintFailOnError = Whitebox.getInternalState( mojo, "parsedFailOnError" );
        Boolean lintIgnoreWarnings = Whitebox.getInternalState( mojo, "parsedIgnoreWarnings" );
        Boolean lintWarnAll = Whitebox.getInternalState( mojo, "parsedWarnAll" );
        Boolean lintWarningsAsErrors = Whitebox.getInternalState( mojo, "parsedWarningsAsErrors" );
        String lintConfig = Whitebox.getInternalState( mojo, "parsedConfig" );

        // TODO finish all default settings tests
        Boolean lintEnableXml = Whitebox.getInternalState( mojo, "parsedEnableXml" );
        Boolean lintEnableHtml = Whitebox.getInternalState( mojo, "parsedEnableHtml" );
        Boolean lintEnableSimpleHtml = Whitebox.getInternalState( mojo, "parsedEnableSimpleHtml" );

        assertTrue( "lint skip parameter should be false", lintSkip );
        assertFalse( "lint failOnError parameter should be false", lintFailOnError );
        assertFalse( "lint ignoreWarning parameter should be false", lintFailOnError );
        assertFalse( "lint warnAll parameter should be false", lintFailOnError );
        assertFalse( "lint warningsAsErrors parameter should be false", lintFailOnError );
        assertNotNull( "lint config parameter should be false", lintConfig );
        File lintConfigFile = new File( project.getBuild().getDirectory(), "lint.xml" );
        assertEquals( "lint config parameter should point to lint.xml", lintConfig, lintConfigFile.getAbsolutePath() );

        assertTrue( "lint enableXml parameter should be true", lintEnableXml );
        assertFalse( "lint enableHtml parameter should be false", lintEnableHtml );
        assertFalse( "lint enableSimplHtml parameter should be false", lintEnableSimpleHtml );
    }

    // TODO add test where all default values are overridden

    // TODO test the mojo logic with a mock executor and check parameters

}
