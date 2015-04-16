package com.simpligility.maven.plugins.android.standalonemojos;

import com.simpligility.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.simpligility.maven.plugins.android.CommandExecutor;
import com.simpligility.maven.plugins.android.common.AndroidExtension;
import com.simpligility.maven.plugins.android.config.ConfigHandler;
import com.simpligility.maven.plugins.android.standalonemojos.ZipalignMojo;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Eugen
 */
@Ignore("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest") 
@RunWith ( PowerMockRunner.class )
@PrepareForTest (
        { CommandExecutor.Factory.class, FileUtils.class, ZipalignMojo.class } )
public class ZipalignMojoTest extends AbstractAndroidMojoTestCase<ZipalignMojo>
{
    @Override
    public String getPluginGoalName ()
    {
        return "zipalign";
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception
     */
    public void testDefaultConfig () throws Exception
    {
        ZipalignMojo mojo = createMojo( "zipalign-config-project0" );
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
        cfh.parseConfiguration();

        Boolean skip = Whitebox.getInternalState( mojo, "parsedSkip" );
        assertTrue( "zipalign 'skip' parameter should be true", skip );

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

    /**
     * Tests all parameters parsing
     * <p/>
     * Probably not needed since it is like testing maven itself
     *
     * @throws Exception
     */
    public void testConfigParse () throws Exception
    {
        ZipalignMojo mojo = createMojo( "zipalign-config-project1" );
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );
        cfh.parseConfiguration();

        Boolean skip = Whitebox.getInternalState( mojo, "parsedSkip" );
        assertFalse( "zipalign 'skip' parameter should be false", skip );

        Boolean verbose = Whitebox.getInternalState( mojo, "parsedVerbose" );
        assertTrue( "zipalign 'verbose' parameter should be true", verbose );

        String inputApk = Whitebox.getInternalState( mojo, "parsedInputApk" );
        assertEquals( "zipalign 'inputApk' parameter should be equal", "app.apk", inputApk );

        String outputApk = Whitebox.getInternalState( mojo, "parsedOutputApk" );
        assertEquals( "zipalign 'outputApk' parameter should be equal", "app-updated.apk", outputApk );
    }

    /**
     * Tests run of zipalign with correct parameters as well adding aligned file to artifacts
     *
     * @throws Exception
     */
    public void testDefaultRun () throws Exception
    {
        ZipalignMojo mojo = createMojo( "zipalign-config-project3" );

        MavenProject project = Whitebox.getInternalState( mojo, "project" );
        project.setPackaging( AndroidExtension.APK );

        MavenProjectHelper projectHelper = EasyMock.createNiceMock( MavenProjectHelper.class );
        Capture<File> capturedParameter = new Capture<File>();
        projectHelper.attachArtifact( EasyMock.eq( project ), EasyMock.eq( AndroidExtension.APK ),
                EasyMock.eq( "aligned" ), EasyMock.capture( capturedParameter ) );
        Whitebox.setInternalState( mojo, "projectHelper", projectHelper );

        final CommandExecutor mockExecutor = PowerMock.createMock( CommandExecutor.class );
        PowerMock.replace( CommandExecutor.Factory.class.getDeclaredMethod( "createDefaultCommmandExecutor" ) ).with(
                new InvocationHandler()
                {
                    @Override
                    public Object invoke ( Object proxy, Method method, Object[] args ) throws Throwable
                    {
                        return mockExecutor;
                    }
                } );

        Capture<List<String>> capturedFile = new Capture<List<String>>();
        mockExecutor.setLogger( EasyMock.anyObject( Log.class ) );
        mockExecutor.setCaptureStdOut( EasyMock.anyBoolean() );
        mockExecutor.executeCommand( EasyMock.anyObject( String.class ), EasyMock.capture( capturedFile ) );

        PowerMock.mockStatic( FileUtils.class );
        EasyMock.expect( FileUtils.fileExists( "app-updated.apk" ) ).andReturn( true );

        EasyMock.replay( projectHelper );
        PowerMock.replay( mockExecutor );
        PowerMock.replay( FileUtils.class );

        mojo.execute();

        PowerMock.verify( mockExecutor );
        List<String> parameters = capturedFile.getValue();
        List<String> parametersExpected = new ArrayList<String>();
        parametersExpected.add( "-v" );
        parametersExpected.add( "-f" );
        parametersExpected.add( "4" );
        parametersExpected.add( "app.apk" );
        parametersExpected.add( "app-updated.apk" );
        assertEquals( "Zipalign arguments aren't as expected", parametersExpected, parameters );

        PowerMock.verify( projectHelper );
        assertEquals( "File should be same as expected", new File( "app-updated.apk" ), capturedParameter.getValue() );

        // verify that all method were invoked
        PowerMock.verify( FileUtils.class );
    }

    /**
     * Tests run of zipalign with correct parameters
     *
     * @throws Exception
     */
    public void testRunWhenInputApkIsSameAsOutput () throws Exception
    {
        ZipalignMojo mojo = createMojo( "zipalign-config-project2" );

        MavenProject project = Whitebox.getInternalState( mojo, "project" );
        project.setPackaging( AndroidExtension.APK );

        MavenProjectHelper projectHelper = EasyMock.createNiceMock( MavenProjectHelper.class );
        Whitebox.setInternalState( mojo, "projectHelper", projectHelper );

        final CommandExecutor mockExecutor = PowerMock.createMock( CommandExecutor.class );
        PowerMock.replace( CommandExecutor.Factory.class.getDeclaredMethod( "createDefaultCommmandExecutor" ) ).with(
                new InvocationHandler()
                {
                    @Override
                    public Object invoke ( Object proxy, Method method, Object[] args ) throws Throwable
                    {
                        return mockExecutor;
                    }
                } );

        Capture<List<String>> capturedFile = new Capture<List<String>>();
        mockExecutor.setLogger( EasyMock.anyObject( Log.class ) );
        mockExecutor.setCaptureStdOut( EasyMock.anyBoolean() );
        mockExecutor.executeCommand( EasyMock.anyObject( String.class ), EasyMock.capture( capturedFile ) );

        PowerMock.mockStatic( FileUtils.class );
        EasyMock.expect( FileUtils.fileExists( "app-aligned-temp.apk" ) ).andReturn( true );
        FileUtils.rename( new File( "app-aligned-temp.apk" ) , new File( "app.apk" ) );
        EasyMock.expectLastCall();

        PowerMock.replay( projectHelper );
        PowerMock.replay( mockExecutor );
        PowerMock.replay( FileUtils.class );

        mojo.execute();

        PowerMock.verify( mockExecutor );
        List<String> parameters = capturedFile.getValue();
        List<String> parametersExpected = new ArrayList<String>();
        parametersExpected.add( "-v" );
        parametersExpected.add( "-f" );
        parametersExpected.add( "4" );
        parametersExpected.add( "app.apk" );
        parametersExpected.add( "app-aligned-temp.apk" );
        assertEquals( "Zipalign arguments aren't as expected", parametersExpected, parameters );

        // no invocations to attach artifact
        PowerMock.verify( projectHelper );

        // verify that all method were invoked
        PowerMock.verify( FileUtils.class );
    }
}

