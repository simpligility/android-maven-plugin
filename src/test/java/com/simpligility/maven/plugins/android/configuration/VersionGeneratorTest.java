
package com.simpligility.maven.plugins.android.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import com.simpligility.maven.plugins.android.configuration.VersionGenerator;

import static org.junit.Assert.*;

/**
 * @author Pappy STĂNESCU <a href="mailto:pappy.stanescu&#64;gmail.com">&lt;pappy.stanescu&#64;gmail.com&gt;</a>
 */
public class VersionGeneratorTest
{

    @Test
    public void generate() throws MojoExecutionException
    {
        assertEquals( 2147483647, new VersionGenerator( "1,2,3,4" ).generate( "2.14.748.3647" ) );
        assertEquals( 2147483647, new VersionGenerator( "4,3,2,1" ).generate( "2147.483.64.7" ) );

        // that's weird versioning scheme :)
        assertEquals( 2147483647, new VersionGenerator( "1,1,1,1,1,1,1,1,1,1" ).generate( "2.1.4.7.4.8.3.6.4.7" ) );
    }

    @Test
    public void generateRegex() throws MojoExecutionException
    {
        assertEquals( 2147483647, new VersionGenerator( "1,2,3,4", "^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$" ).generate( "2.14.748.3647" ) );
        assertEquals( 2147483647, new VersionGenerator( "4,3,2,1", "^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$" ).generate( "2147.483.64.7" ) );
    }

    @Test
    public void compare() throws MojoExecutionException {
        VersionGenerator gen = new VersionGenerator( "3,3,3" );

        assertTrue( gen.generate( "1.0" ) < gen.generate( "1.0.1" ) );
        assertTrue( gen.generate( "2.0" ) > gen.generate( "1.0.1" ) );
   }

    @Test
    public void realCase() throws MojoExecutionException
    {
        VersionGenerator gen = new VersionGenerator( "1,1,2,1,4" );

        int v1 = gen.generate( "4.1.16.8-SNAPSHOT.1946" );
        int v2 = gen.generate( "4.1.17.8-SNAPSHOT.1246" );

        assertEquals(411681946, v1 );
        assertEquals(411781246, v2 );

        assertTrue( v1 < v2 );

    }

    @Test
    public void realCaseRegex() throws MojoExecutionException
    {
        {
            VersionGenerator gen = new VersionGenerator( "1,1,2,1,4", "^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)(?:-[A-Z]+\\.(\\d+))?$" );

            int v1 = gen.generate( "4.1.16.8-SNAPSHOT.1946" );
            int v2 = gen.generate( "4.1.17.8" );

            assertEquals( 411681946, v1 );
            assertEquals( 411780000, v2 );

            assertTrue( v1 < v2 );
        }

        {
            VersionGenerator gen = new VersionGenerator( "4,3,3", "^(\\d+)\\.(\\d+)(?:-(\\d+)-g[0-9a-f]+(?:-dirty)?)?$" );

            int v1 = gen.generate( "0.0-38-g493f883" );
            int v2 = gen.generate( "0.1" );

            assertEquals( 38, v1 );
            assertEquals( 1000, v2 );

            assertTrue( v1 < v2 );
        }

        {
            VersionGenerator gen = new VersionGenerator( "3,2,2,3", "^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)" );

            int v1 = gen.generate( "6.1.0.66-r1062275" );
            int v2 = gen.generate( "6.2.0.0-r1234567" );

            assertEquals( 60100066, v1 );
            assertEquals( 60200000, v2 );

            assertTrue( v1 < v2 );
        }
    }

    @Test
    public void mixedCase() throws MojoExecutionException
    {
        VersionGenerator g1 = new VersionGenerator( "1,1,2,1,4" );
        VersionGenerator g2 = new VersionGenerator( "1,1,2,5" );

        int v1 = g1.generate( "1.2.15.8-SNAPSHOT.1946" );
        int v2 = g2.generate( "2.1.6-SNAPSHOT.1246" );

        assertEquals(121581946, v1 );
        assertEquals(210601246, v2 );

        assertTrue( v1 < v2 );

    }

    @Test
    public void faulty() throws MojoExecutionException
    {
        try
        {
            new VersionGenerator( "4,4,4" );
            fail( "Expecting IllegalArgumentException" );
        }
        catch ( IllegalArgumentException e )
        {
            System.err.println( "OK: " + e );
        }
        try
        {
            new VersionGenerator( "4,3,3" ).generate( "2200.999.999" );
            fail( "Expecting MojoExecutionException" );
        }
        catch ( MojoExecutionException e )
        {
            System.err.println( "OK: " + e );
        }

        try
        {
            new VersionGenerator( "4,3,3" ).generate( "2200.999.999" );
            fail( "Expecting MojoExecutionException" );
        }
        catch ( MojoExecutionException e )
        {
            System.err.println( "OK: " + e );
        }

        try
        {
            new VersionGenerator( "4,3,3" ).generate( "1.1000.999" );
            fail( "Expecting MojoExecutionException" );
        }
        catch ( MojoExecutionException e )
        {
            System.err.println( "OK: " + e );
        }
    }
}
