package com.simpligility.maven.plugins.android.standalonemojos;

import com.simpligility.maven.plugins.android.AbstractAndroidMojo;
import com.simpligility.maven.plugins.android.CommandExecutor;
import com.simpligility.maven.plugins.android.ExecutionException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Connect external IP addresses to the ADB server.
 *
 * @author demey.emmanuel@gmail.com
 */
@Mojo( name = "connect", requiresProject = false )
public class ConnectMojo extends AbstractAndroidMojo
{

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {

        if ( ips.length > 0 )
        {
            CommandExecutor executor = getExecutor();

            for ( String ip : ips )
            {
                getLog().debug( "Connecting " + ip );

                // It would be better to use the AndroidDebugBridge class
                // rather than calling the command line tool
                String command = getAndroidSdk().getAdbPath();
                // We first have to the put the bridge in tcpip mode or else it will fail to connect
                // First make sure everything is clean ...
                List<String> parameters = new ArrayList<String>();
                parameters.add( "kill-server" );

                try
                {
                    executor.executeCommand( command, parameters, false );
                    parameters.clear();
                    // initial connect to get adb in the right frame of mind
                    // http://stackoverflow.com/questions/14899935/set-adb-in-tcp-ip-mode-device-not-found
                    executor = getExecutor();
                    parameters.add( "connect" );
                    parameters.add( ip );
                    executor.executeCommand( command, parameters, false );
                    parameters.clear();
                    // ... now put in wireless mode ...
                    executor = getExecutor();
                    String hostport[] = ip.split( ":" );
                    parameters.add( "tcpip" );
                    parameters.add( hostport[1] );
                    executor.executeCommand( command, parameters, false );
                    parameters.clear();
                    // ... and finally really connect
                    executor = getExecutor();
                    parameters.add( "connect" );
                    parameters.add( ip );
                    executor.executeCommand( command, parameters, false );
                }
                catch ( ExecutionException e )
                {
                    throw new MojoExecutionException( String.format( "Can not connect %s", ip ), e );
                }
            }
        }
    }

    private CommandExecutor getExecutor()
    {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );
        executor.setCaptureStdOut( true );
        return executor;
    }
}
