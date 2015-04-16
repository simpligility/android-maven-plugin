package com.simpligility.maven.plugins.android.standalonemojos;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.simpligility.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.simpligility.maven.plugins.android.CommandExecutor;
import com.simpligility.maven.plugins.android.config.ConfigHandler;
import com.simpligility.maven.plugins.android.standalonemojos.MonkeyMojo;

/**
 * Test the Monkey mojo. Tests options' default values and parsing. We do not test the command line that is passed to
 * the adb bridge, it should be possible to mock it though.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * 
 */
@Ignore("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest") 
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
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
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
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
        cfh.parseConfiguration();

        // then
        Boolean monkeySkip = Whitebox.getInternalState( mojo, "parsedSkip" );
        Integer monkeyEventCount = Whitebox.getInternalState( mojo, "parsedEventCount" );
        Long monkeySeed = Whitebox.getInternalState( mojo, "parsedSeed" );
        Long monkeyThrottle = Whitebox.getInternalState( mojo, "parsedThrottle" );
        Integer monkeyPercentTouch = Whitebox.getInternalState( mojo, "parsedPercentTouch" );
        Integer monkeyPercentMotion = Whitebox.getInternalState( mojo, "parsedPercentMotion" );
        Integer monkeyPercentTrackball = Whitebox.getInternalState( mojo, "parsedPercentTrackball" );
        Integer monkeyPercentNav = Whitebox.getInternalState( mojo, "parsedPercentNav" );
        Integer monkeyPercentMajorNav = Whitebox.getInternalState( mojo, "parsedPercentMajorNav" );
        Integer monkeyPercentSyskeys = Whitebox.getInternalState( mojo, "parsedPercentSyskeys" );
        Integer monkeyPercentAppSwitch = Whitebox.getInternalState( mojo, "parsedPercentAppswitch" );
        Integer monkeyPercentAnyEvent = Whitebox.getInternalState( mojo, "parsedPercentAnyevent" );

        String[] monkeyPackages = Whitebox.getInternalState( mojo, "parsedPackages" );
        String[] monkeyCategories = Whitebox.getInternalState( mojo, "parsedCategories" );

        Boolean monkeyDebugNoEvents = Whitebox.getInternalState( mojo, "parsedDebugNoEvents" );
        Boolean monkeyHprof = Whitebox.getInternalState( mojo, "parsedHprof" );
        Boolean monkeyIgnoreCrashes = Whitebox.getInternalState( mojo, "parsedIgnoreCrashes" );
        Boolean monkeyIgnoreTimeouts = Whitebox.getInternalState( mojo, "parsedIgnoreTimeouts" );
        Boolean monkeyIgnoreSecurityExceptions = Whitebox.getInternalState( mojo, "parsedIgnoreSecurityExceptions" );
        Boolean monkeyKillProcessAfterError = Whitebox.getInternalState( mojo, "parsedKillProcessAfterError" );
        Boolean monkeyMonitorNativeCrashes = Whitebox.getInternalState( mojo, "parsedMonitorNativeCrashes" );
        Boolean monkeyCreateReport = Whitebox.getInternalState( mojo, "parsedCreateReport" );

        assertFalse( "Monkey skip parameter should be false", monkeySkip );
        final int expectedEventCount = 1000;
        assertEquals( "Monkey eventCount parameter should be 5000", new Integer( expectedEventCount ), //
                monkeyEventCount );
        assertNull( "Monkey seed should be null", monkeySeed );
        assertNull( "Monkey throttle should be null", monkeyThrottle );
        assertNull( "Monkey percentTouch should be null", monkeyPercentTouch );
        assertNull( "Monkey percentMotion should be null", monkeyPercentMotion );
        assertNull( "Monkey percentTrackball should be null", monkeyPercentTrackball );
        assertNull( "Monkey percentNav should be null", monkeyPercentNav );
        assertNull( "Monkey percentMajorNav should be null", monkeyPercentMajorNav );
        assertNull( "Monkey percentSyskeys should be null", monkeyPercentSyskeys );
        assertNull( "Monkey percentAppswitch should be null", monkeyPercentAppSwitch );
        assertNull( "Monkey percentAnyevent should be null", monkeyPercentAnyEvent );

        assertNull( "Monkey packages should be null", monkeyPackages );
        assertNull( "Monkey categories should be null", monkeyCategories );

        assertFalse( "Monkey debugNoEvents should be false", monkeyDebugNoEvents );
        assertFalse( "Monkey hprof should be false", monkeyHprof );
        assertFalse( "Monkey ignoreCrashes should be false", monkeyIgnoreCrashes );
        assertFalse( "Monkey ignoreTimeouts should be false", monkeyIgnoreTimeouts );
        assertFalse( "Monkey ignoreSecurityExceptions should be false", monkeyIgnoreSecurityExceptions );
        assertFalse( "Monkey killProcessAfterError should be false", monkeyKillProcessAfterError );
        assertFalse( "Monkey monitorNativeCrashes should be false", monkeyMonitorNativeCrashes );
        assertFalse( "Monkey createReport should be false", monkeyCreateReport );
    }

    /**
     * Tests all options, checks if they are parsed correctly.
     * 
     * @throws Exception
     */
    public void testCustomMonkeyConfig() throws Exception
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
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
        cfh.parseConfiguration();

        // then
        Boolean monkeySkip = Whitebox.getInternalState( mojo, "parsedSkip" );
        Integer monkeyEventCount = Whitebox.getInternalState( mojo, "parsedEventCount" );
        Long monkeySeed = Whitebox.getInternalState( mojo, "parsedSeed" );
        Long monkeyThrottle = Whitebox.getInternalState( mojo, "parsedThrottle" );
        Integer monkeyPercentTouch = Whitebox.getInternalState( mojo, "parsedPercentTouch" );
        Integer monkeyPercentMotion = Whitebox.getInternalState( mojo, "parsedPercentMotion" );
        Integer monkeyPercentTrackball = Whitebox.getInternalState( mojo, "parsedPercentTrackball" );
        Integer monkeyPercentNav = Whitebox.getInternalState( mojo, "parsedPercentNav" );
        Integer monkeyPercentMajorNav = Whitebox.getInternalState( mojo, "parsedPercentMajorNav" );
        Integer monkeyPercentSyskeys = Whitebox.getInternalState( mojo, "parsedPercentSyskeys" );
        Integer monkeyPercentAppSwitch = Whitebox.getInternalState( mojo, "parsedPercentAppswitch" );
        Integer monkeyPercentAnyEvent = Whitebox.getInternalState( mojo, "parsedPercentAnyevent" );

        String[] monkeyPackages = Whitebox.getInternalState( mojo, "parsedPackages" );
        String[] monkeyCategories = Whitebox.getInternalState( mojo, "parsedCategories" );

        Boolean monkeyDebugNoEvents = Whitebox.getInternalState( mojo, "parsedDebugNoEvents" );
        Boolean monkeyHprof = Whitebox.getInternalState( mojo, "parsedHprof" );
        Boolean monkeyIgnoreCrashes = Whitebox.getInternalState( mojo, "parsedIgnoreCrashes" );
        Boolean monkeyIgnoreTimeouts = Whitebox.getInternalState( mojo, "parsedIgnoreTimeouts" );
        Boolean monkeyIgnoreSecurityExceptions = Whitebox.getInternalState( mojo, "parsedIgnoreSecurityExceptions" );
        Boolean monkeyKillProcessAfterError = Whitebox.getInternalState( mojo, "parsedKillProcessAfterError" );
        Boolean monkeyMonitorNativeCrashes = Whitebox.getInternalState( mojo, "parsedMonitorNativeCrashes" );
        Boolean monkeyCreateReport = Whitebox.getInternalState( mojo, "parsedCreateReport" );

        assertFalse( "Monkey skip parameter should be false", monkeySkip );
        final int expectedEventCount = 5000;
        assertEquals( "Monkey eventCount parameter should be 5000", new Integer( expectedEventCount ), //
                monkeyEventCount );
        final int expectedSeed = 123456;
        assertEquals( "Monkey seed should be 123456", new Long( expectedSeed ), monkeySeed );
        assertEquals( "Monkey throttle should be 10", new Long( 10 ), monkeyThrottle );
        assertEquals( "Monkey percentTouch should be 10", new Integer( 10 ), monkeyPercentTouch );
        assertEquals( "Monkey percentMotion should be 10", new Integer( 10 ), monkeyPercentMotion );
        assertEquals( "Monkey percentTrackball should be 10", new Integer( 10 ), monkeyPercentTrackball );
        assertEquals( "Monkey percentNav should be 10", new Integer( 10 ), monkeyPercentNav );
        assertEquals( "Monkey percentMajorNav should be 10", new Integer( 10 ), monkeyPercentMajorNav );
        assertEquals( "Monkey percentSyskeys should be 10", new Integer( 10 ), monkeyPercentSyskeys );
        assertEquals( "Monkey percentAppswitch should be 10", new Integer( 10 ), monkeyPercentAppSwitch );
        assertEquals( "Monkey percentAnyevent should be 10", new Integer( 10 ), monkeyPercentAnyEvent );

        String[] expectedPackages = new String[]
        { "com.foo", "com.bar" };
        assertTrue( "Monkey packages should be [com.foo,com.bar]", Arrays.equals( expectedPackages, monkeyPackages ) );
        String[] expectedCategories = new String[]
        { "foo", "bar" };
        assertTrue( "Monkey categories should be [foo,bar]", Arrays.equals( expectedCategories, monkeyCategories ) );

        assertTrue( "Monkey debugNoEvents should be true", monkeyDebugNoEvents );
        assertTrue( "Monkey hprof should be true", monkeyHprof );
        assertTrue( "Monkey ignoreCrashes should be true", monkeyIgnoreCrashes );
        assertTrue( "Monkey ignoreTimeouts should be true", monkeyIgnoreTimeouts );
        assertTrue( "Monkey ignoreSecurityExceptions should be true", monkeyIgnoreSecurityExceptions );
        assertTrue( "Monkey killProcessAfterError should be true", monkeyKillProcessAfterError );
        assertTrue( "Monkey monitorNativeCrashes should be true", monkeyMonitorNativeCrashes );
        assertTrue( "Monkey createReport should be true", monkeyCreateReport );

    }

}
