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
            CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();

            for ( String ip : ips )
            {
                getLog().debug( "Connecting " + ip );

                // It would be better to use the AndroidDebugBridge class 
                // rather than calling the command line tool
                String command = getAndroidSdk().getAdbPath();
                List<String> parameters = new ArrayList<String>();
                parameters.add( "connect" );
                parameters.add( ip );

                try
                {
                    executor.setCaptureStdOut( true );
                    executor.executeCommand( command, parameters );
                }
                catch ( ExecutionException e )
                {
                    throw new MojoExecutionException( String.format( "Can not connect %s", ip ), e );
                }
            }
        }
    }
}