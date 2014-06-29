package com.jayway.maven.plugins.android;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

@RunWith( PowerMockRunner.class )
@PrepareForTest(
{ AndroidDebugBridge.class, CommandExecutor.Factory.class } )
public class AbstractEmulatorMojoTest
{
    private static final String AVD_NAME = "emulator";
    private static final long DEFAULT_TIMEOUT = 500;
    private AbstractEmulatorMojoToTest abstractEmulatorMojo;
    private CommandExecutor mockExecutor;
    private AndroidDebugBridge mockAndroidDebugBridge;

    @Before
    public void setUp() throws Exception
    {
        mockExecutor = PowerMock.createNiceMock( CommandExecutor.class );
        mockExecutor.executeCommand( anyObject( String.class ), isNull( List.class ) );
        PowerMock.replay( mockExecutor );

        mockStatic( CommandExecutor.Factory.class );
        expect( CommandExecutor.Factory.createDefaultCommmandExecutor() ).andReturn( mockExecutor );
        PowerMock.replay( CommandExecutor.Factory.class );

        mockAndroidDebugBridge = createMock( AndroidDebugBridge.class );

        abstractEmulatorMojo = new AbstractEmulatorMojoToTest();
    }

    @Test
    public void testStartAndroidEmulatorWithTimeoutToConnect() throws MojoExecutionException, ExecutionException
    {
        boolean onlineAtSecondTry = false;
        int extraBootStatusPollCycles = -1;//ignored
        abstractEmulatorMojo.setWait( DEFAULT_TIMEOUT );

        IDevice emulatorDevice = withEmulatorDevice( onlineAtSecondTry, extraBootStatusPollCycles );

        withConnectedDebugBridge( emulatorDevice );

        try
        {
            abstractEmulatorMojo.startAndroidEmulator();
            fail();
        }
        catch ( MojoExecutionException e )
        {
            verify( mockExecutor );
        }
    }

    @Test
    public void testStartAndroidEmulatorAlreadyBooted() throws MojoExecutionException, ExecutionException
    {
        boolean onlineAtSecondTry = true;
        int extraBootStatusPollCycles = 0;
        abstractEmulatorMojo.setWait( DEFAULT_TIMEOUT );

        IDevice emulatorDevice = withEmulatorDevice( onlineAtSecondTry, extraBootStatusPollCycles );
        withConnectedDebugBridge( emulatorDevice );

        abstractEmulatorMojo.startAndroidEmulator();

        verify( mockExecutor );
    }

    @Test
    public void testStartAndroidEmulatorWithOngoingBoot() throws MojoExecutionException, ExecutionException
    {
        boolean onlineAtSecondTry = true;
        int extraBootStatusPollCycles = 1;
        abstractEmulatorMojo.setWait( extraBootStatusPollCycles * 5000 + 500 );

        IDevice emulatorDevice = withEmulatorDevice( onlineAtSecondTry, extraBootStatusPollCycles );
        withConnectedDebugBridge( emulatorDevice );

        abstractEmulatorMojo.startAndroidEmulator();

        verify( mockExecutor );
    }

    @Test
    public void testStartAndroidEmulatorWithBootTimeout() throws MojoExecutionException, ExecutionException
    {
        boolean onlineAtSecondTry = true;
        int extraBootStatusPollCycles = -1;
        abstractEmulatorMojo.setWait( DEFAULT_TIMEOUT );

        IDevice emulatorDevice = withEmulatorDevice( onlineAtSecondTry, extraBootStatusPollCycles );
        withConnectedDebugBridge( emulatorDevice );

        try
        {
            abstractEmulatorMojo.startAndroidEmulator();
            fail();
        }
        catch ( MojoExecutionException e )
        {
            verify( mockExecutor );
        }
    }

    /**
     * @param onlineAtSecondTry
     * @param extraBootStatusPollCycles < 0 to simulate 'stuck in boot animation'
     * @return
     */
    private IDevice withEmulatorDevice( boolean onlineAtSecondTry, int extraBootStatusPollCycles )
    {
        IDevice emulatorDevice = createMock( IDevice.class );
        expect( emulatorDevice.getAvdName() ).andReturn( AVD_NAME ).atLeastOnce();
        expect( emulatorDevice.isEmulator() ).andReturn( true ).atLeastOnce();
        if ( onlineAtSecondTry )
        {
            try
            {
                expect( emulatorDevice.isOnline() ).andReturn( false ).andReturn( true );

                if ( extraBootStatusPollCycles < 0 )
                {
                    //Simulate 'stuck in boot animation'
                    expect( emulatorDevice.getPropertySync( "dev.bootcomplete" ) )
                            .andReturn( null ).atLeastOnce();
                    expect( emulatorDevice.getPropertySync( "sys.boot_completed" ) )
                            .andReturn( null ).atLeastOnce();
                    expect( emulatorDevice.getPropertySync( "init.svc.bootanim" ) )
                            .andReturn( null ).once()
                            .andReturn( "running" ).atLeastOnce(); //never changes to "stopped"
                }
                else if ( extraBootStatusPollCycles == 0 )
                {
                    //Simulate 'already booted'
                    expect( emulatorDevice.getPropertySync( "dev.bootcomplete" ) )
                            .andReturn( "1" ).once(); //to be cached
                    expect( emulatorDevice.getPropertySync( "sys.boot_completed" ) )
                            .andReturn( "1" ).once(); //to be cached
                    expect( emulatorDevice.getPropertySync( "init.svc.bootanim" ) )
                            .andReturn( "stopped" ).once(); //to be cached
                }
                else if ( extraBootStatusPollCycles == 1 )
                {
                    //Simulate 'almost booted (1 extra poll)'
                    expect( emulatorDevice.getPropertySync( "dev.bootcomplete" ) )
                            .andReturn( null ).once()
                            .andReturn( "1" ).once(); //to be cached
                    expect( emulatorDevice.getPropertySync( "sys.boot_completed" ) )
                            .andReturn( null ).once()
                            .andReturn( "1" ).once(); //to be cached
                    expect( emulatorDevice.getPropertySync( "init.svc.bootanim" ) )
                            .andReturn( "running" ).once()
                            .andReturn( "stopped" ).once(); //to be cached
                }
                else if( extraBootStatusPollCycles >=3 )
                {
                    //Simulate 'almost booted (>=3 extra polls)'
                    expect( emulatorDevice.getPropertySync( "dev.bootcomplete" ) )
                            .andReturn( null ).times( extraBootStatusPollCycles - 1 )
                            .andReturn( "1" ).once(); //to be cached
                    expect( emulatorDevice.getPropertySync( "sys.boot_completed" ) )
                            .andReturn( null ).times( extraBootStatusPollCycles - 1 )
                            .andReturn( "1" ).once(); //to be cached
                    expect( emulatorDevice.getPropertySync( "init.svc.bootanim" ) )
                            .andReturn( null ).times( extraBootStatusPollCycles / 2 )
                            .andReturn( "running" ).times( extraBootStatusPollCycles / 2 + extraBootStatusPollCycles % 2 )
                            .andReturn( "stopped" ).once(); //to be cached
                }
                else if( extraBootStatusPollCycles >=2 )
                {
                    //Simulate 'almost booted (>=2 extra polls)'
                    expect( emulatorDevice.getPropertySync( "dev.bootcomplete" ) )
                            .andReturn( null ).times( extraBootStatusPollCycles - 1 )
                            .andReturn( "1" ).once(); //to be cached
                    expect( emulatorDevice.getPropertySync( "sys.boot_completed" ) )
                            .andReturn( null ).times( extraBootStatusPollCycles - 1 )
                            .andReturn( "1" ).once(); //to be cached
                    expect( emulatorDevice.getPropertySync( "init.svc.bootanim" ) )
                            .andReturn( "running" ).times( extraBootStatusPollCycles -1 )
                            .andReturn( "stopped" ).once(); //to be cached
                }
            }
            catch ( TimeoutException e)
            {
                throw new RuntimeException( "Unexpected checked exception during mock setup", e );
            }
            catch ( AdbCommandRejectedException e)
            {
                throw new RuntimeException( "Unexpected checked exception during mock setup", e );
            }
            catch ( ShellCommandUnresponsiveException e)
            {
                throw new RuntimeException( "Unexpected checked exception during mock setup", e );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Unexpected checked exception during mock setup", e );
            }
        }
        else
        {
            expect( emulatorDevice.isOnline() ).andReturn( false ).atLeastOnce();
        }
        replay( emulatorDevice );
        return emulatorDevice;
    }

    private void withConnectedDebugBridge( IDevice emulatorDevice )
    {
        expect( mockAndroidDebugBridge.isConnected() ).andReturn( true );
        expect( mockAndroidDebugBridge.hasInitialDeviceList() ).andReturn( true );
        expect( mockAndroidDebugBridge.getDevices() ).andReturn( new IDevice[ 0 ] ).andReturn( new IDevice[]
        { emulatorDevice } ).atLeastOnce();
        replay( mockAndroidDebugBridge );
    }

    private class AbstractEmulatorMojoToTest extends AbstractEmulatorMojo
    {
        private long wait = DEFAULT_TIMEOUT;

        public long getWait()
        {
            return wait;
        }

        public void setWait( long wait )
        {
            this.wait = wait;
        }

        @Override
        protected AndroidSdk getAndroidSdk()
        {
            return new SdkTestSupport().getSdk_with_platform_default();
        }

        @Override
        public void execute() throws MojoExecutionException, MojoFailureException
        {
        }

        @Override
        protected AndroidDebugBridge initAndroidDebugBridge() throws MojoExecutionException
        {
            return mockAndroidDebugBridge;
        }

        @Override
        String determineAvd()
        {
            return AVD_NAME;
        }

        @Override
        String determineWait()
        {
            return String.valueOf( wait );
        }
    }

}
