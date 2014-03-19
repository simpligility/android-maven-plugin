package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.util.ArrayList;
import java.util.List;

/**
 * Disconnect external IP addresses from the ADB server.
 *
 * @author demey.emmanuel@gmail.com
 * @goal disconnect
 * @requiresProject false
 */
public class DisconnectMojo extends AbstractAndroidMojo
{
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if ( ips.length > 0 )
        {
            CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();

            for ( String ip : ips )
            {
                getLog().debug( "Disconnecting " + ip );

                // It would be better to use the AndroidDebugBridge class 
                // rather than calling the command line tool
                String command = getAndroidSdk().getAdbPath();

                List<String> parameters = new ArrayList<String>();
                parameters.add( "disconnect" );
                parameters.add( ip );

                try
                {
                    executor.executeCommand( command, parameters );
                }
                catch ( ExecutionException e )
                {
                    throw new MojoExecutionException( String.format( "Can not disconnect %s", ip ), e );
                }
            }
        }
    }
}