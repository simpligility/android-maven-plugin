package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.jayway.maven.plugins.android.AndroidSdk;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.SdkTestSupport;
import com.jayway.maven.plugins.android.config.ConfigHandler;

/**
 * Test the lint mojo. Tests options' default values and parsing. Tests the parameters passed to lint.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * 
 */
@Ignore("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest") 
@RunWith( PowerMockRunner.class )
@PrepareForTest(
{ CommandExecutor.Factory.class, ConfigHandler.class } )
public class LintMojoTest extends AbstractAndroidMojoTestCase< LintMojo >
{
    @Override
    public String getPluginGoalName()
    {
        return "lint";
    }

    /**
     * Tests all options, checks if their default values are correct.
     * 
     * @throws Exception
     */
    public void testDefaultLintConfig() throws Exception
    {
        LintMojo mojo = createMojo( "lint-config-project0" );
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
        cfh.parseConfiguration();

        Boolean lintSkip = Whitebox.getInternalState( mojo, "parsedSkip" );

        assertTrue( "lint skip parameter should be true", lintSkip );
    }

    /**
     * Tests all options, checks if their default values are correct.
     * 
     * @throws Exception
     */
    public void testDefaultUnskippedLintConfig() throws Exception
    {
        LintMojo mojo = createMojo( "lint-config-project1" );
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
        cfh.parseConfiguration();
        MavenProject project = Whitebox.getInternalState( mojo, "project" );

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

        Boolean lintEnableSources = Whitebox.getInternalState( mojo, "parsedEnableSources" );
        String lintSources = Whitebox.getInternalState( mojo, "parsedSources" );
        Boolean lintEnableClasspath = Whitebox.getInternalState( mojo, "parsedEnableClasspath" );
        String lintClasspath = Whitebox.getInternalState( mojo, "parsedClasspath" );
        Boolean lintEnableLibraries = Whitebox.getInternalState( mojo, "parsedEnableLibraries" );
        String lintLibraries = Whitebox.getInternalState( mojo, "parsedLibraries" );

        assertFalse( "lint skip parameter should be false", lintSkip );
        assertFalse( "lint failOnError parameter should be false", lintFailOnError );
        assertFalse( "lint ignoreWarning parameter should be false", lintIgnoreWarnings );
        assertFalse( "lint warnAll parameter should be false", lintWarnAll );
        assertFalse( "lint warningsAsErrors parameter should be false", lintWarningsAsErrors );
        assertEquals( "lint config parameter should be  null", "null", lintConfig );

        assertFalse( "lint fullPath parameter should be false", lintFullPath );
        assertTrue( "lint showAll parameter should be true", lintShowAll );
        assertFalse( "lint disableSourceLines parameter should be false", lintDisableSourceLines );
        assertEquals( "lint url parameter should be none", "none", lintUrl );

        assertTrue( "lint enableXml parameter should be true", lintEnableXml );
        File lintXmlOutputFile = new File( project.getBuild().getDirectory(), "lint-results/lint-results.xml" );
        assertEquals( "lint xmlOutputPath parameter should point to lint-results.xml", lintXmlOutputFile.getAbsolutePath(),
                lintXmlOutputPath );
        assertFalse( "lint enableHtml parameter should be false", lintEnableHtml );
        File lintHtmlOutputFile = new File( project.getBuild().getDirectory(), "lint-results/lint-results-html" );
        assertEquals( "lint htmlOutputPath parameter should point to lint-html", lintHtmlOutputFile.getAbsolutePath(),
                lintHtmlOutputPath );
        assertFalse( "lint enableSimplHtml parameter should be false", lintEnableSimpleHtml );
        File lintSimpleHtmlOutputFile = new File( project.getBuild().getDirectory(), "lint-results/lint-results-simple-html" );
        assertEquals( "lint simpleHtmlOutputPath parameter should point to lint-simple-html",
                lintSimpleHtmlOutputFile.getAbsolutePath(), lintSimpleHtmlOutputPath );

        assertTrue( "lint enableSources parameter should be true", lintEnableSources );
        assertEquals( "lint sources parameter should point to src/", project.getBuild().getSourceDirectory(),
                lintSources );
        assertFalse( "lint enableClasspath parameter should be false", lintEnableClasspath );
        assertEquals( "lint classpath parameter should point to target/classes", project.getBuild()
                .getOutputDirectory(), lintClasspath );
        assertFalse( "lint enableLibraries parameter should be false", lintEnableLibraries );
        assertNull( "lint libraries parameter should point not contain dependencies", lintLibraries );
    }

    /**
     * Tests all options, checks if they are parsed correctly.
     * 
     * @throws Exception
     */
    public void testCustomLintConfig() throws Exception
    {
        LintMojo mojo = createMojo( "lint-config-project2" );
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
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

        Boolean lintEnableSources = Whitebox.getInternalState( mojo, "parsedEnableSources" );
        String lintSources = Whitebox.getInternalState( mojo, "parsedSources" );
        Boolean lintEnableClasspath = Whitebox.getInternalState( mojo, "parsedEnableClasspath" );
        String lintClasspath = Whitebox.getInternalState( mojo, "parsedClasspath" );
        Boolean lintEnableLibraries = Whitebox.getInternalState( mojo, "parsedEnableLibraries" );
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

        assertFalse( "lint enableSources parameter should be false", lintEnableSources );
        assertTrue( "lint enableClasspath parameter should be true", lintEnableClasspath );
        assertTrue( "lint enableLibraries parameter should be true", lintEnableLibraries );

        assertEquals( "lint sources parameter should point to src2", "src2", lintSources );
        assertEquals( "lint classpath parameter should point to cla2", "cla2", lintClasspath );
        assertEquals( "lint libraries parameter should point to lib2", "lib2", lintLibraries );

    }

    public void testAllLintCommandParametersWithDefaultUnskippedConfig() throws Exception
    {
        LintMojo mojo = createMojo( "lint-config-project1" );

        MavenProject project = EasyMock.createNiceMock( MavenProject.class );
        Whitebox.setInternalState( mojo, "project", project );
        File projectBaseDir = new File( getBasedir() );
        Build projectBuild = new Build();
        projectBuild.setDirectory( "target/" );
        projectBuild.setSourceDirectory( "src/" );
        projectBuild.setOutputDirectory( "classes/" );
        EasyMock.expect( project.getBasedir() ).andReturn( projectBaseDir ).anyTimes();
        EasyMock.expect( project.getBuild() ).andReturn( projectBuild ).anyTimes();
        final CommandExecutor mockExecutor = PowerMock.createMock( CommandExecutor.class );
        PowerMock.replace( CommandExecutor.Factory.class.getDeclaredMethod( "createDefaultCommmandExecutor" ) ).with(
                new InvocationHandler()
                {

                    @Override
                    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
                    {
                        return mockExecutor;
                    }
                } );

        PowerMock.expectNew( ConfigHandler.class, mojo, this.session, this.execution )
            .andReturn( EasyMock.createNiceMock( ConfigHandler.class ) );
        Capture< List< String > > capturedArgument = new Capture< List< String > >();

        mockExecutor.setLogger( EasyMock.anyObject( Log.class ) );
        mockExecutor.executeCommand( EasyMock.anyObject( String.class ), EasyMock.capture( capturedArgument ),
                EasyMock.eq( false ) );
        PowerMock.replay( project );
        PowerMock.replay( mockExecutor );

        mojo.execute();

        PowerMock.verify( mockExecutor );
        List< String > parameters = capturedArgument.getValue();
        List< String > parametersExpected = new ArrayList< String >();
        parametersExpected.add( "--showall" );
        parametersExpected.add( "--xml" );
        parametersExpected.add( projectBaseDir.getAbsolutePath()
                + FilenameUtils.separatorsToSystem( "/target/lint-results/lint-results.xml" ) );
        parametersExpected.add( "--sources" );
        parametersExpected.add( projectBaseDir.getAbsolutePath() + File.separator + "src" );
        parametersExpected.add( projectBaseDir.getAbsolutePath() );
        parametersExpected.add( "--exitcode" );
        assertEquals( parametersExpected, parameters );
    }

    public void testAllLintCommandParametersWithCustomConfig() throws Exception
    {
        LintMojo mojo = createMojo( "lint-config-project2" );

        MavenProject project = EasyMock.createNiceMock( MavenProject.class );
        Whitebox.setInternalState( mojo, "project", project );
        File projectBaseDir = new File( "project/" );
        EasyMock.expect( project.getBasedir() ).andReturn( projectBaseDir );
        final CommandExecutor mockExecutor = PowerMock.createMock( CommandExecutor.class );
        PowerMock.replace( CommandExecutor.Factory.class.getDeclaredMethod( "createDefaultCommmandExecutor" ) )
            .with( new InvocationHandler()
                {

                    @Override
                    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
                    {
                        return mockExecutor;
                    }
                } );

        PowerMock.expectNew( ConfigHandler.class, mojo, this.session, this.execution )
            .andReturn( EasyMock.createNiceMock( ConfigHandler.class ) );
        Whitebox.setInternalState( mojo, "parsedSkip", Boolean.FALSE );
        Capture< List< String > > capturedArgument = new Capture< List< String > >();

        mockExecutor.setLogger( EasyMock.anyObject( Log.class ) );
        mockExecutor.executeCommand( EasyMock.anyObject( String.class ), EasyMock.capture( capturedArgument ),
                EasyMock.eq( false ) );
        PowerMock.replay( project );
        PowerMock.replay( mockExecutor );

        mojo.execute();

        PowerMock.verify( mockExecutor );
        List< String > parameters = capturedArgument.getValue();
        List< String > parametersExpected = new ArrayList< String >();
        parametersExpected.add( "-w" );
        parametersExpected.add( "-Wall" );
        parametersExpected.add( "-Werror" );
        parametersExpected.add( "--config" );
        parametersExpected.add( "lint" );
        parametersExpected.add( "--fullpath" );
        parametersExpected.add( "--nolines" );
        parametersExpected.add( "--html" );
        parametersExpected.add( "html" );
        parametersExpected.add( "--url" );
        parametersExpected.add( "url" );
        parametersExpected.add( "--simplehtml" );
        parametersExpected.add( "simple" );
        parametersExpected.add( "--classpath" );
        parametersExpected.add( "cla2" );
        parametersExpected.add( "--libraries" );
        parametersExpected.add( "lib2" );
        parametersExpected.add( projectBaseDir.getAbsolutePath() );
        parametersExpected.add( "--exitcode" );
        assertEquals( parametersExpected, parameters );
    }

    public void testAllParametersOffConfig() throws Exception
    {
        LintMojo mojo = new LintMojo() {
            @Override
            protected AndroidSdk getAndroidSdk() {
                return new SdkTestSupport().getSdk_with_platform_default();
            }
        };
        MavenProject project = EasyMock.createNiceMock( MavenProject.class );
        Whitebox.setInternalState( mojo, "project", project );
        File projectBaseDir = new File( "project/" );
        EasyMock.expect( project.getBasedir() ).andReturn( projectBaseDir );

        final CommandExecutor mockExecutor = PowerMock.createMock( CommandExecutor.class );
        PowerMock.replace( CommandExecutor.Factory.class.getDeclaredMethod( "createDefaultCommmandExecutor" ) ).with(
                new InvocationHandler()
                {

                    @Override
                    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
                    {
                        return mockExecutor;
                    }
                } );

        Capture< List< String > > capturedArgument = new Capture< List< String > >();

        mockExecutor.setLogger( EasyMock.anyObject( Log.class ) );
        mockExecutor.executeCommand( EasyMock.anyObject( String.class ), EasyMock.capture( capturedArgument ),
                EasyMock.eq( false ) );
        PowerMock.replay( mockExecutor );
        PowerMock.replay( project );
        Whitebox.setInternalState( mojo, "parsedConfig", "null" );
        Whitebox.setInternalState( mojo, "parsedClasspath", "null" );
        Whitebox.setInternalState( mojo, "parsedLibraries", "null" );
        Whitebox.invokeMethod( mojo, "executeWhenConfigured" );

        PowerMock.verify( mockExecutor );
        PowerMock.verify( project );
        List< String > parameters = capturedArgument.getValue();
        List< String > parametersExpected = new ArrayList< String >();
        parametersExpected.add( projectBaseDir.getAbsolutePath() );
        parametersExpected.add( "--exitcode" );
        assertEquals( parametersExpected, parameters );
    }
}
