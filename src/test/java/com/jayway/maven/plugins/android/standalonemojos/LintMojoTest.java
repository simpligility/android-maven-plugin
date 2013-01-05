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
        Boolean lintFullPath = Whitebox.getInternalState( mojo, "parsedFullPath" );
        Boolean lintShowAll = Whitebox.getInternalState( mojo, "parsedShowAll" );
        Boolean lintDisableSourceLines = Whitebox.getInternalState( mojo, "parsedDisableSourceLines" );
        String lintUrl = Whitebox.getInternalState( mojo, "parsedUrl" );

        Boolean lintEnableXml = Whitebox.getInternalState( mojo, "parsedEnableXml" );
        String lintXmlOutputPath = Whitebox.getInternalState( mojo, "parsedXmlOutputPath" );
        Boolean lintEnableHtml = Whitebox.getInternalState( mojo, "parsedEnableHtml" );
        String lintHtmlOutputPath = Whitebox.getInternalState( mojo, "parsedHtmlOutputPath" );
        Boolean lintEnableSimpleHtml = Whitebox.getInternalState( mojo, "parsedEnableSimpleHtml" );
        String lintSimpleHtmlOutputPath = Whitebox.getInternalState( mojo, "parsedSimpleHtmlOutputPath" );

        String lintSources = Whitebox.getInternalState( mojo, "parsedSources" );
        String lintClasspath = Whitebox.getInternalState( mojo, "parsedClasspath" );
        String lintLibraries = Whitebox.getInternalState( mojo, "parsedLibraries" );

        assertTrue( "lint skip parameter should be false", lintSkip );
        assertFalse( "lint failOnError parameter should be false", lintFailOnError );
        assertFalse( "lint ignoreWarning parameter should be false", lintIgnoreWarnings );
        assertFalse( "lint warnAll parameter should be false", lintWarnAll );
        assertFalse( "lint warningsAsErrors parameter should be false", lintWarningsAsErrors );
        assertNotNull( "lint config parameter should be false", lintConfig );
        File lintConfigFile = new File( project.getBuild().getDirectory(), "lint.xml" );
        assertEquals( "lint config parameter should point to lint.xml", lintConfig, lintConfigFile.getAbsolutePath() );

        assertFalse( "lint fullPath parameter should be false", lintFullPath );
        assertTrue( "lint showAll parameter should be false", lintShowAll );
        assertFalse( "lint disableSourceLines parameter should be false", lintDisableSourceLines );
        assertEquals( "lint url parameter should be none", "none", lintUrl );

        assertTrue( "lint enableXml parameter should be true", lintEnableXml );
        File lintXmlOutputFile = new File( project.getBuild().getDirectory(), "lint.xml" );
        assertEquals( "lint xmlOutputPath parameter should point to lint.xml", lintXmlOutputFile.getAbsolutePath(),
                lintXmlOutputPath );
        assertFalse( "lint enableHtml parameter should be false", lintEnableHtml );
        File lintHtmlOutputFile = new File( project.getBuild().getDirectory(), "lint-html" );
        assertEquals( "lint htmlOutputPath parameter should point to lint-html", lintHtmlOutputFile.getAbsolutePath(),
                lintHtmlOutputPath );
        assertFalse( "lint enableSimplHtml parameter should be false", lintEnableSimpleHtml );
        File lintSimpleHtmlOutputFile = new File( project.getBuild().getDirectory(), "lint-simple-html" );
        assertEquals( "lint simpleHtmlOutputPath parameter should point to lint-simple-html",
                lintSimpleHtmlOutputFile.getAbsolutePath(), lintSimpleHtmlOutputPath );

        assertEquals( "lint sources parameter should point to src/", project.getBuild().getSourceDirectory(),
                lintSources );
        assertEquals( "lint classpath parameter should point to target/classes", project.getBuild()
                .getOutputDirectory(), lintClasspath );
        assertNull( "lint libraries parameter should point not contain dependencies", lintLibraries );

    }

    public void testCustomLintConfig() throws Exception
    {
        LintMojo mojo = createMojo( "lint-config-project2" );
        final ConfigHandler cfh = new ConfigHandler( mojo );
        cfh.parseConfiguration();

        Boolean lintSkip = Whitebox.getInternalState( mojo, "parsedSkip" );
        Boolean lintFailOnError = Whitebox.getInternalState( mojo, "parsedFailOnError" );
        Boolean lintIgnoreWarnings = Whitebox.getInternalState( mojo, "parsedIgnoreWarnings" );
        Boolean lintWarnAll = Whitebox.getInternalState( mojo, "parsedWarnAll" );
        Boolean lintWarningsAsErrors = Whitebox.getInternalState( mojo, "parsedWarningsAsErrors" );
        String lintConfig = Whitebox.getInternalState( mojo, "parsedConfig" );

        Boolean lintFullPath = Whitebox.getInternalState( mojo, "parsedFullPath" );
        Boolean lintShowAll = Whitebox.getInternalState( mojo, "parsedShowAll" );
        Boolean lintDisableSourceLines = Whitebox.getInternalState( mojo, "parsedDisableSourceLines" );
        String lintUrl = Whitebox.getInternalState( mojo, "parsedUrl" );

        Boolean lintEnableXml = Whitebox.getInternalState( mojo, "parsedEnableXml" );
        String lintXmlOutputPath = Whitebox.getInternalState( mojo, "parsedXmlOutputPath" );
        Boolean lintEnableHtml = Whitebox.getInternalState( mojo, "parsedEnableHtml" );
        String lintHtmlOutputPath = Whitebox.getInternalState( mojo, "parsedHtmlOutputPath" );
        Boolean lintEnableSimpleHtml = Whitebox.getInternalState( mojo, "parsedEnableSimpleHtml" );
        String lintSimpleHtmlOutputPath = Whitebox.getInternalState( mojo, "parsedSimpleHtmlOutputPath" );

        String lintSources = Whitebox.getInternalState( mojo, "parsedSources" );
        String lintClasspath = Whitebox.getInternalState( mojo, "parsedClasspath" );
        String lintLibraries = Whitebox.getInternalState( mojo, "parsedLibraries" );

        assertFalse( "lint skip parameter should be false", lintSkip );
        assertTrue( "lint failOnError parameter should be true", lintFailOnError );
        assertTrue( "lint ignoreWarning parameter should be true", lintIgnoreWarnings );
        assertTrue( "lint warnAll parameter should be true", lintWarnAll );
        assertTrue( "lint warningsAsErrors parameter should be true", lintWarningsAsErrors );
        assertNotNull( "lint config parameter should be non null", lintConfig );
        assertEquals( "lint config parameter should point to lint", "lint", lintConfig );

        assertTrue( "lint fullPath parameter should be true", lintFullPath );
        assertFalse( "lint showAll parameter should be false", lintShowAll );
        assertTrue( "lint disableSourceLines parameter should be true", lintDisableSourceLines );
        assertEquals( "lint url parameter should be url", "url", lintUrl );

        assertFalse( "lint enableXml parameter should be false", lintEnableXml );
        assertEquals( "lint xmlOutputPath parameter should point to xml", "xml", lintXmlOutputPath );
        assertTrue( "lint enableHtml parameter should be true", lintEnableHtml );
        assertEquals( "lint htmlOutputPath parameter should point to html", "html", lintHtmlOutputPath );
        assertTrue( "lint enableSimplHtml parameter should be true", lintEnableSimpleHtml );
        assertEquals( "lint simpleHtmlOutputPath parameter should point to simple", "simple", lintSimpleHtmlOutputPath );

        assertEquals( "lint sources parameter should point to src2", "src2", lintSources );
        assertEquals( "lint classpath parameter should point to cla2", "cla2", lintClasspath );
        assertEquals( "lint libraries parameter should point to lib2", "lib2", lintLibraries );

    }
    // TODO test the mojo logic with a mock executor and check parameters

}
