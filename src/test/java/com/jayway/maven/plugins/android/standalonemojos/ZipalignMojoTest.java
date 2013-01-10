package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.common.AndroidExtension;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.easymock.Capture;
import org.easymock.EasyMock;
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
@RunWith( PowerMockRunner.class )
@PrepareForTest(
{ CommandExecutor.Factory.class, File.class, ZipalignMojo.class } )
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
    public void testDefaultConfig() throws Exception
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

    /**
     * Tests all parameters parsing
     *
     * Probably not needed since it is like testing maven itself
     *
     * @throws Exception
     */
    public void testConfigParse() throws Exception
    {
        ZipalignMojo mojo = createMojo( "zipalign-config-project1" );
        final ConfigHandler cfh = new ConfigHandler( mojo );
        cfh.parseConfiguration();

        Boolean skip = Whitebox.getInternalState( mojo, "parsedSkip" );
        assertFalse("zipalign 'skip' parameter should be false", skip);

        Boolean verbose = Whitebox.getInternalState( mojo, "parsedVerbose" );
        assertTrue("zipalign 'verbose' parameter should be true", verbose);

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
    public void testDefaultRun() throws Exception
    {
        ZipalignMojo mojo = createMojo( "zipalign-config-project3" );

        MavenProject project = Whitebox.getInternalState(mojo, "project");
        project.setPackaging(AndroidExtension.APK);

        MavenProjectHelper projectHelper = EasyMock.createNiceMock( MavenProjectHelper.class );
        Capture< File > capturedParameter = new Capture< File >();
        projectHelper.attachArtifact( EasyMock.eq( project ), EasyMock.eq( AndroidExtension.APK ),
                                            EasyMock.eq("aligned"), EasyMock.capture( capturedParameter ) );
        Whitebox.setInternalState( mojo, "projectHelper", projectHelper );

        final CommandExecutor mockExecutor = PowerMock.createMock(CommandExecutor.class);
        PowerMock.replace( CommandExecutor.Factory.class.getDeclaredMethod( "createDefaultCommmandExecutor" ) ).with(
                new InvocationHandler()
                {
                    @Override
                    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
                    {
                        return mockExecutor;
                    }
                } );

        Capture< List< String > > capturedFile = new Capture< List< String > >();
        mockExecutor.setLogger( EasyMock.anyObject( Log.class ) );
        mockExecutor.executeCommand( EasyMock.anyObject( String.class ), EasyMock.capture( capturedFile ) );

        EasyMock.replay( projectHelper );
        PowerMock.replay( mockExecutor );

        mojo.execute();

        PowerMock.verify( mockExecutor );
        List< String > parameters = capturedFile.getValue();
        List< String > parametersExpected = new ArrayList< String >();
        parametersExpected.add( "-v" );
        parametersExpected.add( "-f" );
        parametersExpected.add( "4" );
        parametersExpected.add( "app.apk" );
        File etalonFile = new File( project.getBasedir(), "app-updated.apk" );
        parametersExpected.add( etalonFile.getAbsolutePath() );
        assertEquals( "Zipalign arguments aren't as expected", parametersExpected, parameters );

        PowerMock.verify( projectHelper );
        assertEquals( "File should be same as expected", etalonFile, capturedParameter.getValue() );
    }

    /**
     * Tests run of zipalign with correct parameters
     *
     * @throws Exception
     */
    public void testRunWhenInputApkIsSameAsOutput() throws Exception
    {
        ZipalignMojo mojo = createMojo( "zipalign-config-project2" );

        MavenProject project = Whitebox.getInternalState(mojo, "project");
        project.setPackaging(AndroidExtension.APK);

        MavenProjectHelper projectHelper = EasyMock.createNiceMock( MavenProjectHelper.class );
        Whitebox.setInternalState( mojo, "projectHelper", projectHelper );

        final CommandExecutor mockExecutor = PowerMock.createMock(CommandExecutor.class);
        PowerMock.replace( CommandExecutor.Factory.class.getDeclaredMethod( "createDefaultCommmandExecutor" ) ).with(
                new InvocationHandler()
                {
                    @Override
                    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
                    {
                        return mockExecutor;
                    }
                } );

        Capture< List< String > > capturedFile = new Capture< List< String > >();
        mockExecutor.setLogger( EasyMock.anyObject( Log.class ) );
        mockExecutor.executeCommand( EasyMock.anyObject( String.class ), EasyMock.capture( capturedFile ) );

        EasyMock.replay( projectHelper );
        PowerMock.replay( mockExecutor );

        mojo.execute();

        File alignedFile = new File( project.getBasedir(), "app-aligned-temp.apk" );
        File originalFile = new File( project.getBasedir(), "app.apk" );
        PowerMock.verify( mockExecutor );
        List< String > parameters = capturedFile.getValue();
        List< String > parametersExpected = new ArrayList< String >();
        parametersExpected.add( "-v" );
        parametersExpected.add( "-f" );
        parametersExpected.add( "4" );
        parametersExpected.add( originalFile.getAbsolutePath() );
        parametersExpected.add( alignedFile.getAbsolutePath() );
        assertEquals( "Zipalign arguments aren't as expected", parametersExpected, parameters );

        PowerMock.verify(projectHelper);

        assertTrue("File should be replaced (created in test) after aligning", originalFile.exists());

        // remove created file
        originalFile.delete();
    }
}

