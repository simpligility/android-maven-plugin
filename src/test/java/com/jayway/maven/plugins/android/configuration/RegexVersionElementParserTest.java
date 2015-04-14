package com.jayway.maven.plugins.android.configuration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

/**
 * @author Wang Xuerui <idontknw.wang@gmail.com>
 */
public class RegexVersionElementParserTest
{

    @Test
    public void prefixMatch() throws MojoExecutionException
    {
        assertArrayEquals( new int[] { 4, 1, 16, 8 }, new RegexVersionElementParser( "^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)" ).parseVersionElements( "4.1.16.8-SNAPSHOT.1946" ) );
        assertArrayEquals( new int[] { 0, 0, 38 }, new RegexVersionElementParser( "^(\\d+)\\.(\\d+)-(\\d+)" ).parseVersionElements( "0.0-38-g493f883" ) );
        assertArrayEquals( new int[] { 5, 0, 1 }, new RegexVersionElementParser( "^(\\d+)\\.(\\d+)\\.(\\d+)" ).parseVersionElements( "5.0.1 (1642443)" ) );
        assertArrayEquals( new int[] { 6, 1, 0, 66 }, new RegexVersionElementParser( "^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)" ).parseVersionElements( "6.1.0.66-r1062275" ) );
    }

    @Test
    public void fullMatch() throws MojoExecutionException
    {
        assertArrayEquals( new int[] { 5, 0, 1, 1642443 }, new RegexVersionElementParser( "^(\\d+)\\.(\\d+)\\.(\\d+) \\((\\d+)\\)$" ).parseVersionElements( "5.0.1 (1642443)" ) );
        assertArrayEquals( new int[] { 6, 1, 0, 66, 1062275 }, new RegexVersionElementParser( "^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)-r(\\d+)$" ).parseVersionElements( "6.1.0.66-r1062275" ) );
    }

    @Test
    public void variableLengthResult() throws MojoExecutionException
    {
        final String pattern1 = "^(\\d+)\\.(\\d+)(?:-(\\d+)-g[0-9a-f]+(?:-dirty)?)?$";
        assertArrayEquals( new int[] { 0, 0, 38 }, new RegexVersionElementParser( pattern1 ).parseVersionElements( "0.0-38-g493f883" ) );
        assertArrayEquals( new int[] { 0, 0, 38 }, new RegexVersionElementParser( pattern1 ).parseVersionElements( "0.0-38-g493f883-dirty" ) );
        assertArrayEquals( new int[] { 1, 0, 0 }, new RegexVersionElementParser( pattern1 ).parseVersionElements( "1.0" ) );

        final String pattern2 = "^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)(?:-[A-Z]+\\.(\\d+))?$";
        assertArrayEquals( new int[] { 4, 1, 16, 8, 1946 }, new RegexVersionElementParser( pattern2 ).parseVersionElements( "4.1.16.8-SNAPSHOT.1946" ) );
        assertArrayEquals( new int[] { 4, 1, 16, 8, 0 }, new RegexVersionElementParser( pattern2 ).parseVersionElements( "4.1.16.8" ) );
    }

    @Test
    public void failedMatch() throws MojoExecutionException
    {
        try
        {
            new RegexVersionElementParser( "^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)" ).parseVersionElements( "4.1.16-SNAPSHOT.1946" );
            fail( "Expecting MojoExecutionException" );
        }
        catch ( MojoExecutionException e )
        {
            System.err.println( "OK: " + e );
        }
    }
}
