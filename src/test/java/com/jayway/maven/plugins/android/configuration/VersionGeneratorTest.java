
package com.jayway.maven.plugins.android.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.junit.Assert.*;

public class VersionGeneratorTest
{

    @Test
    public void generate() throws MojoExecutionException
    {
        assertEquals( 2146999999, new VersionGenerator( 3, 3 ).generate( "2146.999.999" ) );
        assertEquals( 2147483647, new VersionGenerator( 3, 3 ).generate( "2147.483.647" ) );

        // that's weird versioning scheme :)
        assertEquals( 2147483647, new VersionGenerator( 10, 1 ).generate( "2.1.4.7.4.8.3.6.4.7" ) );

        assertTrue( new VersionGenerator( 3, 3 ).generate( "1.0" ) < new VersionGenerator( 3, 3 ).generate( "1.0.1" ) );
        assertTrue( new VersionGenerator( 3, 2 ).generate( "1.0" ) < new VersionGenerator( 3, 2 ).generate( "1.0.1" ) );
    }

    @Test
    public void maxVersionCode() throws MojoExecutionException
    {
        try
        {
            new VersionGenerator( 3, 3 ).generate( "2200.999.999" );
            fail( "Expecting MojoExecutionException" );
        }
        catch ( MojoExecutionException e )
        {
            System.err.println( "OK: " + e );
        }

        try
        {
            new VersionGenerator( 4, 3 ).generate( "2200.999.999" );
            fail( "Expecting MojoExecutionException" );
        }
        catch ( MojoExecutionException e )
        {
            System.err.println( "OK: " + e );
        }

        try
        {
            new VersionGenerator( 3, 3 ).generate( "1.1000.999" );
            fail( "Expecting MojoExecutionException" );
        }
        catch ( MojoExecutionException e )
        {
            System.err.println( "OK: " + e );
        }
    }
}
