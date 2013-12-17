package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.util.Arrays;

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
 * Test the UIAutomator mojo. Tests options' default values and parsing. We do not test the command line that is passed
 * to the adb bridge, it should be possible to mock it though.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * 
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest(
{ CommandExecutor.Factory.class, ConfigHandler.class } )
public class UIAutomatorMojoTest extends AbstractAndroidMojoTestCase< UIAutomatorMojo >
{
    @Override
    public String getPluginGoalName()
    {
        return "uiautomator";
    }

    /**
     * Tests all options, checks if their default values are correct.
     * 
     * @throws Exception
     */
    public void testDefaultUIAutomatorConfig() throws Exception
    {
        // given
        UIAutomatorMojo mojo = createMojo( "ui-automator-config-project0" );
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
        cfh.parseConfiguration();

        // when
        Boolean automatorSkip = Whitebox.getInternalState( mojo, "parsedSkip" );

        // then
        assertTrue( "UIAutomator skip parameter should be true", automatorSkip );
    }

    /**
     * Tests all options, checks if their default values are correct.
     * 
     * @throws Exception
     */
    public void testDefaultUnskippedUIAutomatorConfig() throws Exception
    {
        // given
        UIAutomatorMojo mojo = createMojo( "ui-automator-config-project1" );

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
        Boolean automatorSkip = Whitebox.getInternalState( mojo, "parsedSkip" );
        Boolean automatorDebug = Whitebox.getInternalState( mojo, "parsedDebug" );
        String automatorJarFile = Whitebox.getInternalState( mojo, "parsedJarFile" );
        String[] automatorTestClassOrMethods = Whitebox.getInternalState( mojo, "parsedTestClassOrMethods" );
        Boolean automatorTakeScreenshotOnFailure = Whitebox.getInternalState( mojo, "parsedTakeScreenshotOnFailure" );
        String automatorScreenshotsPathOnDevice = Whitebox.getInternalState( mojo, "parsedScreenshotsPathOnDevice" );
		Boolean automatorCreateReport = Whitebox.getInternalState( mojo, "parsedCreateReport" );
		String automatorReportSuffix = Whitebox.getInternalState( mojo, "parsedReportSuffix" );
		String automatorPropertiesKeyPrefix = Whitebox.getInternalState( mojo, "parsedPropertiesKeyPrefix" );

        assertFalse( "UIAutomator skip parameter should be false", automatorSkip );
        assertFalse( "UIAutomator debug parameter should be false", automatorDebug );
        String expectedJarFile = buildName + ".jar";
        assertNotNull( "UIAutomator jarFile parameter should be not null", automatorJarFile );
        assertEquals( "UIAutomator jarFile parameter should be equal to artifact name", expectedJarFile,
                automatorJarFile );
        assertNull( "UIAutomator testClassOrMethods parameter should be null", automatorTestClassOrMethods );
        assertFalse( "UIAutomator takeScreenshotOnFailure parameter should be equal false",
                automatorTakeScreenshotOnFailure );
        assertEquals( "UIAutomator screenshotsPath on device be equal /sdcard/uiautomator-screenshots/",
                "/sdcard/uiautomator-screenshots/", automatorScreenshotsPathOnDevice );
		assertFalse( "UIAutomator createReport parameter should be false", automatorCreateReport );
		assertNull( "UIAutomator reportSuffix parameter should be null", automatorReportSuffix );
		assertNull( "UIAutomator propertiesKeyPrefix parameter should be null", automatorPropertiesKeyPrefix );
    }

    /**
     * Tests all options, checks if they are parsed correctly.
     * 
     * @throws Exception
     */
    public void testCustomUIAutomatorConfig() throws Exception
    {
        // given
        UIAutomatorMojo mojo = createMojo( "ui-automator-config-project2" );
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
        Boolean automatorSkip = Whitebox.getInternalState( mojo, "parsedSkip" );
        Boolean automatorDebug = Whitebox.getInternalState( mojo, "parsedDebug" );
        String automatorJarFile = Whitebox.getInternalState( mojo, "parsedJarFile" );
        String[] automatorTestClassOrMethods = Whitebox.getInternalState( mojo, "parsedTestClassOrMethods" );
        Boolean automatorTakeScreenshotOnFailure = Whitebox.getInternalState( mojo, "parsedTakeScreenshotOnFailure" );
        String automatorScreenshotsPathOnDevice = Whitebox.getInternalState( mojo, "parsedScreenshotsPathOnDevice" );
		Boolean automatorCreateReport = Whitebox.getInternalState( mojo, "parsedCreateReport" );
		String automatorReportSuffix = Whitebox.getInternalState( mojo, "parsedReportSuffix" );
		String automatorPropertiesKeyPrefix = Whitebox.getInternalState( mojo, "parsedPropertiesKeyPrefix" );

        assertFalse( "UIAutomator skip parameter should be false", automatorSkip );
        assertFalse( "UIAutomator debug parameter should be false", automatorDebug );
        assertNotNull( "UIAutomator jarFile parameter should be not null", automatorJarFile );
        String expectedJarFile = buildName + ".jar";
        assertEquals( "UIAutomator jarFile parameter should be equal to artifact name", expectedJarFile,
                automatorJarFile );

        assertNotNull( "UIAutomator jarFile parameter should be not null", automatorTestClassOrMethods );
        String[] expectedTestClassOrMethods = new String[]
        { "a", "b#c" };
        assertTrue( "UIAutomator testClassOrMethods parameter should be equal [a,b#c]",
                Arrays.equals( expectedTestClassOrMethods, automatorTestClassOrMethods ) );
        assertTrue( "UIAutomator takeScreenshotOnFailure parameter should be equal true",
                automatorTakeScreenshotOnFailure );
        assertEquals( "UIAutomator screenshotsPath on device be equal /mnt/sdcard/screenshots/",
                "/mnt/sdcard/screenshots/", automatorScreenshotsPathOnDevice );
		assertTrue( "UIAutomator createReport parameter should be true", automatorCreateReport );
		String expectedReportSuffix = "-mySpecialReport";
		assertEquals( "UIAutomator reportSuffix parameter should be equal to "+expectedReportSuffix,
				expectedReportSuffix, automatorReportSuffix );
		String expectedPropertiesKeyPrefix = "UIA";
		assertEquals( "UIAutomator propertiesKeyPrefix should be equal to " + expectedPropertiesKeyPrefix,
				expectedPropertiesKeyPrefix, automatorPropertiesKeyPrefix);
    }

}
