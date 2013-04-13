package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.configuration.Program;

/**
 * Test the monkeyrunner mojo. Tests options' default values and parsing. Tests the parameters passed to monkeyrunner.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * 
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest(
{ CommandExecutor.Factory.class, ConfigHandler.class } )
public class MonkeyRunnerMojoTest extends AbstractAndroidMojoTestCase< MonkeyRunnerMojo >
{
    @Override
    public String getPluginGoalName()
    {
        return "monkeyrunner";
    }

    /**
     * Tests all options, checks if their default values are correct.
     * 
     * @throws Exception
     */
    public void testDefaultMonkeyRunnerConfig() throws Exception
    {
        MonkeyRunnerMojo mojo = createMojo( "monkey-runner-config-project0" );
        final ConfigHandler cfh = new ConfigHandler( mojo );
        cfh.parseConfiguration();

        Boolean monkeyrunnerSkip = Whitebox.getInternalState( mojo, "parsedSkip" );

        assertTrue( "monkeyrunner skip parameter should be true", monkeyrunnerSkip );
    }

    /**
     * Tests all options, checks if their default values are correct.
     * 
     * @throws Exception
     */
    public void testDefaultUnskippedMonkeyRunnerConfig() throws Exception
    {
        MonkeyRunnerMojo mojo = createMojo( "monkey-runner-config-project1" );
        final ConfigHandler cfh = new ConfigHandler( mojo );
        cfh.parseConfiguration();

        Boolean monkeyrunnerSkip = Whitebox.getInternalState( mojo, "parsedSkip" );
        String[] monkeyrunnerPlugins = Whitebox.getInternalState( mojo, "parsedPlugins" );
        List< Program > monkeyrunnerPrograms = Whitebox.getInternalState( mojo, "parsedPrograms" );
        Boolean monkeyrunnerCreateReport = Whitebox.getInternalState( mojo, "parsedCreateReport" );

        assertFalse( "monkeyrunner skip parameter should be false", monkeyrunnerSkip );
        assertNull( "monkeyrunner plugins parameter should not contain plugins", monkeyrunnerPlugins );
        assertNull( "monkeyrunner programs parameter should not contain programs", monkeyrunnerPrograms );
        assertFalse( "monkeyrunner monkeyrunnerCreateReport parameter should be false", monkeyrunnerCreateReport );
    }

    /**
     * Tests all options, checks if they are parsed correctly.
     * 
     * @throws Exception
     */
    public void testCustomMonkeyRunnerConfig() throws Exception
    {
        MonkeyRunnerMojo mojo = createMojo( "monkey-runner-config-project2" );
        final ConfigHandler cfh = new ConfigHandler( mojo );
        cfh.parseConfiguration();

        Boolean monkeyrunnerSkip = Whitebox.getInternalState( mojo, "parsedSkip" );
        String[] monkeyrunnerPlugins = Whitebox.getInternalState( mojo, "parsedPlugins" );
        List< Program > monkeyrunnerPrograms = Whitebox.getInternalState( mojo, "parsedPrograms" );
        Boolean monkeyrunnerCreateReport = Whitebox.getInternalState( mojo, "parsedCreateReport" );

        assertFalse( "monkeyrunner skip parameter should be false", monkeyrunnerSkip );
        assertNotNull( "monkeyrunner plugins parameter should not contain plugins", monkeyrunnerPlugins );
        String[] expectedPlugins =
        { "foo" };
        assertTrue( Arrays.equals( expectedPlugins, monkeyrunnerPlugins ) );
        assertNotNull( "monkeyrunner programs parameter should not contain programs", monkeyrunnerPrograms );
        List< Program > expectedProgramList = new ArrayList< Program >();
        expectedProgramList.add( new Program( "foo", null ) );
        expectedProgramList.add( new Program( "bar", "qux" ) );
        expectedProgramList.removeAll( monkeyrunnerPrograms );
        assertEquals( 0, expectedProgramList.size() );
        assertTrue( "monkeyrunner monkeyrunnerCreateReport parameter should be false", monkeyrunnerCreateReport );
    }

    public void testAllMonkeyRunnerCommandParametersWithDefaultUnskippedConfig() throws Exception
    {
        MonkeyRunnerMojo mojo = createMojo( "monkey-runner-config-project1" );

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

        PowerMock.expectNew( ConfigHandler.class, mojo ).andReturn( EasyMock.createNiceMock( ConfigHandler.class ) );
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
                + FilenameUtils.separatorsToSystem( "/target/monkeyrunner-results/monkeyrunner-results.xml" ) );
        parametersExpected.add( "--sources" );
        parametersExpected.add( projectBaseDir.getAbsolutePath() + File.separator + "src" );
        parametersExpected.add( projectBaseDir.getAbsolutePath() );
        parametersExpected.add( "--exitcode" );
        assertEquals( parametersExpected, parameters );
    }

    public void testAllMonkeyRunnerCommandParametersWithCustomConfig() throws Exception
    {
        MonkeyRunnerMojo mojo = createMojo( "monkey-runner-config-project2" );

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

        PowerMock.expectNew( ConfigHandler.class, mojo ).andReturn( EasyMock.createNiceMock( ConfigHandler.class ) );
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
        parametersExpected.add( "monkeyrunner" );
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
        MonkeyRunnerMojo mojo = new MonkeyRunnerMojo();
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
