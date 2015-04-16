package com.simpligility.maven.plugins.android.configuration;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * VersionElementParser implementing the old version generator behavior.
 *
 * @author Wang Xuerui <idontknw.wang@gmail.com>
 *
 */
public class SimpleVersionElementParser implements VersionElementParser
{

    @Override
    public int[] parseVersionElements( final String versionName ) throws MojoExecutionException
    {
        final String[] versionNameElements = versionName.replaceAll( "[^0-9.]", "" ).split( "\\." );
        int[] result = new int[versionNameElements.length];

        for ( int i = 0; i < versionNameElements.length; i++ )
        {
            result[i] = Integer.valueOf( versionNameElements[i] );
        }

        return result;
    }
}
