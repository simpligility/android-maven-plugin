package com.jayway.maven.plugins.android.standalonemojos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.runner.RunWith;
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
@Ignore("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest") 
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
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
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
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
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
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
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
        assertEquals( expectedProgramList, monkeyrunnerPrograms );
        assertTrue( "monkeyrunner monkeyrunnerCreateReport parameter should be false", monkeyrunnerCreateReport );
    }

    /**
     * I don't understand why getAndroidSdk fails here when it runs fine in LintMojo Test public void
     * testAllMonkeyRunnerCommandParametersWithCustomConfig() throws Exception { MonkeyRunnerMojo mojo = createMojo(
     * "monkey-runner-config-project2" );
     * 
     * MavenProject project = EasyMock.createNiceMock( MavenProject.class ); Whitebox.setInternalState( mojo, "project",
     * project ); File projectBaseDir = new File( "project/" ); EasyMock.expect( project.getBasedir() ).andReturn(
     * projectBaseDir ); final CommandExecutor mockExecutor = PowerMock.createMock( CommandExecutor.class );
     * PowerMock.replace( CommandExecutor.Factory.class.getDeclaredMethod( "createDefaultCommmandExecutor" ) ).with( new
     * InvocationHandler() {
     * 
     * @Override public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable { return
     *           mockExecutor; } } );
     * 
     *           Capture< List< String > > capturedArgument = new Capture< List< String > >();
     * 
     *           mockExecutor.setLogger( EasyMock.anyObject( Log.class ) ); mockExecutor.executeCommand(
     *           EasyMock.anyObject( String.class ), EasyMock.capture( capturedArgument ), EasyMock.eq( false ) );
     *           mockExecutor.setCustomShell( EasyMock.anyObject( Shell.class ) );
     * 
     *           IDevice mockDevice = EasyMock.createMock( IDevice.class );
     * 
     *           PowerMock.replay( project ); PowerMock.replay( mockExecutor ); PowerMock.replay( mockDevice );
     * 
     *           mojo.run( mockDevice );
     * 
     *           PowerMock.verify( mockExecutor ); List< String > parameters = capturedArgument.getValue(); List< String
     *           > parametersExpected = new ArrayList< String >(); parametersExpected.add( "-plugin foo" );
     *           parametersExpected.add( "foo" ); assertEquals( parametersExpected, parameters ); }
     */

}
