package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.easymock.EasyMock;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.config.ConfigHandler;

/**
 * Test the Monkey mojo. Tests options' default values and parsing. We do not test the command line that is passed to
 * the adb bridge, it should be possible to mock it though.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * 
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest(
{ CommandExecutor.Factory.class, ConfigHandler.class } )
public class MonkeyMojoTest extends AbstractAndroidMojoTestCase< MonkeyMojo >
{
    @Override
    public String getPluginGoalName()
    {
        return "monkey";
    }

    /**
     * Tests all options, checks if their default values are correct.
     * 
     * @throws Exception
     */
    public void testDefaultMonkeyConfig() throws Exception
    {
        // given
        MonkeyMojo mojo = createMojo( "monkey-config-project0" );
        final ConfigHandler cfh = new ConfigHandler( mojo );
        cfh.parseConfiguration();

        // when
        Boolean automatorSkip = Whitebox.getInternalState( mojo, "parsedSkip" );

        // then
        assertTrue( "Monkey skip parameter should be true", automatorSkip );
    }

    /**
     * Tests all options, checks if their default values are correct.
     * 
     * @throws Exception
     */
    public void testDefaultUnskippedMonkeyConfig() throws Exception
    {
        // given
        MonkeyMojo mojo = createMojo( "monkey-config-project1" );

        MavenProject project = EasyMock.createNiceMock( MavenProject.class );
        Whitebox.setInternalState( mojo, "project", project );
        File projectBaseDir = new File( getBasedir() );
        Build projectBuild = new Build();
        String buildName = "monkey-config-project1-15.4.3.1011";
        projectBuild.setFinalName( buildName );
        projectBuild.setDirectory( "target/" );
        projectBuild.setSourceDirectory( "src/" );
        projectBuild.setOutputDirectory( "classes/" );
        EasyMock.expect( project.getBasedir() ).andReturn( projectBaseDir ).anyTimes();
        EasyMock.expect( project.getBuild() ).andReturn( projectBuild ).anyTimes();
        PowerMock.replay( project );

        // when
        final ConfigHandler cfh = new ConfigHandler( mojo );
        cfh.parseConfiguration();

        // then
        Boolean automatorSkip = Whitebox.getInternalState( mojo, "parsedSkip" );
        Long automatorEventCount = Whitebox.getInternalState( mojo, "parsedEventCount" );

        assertFalse( "Monkey skip parameter should be false", automatorSkip );
        assertEquals( "Monkey eventCount parameter should be 1000", new Long( 1000 ), automatorEventCount );
    }

    /**
     * Tests all options, checks if they are parsed correctly.
     * 
     * @throws Exception
     */
    public void testCustomUIAutomatorConfig() throws Exception
    {
        // given
        MonkeyMojo mojo = createMojo( "monkey-config-project2" );
        MavenProject project = EasyMock.createNiceMock( MavenProject.class );
        Whitebox.setInternalState( mojo, "project", project );
        File projectBaseDir = new File( getBasedir() );
        Build projectBuild = new Build();
        String buildName = "ui-automator-config-project1-15.4.3.1011";
        projectBuild.setFinalName( buildName );
        projectBuild.setDirectory( "target/" );
        projectBuild.setSourceDirectory( "src/" );
        projectBuild.setOutputDirectory( "classes/" );
        EasyMock.expect( project.getBasedir() ).andReturn( projectBaseDir ).anyTimes();
        EasyMock.expect( project.getBuild() ).andReturn( projectBuild ).anyTimes();

        PowerMock.replay( project );

        // when
        final ConfigHandler cfh = new ConfigHandler( mojo );
        cfh.parseConfiguration();

        // then
        // then
        Boolean automatorSkip = Whitebox.getInternalState( mojo, "parsedSkip" );
        Long automatorEventCount = Whitebox.getInternalState( mojo, "parsedEventCount" );

        assertFalse( "Monkey skip parameter should be false", automatorSkip );
        final int expectedEventCount = 5000;
        assertEquals( "Monkey eventCount parameter should be 5000", new Long( expectedEventCount ), //
                automatorEventCount );
    }

}
